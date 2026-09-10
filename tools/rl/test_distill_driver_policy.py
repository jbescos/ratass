import copy
import unittest

import numpy as np
import torch

from distill_driver_policy import ExportedDriver


class ExportedDriverTest(unittest.TestCase):
    def test_forward_matches_exported_tanh_network_and_clips_only_actions(self):
        source = {"observationSize": 2, "actionSize": 2, "layers": [
            {"inputSize": 2, "outputSize": 2, "activation": "tanh",
             "weights": [1., 2., 3., 4.], "bias": [0.1, 0.2]},
            {"inputSize": 2, "outputSize": 4, "activation": "linear",
             "weights": [5., 6., -7., 8., 9., 10., 11., 12.], "bias": [0.3, 0.4, 0.5, 0.6]}]}
        original = copy.deepcopy(source)
        values = np.array([[0.2, -0.3], [0.9, 0.8]], dtype=np.float32)
        hidden = np.tanh(values @ np.array([[1., 2.], [3., 4.]]).T + [0.1, 0.2])
        expected = np.clip(hidden @ np.array([[5., 6.], [-7., 8.]]).T + [0.3, 0.4], -1, 1)
        model = ExportedDriver(source)
        np.testing.assert_allclose(model(torch.from_numpy(values)).detach().numpy(), expected, atol=1e-6)
        exported = model.payload()
        self.assertEqual(exported["layers"][-1]["weights"][4:], source["layers"][-1]["weights"][4:])
        with torch.no_grad():
            model.layers[0].bias.add_(0.02)
        modified = model.payload()
        self.assertNotEqual(modified["layers"][0]["bias"], original["layers"][0]["bias"])
        self.assertEqual(source, original)

    def test_unknown_activation_is_not_silently_ignored(self):
        with self.assertRaises(ValueError):
            ExportedDriver({"layers": [{"activation": "unknown"}]})


if __name__ == "__main__":
    unittest.main()
