# Ratass RL Target Training

The Java game owns the Box2D simulation and target-circle environment. Python
uses JPype to step that environment and RLlib PPO to train a policy.

The old scripted driving AI, behavior/evolution tuning, combat objective, and
navigation compatibility path have been removed from the training flow.

## Build

```bash
mvn -pl desktop -am package
```

The Python wrapper uses `desktop/target/ratass-desktop-1.0.jar` as the JVM
classpath.

## Install Python Dependencies

```bash
python3 -m venv .venv-rl
. .venv-rl/bin/activate
pip install -r tools/rl/requirements.txt
```

GPU use is optional and only helps the PPO/PyTorch part. Java physics rollout
still runs on CPU.

## Environment Contract

- Observation size: `44` floats per learner.
- Action size: `2` floats per learner: `[throttle, turn]`, each in `[-1, 1]`.
- Target observations include relative target vector, normalized distance,
  heading alignment, velocity, angular velocity, inside-circle state, hold
  progress, local hazard ray clearances, recovery direction, previous action,
  nearest-car state, and car proximity rays.
- Rewards are bucketed as `progress`, `enter`, `hold`, `complete`, `safety`,
  `control`, `speed`, `alive`, `death`, `timeout`, and `contest`.
- Java exposes episode metrics for goals reached, fall deaths, inside-target
  time, progress toward target, and edge-risk events.

## Train

```bash
python tools/rl/train_rllib.py --iterations 25
```

Useful knobs:

```bash
python tools/rl/train_rllib.py \
  --controlled-agents 1 \
  --max-goals 6 \
  --target-radius 1.65 \
  --target-hold-seconds 0.85 \
  --max-action-steps 1350
```

Focus training on specific maps:

```bash
python tools/rl/train_rllib.py --map-ids map001,map003,map006 --iterations 100
```

## Curriculum

```bash
bash tools/rl/train_forever.sh curriculum
```

Single-stage presets:

```bash
bash tools/rl/train_forever.sh target-easy
bash tools/rl/train_forever.sh target-hard
bash tools/rl/train_forever.sh target-many
bash tools/rl/train_forever.sh target-crowd
```

The curriculum starts with larger targets on easier maps, then moves to
hole-heavy maps, then runs 4 learner cars on the full target contract, then
runs 50 learner cars forever by default. Checkpoint output defaults to
`rl-checkpoints-curriculum-target-cars-768-v1` for curriculum runs.

## Docker

```bash
bash tools/rl/train_docker.sh curriculum
```

Set `RL_DOCKER_GPU=1 RL_NUM_GPUS=1` only when Docker can see a CUDA-enabled
Torch install.

## Export And Evaluate

```bash
python tools/rl/export_policy.py
python tools/rl/evaluate_policy.py --episodes 20
```

Export writes `assets/ai/rl_enemy_policy.json`. If that JSON was produced for an
older observation size, the game ignores it until a new policy is trained and
exported.
