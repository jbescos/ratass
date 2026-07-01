#!/usr/bin/env python3

import math
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from checkpoint_candidates import (
    CheckpointCandidate,
    select_checkpoint_candidate,
    should_capture_checkpoint_candidate,
)


def candidate(iteration, reward, length, episodes=1):
    return CheckpointCandidate(iteration, reward, length, episodes, f"checkpoint-{iteration}")


class CheckpointCandidatesTest(unittest.TestCase):
    def test_captures_only_ten_iterations_before_each_checkpoint(self):
        captured = [
            iteration
            for iteration in range(1, 101)
            if should_capture_checkpoint_candidate(iteration, 100, 50)
        ]

        self.assertEqual(list(range(41, 51)) + list(range(91, 101)), captured)

    def test_final_partial_checkpoint_also_has_a_ten_iteration_window(self):
        captured = [
            iteration
            for iteration in range(191, 206)
            if should_capture_checkpoint_candidate(iteration, 205, 50)
        ]

        self.assertEqual(list(range(191, 206)), captured)

    def test_selects_highest_reward_even_when_episode_is_longer(self):
        selection = select_checkpoint_candidate([
            candidate(1, 100.0, 500.0),
            candidate(2, 90.0, 100.0),
            candidate(3, 80.0, 200.0),
        ])

        self.assertEqual(1, selection.candidate.iteration)
        self.assertEqual("highest_reward_mean", selection.reason)

    def test_equal_rewards_select_later_iteration(self):
        selection = select_checkpoint_candidate([
            candidate(9, 10.0, 50.0),
            candidate(10, 10.0, 100.0),
        ])

        self.assertEqual(10, selection.candidate.iteration)

    def test_ignores_iterations_without_completed_episode_metrics(self):
        selection = select_checkpoint_candidate([
            candidate(8, math.nan, math.nan, episodes=0),
            candidate(9, 15.0, math.nan),
            candidate(10, math.nan, math.nan, episodes=0),
        ])

        self.assertEqual(9, selection.candidate.iteration)
        self.assertEqual(1, selection.eligible_count)

    def test_falls_back_to_latest_when_no_iteration_completed_an_episode(self):
        selection = select_checkpoint_candidate([
            candidate(9, math.nan, math.nan, episodes=0),
            candidate(10, math.nan, math.nan, episodes=0),
        ])

        self.assertEqual(10, selection.candidate.iteration)
        self.assertEqual("no_completed_episodes", selection.reason)


if __name__ == "__main__":
    unittest.main()
