# Recovery retraining with a separate critic

## Changes

- Preserve the existing recovery actor: 33 observations, two controls, 96x2 tanh.
- Use an independent critic with learner reward scale 0.001 and value loss
  coefficient 1.0. Raw evaluation rewards and simulation physics are unchanged.
- Use `rl-checkpoints/recovery-mixed-direct-v2` to avoid restoring the previous
  shared-network optimizer/value state. Future runs resume the new checkpoint.
- Set recovery's default rollout-worker count to six after a throughput check.
- Log `train_seconds` for each driver/recovery/overtaking training iteration,
  excluding external checkpoint evaluation.

## Training and validation

Started from the installed actor, ran 80 iterations, and retained iteration 70.
Then compared two 20-iteration continuations from that same checkpoint with
four and six workers. The four-worker continuation produced the best evaluated
policy; the six-worker candidate was weaker and was not installed.

| Suite | Previous success | New success | Previous recovery time | New recovery time |
| --- | ---: | ---: | ---: | ---: |
| Selection, seed 20260506 | 91.1% | 93.4% | 6.04s | 4.83s |
| Separate-seed check, 20260911 | 89.8% | 91.2% | 6.07s | 5.23s |

Both suites contain 988 cases: 52 per map across all 19 maps. Times average only
successful recoveries; the faster time is accompanied by improved completion,
not simply the exclusion of more failed cases. These are measured improvements,
not a guarantee of recovery in every gameplay situation. The separate-seed
check uses the recovery evaluator's default reward coefficients consistently
for both models; only completion and time are compared between suites.

The selected candidate and matching resumable checkpoint were installed at the
standard recovery paths. The original model remains backed up under
`logs/recovery-separated-20260911/before.json`.

## Network health

Probed both encoders on 27,529 actual observations from 247 mixed recovery
episodes across the 19 maps. The selected actor has 0%/3.55% saturated
activations in its two hidden layers, down from 0%/4.75% in the old actor.
The new critic has zero saturated activations in both layers. Neither network
has persistently saturated neurons. During training, critic explained variance
reached approximately 0.68. No hidden-layer shape or sensor changes were made.

## Worker throughput

Same starting checkpoint and settings, 20 iterations each. Discarding the first
three warm-up iterations:

| Workers | Mean training iteration |
| --- | ---: |
| 4 | 3.453s |
| 6 | 2.833s |

Six workers reduced measured iteration time by 18%. This is a small sequential
benchmark, not a universal guarantee. It excludes evaluation and checkpoint
overhead. Synchronous sampling, learner updates and serial evaluation still
leave CPU idle in parts of the run. Driver defaults were not changed by this
task; driver throughput with six versus eight workers has not been benchmarked.

## Verification

- 97 Python RL tests pass, including recovery-script forwarding and overrides.
- `git diff --check` passes.
- All training and diagnostic processes launched for this task completed.
- Logs, backups, timing comparisons and health probes are under
  `logs/recovery-separated-20260911/`.
