#!/usr/bin/env python3

import io
import sys
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

from evaluate_policy import (
    evaluation_score,
    make_stats,
    parse_args,
    print_evaluation_tables,
    select_maps,
    summary_metrics,
)


class EvaluationArgumentsTest(unittest.TestCase):
    def test_overtaking_ignores_random_route_spawn_flag(self):
        with patch.object(
            sys,
            "argv",
            ["evaluate_policy.py", "--objective", "overtaking", "--random-race-spawns"],
        ):
            args = parse_args()

        self.assertFalse(args.random_race_spawns)

    def test_race_keeps_random_route_spawn_flag(self):
        with patch.object(
            sys,
            "argv",
            ["evaluate_policy.py", "--objective", "race", "--random-race-spawns"],
        ):
            args = parse_args()

        self.assertTrue(args.random_race_spawns)


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

    def test_completed_lap_score_prefers_lower_goal_time(self):
        faster = make_stats()
        faster.update(
            episodes=1,
            successes=1,
            goal_time_count=1,
            goal_time_seconds=35.0,
        )
        slower = dict(faster)
        slower["goal_time_seconds"] = 35.1

        self.assertGreater(
            evaluation_score(faster, prefer_goal_time=True),
            evaluation_score(slower, prefer_goal_time=True),
        )
        self.assertEqual(-35.0, evaluation_score(faster, prefer_goal_time=True))
        self.assertEqual(evaluation_score(faster), evaluation_score(slower))

    def test_incomplete_lap_score_cannot_beat_a_completed_lap(self):
        complete = make_stats()
        complete.update(
            episodes=2,
            successes=2,
            goal_time_count=2,
            goal_time_seconds=80.0,
        )
        incomplete = dict(complete)
        incomplete.update(
            successes=1,
            goal_time_count=1,
            goal_time_seconds=30.0,
        )

        self.assertGreater(
            evaluation_score(complete, prefer_goal_time=True),
            evaluation_score(incomplete, prefer_goal_time=True),
        )

    def test_reward_table_includes_every_reward_bucket(self):
        output = io.StringIO()

        with redirect_stdout(output):
            print_evaluation_tables(make_stats(), {})

        self.assertIn("drift", output.getvalue())


if __name__ == "__main__":
    unittest.main()
