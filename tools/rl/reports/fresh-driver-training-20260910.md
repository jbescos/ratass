# Fresh Driver Training Investigation

## Reproduced Failure

The profile00 5%-route checkpoint selected at iteration 95 was replayed through
the current headless Java environment. All 19 maps ended after 225 actions with
reward -627.500, no progress, and no off-road actions, matching the reported log.

The actor emitted about -0.087 throttle and +0.030 steering on every map.
At rest this means braking, not reverse: reverse engagement requires a command
below -0.1. With no motion, steering cannot move the car. This is not the old
6% control dead zone or a four-versus-five-frame timing discrepancy.

All 96 neurons in the second tanh hidden layer were saturated (absolute output
above 0.99; preactivations approximately -9.43 to +9.44). Different real map
observations therefore produced virtually identical actions. NumPy float32
inference agreed with the game's Java inference, excluding an obvious export
or input-layout discrepancy.

The stationary return consists of -22.5 step reward and -605 in the no-progress
bucket (including the -500 terminal penalty). Sampling this frozen actor's
Gaussian outputs yielded 0/19 successes and a worse mean reward (-1048.477).
This demonstrates the bad local solution, not that changing penalties alone
would fix learning.

## Training Fix

The actor previously shared its encoder with a critic learning large, unscaled
returns. The August 22 change removed effective clipping of the squared critic
loss; August 27 also raised failure penalties from 50 to 500. Reducing the
critic loss coefficient did not prevent shared-layer saturation in this run.

- Separate actor and critic encoders (`RL_SEPARATE_VALUE_NETWORK=1`).
- Scale rewards uniformly by 0.01 at the PPO boundary, with value loss weight 1.
- Keep evaluation rewards, physics, observations, action repeat 4, and the
  exported 33-input, 96x2 driver architecture unchanged.
- Require a successful archived evaluation before advancing curriculum stages;
  retry failures from their checkpoint rather than silently advancing.

The two learner changes were tested together. They were not independently
ablated, so the relative contribution of separation versus scaling is unknown.

## Initial Result

An isolated fresh start, with no existing driver weights, completed all 19
5%-route evaluation cases by the first checkpoint at iteration 25. The best
archived candidate from the first 100 iterations was iteration 43, with 19/19
success, no off-road actions, and a 2.646-second average route completion time.
At iteration 100 the critic explained variance was 0.649, versus approximately
zero in the failed run. The best checkpoint was retained when a later candidate
regressed to 18/19.

Longer-route and full-lap results are recorded in
`logs/profile00-fresh-fix/`. The installed policies were backed up under its
`before/` directory; experimental exports are isolated from game assets until
validation. `before-times.log` contains a fresh all-map benchmark under the
current four-frame, no-dead-zone controls. Historical comparison values remain
in `driver-control-baseline-20260910.md`.

## Full-Lap Fresh Start

The same fresh profile00 lineage continued through 10%, 25%, 50%, and 75%
routes (100 iterations each), then 300 iterations of three-lap training.
Every curriculum stage passed all 19 maps before advancing.

The fastest three-lap checkpoint, iteration 241 (35.625 seconds per lap),
failed a five-lap test. Iteration 292 was selected instead:

| Validation | Maps completed | Average lap (seconds) |
| --- | ---: | ---: |
| No cards, three laps | 19/19 | 35.770 |
| No cards, five laps | 19/19 | 35.729 |
| Agile Chassis, five laps | 19/19 | 34.624 |

This improves on the user's historical 36.199-second profile00 record without
loading any old driver weights. An additional five-lap refinement was stopped
after iteration 292 passed these checks; its experimental checkpoint remains
separate from the installed policy.

## Other Profiles

Existing drivers are refined from their exported actor weights, not from
scratch. Each candidate is checked against the pre-run model over all maps in
three-lap, five-lap, and five-lap Agile Chassis races. A faster three-lap result
does not justify introducing a new longer-race failure. Original models and
per-map validation results remain under `logs/profile00-fresh-fix/`.

Profiles 02, 04, and 06 already beat their historical average times under the
current controls and finish all maps, so they are not retrained. Once profiles
00, 05, and 09 also achieved this, no additional passes were scheduled for them.
The goal for the upper tiers is three fast standard drivers and a clearly
faster profile10 at T4; ordinary tiers remain assigned by measured lap times.

### Installed Results

Seconds per lap, averaged over all 19 maps with three laps and no cards.
The historical column is the user's pre-control-change record; the before
column was re-simulated with the current controls before this training.

| Profile | Historical | Before training | Installed | Action |
| --- | ---: | ---: | ---: | --- |
| 00 | 36.199 | 36.521 (18/19) | 35.770 | Trained from scratch |
| 01 | 36.327 | 36.447 | 35.359 | Refined |
| 02 | 35.552 | 35.429 | 35.429 | Model unchanged |
| 03 | 35.468 | 35.489 | 34.972 | Refined |
| 04 | 36.011 | 35.973 | 35.973 | Model unchanged |
| 05 | 36.015 | 36.302 | 35.198 | Refined |
| 06 | 35.880 | 35.820 | 35.820 | Model unchanged |
| 07 | 34.968 | 35.669 | 35.063 | Refined, then five-lap robustness pass |
| 08 | 34.760 | 35.316 | 35.062 | Refined |
| 09 | 35.733 | 35.559 (18/19) | 35.414 | Refined |
| 10 | 34.477 | 34.524 | 34.446 | Refined; remains T4 |

All installed models finish 19/19 maps. Every newly installed model also
passed five-lap runs on every map both without cards and with Agile Chassis.
Profiles 07 and 08 improved under the current controls but did not recover
their historical records. DNF averages are not directly comparable to complete
all-map averages; promotion additionally compared times on common complete maps.

The automatically ranked T3 drivers are now 03, 08, and 07. T4 profile10 is
0.526 seconds per lap faster than the best T3, approximately 1.58 seconds over
three laps. This is an average advantage, not a guarantee of being fastest on
every individual map or with every card combination.

The ordinary refinement runs used 100 iterations, learning rate 3e-5, three
epochs, initial log standard deviation -2.5, and entropy coefficient 0.002.
Profile07's additional 100-iteration pass used five laps, 25% Agile Chassis
episodes, learning rate 1e-5, two epochs, initial log standard deviation -3,
and zero entropy coefficient. Profile10's installed checkpoint is iteration
50 of its ordinary refinement run. All used separate critics, reward scale
0.01, and action repeat 4.

All driver metadata was regenerated by simulation with the current controls;
profile10's explicit T4 assignment was preserved. Final benchmark output is
`logs/profile00-fresh-fix/after-times.log`. Training completed and no trainer
process was left running.

## Tests

90 Python RL tests passed, including checks that a critic update cannot change
the exported actor, learner scaling leaves actions and raw simulation rewards
unchanged, and the curriculum rejects failed or missing evaluations.
