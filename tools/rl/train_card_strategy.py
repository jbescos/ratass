#!/usr/bin/env python3
"""Train a candidate-scoring policy for roguelite card decisions."""

from __future__ import annotations

import argparse
import copy
import json
import random
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import jpype
import numpy as np
import torch
from torch import nn
from torch.distributions import Categorical


ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = ROOT / "desktop/target/ratass-desktop-1.0.jar"
DEFAULT_POLICY_ROOT = ROOT / "assets/ai/policies"


class CandidateScorer(nn.Module):
    def __init__(self, observation_size: int, hidden_size: int, hidden_layers: int):
        super().__init__()
        layers: list[nn.Module] = []
        input_size = observation_size
        for _ in range(hidden_layers):
            layers.extend((nn.Linear(input_size, hidden_size), nn.Tanh()))
            input_size = hidden_size
        layers.append(nn.Linear(input_size, 1))
        self.network = nn.Sequential(*layers)

    def forward(self, observations: torch.Tensor) -> torch.Tensor:
        return self.network(observations).squeeze(-1)


class StateValue(nn.Module):
    def __init__(self, observation_size: int, hidden_size: int, hidden_layers: int):
        super().__init__()
        layers: list[nn.Module] = []
        input_size = observation_size
        for _ in range(hidden_layers):
            layers.extend((nn.Linear(input_size, hidden_size), nn.Tanh()))
            input_size = hidden_size
        layers.append(nn.Linear(input_size, 1))
        self.network = nn.Sequential(*layers)

    def forward(self, observation: torch.Tensor) -> torch.Tensor:
        return self.network(observation).squeeze(-1)


@dataclass
class Metrics:
    episodes: int
    championships: int
    wins: int
    first_wins: int
    final_position_sum: float
    first_position_sum: float
    level_sum: float
    experience_sum: float
    reward_sum: float
    decision_sum: int
    offer_counts: dict[str, int]
    card_counts: dict[str, int]
    race_end_equipped_counts: dict[str, int]
    activation_counts: dict[str, int]
    tier_counts: dict[int, int]
    type_counts: dict[str, int]
    card_tiers: dict[str, int]
    card_types: dict[str, str]
    cards_by_type: dict[str, dict[str, int]]
    skip_count: int
    stat_synergy_count: int
    stat_synergy_gain_sum: float
    set_completion_count: int
    set_episode_count: int
    final_set_count: int
    best_set_progress_sum: float
    completed_set_counts: dict[str, int]
    championships_with_set: int
    championship_set_counts: dict[str, int]

    @property
    def win_rate(self) -> float:
        return self.wins / max(1, self.championships)

    @property
    def first_win_rate(self) -> float:
        return self.first_wins / max(1, self.episodes)

    @property
    def average_position(self) -> float:
        return self.final_position_sum / max(1, self.championships)

    @property
    def average_first_position(self) -> float:
        return self.first_position_sum / max(1, self.episodes)

    @property
    def average_reward(self) -> float:
        return self.reward_sum / max(1, self.episodes)

    def rank_key(
        self,
        selection_mode: str,
        preferred_cards: frozenset[str] = frozenset(),
    ) -> tuple[float, ...]:
        if selection_mode == "preference":
            selections = sum(
                self.card_counts.get(f"card:{card_id}", 0)
                for card_id in preferred_cards
            ) / max(1, self.episodes)
            return (selections, self.win_rate, -self.average_position)
        if selection_mode == "reward":
            return (self.average_reward, self.win_rate, -self.average_position)
        if selection_mode == "set":
            set_rate = self.championships_with_set / max(1, self.championships)
            return (set_rate, self.win_rate, -self.average_position)
        if selection_mode == "explore":
            skip_rate = self.skip_count / max(1, self.decision_sum)
            set_rate = self.championships_with_set / max(1, self.championships)
            return (-skip_rate, float(len(self.card_counts)), set_rate, self.win_rate)
        return (self.win_rate, -self.average_position, self.average_reward)

    def satisfies_identity(
        self,
        minimum_unique_cards: int,
        minimum_stat_synergies_per_episode: float,
        minimum_set_completion_rate: float,
    ) -> bool:
        synergies_per_episode = self.stat_synergy_count / max(1, self.episodes)
        return (
            len(self.card_counts) >= minimum_unique_cards
            and synergies_per_episode >= minimum_stat_synergies_per_episode
            and self.set_episode_count / max(1, self.episodes)
            >= minimum_set_completion_rate
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile-id", default="strategy00")
    parser.add_argument("--strategy-type", default="")
    parser.add_argument("--jar", default=str(DEFAULT_JAR))
    parser.add_argument("--policy-root", default=str(DEFAULT_POLICY_ROOT))
    parser.add_argument("--output", required=True)
    parser.add_argument("--checkpoint", required=True)
    parser.add_argument("--card-usage-output", default="")
    parser.add_argument("--init-checkpoint")
    parser.add_argument("--init-policy")
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--refresh-imitation", action="store_true")
    parser.add_argument("--preserve-first-championship-policy", action="store_true")
    parser.add_argument("--evaluate-only", action="store_true")
    parser.add_argument(
        "--evaluate-mode",
        choices=(
            "model",
            "algorithmic",
            "race_strength",
            "training_strength",
            "observable_strength",
            "random",
        ),
        default="model",
    )
    parser.add_argument("--force-export", action="store_true")
    parser.add_argument("--episodes", type=int, default=8000)
    parser.add_argument("--imitation-decisions", type=int, default=2000)
    parser.add_argument("--batch-episodes", type=int, default=16)
    parser.add_argument("--eval-episodes", type=int, default=500)
    parser.add_argument("--selection-eval-episodes", type=int, default=500)
    parser.add_argument("--validation-every", type=int, default=800)
    parser.add_argument("--early-stop-patience", type=int, default=0)
    parser.add_argument("--hidden-size", type=int, default=96)
    parser.add_argument("--hidden-layers", type=int, default=2)
    parser.add_argument("--lr", type=float, default=3e-4)
    parser.add_argument("--imitation-lr", type=float, default=3e-4)
    parser.add_argument("--personality-teacher-weight", type=float, default=0.0035)
    parser.add_argument(
        "--imitation-teacher",
        choices=("strength", "algorithmic"),
        default="strength",
    )
    parser.add_argument("--set-imitation-weight", type=float, default=0.0)
    parser.add_argument("--teacher-rollout-ratio", type=float, default=0.0)
    parser.add_argument("--teacher-rollout-final-ratio", type=float, default=-1.0)
    parser.add_argument("--gamma", type=float, default=0.995)
    parser.add_argument("--entropy", type=float, default=0.01)
    parser.add_argument("--ppo-clip", type=float, default=0.10)
    parser.add_argument("--ppo-epochs", type=int, default=4)
    parser.add_argument("--elite-fraction", type=float, default=0.0)
    parser.add_argument("--value-coefficient", type=float, default=0.5)
    parser.add_argument("--grad-clip", type=float, default=1.0)
    parser.add_argument("--self-play-ratio", type=float, default=0.5)
    parser.add_argument("--mixed-training-ratio", type=float, default=0.0)
    parser.add_argument("--self-play-snapshot-every", type=int, default=400)
    parser.add_argument(
        "--selection-opponents",
        choices=("default", "mixed", "self_play"),
        default="default",
    )
    parser.add_argument(
        "--selection-mode",
        choices=("win", "reward", "preference", "set", "explore"),
        default="win",
    )
    parser.add_argument("--max-win-rate-regression", type=float, default=0.0)
    parser.add_argument("--minimum-unique-cards", type=int, default=0)
    parser.add_argument("--minimum-stat-synergies-per-episode", type=float, default=0.0)
    parser.add_argument("--minimum-set-completion-rate", type=float, default=0.0)
    parser.add_argument("--mixed-opponent-policies", default="")
    parser.add_argument("--field-size", type=int, default=10)
    parser.add_argument("--circuits", type=int, default=19)
    parser.add_argument("--laps", type=int, default=5)
    parser.add_argument("--min-championships", type=int, default=1)
    parser.add_argument("--max-championships", type=int, default=1)
    parser.add_argument("--continuation-eval-championships", type=int, default=3)
    parser.add_argument("--max-first-win-rate-regression", type=float, default=0.02)
    parser.add_argument("--seed", type=int, default=20260819)
    parser.add_argument("--reward-championship-win", type=float, default=100.0)
    parser.add_argument("--reward-final-position", type=float, default=30.0)
    parser.add_argument("--reward-race-position", type=float, default=3.0)
    parser.add_argument("--reward-level", type=float, default=1.0)
    parser.add_argument("--reward-xp", type=float, default=0.10)
    parser.add_argument("--reward-novelty", type=float, default=0.0)
    parser.add_argument("--reward-skip-penalty", type=float, default=0.5)
    parser.add_argument("--reward-driver", type=float, default=0.0)
    parser.add_argument("--reward-tuning", type=float, default=0.0)
    parser.add_argument("--reward-technique", type=float, default=0.0)
    parser.add_argument("--reward-powerup", type=float, default=0.0)
    parser.add_argument("--reward-revenge", type=float, default=0.0)
    parser.add_argument("--reward-technique-amplifier", type=float, default=0.0)
    parser.add_argument("--reward-powerup-amplifier", type=float, default=0.0)
    parser.add_argument("--reward-revenge-amplifier", type=float, default=0.0)
    parser.add_argument("--reward-amplifier-link", type=float, default=0.0)
    parser.add_argument("--reward-random-powerup", type=float, default=0.0)
    parser.add_argument("--reward-random-revenge", type=float, default=0.0)
    parser.add_argument("--reward-preferred-cards", default="")
    parser.add_argument("--reward-preferred-card", type=float, default=0.0)
    parser.add_argument("--reward-discouraged-cards", default="")
    parser.add_argument("--reward-discouraged-card-penalty", type=float, default=0.0)
    parser.add_argument("--reward-lap-win", type=float, default=0.0)
    parser.add_argument("--reward-card-selection", type=float, default=0.0)
    parser.add_argument("--reward-tuning-technique-synergy", type=float, default=0.0)
    parser.add_argument("--reward-card-type-rotation", type=float, default=0.0)
    parser.add_argument("--reward-rival-powerup-overlap-penalty", type=float, default=0.0)
    parser.add_argument("--reward-rival-revenge-overlap-penalty", type=float, default=0.0)
    parser.add_argument("--reward-set-progress", type=float, default=0.0)
    parser.add_argument("--reward-set-completion", type=float, default=0.0)
    parser.add_argument("--reward-set-break-penalty", type=float, default=0.0)
    return parser.parse_args()


def start_jvm(jar: Path) -> None:
    if not jar.is_file():
        raise FileNotFoundError(f"Desktop JAR was not found: {jar}")
    if not jpype.isJVMStarted():
        jpype.startJVM(classpath=[str(jar)])


def load_driver_catalog(policy_root: Path):
    array_list = jpype.JClass("java.util.ArrayList")
    metadata_class = jpype.JClass(
        "com.github.jbescos.gameplay.roguelite.DriverProfileMetadata"
    )
    catalog_class = jpype.JClass(
        "com.github.jbescos.gameplay.roguelite.DriverProfileCatalog"
    )
    metadata = array_list()
    for path in sorted(policy_root.glob("profile*/driver_metadata.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        if int(data.get("schemaVersion", 0)) != 3:
            continue
        metadata.add(
            metadata_class(
                str(data.get("profileId", path.parent.name)),
                str(data.get("policySha256", "")),
                str(data.get("benchmarkVersion", "")),
                float(data.get("paceRating", 0.0)),
                float(data.get("controlRating", 0.0)),
                float(data.get("consistencyRating", 0.0)),
                float(data.get("finishRate", 0.0)),
                float(data.get("averageFastestLapSeconds", 0.0)),
                float(data.get("averageLapSeconds", 0.0)),
                float(data.get("averageOffRoadActions", 0.0)),
                float(data.get("averageOffRoadPercent", 0.0)),
                float(data.get("averageDriftPercent", 0.0)),
                float(data.get("maximumSpeedKph", 0.0)),
            )
        )
    if metadata.isEmpty():
        return catalog_class.fallback()
    return catalog_class(metadata)


def create_environment(args: argparse.Namespace):
    reward_class = jpype.JClass(
        "com.github.jbescos.gameplay.roguelite.strategy.CardStrategyRewardConfig"
    )
    environment_class = jpype.JClass(
        "com.github.jbescos.gameplay.roguelite.strategy.CardStrategyTrainingEnvironment"
    )
    rewards = reward_class(
        args.reward_championship_win,
        args.reward_final_position,
        args.reward_race_position,
        args.reward_level,
        args.reward_xp,
        args.reward_novelty,
        args.reward_skip_penalty,
        args.reward_driver,
        args.reward_tuning,
        args.reward_technique,
        args.reward_powerup,
        args.reward_revenge,
        args.reward_technique_amplifier,
        args.reward_powerup_amplifier,
        args.reward_revenge_amplifier,
        args.reward_amplifier_link,
        args.reward_random_powerup,
        args.reward_random_revenge,
        args.reward_preferred_cards,
        args.reward_preferred_card,
        args.reward_discouraged_cards,
        args.reward_discouraged_card_penalty,
        args.reward_lap_win,
        args.reward_card_selection,
        args.reward_tuning_technique_synergy,
        args.reward_card_type_rotation,
        args.reward_rival_powerup_overlap_penalty,
        args.reward_rival_revenge_overlap_penalty,
        args.reward_set_progress,
        args.reward_set_completion,
        args.reward_set_break_penalty,
    )
    environment = environment_class(
        load_driver_catalog(Path(args.policy_root)),
        rewards,
        args.field_size,
        args.circuits,
        args.laps,
        args.personality_teacher_weight,
    )
    environment.setChampionshipRange(
        args.min_championships, args.max_championships
    )
    configure_mixed_opponents(environment, args.mixed_opponent_policies)
    return environment


def configure_mixed_opponents(environment, configured_paths: str) -> None:
    paths = [
        Path(value.strip()).resolve()
        for value in configured_paths.split(",")
        if value.strip()
    ]
    observation_size = int(environment.getObservationSize())
    available: list[Path] = []
    for path in paths:
        if not path.is_file():
            continue
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
            policy_observation_size = int(payload.get("observationSize", -1))
        except (OSError, ValueError, TypeError, json.JSONDecodeError):
            print(
                "strategy_mixed_opponent_skipped "
                f"reason=invalid_policy profile={path.parent.name}"
            )
            continue
        if policy_observation_size != observation_size:
            print(
                "strategy_mixed_opponent_skipped "
                f"reason=shape_mismatch profile={path.parent.name} "
                f"policy_observation_size={policy_observation_size} "
                f"observation_size={observation_size}"
            )
            continue
        available.append(path)
    if not available:
        return
    profile_ids = jpype.JArray(jpype.JString)([path.parent.name for path in available])
    policies = jpype.JArray(jpype.JString)(
        [path.read_text(encoding="utf-8") for path in available]
    )
    environment.setMixedOpponentPolicies(profile_ids, policies)
    print(
        "strategy_mixed_opponents profiles="
        + ",".join(path.parent.name for path in available)
    )


def candidate_tensor(environment) -> torch.Tensor:
    values = np.asarray(
        environment.getCandidateObservations(), dtype=np.float32
    ).copy()
    return torch.from_numpy(values)


def imitate_race_strength(
    environment,
    actor: CandidateScorer,
    optimizer: torch.optim.Optimizer,
    decision_count: int,
    seed: int,
    teacher_rollout_ratio: float,
    teacher_rollout_final_ratio: float,
    teacher_mode: str,
    set_imitation_weight: float,
    first_championship_teacher: CandidateScorer | None = None,
    self_play: bool = False,
    mixed_opponents: bool = False,
) -> None:
    if decision_count <= 0:
        return
    actor.train()
    losses: list[torch.Tensor] = []
    completed = 0
    episode = 0
    teacher_matches = 0
    next_progress = min(10_000, decision_count)
    rollout_random = random.Random(seed ^ 0x24A71C)
    environment.setSelfPlayOpponents(self_play)
    environment.setMixedOpponents(mixed_opponents)
    try:
        while completed < decision_count:
            environment.reset(seed + episode)
            while not environment.isDone() and completed < decision_count:
                observations = candidate_tensor(environment)
                logits = actor(observations)
                preserve_first = (
                    first_championship_teacher is not None
                    and int(environment.getCompletedChampionshipCount()) == 0
                )
                if preserve_first:
                    with torch.no_grad():
                        strengths = first_championship_teacher(observations).detach()
                elif teacher_mode == "algorithmic":
                    teacher_index = int(environment.getAlgorithmicAction())
                    strengths = torch.zeros(logits.shape[0], dtype=torch.float32)
                    strengths[teacher_index] = 1.0
                else:
                    strengths = torch.from_numpy(
                        np.asarray(
                            environment.getTrainingTargetScores(), dtype=np.float32
                        ).copy()
                    )
                centered = strengths - strengths.mean()
                scale = centered.std()
                targets = centered / (scale + 1e-6)
                teacher_action = torch.argmax(strengths).reshape(1)
                teacher_matches += int(
                    torch.argmax(logits.detach()).item() == teacher_action.item()
                )
                choice_loss = nn.functional.cross_entropy(
                    logits.reshape(1, -1), teacher_action
                )
                score_shape_loss = nn.functional.mse_loss(logits, targets)
                selected_depth = int(
                    environment.getOfferSetDepths()[teacher_action.item()]
                )
                set_weight = 1.0 + set_imitation_weight * max(
                    0, selected_depth - 1
                )
                losses.append(
                    (choice_loss + score_shape_loss * 0.10) * set_weight
                )
                rollout_ratio = teacher_rollout_ratio
                if (
                    teacher_rollout_final_ratio >= 0.0
                    and completed >= decision_count // 2
                ):
                    rollout_ratio = teacher_rollout_final_ratio
                if rollout_random.random() < rollout_ratio:
                    action = int(torch.argmax(strengths).item())
                else:
                    action = int(torch.argmax(logits.detach()).item())
                environment.step(action)
                completed += 1
                if completed >= next_progress:
                    print(
                        "strategy_strength_distillation_progress "
                        f"decisions={completed}/{decision_count} episodes={episode + 1} "
                        f"teacher_accuracy={teacher_matches / completed:.3f}",
                        flush=True,
                    )
                    next_progress = min(decision_count, next_progress + 10_000)
                if len(losses) >= 128:
                    optimizer.zero_grad()
                    torch.stack(losses).mean().backward()
                    nn.utils.clip_grad_norm_(actor.parameters(), 1.0)
                    optimizer.step()
                    losses.clear()
            episode += 1
    finally:
        environment.setSelfPlayOpponents(False)
        environment.setMixedOpponents(False)
    if losses:
        optimizer.zero_grad()
        torch.stack(losses).mean().backward()
        nn.utils.clip_grad_norm_(actor.parameters(), 1.0)
        optimizer.step()
    print(
        f"strategy_strength_distillation decisions={completed} episodes={episode} "
        f"teacher_accuracy={teacher_matches / max(1, completed):.3f}"
    )


def evaluate(
    environment,
    actor: CandidateScorer | None,
    mode: str,
    episodes: int,
    seed: int,
    self_play: bool = False,
    mixed_opponents: bool = False,
    championship_range: tuple[int, int] | None = None,
) -> Metrics:
    rng = random.Random(seed ^ 0x7134A91)
    metrics = Metrics(
        episodes=episodes,
        championships=0,
        wins=0,
        first_wins=0,
        final_position_sum=0.0,
        first_position_sum=0.0,
        level_sum=0.0,
        experience_sum=0.0,
        reward_sum=0.0,
        decision_sum=0,
        offer_counts={},
        card_counts={},
        race_end_equipped_counts={},
        activation_counts={},
        tier_counts={},
        type_counts={},
        card_tiers={},
        card_types={},
        cards_by_type={},
        skip_count=0,
        stat_synergy_count=0,
        stat_synergy_gain_sum=0.0,
        set_completion_count=0,
        set_episode_count=0,
        final_set_count=0,
        best_set_progress_sum=0.0,
        completed_set_counts={},
        championships_with_set=0,
        championship_set_counts={},
    )
    if actor is not None:
        actor.eval()
    previous_minimum = int(environment.getMinimumChampionships())
    previous_maximum = int(environment.getMaximumChampionships())
    if championship_range is not None:
        environment.setChampionshipRange(*championship_range)
    environment.setSelfPlayOpponents(self_play)
    environment.setMixedOpponents(mixed_opponents)
    try:
        with torch.no_grad():
            for episode in range(episodes):
                environment.reset(seed + episode)
                episode_reward = 0.0
                while not environment.isDone():
                    offer_ids = [str(value) for value in environment.getOfferIds()]
                    offer_tiers = [int(value) for value in environment.getOfferTiers()]
                    offer_types = [str(value) for value in environment.getOfferTypes()]
                    for offer_index, offered in enumerate(offer_ids):
                        if offered != "skip":
                            metrics.offer_counts[offered] = (
                                metrics.offer_counts.get(offered, 0) + 1
                            )
                            metrics.card_tiers.setdefault(
                                offered, offer_tiers[offer_index]
                            )
                            metrics.card_types.setdefault(
                                offered, offer_types[offer_index]
                            )
                    stat_synergy_gains = [
                        float(value)
                        for value in environment.getOfferStatSynergyGains()
                    ]
                    if mode == "algorithmic":
                        action = int(environment.getAlgorithmicAction())
                    elif mode == "race_strength":
                        action = int(environment.getRaceStrengthAction())
                    elif mode == "training_strength":
                        action = int(environment.getTrainingTargetAction())
                    elif mode == "observable_strength":
                        action = int(
                            torch.argmax(candidate_tensor(environment)[:, -2]).item()
                        )
                    elif mode == "random":
                        action = rng.randrange(int(environment.getActionCount()))
                    else:
                        action = int(torch.argmax(actor(candidate_tensor(environment))).item())
                    selected = offer_ids[action]
                    if selected == "skip":
                        metrics.skip_count += 1
                    else:
                        metrics.card_counts[selected] = metrics.card_counts.get(selected, 0) + 1
                        tier = offer_tiers[action]
                        metrics.tier_counts[tier] = metrics.tier_counts.get(tier, 0) + 1
                        card_type = offer_types[action]
                        metrics.type_counts[card_type] = (
                            metrics.type_counts.get(card_type, 0) + 1
                        )
                        metrics.card_tiers[selected] = tier
                        type_cards = metrics.cards_by_type.setdefault(card_type, {})
                        type_cards[selected] = type_cards.get(selected, 0) + 1
                        if stat_synergy_gains[action] > 0.0:
                            metrics.stat_synergy_count += 1
                            metrics.stat_synergy_gain_sum += stat_synergy_gains[action]
                    episode_reward += float(environment.step(action))
                    metrics.decision_sum += 1
                championships = int(environment.getCompletedChampionshipCount())
                first_position = int(environment.getFirstChampionshipPosition())
                metrics.championships += championships
                metrics.wins += int(environment.getChampionshipWinCount())
                metrics.first_wins += int(first_position == 1)
                metrics.final_position_sum += int(
                    environment.getChampionshipPositionSum()
                )
                metrics.first_position_sum += first_position
                metrics.level_sum += int(environment.getLevel())
                metrics.experience_sum += int(environment.getTotalExperience())
                metrics.reward_sum += episode_reward
                set_completions = int(environment.getSetCompletionCount())
                metrics.set_completion_count += set_completions
                metrics.set_episode_count += int(set_completions > 0)
                metrics.final_set_count += int(
                    bool(str(environment.getCurrentCompletedSetId()))
                )
                metrics.best_set_progress_sum += int(environment.getBestSetProgress())
                for set_id in environment.getCompletedSetIds():
                    normalized_id = str(set_id)
                    metrics.completed_set_counts[normalized_id] = (
                        metrics.completed_set_counts.get(normalized_id, 0) + 1
                    )
                metrics.championships_with_set += int(
                    environment.getChampionshipsWithSet()
                )
                for set_id in environment.getChampionshipSetOccurrences():
                    normalized_id = str(set_id)
                    metrics.championship_set_counts[normalized_id] = (
                        metrics.championship_set_counts.get(normalized_id, 0) + 1
                    )
                count_occurrences(
                    metrics.race_end_equipped_counts,
                    environment.getRaceEndEquippedCardOccurrences(),
                )
                count_occurrences(
                    metrics.activation_counts,
                    environment.getActivatedCardOccurrences(),
                )
    finally:
        environment.setSelfPlayOpponents(False)
        environment.setMixedOpponents(False)
        environment.setChampionshipRange(previous_minimum, previous_maximum)
    return metrics


def count_occurrences(target: dict[str, int], values) -> None:
    for value in values:
        normalized = str(value)
        target[normalized] = target.get(normalized, 0) + 1


def card_catalog_metadata() -> list[dict[str, object]]:
    catalog = jpype.JClass(
        "com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog"
    )
    return [
        {
            "id": f"card:{definition.getId().name()}",
            "title": str(definition.getTitle()),
            "type": str(definition.getSlotType().name()).lower(),
            "tier": int(definition.getTier()),
        }
        for definition in catalog.all()
    ]


def card_usage_payload(
    profile_id: str,
    strategy_type: str,
    metrics: Metrics,
    catalog_cards: list[dict[str, object]],
) -> dict[str, object]:
    metadata_by_id = {
        str(card["id"]): dict(card)
        for card in catalog_cards
    }
    observed_ids = (
        set(metrics.offer_counts)
        | set(metrics.card_counts)
        | set(metrics.race_end_equipped_counts)
        | set(metrics.activation_counts)
    )
    for card_id in observed_ids:
        if card_id not in metadata_by_id:
            metadata_by_id[card_id] = {
                "id": card_id,
                "title": card_id.split(":", 1)[-1],
                "type": metrics.card_types.get(card_id, "driver"),
                "tier": metrics.card_tiers.get(card_id, 0),
            }

    cards: list[dict[str, object]] = []
    for card_id, metadata in metadata_by_id.items():
        offered = metrics.offer_counts.get(card_id, 0)
        selected = metrics.card_counts.get(card_id, 0)
        cards.append(
            {
                **metadata,
                "offered": offered,
                "selected": selected,
                "selectionRate": selected / offered if offered > 0 else None,
                "equippedAtRaceEnd": metrics.race_end_equipped_counts.get(card_id, 0),
                "activated": metrics.activation_counts.get(card_id, 0),
            }
        )
    cards.sort(
        key=lambda card: (
            str(card["type"]),
            int(card["tier"]),
            str(card["title"]),
        )
    )
    return {
        "schemaVersion": 1,
        "profileId": profile_id,
        "strategyType": strategy_type,
        "episodes": metrics.episodes,
        "championships": metrics.championships,
        "activationDefinition": (
            "Card contributed to one simulated lap. The strategy simulator uses "
            "expected card value and does not reproduce frame-level runtime triggers."
        ),
        "cards": cards,
    }


def write_card_usage_report(
    path: Path,
    profile_id: str,
    strategy_type: str,
    metrics: Metrics,
) -> None:
    payload = card_usage_payload(
        profile_id,
        strategy_type,
        metrics,
        card_catalog_metadata(),
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    unused = sum(
        1
        for card in payload["cards"]
        if int(card["offered"]) > 0 and int(card["selected"]) == 0
    )
    print(f"strategy_card_usage output={path} unused_offered_cards={unused}")


def format_metrics(name: str, metrics: Metrics) -> str:
    unique = len(metrics.card_counts)
    top_card, top_card_count = max(
        metrics.card_counts.items(),
        key=lambda item: (item[1], item[0]),
        default=("none", 0),
    )
    tiers = ",".join(
        f"T{tier}={metrics.tier_counts.get(tier, 0) / max(1, metrics.episodes):.2f}/ep"
        for tier in range(1, 5)
    )
    card_types = ",".join(
        f"{card_type}={metrics.type_counts.get(card_type, 0) / max(1, metrics.episodes):.2f}/ep"
        for card_type in ("driver", "tuning", "technique", "powerup", "revenge")
    )
    amplifier_ids = (
        "TECHNIQUE_SINGULARITY",
        "POWERUP_NEXUS",
        "NEMESIS_ENGINE",
    )
    relay_ids = (
        "LUCKY_SPARK", "CHAOS_RELAY", "WILDCARD_CORE",
        "LOADED_GRUDGE", "CHAOS_RETORT", "FATES_REVENGE",
    )
    amplifier_selections = sum(
        metrics.card_counts.get(f"card:{card_id}", 0) for card_id in amplifier_ids
    ) / max(1, metrics.episodes)
    relay_selections = sum(
        metrics.card_counts.get(f"card:{card_id}", 0) for card_id in relay_ids
    ) / max(1, metrics.episodes)
    skip_rate = metrics.skip_count / max(1, metrics.decision_sum)
    stat_synergies = metrics.stat_synergy_count / max(1, metrics.episodes)
    average_synergy_gain = metrics.stat_synergy_gain_sum / max(
        1, metrics.stat_synergy_count
    )
    set_episode_rate = metrics.set_episode_count / max(1, metrics.episodes)
    final_set_rate = metrics.final_set_count / max(1, metrics.episodes)
    set_completions = metrics.set_completion_count / max(1, metrics.episodes)
    championship_set_rate = (
        metrics.championships_with_set / max(1, metrics.championships)
    )
    top_set, top_set_count = max(
        metrics.completed_set_counts.items(),
        key=lambda item: (item[1], item[0]),
        default=("none", 0),
    )
    return (
        f"{name} episodes={metrics.episodes} championships={metrics.championships} "
        f"win_rate={metrics.win_rate:.3f} first_win_rate={metrics.first_win_rate:.3f} "
        f"avg_position={metrics.average_position:.3f} "
        f"avg_first_position={metrics.average_first_position:.3f} "
        f"avg_level={metrics.level_sum / metrics.episodes:.2f} "
        f"avg_xp={metrics.experience_sum / metrics.episodes:.1f} "
        f"avg_reward={metrics.reward_sum / metrics.episodes:.3f} "
        f"unique_cards={unique} amplifier_selections={amplifier_selections:.2f}/ep "
        f"random_relays={relay_selections:.2f}/ep "
        f"skip_rate={skip_rate:.3f} "
        f"stat_synergies={stat_synergies:.2f}/ep "
        f"avg_synergy_gain={average_synergy_gain:.3f} "
        f"set_episode_rate={set_episode_rate:.3f} "
        f"set_completions={set_completions:.3f}/ep "
        f"championship_set_rate={championship_set_rate:.3f} "
        f"distinct_sets={len(metrics.championship_set_counts)} "
        f"final_set_rate={final_set_rate:.3f} "
        f"avg_set_progress={metrics.best_set_progress_sum / max(1, metrics.episodes):.3f}/4 "
        f"top_set={top_set} top_set_rate={top_set_count / max(1, metrics.episodes):.3f} "
        f"top_card={top_card} top_card_selections={top_card_count / max(1, metrics.episodes):.2f}/ep "
        f"tier_selections={tiers} type_selections={card_types}"
    )


def format_set_preferences(name: str, metrics: Metrics) -> str:
    ordered = sorted(
        metrics.championship_set_counts.items(),
        key=lambda item: (-item[1], item[0]),
    )
    sets = ",".join(
        f"{set_id}={count / max(1, metrics.championships):.3f}/champ"
        for set_id, count in ordered
    )
    return f"{name}_sets sets={sets or 'none'}"


def format_tier_top_cards(
    name: str, metrics: Metrics, tier: int, limit: int = 5
) -> str:
    counts = [
        (card_id, count)
        for card_id, count in metrics.card_counts.items()
        if metrics.card_tiers.get(card_id) == tier
    ]
    ordered = sorted(counts, key=lambda item: (-item[1], item[0]))[:limit]
    cards = ",".join(
        f"{card_id}={count / max(1, metrics.championships):.3f}/champ"
        for card_id, count in ordered
    )
    return f"{name}_tier_top tier={tier} cards={cards or 'none'}"


def format_card_preferences(
    name: str,
    metrics: Metrics,
    card_type: str,
    limit: int = 5,
) -> str:
    counts = metrics.cards_by_type.get(card_type, {})
    ordered = sorted(counts.items(), key=lambda item: (-item[1], item[0]))[:limit]
    cards = ",".join(
        f"{card_id}:T{metrics.card_tiers.get(card_id, 0)}="
        f"{count / max(1, metrics.episodes):.3f}/ep"
        for card_id, count in ordered
    )
    return f"{name}_preferences type={card_type} cards={cards or 'none'}"


def format_tier_preferences(name: str, metrics: Metrics, tier: int) -> str:
    cells: list[str] = []
    for card_type in ("driver", "tuning", "technique", "powerup", "revenge"):
        counts = metrics.cards_by_type.get(card_type, {})
        card_id, count = max(
            (
                (candidate_id, candidate_count)
                for candidate_id, candidate_count in counts.items()
                if metrics.card_tiers.get(candidate_id) == tier
            ),
            key=lambda item: (item[1], item[0]),
            default=("none", 0),
        )
        cells.append(
            f"{card_type}={card_id}:{count / max(1, metrics.episodes):.3f}/ep"
        )
    return f"{name}_tier_preferences tier={tier} " + " ".join(cells)


def format_technique_families(name: str, metrics: Metrics) -> str:
    families = {
        "corner": ("CORNER_", "APEX_", "TRACTION_", "AGILITY_"),
        "slipstream": ("DRAFT_",),
        "straight": ("STRAIGHT_", "SPRINT_"),
        "drift": ("DRIFT_", "SLIDE_"),
        "offroad": ("RALLY_",),
        "lower_position": ("UNDERDOG_", "COMEBACK_", "LAST_PLACE_"),
        "nearby": ("CLOSE_QUARTERS", "PACK_RACER", "TRAFFIC_DOMINANCE"),
        "powerup_amplifier": ("POWERUP_",),
    }
    counts = metrics.cards_by_type.get("technique", {})
    totals: list[str] = []
    for family, prefixes in families.items():
        count = sum(
            candidate_count
            for candidate_id, candidate_count in counts.items()
            if candidate_id.removeprefix("card:").startswith(prefixes)
        )
        totals.append(f"{family}={count / max(1, metrics.episodes):.3f}/ep")
    return f"{name}_technique_families " + " ".join(totals)


def is_better(
    candidate: Metrics,
    incumbent: Metrics,
    selection_mode: str,
    minimum_win_rate: float,
    minimum_unique_cards: int,
    minimum_stat_synergies_per_episode: float,
    minimum_set_completion_rate: float,
    preferred_cards: frozenset[str] = frozenset(),
) -> bool:
    if candidate.win_rate < minimum_win_rate:
        return False
    candidate_identity = candidate.satisfies_identity(
        minimum_unique_cards,
        minimum_stat_synergies_per_episode,
        minimum_set_completion_rate,
    )
    incumbent_identity = incumbent.satisfies_identity(
        minimum_unique_cards,
        minimum_stat_synergies_per_episode,
        minimum_set_completion_rate,
    )
    if candidate_identity != incumbent_identity:
        return candidate_identity
    return candidate.rank_key(
        selection_mode, preferred_cards
    ) > incumbent.rank_key(selection_mode, preferred_cards)


def discounted_returns(rewards: Iterable[float], gamma: float) -> list[float]:
    result: list[float] = []
    value = 0.0
    for reward in reversed(list(rewards)):
        value = reward + gamma * value
        result.append(value)
    result.reverse()
    return result


def championship_discounted_returns(
    rewards: list[float], championship_boundaries: list[bool], gamma: float
) -> list[float]:
    if len(rewards) != len(championship_boundaries):
        raise ValueError("A boundary flag is required for every strategy reward.")
    result = [0.0] * len(rewards)
    value = 0.0
    for index in range(len(rewards) - 1, -1, -1):
        if championship_boundaries[index]:
            value = 0.0
        value = rewards[index] + gamma * value
        result[index] = value
    return result


def pad_candidate_observations(
    observations: list[torch.Tensor],
) -> tuple[torch.Tensor, torch.Tensor]:
    if not observations:
        raise ValueError("At least one candidate observation is required.")
    max_candidates = max(int(value.shape[0]) for value in observations)
    observation_size = int(observations[0].shape[1])
    batch = observations[0].new_zeros(
        (len(observations), max_candidates, observation_size)
    )
    mask = torch.zeros(
        (len(observations), max_candidates), dtype=torch.bool, device=batch.device
    )
    for index, value in enumerate(observations):
        if value.ndim != 2 or int(value.shape[1]) != observation_size:
            raise ValueError("Candidate observations have inconsistent shapes.")
        count = int(value.shape[0])
        batch[index, :count] = value
        mask[index, :count] = True
    return batch, mask


def masked_candidate_logits(
    actor: CandidateScorer,
    observations: torch.Tensor,
    mask: torch.Tensor,
) -> torch.Tensor:
    logits = actor(observations)
    return logits.masked_fill(~mask, torch.finfo(logits.dtype).min)


def candidate_state_mean(
    observations: torch.Tensor,
    mask: torch.Tensor,
) -> torch.Tensor:
    weights = mask.unsqueeze(-1).to(observations.dtype)
    return (observations * weights).sum(dim=1) / weights.sum(dim=1).clamp_min(1.0)


def train(
    args: argparse.Namespace,
    environment,
    actor: CandidateScorer,
    critic: StateValue,
    warm_started: bool,
) -> dict[str, torch.Tensor]:
    preferred_cards = frozenset(
        card_id.strip()
        for card_id in args.reward_preferred_cards.split(",")
        if card_id.strip()
    )
    optimizer = torch.optim.Adam(
        list(actor.parameters()) + list(critic.parameters()), lr=args.lr
    )
    imitation_optimizer = torch.optim.Adam(actor.parameters(), lr=args.imitation_lr)
    selection_episodes = min(args.selection_eval_episodes, args.eval_episodes)
    selection_self_play = args.selection_opponents == "self_play"
    selection_mixed = args.selection_opponents == "mixed"
    best_metrics = None
    best_first_metrics = None
    best_state = copy.deepcopy(actor.state_dict())
    if warm_started:
        best_metrics = evaluate(
            environment,
            actor,
            "model",
            selection_episodes,
            args.seed + 700000,
            self_play=selection_self_play,
            mixed_opponents=selection_mixed,
        )
        print(format_metrics("strategy_before_distillation", best_metrics))
        best_first_metrics = evaluate(
            environment,
            actor,
            "model",
            selection_episodes,
            args.seed + 710000,
            self_play=selection_self_play,
            mixed_opponents=selection_mixed,
            championship_range=(1, 1),
        )
        print(format_metrics("strategy_before_distillation_first", best_first_metrics))
    minimum_win_rate = max(
        0.0,
        (best_metrics.win_rate if best_metrics is not None else 0.0)
        - args.max_win_rate_regression,
    )
    minimum_first_win_rate = max(
        0.0,
        (best_first_metrics.win_rate if best_first_metrics is not None else 0.0)
        - args.max_first_win_rate_regression,
    )
    ran_imitation = (
        (not warm_started or args.refresh_imitation)
        and args.imitation_decisions > 0
    )
    if ran_imitation:
        first_championship_teacher = None
        if warm_started and args.preserve_first_championship_policy:
            first_championship_teacher = copy.deepcopy(actor)
            first_championship_teacher.eval()
        imitate_race_strength(
            environment,
            actor,
            imitation_optimizer,
            args.imitation_decisions,
            args.seed - 100000,
            args.teacher_rollout_ratio,
            args.teacher_rollout_final_ratio,
            args.imitation_teacher,
            args.set_imitation_weight,
            first_championship_teacher,
            self_play=selection_self_play,
            mixed_opponents=selection_mixed,
        )

    initial = best_metrics
    initial_first = best_first_metrics
    if initial is None or ran_imitation:
        initial = evaluate(
            environment,
            actor,
            "model",
            selection_episodes,
            args.seed + 700000,
            self_play=selection_self_play,
            mixed_opponents=selection_mixed,
        )
        print(format_metrics("strategy_after_distillation", initial))
        initial_first = evaluate(
            environment,
            actor,
            "model",
            selection_episodes,
            args.seed + 710000,
            self_play=selection_self_play,
            mixed_opponents=selection_mixed,
            championship_range=(1, 1),
        )
        print(format_metrics("strategy_after_distillation_first", initial_first))
    initial_preserves_first = (
        initial_first is not None
        and initial_first.win_rate >= minimum_first_win_rate
    )
    if best_metrics is None or (
        initial_preserves_first
        and is_better(
            initial,
            best_metrics,
            args.selection_mode,
            minimum_win_rate,
            args.minimum_unique_cards,
            args.minimum_stat_synergies_per_episode,
            args.minimum_set_completion_rate,
            preferred_cards,
        )
    ):
        best_metrics = initial
        best_first_metrics = initial_first
        best_state = copy.deepcopy(actor.state_dict())
    elif ran_imitation:
        actor.load_state_dict(best_state)
        print("strategy_distillation_reverted reason=no_selection_improvement")
    install_self_play_snapshot(environment, actor, best_state)
    opponent_random = random.Random(args.seed ^ 0x51F1A7)
    validations_without_improvement = 0

    actor.train()
    critic.train()
    for batch_start in range(0, args.episodes, args.batch_episodes):
        observations: list[torch.Tensor] = []
        actions: list[int] = []
        old_log_probs: list[torch.Tensor] = []
        all_returns: list[float] = []
        episode_trajectories: list[
            tuple[float, float, list[torch.Tensor], list[int]]
        ] = []
        batch_end = min(args.episodes, batch_start + args.batch_episodes)
        for episode in range(batch_start, batch_end):
            opponent_roll = opponent_random.random()
            use_mixed_opponents = opponent_roll < args.mixed_training_ratio
            use_self_play = (
                not use_mixed_opponents
                and opponent_roll
                < args.mixed_training_ratio + args.self_play_ratio
            )
            environment.setMixedOpponents(use_mixed_opponents)
            environment.setSelfPlayOpponents(use_self_play)
            environment.reset(args.seed + episode)
            episode_rewards: list[float] = []
            episode_observations: list[torch.Tensor] = []
            episode_actions: list[int] = []
            episode_old_log_probs: list[torch.Tensor] = []
            episode_boundaries: list[bool] = []
            while not environment.isDone():
                candidates = candidate_tensor(environment)
                logits = actor(candidates)
                distribution = Categorical(logits=logits)
                action = distribution.sample()
                championships_before = int(
                    environment.getCompletedChampionshipCount()
                )
                reward = float(environment.step(int(action.item())))
                episode_boundaries.append(
                    int(environment.getCompletedChampionshipCount())
                    > championships_before
                )
                episode_observations.append(candidates)
                episode_actions.append(int(action.item()))
                episode_old_log_probs.append(distribution.log_prob(action).detach())
                episode_rewards.append(reward)
            observations.extend(episode_observations)
            actions.extend(episode_actions)
            old_log_probs.extend(episode_old_log_probs)
            all_returns.extend(
                championship_discounted_returns(
                    episode_rewards, episode_boundaries, args.gamma
                )
            )
            episode_trajectories.append(
                (
                    float(environment.getChampionshipPositionSum())
                    / max(1, int(environment.getCompletedChampionshipCount())),
                    sum(episode_rewards),
                    episode_observations,
                    episode_actions,
                )
            )

        if args.elite_fraction > 0.0:
            elite_count = max(
                1, int(round(len(episode_trajectories) * args.elite_fraction))
            )
            elite = sorted(
                episode_trajectories, key=lambda item: (item[0], -item[1])
            )[:elite_count]
            elite_observations, elite_mask = pad_candidate_observations(
                [value for _, _, values, _ in elite for value in values]
            )
            elite_actions = torch.tensor(
                [value for _, _, _, values in elite for value in values],
                dtype=torch.long,
            )
            for _ in range(args.ppo_epochs):
                distribution = Categorical(
                    logits=masked_candidate_logits(
                        actor, elite_observations, elite_mask
                    )
                )
                actor_loss = -distribution.log_prob(elite_actions).mean()
                entropy = distribution.entropy().mean()
                value_loss = torch.zeros((), dtype=torch.float32)
                loss = actor_loss - args.entropy * entropy
                optimizer.zero_grad()
                loss.backward()
                nn.utils.clip_grad_norm_(actor.parameters(), args.grad_clip)
                optimizer.step()
        else:
            observation_tensor, candidate_mask = pad_candidate_observations(
                observations
            )
            state_tensor = candidate_state_mean(observation_tensor, candidate_mask)
            action_tensor = torch.tensor(actions, dtype=torch.long)
            old_log_prob_tensor = torch.stack(old_log_probs)
            return_tensor = torch.tensor(all_returns, dtype=torch.float32)
            with torch.no_grad():
                old_value_tensor = critic(state_tensor)
            advantage = return_tensor - old_value_tensor
            if advantage.numel() > 1:
                advantage = (advantage - advantage.mean()) / (advantage.std() + 1e-6)
            for _ in range(args.ppo_epochs):
                logits = masked_candidate_logits(
                    actor, observation_tensor, candidate_mask
                )
                distribution = Categorical(logits=logits)
                log_prob = distribution.log_prob(action_tensor)
                ratio = torch.exp(log_prob - old_log_prob_tensor)
                clipped_ratio = torch.clamp(
                    ratio, 1.0 - args.ppo_clip, 1.0 + args.ppo_clip
                )
                actor_loss = -torch.minimum(
                    ratio * advantage, clipped_ratio * advantage
                ).mean()
                entropy = distribution.entropy().mean()
                value_tensor = critic(state_tensor)
                value_loss = nn.functional.mse_loss(value_tensor, return_tensor)
                loss = (
                    actor_loss
                    - args.entropy * entropy
                    + args.value_coefficient * value_loss
                )

                optimizer.zero_grad()
                loss.backward()
                # The critic learns from large championship-scale returns. Clipping
                # both networks as one vector lets that loss suppress the actor's
                # much smaller policy gradient almost completely.
                nn.utils.clip_grad_norm_(actor.parameters(), args.grad_clip)
                nn.utils.clip_grad_norm_(critic.parameters(), args.grad_clip)
                optimizer.step()

        completed = batch_end
        if completed % args.validation_every == 0 or completed == args.episodes:
            current = evaluate(
                environment,
                actor,
                "model",
                selection_episodes,
                args.seed + 700000,
                self_play=selection_self_play,
                mixed_opponents=selection_mixed,
            )
            print(
                f"strategy_training episodes={completed} actor_loss={actor_loss.item():.4f} "
                f"value_loss={value_loss.item():.4f} entropy={entropy.item():.4f}"
            )
            print(format_metrics("strategy_candidate", current))
            current_first = evaluate(
                environment,
                actor,
                "model",
                selection_episodes,
                args.seed + 710000,
                self_play=selection_self_play,
                mixed_opponents=selection_mixed,
                championship_range=(1, 1),
            )
            print(format_metrics("strategy_candidate_first", current_first))
            if (
                current_first.win_rate >= minimum_first_win_rate
                and is_better(
                    current,
                    best_metrics,
                    args.selection_mode,
                    minimum_win_rate,
                    args.minimum_unique_cards,
                    args.minimum_stat_synergies_per_episode,
                    args.minimum_set_completion_rate,
                    preferred_cards,
                )
            ):
                best_metrics = current
                best_first_metrics = current_first
                best_state = copy.deepcopy(actor.state_dict())
                if completed % args.self_play_snapshot_every == 0:
                    install_self_play_snapshot(environment, actor, best_state)
                validations_without_improvement = 0
            else:
                validations_without_improvement += 1
                actor.load_state_dict(best_state)
                optimizer.state.clear()
                if (
                    args.early_stop_patience > 0
                    and validations_without_improvement >= args.early_stop_patience
                ):
                    print(
                        "strategy_training_early_stop "
                        f"episodes={completed} "
                        f"validations_without_improvement={validations_without_improvement}"
                    )
                    break
            actor.train()
            critic.train()
    return best_state


def policy_payload(
    model: CandidateScorer,
    observation_size: int,
    strategy_type: str = "",
) -> dict[str, object]:
    layers: list[dict[str, object]] = []
    linear_layers = [layer for layer in model.network if isinstance(layer, nn.Linear)]
    for index, layer in enumerate(linear_layers):
        layers.append(
            {
                "inputSize": layer.in_features,
                "outputSize": layer.out_features,
                "weights": layer.weight.detach().cpu().numpy().reshape(-1).tolist(),
                "bias": layer.bias.detach().cpu().numpy().tolist(),
                "activation": "linear" if index == len(linear_layers) - 1 else "tanh",
            }
        )
    payload: dict[str, object] = {
        "format": "ratass-rl-policy-v3",
        "observationSize": observation_size,
        "actionSize": 1,
        "layers": layers,
    }
    if strategy_type:
        payload["strategyType"] = strategy_type
    return payload


def export_policy(
    model: CandidateScorer,
    output_path: Path,
    observation_size: int,
    strategy_type: str,
) -> None:
    payload = policy_payload(model, observation_size, strategy_type)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, separators=(",", ":")), encoding="utf-8")


def install_self_play_snapshot(
    environment,
    actor: CandidateScorer,
    state: dict[str, torch.Tensor],
) -> None:
    snapshot = copy.deepcopy(actor.state_dict())
    actor.load_state_dict(state)
    environment.setSelfPlayPolicyJson(
        json.dumps(
            policy_payload(actor, actor.network[0].in_features),
            separators=(",", ":"),
        )
    )
    actor.load_state_dict(snapshot)


def restore_checkpoint(
    path: Path,
    actor: CandidateScorer,
    critic: StateValue,
    restore_critic: bool = True,
) -> bool:
    if not path.is_file():
        return False
    checkpoint = torch.load(path, map_location="cpu", weights_only=False)
    checkpoint_observation_size = int(checkpoint.get("observation_size", -1))
    current_observation_size = actor.network[0].in_features
    if (
        checkpoint_observation_size <= 0
        or checkpoint_observation_size > current_observation_size
        or int(checkpoint.get("hidden_size", -1)) != actor.network[0].out_features
        or int(checkpoint.get("hidden_layers", -1))
        != sum(isinstance(layer, nn.Tanh) for layer in actor.network)
    ):
        print(f"strategy_resume_skipped reason=shape_mismatch checkpoint={path}")
        return False
    actor_state = checkpoint["actor"]
    if checkpoint_observation_size < current_observation_size:
        actor_state = append_zero_input_features(actor_state, current_observation_size)
        print(
            "strategy_checkpoint_migrated "
            f"observation_size={checkpoint_observation_size}->{current_observation_size}"
        )
    actor.load_state_dict(actor_state)
    critic_state = checkpoint.get("critic")
    if restore_critic and critic_state is not None:
        if checkpoint_observation_size < current_observation_size:
            critic_state = append_zero_input_features(
                critic_state, current_observation_size
            )
        critic.load_state_dict(critic_state)
    print(f"strategy_resumed checkpoint={path}")
    return True


def restore_exported_policy(path: Path, actor: CandidateScorer) -> bool:
    if not path.is_file():
        return False
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"Cannot read strategy policy {path}: {error}") from error

    exported_layers = payload.get("layers")
    actor_layers = [layer for layer in actor.network if isinstance(layer, nn.Linear)]
    if not isinstance(exported_layers, list) or len(exported_layers) != len(actor_layers):
        raise ValueError(f"Strategy policy layer count does not match the actor: {path}")

    with torch.no_grad():
        for index, (exported, layer) in enumerate(zip(exported_layers, actor_layers)):
            if not isinstance(exported, dict):
                raise ValueError(f"Invalid strategy policy layer {index}: {path}")
            input_size = int(exported.get("inputSize", -1))
            output_size = int(exported.get("outputSize", -1))
            can_append_input = index == 0 and 0 < input_size < layer.in_features
            if output_size != layer.out_features or (
                input_size != layer.in_features and not can_append_input
            ):
                raise ValueError(
                    f"Strategy policy layer {index} shape does not match the actor: {path}"
                )
            weights = torch.tensor(exported.get("weights", []), dtype=layer.weight.dtype)
            biases = torch.tensor(exported.get("bias", []), dtype=layer.bias.dtype)
            if weights.numel() != output_size * input_size:
                raise ValueError(f"Invalid strategy policy weights in layer {index}: {path}")
            if biases.numel() != output_size:
                raise ValueError(f"Invalid strategy policy bias in layer {index}: {path}")
            restored_weights = weights.reshape(output_size, input_size)
            if can_append_input:
                restored_weights = nn.functional.pad(
                    restored_weights, (0, layer.in_features - input_size)
                )
            layer.weight.copy_(restored_weights)
            layer.bias.copy_(biases)

    print(f"strategy_initialized policy={path}")
    return True


def append_zero_input_features(
    state: dict[str, torch.Tensor],
    observation_size: int,
) -> dict[str, torch.Tensor]:
    migrated = copy.deepcopy(state)
    weight = migrated["network.0.weight"]
    expanded = torch.zeros(
        (weight.shape[0], observation_size),
        dtype=weight.dtype,
        device=weight.device,
    )
    expanded[:, : weight.shape[1]] = weight
    migrated["network.0.weight"] = expanded
    return migrated


def main() -> None:
    args = parse_args()
    if args.episodes <= 0 or args.batch_episodes <= 0 or args.eval_episodes <= 0:
        raise ValueError("Training and evaluation episode counts must be positive")
    if args.selection_eval_episodes <= 0 or args.validation_every <= 0:
        raise ValueError("Selection evaluation settings must be positive")
    if args.early_stop_patience < 0:
        raise ValueError("Early-stop patience cannot be negative")
    if not 0.0 < args.ppo_clip < 1.0 or args.ppo_epochs <= 0:
        raise ValueError("PPO clipping and epoch settings are invalid")
    if not 0.0 <= args.elite_fraction <= 1.0:
        raise ValueError("Elite trajectory fraction must be between zero and one")
    if not 0.0 <= args.self_play_ratio <= 1.0:
        raise ValueError("Self-play ratio must be between zero and one")
    if not 0.0 <= args.mixed_training_ratio <= 1.0:
        raise ValueError("Mixed training ratio must be between zero and one")
    if args.self_play_ratio + args.mixed_training_ratio > 1.0:
        raise ValueError("Self-play and mixed training ratios cannot exceed one")
    if not 0.0 <= args.teacher_rollout_ratio <= 1.0:
        raise ValueError("Teacher rollout ratio must be between zero and one")
    if args.teacher_rollout_final_ratio >= 0.0 and not (
        0.0 <= args.teacher_rollout_final_ratio <= 1.0
    ):
        raise ValueError("Final teacher rollout ratio must be between zero and one")
    if args.self_play_snapshot_every <= 0:
        raise ValueError("Self-play snapshot interval must be positive")
    if (
        args.minimum_unique_cards < 0
        or args.minimum_stat_synergies_per_episode < 0.0
        or not 0.0 <= args.minimum_set_completion_rate <= 1.0
    ):
        raise ValueError("Strategy identity thresholds cannot be negative")
    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)
    torch.set_num_threads(max(1, min(8, torch.get_num_threads())))

    start_jvm(Path(args.jar).resolve())
    environment = create_environment(args)
    environment.reset(args.seed)
    observation_size = int(environment.getObservationSize())
    actor = CandidateScorer(observation_size, args.hidden_size, args.hidden_layers)
    critic = StateValue(observation_size, args.hidden_size, args.hidden_layers)
    checkpoint_path = Path(args.checkpoint)
    initialized = bool(
        args.init_policy
        and restore_exported_policy(Path(args.init_policy), actor)
    )
    resumed = (
        not initialized
        and args.resume
        and restore_checkpoint(checkpoint_path, actor, critic)
    )
    if not resumed and not initialized and args.init_checkpoint:
        initialized = restore_checkpoint(
            Path(args.init_checkpoint), actor, critic, restore_critic=False
        )
    if args.resume and not resumed and not initialized:
        raise ValueError(
            "Strategy resume requested, but no compatible checkpoint or exported policy "
            "could be restored."
        )
    warm_started = resumed or initialized
    installed_state = copy.deepcopy(actor.state_dict()) if warm_started else None

    if args.evaluate_only:
        if args.evaluate_mode == "model" and not warm_started:
            raise ValueError(
                f"Cannot evaluate missing checkpoint: {checkpoint_path}"
            )
        metrics = evaluate(
            environment,
            actor if args.evaluate_mode == "model" else None,
            args.evaluate_mode,
            args.eval_episodes,
            args.seed + 900000,
            championship_range=(1, 1),
        )
        name = f"{args.profile_id}_evaluation_first"
        print(format_metrics(name, metrics))
        for card_type in ("driver", "tuning", "technique", "powerup", "revenge"):
            print(format_card_preferences(name, metrics, card_type))
        for tier in range(1, 5):
            print(format_tier_preferences(name, metrics, tier))
            print(format_tier_top_cards(name, metrics, tier))
        print(format_technique_families(name, metrics))
        print(format_set_preferences(name, metrics))
        continued = evaluate(
            environment,
            actor if args.evaluate_mode == "model" else None,
            args.evaluate_mode,
            args.eval_episodes,
            args.seed + 910000,
            championship_range=(
                args.continuation_eval_championships,
                args.continuation_eval_championships,
            ),
        )
        continued_name = f"{args.profile_id}_evaluation_continued"
        print(format_metrics(continued_name, continued))
        for tier in range(1, 5):
            print(format_tier_top_cards(continued_name, continued, tier))
        print(format_set_preferences(continued_name, continued))
        mixed = evaluate(
            environment,
            actor if args.evaluate_mode == "model" else None,
            args.evaluate_mode,
            args.eval_episodes,
            args.seed + 920000,
            mixed_opponents=True,
            championship_range=(1, 1),
        )
        print(format_metrics(f"{name}_mixed", mixed))
        continued_mixed = evaluate(
            environment,
            actor if args.evaluate_mode == "model" else None,
            args.evaluate_mode,
            args.eval_episodes,
            args.seed + 930000,
            mixed_opponents=True,
            championship_range=(
                args.continuation_eval_championships,
                args.continuation_eval_championships,
            ),
        )
        print(format_metrics(f"{continued_name}_mixed", continued_mixed))
        print(format_set_preferences(f"{continued_name}_mixed", continued_mixed))
        if args.evaluate_mode == "model":
            export_policy(actor, Path(args.output), observation_size, args.strategy_type)
            print(f"strategy_policy_exported output={args.output}")
        if args.card_usage_output:
            write_card_usage_report(
                Path(args.card_usage_output),
                args.profile_id,
                args.strategy_type,
                metrics,
            )
        return

    baseline_episodes = min(args.eval_episodes, 500)
    algorithmic = evaluate(
        environment, None, "algorithmic", baseline_episodes, args.seed + 800000
    )
    race_strength = evaluate(
        environment, None, "race_strength", baseline_episodes, args.seed + 800000
    )
    random_metrics = evaluate(
        environment, None, "random", baseline_episodes, args.seed + 800000
    )
    print(format_metrics("strategy_algorithmic_baseline", algorithmic))
    print(format_metrics("strategy_race_strength_baseline", race_strength))
    print(format_metrics("strategy_random_baseline", random_metrics))

    best_state = train(args, environment, actor, critic, warm_started)
    actor.load_state_dict(best_state)
    final = evaluate(environment, actor, "model", args.eval_episodes, args.seed + 900000)
    final_algorithmic = evaluate(
        environment, None, "algorithmic", args.eval_episodes, args.seed + 900000
    )
    print(format_metrics(f"{args.profile_id}_final", final))
    print(format_metrics("strategy_algorithmic_final", final_algorithmic))
    final_mixed = evaluate(
        environment,
        actor,
        "model",
        args.eval_episodes,
        args.seed + 910000,
        mixed_opponents=True,
    )
    print(format_metrics(f"{args.profile_id}_mixed_final", final_mixed))
    final_selection = final_mixed if args.selection_opponents == "mixed" else final
    if args.selection_opponents == "self_play":
        final_selection = evaluate(
            environment,
            actor,
            "model",
            args.eval_episodes,
            args.seed + 920000,
            self_play=True,
        )
        print(format_metrics(f"{args.profile_id}_self_play_final", final_selection))

    installed = None
    installed_selection = None
    if installed_state is not None:
        candidate_state = copy.deepcopy(actor.state_dict())
        actor.load_state_dict(installed_state)
        installed = evaluate(
            environment,
            actor,
            "model",
            args.eval_episodes,
            args.seed + 900000,
        )
        print(format_metrics("strategy_installed_final", installed))
        installed_selection = installed
        if args.selection_opponents == "mixed":
            installed_selection = evaluate(
                environment,
                actor,
                "model",
                args.eval_episodes,
                args.seed + 910000,
                mixed_opponents=True,
            )
            print(format_metrics("strategy_installed_mixed_final", installed_selection))
        elif args.selection_opponents == "self_play":
            installed_selection = evaluate(
                environment,
                actor,
                "model",
                args.eval_episodes,
                args.seed + 920000,
                self_play=True,
            )
            print(format_metrics("strategy_installed_self_play_final", installed_selection))
        actor.load_state_dict(candidate_state)

    promotion_baseline = (
        installed_selection if installed_selection is not None else final_algorithmic
    )
    preferred_cards = frozenset(
        card_id.strip()
        for card_id in args.reward_preferred_cards.split(",")
        if card_id.strip()
    )
    minimum_win_rate = max(
        0.0, promotion_baseline.win_rate - args.max_win_rate_regression
    )
    if not args.force_export and not is_better(
        final_selection,
        promotion_baseline,
        args.selection_mode,
        minimum_win_rate,
        args.minimum_unique_cards,
        args.minimum_stat_synergies_per_episode,
        args.minimum_set_completion_rate,
        preferred_cards,
    ):
        print(
            "strategy_policy_not_promoted reason=no_held_out_improvement "
            f"candidate_win_rate={final_selection.win_rate:.3f} "
            f"baseline_win_rate={promotion_baseline.win_rate:.3f}"
        )
        if args.card_usage_output:
            write_card_usage_report(
                Path(args.card_usage_output),
                args.profile_id,
                args.strategy_type,
                promotion_baseline,
            )
        return

    checkpoint_path.parent.mkdir(parents=True, exist_ok=True)
    torch.save(
        {
            "profile_id": args.profile_id,
            "strategy_type": args.strategy_type,
            "observation_size": observation_size,
            "hidden_size": args.hidden_size,
            "hidden_layers": args.hidden_layers,
            "actor": actor.state_dict(),
            "critic": critic.state_dict(),
            "metrics": {
                "win_rate": final_selection.win_rate,
                "average_position": final_selection.average_position,
                "average_level": final_selection.level_sum / final_selection.episodes,
                "average_experience": (
                    final_selection.experience_sum / final_selection.episodes
                ),
                "average_reward": final_selection.average_reward,
                "unique_cards": len(final_selection.card_counts),
                "selection_opponents": args.selection_opponents,
                "tier_selections_per_episode": {
                    str(tier): count / final_selection.episodes
                    for tier, count in sorted(final_selection.tier_counts.items())
                },
                "type_selections_per_episode": {
                    card_type: count / final_selection.episodes
                    for card_type, count in sorted(final_selection.type_counts.items())
                },
                "algorithmic_win_rate": final_algorithmic.win_rate,
                "algorithmic_average_position": final_algorithmic.average_position,
                "set_episode_rate": (
                    final_selection.set_episode_count / final_selection.episodes
                ),
                "set_completions_per_episode": (
                    final_selection.set_completion_count / final_selection.episodes
                ),
                "final_set_rate": (
                    final_selection.final_set_count / final_selection.episodes
                ),
                "completed_sets": final_selection.completed_set_counts,
            },
        },
        checkpoint_path,
    )
    export_policy(actor, Path(args.output), observation_size, args.strategy_type)
    if args.card_usage_output:
        write_card_usage_report(
            Path(args.card_usage_output),
            args.profile_id,
            args.strategy_type,
            final_selection,
        )
    print(f"strategy_policy={Path(args.output)}")
    print(f"strategy_checkpoint={checkpoint_path}")


if __name__ == "__main__":
    main()
