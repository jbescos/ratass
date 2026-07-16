# RL Driver Profiles

Each subdirectory is one trainable driving personality. The game loads models from
`assets/ai/policies/<id>/rl_enemy_policy.json`. Only profiles with an available
model are used for random enemy assignment.

Use `profile.properties` to configure a personality. Profiles inherit
`tools/rl/policies/default.properties` and can override any `RL_*` value.
`default.properties` is a shared baseline config, not a trainable profile.

Common properties:

```bash
# Network body; input/output sizes are fixed by the game.
RL_HIDDEN_SIZE=256
RL_HIDDEN_LAYERS=2
RL_HIDDEN_ACTIVATION=tanh
RL_LR=3e-4
RL_ENTROPY_COEFF=0.005
RL_GAMMA=0.995

# Route/lap curriculum. Percentages are fractions of a lap; use "%_real", for
# example "5%_real", to train that chunk on real game maps. lap_easy is a
# full-lap stage on route-only easy masks, lap_training uses training maps, and
# lap_real uses real game maps.
RL_STAGE_ROUTE_TARGETS=5%,10%,25%,50%,75%,lap_easy,lap_training,lap_real
RL_STAGE_ITERATIONS=100,100,300,100,100,200,200,200

# Number of cars for each stage. Route-target stages must use 1 car; lap stages
# can use more cars with fixed grid spawns.
RL_STAGE_NUMBER_OF_CARS=1,1,1,1,1,1,1,4

# Rewards.
RL_REWARD_PROGRESS=0.25
# Positive values reward sustained on-road drifting progress; 0 disables it.
RL_REWARD_DRIFT=0.0
RL_REWARD_OFF_ROAD_PENALTY=0.80
RL_REWARD_CAR_PUSH_PENALTY=3.0
RL_REWARD_STEERING_PENALTY=0.010
```

Recommended commands:

```bash
bash tools/rl/train.sh
bash tools/rl/train.sh aggressive
bash tools/rl/train.sh --detach aggressive
bash tools/rl/train_docker.sh
bash tools/rl/train_docker.sh aggressive
```
