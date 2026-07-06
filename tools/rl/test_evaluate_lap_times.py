#!/usr/bin/env python3

import io
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from evaluate_lap_times import (
    TimedRun,
    best_times,
    car_average_row,
    highlight,
    load_car_names,
    overall_car_averages,
    overall_profile_averages,
    print_overall_car_averages,
    print_overall_profile_averages,
    print_table,
    selected_cars,
)


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


class CarAverageTest(unittest.TestCase):
    def test_averages_fastest_laps_only(self):
        rows = [
            TimedRun("map000", "expert", "1", 10.0, 12.0, 60.0, 5, 5),
            TimedRun("map000", "expert", "2", 14.0, 18.0, 90.0, 5, 5),
        ]

        average = car_average_row("map000", "expert", rows)

        self.assertEqual(average.car, "avg")
        self.assertEqual(average.fastest_lap, 12.0)
        self.assertEqual(average.avg_lap, 15.0)
        self.assertEqual(average.total_time, 75.0)
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
        self.assertTrue(any("| avg |" in line for line in lines))

    def test_table_omits_car_column_for_default_physics(self):
        rows = [TimedRun("map000", "expert", "default", 10.0, 12.0, 60.0, 5, 5)]
        output = io.StringIO()

        with redirect_stdout(output):
            print_table(rows, group_by_map=False)

        header = output.getvalue().splitlines()[0]
        self.assertIn("| map    | profile |", header)
        self.assertNotIn("| car", header)

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
            TimedRun("map000", "aggressive", "default", 10.0, 12.0, 60.0, 5, 5),
            TimedRun("map001", "aggressive", "default", 14.0, 16.0, 80.0, 5, 5),
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

    def test_requires_every_map_and_prints_requested_columns(self):
        rows = [
            TimedRun("map000", "aggressive", "default", 10.0, 12.0, 60.0, 5, 5),
            TimedRun("map001", "aggressive", "default", None, None, None, 0, 5),
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
        self.assertIn("DNF 1/2", rendered)


if __name__ == "__main__":
    unittest.main()
