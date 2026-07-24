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
