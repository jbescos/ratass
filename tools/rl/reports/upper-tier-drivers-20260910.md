# Upper-Tier Driver Refinement

## Standard Drivers

Continued from the installed 96x2 policies with the same 33 observations and
four-frame action repeat. No physics, sensors, or reward weights were changed.
The learner used separate actor/critic encoders and reward scaling of 0.01.

| Profile | Historical target | Starting average | Installed average |
| --- | ---: | ---: | ---: |
| profile07 | 34.968 | 35.063 | 34.927 |
| profile08 | 34.760 | 35.062 | 34.750 |

All values are seconds per lap, averaged across all 19 maps over three laps.
Both installed candidates also completed every map in five-lap tests without
cards and with Agile Chassis. Profile08 beat its target in the first
150-iteration pass. Profile07 required a second pass; its iteration-23 candidate
passed validation and the remaining training was interrupted intentionally.
The resulting interruption message in its trainer log is not a model failure.

These passes used learning rate 1.5e-5, two epochs, initial log standard
deviation -3, zero entropy coefficient, batch size 32768, and three workers.

## Profile10 Architecture Comparison

Two isolated runs continue from the same installed profile10 actor, with
hidden layers of 96x2 and 128x2. Both use 600 iterations, batch size 65536,
learning rate 1e-5, two epochs, initial log standard deviation -3, zero entropy
coefficient, three workers, and seed 20260531. Sensors, rewards, and action
repeat remain unchanged. This equal-sample comparison is not equal CPU cost.

The existing partial actor initializer can widen the layers while preserving
the original action outputs: new connections into the existing downstream
neurons initially have zero weight. A regression test checks 96-to-128 action
equivalence. Both real Java baseline evaluations also produced the same
103.337-second average three-lap time (34.446 seconds per lap).

Models, checkpoints, and logs are isolated under
`logs/driver-upper-tiers-20260910/t4-96/` and `t4-128/`. Each run keeps its best
checkpoint and validates its final candidate without installing it. The
installed T4 must only be replaced after comparing completion reliability,
lap times, and the larger model's inference cost. The profile's training
hidden-size property must match whichever architecture is finally installed.

## Focused 96x2 Continuation

At the user's request, the 128x2 comparison was stopped at iteration 352: its
best evaluation was still the original baseline. The 96x2 run had reached a
34.394-second average at iteration 243. That candidate completes all maps over
three and five laps without cards but fails one five-lap Agile Chassis case,
so it was not installed.

The initial 96x2 process was checkpoint-stopped at iteration 362 to release
its three-worker configuration. It then resumed for 1000 additional iterations
with six workers. The stage baseline logic restores the best iteration-243
checkpoint, including the critic and optimizer, rather than starting a new
actor-only run. The original stop-state checkpoint was backed up as
`t4-96/resume-origin/`. The continuation log is `t4-96/long-training.log`;
iteration numbers in that log restart at 1.

This is evidence favoring 96x2 in these runs, not proof that wider networks
cannot work.

## Final Profile10 Result

The full 1000-iteration continuation finished on September 11. Its best
candidate, continuation iteration 721, averaged 34.363 seconds per lap. It
completed every map without cards over three and five laps but failed one
five-lap Agile Chassis case, so it was not installed.

A further 150-iteration 96x2 pass started from that candidate, training five
laps with Agile Chassis equipped in 25% of episodes. It used two workers,
batch size 32768, learning rate 1e-5, two epochs, initial log standard deviation
-3, and zero entropy coefficient. Its iteration-25 checkpoint passed all
three validation suites and was installed:

| Validation | Previous installed | New installed | Maps completed |
| --- | ---: | ---: | ---: |
| Three laps, no cards | 34.446 | 34.432 | 19/19 |
| Five laps, no cards | 34.394 | 34.333 | 19/19 |
| Five laps, Agile Chassis | 33.368 | 33.352 | 19/19 |

Values are average seconds per lap. The three-lap gain is only 0.014 seconds;
this is a small improvement, not a major new performance level. The larger
architecture did not justify replacing 96x2 in this experiment. The broader
card catalogue, every amplified build, and every collision scenario are not
covered by these solo validation suites.

In total, profile10 received 362 initial 96x2 iterations, 1000 continuation
iterations, and 150 mixed-build iterations, plus the 352-iteration 128x2
comparison. The continuation restored the best earlier state, so these counts
describe work performed rather than a single uninterrupted optimization path.

The installed profile10 remains 33-input, 96x2, action repeat 4, and T4.
Its metadata was regenerated from simulation. All training processes finished
or were explicitly stopped; no training was left running. The experiment
checkpoints remain in the ignored log directory for future continuation.
