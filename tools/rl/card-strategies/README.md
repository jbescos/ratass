# Card strategy training

Card strategies choose among the three card offers, or skip. They do not drive cars and do not
create a LibGDX world. Training uses the real card/progression rules with a fast headless race
estimator.

The policy does not receive the current weather or a hand-authored synergy score. Weather remains
part of the simulated race variability, while useful card combinations must improve XP, race
positions, or the final championship result to be learned.

Opponent observations contain levels, race/championship positions, and driver metrics. Their
equipped cards are intentionally excluded to keep the policy compact and easier to train.

Run the balanced profile with:

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

`CARD_STRATEGY_PERSONALITY_TEACHER_WEIGHT` controls how strongly those selection rewards influence
the race-strength teacher. It remains conservative by default; specialist profiles can raise it
without inflating their real episode rewards.

`strategy00` is the balanced win optimizer. The other profiles are:

- `strategy01`: XP Hunter.
- `strategy02`: Engineer.
- `strategy03`: Powerup Specialist.
- `strategy04`: Avenger.
- `strategy05`: Driver Scout.
- `strategy06`: Adaptive.
- `strategy07`: Wildcard.
- `strategy08`: Chain Builder, focused on amplifier chains and random relays.
- `strategy09`: Front Runner.
- `strategy10`: Saboteur.
- `strategy11`: Comeback.
- `strategy12`: Chaos.

Untrained specialists initialize from `strategy00`, then optimize their own small shaping rewards.
Reward-oriented profiles may trade at most four percentage points of held-out win rate for a
stronger personality. This keeps winning dominant while allowing visibly different selections.
Chain Builder permits eight points because the fast estimator does not execute the Powerup and
Revenge effects amplified by its mechanically valid chain.
The algorithmic selector remains available in the game and is always the fallback for missing or
invalid neural policies.

`strategy00` resumes its last checkpoint and mixes algorithmic rivals with frozen self-play
snapshots. `CARD_STRATEGY_SELF_PLAY_RATIO` controls that mix. Checkpoint selection uses
`CARD_STRATEGY_SELECTION_EVAL_EPISODES` held-out championships at
`CARD_STRATEGY_VALIDATION_EVERY` intervals. A candidate is promoted only when it beats the
installed policy on the same final held-out championships.

Before self-play, strategy00 refreshes its policy with on-policy race-strength distillation. The
current policy chooses the cards during collection while the headless race estimator labels each
visited offer. This avoids the distribution shift caused by copying only perfect teacher runs.
