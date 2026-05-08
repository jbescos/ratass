#!/usr/bin/env python3
"""Train a shared PPO policy against the Ratass headless Java RL environment.

Build the desktop jar first:

    mvn -pl desktop -am package

Then install the optional Python dependencies from this directory and run:

    python tools/rl/train_rllib.py --iterations 25
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path
from typing import Dict, Tuple

import gymnasium as gym
import jpype
import numpy as np
from ray.rllib.algorithms.ppo import PPOConfig
from ray.rllib.env.multi_agent_env import MultiAgentEnv


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_JAR = REPO_ROOT / "desktop" / "target" / "ratass-desktop-1.0.jar"
DEFAULT_CHECKPOINT = REPO_ROOT / "rl-checkpoints-circle"
OBSERVATION_SIZE = 30
ACTION_SIZE = 2


def _start_jvm(jar_path: Path) -> None:
    if jpype.isJVMStarted():
        return
    if not jar_path.exists():
        raise FileNotFoundError(
            f"{jar_path} does not exist. Run `mvn -pl desktop -am package` first."
        )
    jpype.startJVM(classpath=[str(jar_path)])
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
        training_config.withControlledAgentCount(int(env_config.get("controlled_agents", 1)))
        training_config.withFieldSize(int(env_config.get("field_size", 12)))
        training_config.withActionRepeat(int(env_config.get("action_repeat", 4)))
        training_config.withMaxActionSteps(int(env_config.get("max_action_steps", 1350)))
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
        return self._observations(result, self.agents), {agent: {} for agent in self.agents}

    def step(self, action_dict):
        actions = np.zeros((self._agent_count, ACTION_SIZE), dtype=np.float32)
        current_agents = list(self.agents)
        for agent in current_agents:
            index = self._agent_index(agent)
            action = np.asarray(action_dict.get(agent, np.zeros(ACTION_SIZE)), dtype=np.float32)
            actions[index] = np.clip(action, -1.0, 1.0)

        result = self._env.step(self._java_float_array(actions.reshape(-1).tolist()))
        rewards = {
            agent: float(result.rewards[self._agent_index(agent)])
            for agent in current_agents
        }
        terminateds = {agent: bool(result.episodeDone) for agent in current_agents}
        terminateds["__all__"] = bool(result.episodeDone)
        truncateds = {agent: False for agent in current_agents}
        truncateds["__all__"] = False
        infos = {
            agent: {
                "action_step": int(result.actionStep),
                "winner_agent_index": int(result.winnerAgentIndex),
                "winner_label": str(result.winnerLabel),
            }
            for agent in current_agents
        }

        if result.episodeDone:
            self.agents = []
        else:
            self.agents = current_agents
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
        "seed": args.seed,
        "map_ids": args.map_ids,
    }
    config = (
        PPOConfig()
        .environment(RatassMultiAgentEnv, env_config=env_config)
        .framework("torch")
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
        config = config.env_runners(num_env_runners=args.workers)
    else:
        config = config.rollouts(num_rollout_workers=args.workers)

    if hasattr(config, "build_algo"):
        return config.build_algo()
    return config.build()


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


def checkpoint_path(save_result) -> str:
    checkpoint = getattr(save_result, "checkpoint", save_result)
    path = getattr(checkpoint, "path", None)
    return os.fspath(path) if path is not None else str(save_result)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default=os.fspath(DEFAULT_JAR))
    parser.add_argument("--iterations", type=int, default=25)
    parser.add_argument("--workers", type=int, default=0)
    parser.add_argument("--controlled-agents", type=int, default=1)
    parser.add_argument("--field-size", type=int, default=12)
    parser.add_argument("--action-repeat", type=int, default=4)
    parser.add_argument("--max-action-steps", type=int, default=1350)
    parser.add_argument("--num-gpus", type=float, default=0.0)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument(
        "--map-ids",
        default="",
        help="comma-separated map ids to train on, for example map004,map006",
    )
    parser.add_argument("--gamma", type=float, default=0.995)
    parser.add_argument("--lr", type=float, default=3e-4)
    parser.add_argument("--train-batch-size", type=int, default=8192)
    parser.add_argument("--minibatch-size", type=int, default=512)
    parser.add_argument("--checkpoint-dir", default=os.fspath(DEFAULT_CHECKPOINT))
    parser.add_argument("--checkpoint-every", type=int, default=0)
    parser.add_argument(
        "--resume",
        action="store_true",
        help="restore PPO state from --checkpoint-dir before continuing training",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    checkpoint_dir = Path(args.checkpoint_dir).resolve()
    checkpoint_dir.mkdir(parents=True, exist_ok=True)
    configure_ray_output(checkpoint_dir)
    algorithm = build_algorithm(args)
    if args.resume:
        checkpoint_file = checkpoint_dir / "rllib_checkpoint.json"
        if not checkpoint_file.exists():
            raise FileNotFoundError(
                f"{checkpoint_file} does not exist. Remove --resume or train once first."
            )
        algorithm.restore(str(checkpoint_dir))
        print(f"restored={checkpoint_dir}", flush=True)

    try:
        for iteration in range(1, args.iterations + 1):
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
            print(
                f"iteration={iteration} "
                f"reward_mean={reward_mean:.3f} "
                f"episode_len_mean={length_mean:.1f} "
                f"episodes={episodes:.0f}",
                flush=True,
            )
            if args.checkpoint_every > 0 and iteration % args.checkpoint_every == 0:
                checkpoint = algorithm.save(str(checkpoint_dir))
                print(f"checkpoint={checkpoint_path(checkpoint)}", flush=True)

        checkpoint = algorithm.save(str(checkpoint_dir))
        print(f"checkpoint={checkpoint_path(checkpoint)}", flush=True)
    finally:
        algorithm.stop()


if __name__ == "__main__":
    main()
