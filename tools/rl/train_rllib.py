#!/usr/bin/env python3
"""Train a shared PPO policy against the Ratass headless Java RL environment.

Build the desktop jar first:

    mvn -pl desktop -am package

Then install the optional Python dependencies from this directory and run:

    python tools/rl/train_rllib.py --iterations 25
"""

from __future__ import annotations

import argparse
import json
import math
import os
import platform
import re
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import gymnasium as gym
import jpype
import numpy as np
from ray.rllib.algorithms.ppo import PPOConfig
from ray.rllib.core.rl_module.default_model_config import DefaultModelConfig
from ray.rllib.env.multi_agent_env import MultiAgentEnv

from checkpoint_candidates import (
    CHECKPOINT_CANDIDATE_WINDOW,
    CheckpointCandidate,
    select_checkpoint_candidate,
    should_capture_checkpoint_candidate,
)
from export_policy import export_policy as export_checkpoint_policy


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = REPO_ROOT / "desktop" / "target" / "ratass-desktop-1.0.jar"
DEFAULT_CHECKPOINT = REPO_ROOT / "rl-checkpoints-race-physics-v1"
OBSERVATION_SIZE = 33
ACTION_SIZE = 2
DEFAULT_CONTROLLED_AGENTS = 1
DEFAULT_FIELD_SIZE = 1
EXPORT_OBJECTIVES = {"race": "race-route-progress-v1"}
INCOMPATIBLE_CHECKPOINT_PATTERNS = (
    "Error(s) in loading state_dict",
    "size mismatch",
    "Missing key(s)",
    "Unexpected key(s)",
)


def _metric_token(value: str) -> str:
    text = str(value).strip()
    if not text:
        return "-"
    return re.sub(r"[^A-Za-z0-9_.:-]+", "_", text)


class RewardSummary:
    def __init__(self) -> None:
        self._episodes: List[Dict] = []

    def mark(self) -> int:
        return len(self._episodes)

    def record_episode(
        self,
        map_id: str,
        map_name: str,
        agent: str,
        reward_total: float,
        bucket_names: List[str],
        bucket_totals: np.ndarray,
        targets_reached: int,
        progress_total: float,
    ) -> None:
        self._episodes.append(
            {
                "map_id": str(map_id),
                "map_name": str(map_name),
                "agent": str(agent),
                "reward_total": float(reward_total),
                "bucket_names": list(bucket_names),
                "bucket_totals": np.asarray(bucket_totals, dtype=np.float64).copy(),
                "targets_reached": int(targets_reached),
                "progress_total": float(progress_total),
            }
        )

    def render_since(self, mark: int, iteration: int) -> List[str]:
        episodes = self._episodes[mark:]
        lines = [
            f"reward_summary iteration={iteration} completed_agent_episodes={len(episodes)}"
        ]
        if not episodes:
            return lines

        grouped: Dict[Tuple[str, str], Dict] = {}
        for episode in episodes:
            key = (episode["map_id"], episode["agent"])
            group = grouped.get(key)
            if group is None:
                group = {
                    "map_id": episode["map_id"],
                    "map_name": episode["map_name"],
                    "agent": episode["agent"],
                    "episodes": 0,
                    "reward_total": 0.0,
                    "targets_reached": 0,
                    "progress_total": 0.0,
                    "bucket_names": episode["bucket_names"],
                    "bucket_totals": np.zeros_like(episode["bucket_totals"]),
                }
                grouped[key] = group

            group["episodes"] += 1
            group["reward_total"] += episode["reward_total"]
            group["targets_reached"] += episode["targets_reached"]
            group["progress_total"] += episode["progress_total"]
            group["bucket_totals"] += episode["bucket_totals"]

        for key in sorted(grouped):
            group = grouped[key]
            episode_count = max(1, int(group["episodes"]))
            reward_avg = group["reward_total"] / episode_count
            fields = [
                f"reward_map iteration={iteration}",
                f"map={_metric_token(group['map_id'])}",
                f"map_name={_metric_token(group['map_name'])}",
                f"car={_metric_token(group['agent'])}",
                f"episodes={group['episodes']}",
                f"targets_avg={group['targets_reached'] / episode_count:.3f}",
                f"progress_avg={group['progress_total'] / episode_count:.3f}",
                f"reward_avg={reward_avg:.3f}",
                f"reward_total={group['reward_total']:.3f}",
            ]
            for name, value in zip(group["bucket_names"], group["bucket_totals"]):
                fields.append(f"{_metric_token(name)}={value / episode_count:.3f}")
            lines.append(" ".join(fields))
        return lines


REWARD_SUMMARY = RewardSummary()


def _parse_java_major(version_output: str) -> Optional[int]:
    match = re.search(r'version "(\d+)(?:\.(\d+))?', version_output)
    if match is None:
        return None

    major = int(match.group(1))
    minor = match.group(2)
    if major == 1 and minor is not None:
        return int(minor)
    return major


def _java_executable_for_jvm(jvm_path: str) -> Optional[Path]:
    executable_name = "java.exe" if os.name == "nt" else "java"
    path = Path(jvm_path)
    candidates = [
        path.parent.parent / executable_name,
        path.parent.parent.parent / "bin" / executable_name,
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    return None


def _java_major_for_jvm(jvm_path: str) -> Optional[int]:
    executable = _java_executable_for_jvm(jvm_path)
    command = [str(executable)] if executable is not None else ["java"]
    try:
        completed = subprocess.run(
            command + ["-version"],
            check=False,
            capture_output=True,
            text=True,
        )
    except OSError:
        return None

    return _parse_java_major(completed.stderr + completed.stdout)


def _validate_windows_jvm(jvm_path: str) -> None:
    if platform.system() != "Windows":
        return
    if os.environ.get("RL_ALLOW_UNTESTED_JAVA") == "1":
        return

    major = _java_major_for_jvm(jvm_path)
    if major is not None and major > 21:
        raise RuntimeError(
            "Windows RL training embeds Java in the Python/Ray process through "
            f"JPype, and Java {major} is not supported here. Use JDK 17 or 21 "
            "for training, or set RL_ALLOW_UNTESTED_JAVA=1 to override."
        )


def _start_jvm(jar_path: Path) -> None:
    if jpype.isJVMStarted():
        return
    if not jar_path.exists():
        raise FileNotFoundError(
            f"{jar_path} does not exist. Run `mvn -pl desktop -am package` first."
        )
    jvm_path = jpype.getDefaultJVMPath()
    _validate_windows_jvm(jvm_path)
    print(f"jvm_path={jvm_path}", flush=True)
    max_heap = os.environ.get("RATASS_RL_JVM_MAX_HEAP", "512m").strip()
    jvm_args = ["-Xrs"]
    if max_heap:
        jvm_args.append(f"-Xmx{max_heap}")
    jpype.startJVM(jvm_path, *jvm_args, classpath=[str(jar_path)])
    _configure_libgdx_files()


def _configure_libgdx_files() -> None:
    """Make packaged assets visible to the Java map loader during JPype training."""
    jpype.JClass("com.badlogic.gdx.utils.GdxNativesLoader").load()
    gdx = jpype.JClass("com.badlogic.gdx.Gdx")
    if gdx.files is None:
        gdx.files = jpype.JClass("com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files")()


class RatassMultiAgentEnv(MultiAgentEnv):
    def __init__(self, env_config: Dict):
        super().__init__()
        jar_path = Path(env_config.get("jar_path", DEFAULT_JAR))
        _start_jvm(jar_path)

        ratass_game = jpype.JClass("com.github.jbescos.RatassGame")
        training_config = ratass_game.RlTrainingConfig()
        training_config.withControlledAgentCount(
            int(env_config.get("controlled_agents", DEFAULT_CONTROLLED_AGENTS))
        )
        training_config.withFieldSize(int(env_config.get("field_size", DEFAULT_FIELD_SIZE)))
        training_config.withActionRepeat(int(env_config.get("action_repeat", 4)))
        training_config.withMaxActionSteps(int(env_config.get("max_action_steps", 19200)))
        training_config.withNoProgressMaxActionSteps(
            int(env_config.get("no_progress_max_action_steps", 600))
        )
        training_config.withOffRoadFailureMaxActionSteps(
            int(env_config.get("off_road_failure_max_action_steps", 45))
        )
        training_config.withRouteTargets(int(env_config.get("route_targets", 6)))
        training_config.withRouteTargetFraction(float(env_config.get("route_target_fraction", 0.0)))
        training_config.withRaceMode(True)
        training_config.withRandomRaceSpawns(bool(env_config.get("random_race_spawns", False)))
        base_seed = int(env_config.get("seed", 1))
        worker_index = int(getattr(env_config, "worker_index", 0) or 0)
        vector_index = int(getattr(env_config, "vector_index", 0) or 0)
        training_config.withSeed(base_seed + worker_index * 1_000_003 + vector_index * 10_007)
        training_config.withStepPenalty(float(env_config.get("reward_step_penalty", 0.006)))
        training_config.withProgressReward(float(env_config.get("reward_progress", 0.25)))
        training_config.withRouteAlignmentReward(
            float(env_config.get("reward_route_alignment", 0.0))
        )
        training_config.withSteeringPenalty(float(env_config.get("reward_steering_penalty", 0.010)))
        training_config.withReverseSpeedPenalty(
            float(env_config.get("reward_reverse_free_epsilon", 0.20)),
            float(env_config.get("reward_reverse_penalty_per_unit", 0.08)),
            float(env_config.get("reward_reverse_max_penalty", 0.90)),
        )
        training_config.withCarPushPenalty(
            float(env_config.get("reward_car_push_penalty", 3.0)),
            float(env_config.get("reward_car_push_max_step_penalty", 8.0)),
        )
        training_config.withOffRoadPenalty(
            float(env_config.get("reward_off_road_penalty", 0.80)),
            float(env_config.get("reward_off_road_distance_penalty", 0.22)),
            float(env_config.get("reward_off_road_max_penalty", 5.0)),
        )
        training_config.withNoProgressPenalty(
            float(env_config.get("reward_no_progress_penalty", 50.0))
        )
        training_config.withOffRoadRecoveryReward(
            float(env_config.get("reward_off_road_recovery", 4.0))
        )
        training_config.withOffRoadFailurePenalty(
            float(env_config.get("reward_off_road_failure_penalty", 50.0))
        )
        self._reward_summary_enabled = bool(env_config.get("reward_summary", True))
        training_config.withRewardBreakdownEnabled(self._reward_summary_enabled)
        training_config.withStepDetailsEnabled(self._reward_summary_enabled)
        self._add_selected_maps(training_config, env_config.get("map_ids", ""))

        self._java_float_array = jpype.JArray(jpype.JFloat)
        self._env = ratass_game.RlTrainingEnvironment(training_config)
        self._agent_count = int(self._env.getControlledAgentCount())
        self._agents = [f"learner_{i}" for i in range(self._agent_count)]
        self._agent_indices = {agent: index for index, agent in enumerate(self._agents)}
        self._java_action_buffer = self._java_float_array(self._agent_count * ACTION_SIZE)
        self.possible_agents = list(self._agents)
        self.agents = list(self._agents)

        observation_space = gym.spaces.Box(
            low=-1.0,
            high=1.0,
            shape=(OBSERVATION_SIZE,),
            dtype=np.float32,
        )
        action_space = gym.spaces.Box(
            low=-1.0,
            high=1.0,
            shape=(ACTION_SIZE,),
            dtype=np.float32,
        )
        self.observation_spaces = {agent: observation_space for agent in self._agents}
        self.action_spaces = {agent: action_space for agent in self._agents}
        self._reward_breakdown_names: List[str] = []
        self._episode_map_id = ""
        self._episode_map_name = ""
        self._episode_reward_totals: Dict[str, float] = {}
        self._episode_bucket_totals: Dict[str, np.ndarray] = {}
        self._episode_progress_totals: Dict[str, float] = {}

    def _add_selected_maps(self, training_config, map_ids: str) -> None:
        selected_ids = [
            map_id.strip()
            for map_id in str(map_ids).split(",")
            if map_id.strip()
        ]
        if not selected_ids:
            return

        arena_maps = jpype.JClass("com.github.jbescos.gameplay.maps.ArenaMaps")
        selected_maps = arena_maps.createSelectedSet(",".join(selected_ids))
        selected_by_id = {
            str(selected_maps.get(index).getId()): selected_maps.get(index)
            for index in range(int(selected_maps.size))
        }
        for map_id in selected_ids:
            training_config.addMap(selected_by_id[map_id])

    def reset(self, *, seed=None, options=None):
        result = self._env.reset()
        self.agents = list(self._agents)
        if self._reward_summary_enabled:
            self._reset_episode_accounting(result)
        return self._observations(result, self.agents), {agent: {} for agent in self.agents}

    def step(self, action_dict):
        current_agents = self._agents
        for action_index in range(self._agent_count * ACTION_SIZE):
            self._java_action_buffer[action_index] = 0.0
        for agent in current_agents:
            index = self._agent_indices[agent]
            action = action_dict.get(agent)
            throttle = 0.0
            turn = 0.0
            if action is not None:
                if len(action) > 0:
                    throttle = float(action[0])
                if len(action) > 1:
                    turn = float(action[1])
            action_offset = index * ACTION_SIZE
            self._java_action_buffer[action_offset] = min(1.0, max(-1.0, throttle))
            self._java_action_buffer[action_offset + 1] = min(1.0, max(-1.0, turn))

        result = self._env.step(self._java_action_buffer)
        rewards = {
            agent: float(result.rewards[index])
            for index, agent in enumerate(current_agents)
        }
        episode_done = bool(result.episodeDone)
        episode_truncated = bool(result.episodeTruncated)
        episode_terminated = bool(result.episodeTerminated)
        terminateds = {
            agent: episode_terminated
            for agent in current_agents
        }
        terminateds["__all__"] = episode_terminated
        truncateds = {
            agent: episode_truncated
            for agent in current_agents
        }
        truncateds["__all__"] = episode_truncated
        reward_breakdown = None
        if self._reward_summary_enabled:
            reward_breakdown = self._reward_breakdown_array(result)
            self._record_step_accounting(reward_breakdown, rewards, current_agents, result)

        if self._reward_summary_enabled:
            infos = {
                agent: {
                    "action_step": int(result.actionStep),
                    "winner_agent_index": int(result.winnerAgentIndex),
                    "winner_label": str(result.winnerLabel),
                    "current_map_id": str(result.currentMapId),
                    "current_map_name": str(result.currentMapName),
                    "agent_done": bool(result.dones[index]),
                    "route_targets_reached": int(result.routeTargetsReached[index]),
                    "route_progress_delta": float(
                        result.routeProgressDeltas[index]
                    ),
                    "reward_breakdown": self._reward_breakdown_for_agent(
                        reward_breakdown,
                        agent,
                    ),
                }
                for index, agent in enumerate(current_agents)
            }
        else:
            infos = {agent: {} for agent in current_agents}

        if episode_done:
            if self._reward_summary_enabled:
                self._finalize_episode_accounting(result)
            self.agents = []
        else:
            self.agents = list(self._agents)
        return (
            self._observations(result, current_agents),
            rewards,
            terminateds,
            truncateds,
            infos,
        )

    def close(self):
        self._env.close()

    def _agent_index(self, agent: str) -> int:
        return self._agent_indices[agent]

    def _observations(self, result, agents) -> Dict[str, np.ndarray]:
        flat = np.asarray(result.observations, dtype=np.float32)
        observations = flat.reshape((self._agent_count, OBSERVATION_SIZE))
        return {
            agent: observations[self._agent_indices[agent]].copy()
            for agent in agents
        }

    def _reset_episode_accounting(self, result) -> None:
        self._reward_breakdown_names = [str(name) for name in result.rewardBreakdownNames]
        self._episode_map_id = str(result.currentMapId)
        self._episode_map_name = str(result.currentMapName)
        self._episode_reward_totals = {agent: 0.0 for agent in self._agents}
        self._episode_bucket_totals = {
            agent: np.zeros(len(self._reward_breakdown_names), dtype=np.float64)
            for agent in self._agents
        }
        self._episode_progress_totals = {agent: 0.0 for agent in self._agents}

    def _record_step_accounting(self, breakdown, rewards, agents, result) -> None:
        for agent in agents:
            index = self._agent_index(agent)
            self._episode_reward_totals[agent] = (
                self._episode_reward_totals.get(agent, 0.0) + float(rewards.get(agent, 0.0))
            )
            if agent in self._episode_bucket_totals and index < breakdown.shape[0]:
                self._episode_bucket_totals[agent] += breakdown[index]
            if agent in self._episode_progress_totals:
                self._episode_progress_totals[agent] += float(result.routeProgressDeltas[index])

    def _finalize_episode_accounting(self, result) -> None:
        for agent in self._agents:
            index = self._agent_index(agent)
            REWARD_SUMMARY.record_episode(
                self._episode_map_id,
                self._episode_map_name,
                agent,
                self._episode_reward_totals.get(agent, 0.0),
                self._reward_breakdown_names,
                self._episode_bucket_totals.get(
                    agent,
                    np.zeros(len(self._reward_breakdown_names), dtype=np.float64),
                ),
                int(result.routeTargetsReached[index]),
                self._episode_progress_totals.get(agent, 0.0),
            )

    def _reward_breakdown_for_agent(self, breakdown, agent) -> Dict[str, float]:
        index = self._agent_index(agent)
        if breakdown is None or index >= breakdown.shape[0]:
            return {}
        return {
            name: float(value)
            for name, value in zip(self._reward_breakdown_names, breakdown[index])
        }

    def _reward_breakdown_array(self, result) -> np.ndarray:
        names = self._reward_breakdown_names
        if not names:
            return np.zeros((self._agent_count, 0), dtype=np.float64)
        flat = np.asarray(result.rewardBreakdown, dtype=np.float64)
        expected_size = self._agent_count * len(names)
        if flat.size != expected_size:
            return np.zeros((self._agent_count, len(names)), dtype=np.float64)
        return flat.reshape((self._agent_count, len(names)))


def build_algorithm(args):
    observation_space = gym.spaces.Box(
        low=-1.0,
        high=1.0,
        shape=(OBSERVATION_SIZE,),
        dtype=np.float32,
    )
    action_space = gym.spaces.Box(
        low=-1.0,
        high=1.0,
        shape=(ACTION_SIZE,),
        dtype=np.float32,
    )

    env_config = {
        "jar_path": str(Path(args.jar).resolve()),
        "controlled_agents": args.controlled_agents,
        "field_size": args.field_size,
        "action_repeat": args.action_repeat,
        "max_action_steps": args.max_action_steps,
        "no_progress_max_action_steps": args.no_progress_max_action_steps,
        "off_road_failure_max_action_steps": args.off_road_failure_max_action_steps,
        "route_targets": args.route_targets,
        "route_target_fraction": args.route_target_fraction,
        "random_race_spawns": args.random_race_spawns,
        "seed": args.seed,
        "map_ids": args.map_ids,
        "objective": args.objective,
        "reward_summary": not args.no_reward_summary,
        "reward_step_penalty": args.reward_step_penalty,
        "reward_progress": args.reward_progress,
        "reward_route_alignment": args.reward_route_alignment,
        "reward_steering_penalty": args.reward_steering_penalty,
        "reward_reverse_free_epsilon": args.reward_reverse_free_epsilon,
        "reward_reverse_penalty_per_unit": args.reward_reverse_penalty_per_unit,
        "reward_reverse_max_penalty": args.reward_reverse_max_penalty,
        "reward_car_push_penalty": args.reward_car_push_penalty,
        "reward_car_push_max_step_penalty": args.reward_car_push_max_step_penalty,
        "reward_off_road_penalty": args.reward_off_road_penalty,
        "reward_off_road_distance_penalty": args.reward_off_road_distance_penalty,
        "reward_off_road_max_penalty": args.reward_off_road_max_penalty,
        "reward_no_progress_penalty": args.reward_no_progress_penalty,
        "reward_off_road_recovery": args.reward_off_road_recovery,
        "reward_off_road_failure_penalty": args.reward_off_road_failure_penalty,
    }
    config = (
        PPOConfig()
        .environment(RatassMultiAgentEnv, env_config=env_config)
        .framework("torch")
        .debugging(seed=args.seed)
        .rl_module(
            model_config=DefaultModelConfig(
                fcnet_hiddens=[args.hidden_size] * args.hidden_layers,
                fcnet_activation=args.hidden_activation,
            )
        )
    )
    if args.num_gpus > 0:
        config = config.resources(num_gpus=args.num_gpus)
    try:
        config = config.training(
            gamma=args.gamma,
            lr=args.lr,
            entropy_coeff=args.entropy_coeff,
            train_batch_size=args.train_batch_size,
            minibatch_size=args.minibatch_size,
            num_epochs=args.num_epochs,
            grad_clip=args.grad_clip,
            vf_clip_param=args.vf_clip_param,
        )
    except TypeError:
        config = config.training(
            gamma=args.gamma,
            lr=args.lr,
            entropy_coeff=args.entropy_coeff,
            train_batch_size=args.train_batch_size,
            sgd_minibatch_size=args.minibatch_size,
            num_sgd_iter=args.num_epochs,
            grad_clip=args.grad_clip,
            vf_clip_param=args.vf_clip_param,
        )

    config = config.multi_agent(
        policies={"shared_policy": (None, observation_space, action_space, {})},
        policy_mapping_fn=lambda agent_id, *unused_args, **unused_kwargs: "shared_policy",
    )

    if hasattr(config, "env_runners"):
        config = config.env_runners(
            num_env_runners=args.workers,
            sample_timeout_s=args.sample_timeout_s,
        )
    else:
        try:
            config = config.rollouts(
                num_rollout_workers=args.workers,
                sample_timeout_s=args.sample_timeout_s,
            )
        except TypeError:
            config = config.rollouts(num_rollout_workers=args.workers)

    if hasattr(config, "build_algo"):
        return config.build_algo()
    return config.build()


def _reshape_exported_layer(layer: Dict, policy_file: Path) -> Tuple[np.ndarray, np.ndarray]:
    try:
        input_size = int(layer["inputSize"])
        output_size = int(layer["outputSize"])
        weights = np.asarray(layer["weights"], dtype=np.float32).reshape(
            output_size,
            input_size,
        )
        bias = np.asarray(layer["bias"], dtype=np.float32).reshape(output_size)
    except (KeyError, TypeError, ValueError) as exc:
        raise ValueError(f"{policy_file} has an invalid exported layer") from exc
    return weights, bias


def exported_actor_layer_prefixes(layer_count: int) -> List[Tuple[str, ...]]:
    if layer_count < 2:
        raise ValueError("Exported actor must contain at least one hidden layer and an output layer")
    hidden_count = layer_count - 1
    return [
        (
            f"encoder.encoder.net.mlp.{index * 2}",
            f"encoder.actor_encoder.net.mlp.{index * 2}",
        )
        for index in range(hidden_count)
    ] + [("pi.net.mlp.0",)]


def load_exported_actor_state(policy_file: Path) -> Dict[Tuple[str, ...], np.ndarray]:
    with policy_file.open("r", encoding="utf-8") as handle:
        payload = json.load(handle)

    exported_observation_size = int(payload.get("observationSize", -1))
    if exported_observation_size != OBSERVATION_SIZE:
        raise ValueError(
            f"{policy_file} observationSize={payload.get('observationSize')} is not compatible "
            f"with current observation size {OBSERVATION_SIZE}"
        )
    if int(payload.get("actionSize", -1)) != ACTION_SIZE:
        raise ValueError(
            f"{policy_file} actionSize={payload.get('actionSize')} "
            f"does not match current action size {ACTION_SIZE}"
        )

    layers = payload.get("layers")
    if not isinstance(layers, list) or len(layers) < 2:
        raise ValueError(f"{policy_file} must contain hidden and output actor layers")

    actor_state: Dict[Tuple[str, ...], np.ndarray] = {}
    layer_prefixes = exported_actor_layer_prefixes(len(layers))
    for layer_index, (prefixes, layer) in enumerate(zip(layer_prefixes, layers)):
        weights, bias = _reshape_exported_layer(layer, policy_file)
        if layer_index == 0 and weights.shape[1] != exported_observation_size:
            raise ValueError(
                f"{policy_file} actor input size {weights.shape[1]} does not match exported "
                f"observation size {exported_observation_size}"
            )
        actor_state[tuple(f"{prefix}.weight" for prefix in prefixes)] = weights
        actor_state[tuple(f"{prefix}.bias" for prefix in prefixes)] = bias
    return actor_state


def copy_exported_actor_values(
        key: str,
        current_values: np.ndarray,
        exported_values: np.ndarray) -> Tuple[np.ndarray, bool]:
    current = np.asarray(current_values)
    exported = np.asarray(exported_values, dtype=current.dtype)
    if tuple(current.shape) == tuple(exported.shape):
        return exported, False

    if len(current.shape) != len(exported.shape):
        raise ValueError(
            f"{key} rank mismatch: exported {exported.shape}, current {current.shape}"
        )

    updated = current.copy()
    if current.ndim == 1:
        if exported.shape[0] > current.shape[0]:
            raise ValueError(
                f"{key} shape mismatch: exported {exported.shape}, current {current.shape}"
            )
        updated[:exported.shape[0]] = exported
        return updated, True

    if current.ndim == 2:
        if exported.shape[0] > current.shape[0] or exported.shape[1] > current.shape[1]:
            raise ValueError(
                f"{key} shape mismatch: exported {exported.shape}, current {current.shape}"
            )
        updated[:exported.shape[0], :exported.shape[1]] = exported
        if exported.shape[1] < current.shape[1]:
            updated[:exported.shape[0], exported.shape[1]:] = 0.0
        return updated, True

    raise ValueError(f"{key} unsupported tensor shape: {current.shape}")


def initialize_actor_from_exported_policy(algorithm, policy_file: Path) -> None:
    if not policy_file.exists():
        raise FileNotFoundError(f"{policy_file} does not exist")

    actor_state = load_exported_actor_state(policy_file)
    learner_weights = algorithm.learner_group.get_weights(module_ids=["shared_policy"])
    policy_weights = dict(learner_weights["shared_policy"])

    partial_initialization = False
    for candidate_keys, values in actor_state.items():
        key = next((candidate for candidate in candidate_keys if candidate in policy_weights), None)
        if key is None:
            raise ValueError(
                "Current PPO module does not contain any actor key from "
                + ", ".join(candidate_keys)
            )
        current = np.asarray(policy_weights[key])
        copied_values, partial = copy_exported_actor_values(key, current, values)
        partial_initialization = partial_initialization or partial
        policy_weights[key] = copied_values

    algorithm.learner_group.set_weights({"shared_policy": policy_weights})
    algorithm.env_runner_group.sync_weights(
        from_worker_or_learner_group=algorithm.learner_group,
        policies=["shared_policy"],
        inference_only=True,
    )
    print(
        f"initialized_policy={policy_file} partial={str(partial_initialization).lower()}",
        flush=True,
    )


def try_initialize_actor_from_exported_policy(algorithm, policy_file: Path) -> bool:
    try:
        initialize_actor_from_exported_policy(algorithm, policy_file)
    except (FileNotFoundError, OSError, ValueError) as error:
        message = str(error).replace("\n", " ")
        print(
            f"init_policy_ignored={policy_file} "
            f"reason=incompatible_or_unreadable action=train_from_scratch error={message}",
            flush=True,
        )
        return False
    return True


def configure_ray_output(checkpoint_dir: Path) -> None:
    ray_results_dir = checkpoint_dir / "ray-results"
    ray_results_dir.mkdir(parents=True, exist_ok=True)

    # Direct Algorithm builds still use Ray Tune's default ~/ray_results path.
    # In this workspace that may be read-only, so patch the imported constants.
    import ray.train.constants as ray_train_constants
    import ray.tune.experiment.experiment as ray_experiment
    import ray.tune.trainable.trainable as ray_trainable

    storage_path = os.fspath(ray_results_dir)
    ray_train_constants.DEFAULT_STORAGE_PATH = storage_path
    ray_experiment.DEFAULT_STORAGE_PATH = storage_path
    ray_trainable.DEFAULT_STORAGE_PATH = storage_path


def configure_ray_runtime(args: argparse.Namespace) -> None:
    init_kwargs = {
        "include_dashboard": False,
        "ignore_reinit_error": True,
    }
    should_init = False

    if args.ray_node_ip:
        init_kwargs["_node_ip_address"] = args.ray_node_ip
        should_init = True
    if args.ray_num_cpus > 0:
        init_kwargs["num_cpus"] = args.ray_num_cpus
        should_init = True
    if args.ray_temp_dir:
        ray_temp_dir = Path(args.ray_temp_dir).resolve()
        ray_temp_dir.mkdir(parents=True, exist_ok=True)
        init_kwargs["_temp_dir"] = os.fspath(ray_temp_dir)
        should_init = True

    if not should_init:
        return

    import ray

    if not ray.is_initialized():
        ray.init(**init_kwargs)


def is_incompatible_checkpoint_error(error: BaseException) -> bool:
    message = str(error)
    return any(pattern in message for pattern in INCOMPATIBLE_CHECKPOINT_PATTERNS)


def archive_incompatible_checkpoint(checkpoint_dir: Path) -> Path:
    timestamp = time.strftime("%Y%m%d-%H%M%S")
    archive_dir = checkpoint_dir.with_name(f"{checkpoint_dir.name}.incompatible-{timestamp}")
    suffix = 1
    while archive_dir.exists():
        archive_dir = checkpoint_dir.with_name(
            f"{checkpoint_dir.name}.incompatible-{timestamp}-{suffix}"
        )
        suffix += 1
    shutil.move(os.fspath(checkpoint_dir), os.fspath(archive_dir))
    checkpoint_dir.mkdir(parents=True, exist_ok=True)
    configure_ray_output(checkpoint_dir)
    return archive_dir


def restore_algorithm_checkpoint(algorithm, checkpoint_dir: Path) -> None:
    checkpoint_dir = checkpoint_dir.resolve()
    algorithm.restore(str(checkpoint_dir))

    # RLlib's Algorithm.restore() can leave the newly built LearnerGroup at its
    # initialized weights when the checkpoint came from a different curriculum
    # stage. Restore that component explicitly, including optimizer state, then
    # synchronize the inference copies used by the EnvRunners.
    learner_checkpoint = checkpoint_dir / "learner_group"
    if not learner_checkpoint.is_dir():
        return
    algorithm.learner_group.restore_from_path(str(learner_checkpoint))
    algorithm.env_runner_group.sync_weights(
        from_worker_or_learner_group=algorithm.learner_group,
        inference_only=True,
    )
    if algorithm.eval_env_runner_group:
        algorithm.eval_env_runner_group.sync_weights(
            from_worker_or_learner_group=algorithm.learner_group,
            inference_only=True,
        )
    print(f"checkpoint_learner_state_restored={learner_checkpoint}", flush=True)


def build_algorithm_with_restore(args, checkpoint_dir: Path):
    algorithm = build_algorithm(args)
    if args.resume:
        checkpoint_file = checkpoint_dir / "rllib_checkpoint.json"
        if not checkpoint_file.exists():
            raise FileNotFoundError(
                f"{checkpoint_file} does not exist. Remove --resume or train once first."
            )
        try:
            restore_algorithm_checkpoint(algorithm, checkpoint_dir)
        except RuntimeError as error:
            if not is_incompatible_checkpoint_error(error):
                raise
            algorithm.stop()
            archive_dir = archive_incompatible_checkpoint(checkpoint_dir)
            print(
                "checkpoint_resume_incompatible=1 "
                f"archived={archive_dir} "
                f"fresh_checkpoint_dir={checkpoint_dir}",
                flush=True,
            )
            algorithm = build_algorithm(args)
            if args.init_policy:
                try_initialize_actor_from_exported_policy(
                    algorithm,
                    Path(args.init_policy).resolve(),
                )
            return algorithm
        print(f"restored={checkpoint_dir}", flush=True)
        if args.init_policy:
            print("init_policy_ignored=resume_checkpoint_present", flush=True)
    elif args.init_policy:
        try_initialize_actor_from_exported_policy(
            algorithm,
            Path(args.init_policy).resolve(),
        )
    return algorithm


def read_metric(result: Dict, *paths: Tuple[str, ...], default: float = float("nan")) -> float:
    for path in paths:
        value = result
        for key in path:
            if not isinstance(value, dict) or key not in value:
                break
            value = value[key]
        else:
            try:
                return float(value)
            except (TypeError, ValueError):
                continue
    return default


def has_metric(value: float) -> bool:
    return math.isfinite(value)


def format_metric(value: float, precision: int = 3) -> str:
    if not has_metric(value):
        return "none"
    return f"{value:.{precision}f}"


def format_count_metric(value: float) -> str:
    if not has_metric(value):
        return "0"
    return f"{value:.0f}"


def checkpoint_path(save_result) -> str:
    checkpoint = getattr(save_result, "checkpoint", save_result)
    path = getattr(checkpoint, "path", None)
    return os.fspath(path) if path is not None else str(save_result)


def capture_checkpoint_candidate(
        algorithm,
        candidate_root: Path,
        iteration: int,
        reward_mean: float,
        episode_len_mean: float,
        episodes: float) -> CheckpointCandidate:
    destination = candidate_root / f"iteration-{iteration:06d}"
    if destination.exists():
        shutil.rmtree(destination)
    saved_path = checkpoint_path(algorithm.save(str(destination)))
    return CheckpointCandidate(
        iteration=iteration,
        reward_mean=reward_mean,
        episode_len_mean=episode_len_mean,
        episodes=episodes,
        checkpoint_path=saved_path,
    )


def prune_checkpoint_candidates(
        candidates: List[CheckpointCandidate],
        current_iteration: int) -> None:
    minimum_iteration = current_iteration - CHECKPOINT_CANDIDATE_WINDOW + 1
    stale = [candidate for candidate in candidates if candidate.iteration < minimum_iteration]
    candidates[:] = [
        candidate for candidate in candidates if candidate.iteration >= minimum_iteration
    ]
    for candidate in stale:
        path = Path(candidate.checkpoint_path)
        if path.is_dir():
            shutil.rmtree(path)
        elif path.exists():
            path.unlink()


def parse_metric_tokens(line: str) -> Dict[str, str]:
    metrics: Dict[str, str] = {}
    for token in line.strip().split():
        if "=" not in token:
            continue
        key, value = token.split("=", 1)
        metrics[key] = value
    return metrics


def read_best_state(path: Path) -> Dict:
    if not path.exists():
        return {}
    try:
        with path.open("r", encoding="utf-8") as handle:
            return json.load(handle)
    except (OSError, json.JSONDecodeError):
        return {}


def write_best_state(path: Path, state: Dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(state, handle, indent=2, sort_keys=True)
        handle.write("\n")


def best_eval_state_path(args, checkpoint_dir: Path) -> Path:
    if args.best_eval_state:
        return Path(args.best_eval_state).resolve()
    return checkpoint_dir / "best-eval" / "best_policy.json"


def best_eval_dir(args, checkpoint_dir: Path) -> Path:
    if args.best_eval_state:
        return best_eval_state_path(args, checkpoint_dir).parent
    return checkpoint_dir / "best-eval"


def best_eval_candidate_path(args, checkpoint_dir: Path) -> Path:
    path = best_eval_dir(args, checkpoint_dir) / "candidate_policy.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def best_eval_archive_path(args, checkpoint_dir: Path) -> Path:
    path = best_eval_dir(args, checkpoint_dir) / "best_policy_export.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def best_eval_checkpoint_dir(args, checkpoint_dir: Path) -> Path:
    return best_eval_dir(args, checkpoint_dir) / "best-rllib-checkpoint"


def run_policy_evaluation(args, candidate_policy: Path) -> Tuple[Optional[float], Dict[str, str]]:
    episodes_per_map = args.best_eval_episodes_per_map
    episodes = args.best_eval_episodes
    if episodes_per_map <= 0 and episodes <= 0:
        episodes_per_map = 1

    field_size = args.best_eval_field_size if args.best_eval_field_size > 0 else args.field_size
    controlled_agents = (
        args.best_eval_controlled_agents
        if args.best_eval_controlled_agents > 0
        else args.controlled_agents
    )
    command = [
        sys.executable,
        os.fspath(REPO_ROOT / "tools" / "rl" / "evaluate_policy.py"),
        "--jar",
        os.fspath(Path(args.jar).resolve()),
        "--policy",
        os.fspath(candidate_policy),
        "--quiet",
        "--objective",
        args.objective,
        "--controlled-agents",
        str(controlled_agents),
        "--field-size",
        str(field_size),
        "--action-repeat",
        str(args.action_repeat),
        "--route-targets",
        str(args.route_targets),
        "--route-target-fraction",
        str(args.route_target_fraction),
        "--no-progress-max-action-steps",
        str(args.no_progress_max_action_steps),
        "--off-road-failure-max-action-steps",
        str(args.off_road_failure_max_action_steps),
        "--seed",
        str(args.seed),
        "--reward-step-penalty",
        str(args.reward_step_penalty),
        "--reward-progress",
        str(args.reward_progress),
        "--reward-route-alignment",
        str(args.reward_route_alignment),
        "--reward-steering-penalty",
        str(args.reward_steering_penalty),
        "--reward-reverse-free-epsilon",
        str(args.reward_reverse_free_epsilon),
        "--reward-reverse-penalty-per-unit",
        str(args.reward_reverse_penalty_per_unit),
        "--reward-reverse-max-penalty",
        str(args.reward_reverse_max_penalty),
        "--reward-car-push-penalty",
        str(args.reward_car_push_penalty),
        "--reward-car-push-max-step-penalty",
        str(args.reward_car_push_max_step_penalty),
        "--reward-off-road-penalty",
        str(args.reward_off_road_penalty),
        "--reward-off-road-distance-penalty",
        str(args.reward_off_road_distance_penalty),
        "--reward-off-road-max-penalty",
        str(args.reward_off_road_max_penalty),
        "--reward-no-progress-penalty",
        str(args.reward_no_progress_penalty),
        "--reward-off-road-recovery",
        str(args.reward_off_road_recovery),
        "--reward-off-road-failure-penalty",
        str(args.reward_off_road_failure_penalty),
    ]
    if episodes_per_map > 0:
        command.extend(["--episodes-per-map", str(episodes_per_map)])
    else:
        command.extend(["--episodes", str(episodes)])
    if args.best_eval_steps > 0:
        command.extend(["--steps", str(args.best_eval_steps)])
    if args.best_eval_map_ids:
        command.extend(["--map-ids", args.best_eval_map_ids])
    command.append("--random-race-spawns" if args.random_race_spawns else "--fixed-race-spawns")

    completed = subprocess.run(
        command,
        cwd=REPO_ROOT,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        check=False,
    )
    metrics: Dict[str, str] = {}
    score: Optional[float] = None
    for line in completed.stdout.splitlines():
        print(f"best_eval {line}", flush=True)
        if line.startswith("evaluation_score="):
            metrics.update(parse_metric_tokens(line))
    if completed.returncode != 0:
        print(f"best_eval_failed exit_code={completed.returncode}", flush=True)
        return None, metrics
    try:
        score = float(metrics["evaluation_score"])
    except (KeyError, ValueError):
        print("best_eval_failed reason=missing_evaluation_score", flush=True)
        return None, metrics
    return score, metrics


def maybe_promote_best_policy(
        algorithm,
        args,
        checkpoint_dir: Path,
        iteration: int,
        saved_checkpoint: str,
        compare_installed: bool = True) -> Dict[str, object]:
    result: Dict[str, object] = {
        "evaluated": False,
        "accepted": False,
        "promoted": False,
        "score": None,
        "previous_score": None,
    }
    if not args.best_export_output:
        return result

    candidate_policy = best_eval_candidate_path(args, checkpoint_dir)
    export_objective = args.best_export_objective or EXPORT_OBJECTIVES[args.objective]
    export_checkpoint_policy(
        checkpoint_dir,
        candidate_policy,
        export_objective,
        args.hidden_activation,
    )
    output_file = Path(args.best_export_output).resolve()
    score, metrics = run_policy_evaluation(args, candidate_policy)
    if score is None:
        print(
            f"model_export_not_overwritten reason=evaluation_failed "
            f"output={output_file} checkpoint={saved_checkpoint}",
            flush=True,
        )
        return result
    result["evaluated"] = True
    result["score"] = score
    try:
        avg_targets = float(metrics.get("avg_targets", "nan"))
    except (TypeError, ValueError):
        avg_targets = float("nan")
    if avg_targets < args.best_eval_min_route_targets:
        print(
            f"best_policy_rejected score={score:.3f} "
            f"avg_targets={avg_targets:.3f} "
            f"min_route_targets={args.best_eval_min_route_targets:.3f}",
            flush=True,
        )
        print(
            f"model_export_not_overwritten reason=insufficient_route_goals "
            f"output={output_file} score={score:.3f} "
            f"avg_targets={avg_targets:.3f} "
            f"min_route_targets={args.best_eval_min_route_targets:.3f} "
            f"checkpoint={saved_checkpoint}",
            flush=True,
        )
        return result
    result["accepted"] = True

    state_path = best_eval_state_path(args, checkpoint_dir)
    state = read_best_state(state_path)
    try:
        previous_score = float(state.get("best_score", "-inf"))
    except (TypeError, ValueError):
        previous_score = float("-inf")
    if output_file.exists() and compare_installed and not args.best_eval_ignore_installed:
        installed_score, installed_metrics = run_policy_evaluation(args, output_file)
        if installed_score is not None and installed_score > previous_score:
            previous_score = installed_score
            state = {
                "best_score": installed_score,
                "iteration": 0,
                "checkpoint": "installed_policy",
                "policy": os.fspath(output_file),
                "archived_policy": os.fspath(output_file),
                "metrics": installed_metrics,
            }
    result["previous_score"] = previous_score
    if score <= previous_score:
        print(
            f"best_policy_unchanged score={score:.3f} best_score={previous_score:.3f}",
            flush=True,
        )
        print(
            f"model_export_not_overwritten reason=score_not_improved "
            f"output={output_file} score={score:.3f} "
            f"best_score={previous_score:.3f} checkpoint={saved_checkpoint}",
            flush=True,
        )
        return result

    archive_file = best_eval_archive_path(args, checkpoint_dir)
    output_existed = output_file.exists()
    shutil.copyfile(candidate_policy, archive_file)
    output_file.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(candidate_policy, output_file)
    archived_checkpoint = best_eval_checkpoint_dir(args, checkpoint_dir)
    if archived_checkpoint.exists():
        shutil.rmtree(archived_checkpoint)
    archived_checkpoint.parent.mkdir(parents=True, exist_ok=True)
    archived_checkpoint_path = checkpoint_path(algorithm.save(str(archived_checkpoint)))
    next_state = {
        "best_score": score,
        "iteration": iteration,
        "checkpoint": saved_checkpoint,
        "best_rllib_checkpoint": archived_checkpoint_path,
        "policy": os.fspath(output_file),
        "archived_policy": os.fspath(archive_file),
        "metrics": metrics,
    }
    write_best_state(state_path, next_state)
    print(
        f"best_policy_promoted score={score:.3f} previous_score={previous_score:.3f} "
        f"output={output_file} state={state_path}",
        flush=True,
    )
    if output_existed:
        print(
            f"model_export_overwritten output={output_file} score={score:.3f} "
            f"previous_score={previous_score:.3f} checkpoint={saved_checkpoint}",
            flush=True,
        )
    else:
        print(
            f"model_export_created output={output_file} score={score:.3f} "
            f"checkpoint={saved_checkpoint}",
            flush=True,
        )
    result["promoted"] = True
    return result


def has_restorable_best_checkpoint(state: Dict) -> bool:
    archived_checkpoint = state.get("best_rllib_checkpoint")
    if not archived_checkpoint:
        return False
    return (Path(str(archived_checkpoint)).resolve() / "rllib_checkpoint.json").exists()


def establish_stage_baseline(algorithm, args, checkpoint_dir: Path) -> Dict[str, object]:
    result: Dict[str, object] = {
        "evaluated": False,
        "accepted": False,
        "promoted": False,
        "score": None,
        "previous_score": None,
    }
    if not args.best_export_output:
        print("stage_baseline_skipped reason=best_export_disabled", flush=True)
        return result

    state_path = best_eval_state_path(args, checkpoint_dir)
    state = read_best_state(state_path)
    if has_restorable_best_checkpoint(state):
        archived_path = Path(str(state["best_rllib_checkpoint"])).resolve()
        restore_algorithm_checkpoint(algorithm, archived_path)
        restored_path = checkpoint_path(algorithm.save(str(checkpoint_dir)))
        print(
            f"stage_baseline_restored iteration={state.get('iteration', '?')} "
            f"score={state.get('best_score', '?')} "
            f"checkpoint={archived_path} output={restored_path}",
            flush=True,
        )
        return result

    if state_path.exists():
        state_path.unlink()
        print(
            f"stage_baseline_state_reset reason=checkpoint_not_restorable "
            f"state={state_path}",
            flush=True,
        )

    checkpoint = algorithm.save(str(checkpoint_dir))
    saved_path = checkpoint_path(checkpoint)
    print(
        f"stage_baseline_evaluation_start iteration=0 checkpoint={saved_path}",
        flush=True,
    )
    result = maybe_promote_best_policy(
        algorithm,
        args,
        checkpoint_dir,
        0,
        saved_path,
        compare_installed=False,
    )
    if result["promoted"]:
        print(
            f"stage_baseline_archived iteration=0 score={result['score']:.3f} "
            f"checkpoint={best_eval_checkpoint_dir(args, checkpoint_dir)}",
            flush=True,
        )
    elif result["accepted"]:
        print(
            f"stage_baseline_kept_existing score={result['score']:.3f}",
            flush=True,
        )
    else:
        print(
            "stage_baseline_not_eligible reason=evaluation_or_route_goals",
            flush=True,
        )
    return result


def save_checkpoint(algorithm, checkpoint_dir: Path, args, iteration: int) -> Dict[str, object]:
    checkpoint = algorithm.save(str(checkpoint_dir))
    saved_path = checkpoint_path(checkpoint)
    print(f"checkpoint={saved_path}", flush=True)
    return maybe_promote_best_policy(algorithm, args, checkpoint_dir, iteration, saved_path)


def save_selected_checkpoint_candidate(
        algorithm,
        checkpoint_dir: Path,
        args,
        candidates: List[CheckpointCandidate],
        current_iteration: int) -> Dict[str, object]:
    window_start = max(1, current_iteration - CHECKPOINT_CANDIDATE_WINDOW + 1)
    window_candidates = [
        candidate
        for candidate in candidates
        if window_start <= candidate.iteration <= current_iteration
    ]
    latest_candidate = max(
        window_candidates,
        key=lambda candidate: candidate.iteration,
        default=None,
    )
    if latest_candidate is None or latest_candidate.iteration != current_iteration:
        print(
            f"checkpoint_candidate_selected iteration={current_iteration} "
            f"window={window_start}-{current_iteration} candidates={len(window_candidates)} "
            "eligible=0 reward_mean=none episode_len_mean=none episodes=0 "
            "reason=current_snapshot_unavailable",
            flush=True,
        )
        return save_checkpoint(algorithm, checkpoint_dir, args, current_iteration)

    selection = select_checkpoint_candidate(window_candidates)
    selected = selection.candidate
    print(
        f"checkpoint_candidate_selected iteration={selected.iteration} "
        f"window={window_start}-{current_iteration} "
        f"candidates={len(window_candidates)} eligible={selection.eligible_count} "
        f"reward_mean={format_metric(selected.reward_mean)} "
        f"episode_len_mean={format_metric(selected.episode_len_mean, 1)} "
        f"episodes={format_count_metric(selected.episodes)} "
        f"reason={selection.reason}",
        flush=True,
    )

    # Evaluate the selected historical state without rolling live training back.
    selected_is_latest = selected.iteration == current_iteration
    if not selected_is_latest:
        restore_algorithm_checkpoint(algorithm, Path(selected.checkpoint_path))
    try:
        return save_checkpoint(algorithm, checkpoint_dir, args, selected.iteration)
    finally:
        if not selected_is_latest:
            restore_algorithm_checkpoint(
                algorithm,
                Path(latest_candidate.checkpoint_path),
            )
            latest_checkpoint = checkpoint_path(algorithm.save(str(checkpoint_dir)))
            print(
                f"checkpoint_training_state_restored iteration={current_iteration} "
                f"checkpoint={latest_checkpoint}",
                flush=True,
            )


def restore_stage_best_checkpoint(algorithm, args, checkpoint_dir: Path) -> bool:
    state = read_best_state(best_eval_state_path(args, checkpoint_dir))
    if not has_restorable_best_checkpoint(state):
        print("stage_best_checkpoint_restore_skipped reason=no_promoted_checkpoint", flush=True)
        return False
    archived_checkpoint = state["best_rllib_checkpoint"]
    archived_path = Path(str(archived_checkpoint)).resolve()

    restore_algorithm_checkpoint(algorithm, archived_path)
    restored_path = checkpoint_path(algorithm.save(str(checkpoint_dir)))
    print(
        f"stage_best_checkpoint_restored checkpoint={archived_path} "
        f"stage_iteration={state.get('iteration', '?')} output={restored_path}",
        flush=True,
    )
    return True


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default=os.fspath(DEFAULT_JAR))
    parser.add_argument("--iterations", type=int, default=25)
    parser.add_argument("--workers", type=int, default=0)
    parser.add_argument(
        "--controlled-agents",
        type=int,
        default=None,
        help="controlled learner cars sharing the race policy",
    )
    parser.add_argument(
        "--field-size",
        type=int,
        default=None,
        help="field size for the Java race environment",
    )
    parser.add_argument("--action-repeat", type=int, default=4)
    parser.add_argument("--max-action-steps", type=int, default=19200)
    parser.add_argument(
        "--no-progress-max-action-steps",
        type=int,
        default=600,
        help="truncate an episode after this many actions without meaningful forward progress; 0 disables it",
    )
    parser.add_argument(
        "--off-road-failure-max-action-steps",
        type=int,
        default=45,
        help="terminate an attempt after this many consecutive actions physically outside the road; 0 disables it",
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
        help="optional fraction of a lap for each random-spawn route target; 0 keeps the default quarter-lap target",
    )
    parser.add_argument(
        "--random-race-spawns",
        action="store_true",
        default=None,
        help=(
            "spawn learners at safe random road locations, facing the route tangent; "
            "valid for single-car route-target training"
        ),
    )
    parser.add_argument(
        "--fixed-race-spawns",
        action="store_false",
        dest="random_race_spawns",
        help="use fixed map spawn points; required for full-lap training",
    )
    parser.add_argument("--num-gpus", type=float, default=0.0)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument(
        "--objective",
        choices=("race",),
        default="race",
        help="race follows route progress around the circuit",
    )
    parser.add_argument(
        "--map-ids",
        default="",
        help="optional comma-separated map ids to train on; defaults to all discovered masks",
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
    parser.add_argument("--gamma", type=float, default=0.995)
    parser.add_argument("--lr", type=float, default=3e-4)
    parser.add_argument("--entropy-coeff", type=float, default=0.005)
    parser.add_argument("--num-epochs", type=int, default=30)
    parser.add_argument("--grad-clip", type=float, default=40.0)
    parser.add_argument("--vf-clip-param", type=float, default=100.0)
    parser.add_argument("--train-batch-size", type=int, default=8192)
    parser.add_argument("--minibatch-size", type=int, default=512)
    parser.add_argument(
        "--hidden-size",
        type=int,
        default=1024,
        help="width of each PPO fully-connected hidden layer",
    )
    parser.add_argument(
        "--hidden-layers",
        type=int,
        default=2,
        help="number of PPO fully-connected hidden layers",
    )
    parser.add_argument(
        "--hidden-activation",
        default="tanh",
        help="activation for PPO fully-connected hidden layers",
    )
    parser.add_argument("--checkpoint-dir", default=os.fspath(DEFAULT_CHECKPOINT))
    parser.add_argument(
        "--checkpoint-every",
        type=int,
        default=0,
        help=(
            "checkpoint and evaluate the highest-reward candidate "
            "from the preceding ten iterations"
        ),
    )
    parser.add_argument(
        "--ray-num-cpus",
        type=int,
        default=0,
        help="explicit CPU count for Ray; 0 lets Ray decide",
    )
    parser.add_argument(
        "--ray-temp-dir",
        default="",
        help="optional Ray temp directory, useful for keeping logs/checkpoints local",
    )
    parser.add_argument(
        "--ray-node-ip",
        default="127.0.0.1",
        help="node IP used for local Ray worker RPC; 127.0.0.1 avoids network-interface disconnects",
    )
    parser.add_argument(
        "--sample-timeout-s",
        type=float,
        default=600.0,
        help="seconds to wait for remote RLlib env runners before treating an iteration as having no samples",
    )
    parser.add_argument(
        "--resume",
        action="store_true",
        help="restore PPO state from --checkpoint-dir before continuing training",
    )
    parser.add_argument(
        "--init-policy",
        default="",
        help=(
            "warm-start a fresh PPO run from an exported game policy JSON; ignored "
            "when --resume is used"
        ),
    )
    parser.add_argument(
        "--no-reward-summary",
        action="store_true",
        help="disable per-map/per-car reward bucket summaries after each iteration",
    )
    parser.add_argument(
        "--best-export-output",
        default="",
        help="export and promote only policies with the best evaluation score to this JSON file",
    )
    parser.add_argument(
        "--best-export-objective",
        default="",
        help="objective label written into the promoted JSON policy",
    )
    parser.add_argument(
        "--best-eval-episodes-per-map",
        type=int,
        default=0,
        help="episodes per map for route evaluation; 1 is used when best export is enabled and no episode count is set",
    )
    parser.add_argument(
        "--best-eval-episodes",
        type=int,
        default=0,
        help="shuffled episodes for route evaluation when episodes-per-map is 0",
    )
    parser.add_argument(
        "--best-eval-min-route-targets",
        type=float,
        default=1.0,
        help="do not promote/export a policy unless evaluation reaches at least this many average route targets",
    )
    parser.add_argument(
        "--best-eval-controlled-agents",
        type=int,
        default=0,
        help="controlled learner cars used by route evaluation; defaults to the training controlled-agent count",
    )
    parser.add_argument(
        "--best-eval-field-size",
        type=int,
        default=0,
        help="field size used by route evaluation; defaults to the training field size",
    )
    parser.add_argument(
        "--best-eval-steps",
        type=int,
        default=0,
        help="max action steps used by route evaluation; 0 uses evaluate_policy.py defaults",
    )
    parser.add_argument(
        "--best-eval-map-ids",
        default="",
        help="optional comma-separated map ids for route evaluation",
    )
    parser.add_argument(
        "--best-eval-state",
        default="",
        help="JSON state file that stores the best evaluation score",
    )
    parser.add_argument(
        "--best-eval-ignore-installed",
        action="store_true",
        help=(
            "compare candidate policies only against the current best-eval state, "
            "not the policy currently installed at --best-export-output"
        ),
    )
    args = parser.parse_args()
    apply_objective_defaults(args)
    validate_spawn_configuration(args, parser)
    return args


def apply_objective_defaults(args: argparse.Namespace) -> None:
    if args.controlled_agents is None:
        args.controlled_agents = DEFAULT_CONTROLLED_AGENTS
    if args.field_size is None:
        args.field_size = max(DEFAULT_FIELD_SIZE, args.controlled_agents)


def validate_spawn_configuration(args: argparse.Namespace, parser: argparse.ArgumentParser) -> None:
    if args.random_race_spawns is None:
        args.random_race_spawns = args.route_targets > 0

    if args.route_targets > 0:
        if args.controlled_agents > 1:
            parser.error(
                "--controlled-agents must be 1 when --route-targets is a route-target "
                "stage; train multiple cars only with full-lap training"
            )
        if not args.random_race_spawns:
            parser.error(
                "route-target training requires --random-race-spawns so saved "
                "random spawn seeds can be reused"
            )
        return

    if args.random_race_spawns:
        parser.error("full-lap training requires fixed race spawns")


def main() -> None:
    args = parse_args()
    checkpoint_dir = Path(args.checkpoint_dir).resolve()
    checkpoint_dir.mkdir(parents=True, exist_ok=True)
    configure_ray_output(checkpoint_dir)
    configure_ray_runtime(args)
    algorithm = build_algorithm_with_restore(args, checkpoint_dir)
    candidate_root = Path(
        tempfile.mkdtemp(
            prefix=f".{checkpoint_dir.name}-checkpoint-candidates-",
            dir=checkpoint_dir.parent,
        )
    )
    checkpoint_candidates: List[CheckpointCandidate] = []
    if args.workers > 0 and not args.no_reward_summary:
        print(
            "reward_summary_warning=local_summary_only workers>0 may hide remote worker episodes",
            flush=True,
        )

    try:
        establish_stage_baseline(algorithm, args, checkpoint_dir)
        last_saved_iteration = 0
        current_iteration = 0
        try:
            for iteration in range(1, args.iterations + 1):
                current_iteration = iteration
                summary_mark = REWARD_SUMMARY.mark()
                result = algorithm.train()
                reward_mean = read_metric(
                    result,
                    ("episode_reward_mean",),
                    ("episode_return_mean",),
                    ("env_runners", "episode_return_mean"),
                )
                length_mean = read_metric(
                    result,
                    ("episode_len_mean",),
                    ("env_runners", "episode_len_mean"),
                )
                episodes = read_metric(
                    result,
                    ("episodes_this_iter",),
                    ("num_episodes",),
                    ("env_runners", "num_episodes"),
                )
                env_steps_sampled = read_metric(
                    result,
                    ("num_env_steps_sampled",),
                    ("num_env_steps_sampled_lifetime",),
                    ("env_runners", "num_env_steps_sampled"),
                    ("env_runners", "num_env_steps_sampled_lifetime"),
                )
                metric_status = (
                    "ok"
                    if has_metric(reward_mean) and has_metric(length_mean) and has_metric(episodes)
                    else "no_completed_episodes"
                )
                print(
                    f"iteration={iteration} "
                    f"reward_mean={format_metric(reward_mean)} "
                    f"episode_len_mean={format_metric(length_mean, 1)} "
                    f"episodes={format_count_metric(episodes)} "
                    f"metric_status={metric_status} "
                    f"env_steps_sampled={format_count_metric(env_steps_sampled)}",
                    flush=True,
                )
                if not args.no_reward_summary:
                    for line in REWARD_SUMMARY.render_since(summary_mark, iteration):
                        print(line, flush=True)
                if should_capture_checkpoint_candidate(
                        iteration,
                        args.iterations,
                        args.checkpoint_every):
                    checkpoint_candidates.append(capture_checkpoint_candidate(
                        algorithm,
                        candidate_root,
                        iteration,
                        reward_mean,
                        length_mean,
                        episodes,
                    ))
                    prune_checkpoint_candidates(checkpoint_candidates, iteration)
                checkpoint_due = (
                    args.checkpoint_every > 0
                    and iteration % args.checkpoint_every == 0
                )
                if checkpoint_due:
                    save_selected_checkpoint_candidate(
                        algorithm,
                        checkpoint_dir,
                        args,
                        checkpoint_candidates,
                        iteration,
                    )
                    last_saved_iteration = iteration
        except KeyboardInterrupt:
            save_iteration = current_iteration if current_iteration > 0 else last_saved_iteration
            save_iteration = max(1, save_iteration)
            print(
                f"interrupted=1 saving_checkpoint=1 iteration={save_iteration}",
                flush=True,
            )
            print(
                f"checkpoint_candidate_selected iteration={save_iteration} "
                f"window={save_iteration}-{save_iteration} candidates=1 eligible=0 "
                "reward_mean=none episode_len_mean=none episodes=0 "
                "reason=interrupted_live_state",
                flush=True,
            )
            save_checkpoint(algorithm, checkpoint_dir, args, save_iteration)
            raise SystemExit(130)

        final_iteration = current_iteration if current_iteration > 0 else args.iterations
        if last_saved_iteration != final_iteration:
            save_selected_checkpoint_candidate(
                algorithm,
                checkpoint_dir,
                args,
                checkpoint_candidates,
                final_iteration,
            )
        restore_stage_best_checkpoint(algorithm, args, checkpoint_dir)
    finally:
        try:
            algorithm.stop()
        finally:
            shutil.rmtree(candidate_root, ignore_errors=True)


if __name__ == "__main__":
    main()
