# Project Guidance

## Visual Features And RL Training

- Treat `RatassGame.RlTrainingEnvironment` as a headless simulation path.
- Keep every presentation-only feature outside RL training work. This includes
  particles, trails, animation state, render interpolation, camera work, audio,
  HUD state, and texture preparation.
- Gate presentation work with `RatassGame.isPresentationEnabled()` or an
  equivalent guard before per-car loops, allocations, or state updates.
- Visual features must not change physics, observations, rewards, map geometry,
  random-number consumption, or episode timing.
- Add focused tests for standalone visual-effect state and run an RL smoke test
  after integrating a visual feature.

## Tuning Card Balance

- Benchmark tuning balance across every map with three laps using profile04 for
  Tier 1 at x1.5, profile02 for Tier 2 at x2, and profile10 for Tier 3 at x3.
  Verify the final values without amplification and require meaningful gains
  between consecutive tiers.
- Tier 1 and Tier 2 tuning cards must each have a unique combination of modified
  attributes. Prefer different positive-attribute combinations where possible.
- Tier 3 may contain two cards with the same pair of positive attributes only
  when each card substantially favors a different attribute in that pair.
- Retrain card strategies after tuning-card balance changes, with the Winner
  strategy trained last.

## Conversation Context

- If a request appears unrelated to this racing game project or the current
  task, pause before acting and tell the user it may have been intended for a
  different Codex conversation. Continue only after the user confirms the
  context switch.
