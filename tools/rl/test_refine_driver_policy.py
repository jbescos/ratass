import unittest

import numpy as np

from refine_driver_policy import adjusted_hidden_unit, blended_policy, calibrated_policy, passes_regression_limits


class CalibrationTest(unittest.TestCase):
    def test_blend_preserves_architecture_and_source(self):
        source = {"observationSize": 1, "actionSize": 1,
                  "layers": [{"inputSize": 1, "outputSize": 1, "activation": "linear",
                              "weights": [2.0], "bias": [1.0]}]}
        other = {"observationSize": 1, "actionSize": 1,
                 "layers": [{"inputSize": 1, "outputSize": 1, "activation": "linear",
                             "weights": [6.0], "bias": [3.0]}]}
        result = blended_policy(source, other, 0.25)
        self.assertEqual(result["layers"][0]["weights"], [3.0])
        self.assertEqual(result["layers"][0]["bias"], [1.5])
        self.assertEqual(source["layers"][0]["weights"], [2.0])
        self.assertEqual(blended_policy(source, other, 0), source)
        self.assertEqual(blended_policy(source, other, 1), other)
        with self.assertRaises(ValueError):
            blended_policy(source, other, 1.1)
        other["layers"][0]["activation"] = "tanh"
        with self.assertRaises(ValueError):
            blended_policy(source, other, 0.5)

    def test_hidden_unit_adjustment_preserves_other_units_and_output_layer(self):
        source = {"layers": [
            {"inputSize": 2, "outputSize": 2, "activation": "tanh",
             "weights": [1., 2., 3., 4.], "bias": [0.1, 0.2]},
            {"inputSize": 2, "outputSize": 2, "activation": "linear",
             "weights": [5., 6., 7., 8.], "bias": [0.3, 0.4]}]}
        candidate = adjusted_hidden_unit(source, 0, 1, np.log(1.2), 0.05)
        np.testing.assert_allclose(candidate["layers"][0]["weights"], [1, 2, 3.6, 4.8])
        np.testing.assert_allclose(candidate["layers"][0]["bias"], [0.1, 0.29])
        self.assertEqual(candidate["layers"][1], source["layers"][1])
        self.assertEqual(source["layers"][0]["weights"], [1., 2., 3., 4.])
        self.assertEqual(source["layers"][0]["bias"], [0.1, 0.2])
        with self.assertRaises(ValueError):
            adjusted_hidden_unit(source, 1, 0)
        with self.assertRaises(ValueError):
            adjusted_hidden_unit(source, 0, 2)
        with self.assertRaises(ValueError):
            adjusted_hidden_unit(source, 0, 0, offset=float("nan"))

    def test_existing_failure_cannot_be_traded_for_a_new_failure(self):
        baseline = np.array([[1, 35, 0.04], [0, 90, 0.9]])
        swapped = np.array([[0, 90, 0.9], [1, 30, 0.03]])
        self.assertFalse(passes_regression_limits(swapped, baseline, allow_existing_failures=True))
        fixed = np.array([[1, 34, 0.03], [1, 30, 0.03]])
        self.assertTrue(passes_regression_limits(fixed, baseline, allow_existing_failures=True))
        unchanged_failure = np.array([[1, 34, 0.03], [0, 90, 0.9]])
        self.assertTrue(passes_regression_limits(unchanged_failure, baseline, allow_existing_failures=True))
        self.assertFalse(passes_regression_limits(unchanged_failure, baseline))

    def test_fixing_failure_does_not_mask_regression_on_completed_maps(self):
        baseline = np.array([[1, 35, 0.04], [0, 90, 0.9]])
        candidate = np.array([[1, 36, 0.04], [1, 30, 0.03]])
        self.assertFalse(passes_regression_limits(candidate, baseline, 0.05,
                                                 allow_existing_failures=True))

    def test_calibration_is_folded_into_linear_outputs_without_mutating_source(self):
        source = {"actionSize": 2, "layers": [{"inputSize": 2, "outputSize": 4,
                  "activation": "linear", "weights": [1., 2., 3., 4., 5., 6., 7., 8.],
                  "bias": [0.1, 0.2, 0.3, 0.4]}]}
        params = np.array([np.log(1.5), np.log(0.8), -0.1, 0.2])
        modified = calibrated_policy(source, params)
        inputs = np.array([0.2, -0.3])
        original_outputs = np.asarray(source["layers"][0]["weights"]).reshape(4, 2) @ inputs
        original_outputs += source["layers"][0]["bias"]
        layer = modified["layers"][0]
        outputs = np.asarray(layer["weights"]).reshape(4, 2) @ inputs + layer["bias"]
        np.testing.assert_allclose(outputs[:2], original_outputs[:2] * [1.5, 0.8] + [-0.1, 0.2])
        np.testing.assert_allclose(outputs[2:], original_outputs[2:])
        self.assertEqual(source["layers"][0]["weights"], [1., 2., 3., 4., 5., 6., 7., 8.])

    def test_rejects_nonlinear_output_layer(self):
        with self.assertRaises(ValueError):
            calibrated_policy({"actionSize": 2, "layers": [{"activation": "tanh"}]}, np.zeros(4))

    def test_faster_average_cannot_hide_failed_map(self):
        baseline = np.array([[1, 35, 0.04], [1, 40, 0.04]])
        candidate = np.array([[1, 34, 0.03], [0, 30, 0.03]])
        self.assertFalse(passes_regression_limits(candidate, baseline))

    def test_faster_average_cannot_hide_large_single_map_regression(self):
        baseline = np.array([[1, 35, 0.04], [1, 40, 0.04]])
        self.assertFalse(passes_regression_limits(np.array([[1, 38, 0.03], [1, 30, 0.03]]), baseline))
        self.assertTrue(passes_regression_limits(np.array([[1, 35.2, 0.03], [1, 39, 0.03]]), baseline))

    def test_longer_races_cannot_regress_in_average_or_leave_road_more(self):
        baseline = np.array([[1, 35, 0.04], [1, 40, 0.04]])
        self.assertFalse(passes_regression_limits(np.array([[1, 35.2, 0.03], [1, 40, 0.03]]), baseline, 0.05))
        self.assertFalse(passes_regression_limits(np.array([[1, 34, 0.07], [1, 39, 0.07]]), baseline, 0.05))


if __name__ == "__main__":
    unittest.main()
