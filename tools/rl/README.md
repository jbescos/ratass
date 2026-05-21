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

- Observation size: `51` floats per learner.
- Action size: `2` floats per learner: `[throttle, turn]`, each in `[-1, 1]`.
- Default PPO network: two fully-connected hidden layers of width `1024`
  with `tanh` activation.
- Target observations include relative target vector, a route waypoint vector
  computed from the map hazard field, a farther route lookahead vector,
  normalized route distance, route alignment, car-frame velocity, angular
  velocity, inside-circle state, hold progress, local hazard ray clearances,
  short braking rays, recovery direction in both world and car frame, and
  previous action. Car/opponent sensors are intentionally not part of this
  one-car survival contract.
- Rewards are bucketed as `progress`, `enter`, `hold`, `complete`, `safety`,
  `control`, `alive`, `death`, and `timeout`. The `progress` bucket is symmetric route
  distance progress/regression toward the target circle, not straight-line
  distance through hazards. The `enter` bucket pays once per target sequence,
  so re-entering or skimming the circle cannot farm reward. There is no speed,
  car-contest, or push reward.
- Java exposes episode metrics for goals reached, fall deaths, inside-target
  time, route progress toward target, and edge-risk events.
- One-car target training uses a larger random-spawn hazard margin than normal
  gameplay so early hard-map learning is about navigation instead of immediate
  unrecoverable edge starts.

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

Fast diagnostic curriculum for checking reward or observation changes before
spending hours on a full run:

```bash
bash tools/rl/train_forever.sh diagnostic
```

Convenience wrappers:

```bash
bash tools/rl/train_curriculum_400.sh
bash tools/rl/train_curriculum_40_to_4.sh
```

Common presets:

```bash
bash tools/rl/train_forever.sh diagnostic
bash tools/rl/train_forever.sh quick
bash tools/rl/train_forever.sh fast
bash tools/rl/train_forever.sh target-easy
bash tools/rl/train_forever.sh target-hard
bash tools/rl/train_forever.sh target-2
bash tools/rl/train_forever.sh target-4
bash tools/rl/train_forever.sh target-8
bash tools/rl/train_forever.sh target-16
bash tools/rl/train_forever.sh target-32
bash tools/rl/train_forever.sh target-many
bash tools/rl/train_forever.sh target-crowd
```

The curriculum starts with larger targets on easier maps, then moves to
hole-heavy maps, then ramps the number of learner cars through `2`, `4`, `8`,
`16`, `32`, and finally `50` cars forever by default. Each phase keeps a
separate best-evaluation state so harder crowded phases are not compared
against easier single-car scores. Checkpoint output defaults to
`rl-checkpoints-curriculum-route-escape51-survival-v2` for long curriculum
runs.

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
