#!/usr/bin/env python3

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from evaluate_policy import evaluation_score, make_stats, summary_metrics


class EvaluationScoreTest(unittest.TestCase):
    def test_alignment_is_diagnostic_only(self):
        aligned = make_stats()
        aligned.update(
            episodes=1,
            actions=1,
            observation_samples=1,
            avg_route_alignment=1.0,
            avg_target_alignment=1.0,
        )
        drifting = dict(aligned)
        drifting["avg_route_alignment"] = -1.0
        drifting["avg_target_alignment"] = -1.0

        self.assertEqual(evaluation_score(aligned), evaluation_score(drifting))
        self.assertNotEqual(
            summary_metrics(aligned)["avg_route_alignment"],
            summary_metrics(drifting)["avg_route_alignment"],
        )
        self.assertNotEqual(
            summary_metrics(aligned)["avg_target_alignment"],
            summary_metrics(drifting)["avg_target_alignment"],
        )


if __name__ == "__main__":
    unittest.main()
