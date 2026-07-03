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


if __name__ == "__main__":
    unittest.main()
