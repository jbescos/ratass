import json
import tempfile
import unittest
from pathlib import Path

from curriculum_gate import stage_passed


class CurriculumGateTest(unittest.TestCase):
    def test_requires_successful_evaluation_and_restorable_policy(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            state_path = root / "best.json"
            self.assertFalse(stage_passed(state_path, 1))
            archive = root / "policy.json"
            archive.touch()
            checkpoint = root / "checkpoint"
            checkpoint.mkdir()
            (checkpoint / "rllib_checkpoint.json").touch()
            state = {"archived_policy": str(archive), "best_rllib_checkpoint": str(checkpoint)}
            for success, targets, expected in [(0, 0, False), (0.947, 0.947, False),
                                               (1, 0.5, False), (1, 1, True),
                                               (float("nan"), 1, False)]:
                state["metrics"] = {"success_rate": success, "avg_targets": targets}
                state_path.write_text(json.dumps(state))
                self.assertEqual(expected, stage_passed(state_path, 1))
            state["metrics"] = {"success_rate": 1, "avg_targets": 3}
            state_path.write_text(json.dumps(state))
            self.assertTrue(stage_passed(state_path, 3))
            archive.unlink()
            self.assertFalse(stage_passed(state_path, 3))
            state_path.write_text("broken")
            self.assertFalse(stage_passed(state_path, 1))


if __name__ == "__main__":
    unittest.main()
