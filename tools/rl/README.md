# Ratass RL Race Training

The Java game owns the Box2D simulation and route-progress race environment. Python
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

- Observation size: `41` floats per learner.
- Action size: `2` floats per learner: `[throttle, turn]`, each in `[-1, 1]`.
- Default PPO network: two fully-connected hidden layers of width `256`
  with `tanh` activation.
- Race observations include normalized route progress, current/near/far route
  tangent alignment, route lookahead alignment/clearance/distance, route curvature,
  route-side clearances, upcoming-corner distance/direction/severity, route width,
  speed and car-frame velocity, edge/off-road state, angular velocity,
  previous action, six opponent-car ray clearances, and
  left/right/front/front-diagonal road-clearance rays. They also include
  current route-point forward/side/distance offsets matching the sandbox route
  guidance ray. The old checkpoint target
  observations, nearest-car aggregate observations, and constant `active`
  observation were removed.
- Rewards are bucketed as `route_progress`, `step_cost`, `off_road`, `steering`,
  `reverse_speed`, `car_push`, and `route_alignment`. The route-progress bucket
  contains signed progress along the circuit route plus the route-target
  completion reward. Braking is not penalized, but actual negative forward speed
  is. Car collisions are treated as push/contact penalties, not as rewards.
- Java exposes episode metrics for route targets reached and route progress.
- The shell training presets stage route learning through `5%`, `10%`, `25%`,
  `50%`, and `75%` route targets. Add `_real`, for example `5%_real`, to run
  that route-target stage on `assets/maps`. Full-lap stages are `lap_easy`
  route-only masks, `lap_training` on `tools/rl/trainingMaps`, and `lap_real`
  on `assets/maps`. `RL_STAGE_NUMBER_OF_CARS` controls how many cars are used in
  each stage. Route-target stages must stay single-car; lap stages can train
  with traffic.
- Route-target stages use saved random spawn seeds. By default each
  `tools/rl/train.sh` invocation creates a new spawn-seed session, so spawns
  are stable during that run but different on the next run. Set
  `RL_ROUTE_SPAWN_RUN_ID` or `RL_ROUTE_SPAWN_SEED_DIR` to reproduce an earlier
  spawn session exactly.

## Train

```bash
python tools/rl/train_rllib.py --iterations 25
```

Useful knobs:

```bash
python tools/rl/train_rllib.py \
  --controlled-agents 1 \
  --route-targets -1 \
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

`train.sh` starts a detached training process by default and tails its log in
your terminal. `Ctrl+C` stops the detached training process. If the terminal or
network connection disappears, training keeps running. Each new run clears its
log before starting; if the pid file still points to a running process, the
launcher refuses to overwrite that active log. Use `--detach` when you want to
start it and return immediately:

```bash
tools/rl/train.sh --detach aggressive
```

Use `--foreground` only when debugging the trainer itself and you want it tied
directly to the terminal.

Local Ray workers bind to `RL_RAY_NODE_IP=127.0.0.1` by default so unplugging
or changing Wi-Fi/VPN interfaces does not break local worker RPC.

Train one specific profile:

```bash
tools/rl/train.sh aggressive
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
tools/rl/train_docker.sh aggressive
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

The current convenience curriculum trains the race objective on synthetic
training-only masks from `tools/rl/trainingMaps` when they exist. Best-eval still
uses the playable game masks from `assets/maps`, so the exported policy is
selected by performance on the real circuits while most learning happens on
circuits the player will not race on. The single-car phase is split into route
stages: progress through `25%`, `50%`, `75%`, an easy full lap on `route*`
training maps, then a full normal lap.
After that, the curriculum ramps
learner cars through `1`, `2`, `4`, `8`, `16`, and finally `20` cars forever by
default. The 400-iteration single-car wrapper splits its budget into `100`
iterations for each route stage. Each phase keeps a separate
best-evaluation state so harder crowded phases are not compared against easier
single-car scores. The playable policy at `assets/ai/rl_enemy_policy.json` tracks
the best model from the latest trained stage, so stopping early still leaves a
usable model from the most recent stage. RLlib checkpoint output defaults to
`rl-checkpoints-curriculum-400-race-physics68-1024x2-v1` for the 400-iteration
wrapper. Training scripts delete their checkpoint directory at startup by
default, so a new run starts from scratch instead of resuming an older
RLlib checkpoint. Set `RL_FRESH_START=0` only when you intentionally want to resume.
Route-target stages use single-car route-aligned random road spawns
from saved per-stage seed files under the RLlib checkpoint directory, so a resumed run
reuses the same training cases. Full-lap stages always use the fixed start grid.
Multi-car training is valid only for full-lap stages; route-target stages fail
fast if configured with more than one learner car.

Regenerate the synthetic training maps and prebuild their caches:

```bash
python3 tools/rl/generate_training_maps.py
mvn -q -DskipTests package
.venv-rl/bin/python tools/rl/prebuild_training_map_cache.py
```

Training workers load the map set once when each Java environment starts. They
do not reload PNGs every PPO iteration. The `.json.gz` files beside the masks keep
startup fast by avoiding repeated mask parsing and distance-field generation.

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
