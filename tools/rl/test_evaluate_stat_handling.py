import itertools
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

from evaluate_stat_handling import main, scenarios


class StatHandlingScenariosTest(unittest.TestCase):
    def test_reproducible_cases_within_requested_range(self):
        cases = scenarios()
        self.assertEqual(cases, scenarios())
        self.assertEqual(155, len(cases))
        self.assertEqual([1.0] * 4, cases['baseline'])
        for stats in cases.values():
            self.assertEqual(4, len(stats))
            self.assertTrue(all(0.1 <= value <= 5 for value in stats))

    def test_covers_every_pair_and_all_extreme_combinations(self):
        cases = scenarios()
        pairs = [tuple(v) for k, v in cases.items() if k.startswith('pair-')]
        self.assertEqual(54, len(pairs))
        for first, second in itertools.combinations(range(4), 2):
            for a, b in itertools.product((0.1, 2.2, 5.0), repeat=2):
                stats = [1.0] * 4
                stats[first], stats[second] = a, b
                self.assertIn(tuple(stats), pairs)
        extremes = {tuple(v) for k, v in cases.items() if k.startswith('extreme-')}
        self.assertEqual(set(itertools.product((0.1, 5.0), repeat=4)), extremes)

    def test_retry_of_complete_report_does_not_start_workers(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / 'complete.jsonl'
            output = Path(directory) / 'retry.jsonl'
            report.write_text(json.dumps({'case': 'baseline', 'map_id': 'map000',
                                          'completed_laps': 3, 'expected_laps': 3, 'error': ''}) + '\n')
            with patch.object(sys, 'argv', ['evaluate_stat_handling.py', '--workers', '2',
                                           '--output', str(output), '--retry-incomplete', str(report)]), \
                    patch('evaluate_stat_handling.subprocess.Popen') as spawn:
                main()
            spawn.assert_not_called()
            self.assertEqual('', output.read_text())


if __name__ == '__main__':
    unittest.main()
