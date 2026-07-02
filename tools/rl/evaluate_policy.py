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
REWARD_BUCKETS = (
    "route_progress",
    "step_cost",
    "off_road",
    "steering",
    "reverse_speed",
    "car_push",
    "route_alignment",
    "no_progress",
    "off_road_recovery",
    "off_road_failure",
)


def start_jvm(jar_path: Path) -> None:
    if not jpype.isJVMStarted():
        max_heap = os.environ.get("RATASS_RL_JVM_MAX_HEAP", "512m").strip()
        jvm_args = ["-Xrs"]
        if max_heap:
            jvm_args.append(f"-Xmx{max_heap}")
        jpype.startJVM(*jvm_args, classpath=[str(jar_path)])
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
        "--no-progress-max-action-steps",
        type=int,
        default=600,
    )
    parser.add_argument(
        "--off-road-failure-max-action-steps",
        type=int,
        default=45,
    )
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
    parser.add_argument("--reward-step-penalty", type=float, default=0.006)
    parser.add_argument("--reward-progress", type=float, default=0.25)
    parser.add_argument("--reward-route-alignment", type=float, default=0.0)
    parser.add_argument("--reward-steering-penalty", type=float, default=0.010)
    parser.add_argument("--reward-reverse-free-epsilon", type=float, default=0.20)
    parser.add_argument("--reward-reverse-penalty-per-unit", type=float, default=0.08)
    parser.add_argument("--reward-reverse-max-penalty", type=float, default=0.90)
    parser.add_argument("--reward-car-push-penalty", type=float, default=3.0)
    parser.add_argument("--reward-car-push-max-step-penalty", type=float, default=8.0)
    parser.add_argument("--reward-off-road-penalty", type=float, default=0.80)
    parser.add_argument("--reward-off-road-distance-penalty", type=float, default=0.22)
    parser.add_argument("--reward-off-road-max-penalty", type=float, default=5.0)
    parser.add_argument("--reward-no-progress-penalty", type=float, default=50.0)
    parser.add_argument("--reward-off-road-recovery", type=float, default=4.0)
    parser.add_argument("--reward-off-road-failure-penalty", type=float, default=50.0)
    return parser.parse_args()


def available_maps():
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    maps = arena_maps.createDefaultSet()
    return [maps.get(index) for index in range(int(maps.size))]


def select_map(ratass_game, map_id: str):
    if not map_id:
        return None
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    maps = arena_maps.createSelectedSet(map_id)
    return maps.get(0)


def select_maps(ratass_game, map_ids: str):
    ids = [map_id.strip() for map_id in map_ids.split(",") if map_id.strip()]
    if not ids:
        return []
    arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
    maps = arena_maps.createSelectedSet(",".join(ids))
    maps_by_id = {
        str(maps.get(index).getId()): maps.get(index)
        for index in range(int(maps.size))
    }
    return [maps_by_id[map_id] for map_id in ids]


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
        "no_progress": 0.0,
        "off_road_recovery": 0.0,
        "off_road_failure": 0.0,
        "lookahead_blocked_steps": 0,
        "off_road_steps": 0,
        "near_edge_steps": 0,
        "near_edge_fast_steps": 0,
        "braking_risk_steps": 0,
        "avg_route_alignment": 0.0,
        "avg_target_alignment": 0.0,
        "avg_route_curvature": 0.0,
        "avg_target_curvature": 0.0,
        "avg_route_left_clearance": 0.0,
        "avg_route_right_clearance": 0.0,
        "avg_lateral_slip": 0.0,
        "avg_brake_demand": 0.0,
        "avg_edge_clearance": 0.0,
        "avg_left_road_clearance": 0.0,
        "avg_right_road_clearance": 0.0,
        "avg_front_road_clearance": 0.0,
        "avg_front_left_road_clearance": 0.0,
        "avg_front_right_road_clearance": 0.0,
        "avg_trajectory_clearance": 0.0,
        "avg_target_left_route_clearance": 0.0,
        "avg_target_right_route_clearance": 0.0,
        "observation_samples": 0,
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
        .withNoProgressMaxActionSteps(args.no_progress_max_action_steps)
        .withOffRoadFailureMaxActionSteps(args.off_road_failure_max_action_steps)
        .withRouteTargets(args.route_targets)
        .withRouteTargetFraction(float(args.route_target_fraction))
        .withRaceMode(True)
        .withRandomRaceSpawns(args.random_race_spawns)
        .withDebugTraceEnabled(bool(args.trace_dir))
        .withSeed(args.seed)
        .withStepPenalty(float(args.reward_step_penalty))
        .withProgressReward(float(args.reward_progress))
        .withRouteAlignmentReward(float(args.reward_route_alignment))
        .withSteeringPenalty(float(args.reward_steering_penalty))
        .withReverseSpeedPenalty(
            float(args.reward_reverse_free_epsilon),
            float(args.reward_reverse_penalty_per_unit),
            float(args.reward_reverse_max_penalty),
        )
        .withCarPushPenalty(
            float(args.reward_car_push_penalty),
            float(args.reward_car_push_max_step_penalty),
        )
        .withOffRoadPenalty(
            float(args.reward_off_road_penalty),
            float(args.reward_off_road_distance_penalty),
            float(args.reward_off_road_max_penalty),
        )
        .withNoProgressPenalty(float(args.reward_no_progress_penalty))
        .withOffRoadRecoveryReward(float(args.reward_off_road_recovery))
        .withOffRoadFailurePenalty(float(args.reward_off_road_failure_penalty))
    )
    if arena_map is not None:
        config.addMap(arena_map)
    return config


def trace_path(trace_dir: str, map_id: str, episode: int) -> Path:
    directory = Path(trace_dir)
    directory.mkdir(parents=True, exist_ok=True)
    return directory / f"{map_id}-episode{episode:03d}.csv"


def open_trace(
    trace_dir: str,
    map_id: str,
    episode: int,
    observation_names,
    debug_trace_names,
):
    if not trace_dir:
        return None, None
    handle = trace_path(trace_dir, map_id, episode).open("w", encoding="utf-8", newline="")
    writer = csv.writer(handle)
    observation_headers = [f"observation_{name}" for name in observation_names]
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
            "no_progress",
            "off_road_recovery",
            "off_road_failure",
            "throttle",
            "turn",
            "effective_throttle",
            "effective_turn",
        ]
        + observation_headers
        + debug_headers
    )
    return handle, writer


def trace_step(writer, step, result, reward, throttle, turn, observation_size):
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
            f"{breakdown[7]:.6f}" if len(breakdown) > 7 else "0.000000",
            f"{breakdown[8]:.6f}" if len(breakdown) > 8 else "0.000000",
            f"{breakdown[9]:.6f}" if len(breakdown) > 9 else "0.000000",
            f"{throttle:.6f}",
            f"{turn:.6f}",
            f"{effective_throttle:.6f}",
            f"{effective_turn:.6f}",
        ]
        + [
            f"{observations[index]:.6f}" if index < len(observations) else "0.000000"
            for index in range(observation_size)
        ]
        + [
            f"{debug_trace[index]:.6f}" if index < len(debug_trace) else "0.000000"
            for index in range(len(debug_trace_names))
        ]
    )


def update_observation_stats(
    stats,
    result,
    controlled_agents: int,
    observation_size: int,
    observation_indices,
):
    observations = [float(value) for value in result.observations]

    def value(offset, name, default=0.0):
        index = observation_indices.get(name)
        if index is None or offset + index >= len(observations):
            return default
        return observations[offset + index]

    for agent_index in range(controlled_agents):
        offset = agent_index * observation_size
        if offset + observation_size > len(observations):
            break
        stats["observation_samples"] += 1
        forward_speed = value(offset, "forward_speed")
        trajectory_clearance = value(offset, "trajectory_clear", 1.0)
        left_road_clearance = value(offset, "road_left", 1.0)
        right_road_clearance = value(offset, "road_right", 1.0)
        front_road_clearance = value(offset, "road_front", 1.0)
        left_margin = value(offset, "route_left_margin", 1.0)
        right_margin = value(offset, "route_right_margin", 1.0)
        edge_clearance = min(left_road_clearance, right_road_clearance)
        stats["avg_edge_clearance"] += edge_clearance
        stats["avg_route_alignment"] += value(offset, "route_fwd")
        stats["avg_target_alignment"] += value(offset, "target_fwd")
        stats["avg_route_curvature"] += abs(value(offset, "route_curvature"))
        stats["avg_target_curvature"] += abs(value(offset, "target_curvature"))
        stats["avg_route_left_clearance"] += left_margin
        stats["avg_route_right_clearance"] += right_margin
        stats["avg_lateral_slip"] += abs(value(offset, "slip_angle"))
        brake_demand = value(offset, "brake_demand")
        stats["avg_brake_demand"] += brake_demand
        stats["avg_left_road_clearance"] += left_road_clearance
        stats["avg_right_road_clearance"] += right_road_clearance
        stats["avg_front_road_clearance"] += front_road_clearance
        stats["avg_front_left_road_clearance"] += value(offset, "road_front_left", 1.0)
        stats["avg_front_right_road_clearance"] += value(offset, "road_front_right", 1.0)
        stats["avg_trajectory_clearance"] += trajectory_clearance
        stats["avg_target_left_route_clearance"] += value(
            offset,
            "target_route_left_clear",
        )
        stats["avg_target_right_route_clearance"] += value(
            offset,
            "target_route_right_clear",
        )
        if front_road_clearance < 0.18:
            stats["lookahead_blocked_steps"] += 1
        if value(offset, "off_road_dist") > 0.0:
            stats["off_road_steps"] += 1
        if edge_clearance < 0.15:
            stats["near_edge_steps"] += 1
        if edge_clearance < 0.15 and forward_speed > 0.32:
            stats["near_edge_fast_steps"] += 1
        if brake_demand > 0.05:
            stats["braking_risk_steps"] += 1
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
    observation_names = [str(name) for name in environment.getObservationNames()]
    if len(observation_names) != env_observation_size:
        raise ValueError(
            f"environment returned {len(observation_names)} observation names "
            f"for {env_observation_size} values"
        )
    observation_indices = {name: index for index, name in enumerate(observation_names)}
    reward_breakdown_totals = [0.0 for _ in range(len(reward_breakdown_names) * controlled_agents)]
    last_raw_sign = 0
    last_effective_sign = 0
    trace_handle, trace_writer = open_trace(
        args.trace_dir,
        map_id,
        episode,
        observation_names,
        [str(value) for value in result.debugTraceNames],
    )
    observation_buffers = [
        java_float_array(env_observation_size) for _ in range(controlled_agents)
    ]
    scratch_a_buffers = [java_float_array(scratch_size) for _ in range(controlled_agents)]
    scratch_b_buffers = [java_float_array(scratch_size) for _ in range(controlled_agents)]
    decisions = [ai_control_decision() for _ in range(controlled_agents)]
    action_values = java_float_array(controlled_agents * 2)

    try:
        while not result.episodeDone and steps < steps_limit:
            throttle = 0.0
            turn = 0.0
            for agent_index in range(controlled_agents):
                offset = agent_index * env_observation_size
                observation = observation_buffers[agent_index]
                for observation_index in range(env_observation_size):
                    observation[observation_index] = result.observations[
                        offset + observation_index
                    ]
                decision = policy.computeAction(
                    observation,
                    scratch_a_buffers[agent_index],
                    scratch_b_buffers[agent_index],
                    decisions[agent_index],
                )
                agent_throttle = float(decision.throttle)
                agent_turn = float(decision.turn)
                action_values[agent_index * 2] = agent_throttle
                action_values[agent_index * 2 + 1] = agent_turn
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

            result = environment.step(action_values)
            step_reward = sum(float(value) for value in result.rewards)
            trace_step(
                trace_writer,
                steps + 1,
                result,
                step_reward,
                throttle,
                turn,
                env_observation_size,
            )
            update_observation_stats(
                stats,
                result,
                controlled_agents,
                env_observation_size,
                observation_indices,
            )

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
        "avg_target_alignment": stats["avg_target_alignment"] / observation_samples,
        "avg_route_curvature": stats["avg_route_curvature"] / observation_samples,
        "avg_target_curvature": stats["avg_target_curvature"] / observation_samples,
        "avg_route_left_clearance": stats["avg_route_left_clearance"] / observation_samples,
        "avg_route_right_clearance": stats["avg_route_right_clearance"] / observation_samples,
        "avg_lateral_slip": stats["avg_lateral_slip"] / observation_samples,
        "avg_brake_demand": stats["avg_brake_demand"] / observation_samples,
        "avg_edge_clearance": stats["avg_edge_clearance"] / observation_samples,
        "avg_left_road_clearance": stats["avg_left_road_clearance"] / observation_samples,
        "avg_right_road_clearance": stats["avg_right_road_clearance"] / observation_samples,
        "avg_front_road_clearance": stats["avg_front_road_clearance"] / observation_samples,
        "avg_front_left_road_clearance":
            stats["avg_front_left_road_clearance"] / observation_samples,
        "avg_front_right_road_clearance":
            stats["avg_front_right_road_clearance"] / observation_samples,
        "avg_trajectory_clearance": stats["avg_trajectory_clearance"] / observation_samples,
        "avg_target_left_route_clearance":
            stats["avg_target_left_route_clearance"] / observation_samples,
        "avg_target_right_route_clearance":
            stats["avg_target_right_route_clearance"] / observation_samples,
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
        + metrics["avg_target_alignment"] * 5.0
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
        f"avg_brake_demand={metrics['avg_brake_demand']:.3f} "
        f"avg_route_alignment={metrics['avg_route_alignment']:.3f} "
        f"avg_target_alignment={metrics['avg_target_alignment']:.3f} "
        f"avg_route_curvature={metrics['avg_route_curvature']:.3f} "
        f"avg_target_curvature={metrics['avg_target_curvature']:.3f} "
        f"avg_lateral_slip={metrics['avg_lateral_slip']:.3f} "
        f"avg_edge_clearance={metrics['avg_edge_clearance']:.3f} "
        f"avg_left_road_clearance={metrics['avg_left_road_clearance']:.3f} "
        f"avg_right_road_clearance={metrics['avg_right_road_clearance']:.3f} "
        f"avg_front_road_clearance={metrics['avg_front_road_clearance']:.3f} "
        f"avg_target_left_route_clearance={metrics['avg_target_left_route_clearance']:.3f} "
        f"avg_target_right_route_clearance={metrics['avg_target_right_route_clearance']:.3f} "
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
                "no_progress",
                "off_road_recovery",
                "off_road_failure",
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
            ]
        )
        driving_rows.append(
            [
                scope,
                format_table_number(metrics["progress_avg"]),
                format_table_number(metrics["avg_route_alignment"]),
                format_table_number(metrics["avg_target_alignment"]),
                format_table_number(metrics["avg_route_curvature"]),
                format_table_number(metrics["avg_target_curvature"]),
                format_table_number(metrics["avg_route_left_clearance"]),
                format_table_number(metrics["avg_route_right_clearance"]),
                format_table_number(metrics["avg_edge_clearance"]),
                format_table_number(metrics["avg_trajectory_clearance"]),
                format_table_number(metrics["avg_front_road_clearance"]),
                format_table_number(metrics["avg_left_road_clearance"]),
                format_table_number(metrics["avg_right_road_clearance"]),
                format_table_number(metrics["avg_front_left_road_clearance"]),
                format_table_number(metrics["avg_front_right_road_clearance"]),
                format_table_number(metrics["near_edge_fraction"]),
                format_table_number(metrics["near_edge_fast_fraction"]),
                format_table_number(metrics["avg_brake_demand"]),
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
        ],
        overview_rows,
        right_aligned=set(range(1, 9)),
    )
    print_table(
        "evaluation_driving",
        [
            "scope",
            "progress",
            "route_align",
            "target_align",
            "curve_now",
            "curve_target",
            "margin_left",
            "margin_right",
            "margin_min",
            "trajectory",
            "road_front",
            "road_left",
            "road_right",
            "road_fl",
            "road_fr",
            "near_edge",
            "edge_fast",
            "brake_d",
            "brake_risk",
            "lat_slip",
        ],
        driving_rows,
        right_aligned=set(range(1, 20)),
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
            "no_progress",
            "off_road_recovery",
            "off_road_failure",
        ],
        reward_rows,
        right_aligned=set(range(1, 11)),
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
        f"avg_route_left_clearance={metrics['avg_route_left_clearance']:.3f} "
        f"avg_route_right_clearance={metrics['avg_route_right_clearance']:.3f} "
        f"near_edge_fast_fraction={metrics['near_edge_fast_fraction']:.3f} "
        f"avg_brake_demand={metrics['avg_brake_demand']:.3f} "
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
