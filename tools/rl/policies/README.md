# RL Policy Profiles

Each subdirectory is one trainable driving personality. The game loads models from
`assets/ai/policies/<id>/rl_enemy_policy.json`; if a specific id is missing it falls back to
`assets/ai/policies/default/rl_enemy_policy.json`.

Use `profile.properties` to configure a personality. Numbered profiles inherit
`tools/rl/policies/default/profile.properties` and can override any `RL_*` value.

Common properties:

```bash
# Network body; input/output sizes are fixed by the game.
RL_HIDDEN_SIZE=1024
RL_HIDDEN_LAYERS=2
RL_HIDDEN_ACTIVATION=tanh
RL_LR=3e-4
RL_GAMMA=0.995

# Curriculum. These are total learner cars in the training environment.
RL_STAGE_CHECKPOINTS=1,2,3,lap
RL_TRAINING_CAR_STAGES=1,2,4,8,20
RL_TRAINING_CAR_ITERATIONS=400,400,400,500,800
RL_TRAINING_CAR_MAX_CYCLES=1,1,1,1,0

# Rewards.
RL_REWARD_PROGRESS=1.60
RL_REWARD_CHECKPOINT=30.0
RL_REWARD_OFF_ROAD_PENALTY=0.80
RL_REWARD_CAR_PUSH_PENALTY=3.0
RL_REWARD_STEERING_PENALTY=0.010
```

Recommended commands:

```bash
bash tools/rl/train.sh
bash tools/rl/train.sh 04
bash tools/rl/train_docker.sh
bash tools/rl/train_docker.sh 04
```
