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


if __name__ == "__main__":
    unittest.main()
