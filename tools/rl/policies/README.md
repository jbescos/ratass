# RL Driver Profiles

Each subdirectory is one trainable driving personality. The game loads models from
`assets/ai/policies/<id>/rl_enemy_policy.json`; if a specific id is missing it falls back to
`assets/ai/policies/default/rl_enemy_policy.json`.

Use `profile.properties` to configure a personality. Named profiles inherit
`tools/rl/policies/default/profile.properties` and can override any `RL_*` value.

Common properties:

```bash
# Network body; input/output sizes are fixed by the game.
RL_HIDDEN_SIZE=256
RL_HIDDEN_LAYERS=2
RL_HIDDEN_ACTIVATION=tanh
RL_LR=3e-4
RL_GAMMA=0.995

# Route curriculum for single-car learning. Percentages are fractions of a lap;
# lap_easy is a full-lap stage on route-only easy masks.
RL_STAGE_ROUTE_TARGETS=25%,50%,75%,lap_easy,lap

# Optional multi-car curriculum. Stages above 1 car train lap-only with the
# fixed grid; route-target stages are always single-car.
RL_TRAINING_CAR_STAGES=1,2
RL_TRAINING_CAR_ITERATIONS=530,150
RL_TRAINING_CAR_MAX_CYCLES=1,1

# Rewards.
RL_REWARD_PROGRESS=1.60
RL_REWARD_ROUTE_TARGET=30.0
RL_REWARD_OFF_ROAD_PENALTY=0.80
RL_REWARD_CAR_PUSH_PENALTY=3.0
RL_REWARD_STEERING_PENALTY=0.010
```

Recommended commands:

```bash
bash tools/rl/train.sh
bash tools/rl/train.sh aggressive
bash tools/rl/train_docker.sh
bash tools/rl/train_docker.sh aggressive
```
