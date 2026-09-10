import unittest

import torch
from torch import nn

from strategy_network_health import network_health


class NetworkHealthTest(unittest.TestCase):
    def network(self):
        model = nn.Module()
        model.network = nn.Sequential(nn.Linear(2, 4), nn.Tanh(), nn.Linear(4, 1))
        return model

    def test_detects_saturation(self):
        model = self.network()
        with torch.no_grad():
            model.network[0].weight.zero_()
            model.network[0].bias.fill_(10)
        health = network_health(model, torch.zeros(10, 2))
        self.assertEqual(1, health["layers"][0]["saturated_fraction"])
        self.assertEqual(4, health["layers"][0]["persistently_saturated_neurons"])

    def test_read_only_and_bounded(self):
        model = self.network()
        observations = torch.randn(50, 2)
        model.network(observations).sum().backward()
        weights = [value.detach().clone() for value in model.parameters()]
        gradients = [value.grad.clone() for value in model.parameters()]
        rng = torch.get_rng_state().clone()
        health = network_health(model, observations, sample_limit=8)
        self.assertLessEqual(health["samples"], 8)
        torch.testing.assert_close(rng, torch.get_rng_state())
        for value, weight, gradient in zip(model.parameters(), weights, gradients):
            torch.testing.assert_close(value, weight)
            torch.testing.assert_close(value.grad, gradient)

    def test_empty(self):
        self.assertEqual([], network_health(self.network(), torch.empty(0, 2))["layers"])


if __name__ == "__main__":
    unittest.main()
