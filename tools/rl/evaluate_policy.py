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
    parser.add_argument(
        "--map-ids",
        default="",
        help="comma-separated map ids to evaluate when using --episodes-per-map",
    )
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
        choices=("target",),
        default="target",
        help="target evaluates the circle reach-and-hold objective",
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


def select_maps(ratass_game, map_ids: str):
    ids = [map_id.strip() for map_id in map_ids.split(",") if map_id.strip()]
    if not ids:
        return []
    return [select_map(ratass_game, map_id) for map_id in ids]


def make_stats():
    return {
        "episodes": 0,
        "successes": 0,
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
        "goals": 0,
        "falls": 0,
        "edge_risk_events": 0,
        "inside_time": 0.0,
        "progress_total": 0.0,
        "progress": 0.0,
        "enter": 0.0,
        "hold": 0.0,
        "complete": 0.0,
        "safety": 0.0,
        "control": 0.0,
        "alive": 0.0,
        "death": 0.0,
        "timeout": 0.0,
        "inside_steps": 0,
        "near_edge_steps": 0,
        "near_edge_fast_steps": 0,
        "unsafe_recovery_steps": 0,
        "avg_target_distance": 0.0,
        "avg_route_alignment": 0.0,
        "avg_route_lookahead_alignment": 0.0,
        "avg_recovery_alignment": 0.0,
        "avg_hold_progress": 0.0,
        "avg_recovery_speed": 0.0,
        "avg_unsafe_recovery": 0.0,
        "avg_speed": 0.0,
        "avg_edge_clearance": 0.0,
    }


def add_stats(target, source):
    for key, value in source.items():
        if key not in target:
            target[key] = 0.0
        target[key] += value


def build_config(ratass_game, args, steps_limit: int, field_size: int, arena_map=None):
    config = (
        ratass_game.RlTrainingConfig()
        .withControlledAgentCount(1)
        .withFieldSize(field_size)
        .withMaxActionSteps(steps_limit)
        .withSeed(args.seed)
    )
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
            "progress",
            "hold",
            "complete",
            "safety",
            "control",
            "alive",
            "death",
            "timeout",
            "throttle",
            "turn",
            "effective_throttle",
            "effective_turn",
            "speed",
            "edge_clearance",
            "target_dx",
            "target_dy",
            "target_distance",
            "route_dx",
            "route_dy",
            "route_forward",
            "route_side",
            "route_distance",
            "inside_circle",
            "hold_progress",
            "hold_remaining",
            "recovery_speed",
            "unsafe_recovery_speed",
            "ray_forward",
            "ray_front_left",
            "ray_front_right",
            "short_ray_forward",
            "short_ray_front_left",
            "short_ray_front_right",
            "route_clearance",
            "target_clearance",
            "stopping_risk",
            "route_lookahead_dx",
            "route_lookahead_dy",
            "route_lookahead_distance",
            "route_lookahead_forward",
            "route_lookahead_side",
            "route_lookahead_clearance",
            "recovery_forward",
            "recovery_side",
            "recovery_clearance",
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
            f"{breakdown[3]:.6f}" if len(breakdown) > 3 else "0.000000",
            f"{breakdown[4]:.6f}" if len(breakdown) > 4 else "0.000000",
            f"{breakdown[5]:.6f}" if len(breakdown) > 5 else "0.000000",
            f"{breakdown[6]:.6f}" if len(breakdown) > 6 else "0.000000",
            f"{breakdown[7]:.6f}" if len(breakdown) > 7 else "0.000000",
            f"{throttle:.6f}",
            f"{turn:.6f}",
            f"{effective_throttle:.6f}",
            f"{effective_turn:.6f}",
            f"{observations[17]:.6f}",
            f"{observations[18]:.6f}",
            f"{observations[1]:.6f}",
            f"{observations[2]:.6f}",
            f"{observations[3]:.6f}",
            f"{observations[4]:.6f}",
            f"{observations[5]:.6f}",
            f"{observations[7]:.6f}",
            f"{observations[8]:.6f}",
            f"{observations[6]:.6f}",
            f"{observations[11]:.6f}",
            f"{observations[12]:.6f}",
            f"{observations[13]:.6f}",
            f"{observations[23]:.6f}",
            f"{observations[24]:.6f}",
            f"{observations[25]:.6f}",
            f"{observations[26]:.6f}",
            f"{observations[27]:.6f}",
            f"{observations[31]:.6f}",
            f"{observations[32]:.6f}",
            f"{observations[33]:.6f}",
            f"{observations[34]:.6f}",
            f"{observations[35]:.6f}",
            f"{observations[36]:.6f}",
            f"{observations[42]:.6f}",
            f"{observations[43]:.6f}",
            f"{observations[44]:.6f}",
            f"{observations[45]:.6f}",
            f"{observations[46]:.6f}",
            f"{observations[47]:.6f}",
            f"{observations[48]:.6f}",
            f"{observations[49]:.6f}",
            f"{observations[50]:.6f}",
        ]
    )


def update_observation_stats(stats, result):
    observations = [float(value) for value in result.observations]
    stats["avg_speed"] += observations[17]
    stats["avg_edge_clearance"] += observations[18]
    stats["avg_target_distance"] += observations[3]
    stats["avg_route_alignment"] += observations[7]
    stats["avg_route_lookahead_alignment"] += observations[45]
    stats["avg_recovery_alignment"] += observations[48]
    stats["avg_hold_progress"] += observations[12]
    stats["avg_recovery_speed"] += observations[23]
    stats["avg_unsafe_recovery"] += observations[24]
    if observations[11] > 0.5:
        stats["inside_steps"] += 1
    if observations[18] < 0.25:
        stats["near_edge_steps"] += 1
    if observations[18] < 0.25 and observations[17] > 0.32:
        stats["near_edge_fast_steps"] += 1
    if observations[24] > 0.18:
        stats["unsafe_recovery_steps"] += 1
    if len(result.progressTowardTarget) > 0:
        stats["progress_total"] += float(result.progressTowardTarget[0])


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
    reward_breakdown_names = [str(name) for name in result.rewardBreakdownNames]
    reward_breakdown_totals = [0.0 for _ in reward_breakdown_names]
    last_raw_sign = 0
    last_effective_sign = 0
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
    stats["successes"] = 1 if int(result.winnerAgentIndex) == 0 else 0
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
    if len(result.goalsReached) > 0:
        stats["goals"] = int(result.goalsReached[0])
    if len(result.fallDeaths) > 0:
        stats["falls"] = int(result.fallDeaths[0])
    if len(result.edgeRiskEvents) > 0:
        stats["edge_risk_events"] = int(result.edgeRiskEvents[0])
    if len(result.insideTime) > 0:
        stats["inside_time"] = float(result.insideTime[0])
    for name, value in zip(reward_breakdown_names, reward_breakdown_totals):
        stats[name] = stats.get(name, 0.0) + value

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
            f"goals={stats['goals']} "
            f"falls={stats['falls']} "
            f"edge_risk_events={stats['edge_risk_events']} "
            f"inside_time={stats['inside_time']:.3f} "
            f"success={stats['successes']}",
            flush=True,
        )

    return map_id, stats


def summary_metrics(stats, episodes_override: int = None):
    episodes = episodes_override if episodes_override is not None else max(1, stats["episodes"])
    actions = max(1, stats["actions"])
    return {
        "success_rate": stats["successes"] / episodes,
        "avg_steps": stats["steps"] / episodes,
        "avg_reward": stats["reward"] / episodes,
        "avg_goals": stats["goals"] / episodes,
        "fall_rate": stats["falls"] / episodes,
        "edge_risk_events_per_episode": stats["edge_risk_events"] / episodes,
        "inside_time_avg": stats["inside_time"] / episodes,
        "progress_avg": stats["progress_total"] / episodes,
        "raw_flips_per_step": stats["raw_flips"] / actions,
        "effective_flips_per_step": stats["effective_flips"] / actions,
        "effective_reverse_fraction": stats["effective_reverse"] / actions,
        "effective_small_throttle_fraction": stats["effective_small"] / actions,
        "inside_fraction": stats["inside_steps"] / actions,
        "near_edge_fraction": stats["near_edge_steps"] / actions,
        "near_edge_fast_fraction": stats["near_edge_fast_steps"] / actions,
        "unsafe_recovery_fraction": stats["unsafe_recovery_steps"] / actions,
        "avg_target_distance": stats["avg_target_distance"] / actions,
        "avg_route_alignment": stats["avg_route_alignment"] / actions,
        "avg_route_lookahead_alignment": stats["avg_route_lookahead_alignment"] / actions,
        "avg_recovery_alignment": stats["avg_recovery_alignment"] / actions,
        "avg_hold_progress": stats["avg_hold_progress"] / actions,
        "avg_recovery_speed": stats["avg_recovery_speed"] / actions,
        "avg_unsafe_recovery": stats["avg_unsafe_recovery"] / actions,
        "avg_speed_signal": stats["avg_speed"] / actions,
        "avg_edge_clearance": stats["avg_edge_clearance"] / actions,
    }


def evaluation_score(stats, episodes_override: int = None) -> float:
    metrics = summary_metrics(stats, episodes_override)
    # Reward remains the main signal, but the score explicitly guards against
    # policies that look good by reward while still driving near holes or
    # oscillating controls.
    return (
        metrics["avg_reward"]
        + metrics["avg_goals"] * 35.0
        + metrics["success_rate"] * 80.0
        + metrics["inside_fraction"] * 35.0
        + metrics["inside_time_avg"] * 4.0
        + metrics["progress_avg"] * 4.0
        + metrics["avg_route_alignment"] * 10.0
        + metrics["avg_route_lookahead_alignment"] * 5.0
        - metrics["near_edge_fast_fraction"] * 80.0
        - metrics["unsafe_recovery_fraction"] * 100.0
        - metrics["effective_flips_per_step"] * 35.0
        - metrics["fall_rate"] * 450.0
    )


def print_summary(label: str, stats, episodes_override: int = None):
    episodes = episodes_override if episodes_override is not None else max(1, stats["episodes"])
    metrics = summary_metrics(stats, episodes_override)
    print(
        f"{label} "
        f"successes={stats['successes']}/{stats['episodes']} "
        f"avg_steps={metrics['avg_steps']:.1f} "
        f"avg_reward={metrics['avg_reward']:.3f} "
        f"avg_goals={metrics['avg_goals']:.3f} "
        f"fall_rate={metrics['fall_rate']:.3f} "
        f"inside_time_avg={metrics['inside_time_avg']:.3f} "
        f"progress_avg={metrics['progress_avg']:.3f} "
        f"edge_risk_events_per_episode={metrics['edge_risk_events_per_episode']:.3f} "
        f"raw_flips_per_step={metrics['raw_flips_per_step']:.3f} "
        f"effective_flips_per_step={metrics['effective_flips_per_step']:.3f} "
        f"effective_reverse_fraction={metrics['effective_reverse_fraction']:.3f} "
        f"effective_small_throttle_fraction={metrics['effective_small_throttle_fraction']:.3f} "
        f"inside_fraction={metrics['inside_fraction']:.3f} "
        f"near_edge_fraction={metrics['near_edge_fraction']:.3f} "
        f"near_edge_fast_fraction={metrics['near_edge_fast_fraction']:.3f} "
        f"unsafe_recovery_fraction={metrics['unsafe_recovery_fraction']:.3f} "
        f"avg_target_distance={metrics['avg_target_distance']:.3f} "
        f"avg_route_alignment={metrics['avg_route_alignment']:.3f} "
        f"avg_route_lookahead_alignment={metrics['avg_route_lookahead_alignment']:.3f} "
        f"avg_recovery_alignment={metrics['avg_recovery_alignment']:.3f} "
        f"avg_hold_progress={metrics['avg_hold_progress']:.3f} "
        f"avg_recovery_speed={metrics['avg_recovery_speed']:.3f} "
        f"avg_unsafe_recovery={metrics['avg_unsafe_recovery']:.3f} "
        f"avg_speed_signal={metrics['avg_speed_signal']:.3f} "
        f"avg_edge_clearance={metrics['avg_edge_clearance']:.3f} "
        + " ".join(
            f"{name}={stats[name] / episodes:.3f}"
            for name in (
                "progress",
                "enter",
                "hold",
                "complete",
                "safety",
                "control",
                "alive",
                "death",
                "timeout",
            )
            if name in stats
        ),
        flush=True,
    )


def print_evaluation_score(stats):
    metrics = summary_metrics(stats)
    score = evaluation_score(stats)
    print(
        f"evaluation_score={score:.3f} "
        f"avg_reward={metrics['avg_reward']:.3f} "
        f"avg_steps={metrics['avg_steps']:.1f} "
        f"success_rate={metrics['success_rate']:.3f} "
        f"avg_goals={metrics['avg_goals']:.3f} "
        f"fall_rate={metrics['fall_rate']:.3f} "
        f"inside_fraction={metrics['inside_fraction']:.3f} "
        f"near_edge_fast_fraction={metrics['near_edge_fast_fraction']:.3f} "
        f"unsafe_recovery_fraction={metrics['unsafe_recovery_fraction']:.3f}",
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
    field_size = args.field_size if args.field_size is not None else 1
    steps_limit = args.steps if args.steps is not None else 1350
    scratch_size = int(policy.getScratchSize())
    total_stats = make_stats()
    per_map = defaultdict(make_stats)
    selected_maps = []
    if args.map_id and args.map_ids:
        raise ValueError("Use either --map-id or --map-ids, not both")
    if args.episodes_per_map > 0:
        selected_maps = available_maps()
        if args.map_id:
            selected_maps = [select_map(ratass_game, args.map_id)]
        elif args.map_ids:
            selected_maps = select_maps(ratass_game, args.map_ids)
    else:
        selected_maps = [select_map(ratass_game, args.map_id)]

    episode_counter = 0
    for arena_map in selected_maps:
        config = build_config(
            ratass_game,
            args,
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
    print_evaluation_score(total_stats)
    if args.per_map or args.episodes_per_map > 0:
        for map_id in sorted(per_map):
            print_summary(f"map_summary map={map_id}", per_map[map_id])


if __name__ == "__main__":
    main()
