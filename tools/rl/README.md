# Ratass RL Race Training

The Java game owns the Box2D simulation and checkpoint-race environment. Python
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

- Observation size: `68` floats per learner.
- Action size: `2` floats per learner: `[throttle, turn]`, each in `[-1, 1]`.
- Default PPO network: two fully-connected hidden layers of width `1024`
  with `tanh` activation.
- Race observations include relative next-checkpoint vector, a route waypoint
  vector computed from the map hazard field, a farther route lookahead vector,
  normalized route distance, route alignment, car-frame velocity, angular
  velocity, near-checkpoint state, local track-limit ray clearances, short
  braking rays, track slowdown, steering authority, lateral slip, braking
  distance, previous action, six opponent-car ray clearances, and nearest-car
  relative state.
- Rewards are bucketed as `checkpoint_progress`, `checkpoint`, `step_cost`,
  `off_road`, `steering`, `reverse_speed`, `elimination`, and `car_push`. The
  progress bucket only pays new best progress along the ordered checkpoint route
  while the car is moving forward. Braking is not penalized, but actual negative
  forward speed is. Car collisions are treated as push/contact penalties, not as
  rewards.
- Java exposes episode metrics for checkpoints reached, eliminations, and route
  progress toward the checkpoint.
- The shell training presets stage the episode target through `1`, `2`, and `3`
  checkpoints before full-lap episodes (`RL_MAX_CHECKPOINTS=-1`). The live game
  can still run longer races because the same checkpoint-following policy repeats
  around the loop.

## Train

```bash
python tools/rl/train_rllib.py --iterations 25
```

Useful knobs:

```bash
python tools/rl/train_rllib.py \
  --controlled-agents 1 \
  --max-checkpoints -1 \
  --checkpoint-radius 3.0 \
  --max-action-steps 6400
```

By default, training uses every map discovered by the Java loader. To focus on a
temporary subset, pass explicit map ids:

```bash
python tools/rl/train_rllib.py --map-ids <map-id-a>,<map-id-b> --iterations 100
```

## Curriculum

```bash
tools/rl/train.sh
```

Train one specific profile:

```bash
tools/rl/train.sh 04
```

Check whether loaded checkpoint centers, checkpoint gates, and first route
targets sit on playable road:

```bash
.venv-rl/bin/python tools/rl/check_map_geometry.py
```

Write a short per-step policy trace after training. This is intentionally off
during training; enable it only for small evaluation runs:

```bash
.venv-rl/bin/python tools/rl/evaluate_policy.py --episodes 1 --steps 300 --controlled-agents 1 --field-size 1 --map-id map003 --trace-dir logs/rl-trace
```

Docker training uses the same profile system:

```bash
tools/rl/train_docker.sh
tools/rl/train_docker.sh 04
```

Internal presets are still available through `train_forever.sh` when debugging
the trainer directly:

```bash
bash tools/rl/train_forever.sh diagnostic
bash tools/rl/train_forever.sh quick
bash tools/rl/train_forever.sh fast
bash tools/rl/train_forever.sh race-single
bash tools/rl/train_forever.sh race-2
bash tools/rl/train_forever.sh race-4
bash tools/rl/train_forever.sh race-8
bash tools/rl/train_forever.sh race-16
bash tools/rl/train_forever.sh race-20
```

The current convenience curriculum trains the race objective on all discovered
mask maps. Every car-count phase is split into checkpoint stages: reach `1`
checkpoint, then `2`, then `3`, then a full lap. After that, the curriculum ramps
learner cars through `1`, `2`, `4`, `8`, `16`, and finally `20` cars forever by
default. The 400-iteration single-car wrapper splits its budget into `100`
iterations for each checkpoint stage. Each phase keeps a separate
best-evaluation state so harder crowded phases are not compared against easier
single-car scores. The playable policy at `assets/ai/rl_enemy_policy.json` tracks
the best model from the latest trained stage, so stopping early still leaves a
usable model from the most recent stage. Checkpoint output defaults to
`rl-checkpoints-curriculum-400-race-physics68-1024x2-v1` for the 400-iteration
wrapper. Training scripts delete their checkpoint directory at startup by
default, so a new run starts from scratch instead of resuming an older
checkpoint. Set `RL_FRESH_START=0` only when you intentionally want to resume.
Race training and policy evaluation use route-aligned random road spawns by
default, and the Java environment assigns the next checkpoint from the same route
selector used to face the car. Set `RL_RANDOM_RACE_SPAWNS=0` or pass
`--fixed-race-spawns` only when you intentionally want fixed-start debugging.

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
python tools/rl/evaluate_policy.py --episodes 5 --controlled-agents 4 --field-size 4
```

Export writes `assets/ai/rl_enemy_policy.json`. If that JSON was produced for a
smaller observation size, the game can still run it against the unchanged input
prefix, but newly added sensors are ignored until a new policy is trained and
exported.
