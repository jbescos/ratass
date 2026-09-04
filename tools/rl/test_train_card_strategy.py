import unittest
from pathlib import Path
import sys

import torch

sys.path.insert(0, str(Path(__file__).resolve().parent))

from train_card_strategy import (
    CandidateScorer,
    candidate_state_mean,
    masked_candidate_logits,
    pad_candidate_observations,
)


class CandidateBatchTest(unittest.TestCase):
    def test_pads_variable_candidate_counts_without_changing_state_mean(self):
        observations = [
            torch.tensor([[1.0, 2.0], [3.0, 4.0], [5.0, 6.0]]),
            torch.tensor([[7.0, 8.0], [9.0, 10.0]]),
        ]

        batch, mask = pad_candidate_observations(observations)

        self.assertEqual((2, 3, 2), tuple(batch.shape))
        self.assertEqual([[True, True, True], [True, True, False]], mask.tolist())
        torch.testing.assert_close(
            candidate_state_mean(batch, mask),
            torch.tensor([[3.0, 4.0], [8.0, 9.0]]),
        )

    def test_masked_candidate_cannot_be_selected(self):
        actor = CandidateScorer(2, 4, 1)
        observations, mask = pad_candidate_observations(
            [torch.zeros((2, 2)), torch.zeros((3, 2))]
        )

        probabilities = torch.softmax(
            masked_candidate_logits(actor, observations, mask), dim=1
        )

        self.assertEqual(0.0, probabilities[0, 2].item())
        self.assertGreater(probabilities[0, :2].sum().item(), 0.999)


if __name__ == "__main__":
    unittest.main()
