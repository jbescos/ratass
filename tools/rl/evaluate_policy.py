#!/usr/bin/env python3
"""Evaluate the exported Ratass RL policy against the headless Java environment."""

from __future__ import annotations

import argparse
import os
from collections import defaultdict
from pathlib import Path

import jpype


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = REPO_ROOT / "desktop" / "target" / "ratass-desktop-1.0.jar"
DEFAULT_POLICY = REPO_ROOT / "assets" / "ai" / "rl_enemy_policy.json"


def start_jvm(jar_path: Path) -> None:
    if not jpype.isJVMStarted():
        jpype.startJVM(classpath=[str(jar_path)])
        configure_libgdx_files()


def configure_libgdx_files() -> None:
    jpype.JClass("com.badlogic.gdx.utils.GdxNativesLoader").load()
    gdx = jpype.JClass("com.badlogic.gdx.Gdx")
    if gdx.files is None:
        gdx.files = jpype.JClass("com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files")()


def throttle_sign(value: float, deadzone: float) -> int:
    if value > deadzone:
        return 1
    if value < -deadzone:
        return -1
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default=os.fspath(DEFAULT_JAR))
    parser.add_argument("--policy", default=os.fspath(DEFAULT_POLICY))
    parser.add_argument("--episodes", type=int, default=20)
    parser.add_argument("--field-size", type=int, default=None)
    parser.add_argument("--steps", type=int, default=None)
    parser.add_argument("--seed", type=int, default=20260506)
    parser.add_argument("--flip-deadzone", type=float, default=0.18)
    parser.add_argument("--map-id", default="")
    parser.add_argument("--per-map", action="store_true")
    parser.add_argument("--quiet", action="store_true", help="only print final summaries")
    parser.add_argument(
        "--objective",
        choices=("combat", "navigation"),
        default="combat",
        help="navigation evaluates the solo safe-circle training objective",
    )
    return parser.parse_args()


def select_map(ratass_game, map_id: str):
    if not map_id:
        return None
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    maps = arena_maps.createDefaultSet()
    for index in range(int(maps.size)):
        arena_map = maps.get(index)
        if str(arena_map.getId()) == map_id:
            return arena_map
    raise ValueError(f"Unknown map id {map_id!r}")


def main() -> None:
    args = parse_args()
    jar_path = Path(args.jar).resolve()
    policy_path = Path(args.policy).resolve()
    start_jvm(jar_path)

    ratass_game = jpype.JClass("com.github.jbescos.RatassGame")
    rl_policy = jpype.JClass("com.github.jbescos.ai.rl.RlPolicy")
    ai_control_decision = jpype.JClass("com.github.jbescos.ai.AiControlDecision")
    java_float_array = jpype.JArray(jpype.JFloat)

    policy = rl_policy.fromJson(policy_path.read_text(encoding="utf-8"))
    navigation_only = args.objective == "navigation"
    field_size = args.field_size if args.field_size is not None else (1 if navigation_only else 10)
    steps_limit = args.steps if args.steps is not None else (1200 if navigation_only else 420)
    config = (
        ratass_game.RlTrainingConfig()
        .withControlledAgentCount(1)
        .withFieldSize(field_size)
        .withMaxActionSteps(steps_limit)
        .withSeed(args.seed)
        .withNavigationOnly(navigation_only)
    )
    if navigation_only:
        config.withOpponentCount(0)
    selected_map = select_map(ratass_game, args.map_id)
    if selected_map is not None:
        config.addMap(selected_map)
    environment = ratass_game.RlTrainingEnvironment(config)

    wins = 0
    total_steps = 0
    total_reward = 0.0
    total_raw_flips = 0
    total_raw_nonzero_pairs = 0
    total_effective_flips = 0
    total_effective_nonzero_pairs = 0
    total_reverse_actions = 0
    total_effective_reverse_actions = 0
    total_small_actions = 0
    total_effective_small_actions = 0
    total_growth_pickups = 0
    total_actions = 0
    per_map = defaultdict(
        lambda: {
            "episodes": 0,
            "wins": 0,
            "steps": 0,
            "reward": 0.0,
            "actions": 0,
            "raw_flips": 0,
            "effective_flips": 0,
            "effective_reverse": 0,
            "effective_small": 0,
            "growth_pickups": 0,
            "survival": 0.0,
            "circle": 0.0,
            "edge": 0.0,
            "attack": 0.0,
            "driving": 0.0,
            "pickup": 0.0,
            "control": 0.0,
            "win": 0.0,
        }
    )
    scratch_size = int(policy.getScratchSize())

    try:
        for episode in range(1, args.episodes + 1):
            result = environment.reset()
            map_id = str(result.currentMapId)
            reward = 0.0
            steps = 0
            raw_flips = 0
            raw_nonzero_pairs = 0
            effective_flips = 0
            effective_nonzero_pairs = 0
            reverse_actions = 0
            effective_reverse_actions = 0
            small_actions = 0
            effective_small_actions = 0
            growth_pickups = 0
            reward_breakdown_names = [str(name) for name in result.rewardBreakdownNames]
            reward_breakdown_totals = [0.0 for _ in reward_breakdown_names]
            last_raw_sign = 0
            last_effective_sign = 0
            was_growth_boosted = bool(float(result.observations[11]) > 0.5)

            while not result.episodeDone and steps < steps_limit:
                observation = java_float_array([float(value) for value in result.observations])
                scratch_a = java_float_array(scratch_size)
                scratch_b = java_float_array(scratch_size)
                decision = policy.computeAction(
                    observation,
                    scratch_a,
                    scratch_b,
                    ai_control_decision(),
                )
                throttle = float(decision.throttle)
                turn = float(decision.turn)
                sign = throttle_sign(throttle, args.flip_deadzone)
                if sign != 0:
                    if last_raw_sign != 0:
                        raw_nonzero_pairs += 1
                        if sign != last_raw_sign:
                            raw_flips += 1
                    last_raw_sign = sign
                if throttle < -args.flip_deadzone:
                    reverse_actions += 1
                if abs(throttle) < args.flip_deadzone:
                    small_actions += 1

                result = environment.step(java_float_array([throttle, turn]))
                is_growth_boosted = bool(float(result.observations[11]) > 0.5)
                if is_growth_boosted and not was_growth_boosted:
                    growth_pickups += 1
                was_growth_boosted = is_growth_boosted
                effective_throttle = float(result.effectiveActions[0])
                effective_sign = throttle_sign(effective_throttle, args.flip_deadzone)
                if effective_sign != 0:
                    if last_effective_sign != 0:
                        effective_nonzero_pairs += 1
                        if effective_sign != last_effective_sign:
                            effective_flips += 1
                    last_effective_sign = effective_sign
                if effective_throttle < -args.flip_deadzone:
                    effective_reverse_actions += 1
                if abs(effective_throttle) < args.flip_deadzone:
                    effective_small_actions += 1
                reward += sum(float(value) for value in result.rewards)
                step_breakdown = [float(value) for value in result.rewardBreakdown]
                for index in range(min(len(reward_breakdown_totals), len(step_breakdown))):
                    reward_breakdown_totals[index] += step_breakdown[index]
                steps += 1

            if int(result.winnerAgentIndex) == 0:
                wins += 1
            map_stats = per_map[map_id]
            map_stats["episodes"] += 1
            if int(result.winnerAgentIndex) == 0:
                map_stats["wins"] += 1
            map_stats["steps"] += steps
            map_stats["reward"] += reward
            map_stats["actions"] += steps
            map_stats["raw_flips"] += raw_flips
            map_stats["effective_flips"] += effective_flips
            map_stats["effective_reverse"] += effective_reverse_actions
            map_stats["effective_small"] += effective_small_actions
            map_stats["growth_pickups"] += growth_pickups
            for name, value in zip(reward_breakdown_names, reward_breakdown_totals):
                map_stats[name] += value
            total_steps += steps
            total_reward += reward
            total_raw_flips += raw_flips
            total_raw_nonzero_pairs += raw_nonzero_pairs
            total_effective_flips += effective_flips
            total_effective_nonzero_pairs += effective_nonzero_pairs
            total_reverse_actions += reverse_actions
            total_effective_reverse_actions += effective_reverse_actions
            total_small_actions += small_actions
            total_effective_small_actions += effective_small_actions
            total_growth_pickups += growth_pickups
            total_actions += steps
            raw_flip_rate = raw_flips / max(1, raw_nonzero_pairs)
            effective_flip_rate = effective_flips / max(1, effective_nonzero_pairs)
            if not args.quiet:
                print(
                    f"episode={episode} "
                    f"map={map_id} "
                    f"steps={steps} "
                    f"reward={reward:.3f} "
                    f"raw_flips={raw_flips} "
                    f"raw_flip_rate={raw_flip_rate:.3f} "
                    f"effective_flips={effective_flips} "
                    f"effective_flip_rate={effective_flip_rate:.3f} "
                    f"growth_pickups={growth_pickups} "
                    f"winner={result.winnerLabel} "
                    f"winnerAgent={result.winnerAgentIndex}",
                    flush=True,
                )

        print(
            f"summary wins={wins}/{args.episodes} "
            f"avg_steps={total_steps / max(1, args.episodes):.1f} "
            f"avg_reward={total_reward / max(1, args.episodes):.3f} "
            f"raw_flip_rate={total_raw_flips / max(1, total_raw_nonzero_pairs):.3f} "
            f"effective_flip_rate={total_effective_flips / max(1, total_effective_nonzero_pairs):.3f} "
            f"raw_flips_per_step={total_raw_flips / max(1, total_actions):.3f} "
            f"effective_flips_per_step={total_effective_flips / max(1, total_actions):.3f} "
            f"raw_reverse_fraction={total_reverse_actions / max(1, total_actions):.3f} "
            f"effective_reverse_fraction={total_effective_reverse_actions / max(1, total_actions):.3f} "
            f"raw_small_throttle_fraction={total_small_actions / max(1, total_actions):.3f} "
            f"effective_small_throttle_fraction={total_effective_small_actions / max(1, total_actions):.3f} "
            f"growth_pickups_per_episode={total_growth_pickups / max(1, args.episodes):.3f}",
            flush=True,
        )
        if args.per_map:
            for map_id in sorted(per_map):
                values = per_map[map_id]
                episodes = max(1, values["episodes"])
                actions = max(1, values["actions"])
                print(
                    f"map_summary map={map_id} "
                    f"wins={values['wins']}/{values['episodes']} "
                    f"avg_steps={values['steps'] / episodes:.1f} "
                    f"avg_reward={values['reward'] / episodes:.3f} "
                    f"raw_flips_per_step={values['raw_flips'] / actions:.3f} "
                    f"effective_flips_per_step={values['effective_flips'] / actions:.3f} "
                    f"effective_reverse_fraction={values['effective_reverse'] / actions:.3f} "
                    f"effective_small_throttle_fraction={values['effective_small'] / actions:.3f} "
                    f"growth_pickups_per_episode={values['growth_pickups'] / episodes:.3f} "
                    + " ".join(
                        f"{name}={values[name] / episodes:.3f}"
                        for name in (
                            "survival",
                            "circle",
                            "edge",
                            "attack",
                            "driving",
                            "pickup",
                            "control",
                            "win",
                        )
                        if name in values
                    ),
                    flush=True,
                )
    finally:
        environment.close()


if __name__ == "__main__":
    main()
