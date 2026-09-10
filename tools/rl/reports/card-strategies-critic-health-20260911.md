# Strategy retraining and critic saturation

Trained Explorer, Engineer, then Winner, for 1,600 episodes each, resuming the
existing actors against the updated driver assets. All three candidates passed
the existing promotion checks and were exported. Algorithmic was unchanged.

## Held-out results

| Strategy | Previous policy | Promoted policy | First championship, previous/new |
| --- | ---: | ---: | ---: |
| Explorer | 11.5% | 13.6% | 14.0% / 17.4% |
| Engineer | 12.2% | 13.8% | 11.8% / 11.8% |
| Winner | 19.3% | 20.1% | 16.4% / 18.4% |

These are the headless strategy estimator's mixed-opponent results, not
full-physics race measurements. Each paired comparison used 500 held-out
episodes, totaling 998 championships including continuations. Opponents were
reloaded between strategies, so these rows are not a single final-roster league
table. Improvements are modest and should not be interpreted as statistical
certainty from one evaluation seed suite.

## Saturation diagnosis

Read-only probes used actual card-selection observations from eight episodes.
All three actors were healthy. The final critic hidden layer was saturated
(`abs(tanh) >= 0.99`) for 97.7% of Explorer activations, 62.5% of Engineer
activations, and 100% of Winner activations. Winner's last two critic layers
were completely saturated, with almost constant outputs.

Unlike the earlier driver problem, the strategy actor and critic already had
separate parameters and independent gradient clipping. The issue was unscaled
championship-return targets driving the critic itself into saturation.

The fix scales critic targets by a fixed 0.001. Raw game rewards, evaluation
rewards, observations, network sizes and runtime inference are unchanged.
Checkpoint scale compatibility is recorded; old actor weights are preserved,
but incompatible critic weights are not restored. Normalized policy advantages
are calculated consistently in the scaled value units.

Diagnostics now log layer saturation, persistent saturation, activation
variation, mean tanh derivatives, finite gradients and value explained variance.
At the final training batch, all critic layers had zero saturated activations.
No actor or critic had persistently saturated neurons. Every hidden-layer weight
gradient was finite and nonzero. Occasional actor activation saturation was
below 0.2% and was not a collapsed layer.

## Verification and artifacts

- 95 Python RL tests passed, including checkpoint scale compatibility and
  read-only diagnostic tests.
- Batch log: `logs/card-strategies-20260911/training-scaled-critic.log`.
- Before/after health probes: `logs/card-strategies-20260911/health-*.log`.
- Pre-training policy backups: `logs/card-strategies-20260911/before/`.
- Promoted runtime policies: `assets/ai/card-strategies/strategy*/rl_card_strategy_policy.json`.
- Resumable checkpoints: `rl-checkpoints/card-strategies/strategy*/model.pt`.
- Usage reports: `target/card-strategy-reports/strategy*-card-usage.json`.

The initial batch was stopped before export when the critic problem was
confirmed; the complete batch was restarted with the fix and diagnostics.
