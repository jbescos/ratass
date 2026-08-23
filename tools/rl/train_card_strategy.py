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
    wins: int
    final_position_sum: float
    level_sum: float
    experience_sum: float
    reward_sum: float
    decision_sum: int
    card_counts: dict[str, int]
    tier_counts: dict[int, int]
    type_counts: dict[str, int]
    card_tiers: dict[str, int]
    cards_by_type: dict[str, dict[str, int]]
    skip_count: int
    stat_synergy_count: int
    stat_synergy_gain_sum: float

    @property
    def win_rate(self) -> float:
        return self.wins / max(1, self.episodes)

    @property
    def average_position(self) -> float:
        return self.final_position_sum / max(1, self.episodes)

    @property
    def average_reward(self) -> float:
        return self.reward_sum / max(1, self.episodes)

    def rank_key(
        self,
        selection_mode: str,
        preferred_cards: frozenset[str] = frozenset(),
    ) -> tuple[float, float, float]:
        if selection_mode == "preference":
            selections = sum(
                self.card_counts.get(f"card:{card_id}", 0)
                for card_id in preferred_cards
            ) / max(1, self.episodes)
            return (selections, self.win_rate, -self.average_position)
        if selection_mode == "reward":
            return (self.average_reward, self.win_rate, -self.average_position)
        return (self.win_rate, -self.average_position, self.average_reward)

    def satisfies_identity(
        self,
        minimum_unique_cards: int,
        minimum_stat_synergies_per_episode: float,
    ) -> bool:
        synergies_per_episode = self.stat_synergy_count / max(1, self.episodes)
        return (
            len(self.card_counts) >= minimum_unique_cards
            and synergies_per_episode >= minimum_stat_synergies_per_episode
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile-id", default="strategy00")
    parser.add_argument("--strategy-type", default="")
    parser.add_argument("--jar", default=str(DEFAULT_JAR))
    parser.add_argument("--policy-root", default=str(DEFAULT_POLICY_ROOT))
    parser.add_argument("--output", required=True)
    parser.add_argument("--checkpoint", required=True)
    parser.add_argument("--init-checkpoint")
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--refresh-imitation", action="store_true")
    parser.add_argument("--evaluate-only", action="store_true")
    parser.add_argument(
        "--evaluate-mode",
        choices=("model", "algorithmic", "race_strength", "observable_strength", "random"),
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
        choices=("win", "reward", "preference"),
        default="win",
    )
    parser.add_argument("--max-win-rate-regression", type=float, default=0.0)
    parser.add_argument("--minimum-unique-cards", type=int, default=0)
    parser.add_argument("--minimum-stat-synergies-per-episode", type=float, default=0.0)
    parser.add_argument("--mixed-opponent-policies", default="")
    parser.add_argument("--field-size", type=int, default=10)
    parser.add_argument("--circuits", type=int, default=19)
    parser.add_argument("--laps", type=int, default=5)
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
    )
    environment = environment_class(
        load_driver_catalog(Path(args.policy_root)),
        rewards,
        args.field_size,
        args.circuits,
        args.laps,
        args.personality_teacher_weight,
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
                losses.append(choice_loss + score_shape_loss * 0.10)
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
) -> Metrics:
    rng = random.Random(seed ^ 0x7134A91)
    metrics = Metrics(
        episodes, 0, 0.0, 0.0, 0.0, 0.0, 0, {}, {}, {}, {}, {}, 0, 0, 0.0
    )
    if actor is not None:
        actor.eval()
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
                    stat_synergy_gains = [
                        float(value)
                        for value in environment.getOfferStatSynergyGains()
                    ]
                    if mode == "algorithmic":
                        action = int(environment.getAlgorithmicAction())
                    elif mode == "race_strength":
                        action = int(environment.getRaceStrengthAction())
                    elif mode == "observable_strength":
                        action = int(
                            torch.argmax(candidate_tensor(environment)[:, -1]).item()
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
                position = int(environment.getFinalPosition())
                metrics.wins += int(position == 1)
                metrics.final_position_sum += position
                metrics.level_sum += int(environment.getLevel())
                metrics.experience_sum += int(environment.getTotalExperience())
                metrics.reward_sum += episode_reward
    finally:
        environment.setSelfPlayOpponents(False)
        environment.setMixedOpponents(False)
    return metrics


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
    return (
        f"{name} episodes={metrics.episodes} win_rate={metrics.win_rate:.3f} "
        f"avg_position={metrics.average_position:.3f} "
        f"avg_level={metrics.level_sum / metrics.episodes:.2f} "
        f"avg_xp={metrics.experience_sum / metrics.episodes:.1f} "
        f"avg_reward={metrics.reward_sum / metrics.episodes:.3f} "
        f"unique_cards={unique} amplifier_selections={amplifier_selections:.2f}/ep "
        f"random_relays={relay_selections:.2f}/ep "
        f"skip_rate={skip_rate:.3f} "
        f"stat_synergies={stat_synergies:.2f}/ep "
        f"avg_synergy_gain={average_synergy_gain:.3f} "
        f"top_card={top_card} top_card_selections={top_card_count / max(1, metrics.episodes):.2f}/ep "
        f"tier_selections={tiers} type_selections={card_types}"
    )


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
    preferred_cards: frozenset[str] = frozenset(),
) -> bool:
    return (
        candidate.win_rate >= minimum_win_rate
        and candidate.satisfies_identity(
            minimum_unique_cards,
            minimum_stat_synergies_per_episode,
        )
        and candidate.rank_key(selection_mode, preferred_cards)
        > incumbent.rank_key(selection_mode, preferred_cards)
    )


def discounted_returns(rewards: Iterable[float], gamma: float) -> list[float]:
    result: list[float] = []
    value = 0.0
    for reward in reversed(list(rewards)):
        value = reward + gamma * value
        result.append(value)
    result.reverse()
    return result


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
    minimum_win_rate = max(
        0.0,
        (best_metrics.win_rate if best_metrics is not None else 0.0)
        - args.max_win_rate_regression,
    )
    if not warm_started or args.refresh_imitation:
        imitate_race_strength(
            environment,
            actor,
            imitation_optimizer,
            args.imitation_decisions,
            args.seed - 100000,
            args.teacher_rollout_ratio,
            args.teacher_rollout_final_ratio,
            self_play=selection_self_play,
            mixed_opponents=selection_mixed,
        )

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
    if best_metrics is None or is_better(
        initial,
        best_metrics,
        args.selection_mode,
        minimum_win_rate,
        args.minimum_unique_cards,
        args.minimum_stat_synergies_per_episode,
        preferred_cards,
    ):
        best_metrics = initial
        best_state = copy.deepcopy(actor.state_dict())
    else:
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
            tuple[int, float, list[torch.Tensor], list[int]]
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
            while not environment.isDone():
                candidates = candidate_tensor(environment)
                logits = actor(candidates)
                distribution = Categorical(logits=logits)
                action = distribution.sample()
                reward = float(environment.step(int(action.item())))
                episode_observations.append(candidates)
                episode_actions.append(int(action.item()))
                episode_old_log_probs.append(distribution.log_prob(action).detach())
                episode_rewards.append(reward)
            observations.extend(episode_observations)
            actions.extend(episode_actions)
            old_log_probs.extend(episode_old_log_probs)
            all_returns.extend(discounted_returns(episode_rewards, args.gamma))
            episode_trajectories.append(
                (
                    int(environment.getFinalPosition()),
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
            elite_observations = torch.stack(
                [value for _, _, values, _ in elite for value in values]
            )
            elite_actions = torch.tensor(
                [value for _, _, _, values in elite for value in values],
                dtype=torch.long,
            )
            for _ in range(args.ppo_epochs):
                distribution = Categorical(logits=actor(elite_observations))
                actor_loss = -distribution.log_prob(elite_actions).mean()
                entropy = distribution.entropy().mean()
                value_loss = torch.zeros((), dtype=torch.float32)
                loss = actor_loss - args.entropy * entropy
                optimizer.zero_grad()
                loss.backward()
                nn.utils.clip_grad_norm_(actor.parameters(), args.grad_clip)
                optimizer.step()
        else:
            observation_tensor = torch.stack(observations)
            action_tensor = torch.tensor(actions, dtype=torch.long)
            old_log_prob_tensor = torch.stack(old_log_probs)
            return_tensor = torch.tensor(all_returns, dtype=torch.float32)
            with torch.no_grad():
                old_value_tensor = critic(observation_tensor.mean(dim=1))
            advantage = return_tensor - old_value_tensor
            if advantage.numel() > 1:
                advantage = (advantage - advantage.mean()) / (advantage.std() + 1e-6)
            for _ in range(args.ppo_epochs):
                logits = actor(observation_tensor)
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
                value_tensor = critic(observation_tensor.mean(dim=1))
                value_loss = nn.functional.mse_loss(value_tensor, return_tensor)
                loss = (
                    actor_loss
                    - args.entropy * entropy
                    + args.value_coefficient * value_loss
                )

                optimizer.zero_grad()
                loss.backward()
                nn.utils.clip_grad_norm_(
                    list(actor.parameters()) + list(critic.parameters()), args.grad_clip
                )
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
            if is_better(
                current,
                best_metrics,
                args.selection_mode,
                minimum_win_rate,
                args.minimum_unique_cards,
                args.minimum_stat_synergies_per_episode,
                preferred_cards,
            ):
                best_metrics = current
                best_state = copy.deepcopy(actor.state_dict())
                if completed % args.self_play_snapshot_every == 0:
                    install_self_play_snapshot(environment, actor, best_state)
                validations_without_improvement = 0
            else:
                validations_without_improvement += 1
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
        checkpoint_observation_size not in (
            current_observation_size,
            current_observation_size - 1,
        )
        or int(checkpoint.get("hidden_size", -1)) != actor.network[0].out_features
        or int(checkpoint.get("hidden_layers", -1))
        != sum(isinstance(layer, nn.Tanh) for layer in actor.network)
    ):
        print(f"strategy_resume_skipped reason=shape_mismatch checkpoint={path}")
        return False
    actor_state = checkpoint["actor"]
    if checkpoint_observation_size == current_observation_size - 1:
        actor_state = append_zero_input_feature(actor_state)
        print(
            "strategy_checkpoint_migrated "
            f"observation_size={checkpoint_observation_size}->{current_observation_size}"
        )
    actor.load_state_dict(actor_state)
    critic_state = checkpoint.get("critic")
    if restore_critic and critic_state is not None:
        if checkpoint_observation_size == current_observation_size - 1:
            critic_state = append_zero_input_feature(critic_state)
        critic.load_state_dict(critic_state)
    print(f"strategy_resumed checkpoint={path}")
    return True


def append_zero_input_feature(
    state: dict[str, torch.Tensor],
) -> dict[str, torch.Tensor]:
    migrated = copy.deepcopy(state)
    weight = migrated["network.0.weight"]
    expanded = torch.zeros(
        (weight.shape[0], weight.shape[1] + 1),
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
    if args.minimum_unique_cards < 0 or args.minimum_stat_synergies_per_episode < 0.0:
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
    resumed = args.resume and restore_checkpoint(checkpoint_path, actor, critic)
    initialized = False
    if not resumed and args.init_checkpoint:
        initialized = restore_checkpoint(
            Path(args.init_checkpoint), actor, critic, restore_critic=False
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
        )
        name = f"{args.profile_id}_evaluation"
        print(format_metrics(name, metrics))
        for card_type in ("driver", "tuning", "technique", "powerup", "revenge"):
            print(format_card_preferences(name, metrics, card_type))
        for tier in range(1, 5):
            print(format_tier_preferences(name, metrics, tier))
        print(format_technique_families(name, metrics))
        mixed = evaluate(
            environment,
            actor if args.evaluate_mode == "model" else None,
            args.evaluate_mode,
            args.eval_episodes,
            args.seed + 910000,
            mixed_opponents=True,
        )
        print(format_metrics(f"{name}_mixed", mixed))
        if args.evaluate_mode == "model":
            export_policy(actor, Path(args.output), observation_size, args.strategy_type)
            print(f"strategy_policy_exported output={args.output}")
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
        preferred_cards,
    ):
        print(
            "strategy_policy_not_promoted reason=no_held_out_improvement "
            f"candidate_win_rate={final_selection.win_rate:.3f} "
            f"baseline_win_rate={promotion_baseline.win_rate:.3f}"
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
            },
        },
        checkpoint_path,
    )
    export_policy(actor, Path(args.output), observation_size, args.strategy_type)
    print(f"strategy_policy={Path(args.output)}")
    print(f"strategy_checkpoint={checkpoint_path}")


if __name__ == "__main__":
    main()
