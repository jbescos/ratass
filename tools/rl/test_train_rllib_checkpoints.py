#!/usr/bin/env python3

import json
import sys
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import Mock, patch

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))

import train_rllib


class RayRuntimeConfigurationTest(unittest.TestCase):
    def test_standard_loopback_is_replaced_with_stable_loopback(self):
        self.assertEqual(
            "127.0.0.2",
            train_rllib.stable_ray_node_ip("127.0.0.1"),
        )
        self.assertEqual(
            "127.0.0.2",
            train_rllib.stable_ray_node_ip("localhost"),
        )

    def test_explicit_nonstandard_address_is_preserved(self):
        self.assertEqual(
            "10.20.30.40",
            train_rllib.stable_ray_node_ip("10.20.30.40"),
        )


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


class PpoRuntimeStateTest(unittest.TestCase):
    def test_checkpoint_records_adaptive_kl_coefficient(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            checkpoint_dir = Path(temp_dir)
            algorithm = SimpleNamespace(save=Mock(return_value=str(checkpoint_dir)))

            with patch.object(
                train_rllib,
                "current_algorithm_kl_coeff",
                return_value=1.25,
            ):
                saved_path = train_rllib.save_algorithm_checkpoint(
                    algorithm,
                    checkpoint_dir,
                )

            self.assertEqual(str(checkpoint_dir), saved_path)
            state = json.loads(
                (checkpoint_dir / train_rllib.PPO_RUNTIME_STATE_FILE).read_text(
                    encoding="utf-8"
                )
            )
            self.assertEqual(1.25, state["kl_coeff"])

    def test_restore_applies_recorded_kl_coefficient(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            checkpoint_dir = Path(temp_dir)
            (checkpoint_dir / train_rllib.PPO_RUNTIME_STATE_FILE).write_text(
                '{"kl_coeff": 0.75}\n',
                encoding="utf-8",
            )
            algorithm = SimpleNamespace()

            with patch.object(
                train_rllib,
                "set_algorithm_kl_coeff",
                return_value=True,
            ) as set_kl:
                train_rllib.restore_ppo_runtime_state(algorithm, checkpoint_dir)

            set_kl.assert_called_once_with(algorithm, 0.75)


class ExportedActorInitializationTest(unittest.TestCase):
    def test_exported_exploration_outputs_are_not_warm_started(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            policy_file = Path(temp_dir) / "policy.json"
            policy_file.write_text(
                json.dumps({
                    "observationSize": 2,
                    "actionSize": 1,
                    "layers": [
                        {
                            "inputSize": 2,
                            "outputSize": 2,
                            "weights": [1.0, 2.0, 3.0, 4.0],
                            "bias": [5.0, 6.0],
                        },
                        {
                            "inputSize": 2,
                            "outputSize": 2,
                            "weights": [7.0, 8.0, 90.0, 91.0],
                            "bias": [9.0, 92.0],
                        },
                    ],
                }),
                encoding="utf-8",
            )

            state = train_rllib.load_exported_actor_state(policy_file)

        output_weights = state[("pi.net.mlp.0.weight",)]
        output_bias = state[("pi.net.mlp.0.bias",)]
        np.testing.assert_array_equal(output_weights, [[7.0, 8.0]])
        np.testing.assert_array_equal(output_bias, [9.0])

    def test_state_dependent_mean_and_log_std_output_can_seed_free_log_std_mean(self):
        current = np.zeros((1, 4), dtype=np.float32)
        exported = np.asarray(
            [[1.0, 2.0, 3.0, 4.0], [9.0, 9.0, 9.0, 9.0]],
            dtype=np.float32,
        )

        copied, partial = train_rllib.copy_exported_actor_values(
            "pi.net.mlp.0.weight",
            current,
            exported,
        )

        np.testing.assert_array_equal(copied, exported[:1])
        self.assertTrue(partial)

    def test_unexported_exploration_head_starts_narrow_and_state_independent(self):
        weights = np.ones((4, 3), dtype=np.float32)
        bias = np.ones(4, dtype=np.float32)

        initialized_weights = train_rllib.initialize_unexported_exploration_values(
            "pi.net.mlp.0.weight",
            weights,
            deterministic_output_size=2,
            initial_log_std=-1.5,
        )
        initialized_bias = train_rllib.initialize_unexported_exploration_values(
            "pi.net.mlp.0.bias",
            bias,
            deterministic_output_size=2,
            initial_log_std=-1.5,
        )

        np.testing.assert_array_equal(initialized_weights[:2], np.ones((2, 3)))
        np.testing.assert_array_equal(initialized_weights[2:], np.zeros((2, 3)))
        np.testing.assert_array_equal(initialized_bias[:2], np.ones(2))
        np.testing.assert_array_equal(initialized_bias[2:], [-1.5, -1.5])

    def test_free_log_std_is_initialized_and_synchronized(self):
        learner_group = Mock()
        learner_group.get_weights.return_value = {
            "shared_policy": {
                "pi.log_std": np.zeros(1, dtype=np.float32),
                "pi.net.mlp.0.bias": np.zeros(1, dtype=np.float32),
            }
        }
        algorithm = SimpleNamespace(
            learner_group=learner_group,
            env_runner_group=Mock(),
        )

        train_rllib.initialize_free_log_std(algorithm, -1.5)

        updated = learner_group.set_weights.call_args.args[0]["shared_policy"]
        np.testing.assert_allclose(updated["pi.log_std"], [-1.5])
        algorithm.env_runner_group.sync_weights.assert_called_once()

class SpawnConfigurationValidationTest(unittest.TestCase):
    def test_fixed_full_laps_accepts_positive_targets_with_fixed_spawns(self):
        args = SimpleNamespace(
            random_race_spawns=False,
            fixed_full_laps=True,
            route_targets=5,
            route_target_fraction=0.0,
            controlled_agents=1,
        )
        parser = Mock()

        train_rllib.validate_spawn_configuration(args, parser)

        parser.error.assert_not_called()

    def test_regular_route_targets_still_reject_fixed_spawns(self):
        args = SimpleNamespace(
            random_race_spawns=False,
            fixed_full_laps=False,
            route_targets=5,
            route_target_fraction=0.0,
            controlled_agents=1,
        )
        parser = Mock()
        parser.error.side_effect = ValueError

        with self.assertRaises(ValueError):
            train_rllib.validate_spawn_configuration(args, parser)

        parser.error.assert_called_once()


class LapEvaluationEligibilityTest(unittest.TestCase):
    def test_fixed_lap_candidate_must_finish_every_evaluation(self):
        args = SimpleNamespace(objective="race", fixed_full_laps=True, route_targets=3)
        complete = train_rllib.PolicyEvaluation(
            score=-100.0,
            metrics={"success_rate": "1.000"},
            output_lines=(),
            return_code=0,
        )
        incomplete = train_rllib.PolicyEvaluation(
            score=-90.0,
            metrics={"success_rate": "0.947"},
            output_lines=(),
            return_code=0,
        )

        self.assertTrue(train_rllib.evaluation_completed_all_laps(args, complete))
        self.assertFalse(train_rllib.evaluation_completed_all_laps(args, incomplete))

    def test_percentage_stage_does_not_require_all_evaluations_to_finish(self):
        args = SimpleNamespace(objective="race", fixed_full_laps=False, route_targets=1)
        incomplete = train_rllib.PolicyEvaluation(
            score=10.0,
            metrics={"success_rate": "0.500"},
            output_lines=(),
            return_code=0,
        )

        self.assertTrue(train_rllib.evaluation_completed_all_laps(args, incomplete))


class EstablishStageBaselineTest(unittest.TestCase):
    def test_only_current_evaluation_score_state_is_reusable(self):
        self.assertFalse(train_rllib.state_uses_current_evaluation_score({}))
        self.assertFalse(
            train_rllib.state_uses_current_evaluation_score(
                {"metrics": {"score_version": "1"}}
            )
        )
        self.assertTrue(
            train_rllib.state_uses_current_evaluation_score(
                {"metrics": {"score_version": "4"}}
            )
        )

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
                        "metrics": {"score_version": "4"},
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


def evaluated_candidate(
        iteration,
        score,
        avg_targets,
        route_eligible=True,
        return_code=0,
        avg_goal_time_s=None):
    checkpoint = train_rllib.CheckpointCandidate(
        iteration=iteration,
        reward_mean=float(iteration),
        episode_len_mean=100.0,
        episodes=1.0,
        checkpoint_path=f"checkpoint-{iteration}",
    )
    evaluation = train_rllib.PolicyEvaluation(
        score=score,
        metrics=(
            {
                "avg_targets": str(avg_targets),
                **(
                    {"avg_goal_time_s": str(avg_goal_time_s)}
                    if avg_goal_time_s is not None
                    else {}
                ),
            }
            if score is not None
            else {}
        ),
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
