#!/usr/bin/env python3

import unittest
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))

from export_policy import OBSERVATION_SIZE, actor_layers


class ExportPolicyTest(unittest.TestCase):
    def test_exports_every_hidden_actor_layer(self):
        state = {
            "encoder.encoder.net.mlp.0.weight": np.zeros((8, OBSERVATION_SIZE)),
            "encoder.encoder.net.mlp.0.bias": np.zeros(8),
            "encoder.encoder.net.mlp.2.weight": np.zeros((8, 8)),
            "encoder.encoder.net.mlp.2.bias": np.zeros(8),
            "encoder.encoder.net.mlp.4.weight": np.zeros((8, 8)),
            "encoder.encoder.net.mlp.4.bias": np.zeros(8),
            "pi.net.mlp.0.weight": np.zeros((4, 8)),
            "pi.net.mlp.0.bias": np.zeros(4),
        }

        layers = actor_layers(state, "relu")

        self.assertEqual(4, len(layers))
        self.assertEqual(["relu", "relu", "relu", "linear"], [
            layer["activation"] for layer in layers
        ])
        self.assertEqual(OBSERVATION_SIZE, layers[0]["inputSize"])
        self.assertEqual(4, layers[-1]["outputSize"])

    def test_rejects_unknown_activation(self):
        with self.assertRaisesRegex(ValueError, "Unsupported hidden activation"):
            actor_layers({}, "unknown")


if __name__ == "__main__":
    unittest.main()
