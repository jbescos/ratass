# Card strategy training

Card strategies choose among the three card offers, or skip. They do not drive cars and do not
create a LibGDX world. Training uses the real card/progression rules with a fast headless race
estimator.

The policy does not receive current weather. Weather remains part of simulated race variability.
Winner learns combinations through race results; Engineer additionally receives a training-only
reward from the same Tuning/Technique compatibility calculation used by the algorithmic selector.

Opponent observations contain levels, race/championship positions, and driver metrics. Their
equipped cards are intentionally excluded to keep the policy compact and easier to train.

Run the Winner profile with:

```bash
tools/rl/train_card_strategy.sh strategy00
```

Train every specialist sequentially with:

```bash
tools/rl/train_card_strategies_all.sh
```

The exported runtime policy is written to
`assets/ai/card-strategies/<profile>/rl_card_strategy_policy.json`. The PyTorch training state is
written below `rl-checkpoints/card-strategies/`, which is ignored by Git.

## Reward controls

Every profile inherits `default.properties` and can override these independent terms:

- `CARD_STRATEGY_REWARD_CHAMPIONSHIP_WIN`: winning the final standings.
- `CARD_STRATEGY_REWARD_FINAL_POSITION`: normalized final championship position.
- `CARD_STRATEGY_REWARD_RACE_POSITION`: normalized position after each race.
- `CARD_STRATEGY_REWARD_LEVEL`: levels gained.
- `CARD_STRATEGY_REWARD_XP`: XP normalized by the current level requirement.
- `CARD_STRATEGY_REWARD_NOVELTY`: preference for less frequently selected cards.
- `CARD_STRATEGY_REWARD_SKIP_PENALTY`: penalty for rejecting an offer.
- `CARD_STRATEGY_REWARD_DRIVER`: Driver selection, multiplied by tier.
- `CARD_STRATEGY_REWARD_TUNING`: Tuning selection, multiplied by tier.
- `CARD_STRATEGY_REWARD_TECHNIQUE`: Technique selection, multiplied by tier.
- `CARD_STRATEGY_REWARD_POWERUP`: Powerup selection, multiplied by tier.
- `CARD_STRATEGY_REWARD_REVENGE`: Revenge selection, multiplied by tier.
- `CARD_STRATEGY_REWARD_TECHNIQUE_AMPLIFIER`: Tuning card that amplifies Technique.
- `CARD_STRATEGY_REWARD_POWERUP_AMPLIFIER`: Technique card that amplifies Powerups.
- `CARD_STRATEGY_REWARD_REVENGE_AMPLIFIER`: Powerup card that amplifies Revenge.
- `CARD_STRATEGY_REWARD_AMPLIFIER_LINK`: each compatible amplifier-chain link newly formed.
- `CARD_STRATEGY_REWARD_RANDOM_POWERUP`: same-tier random Powerup selection.
- `CARD_STRATEGY_REWARD_RANDOM_REVENGE`: same-tier random Revenge selection.
- `CARD_STRATEGY_REWARD_PREFERRED_CARDS`: comma-separated cards defining a specialist family.
- `CARD_STRATEGY_REWARD_PREFERRED_CARD`: reward for entering that family or upgrading its tier.
- `CARD_STRATEGY_REWARD_DISCOURAGED_CARDS`: comma-separated cards the specialist should avoid.
- `CARD_STRATEGY_REWARD_DISCOURAGED_CARD_PENALTY`: penalty for discouraged cards and redundant
  same-tier specialist replacements.
- `CARD_STRATEGY_REWARD_LAP_WIN`: finishing a simulated lap in first place.
- `CARD_STRATEGY_REWARD_CARD_SELECTION`: accepting an offered card instead of skipping.
- `CARD_STRATEGY_REWARD_TUNING_TECHNIQUE_SYNERGY`: improvement from a compatible stat Tuning and
  Technique pair; amplifier-chain cards are deliberately excluded. Repeated selections receive
  diminishing credit. A higher-tier replacement always receives upgrade credit, even when it
  temporarily breaks an old pairing while waiting for a matching card.
- `CARD_STRATEGY_REWARD_CARD_TYPE_ROTATION`: reward for choosing a card type used less than the
  episode average and penalty for overused types.

`CARD_STRATEGY_PERSONALITY_TEACHER_WEIGHT` controls how strongly those selection rewards influence
the race-strength teacher. It remains conservative by default; specialist profiles can raise it
without inflating their real episode rewards.

`CARD_STRATEGY_TEACHER_ROLLOUT_RATIO` controls how often specialist distillation follows the
teacher's choice into the next offer. It is zero for normal optimizers and nonzero for specialists,
allowing training to observe complete specialist builds instead of only the old policy's states.
`CARD_STRATEGY_TEACHER_ROLLOUT_FINAL_RATIO` optionally changes that probability halfway through
distillation. Winner starts with stronger teacher guidance, then shifts toward student-generated
states so the learned policy can recover from its own imperfect choices.

Positive specialist selection rewards apply only to the two strongest offers according to the
race estimator. Explorer therefore rotates among competitive cards instead of sacrificing races
for variety, and Engineer cannot farm rewards by repeatedly replacing a strong Technique with a
weaker one.

`CARD_STRATEGY_MINIMUM_UNIQUE_CARDS` and
`CARD_STRATEGY_MINIMUM_STAT_SYNERGIES_PER_EPISODE` protect each specialist's identity when
selecting checkpoints. `CARD_STRATEGY_EARLY_STOP_PATIENCE` stops policy-gradient training after a
configured number of validations without improvement while retaining the best distilled or
trained state.

The runtime roster is deliberately small:

- `algorithmic`: deterministic tier and synergy heuristic; it is not trained.
- `strategy00` (`Winner`): unrestricted card choices, rewarded for lap and championship wins.
- `strategy01` (`Explorer`): avoids skips and rotates selections across card types.
- `strategy02` (`Engineer`): builds compatible Tuning and Technique stat combinations.

Untrained specialists initialize from `strategy00`, then optimize their own shaping rewards.
The algorithmic selector remains available in the game and is always the fallback for missing or
invalid neural policies.

Final evaluation also runs a mixed roster from `CARD_STRATEGY_MIXED_OPPONENT_POLICIES`. The roster
uses the same guarantee as the game: Algorithmic, Winner, Explorer, and Engineer each appear when
the field is large enough, then remaining rival slots are random draws from those strategies.

`strategy00` resumes its last checkpoint and mixes algorithmic rivals with frozen self-play
snapshots. `CARD_STRATEGY_SELF_PLAY_RATIO` controls that mix. Checkpoint selection uses
`CARD_STRATEGY_SELECTION_EVAL_EPISODES` held-out championships at
`CARD_STRATEGY_VALIDATION_EVERY` intervals. A candidate is promoted only when it beats the
installed policy on the same final held-out championships.

Before self-play, strategy00 refreshes its policy with on-policy race-strength distillation. The
current policy chooses the cards during collection while the headless race estimator labels each
visited offer. This avoids the distribution shift caused by copying only perfect teacher runs.
