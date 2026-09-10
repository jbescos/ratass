import random
import unittest

from training_tuning import TrainingTuning


class FakeConfig:
    def __init__(self):
        self.card = ''
        self.calls = []

    def withBenchmarkTuningCard(self, card):
        if card == 'INVALID':
            raise ValueError('Invalid tuning card')
        self.card = card
        self.calls.append(card)


class TrainingTuningTest(unittest.TestCase):
    def test_disabled_does_not_modify_config_or_consume_global_randomness(self):
        state = random.getstate()
        config = FakeConfig()
        sampler = TrainingTuning(config, '', 0.35, 42)
        for _ in range(10):
            sampler.next_episode()
        self.assertEqual(config.calls, [])
        self.assertEqual(random.getstate(), state)

    def test_seeded_sampling_includes_standard_and_tuned_races(self):
        configs = [FakeConfig(), FakeConfig()]
        samplers = [TrainingTuning(config, 'a,b', 0.35, 42) for config in configs]
        for _ in range(100):
            for sampler in samplers:
                sampler.next_episode()
        self.assertEqual(configs[0].calls, configs[1].calls)
        self.assertEqual(set(configs[0].calls), {'', 'A', 'B'})
        self.assertGreater(configs[0].calls.count(''), 40)

    def test_zero_probability_clears_previous_card(self):
        config = FakeConfig()
        sampler = TrainingTuning(config, 'A', 0, 1)
        config.card = 'A'
        sampler.next_episode()
        self.assertEqual(config.card, '')

    def test_probability_one_always_equips_from_pool(self):
        config = FakeConfig()
        sampler = TrainingTuning(config, 'A,B', 1, 1)
        for _ in range(10):
            sampler.next_episode()
            self.assertIn(config.card, ('A', 'B'))

    def test_rejects_invalid_inputs_before_sampling(self):
        for probability in (-1, 1.01, float('nan')):
            with self.assertRaises(ValueError):
                TrainingTuning(FakeConfig(), '', probability, 1)
        with self.assertRaises(ValueError):
            TrainingTuning(FakeConfig(), 'A,INVALID', 0, 1)


if __name__ == '__main__':
    unittest.main()
