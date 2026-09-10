# Driver Control Change Baselines

## Historical Baseline Supplied by the User

Recorded on 2026-09-10, before removing the control dead zone and correcting
live decision timing. Values are from the user's all-map, three-lap benchmark.
The headless benchmark already used exact action repeats; the timing bug
affected live gameplay. Do not interpret this table as a five-step benchmark.

| Profile | Average fastest lap (s) | Average lap (s) | Average total time | Average off-road actions |
| --- | ---: | ---: | ---: | ---: |
| profile00 | 35.691 | 36.199 | 1:48.596 | 25.8 |
| profile01 | 35.825 | 36.327 | 1:48.982 | 28.8 |
| profile02 | 34.975 | 35.552 | 1:46.656 | 22.2 |
| profile03 | 34.986 | 35.468 | 1:46.404 | 24.2 |
| profile04 | 35.375 | 36.011 | 1:48.032 | 56.6 |
| profile05 | 35.523 | 36.015 | 1:48.046 | 15.3 |
| profile06 | 35.196 | 35.880 | 1:47.639 | 54.2 |
| profile07 | 34.382 | 34.968 | 1:44.905 | 51.2 |
| profile08 | 34.175 | 34.760 | 1:44.281 | 45.5 |
| profile09 | 35.137 | 35.733 | 1:47.200 | 14.6 |
| profile10 | 33.846 | 34.477 | 1:43.432 | 27.0 |

## Immediate Experimental Baseline

The model files backed up for no-dead-zone retraining are the currently
installed exports, not necessarily the historical revisions above.
In particular, profile10 had subsequently improved to a measured 34.394s
average lap with the original dead zone at repeat 4. Its preserved model
SHA-256 is `cde4f6fcaeb9eea743d16ee6f8cef9ac003de9ce73b43655953691f6faf6a410`.

With that same profile10 model and no throttle or steering dead zone, the
average became 34.524s, while the Agile Chassis five-lap test improved from
18/19 completed maps to 19/19. Untuned profile00 and profile09 acquired one
failed map each under the new controls.

Final retraining comparisons should show both the historical figures above
and the same-model baseline under the new controls. Prefer completion rates
before comparing lap times; do not average incomplete runs as successful laps.

Retraining run directory: `logs/driver-no-deadzone-20260910-141648/`.
It contains original model backups, isolated candidates, a fixed runtime JAR,
configuration snapshots, per-profile training logs, and validation results.
