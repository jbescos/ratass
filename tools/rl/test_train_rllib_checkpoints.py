#!/usr/bin/env python3

import json
import sys
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

import train_rllib


class RestoreAlgorithmCheckpointTest(unittest.TestCase):
    def test_restores_learner_and_synchronizes_inference_workers(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            checkpoint_dir = Path(temp_dir)
            (checkpoint_dir / "learner_group").mkdir()
            algorithm = SimpleNamespace(
                restore=Mock(),
                learner_group=Mock(),
                env_runner_group=Mock(),
                eval_env_runner_group=Mock(),
            )

            train_rllib.restore_algorithm_checkpoint(algorithm, checkpoint_dir)

            algorithm.restore.assert_called_once_with(str(checkpoint_dir.resolve()))
            algorithm.learner_group.restore_from_path.assert_called_once_with(
                str((checkpoint_dir / "learner_group").resolve())
            )
            algorithm.env_runner_group.sync_weights.assert_called_once_with(
                from_worker_or_learner_group=algorithm.learner_group,
                inference_only=True,
            )
            algorithm.eval_env_runner_group.sync_weights.assert_called_once_with(
                from_worker_or_learner_group=algorithm.learner_group,
                inference_only=True,
            )

    def test_legacy_checkpoint_uses_algorithm_restore_only(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            checkpoint_dir = Path(temp_dir)
            algorithm = SimpleNamespace(restore=Mock())

            train_rllib.restore_algorithm_checkpoint(algorithm, checkpoint_dir)

            algorithm.restore.assert_called_once_with(str(checkpoint_dir.resolve()))


class EstablishStageBaselineTest(unittest.TestCase):
    def test_evaluates_incoming_policy_without_comparing_installed_policy(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            checkpoint_dir = Path(temp_dir)
            args = SimpleNamespace(best_export_output="policy.json", best_eval_state="")
            algorithm = SimpleNamespace(save=Mock(return_value=str(checkpoint_dir)))
            promotion = {
                "evaluated": True,
                "accepted": True,
                "promoted": True,
                "score": 12.5,
                "previous_score": float("-inf"),
            }

            with patch.object(
                train_rllib,
                "maybe_promote_best_policy",
                return_value=promotion,
            ) as promote:
                result = train_rllib.establish_stage_baseline(
                    algorithm,
                    args,
                    checkpoint_dir,
                )

            self.assertEqual(promotion, result)
            algorithm.save.assert_called_once_with(str(checkpoint_dir))
            promote.assert_called_once_with(
                algorithm,
                args,
                checkpoint_dir,
                0,
                str(checkpoint_dir),
                compare_installed=False,
            )

    def test_existing_stage_restores_best_without_reevaluating(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            checkpoint_dir = Path(temp_dir)
            archived_checkpoint = checkpoint_dir / "archived"
            archived_checkpoint.mkdir()
            (archived_checkpoint / "rllib_checkpoint.json").write_text(
                "{}\n",
                encoding="utf-8",
            )
            state_path = checkpoint_dir / "stage" / "best_policy.json"
            state_path.parent.mkdir()
            state_path.write_text(
                json.dumps(
                    {
                        "best_score": 42.0,
                        "iteration": 7,
                        "best_rllib_checkpoint": str(archived_checkpoint),
                    }
                ),
                encoding="utf-8",
            )
            args = SimpleNamespace(
                best_export_output="policy.json",
                best_eval_state=str(state_path),
            )
            algorithm = SimpleNamespace(save=Mock(return_value=str(checkpoint_dir)))

            with (
                patch.object(train_rllib, "restore_algorithm_checkpoint") as restore,
                patch.object(train_rllib, "maybe_promote_best_policy") as promote,
            ):
                train_rllib.establish_stage_baseline(algorithm, args, checkpoint_dir)

            restore.assert_called_once_with(algorithm, archived_checkpoint.resolve())
            algorithm.save.assert_called_once_with(str(checkpoint_dir))
            promote.assert_not_called()


def evaluated_candidate(iteration, score, avg_targets, route_eligible=True, return_code=0):
    checkpoint = train_rllib.CheckpointCandidate(
        iteration=iteration,
        reward_mean=float(iteration),
        episode_len_mean=100.0,
        episodes=1.0,
        checkpoint_path=f"checkpoint-{iteration}",
    )
    evaluation = train_rllib.PolicyEvaluation(
        score=score,
        metrics={"avg_targets": str(avg_targets)} if score is not None else {},
        output_lines=(f"evaluation_score={score}",) if score is not None else (),
        return_code=return_code,
    )
    return train_rllib.EvaluatedCheckpointCandidate(
        candidate=checkpoint,
        evaluation=evaluation,
        avg_targets=avg_targets,
        route_eligible=route_eligible,
    )


class EvaluatedCheckpointSelectionTest(unittest.TestCase):
    def test_selects_highest_all_map_score_from_route_eligible_candidates(self):
        selection = train_rllib.select_evaluated_checkpoint_candidate([
            evaluated_candidate(41, 150.0, 0.9, route_eligible=False),
            evaluated_candidate(42, 120.0, 1.0),
            evaluated_candidate(43, 130.0, 1.0),
        ])

        self.assertEqual(43, selection.evaluated_candidate.candidate.iteration)
        self.assertEqual(2, selection.eligible_count)
        self.assertEqual(3, selection.evaluated_count)
        self.assertEqual("highest_all_maps_evaluation_score", selection.reason)

    def test_falls_back_to_latest_when_every_evaluation_failed(self):
        selection = train_rllib.select_evaluated_checkpoint_candidate([
            evaluated_candidate(49, None, float("nan"), False, return_code=1),
            evaluated_candidate(50, None, float("nan"), False, return_code=1),
        ])

        self.assertEqual(50, selection.evaluated_candidate.candidate.iteration)
        self.assertEqual(0, selection.evaluated_count)
        self.assertEqual(2, selection.failed_count)
        self.assertEqual("all_candidate_evaluations_failed", selection.reason)

    def test_evaluates_every_checkpoint_without_emitting_loser_output(self):
        args = SimpleNamespace(
            best_export_objective="",
            objective="race",
            hidden_activation="tanh",
            best_eval_min_route_targets=1.0,
        )
        candidates = [
            train_rllib.CheckpointCandidate(
                iteration=iteration,
                reward_mean=float(iteration),
                episode_len_mean=100.0,
                episodes=1.0,
                checkpoint_path=f"checkpoint-{iteration}",
            )
            for iteration in range(41, 51)
        ]

        def evaluation_for_candidate(unused_args, policy_path, emit_output=True):
            iteration = int(policy_path.stem.split("-")[-1])
            self.assertFalse(emit_output)
            return train_rllib.PolicyEvaluation(
                score=float(iteration),
                metrics={"avg_targets": "1.0"},
                output_lines=(f"evaluation_score={iteration}",),
                return_code=0,
            )

        with (
            tempfile.TemporaryDirectory() as temp_dir,
            patch.object(train_rllib, "export_checkpoint_policy") as export,
            patch.object(
                train_rllib,
                "run_policy_evaluation",
                side_effect=evaluation_for_candidate,
            ) as evaluate,
        ):
            results = train_rllib.evaluate_checkpoint_candidates(
                args,
                candidates,
                Path(temp_dir),
            )

        self.assertEqual(10, len(results))
        self.assertEqual(10, export.call_count)
        self.assertEqual(10, evaluate.call_count)
        self.assertTrue(all(
            call.kwargs["emit_summary"] is False
            for call in export.call_args_list
        ))


if __name__ == "__main__":
    unittest.main()
