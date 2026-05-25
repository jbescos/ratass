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
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import gymnasium as gym
import jpype
import numpy as np
from ray.rllib.algorithms.ppo import PPOConfig
from ray.rllib.core.rl_module.default_model_config import DefaultModelConfig
from ray.rllib.env.multi_agent_env import MultiAgentEnv

from export_policy import export_policy as export_checkpoint_policy


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = REPO_ROOT / "desktop" / "target" / "ratass-desktop-1.0.jar"
DEFAULT_CHECKPOINT = REPO_ROOT / "rl-checkpoints-race-physics-v1"
OBSERVATION_SIZE = 68
ACTION_SIZE = 2
DEFAULT_CONTROLLED_AGENTS = 1
DEFAULT_FIELD_SIZE = 1
EXPORTED_ACTOR_LAYERS = (
    "encoder.encoder.net.mlp.0",
    "encoder.encoder.net.mlp.2",
    "pi.net.mlp.0",
)
EXPORT_OBJECTIVES = {"race": "race-checkpoints-v1"}


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
        checkpoints_reached: int,
        eliminations: int,
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
                "checkpoints_reached": int(checkpoints_reached),
                "eliminations": int(eliminations),
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
                    "checkpoints_reached": 0,
                    "eliminations": 0,
                    "progress_total": 0.0,
                    "bucket_names": episode["bucket_names"],
                    "bucket_totals": np.zeros_like(episode["bucket_totals"]),
                }
                grouped[key] = group

            group["episodes"] += 1
            group["reward_total"] += episode["reward_total"]
            group["checkpoints_reached"] += episode["checkpoints_reached"]
            group["eliminations"] += episode["eliminations"]
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
                f"checkpoints_avg={group['checkpoints_reached'] / episode_count:.3f}",
                f"eliminations_avg={group['eliminations'] / episode_count:.3f}",
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
    jpype.startJVM(jvm_path, "-Xrs", classpath=[str(jar_path)])
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
        training_config.withMaxActionSteps(int(env_config.get("max_action_steps", 6400)))
        training_config.withMaxCheckpoints(int(env_config.get("max_checkpoints", 6)))
        training_config.withCheckpointRadius(float(env_config.get("checkpoint_radius", 3.0)))
        training_config.withCheckpointDeadlineSeconds(
            float(env_config.get("checkpoint_deadline_seconds", 0.0))
        )
        training_config.withRaceMode(True)
        training_config.withRandomRaceSpawns(bool(env_config.get("random_race_spawns", False)))
        training_config.withSeed(int(env_config.get("seed", 1)))
        self._add_selected_maps(training_config, env_config.get("map_ids", ""))

        self._java_float_array = jpype.JArray(jpype.JFloat)
        self._env = ratass_game.RlTrainingEnvironment(training_config)
        self._agent_count = int(self._env.getControlledAgentCount())
        self._agents = [f"learner_{i}" for i in range(self._agent_count)]
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
        self._reward_summary_enabled = bool(env_config.get("reward_summary", True))
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
        maps = arena_maps.createDefaultSet()
        available = {}
        for index in range(int(maps.size)):
            arena_map = maps.get(index)
            available[str(arena_map.getId())] = arena_map

        missing = [map_id for map_id in selected_ids if map_id not in available]
        if missing:
            raise ValueError(
                "Unknown map id(s) "
                + ", ".join(missing)
                + ". Available: "
                + ", ".join(sorted(available))
            )

        for map_id in selected_ids:
            training_config.addMap(available[map_id])

    def reset(self, *, seed=None, options=None):
        result = self._env.reset()
        self.agents = list(self._agents)
        if self._reward_summary_enabled:
            self._reset_episode_accounting(result)
        return self._observations(result, self.agents), {agent: {} for agent in self.agents}

    def step(self, action_dict):
        actions = np.zeros((self._agent_count, ACTION_SIZE), dtype=np.float32)
        current_agents = list(self._agents)
        for agent in current_agents:
            index = self._agent_index(agent)
            action = np.asarray(action_dict.get(agent, np.zeros(ACTION_SIZE)), dtype=np.float32)
            actions[index] = np.clip(action, -1.0, 1.0)

        result = self._env.step(self._java_float_array(actions.reshape(-1).tolist()))
        rewards = {
            agent: float(result.rewards[self._agent_index(agent)])
            for agent in current_agents
        }
        reward_breakdown = None
        if self._reward_summary_enabled:
            reward_breakdown = self._reward_breakdown_array(result)
            self._record_step_accounting(reward_breakdown, rewards, current_agents, result)
        # Keep all learner ids present until the Java episode ends. RLlib's
        # new connector stack can still have pending module outputs for an
        # agent after we report that individual agent as done, which can trip
        # a KeyError inside unbatch_to_individual_items. Java ignores actions
        # for inactive cars, so stable ids are the simpler contract here.
        terminateds = {agent: bool(result.episodeDone) for agent in current_agents}
        terminateds["__all__"] = bool(result.episodeDone)
        truncateds = {agent: False for agent in current_agents}
        truncateds["__all__"] = False
        infos = {
            agent: {
                "action_step": int(result.actionStep),
                "winner_agent_index": int(result.winnerAgentIndex),
                "winner_label": str(result.winnerLabel),
                "current_map_id": str(result.currentMapId),
                "current_map_name": str(result.currentMapName),
                "agent_done": bool(result.dones[self._agent_index(agent)]),
                "checkpoints_reached": int(result.checkpointsReached[self._agent_index(agent)]),
                "eliminations": int(result.eliminations[self._agent_index(agent)]),
                "progress_toward_checkpoint": float(
                    result.progressTowardCheckpoint[self._agent_index(agent)]
                ),
            }
            for agent in current_agents
        }
        if self._reward_summary_enabled:
            for agent in current_agents:
                infos[agent]["reward_breakdown"] = self._reward_breakdown_for_agent(
                    reward_breakdown,
                    agent,
                )

        if result.episodeDone:
            if self._reward_summary_enabled:
                self._finalize_episode_accounting(result)
            self.agents = []
        else:
            self.agents = list(self._agents)
        return self._observations(result, self.agents), rewards, terminateds, truncateds, infos

    def close(self):
        self._env.close()

    def _agent_index(self, agent: str) -> int:
        return int(agent.rsplit("_", 1)[1])

    def _observations(self, result, agents) -> Dict[str, np.ndarray]:
        flat = np.asarray(list(result.observations), dtype=np.float32)
        observations = flat.reshape((self._agent_count, OBSERVATION_SIZE))
        return {
            agent: observations[self._agent_index(agent)].copy()
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
                self._episode_progress_totals[agent] += float(result.progressTowardCheckpoint[index])

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
                int(result.checkpointsReached[index]),
                int(result.eliminations[index]),
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
        flat = np.asarray(list(result.rewardBreakdown), dtype=np.float64)
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
        "max_checkpoints": args.max_checkpoints,
        "checkpoint_radius": args.checkpoint_radius,
        "checkpoint_deadline_seconds": args.checkpoint_deadline_seconds,
        "random_race_spawns": args.random_race_spawns,
        "seed": args.seed,
        "map_ids": args.map_ids,
        "objective": args.objective,
        "reward_summary": not args.no_reward_summary,
    }
    config = (
        PPOConfig()
        .environment(RatassMultiAgentEnv, env_config=env_config)
        .framework("torch")
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
            train_batch_size=args.train_batch_size,
            minibatch_size=args.minibatch_size,
        )
    except TypeError:
        config = config.training(
            gamma=args.gamma,
            lr=args.lr,
            train_batch_size=args.train_batch_size,
            sgd_minibatch_size=args.minibatch_size,
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


def load_exported_actor_state(policy_file: Path) -> Dict[str, np.ndarray]:
    with policy_file.open("r", encoding="utf-8") as handle:
        payload = json.load(handle)

    exported_observation_size = int(payload.get("observationSize", -1))
    if exported_observation_size <= 0 or exported_observation_size > OBSERVATION_SIZE:
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
    if not isinstance(layers, list) or len(layers) != len(EXPORTED_ACTOR_LAYERS):
        raise ValueError(
            f"{policy_file} must contain {len(EXPORTED_ACTOR_LAYERS)} exported layers"
        )

    actor_state: Dict[str, np.ndarray] = {}
    for prefix, layer in zip(EXPORTED_ACTOR_LAYERS, layers):
        weights, bias = _reshape_exported_layer(layer, policy_file)
        if prefix == EXPORTED_ACTOR_LAYERS[0] and weights.shape[1] != exported_observation_size:
            raise ValueError(
                f"{policy_file} actor input size {weights.shape[1]} does not match exported "
                f"observation size {exported_observation_size}"
            )
        actor_state[f"{prefix}.weight"] = weights
        actor_state[f"{prefix}.bias"] = bias
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

    learner_weights = algorithm.learner_group.get_weights(module_ids=["shared_policy"])
    policy_weights = dict(learner_weights["shared_policy"])
    actor_state = load_exported_actor_state(policy_file)

    partial_initialization = False
    for key, values in actor_state.items():
        if key not in policy_weights:
            raise ValueError(f"Current PPO module does not contain actor key {key}")
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
        "--checkpoint-deadline-seconds",
        str(args.checkpoint_deadline_seconds),
        "--checkpoint-radius",
        str(args.checkpoint_radius),
        "--max-checkpoints",
        str(args.max_checkpoints),
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
        args,
        checkpoint_dir: Path,
        iteration: int,
        saved_checkpoint: str) -> None:
    if not args.best_export_output:
        return

    candidate_policy = best_eval_candidate_path(args, checkpoint_dir)
    export_objective = args.best_export_objective or EXPORT_OBJECTIVES[args.objective]
    export_checkpoint_policy(checkpoint_dir, candidate_policy, export_objective)
    output_file = Path(args.best_export_output).resolve()
    score, metrics = run_policy_evaluation(args, candidate_policy)
    if score is None:
        return
    try:
        avg_checkpoints = float(metrics.get("avg_checkpoints", "nan"))
    except (TypeError, ValueError):
        avg_checkpoints = float("nan")
    if avg_checkpoints < args.best_eval_min_checkpoints:
        print(
            f"best_policy_rejected score={score:.3f} "
            f"avg_checkpoints={avg_checkpoints:.3f} "
            f"min_checkpoints={args.best_eval_min_checkpoints:.3f}",
            flush=True,
        )
        return

    state_path = best_eval_state_path(args, checkpoint_dir)
    state = read_best_state(state_path)
    try:
        previous_score = float(state.get("best_score", "-inf"))
    except (TypeError, ValueError):
        previous_score = float("-inf")
    if output_file.exists() and not args.best_eval_ignore_installed:
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
    if score <= previous_score:
        print(
            f"best_policy_unchanged score={score:.3f} best_score={previous_score:.3f}",
            flush=True,
        )
        return

    archive_file = best_eval_archive_path(args, checkpoint_dir)
    shutil.copyfile(candidate_policy, archive_file)
    output_file.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(candidate_policy, output_file)
    next_state = {
        "best_score": score,
        "iteration": iteration,
        "checkpoint": saved_checkpoint,
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


def save_checkpoint(algorithm, checkpoint_dir: Path, args, iteration: int) -> None:
    checkpoint = algorithm.save(str(checkpoint_dir))
    saved_path = checkpoint_path(checkpoint)
    print(f"checkpoint={saved_path}", flush=True)
    maybe_promote_best_policy(args, checkpoint_dir, iteration, saved_path)


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
        help="kept for compatibility; race training uses controlled learners only",
    )
    parser.add_argument("--action-repeat", type=int, default=4)
    parser.add_argument("--max-action-steps", type=int, default=6400)
    parser.add_argument(
        "--max-checkpoints",
        type=int,
        default=6,
        help="episode checkpoint target; -1 means one current-map lap, 0 means the full live-race lap count",
    )
    parser.add_argument("--checkpoint-radius", type=float, default=3.0)
    parser.add_argument(
        "--checkpoint-deadline-seconds",
        type=float,
        default=0.0,
        help="seconds a learner can go without crossing the next checkpoint; 0 derives from max steps/checkpoints",
    )
    parser.add_argument(
        "--random-race-spawns",
        action="store_true",
        default=True,
        help="spawn learners at safe random road locations, facing the route to the next checkpoint",
    )
    parser.add_argument(
        "--fixed-race-spawns",
        action="store_false",
        dest="random_race_spawns",
        help="debug override: use fixed map spawn points instead of random road spawns",
    )
    parser.add_argument("--num-gpus", type=float, default=0.0)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument(
        "--objective",
        choices=("race",),
        default="race",
        help="race follows ordered map checkpoints",
    )
    parser.add_argument(
        "--map-ids",
        default="",
        help="optional comma-separated map ids to train on; defaults to all discovered masks",
    )
    parser.add_argument("--gamma", type=float, default=0.995)
    parser.add_argument("--lr", type=float, default=3e-4)
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
    parser.add_argument("--checkpoint-every", type=int, default=0)
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
        help="export and promote only checkpoints with the best evaluation score to this JSON file",
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
        help="episodes per map for checkpoint evaluation; 1 is used when best export is enabled and no episode count is set",
    )
    parser.add_argument(
        "--best-eval-episodes",
        type=int,
        default=0,
        help="shuffled episodes for checkpoint evaluation when episodes-per-map is 0",
    )
    parser.add_argument(
        "--best-eval-min-checkpoints",
        type=float,
        default=1.0,
        help="do not promote/export a policy unless evaluation reaches at least this many average checkpoints",
    )
    parser.add_argument(
        "--best-eval-controlled-agents",
        type=int,
        default=0,
        help="controlled learner cars used by checkpoint evaluation; defaults to the training controlled-agent count",
    )
    parser.add_argument(
        "--best-eval-field-size",
        type=int,
        default=0,
        help="field size used by checkpoint evaluation; defaults to the training field size",
    )
    parser.add_argument(
        "--best-eval-steps",
        type=int,
        default=0,
        help="max action steps used by checkpoint evaluation; 0 uses evaluate_policy.py defaults",
    )
    parser.add_argument(
        "--best-eval-map-ids",
        default="",
        help="optional comma-separated map ids for checkpoint evaluation",
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
    return args


def apply_objective_defaults(args: argparse.Namespace) -> None:
    if args.controlled_agents is None:
        args.controlled_agents = DEFAULT_CONTROLLED_AGENTS
    if args.field_size is None:
        args.field_size = max(DEFAULT_FIELD_SIZE, args.controlled_agents)


def main() -> None:
    args = parse_args()
    checkpoint_dir = Path(args.checkpoint_dir).resolve()
    checkpoint_dir.mkdir(parents=True, exist_ok=True)
    configure_ray_output(checkpoint_dir)
    configure_ray_runtime(args)
    algorithm = build_algorithm(args)
    if args.resume:
        checkpoint_file = checkpoint_dir / "rllib_checkpoint.json"
        if not checkpoint_file.exists():
            raise FileNotFoundError(
                f"{checkpoint_file} does not exist. Remove --resume or train once first."
            )
        algorithm.restore(str(checkpoint_dir))
        print(f"restored={checkpoint_dir}", flush=True)
        if args.init_policy:
            print("init_policy_ignored=resume_checkpoint_present", flush=True)
    elif args.init_policy:
        initialize_actor_from_exported_policy(
            algorithm,
            Path(args.init_policy).resolve(),
        )
    if args.workers > 0 and not args.no_reward_summary:
        print(
            "reward_summary_warning=local_summary_only workers>0 may hide remote worker episodes",
            flush=True,
        )

    try:
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
                if args.checkpoint_every > 0 and iteration % args.checkpoint_every == 0:
                    save_checkpoint(algorithm, checkpoint_dir, args, iteration)
                    last_saved_iteration = iteration
        except KeyboardInterrupt:
            save_iteration = current_iteration if current_iteration > 0 else last_saved_iteration
            save_iteration = max(1, save_iteration)
            print(
                f"interrupted=1 saving_checkpoint=1 iteration={save_iteration}",
                flush=True,
            )
            save_checkpoint(algorithm, checkpoint_dir, args, save_iteration)
            raise SystemExit(130)

        if last_saved_iteration != args.iterations:
            save_checkpoint(algorithm, checkpoint_dir, args, args.iterations)
    finally:
        algorithm.stop()


if __name__ == "__main__":
    main()
