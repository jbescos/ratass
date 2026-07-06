#!/usr/bin/env python3

import sys
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

from evaluate_policy import evaluation_score, make_stats, select_maps, summary_metrics


class EvaluationMapLoadingTest(unittest.TestCase):
    def test_selected_maps_are_loaded_one_at_a_time(self):
        loaded_ids = []

        def load_map(_ratass_game, map_id):
            loaded_ids.append(map_id)
            return map_id

        with patch("evaluate_policy.select_map", side_effect=load_map):
            maps = select_maps(None, "map002,map000,map001")
            self.assertEqual([], loaded_ids)
            self.assertEqual("map002", next(maps))
            self.assertEqual(["map002"], loaded_ids)
            self.assertEqual(["map000", "map001"], list(maps))

        self.assertEqual(["map002", "map000", "map001"], loaded_ids)


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
