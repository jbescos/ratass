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
DEFAULT_ACTION_REPEAT = 4
PHYSICS_STEP_SECONDS = 1.0 / 60.0
OBS_CLOSE_CAR_THRESHOLD = 0.18
OBS_TRACE_SIZE = 41
REWARD_BUCKETS = (
    "route_progress",
    "step_cost",
    "off_road",
    "steering",
    "reverse_speed",
    "car_push",
    "route_alignment",
)


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
    parser.add_argument(
        "--controlled-agents",
        type=int,
        default=None,
        help="number of learner cars to control with the evaluated policy",
    )
    parser.add_argument("--field-size", type=int, default=None)
    parser.add_argument("--steps", type=int, default=None)
    parser.add_argument(
        "--action-repeat",
        type=int,
        default=DEFAULT_ACTION_REPEAT,
        help="physics steps per RL action; used for elapsed goal timing",
    )
    parser.add_argument(
        "--random-race-spawns",
        action="store_true",
        default=True,
        help="evaluate from safe random road positions instead of the fixed starting grid",
    )
    parser.add_argument(
        "--fixed-race-spawns",
        action="store_false",
        dest="random_race_spawns",
        help="debug override: evaluate from fixed map spawn points",
    )
    parser.add_argument(
        "--route-targets",
        type=int,
        default=6,
        help="episode route-target count; -1 means one current-map lap, 0 means the full live-race lap count",
    )
    parser.add_argument(
        "--route-target-fraction",
        type=float,
        default=0.0,
        help="optional fraction of a lap for each random-spawn route target",
    )
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
        choices=("race",),
        default="race",
        help="race evaluates route progress around the circuit",
    )
    return parser.parse_args()


def available_maps():
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    maps = arena_maps.createDefaultSet()
    return [maps.get(index) for index in range(int(maps.size))]


def select_map(ratass_game, map_id: str):
    if not map_id:
        return None
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    for maps in (arena_maps.createHeadlessTrainingSet(), arena_maps.createDefaultSet()):
        for index in range(int(maps.size)):
            arena_map = maps.get(index)
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
        "goal_time_seconds": 0.0,
        "goal_time_count": 0,
        "actions": 0,
        "raw_flips": 0,
        "raw_nonzero_pairs": 0,
        "effective_flips": 0,
        "effective_nonzero_pairs": 0,
        "reverse": 0,
        "effective_reverse": 0,
        "small": 0,
        "effective_small": 0,
        "targets": 0,
        "progress_total": 0.0,
        "route_progress": 0.0,
        "step_cost": 0.0,
        "off_road": 0.0,
        "steering": 0.0,
        "reverse_speed": 0.0,
        "car_push": 0.0,
        "route_alignment": 0.0,
        "lookahead_blocked_steps": 0,
        "off_road_steps": 0,
        "near_edge_steps": 0,
        "near_edge_fast_steps": 0,
        "braking_risk_steps": 0,
        "avg_route_alignment": 0.0,
        "avg_route_lookahead_alignment": 0.0,
        "avg_route_curvature": 0.0,
        "avg_route_left_clearance": 0.0,
        "avg_route_right_clearance": 0.0,
        "avg_route_offset_forward": 0.0,
        "avg_route_offset_side": 0.0,
        "avg_route_offset_distance": 0.0,
        "avg_route_lookahead_distance": 0.0,
        "avg_next_corner_distance": 0.0,
        "avg_next_corner_direction": 0.0,
        "avg_next_corner_severity": 0.0,
        "avg_route_width": 0.0,
        "avg_recovery_alignment": 0.0,
        "avg_track_slowdown": 0.0,
        "avg_steering_authority": 0.0,
        "avg_lateral_slip": 0.0,
        "avg_braking_distance": 0.0,
        "avg_braking_risk": 0.0,
        "avg_speed": 0.0,
        "avg_edge_clearance": 0.0,
        "avg_left_road_clearance": 0.0,
        "avg_right_road_clearance": 0.0,
        "avg_front_road_clearance": 0.0,
        "avg_front_left_road_clearance": 0.0,
        "avg_front_right_road_clearance": 0.0,
        "observation_samples": 0,
        "near_car_steps": 0,
        "close_car_steps": 0,
    }


def add_stats(target, source):
    for key, value in source.items():
        if key not in target:
            target[key] = 0.0
        target[key] += value


def build_config(
    ratass_game,
    args,
    steps_limit: int,
    controlled_agents: int,
    field_size: int,
    arena_map=None,
):
    config = (
        ratass_game.RlTrainingConfig()
        .withControlledAgentCount(controlled_agents)
        .withFieldSize(field_size)
        .withActionRepeat(max(1, args.action_repeat))
        .withMaxActionSteps(steps_limit)
        .withRouteTargets(args.route_targets)
        .withRouteTargetFraction(float(args.route_target_fraction))
        .withRaceMode(True)
        .withRandomRaceSpawns(args.random_race_spawns)
        .withDebugTraceEnabled(bool(args.trace_dir))
        .withSeed(args.seed)
    )
    if arena_map is not None:
        config.addMap(arena_map)
    return config


def trace_path(trace_dir: str, map_id: str, episode: int) -> Path:
    directory = Path(trace_dir)
    directory.mkdir(parents=True, exist_ok=True)
    return directory / f"{map_id}-episode{episode:03d}.csv"


def open_trace(trace_dir: str, map_id: str, episode: int, debug_trace_names):
    if not trace_dir:
        return None, None
    handle = trace_path(trace_dir, map_id, episode).open("w", encoding="utf-8", newline="")
    writer = csv.writer(handle)
    debug_headers = [f"debug_{name}" for name in debug_trace_names]
    writer.writerow(
        [
            "step",
            "reward",
            "route_progress",
            "step_cost",
            "off_road",
            "steering",
            "reverse_speed",
            "car_push",
            "route_alignment",
            "throttle",
            "turn",
            "effective_throttle",
            "effective_turn",
            "route_progress_normalized",
            "route_tangent_forward",
            "route_tangent_side",
            "route_lookahead_forward",
            "route_lookahead_side",
            "route_lookahead_clearance",
            "near_route_tangent_forward",
            "near_route_tangent_side",
            "far_route_tangent_forward",
            "far_route_tangent_side",
            "route_curvature",
            "route_left_clearance",
            "route_right_clearance",
            "speed",
            "forward_speed",
            "lateral_speed",
            "angular_velocity",
            "previous_throttle",
            "previous_turn",
            "off_road",
            "off_road_distance",
            "edge_clearance",
            "left_road_clearance",
            "right_road_clearance",
            "front_road_clearance",
            "front_left_road_clearance",
            "front_right_road_clearance",
            "car_ray_forward",
            "car_ray_front_left",
            "car_ray_front_right",
            "car_ray_left",
            "car_ray_right",
            "car_ray_rear",
            "route_offset_forward",
            "route_offset_side",
            "route_offset_distance",
            "route_lookahead_distance",
            "next_corner_distance",
            "next_corner_direction",
            "next_corner_severity",
            "route_width",
        ]
        + debug_headers
    )
    return handle, writer


def trace_step(writer, step, result, reward, throttle, turn):
    if writer is None:
        return
    observations = [float(value) for value in result.observations]
    breakdown = [float(value) for value in result.rewardBreakdown]
    debug_trace = [float(value) for value in result.debugTrace]
    debug_trace_names = [str(value) for value in result.debugTraceNames]
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
            f"{throttle:.6f}",
            f"{turn:.6f}",
            f"{effective_throttle:.6f}",
            f"{effective_turn:.6f}",
        ]
        + [
            f"{observations[index]:.6f}" if index < len(observations) else "0.000000"
            for index in range(OBS_TRACE_SIZE)
        ]
        + [
            f"{debug_trace[index]:.6f}" if index < len(debug_trace) else "0.000000"
            for index in range(len(debug_trace_names))
        ]
    )


def update_observation_stats(stats, result, controlled_agents: int, observation_size: int):
    observations = [float(value) for value in result.observations]
    for agent_index in range(controlled_agents):
        offset = agent_index * observation_size
        if offset + observation_size > len(observations):
            break
        stats["observation_samples"] += 1
        stats["avg_speed"] += observations[offset + 13]
        stats["avg_edge_clearance"] += observations[offset + 21]
        stats["avg_route_alignment"] += observations[offset + 1]
        stats["avg_route_lookahead_alignment"] += observations[offset + 3]
        stats["avg_route_curvature"] += abs(observations[offset + 10])
        stats["avg_route_left_clearance"] += observations[offset + 11]
        stats["avg_route_right_clearance"] += observations[offset + 12]
        if observation_size > 36:
            stats["avg_route_offset_forward"] += observations[offset + 33]
            stats["avg_route_offset_side"] += observations[offset + 34]
            stats["avg_route_offset_distance"] += observations[offset + 35]
            stats["avg_route_lookahead_distance"] += observations[offset + 36]
        if observation_size > 40:
            stats["avg_next_corner_distance"] += observations[offset + 37]
            stats["avg_next_corner_direction"] += observations[offset + 38]
            stats["avg_next_corner_severity"] += observations[offset + 39]
            stats["avg_route_width"] += observations[offset + 40]
        stats["avg_track_slowdown"] += observations[offset + 20]
        stats["avg_lateral_slip"] += abs(observations[offset + 15])
        stats["avg_braking_risk"] += max(0.0, 1.0 - observations[offset + 5])
        car_ray_distance = min(observations[offset + 27:offset + 33])
        stats["avg_left_road_clearance"] += observations[offset + 22]
        stats["avg_right_road_clearance"] += observations[offset + 23]
        stats["avg_front_road_clearance"] += observations[offset + 24]
        stats["avg_front_left_road_clearance"] += observations[offset + 25]
        stats["avg_front_right_road_clearance"] += observations[offset + 26]
        if observations[offset + 5] < 0.18:
            stats["lookahead_blocked_steps"] += 1
        if observations[offset + 19] > 0.5:
            stats["off_road_steps"] += 1
        if observations[offset + 21] < 0.25:
            stats["near_edge_steps"] += 1
        if observations[offset + 21] < 0.25 and observations[offset + 13] > 0.32:
            stats["near_edge_fast_steps"] += 1
        if observations[offset + 5] < 0.82:
            stats["braking_risk_steps"] += 1
        if car_ray_distance < 1.0:
            stats["near_car_steps"] += 1
        if car_ray_distance < OBS_CLOSE_CAR_THRESHOLD:
            stats["close_car_steps"] += 1
    for agent_index in range(min(controlled_agents, len(result.routeProgressDeltas))):
        stats["progress_total"] += float(result.routeProgressDeltas[agent_index])


def run_episode(
    args,
    environment,
    policy,
    java_float_array,
    ai_control_decision,
    scratch_size,
    env_observation_size,
    controlled_agents,
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
    reward_breakdown_totals = [0.0 for _ in range(len(reward_breakdown_names) * controlled_agents)]
    last_raw_sign = 0
    last_effective_sign = 0
    trace_handle, trace_writer = open_trace(
        args.trace_dir,
        map_id,
        episode,
        [str(value) for value in result.debugTraceNames],
    )

    try:
        while not result.episodeDone and steps < steps_limit:
            flat_observations = [float(value) for value in result.observations]
            action_values = []
            throttle = 0.0
            turn = 0.0
            for agent_index in range(controlled_agents):
                offset = agent_index * env_observation_size
                observation = java_float_array(
                    flat_observations[offset : offset + env_observation_size]
                )
                scratch_a = java_float_array(scratch_size)
                scratch_b = java_float_array(scratch_size)
                decision = policy.computeAction(
                    observation,
                    scratch_a,
                    scratch_b,
                    ai_control_decision(),
                )
                agent_throttle = float(decision.throttle)
                agent_turn = float(decision.turn)
                action_values.extend([agent_throttle, agent_turn])
                if agent_index == 0:
                    throttle = agent_throttle
                    turn = agent_turn
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

            result = environment.step(java_float_array(action_values))
            step_reward = sum(float(value) for value in result.rewards)
            trace_step(trace_writer, steps + 1, result, step_reward, throttle, turn)
            update_observation_stats(stats, result, controlled_agents, env_observation_size)

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
    stats["successes"] = 1 if int(result.winnerAgentIndex) >= 0 else 0
    stats["steps"] = steps
    stats["reward"] = reward
    if stats["successes"] > 0:
        stats["goal_time_seconds"] = (
            int(result.actionStep) * max(1, args.action_repeat) * PHYSICS_STEP_SECONDS
        )
        stats["goal_time_count"] = 1
    stats["actions"] = steps
    stats["raw_flips"] = raw_flips
    stats["raw_nonzero_pairs"] = raw_nonzero_pairs
    stats["effective_flips"] = effective_flips
    stats["effective_nonzero_pairs"] = effective_nonzero_pairs
    stats["reverse"] = reverse_actions
    stats["effective_reverse"] = effective_reverse_actions
    stats["small"] = small_actions
    stats["effective_small"] = effective_small_actions
    if len(result.routeTargetsReached) > 0:
        stats["targets"] = sum(
            int(result.routeTargetsReached[index])
            for index in range(min(controlled_agents, len(result.routeTargetsReached)))
        )
    for agent_index in range(controlled_agents):
        breakdown_offset = agent_index * len(reward_breakdown_names)
        for bucket, name in enumerate(reward_breakdown_names):
            index = breakdown_offset + bucket
            if index < len(reward_breakdown_totals):
                stats[name] = stats.get(name, 0.0) + reward_breakdown_totals[index]

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
            f"targets={stats['targets']} "
            f"success={stats['successes']}",
            flush=True,
        )

    return map_id, stats


def summary_metrics(stats, episodes_override: int = None):
    episodes = episodes_override if episodes_override is not None else max(1, stats["episodes"])
    actions = max(1, stats["actions"])
    observation_samples = max(1, stats["observation_samples"])
    goal_time_count = int(stats.get("goal_time_count", 0))
    return {
        "success_rate": stats["successes"] / episodes,
        "avg_steps": stats["steps"] / episodes,
        "avg_reward": stats["reward"] / episodes,
        "avg_goal_time_s": (
            stats["goal_time_seconds"] / goal_time_count
            if goal_time_count > 0
            else None
        ),
        "avg_targets": stats["targets"] / episodes,
        "progress_avg": stats["progress_total"] / episodes,
        "raw_flips_per_step": stats["raw_flips"] / actions,
        "effective_flips_per_step": stats["effective_flips"] / actions,
        "effective_reverse_fraction": stats["effective_reverse"] / actions,
        "effective_small_throttle_fraction": stats["effective_small"] / actions,
        "lookahead_blocked_fraction": stats["lookahead_blocked_steps"] / observation_samples,
        "off_road_fraction": stats["off_road_steps"] / observation_samples,
        "near_edge_fraction": stats["near_edge_steps"] / observation_samples,
        "near_edge_fast_fraction": stats["near_edge_fast_steps"] / observation_samples,
        "braking_risk_fraction": stats["braking_risk_steps"] / observation_samples,
        "avg_route_alignment": stats["avg_route_alignment"] / observation_samples,
        "avg_route_lookahead_alignment": stats["avg_route_lookahead_alignment"] / observation_samples,
        "avg_route_curvature": stats["avg_route_curvature"] / observation_samples,
        "avg_route_left_clearance": stats["avg_route_left_clearance"] / observation_samples,
        "avg_route_right_clearance": stats["avg_route_right_clearance"] / observation_samples,
        "avg_route_offset_forward": stats["avg_route_offset_forward"] / observation_samples,
        "avg_route_offset_side": stats["avg_route_offset_side"] / observation_samples,
        "avg_route_offset_distance": stats["avg_route_offset_distance"] / observation_samples,
        "avg_route_lookahead_distance": stats["avg_route_lookahead_distance"] / observation_samples,
        "avg_next_corner_distance": stats["avg_next_corner_distance"] / observation_samples,
        "avg_next_corner_direction": stats["avg_next_corner_direction"] / observation_samples,
        "avg_next_corner_severity": stats["avg_next_corner_severity"] / observation_samples,
        "avg_route_width": stats["avg_route_width"] / observation_samples,
        "avg_recovery_alignment": stats["avg_recovery_alignment"] / observation_samples,
        "avg_track_slowdown": stats["avg_track_slowdown"] / observation_samples,
        "avg_steering_authority": stats["avg_steering_authority"] / observation_samples,
        "avg_lateral_slip": stats["avg_lateral_slip"] / observation_samples,
        "avg_braking_distance": stats["avg_braking_distance"] / observation_samples,
        "avg_braking_risk": stats["avg_braking_risk"] / observation_samples,
        "avg_speed_signal": stats["avg_speed"] / observation_samples,
        "avg_edge_clearance": stats["avg_edge_clearance"] / observation_samples,
        "avg_left_road_clearance": stats["avg_left_road_clearance"] / observation_samples,
        "avg_right_road_clearance": stats["avg_right_road_clearance"] / observation_samples,
        "avg_front_road_clearance": stats["avg_front_road_clearance"] / observation_samples,
        "avg_front_left_road_clearance":
            stats["avg_front_left_road_clearance"] / observation_samples,
        "avg_front_right_road_clearance":
            stats["avg_front_right_road_clearance"] / observation_samples,
        "near_car_fraction": stats["near_car_steps"] / observation_samples,
        "close_car_fraction": stats["close_car_steps"] / observation_samples,
    }


def evaluation_score(stats, episodes_override: int = None) -> float:
    metrics = summary_metrics(stats, episodes_override)
    # Reward remains the main signal. Walls are survivable now, so evaluation
    # only applies a small smoothness cost for repeated wall contact.
    return (
        metrics["avg_reward"]
        + metrics["avg_targets"] * 35.0
        + metrics["success_rate"] * 80.0
        + metrics["progress_avg"] * 0.3
        + metrics["avg_route_alignment"] * 10.0
        + metrics["avg_route_lookahead_alignment"] * 5.0
        - metrics["off_road_fraction"] * 12.0
        - metrics["effective_flips_per_step"] * 35.0
    )


def print_summary(label: str, stats, episodes_override: int = None):
    episodes = episodes_override if episodes_override is not None else max(1, stats["episodes"])
    metrics = summary_metrics(stats, episodes_override)
    print(
        f"{label} "
        f"successes={stats['successes']}/{stats['episodes']} "
        f"avg_steps={metrics['avg_steps']:.1f} "
        f"avg_reward={metrics['avg_reward']:.3f} "
        f"avg_goal_time_s={format_optional_number(metrics['avg_goal_time_s'])} "
        f"avg_targets={metrics['avg_targets']:.3f} "
        f"progress_avg={metrics['progress_avg']:.3f} "
        f"raw_flips_per_step={metrics['raw_flips_per_step']:.3f} "
        f"effective_flips_per_step={metrics['effective_flips_per_step']:.3f} "
        f"effective_reverse_fraction={metrics['effective_reverse_fraction']:.3f} "
        f"effective_small_throttle_fraction={metrics['effective_small_throttle_fraction']:.3f} "
        f"lookahead_blocked_fraction={metrics['lookahead_blocked_fraction']:.3f} "
        f"off_road_fraction={metrics['off_road_fraction']:.3f} "
        f"near_edge_fraction={metrics['near_edge_fraction']:.3f} "
        f"near_edge_fast_fraction={metrics['near_edge_fast_fraction']:.3f} "
        f"braking_risk_fraction={metrics['braking_risk_fraction']:.3f} "
        f"avg_route_alignment={metrics['avg_route_alignment']:.3f} "
        f"avg_route_lookahead_alignment={metrics['avg_route_lookahead_alignment']:.3f} "
        f"avg_route_curvature={metrics['avg_route_curvature']:.3f} "
        f"avg_recovery_alignment={metrics['avg_recovery_alignment']:.3f} "
        f"avg_track_slowdown={metrics['avg_track_slowdown']:.3f} "
        f"avg_steering_authority={metrics['avg_steering_authority']:.3f} "
        f"avg_lateral_slip={metrics['avg_lateral_slip']:.3f} "
        f"avg_braking_distance={metrics['avg_braking_distance']:.3f} "
        f"avg_braking_risk={metrics['avg_braking_risk']:.3f} "
        f"avg_speed_signal={metrics['avg_speed_signal']:.3f} "
        f"avg_edge_clearance={metrics['avg_edge_clearance']:.3f} "
        f"avg_left_road_clearance={metrics['avg_left_road_clearance']:.3f} "
        f"avg_right_road_clearance={metrics['avg_right_road_clearance']:.3f} "
        f"avg_front_road_clearance={metrics['avg_front_road_clearance']:.3f} "
        f"avg_front_left_road_clearance={metrics['avg_front_left_road_clearance']:.3f} "
        f"avg_front_right_road_clearance={metrics['avg_front_right_road_clearance']:.3f} "
        f"near_car_fraction={metrics['near_car_fraction']:.3f} "
        f"close_car_fraction={metrics['close_car_fraction']:.3f} "
        + " ".join(
            f"{name}={stats[name] / episodes:.3f}"
            for name in (
                "route_progress",
                "step_cost",
                "off_road",
                "steering",
                "reverse_speed",
                "car_push",
                "route_alignment",
            )
            if name in stats
        ),
        flush=True,
    )


def format_table_number(value: float, digits: int = 3) -> str:
    return f"{value:.{digits}f}"


def format_optional_number(value, digits: int = 3) -> str:
    if value is None:
        return "-"
    return f"{value:.{digits}f}"


def success_count(stats) -> str:
    return f"{int(stats['successes'])}/{int(stats['episodes'])}"


def stats_scope_rows(total_stats, per_map):
    rows = [("overall", total_stats)]
    rows.extend((map_id, per_map[map_id]) for map_id in sorted(per_map))
    return rows


def print_table(title: str, headers, rows, right_aligned=None) -> None:
    right_aligned = set() if right_aligned is None else set(right_aligned)
    widths = [len(str(header)) for header in headers]
    string_rows = [[str(value) for value in row] for row in rows]
    for row in string_rows:
        for index, cell in enumerate(row):
            widths[index] = max(widths[index], len(cell))

    def format_row(cells) -> str:
        formatted = []
        for index, cell in enumerate(cells):
            if index in right_aligned:
                formatted.append(str(cell).rjust(widths[index]))
            else:
                formatted.append(str(cell).ljust(widths[index]))
        return "| " + " | ".join(formatted) + " |"

    separators = []
    for index, width in enumerate(widths):
        marker_width = max(3, width)
        if index in right_aligned:
            separators.append("-" * (marker_width - 1) + ":")
        else:
            separators.append("-" * marker_width)

    print(title, flush=True)
    print(format_row([str(header) for header in headers]), flush=True)
    print("| " + " | ".join(separators) + " |", flush=True)
    for row in string_rows:
        print(format_row(row), flush=True)


def print_evaluation_tables(total_stats, per_map) -> None:
    overview_rows = []
    driving_rows = []
    reward_rows = []
    for scope, stats in stats_scope_rows(total_stats, per_map):
        metrics = summary_metrics(stats)
        score = evaluation_score(stats)
        overview_rows.append(
            [
                scope,
                success_count(stats),
                format_table_number(metrics["avg_steps"], 1),
                format_table_number(metrics["avg_reward"]),
                format_table_number(score),
                format_optional_number(metrics["avg_goal_time_s"], 2),
                format_table_number(metrics["avg_targets"]),
                format_table_number(metrics["off_road_fraction"]),
                format_table_number(metrics["effective_reverse_fraction"]),
                format_table_number(metrics["near_car_fraction"]),
            ]
        )
        driving_rows.append(
            [
                scope,
                format_table_number(metrics["progress_avg"]),
                format_table_number(metrics["avg_route_alignment"]),
                format_table_number(metrics["avg_route_lookahead_alignment"]),
                format_table_number(metrics["avg_route_curvature"]),
                format_table_number(metrics["avg_route_offset_distance"]),
                format_table_number(metrics["avg_route_lookahead_distance"]),
                format_table_number(metrics["avg_next_corner_distance"]),
                format_table_number(metrics["avg_next_corner_severity"]),
                format_table_number(metrics["avg_route_width"]),
                format_table_number(metrics["avg_speed_signal"]),
                format_table_number(metrics["avg_edge_clearance"]),
                format_table_number(metrics["avg_front_road_clearance"]),
                format_table_number(metrics["avg_left_road_clearance"]),
                format_table_number(metrics["avg_right_road_clearance"]),
                format_table_number(metrics["near_edge_fraction"]),
                format_table_number(metrics["near_edge_fast_fraction"]),
                format_table_number(metrics["braking_risk_fraction"]),
                format_table_number(metrics["avg_lateral_slip"]),
            ]
        )
        episodes = max(1, stats["episodes"])
        reward_rows.append(
            [
                scope,
                *[
                    format_table_number(stats[name] / episodes)
                    for name in REWARD_BUCKETS
                ],
            ]
        )

    print_table(
        "evaluation_overview",
        [
            "scope",
            "success",
            "steps",
            "reward",
            "score",
            "goal_s",
            "targets",
            "off_road",
            "reverse",
            "near_car",
        ],
        overview_rows,
        right_aligned=set(range(1, 10)),
    )
    print_table(
        "evaluation_driving",
        [
            "scope",
            "progress",
            "route_align",
            "lookahead",
            "curve",
            "route_off",
            "look_dist",
            "corner_d",
            "corner_s",
            "route_w",
            "speed",
            "edge_clear",
            "road_front",
            "road_left",
            "road_right",
            "near_edge",
            "edge_fast",
            "brake_risk",
            "lat_slip",
        ],
        driving_rows,
        right_aligned=set(range(1, 19)),
    )
    print_table(
        "evaluation_rewards",
        [
            "scope",
            "route_progress",
            "step",
            "off_road",
            "steering",
            "reverse",
            "car_push",
            "align",
        ],
        reward_rows,
        right_aligned=set(range(1, 8)),
    )


def print_evaluation_score(stats):
    metrics = summary_metrics(stats)
    score = evaluation_score(stats)
    print(
        f"evaluation_score={score:.3f} "
        f"avg_reward={metrics['avg_reward']:.3f} "
        f"avg_steps={metrics['avg_steps']:.1f} "
        f"avg_goal_time_s={format_optional_number(metrics['avg_goal_time_s'])} "
        f"success_rate={metrics['success_rate']:.3f} "
        f"avg_targets={metrics['avg_targets']:.3f} "
        f"lookahead_blocked_fraction={metrics['lookahead_blocked_fraction']:.3f} "
        f"off_road_fraction={metrics['off_road_fraction']:.3f} "
        f"avg_left_road_clearance={metrics['avg_left_road_clearance']:.3f} "
        f"avg_right_road_clearance={metrics['avg_right_road_clearance']:.3f} "
        f"avg_front_road_clearance={metrics['avg_front_road_clearance']:.3f} "
        f"avg_route_curvature={metrics['avg_route_curvature']:.3f} "
        f"avg_route_offset_distance={metrics['avg_route_offset_distance']:.3f} "
        f"avg_route_lookahead_distance={metrics['avg_route_lookahead_distance']:.3f} "
        f"avg_next_corner_distance={metrics['avg_next_corner_distance']:.3f} "
        f"avg_next_corner_direction={metrics['avg_next_corner_direction']:.3f} "
        f"avg_next_corner_severity={metrics['avg_next_corner_severity']:.3f} "
        f"avg_route_width={metrics['avg_route_width']:.3f} "
        f"near_car_fraction={metrics['near_car_fraction']:.3f} "
        f"close_car_fraction={metrics['close_car_fraction']:.3f} "
        f"near_edge_fast_fraction={metrics['near_edge_fast_fraction']:.3f} "
        f"braking_risk_fraction={metrics['braking_risk_fraction']:.3f}",
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
    controlled_agents = args.controlled_agents if args.controlled_agents is not None else 1
    if controlled_agents < 1:
        raise ValueError("--controlled-agents must be at least 1")
    field_size = max(field_size, controlled_agents)
    steps_limit = args.steps if args.steps is not None else 6400
    env_observation_size = int(ratass_game.RL_OBSERVATION_SIZE)
    policy_observation_size = int(policy.getObservationSize())
    if policy_observation_size != env_observation_size:
        raise ValueError(
            f"policy observation size {policy_observation_size} != env {env_observation_size}; "
            "retrain the policy for the current observation contract"
        )
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
            controlled_agents,
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
                    env_observation_size,
                    controlled_agents,
                    steps_limit,
                    episode_counter,
                )
                add_stats(total_stats, episode_stats)
                add_stats(per_map[map_id], episode_stats)
        finally:
            environment.close()

    print_evaluation_tables(total_stats, per_map)
    print_evaluation_score(total_stats)


if __name__ == "__main__":
    main()
