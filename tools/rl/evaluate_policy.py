#!/usr/bin/env python3
"""Evaluate the exported Ratass RL policy against the headless Java environment."""

from __future__ import annotations

import argparse
import csv
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
        "--episodes-per-map",
        type=int,
        default=0,
        help="run this many episodes on every map, instead of using shuffled map progression",
    )
    parser.add_argument(
        "--trace-dir",
        default="",
        help="optional directory for per-step CSV traces; use with a small episode count",
    )
    parser.add_argument(
        "--objective",
        choices=("combat", "navigation"),
        default="combat",
        help="navigation evaluates the solo safe-circle training objective",
    )
    return parser.parse_args()


def available_maps():
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    maps = arena_maps.createDefaultSet()
    return [maps.get(index) for index in range(int(maps.size))]


def select_map(ratass_game, map_id: str):
    if not map_id:
        return None
    for arena_map in available_maps():
        if str(arena_map.getId()) == map_id:
            return arena_map
    raise ValueError(f"Unknown map id {map_id!r}")


def make_stats():
    return {
        "episodes": 0,
        "wins": 0,
        "steps": 0,
        "reward": 0.0,
        "actions": 0,
        "raw_flips": 0,
        "raw_nonzero_pairs": 0,
        "effective_flips": 0,
        "effective_nonzero_pairs": 0,
        "reverse": 0,
        "effective_reverse": 0,
        "small": 0,
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
        "inside_steps": 0,
        "urgent_outside_steps": 0,
        "near_edge_steps": 0,
        "near_edge_fast_steps": 0,
        "low_route_steps": 0,
        "route_direct_steps": 0,
        "avg_safe_margin_signal": 0.0,
        "avg_route_target": 0.0,
        "avg_speed": 0.0,
        "avg_edge_clearance": 0.0,
    }


def add_stats(target, source):
    for key, value in source.items():
        target[key] += value


def build_config(ratass_game, args, navigation_only: bool, steps_limit: int, field_size: int, arena_map=None):
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
    if arena_map is not None:
        config.addMap(arena_map)
    return config


def trace_path(trace_dir: str, map_id: str, episode: int) -> Path:
    directory = Path(trace_dir)
    directory.mkdir(parents=True, exist_ok=True)
    return directory / f"{map_id}-episode{episode:03d}.csv"


def open_trace(trace_dir: str, map_id: str, episode: int):
    if not trace_dir:
        return None, None
    handle = trace_path(trace_dir, map_id, episode).open("w", encoding="utf-8", newline="")
    writer = csv.writer(handle)
    writer.writerow(
        [
            "step",
            "reward",
            "survival",
            "circle",
            "edge",
            "driving",
            "control",
            "throttle",
            "turn",
            "effective_throttle",
            "effective_turn",
            "speed",
            "edge_clearance",
            "safe_zone_dx",
            "safe_zone_dy",
            "safe_zone_margin",
            "route_dx",
            "route_dy",
            "route_direct",
            "ray_forward",
            "ray_front_left",
            "ray_front_right",
        ]
    )
    return handle, writer


def trace_step(writer, step, result, reward, throttle, turn):
    if writer is None:
        return
    observations = [float(value) for value in result.observations]
    breakdown = [float(value) for value in result.rewardBreakdown]
    effective_throttle = float(result.effectiveActions[0])
    effective_turn = float(result.effectiveActions[1])
    writer.writerow(
        [
            step,
            f"{reward:.6f}",
            f"{breakdown[0]:.6f}" if len(breakdown) > 0 else "0.000000",
            f"{breakdown[1]:.6f}" if len(breakdown) > 1 else "0.000000",
            f"{breakdown[2]:.6f}" if len(breakdown) > 2 else "0.000000",
            f"{breakdown[4]:.6f}" if len(breakdown) > 4 else "0.000000",
            f"{breakdown[6]:.6f}" if len(breakdown) > 6 else "0.000000",
            f"{throttle:.6f}",
            f"{turn:.6f}",
            f"{effective_throttle:.6f}",
            f"{effective_turn:.6f}",
            f"{observations[8]:.6f}",
            f"{observations[9]:.6f}",
            f"{observations[27]:.6f}",
            f"{observations[28]:.6f}",
            f"{observations[29]:.6f}",
            f"{observations[30]:.6f}",
            f"{observations[31]:.6f}",
            f"{observations[32]:.6f}",
            f"{observations[33]:.6f}",
            f"{observations[34]:.6f}",
            f"{observations[35]:.6f}",
        ]
    )


def update_observation_stats(stats, result):
    observations = [float(value) for value in result.observations]
    stats["avg_speed"] += observations[8]
    stats["avg_edge_clearance"] += observations[9]
    stats["avg_safe_margin_signal"] += observations[29]
    route_dx = observations[30]
    route_dy = observations[31]
    route_distance_signal = (route_dx * route_dx + route_dy * route_dy) ** 0.5
    stats["avg_route_target"] += route_distance_signal
    if observations[29] >= 0.0:
        stats["inside_steps"] += 1
    if observations[29] < -0.55:
        stats["urgent_outside_steps"] += 1
    if observations[9] < 0.25:
        stats["near_edge_steps"] += 1
    if observations[9] < 0.25 and observations[8] > 0.32:
        stats["near_edge_fast_steps"] += 1
    if route_distance_signal < 0.10 and observations[29] < 0.0:
        stats["low_route_steps"] += 1
    if observations[32] > 0.5:
        stats["route_direct_steps"] += 1


def run_episode(
    args,
    environment,
    policy,
    java_float_array,
    ai_control_decision,
    scratch_size,
    steps_limit,
    episode,
):
    result = environment.reset()
    map_id = str(result.currentMapId)
    stats = make_stats()
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
    trace_handle, trace_writer = open_trace(args.trace_dir, map_id, episode)

    try:
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
            step_reward = sum(float(value) for value in result.rewards)
            trace_step(trace_writer, steps + 1, result, step_reward, throttle, turn)
            update_observation_stats(stats, result)

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
            reward += step_reward
            step_breakdown = [float(value) for value in result.rewardBreakdown]
            for index in range(min(len(reward_breakdown_totals), len(step_breakdown))):
                reward_breakdown_totals[index] += step_breakdown[index]
            steps += 1
    finally:
        if trace_handle is not None:
            trace_handle.close()

    stats["episodes"] = 1
    stats["wins"] = 1 if int(result.winnerAgentIndex) == 0 else 0
    stats["steps"] = steps
    stats["reward"] = reward
    stats["actions"] = steps
    stats["raw_flips"] = raw_flips
    stats["raw_nonzero_pairs"] = raw_nonzero_pairs
    stats["effective_flips"] = effective_flips
    stats["effective_nonzero_pairs"] = effective_nonzero_pairs
    stats["reverse"] = reverse_actions
    stats["effective_reverse"] = effective_reverse_actions
    stats["small"] = small_actions
    stats["effective_small"] = effective_small_actions
    stats["growth_pickups"] = growth_pickups
    for name, value in zip(reward_breakdown_names, reward_breakdown_totals):
        stats[name] += value

    if not args.quiet:
        raw_flip_rate = raw_flips / max(1, raw_nonzero_pairs)
        effective_flip_rate = effective_flips / max(1, effective_nonzero_pairs)
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

    return map_id, stats


def print_summary(label: str, stats, episodes_override: int = None):
    episodes = episodes_override if episodes_override is not None else max(1, stats["episodes"])
    actions = max(1, stats["actions"])
    print(
        f"{label} "
        f"wins={stats['wins']}/{stats['episodes']} "
        f"avg_steps={stats['steps'] / episodes:.1f} "
        f"avg_reward={stats['reward'] / episodes:.3f} "
        f"raw_flips_per_step={stats['raw_flips'] / actions:.3f} "
        f"effective_flips_per_step={stats['effective_flips'] / actions:.3f} "
        f"effective_reverse_fraction={stats['effective_reverse'] / actions:.3f} "
        f"effective_small_throttle_fraction={stats['effective_small'] / actions:.3f} "
        f"inside_fraction={stats['inside_steps'] / actions:.3f} "
        f"urgent_outside_fraction={stats['urgent_outside_steps'] / actions:.3f} "
        f"near_edge_fraction={stats['near_edge_steps'] / actions:.3f} "
        f"near_edge_fast_fraction={stats['near_edge_fast_steps'] / actions:.3f} "
        f"low_route_fraction={stats['low_route_steps'] / actions:.3f} "
        f"route_direct_fraction={stats['route_direct_steps'] / actions:.3f} "
        f"avg_safe_margin_signal={stats['avg_safe_margin_signal'] / actions:.3f} "
        f"avg_route_target={stats['avg_route_target'] / actions:.3f} "
        f"avg_speed_signal={stats['avg_speed'] / actions:.3f} "
        f"avg_edge_clearance={stats['avg_edge_clearance'] / actions:.3f} "
        f"growth_pickups_per_episode={stats['growth_pickups'] / episodes:.3f} "
        + " ".join(
            f"{name}={stats[name] / episodes:.3f}"
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
            if name in stats
        ),
        flush=True,
    )


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
    scratch_size = int(policy.getScratchSize())
    total_stats = make_stats()
    per_map = defaultdict(make_stats)
    selected_maps = []
    if args.episodes_per_map > 0:
        selected_maps = available_maps()
        if args.map_id:
            selected_maps = [select_map(ratass_game, args.map_id)]
    else:
        selected_maps = [select_map(ratass_game, args.map_id)]

    episode_counter = 0
    for arena_map in selected_maps:
        config = build_config(
            ratass_game,
            args,
            navigation_only,
            steps_limit,
            field_size,
            arena_map,
        )
        environment = ratass_game.RlTrainingEnvironment(config)
        try:
            episode_count = args.episodes_per_map if args.episodes_per_map > 0 else args.episodes
            for _ in range(episode_count):
                episode_counter += 1
                map_id, episode_stats = run_episode(
                    args,
                    environment,
                    policy,
                    java_float_array,
                    ai_control_decision,
                    scratch_size,
                    steps_limit,
                    episode_counter,
                )
                add_stats(total_stats, episode_stats)
                add_stats(per_map[map_id], episode_stats)
        finally:
            environment.close()

    print_summary("summary", total_stats)
    if args.per_map or args.episodes_per_map > 0:
        for map_id in sorted(per_map):
            print_summary(f"map_summary map={map_id}", per_map[map_id])


if __name__ == "__main__":
    main()
