"""Optional per-episode handling variation for headless driver training."""

import math
import random


class TrainingTuning:
    def __init__(self, config, cards, probability, seed):
        if not math.isfinite(probability) or not 0 <= probability <= 1:
            raise ValueError('Training tuning probability must be between zero and one')
        self.cards = tuple(card.strip().upper() for card in cards.split(',') if card.strip())
        self.config = config
        self.probability = probability
        self.random = random.Random(seed)
        # Validate every entry before starting workers' first sampled race.
        for card in self.cards:
            config.withBenchmarkTuningCard(card)
        if self.cards:
            config.withBenchmarkTuningCard('')

    def next_episode(self):
        if not self.cards:
            return
        card = ''
        if self.probability > 0 and self.random.random() < self.probability:
            card = self.random.choice(self.cards)
        self.config.withBenchmarkTuningCard(card)
