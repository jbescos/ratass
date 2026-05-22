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
OBS_CLOSE_CAR_THRESHOLD = 0.18


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
        "--target-deadline-seconds",
        type=float,
        default=0.0,
        help="seconds a learner can stay outside a target circle before dying; 0 uses the Java default",
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
        "wall_contact_steps": 0,
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
        "observation_samples": 0,
        "near_car_steps": 0,
        "close_car_steps": 0,
        "nearest_car_inside_steps": 0,
        "avg_nearest_car_distance": 0.0,
        "avg_nearest_car_approach": 0.0,
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
        .withMaxActionSteps(steps_limit)
        .withTargetDeadlineSeconds(args.target_deadline_seconds)
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
            "wall_clearance",
            "wall_contact",
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
            "target_time_remaining",
            "wall_recovery_speed",
            "wall_inward_speed",
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
            "car_ray_forward",
            "car_ray_front_left",
            "car_ray_front_right",
            "car_ray_left",
            "car_ray_right",
            "car_ray_rear",
            "nearest_car_forward",
            "nearest_car_side",
            "nearest_car_distance",
            "nearest_car_approach",
            "nearest_car_inside_circle",
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
            f"{observations[19]:.6f}",
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
            f"{observations[51]:.6f}",
            f"{observations[52]:.6f}",
            f"{observations[53]:.6f}",
            f"{observations[54]:.6f}",
            f"{observations[55]:.6f}",
            f"{observations[56]:.6f}",
            f"{observations[57]:.6f}",
            f"{observations[58]:.6f}",
            f"{observations[59]:.6f}",
            f"{observations[60]:.6f}",
            f"{observations[61]:.6f}",
        ]
    )


def update_observation_stats(stats, result, controlled_agents: int, observation_size: int):
    observations = [float(value) for value in result.observations]
    for agent_index in range(controlled_agents):
        offset = agent_index * observation_size
        if offset + observation_size > len(observations):
            break
        stats["observation_samples"] += 1
        stats["avg_speed"] += observations[offset + 17]
        stats["avg_edge_clearance"] += observations[offset + 18]
        stats["avg_target_distance"] += observations[offset + 3]
        stats["avg_route_alignment"] += observations[offset + 7]
        stats["avg_route_lookahead_alignment"] += observations[offset + 45]
        stats["avg_recovery_alignment"] += observations[offset + 48]
        stats["avg_hold_progress"] += observations[offset + 12]
        stats["avg_recovery_speed"] += observations[offset + 23]
        stats["avg_unsafe_recovery"] += observations[offset + 24]
        stats["avg_nearest_car_distance"] += observations[offset + 59]
        stats["avg_nearest_car_approach"] += observations[offset + 60]
        if observations[offset + 11] > 0.5:
            stats["inside_steps"] += 1
        if observations[offset + 19] > 0.5:
            stats["wall_contact_steps"] += 1
        if observations[offset + 18] < 0.25:
            stats["near_edge_steps"] += 1
        if observations[offset + 18] < 0.25 and observations[offset + 17] > 0.32:
            stats["near_edge_fast_steps"] += 1
        if observations[offset + 24] > 0.18:
            stats["unsafe_recovery_steps"] += 1
        if observations[offset + 59] < 1.0:
            stats["near_car_steps"] += 1
        if observations[offset + 59] < OBS_CLOSE_CAR_THRESHOLD:
            stats["close_car_steps"] += 1
        if observations[offset + 61] > 0.5:
            stats["nearest_car_inside_steps"] += 1
    for agent_index in range(min(controlled_agents, len(result.progressTowardTarget))):
        stats["progress_total"] += float(result.progressTowardTarget[agent_index])


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
    trace_handle, trace_writer = open_trace(args.trace_dir, map_id, episode)

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
        stats["goals"] = sum(
            int(result.goalsReached[index])
            for index in range(min(controlled_agents, len(result.goalsReached)))
        )
    if len(result.fallDeaths) > 0:
        stats["falls"] = sum(
            int(result.fallDeaths[index])
            for index in range(min(controlled_agents, len(result.fallDeaths)))
        )
    if len(result.edgeRiskEvents) > 0:
        stats["edge_risk_events"] = sum(
            int(result.edgeRiskEvents[index])
            for index in range(min(controlled_agents, len(result.edgeRiskEvents)))
        )
    if len(result.insideTime) > 0:
        stats["inside_time"] = sum(
            float(result.insideTime[index])
            for index in range(min(controlled_agents, len(result.insideTime)))
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
    observation_samples = max(1, stats["observation_samples"])
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
        "inside_fraction": stats["inside_steps"] / observation_samples,
        "wall_contact_fraction": stats["wall_contact_steps"] / observation_samples,
        "near_edge_fraction": stats["near_edge_steps"] / observation_samples,
        "near_edge_fast_fraction": stats["near_edge_fast_steps"] / observation_samples,
        "unsafe_recovery_fraction": stats["unsafe_recovery_steps"] / observation_samples,
        "avg_target_distance": stats["avg_target_distance"] / observation_samples,
        "avg_route_alignment": stats["avg_route_alignment"] / observation_samples,
        "avg_route_lookahead_alignment": stats["avg_route_lookahead_alignment"] / observation_samples,
        "avg_recovery_alignment": stats["avg_recovery_alignment"] / observation_samples,
        "avg_hold_progress": stats["avg_hold_progress"] / observation_samples,
        "avg_recovery_speed": stats["avg_recovery_speed"] / observation_samples,
        "avg_unsafe_recovery": stats["avg_unsafe_recovery"] / observation_samples,
        "avg_speed_signal": stats["avg_speed"] / observation_samples,
        "avg_edge_clearance": stats["avg_edge_clearance"] / observation_samples,
        "near_car_fraction": stats["near_car_steps"] / observation_samples,
        "close_car_fraction": stats["close_car_steps"] / observation_samples,
        "nearest_car_inside_fraction": stats["nearest_car_inside_steps"] / observation_samples,
        "avg_nearest_car_distance": stats["avg_nearest_car_distance"] / observation_samples,
        "avg_nearest_car_approach": stats["avg_nearest_car_approach"] / observation_samples,
    }


def evaluation_score(stats, episodes_override: int = None) -> float:
    metrics = summary_metrics(stats, episodes_override)
    # Reward remains the main signal. Walls are survivable now, so evaluation
    # only applies a small smoothness cost for repeated wall contact.
    return (
        metrics["avg_reward"]
        + metrics["avg_goals"] * 35.0
        + metrics["success_rate"] * 80.0
        + metrics["inside_fraction"] * 35.0
        + metrics["inside_time_avg"] * 4.0
        + metrics["progress_avg"] * 4.0
        + metrics["avg_route_alignment"] * 10.0
        + metrics["avg_route_lookahead_alignment"] * 5.0
        - metrics["wall_contact_fraction"] * 12.0
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
        f"death_rate={metrics['fall_rate']:.3f} "
        f"fall_rate={metrics['fall_rate']:.3f} "
        f"inside_time_avg={metrics['inside_time_avg']:.3f} "
        f"progress_avg={metrics['progress_avg']:.3f} "
        f"edge_risk_events_per_episode={metrics['edge_risk_events_per_episode']:.3f} "
        f"raw_flips_per_step={metrics['raw_flips_per_step']:.3f} "
        f"effective_flips_per_step={metrics['effective_flips_per_step']:.3f} "
        f"effective_reverse_fraction={metrics['effective_reverse_fraction']:.3f} "
        f"effective_small_throttle_fraction={metrics['effective_small_throttle_fraction']:.3f} "
        f"inside_fraction={metrics['inside_fraction']:.3f} "
        f"wall_contact_fraction={metrics['wall_contact_fraction']:.3f} "
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
        f"near_car_fraction={metrics['near_car_fraction']:.3f} "
        f"close_car_fraction={metrics['close_car_fraction']:.3f} "
        f"nearest_car_inside_fraction={metrics['nearest_car_inside_fraction']:.3f} "
        f"avg_nearest_car_distance={metrics['avg_nearest_car_distance']:.3f} "
        f"avg_nearest_car_approach={metrics['avg_nearest_car_approach']:.3f} "
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
        f"death_rate={metrics['fall_rate']:.3f} "
        f"fall_rate={metrics['fall_rate']:.3f} "
        f"inside_fraction={metrics['inside_fraction']:.3f} "
        f"wall_contact_fraction={metrics['wall_contact_fraction']:.3f} "
        f"near_car_fraction={metrics['near_car_fraction']:.3f} "
        f"close_car_fraction={metrics['close_car_fraction']:.3f} "
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
    controlled_agents = args.controlled_agents if args.controlled_agents is not None else 1
    if controlled_agents < 1:
        raise ValueError("--controlled-agents must be at least 1")
    field_size = max(field_size, controlled_agents)
    steps_limit = args.steps if args.steps is not None else 1350
    env_observation_size = int(ratass_game.RL_OBSERVATION_SIZE)
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

    print_summary("summary", total_stats)
    print_evaluation_score(total_stats)
    if args.per_map or args.episodes_per_map > 0:
        for map_id in sorted(per_map):
            print_summary(f"map_summary map={map_id}", per_map[map_id])


if __name__ == "__main__":
    main()
