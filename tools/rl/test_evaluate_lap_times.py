#!/usr/bin/env python3

import io
import json
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

from evaluate_lap_times import (
    DRIVER_METADATA_SCHEMA_VERSION,
    TimedRun,
    best_times,
    car_average_row,
    driver_metadata,
    format_cell,
    highlight,
    load_car_names,
    make_environment,
    overall_car_averages,
    overall_profile_averages,
    parse_args,
    profile_timing_args,
    print_overall_car_averages,
    print_overall_profile_averages,
    print_table,
    run_lap_timing,
    selected_cars,
    write_driver_metadata,
)


class ProfileCadenceTest(unittest.TestCase):
    def test_default_uses_runtime_cadence(self):
        with patch.object(sys, "argv", ["evaluate_lap_times.py"]):
            self.assertEqual(parse_args().action_repeat, 0)

    def test_profiles_resolve_independently_without_changing_shared_args(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "profile10").mkdir()
            (root / "default.properties").write_text("RL_ACTION_REPEAT=4\n")
            (root / "profile10" / "profile.properties").write_text("RL_ACTION_REPEAT=2\n")
            # Poisoned cached results must never affect simulation configuration.
            (root / "profile10" / "driver_metadata.json").write_text(
                '{"actionRepeat": 99, "averageLapSeconds": 0.001}')
            args = SimpleNamespace(action_repeat=0, driver_action_repeat=0)
            fast = profile_timing_args(args, root, "profile10")
            normal = profile_timing_args(args, root, "profile08")
            self.assertEqual((fast.action_repeat, fast.driver_action_repeat), (2, 2))
            self.assertEqual((normal.action_repeat, normal.driver_action_repeat), (4, 4))
            self.assertEqual(args.action_repeat, 0)
            self.assertEqual(args.driver_action_repeat, 0)

    def test_explicit_benchmark_override_preserves_runtime_cadence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "profile10").mkdir()
            (root / "profile10" / "profile.properties").write_text("RL_ACTION_REPEAT=2\n")
            args = SimpleNamespace(action_repeat=4, driver_action_repeat=0)
            resolved = profile_timing_args(args, root, "profile10")
            self.assertEqual((resolved.action_repeat, resolved.driver_action_repeat), (4, 2))

    def test_new_driver_runtime_override_also_sets_benchmark(self):
        with tempfile.TemporaryDirectory() as directory:
            args = SimpleNamespace(action_repeat=0, driver_action_repeat=2)
            resolved = profile_timing_args(args, Path(directory), "new")
            self.assertEqual((resolved.action_repeat, resolved.driver_action_repeat), (2, 2))

    def test_inherits_default_profile_settings(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "profile10").mkdir()
            args = SimpleNamespace(action_repeat=0, driver_action_repeat=0)
            (root / "default.properties").write_text("RL_ACTION_REPEAT=3\n")
            (root / "profile10" / "profile.properties").write_text("RL_DRIVER_TIER=4\n")
            self.assertEqual(profile_timing_args(args, root, "profile10").action_repeat, 3)

    def test_invalid_profile_cadence_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "default.properties"
            args = SimpleNamespace(action_repeat=0, driver_action_repeat=0)
            for value in ("0", "-2", "invalid"):
                path.write_text(f"RL_ACTION_REPEAT={value}\n")
                with self.subTest(value=value), self.assertRaises(ValueError):
                    profile_timing_args(args, root, "profile10")

    def test_negative_override_rejected(self):
        for benchmark, runtime in ((-1, 0), (0, -1)):
            with self.assertRaises(ValueError):
                profile_timing_args(SimpleNamespace(action_repeat=benchmark,
                    driver_action_repeat=runtime), Path("unused"), "profile10")

    def test_metadata_records_resolved_benchmark_and_runtime_cadence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for profile, repeat in (("profile08", 4), ("profile10", 2)):
                folder = root / profile
                folder.mkdir()
                (folder / "rl_enemy_policy.json").write_text("{}")
                (folder / "profile.properties").write_text(f"RL_ACTION_REPEAT={repeat}\n")
            args = SimpleNamespace(action_repeat=0, driver_action_repeat=0,
                                   laps=3, seed=1, map_source="game", driver_tier=0,
                                   profile_root=str(root))
            rows = [TimedRun("map000", profile, "default", 33, 34, 102, 3, 3)
                    for profile in ("profile08", "profile10")]
            with patch("sys.stderr", new_callable=io.StringIO):
                write_driver_metadata(rows, ("profile08", "profile10"), root, args)
            for profile, repeat in (("profile08", 4), ("profile10", 2)):
                metadata = json.loads((root / profile / "driver_metadata.json").read_text())
                self.assertEqual(metadata["actionRepeat"], repeat)
                self.assertIn(f"repeat={repeat}", metadata["benchmarkVersion"])


class LapTimingEnvironmentTest(unittest.TestCase):
    def test_arbitrary_benchmark_stats_are_forwarded_in_attribute_order(self):
        config = MagicMock()
        for method in ("withControlledAgentCount", "withFieldSize", "withActionRepeat",
                       "withMaxActionSteps", "withNoProgressMaxActionSteps",
                       "withOffRoadFailureMaxActionSteps", "withRouteTargets", "withRaceMode",
                       "withRandomRaceSpawns", "withRewardBreakdownEnabled", "withStepDetailsEnabled",
                       "withDebugTraceEnabled", "withSeed"):
            getattr(config, method).return_value = config
        game = MagicMock()
        game.RlTrainingConfig.return_value = config
        args = SimpleNamespace(steps=9000, action_repeat=4, random_race_spawns=False,
                               seed=1, benchmark_stats=[5.0, 0.1, 2.2, 0.4])
        make_environment(args, game, object(), 3, None)
        config.withBenchmarkStats.assert_called_once_with(5.0, 0.1, 2.2, 0.4)
        config.withDebugTraceEnabled.assert_called_once_with(False)
        config.withBenchmarkTuningCard.assert_not_called()

    def test_disables_training_only_episode_failures(self):
        class FakeConfig:
            def __init__(self):
                self.values = {}

            def __getattr__(self, name):
                def record(value):
                    self.values[name] = value
                    return self

                return record

        class FakeRatassGame:
            def __init__(self):
                self.config = FakeConfig()

            def RlTrainingConfig(self):
                return self.config

            @staticmethod
            def RlTrainingEnvironment(config):
                return config

        ratass_game = FakeRatassGame()
        args = SimpleNamespace(
            steps=0,
            action_repeat=4,
            random_race_spawns=False,
            seed=1,
            tuning_card="AERO_TRIM",
            technique_card="",
        )

        config = make_environment(args, ratass_game, object(), 5, None)

        self.assertEqual(config.values["withNoProgressMaxActionSteps"], 0)
        self.assertEqual(config.values["withOffRoadFailureMaxActionSteps"], 0)
        self.assertTrue(config.values["withRewardBreakdownEnabled"])
        self.assertEqual(config.values["withBenchmarkTuningCard"], "AERO_TRIM")

    def test_equips_one_technique_card_for_benchmarking(self):
        class FakeConfig:
            def __init__(self):
                self.values = {}

            def __getattr__(self, name):
                def record(value):
                    self.values[name] = value
                    return self

                return record

        class FakeRatassGame:
            def __init__(self):
                self.config = FakeConfig()

            def RlTrainingConfig(self):
                return self.config

            @staticmethod
            def RlTrainingEnvironment(config):
                return config

        ratass_game = FakeRatassGame()
        args = SimpleNamespace(
            steps=0,
            action_repeat=4,
            random_race_spawns=False,
            seed=1,
            technique_card="STRAIGHT_FOCUS",
        )

        config = make_environment(args, ratass_game, object(), 3, None)

        self.assertEqual(
            config.values["withBenchmarkTechniqueCard"], "STRAIGHT_FOCUS"
        )

    def test_wall_timeout_preserves_completed_laps(self):
        class FakeEnvironment:
            def __init__(self):
                self.closed = False

            @staticmethod
            def reset():
                return SimpleNamespace(
                    episodeDone=False,
                    actionStep=0,
                    observations=[0.0],
                    routeTargetsReached=[0],
                    rewardBreakdownNames=["off_road"],
                    rewardBreakdown=[0.0],
                )

            @staticmethod
            def getObservationSize():
                return 1

            @staticmethod
            def step(_actions):
                return SimpleNamespace(
                    episodeDone=False,
                    actionStep=1,
                    observations=[0.0],
                    routeTargetsReached=[1],
                    rewardBreakdownNames=["off_road"],
                    rewardBreakdown=[-1.0],
                )

            def close(self):
                self.closed = True

        class FakePolicy:
            @staticmethod
            def getObservationSize():
                return 1

            @staticmethod
            def getScratchSize():
                return 1

            @staticmethod
            def computeAction(*_args):
                return SimpleNamespace(throttle=0.0, turn=0.0)

        class FakeJpype:
            JFloat = float

            @staticmethod
            def JClass(name):
                if name.endswith("AiControlDecision"):
                    return SimpleNamespace
                return object()

            @staticmethod
            def JArray(_type):
                return lambda size: [0.0] * size

        arena_map = SimpleNamespace(getId=lambda: "map013")
        environment = FakeEnvironment()
        args = SimpleNamespace(
            laps=5,
            timeout_seconds=1.0,
            steps=0,
            action_repeat=4,
        )

        with (
            patch("evaluate_lap_times.jpype", FakeJpype()),
            patch("evaluate_lap_times.make_environment", return_value=environment),
            patch("evaluate_lap_times.time.monotonic", side_effect=[0.0, 0.0, 2.0]),
        ):
            result = run_lap_timing(
                args,
                arena_map,
                "clean",
                "default",
                None,
                FakePolicy(),
            )

        self.assertEqual(result.completed_laps, 1)
        self.assertIsNotNone(result.fastest_lap)
        self.assertIsNotNone(result.avg_lap)
        self.assertIsNone(result.total_time)
        self.assertEqual(result.off_road_actions, 1)
        self.assertEqual(result.error, "")
        self.assertTrue(environment.closed)


class CarSelectionTest(unittest.TestCase):
    def test_all_cars_are_one_based_labels_with_zero_based_indices(self):
        self.assertEqual(
            selected_cars("all", 3),
            [("1", 0), ("2", 1), ("3", 2)],
        )

    def test_explicit_cars_preserve_order_and_remove_duplicates(self):
        self.assertEqual(
            selected_cars("3,1,3", 3),
            [("3", 2), ("1", 0)],
        )

    def test_uses_names_from_car_properties(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "00.properties").write_text("name=Falcon GT\n", encoding="utf-8")
            (root / "01.properties").write_text("name=Comet RS\n", encoding="utf-8")

            names = load_car_names(root, 3)

        self.assertEqual(names, ["Falcon GT", "Comet RS", "3"])
        self.assertEqual(
            selected_cars("all", 3, names),
            [("Falcon GT", 0), ("Comet RS", 1), ("3", 2)],
        )


class DriverMetadataTest(unittest.TestCase):
    def test_builds_stable_ratings_and_policy_hash_from_benchmark_rows(self):
        rows = [
            TimedRun(
                "map000",
                "profile03",
                "default",
                30.0,
                31.0,
                93.0,
                3,
                3,
                off_road_actions=6,
                average_drift_percent=12.0,
                maximum_speed_kph=241.5,
                off_road_samples=10,
                driving_samples=100,
                drift_samples=20,
            ),
            TimedRun(
                "map001",
                "profile03",
                "default",
                34.0,
                35.0,
                None,
                2,
                3,
                off_road_actions=4,
                average_drift_percent=8.0,
                maximum_speed_kph=263.25,
                off_road_samples=20,
                driving_samples=300,
                drift_samples=60,
            ),
        ]
        with tempfile.TemporaryDirectory() as directory:
            policy = Path(directory) / "rl_enemy_policy.json"
            policy.write_text('{"policy":"test"}', encoding="utf-8")

            metadata = driver_metadata(
                "profile03",
                rows,
                policy,
                laps=3,
                action_repeat=4,
                driver_action_repeat=2,
                seed=7,
                map_source="game",
            )

        self.assertEqual(metadata["schemaVersion"], DRIVER_METADATA_SCHEMA_VERSION)
        self.assertEqual(metadata["profileId"], "profile03")
        self.assertEqual(metadata["finishRate"], 0.5)
        self.assertEqual(metadata["averageOffRoadActions"], 2.0)
        self.assertEqual(metadata["averageOffRoadPercent"], 7.5)
        self.assertEqual(metadata["averageDriftPercent"], 9.0)
        self.assertEqual(metadata["maximumSpeedKph"], 263.25)
        self.assertEqual(metadata["actionRepeat"], 2)
        self.assertIn("repeat=4", metadata["benchmarkVersion"])
        self.assertEqual(len(metadata["policySha256"]), 64)
        self.assertNotIn("overallRating", metadata)


class CarAverageTest(unittest.TestCase):
    def test_timeout_still_displays_off_road_counter(self):
        row = TimedRun(
            "map000",
            "expert",
            "default",
            None,
            None,
            None,
            0,
            5,
            "timedout",
            off_road_actions=17,
        )

        self.assertEqual(
            format_cell(row, "off_road_actions", best_times([row], "off_road_actions")),
            "17",
        )

    def test_averages_fastest_laps_only(self):
        rows = [
            TimedRun(
                "map000", "expert", "1", 10.0, 12.0, 60.0, 5, 5, off_road_actions=2
            ),
            TimedRun(
                "map000", "expert", "2", 14.0, 18.0, 90.0, 5, 5, off_road_actions=6
            ),
        ]

        average = car_average_row("map000", "expert", rows)

        self.assertEqual(average.car, "avg")
        self.assertEqual(average.fastest_lap, 12.0)
        self.assertEqual(average.avg_lap, 15.0)
        self.assertEqual(average.total_time, 75.0)
        self.assertEqual(average.off_road_actions, 4.0)
        self.assertTrue(average.complete)

    def test_requires_every_car_to_complete(self):
        rows = [
            TimedRun("map000", "expert", "1", 10.0, 12.0, 60.0, 5, 5),
            TimedRun("map000", "expert", "2", 14.0, 18.0, None, 3, 5),
        ]

        average = car_average_row("map000", "expert", rows)

        self.assertIsNone(average.fastest_lap)
        self.assertFalse(average.complete)

    def test_table_places_car_after_profile_and_labels_average(self):
        rows = [
            TimedRun("map000", "expert", "1", 10.0, 12.0, 60.0, 5, 5),
            TimedRun(
                "map000",
                "expert",
                "avg",
                10.0,
                12.0,
                60.0,
                1,
                1,
                is_car_average=True,
            ),
        ]
        output = io.StringIO()

        with redirect_stdout(output):
            print_table(rows, group_by_map=False)

        lines = output.getvalue().splitlines()
        self.assertIn("| map    | profile | car", lines[0])
        self.assertIn("off-road actions", lines[0])
        self.assertTrue(any("| avg |" in line for line in lines))

    def test_table_omits_car_column_for_default_physics(self):
        rows = [TimedRun("map000", "expert", "default", 10.0, 12.0, 60.0, 5, 5)]
        output = io.StringIO()

        with redirect_stdout(output):
            print_table(rows, group_by_map=False)

        header = output.getvalue().splitlines()[0]
        self.assertIn("| map    | profile |", header)
        self.assertNotIn("| car", header)
        self.assertIn("off-road actions", header)

    def test_map_best_and_profile_best_have_distinct_markers(self):
        rows = [
            TimedRun("map000", "expert", "1", 10.0, 12.0, 60.0, 5, 5),
            TimedRun("map000", "expert", "2", 11.0, 13.0, 65.0, 5, 5),
            TimedRun("map000", "rookie", "1", 12.0, 14.0, 70.0, 5, 5),
            TimedRun("map000", "rookie", "2", 13.0, 15.0, 75.0, 5, 5),
        ]
        fastest = best_times(rows, "fastest_lap")

        self.assertEqual(
            highlight("10.000", rows[0], "fastest_lap", fastest),
            "**++10.000++**",
        )
        self.assertEqual(
            highlight("12.000", rows[2], "fastest_lap", fastest),
            "++12.000++",
        )
        self.assertEqual(
            highlight("11.000", rows[1], "fastest_lap", fastest),
            "11.000",
        )

    def test_default_physics_marks_map_best_and_worst_without_plus_markers(self):
        rows = [
            TimedRun("map000", "expert", "default", 10.0, 12.0, 60.0, 5, 5),
            TimedRun("map000", "rookie", "default", 14.0, 16.0, 80.0, 5, 5),
        ]
        fastest = best_times(rows, "fastest_lap")

        self.assertEqual(
            highlight("10.000", rows[0], "fastest_lap", fastest),
            "**10.000**",
        )
        self.assertEqual(
            highlight("14.000", rows[1], "fastest_lap", fastest),
            "--14.000--",
        )


class OverallCarAverageTest(unittest.TestCase):
    def test_averages_each_car_across_maps_and_profiles(self):
        rows = [
            TimedRun("map000", "expert", "Car 1", 10.0, 12.0, 60.0, 5, 5),
            TimedRun("map001", "expert", "Car 1", 14.0, 16.0, 80.0, 5, 5),
            TimedRun("map000", "rookie", "Car 2", 20.0, 22.0, 110.0, 5, 5),
            TimedRun("map001", "rookie", "Car 2", 24.0, 26.0, 130.0, 5, 5),
            TimedRun(
                "map001",
                "rookie",
                "avg",
                19.0,
                21.0,
                105.0,
                2,
                2,
                is_car_average=True,
            ),
        ]

        averages = overall_car_averages(rows)

        self.assertEqual([average.car for average in averages], ["Car 1", "Car 2"])
        self.assertEqual(averages[0].fastest_lap, 12.0)
        self.assertEqual(averages[0].avg_lap, 14.0)
        self.assertEqual(averages[0].total_time, 70.0)
        self.assertEqual(averages[0].completed_runs, 2)
        self.assertEqual(averages[0].expected_runs, 2)

    def test_reports_completion_ratio_and_averages_only_completed_runs(self):
        rows = [
            TimedRun("map000", "expert", "Car 1", 10.0, 12.0, 60.0, 5, 5),
            TimedRun("map001", "expert", "Car 1", None, None, None, 0, 5),
        ]
        output = io.StringIO()

        with redirect_stdout(output):
            print_overall_car_averages(rows)

        rendered = output.getvalue()
        self.assertIn("all_maps_car_average", rendered)
        self.assertIn("| Car 1 |       1/2 |", rendered)
        self.assertIn("**10.000**", rendered)


class OverallProfileAverageTest(unittest.TestCase):
    def test_averages_each_profile_across_maps(self):
        rows = [
            TimedRun(
                "map000",
                "aggressive",
                "default",
                10.0,
                12.0,
                60.0,
                5,
                5,
                off_road_actions=4,
            ),
            TimedRun(
                "map001",
                "aggressive",
                "default",
                14.0,
                16.0,
                80.0,
                5,
                5,
                off_road_actions=8,
            ),
            TimedRun("map000", "clean", "default", 20.0, 22.0, 110.0, 5, 5),
            TimedRun("map001", "clean", "default", 24.0, 26.0, 130.0, 5, 5),
        ]

        averages = overall_profile_averages(rows)

        self.assertEqual(
            [average.profile for average in averages],
            ["aggressive", "clean"],
        )
        self.assertEqual(averages[0].fastest_lap, 12.0)
        self.assertEqual(averages[0].avg_lap, 14.0)
        self.assertEqual(averages[0].total_time, 70.0)
        self.assertEqual(averages[0].off_road_actions, 6.0)

    def test_keeps_available_lap_averages_when_a_map_does_not_finish(self):
        rows = [
            TimedRun("map000", "aggressive", "default", 10.0, 12.0, 60.0, 5, 5),
            TimedRun("map001", "aggressive", "default", 14.0, 16.0, None, 1, 5),
        ]
        output = io.StringIO()

        with redirect_stdout(output):
            print_overall_profile_averages(rows)

        rendered = output.getvalue()
        self.assertIn("all_maps_profile_average", rendered)
        self.assertIn("| profile", rendered)
        self.assertIn("avg fastest lap", rendered)
        self.assertIn("avg lap", rendered)
        self.assertIn("avg total time", rendered)
        self.assertIn("avg off-road actions", rendered)
        self.assertIn("**12.000**", rendered)
        self.assertIn("**14.000**", rendered)
        self.assertIn("DNF 1/2", rendered)


if __name__ == "__main__":
    unittest.main()
