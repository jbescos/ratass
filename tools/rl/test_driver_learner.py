import sys
import unittest
from types import SimpleNamespace
from unittest.mock import Mock, patch

import gymnasium as gym
import numpy as np
import torch
from ray.rllib.algorithms.ppo.torch.default_ppo_torch_rl_module import DefaultPPOTorchRLModule
from ray.rllib.core.columns import Columns

import train_rllib
from export_policy import actor_layers


class DriverLearnerTest(unittest.TestCase):
    def test_wider_actor_preserves_existing_driving_actions(self):
        def module(width):
            return DefaultPPOTorchRLModule(
                observation_space=gym.spaces.Box(-1.0, 1.0, (33,), dtype=np.float32),
                action_space=gym.spaces.Box(-1.0, 1.0, (2,), dtype=np.float32),
                model_config={"fcnet_hiddens": [width, width], "fcnet_activation": "tanh",
                              "vf_share_layers": False},
            )

        narrow, wide = module(96), module(128)
        original = narrow.state_dict()
        expanded = wide.state_dict()
        for key, value in original.items():
            if not (key.startswith("encoder.actor_encoder.") or key.startswith("pi.")):
                continue
            exported = value.detach().numpy()
            if key.startswith("pi."):
                exported = exported[:2]
            copied, _ = train_rllib.copy_exported_actor_values(
                key, expanded[key].detach().numpy(), exported)
            copied = train_rllib.initialize_unexported_exploration_values(
                key, copied, exported.shape[0], -3.0)
            expanded[key] = torch.from_numpy(copied)
        wide.load_state_dict(expanded)
        observations = torch.rand((256, 33)) * 2 - 1
        with torch.no_grad():
            expected = narrow.forward_inference({Columns.OBS: observations})[Columns.ACTION_DIST_INPUTS][:, :2]
            actual = wide.forward_inference({Columns.OBS: observations})[Columns.ACTION_DIST_INPUTS][:, :2]
        torch.testing.assert_close(actual, expected, atol=1e-6, rtol=1e-5)
        self.assertEqual([128, 128, 4], [layer["outputSize"] for layer in actor_layers(
            {key: value.detach().numpy() for key, value in expanded.items()}, "tanh")])

    def test_separate_critic_cannot_change_exported_actor(self):
        module = DefaultPPOTorchRLModule(
            observation_space=gym.spaces.Box(-1.0, 1.0, (33,), dtype=np.float32),
            action_space=gym.spaces.Box(-1.0, 1.0, (2,), dtype=np.float32),
            model_config={"fcnet_hiddens": [96, 96], "fcnet_activation": "tanh",
                          "vf_share_layers": False},
        )
        before = actor_layers({k: v.detach().numpy().copy()
                               for k, v in module.state_dict().items()}, "tanh")
        optimizer = torch.optim.Adam(module.parameters(), lr=0.001)
        values = module.compute_values({Columns.OBS: torch.ones((8, 33))})
        ((values + 500) ** 2).mean().backward()
        optimizer.step()
        after = actor_layers({k: v.detach().numpy()
                              for k, v in module.state_dict().items()}, "tanh")
        self.assertEqual(before, after)
        self.assertEqual([96, 96, 4], [layer["outputSize"] for layer in after])

    def test_reward_scale_changes_only_learner_reward(self):
        env = train_rllib.RatassMultiAgentEnv.__new__(train_rllib.RatassMultiAgentEnv)
        env._agents = ["car"]
        env._agent_count = 1
        env._action_size = 2
        env._agent_indices = {"car": 0}
        env._java_action_buffer = [0.0, 0.0]
        env._reward_summary_enabled = False
        env._learner_reward_scale = 0.01
        result = SimpleNamespace(rewards=[-500.0], episodeDone=True,
                                 episodeTruncated=False, episodeTerminated=True)
        env._env = Mock()
        env._env.stepFast.return_value = result
        env._observations = Mock(return_value={"car": np.zeros(33)})
        _, rewards, terminated, truncated, _ = env.step({"car": [0.04, -0.2]})
        self.assertEqual({"car": -5.0}, rewards)
        self.assertEqual([-500.0], result.rewards)
        self.assertEqual([0.04, -0.2], env._java_action_buffer)
        self.assertTrue(terminated["__all__"])
        self.assertFalse(truncated["__all__"])

    def test_cli_rejects_invalid_reward_scales(self):
        for value in ("0", "-1", "nan", "inf"):
            with self.subTest(value=value), patch.object(sys, "argv", ["train", "--learner-reward-scale", value]):
                with self.assertRaises(SystemExit):
                    train_rllib.parse_args()

    def test_ppo_receives_separate_critic_and_scale(self):
        with patch.object(sys, "argv", ["train", "--separate-value-network", "--learner-reward-scale", "0.01"]):
            args = train_rllib.parse_args()
        config = Mock()
        for method in ("environment", "framework", "debugging", "rl_module", "training", "multi_agent", "env_runners"):
            getattr(config, method).return_value = config
        with patch.object(train_rllib, "PPOConfig", return_value=config):
            train_rllib.build_algorithm(args)
        self.assertFalse(config.rl_module.call_args.kwargs["model_config"].vf_share_layers)
        self.assertEqual(0.01, config.environment.call_args.kwargs["env_config"]["learner_reward_scale"])


if __name__ == "__main__":
    unittest.main()
