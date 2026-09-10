# Profile10 stat-handling sweep, 2026-09-12

## Scope

- Installed profile10, unchanged throughout the experiment.
- Policy SHA-256: `ed28710ac202f3c3ccd82eb0b75fa3fe18c178f88870ce8be3957b5145bf0129`.
- All 19 game maps, three laps, fixed grid, seed 20260531, four physics frames per decision.
- Actual headless simulation, not cached driver metadata. No recovery, passing, or straight-line assistance.
- Independent power, grip, aero efficiency, and mass multipliers from 0.1 to 5.0 (-90% to +400%).
- 155 scenarios: baseline; 40 single-stat changes; 54 pairwise combinations;
  16 four-stat min/max combinations; four simultaneous boosted builds;
  20 reproducible mixed builds; 20 additional held-out mixed builds.
- Pairwise cases cover every stat pair at 0.1, 2.2, and 5.0. Mixed seeds are
  20260912 for calibration and 20260913 for held-out validation.
- Stats are injected through the normal tuning-effect calculations, including
  mass, drag, drive-force allowance, and derived top speed. These are static,
  single-car tests, not a traffic/weather/combat benchmark.

## Selected Changes

Let `S = max(1, cbrt(power * aero))`, `G` be grip, and `R` the existing
upgrade-derived drive-force allowance (normally power/mass for an isolated
tuning effect, with its existing minimum of 0.7).

1. Braking assistance is `S`, previously `S*S`. Brakes still improve with stats,
   but the braking-distance sensor no longer shortens its lookahead as aggressively.
   Both actual braking and the braking-distance calculation use the same multiplier.
2. Turn-rate allowance is `max(S, sqrt(max(1, G)))`. The previous limit only
   considered power/aero, despite grip increasing possible corner speed.
   The existing steering-torque formula is retained.
3. Additional longitudinal traction grows as `sqrt(R)` when `R > 1`;
   penalties retain `R`. This softens extreme power/lightweight combinations
   without a hard upper cap, changing the neural network, or overriding its actions.
4. Extra grip no longer multiplies the sideways velocity-correction gain:
   that part uses `min(1, G)`. The available lateral tire-force limit still
   scales with full grip. Previously the correction could repeatedly overshoot.

At neutral stats these adjustments preserve the previous response. Card values,
driver weights, observation layout, rewards, and decision cadence were not changed.

## Results

A run counts as successful only when all three laps finish. The initial limit
was 9,000 decisions (600 simulated seconds). Only incomplete runs were retried
at 18,000 decisions (1,200 simulated seconds), for both implementations.

| Evaluation | Before | Selected formula |
| --- | ---: | ---: |
| Calibration, 600-second limit | 2,393 / 2,565 | 2,486 / 2,565 |
| Held-out combinations, 600-second limit | 362 / 380 | 369 / 380 |
| All scenarios, 600-second limit | 2,755 / 2,945 (93.5%) | 2,855 / 2,945 (96.9%) |
| All scenarios, extended limit | 2,784 / 2,945 (94.5%) | 2,883 / 2,945 (97.9%) |
| Neutral car, average lap | 33.585 s | 33.585 s |

On the 2,734 case/map pairs completed by both versions at the initial limit:

| Metric | Before | Selected formula |
| --- | ---: | ---: |
| Average lap | 41.404 s | 42.376 s |
| Average off-road action steps per run | 177.0 | 142.2 |

This is about 20% fewer off-road action steps, at the cost of approximately
one second per lap on this paired sample. The sample includes heavily debuffed,
very slow builds; it is not an average for a normal championship.

Large useful bonuses remain effective. Power/grip/aero at 5.0 with mass at 0.1
improved from finishing 12/19 circuits to 19/19, averaging 12.656 s per lap with
the new formula. Conversely, a moderate 1.4 power/grip/aero and 0.6 mass build
slowed from 23.639 s to 25.736 s, with both versions finishing every circuit.

## Candidate Comparison

All candidates below used the same 135 calibration scenarios and 600-second limit.

| Candidate | Completed runs |
| --- | ---: |
| Original handling | 2,393 / 2,565 |
| Limit extra sideways correction only | 2,404 / 2,565 |
| Also soften power/lightweight traction | 2,470 / 2,565 |
| Also account for grip in turn-rate allowance | 2,473 / 2,565 |
| Also use linear braking assistance: selected | 2,486 / 2,565 |
| Alternative: restrict longitudinal grip more, retain squared braking | 2,470 / 2,565 |

The last alternative was discarded. The final compiled build reproduced the
selected candidate's completion, lap times, and off-road counts exactly on all
calibration runs, before evaluating the held-out cases.

## Limits

- This is not a guarantee for every combination in a continuous four-dimensional range.
- At the initial limit, 121 previously incomplete runs finished, but 21 previously
  successful runs became incomplete. The change improves aggregate robustness,
  not every individual trajectory.
- Extending the limit recovered 28 of the selected formula's 90 incomplete runs.
  The remaining 62 concentrate on map015 (32), map011 (11), and map014 (9).
  Do not describe all remaining failures as time-budget issues.
- No scripted rescue or teleport was used to improve these results. Further
  improvement may require training on varied stats, rather than more physics compensation.
- Static-stat tests do not cover sudden effect transitions or collisions.
- Because boosted performance changes, existing tuning balance and strategy
  rankings can change even though the card descriptions and values are untouched.

## Reproduction

```bash
mvn -pl desktop -am -DskipTests package
.venv-rl/bin/python tools/rl/evaluate_stat_handling.py \
  --profile profile10 --workers 6 --output logs/stat-handling/run.jsonl
.venv-rl/bin/python tools/rl/evaluate_stat_handling.py \
  --profile profile10 --workers 6 --steps 18000 \
  --retry-incomplete logs/stat-handling/run.jsonl \
  --output logs/stat-handling/extended.jsonl
```

Use `--cases` and `--maps` to narrow a rerun. The script reads cadence from the
profile properties, records the policy hash, and neither reads nor updates cached
driver timings. Keep the same policy and map assets when comparing builds.

Raw results and candidate jars are in ignored `logs/stat-handling-20260912/`.
The before-results are split across `before.jsonl`, `before-pairs.jsonl`, and
`before-heldout.jsonl`; the final full sweep is `final.jsonl`. Extended retries
are `before-long.jsonl` and `final-long.jsonl`.

Verification: desktop package built; 110 focused Java tests and 37 Python
evaluation tests passed. A separate no-card lap benchmark matched the neutral
stat-injection baseline across all 19 maps.
