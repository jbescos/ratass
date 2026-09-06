import unittest
from pathlib import Path
import sys
from types import SimpleNamespace

import torch

sys.path.insert(0, str(Path(__file__).resolve().parent))

from train_card_strategy import (
    CandidateScorer,
    card_usage_payload,
    candidate_state_mean,
    count_occurrences,
    migrate_observation_weight_matrix,
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

    def test_migrates_each_growing_observation_block_without_shifting_features(self):
        old_weights = torch.arange(1, 868, dtype=torch.float32).reshape(1, 867)

        migrated = migrate_observation_weight_matrix(
            old_weights,
            observation_size=875,
            card_feature_count=135,
            set_feature_count=10,
        )

        self.assertEqual((1, 875), tuple(migrated.shape))
        retained = migrated[migrated != 0]
        torch.testing.assert_close(retained, old_weights.reshape(-1))
        self.assertEqual(8, int((migrated == 0).sum().item()))


class CardUsageReportTest(unittest.TestCase):
    def test_reports_unselected_catalog_cards_and_observed_drivers(self):
        metrics = SimpleNamespace(
            episodes=2,
            championships=2,
            offer_counts={"card:USED": 4, "card:UNUSED": 3},
            card_counts={"card:USED": 2},
            race_end_equipped_counts={"card:USED": 2, "driver:profile08": 2},
            activation_counts={"card:USED": 8, "driver:profile08": 8},
            card_types={"driver:profile08": "driver"},
            card_tiers={"driver:profile08": 3},
        )

        payload = card_usage_payload(
            "strategy00",
            "Winner",
            metrics,
            [
                {"id": "card:USED", "title": "Used", "type": "tuning", "tier": 1},
                {"id": "card:UNUSED", "title": "Unused", "type": "tuning", "tier": 1},
            ],
        )
        cards = {card["id"]: card for card in payload["cards"]}

        self.assertEqual(0.5, cards["card:USED"]["selectionRate"])
        self.assertEqual(0, cards["card:UNUSED"]["selected"])
        self.assertEqual(2, cards["driver:profile08"]["equippedAtRaceEnd"])

    def test_counts_java_style_occurrences(self):
        counts = {}

        count_occurrences(counts, ["card:A", "card:B", "card:A"])

        self.assertEqual({"card:A": 2, "card:B": 1}, counts)


if __name__ == "__main__":
    unittest.main()
