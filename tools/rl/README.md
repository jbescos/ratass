# Ratass RL Training

This is the first reinforcement-learning path for the game. The Java game still owns
the Box2D simulation; Python only drives actions and trains a policy.

## Build

```bash
mvn -pl desktop -am package
```

The Python wrapper uses `desktop/target/ratass-desktop-1.0.jar` as the JVM classpath.

## Install Python Dependencies

Use a virtual environment if possible:

```bash
python3 -m venv .venv-rl
. .venv-rl/bin/activate
pip install -r tools/rl/requirements.txt
```

On Windows:

```bat
py -3 -m venv .venv-rl
.venv-rl\Scripts\python.exe -m pip install -r tools\rl\requirements.txt
```

The Windows forever launcher does this automatically when the default `.venv-rl`
folder is missing. Set `RL_AUTO_SETUP_VENV=0` to disable that behavior.

The machine also needs a JDK and Maven because training starts the packaged
desktop jar through JPype. Use JDK 17 or 21 for training. On Windows, the helper
blocks newer JDKs by default because Java 25 has crashed inside Ray's native
`_raylet.pyd` while JPype had the JVM embedded in the Python process. Set
`RL_JAVA_HOME` to the JDK you want the helper to use:

```bat
set RL_JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.11.9-hotspot
tools\rl\train_forever.cmd
```

Maven 3.9+ is a good baseline. On an NVIDIA machine, install the NVIDIA driver
first; GPU use is optional and only helps the PPO/PyTorch part, while the Java
physics rollout still runs on CPU.

## Docker Training

The Docker launcher is the preferred Windows path because it avoids embedding
Java, Ray, and JPype directly in the Windows Python process. Docker still writes
all useful outputs into this checkout because the repo is mounted into the
container:

- `rl-checkpoints-navigation/` or `rl-checkpoints-direct-circle/`
- `assets/ai/rl_enemy_policy.json`
- `desktop/target/ratass-desktop-1.0.jar`
- `rl-logs/*.log` plus Ray logs under `rl-logs/ray/`

On Linux:

```bash
bash tools/rl/train_navigation_docker.sh
```

On Windows Command Prompt:

```bat
tools\rl\train_navigation_docker.cmd
```

For the later combat phase, use the generic wrappers instead:

```bash
bash tools/rl/train_docker.sh
```

```bat
tools\rl\train_docker.cmd
```

The first run builds the `ratass-rl:latest` image. Later runs can skip the image
build when only training data changed:

```bash
RL_DOCKER_BUILD=0 bash tools/rl/train_navigation_docker.sh
```

```bat
set RL_DOCKER_BUILD=0
tools\rl\train_navigation_docker.cmd
```

The Docker launcher defaults to navigation training and keeps reward summaries
enabled so the log is useful for later inspection. It still exports and packages
after every cycle. Stop it with
`Ctrl-C`; the in-container training script handles the interrupt and exports the
latest saved checkpoint before exiting. At most the work since the previous
checkpoint is lost.
Set `RL_MAX_CYCLES=1` when you want a finite smoke run instead of unattended
training.

Useful Docker knobs:

```bash
RL_FOREVER_ITERATIONS=200 \
RL_CHECKPOINT_EVERY=10 \
RL_MAP_IDS=map001,map006 \
bash tools/rl/train_navigation_docker.sh
```

If Docker has GPU support available, enable it explicitly:

```bash
RL_DOCKER_GPU=1 RL_NUM_GPUS=1 bash tools/rl/train_navigation_docker.sh
```

Ray benefits from Docker shared memory. The launcher defaults to `4g`; increase
it if Ray warns about the object store:

```bash
RL_DOCKER_SHM_SIZE=12g bash tools/rl/train_navigation_docker.sh
```

On Linux, the launcher runs the container with your host UID/GID so generated
checkpoints and logs stay writable from the checkout. Set `RL_DOCKER_USER=0` if
you intentionally want to run the container as root.

For maximum unattended throughput, disable the per-map reward summary:

```bash
RL_NO_REWARD_SUMMARY=1 bash tools/rl/train_navigation_docker.sh
```

## Train

```bash
python tools/rl/train_rllib.py --iterations 25
```

The current policy controls the car directly. Its two outputs are throttle and
turn, each in `[-1, 1]`; the scripted tactical mode layer is not used for RL
cars. Its exported format is `ratass-rl-policy-v3`, tied to the direct
safe-circle objective. The default checkpoint directory is
`rl-checkpoints-direct-circle`; do not resume older checkpoints from
`rl-checkpoints`, `rl-checkpoints-tactical`, or `rl-checkpoints-circle`, because
those were trained with previous action and reward meanings.

Continue from the latest saved checkpoint:

```bash
python tools/rl/train_rllib.py --resume --iterations 100
```

Useful early tuning knobs:

```bash
python tools/rl/train_rllib.py \
  --iterations 100 \
  --controlled-agents 1 \
  --field-size 12 \
  --workers 0
```

Focus training on one or more specific maps:

```bash
python tools/rl/train_rllib.py --resume --map-ids map004,map006 --iterations 100
```

Train the navigation-only phase without enemies or pickups:

```bash
python tools/rl/train_rllib.py \
  --objective navigation \
  --checkpoint-dir rl-checkpoints-navigation \
  --controlled-agents 1 \
  --field-size 1 \
  --iterations 100
```

This phase keeps one learner alive through repeated safe circles. It is meant to
teach circle approach, braking inside the circle, and edge recovery before
combat is introduced again.

After each PPO iteration, `train_rllib.py` prints a compact reward breakdown for
the completed learner episodes grouped by map and car:

```text
reward_map iteration=1 map=map004 car=learner_0 episodes=2 wins=1 reward_avg=-8.250 circle=-5.200 edge=-1.100 attack=0.350
```

The bucket values are averages per completed learner episode. Large negative
`circle`, `edge`, or `survival` values mean the policy is still losing reward by
missing circles or suiciding. Positive `attack` is useful only when it is not
being outweighed by those safety buckets. Use `--no-reward-summary` when you
want more throughput and can skip the per-map reward diagnostics.

For long curriculum training, the helper script rebuilds the desktop jar, warms
up on the easiest map, focuses on `map001`, drills the hard maps
(`map001,map003,map006`), then finishes on the full map set:

```bash
bash tools/rl/train_for_hours.sh
```

The phase lengths can be tuned with environment variables:
`RL_WARMUP_ITERATIONS`, `RL_MAP001_ITERATIONS`, `RL_HARD_ITERATIONS`, and
`RL_MIXED_ITERATIONS`.

The helper starts a fresh warmup phase, then resumes that same direct-control
checkpoint for the later curriculum phases. It defaults to single-process Ray because that
has produced faster iteration feedback in this project. It also defaults to six
controlled learners so the shared policy practices
fighting around several RL-driven cars instead of learning mostly one-car
survival against heuristic opponents.

For quick experiments, one controlled learner is still useful. For the long run,
multiple controlled learners gives the shared policy more self-play pressure.

For unattended training over days, use the forever helper:

```bash
bash tools/rl/train_forever.sh
```

For the current navigation-first phase, use the navigation wrapper instead:

```bash
bash tools/rl/train_navigation_forever.sh
```

On Windows, use the Command Prompt launcher:

```bat
tools\rl\train_forever.cmd
```

For navigation-only training on Windows:

```bat
tools\rl\train_navigation_forever.cmd
```

The combat helper resumes `rl-checkpoints-direct-circle`. The navigation wrapper
resumes `rl-checkpoints-navigation`. Both train in repeated chunks, checkpoint
during each chunk, and export `assets/ai/rl_enemy_policy.json`.
By default it rebuilds the desktop jar before training and after every chunk, so
the packaged game contains the latest exported policy. Stop it with `Ctrl-C`;
the interrupt handler exports and packages the latest saved checkpoint, so at
most the work since the previous checkpoint is lost.

Useful knobs for the forever helper:

```bash
RL_FOREVER_ITERATIONS=200 \
RL_CHECKPOINT_EVERY=10 \
RL_WORKERS=4 \
RL_NUM_GPUS=1 \
bash tools/rl/train_forever.sh
```

Useful navigation run:

```bash
RL_FOREVER_ITERATIONS=200 \
RL_CHECKPOINT_EVERY=10 \
bash tools/rl/train_navigation_forever.sh
```

On Windows Command Prompt:

```bat
set RL_FOREVER_ITERATIONS=200
set RL_CHECKPOINT_EVERY=10
set RL_WORKERS=4
set RL_NUM_GPUS=1
set RL_NO_REWARD_SUMMARY=1
tools\rl\train_forever.cmd
```

`RL_NUM_GPUS=1` only works if Ray/PyTorch can see a CUDA-enabled Torch install.
If CUDA is not set up, leave `RL_NUM_GPUS=0`. `RL_MAP_IDS=map001,map006` can be
used to focus training on specific maps. `RL_PACKAGE_EVERY_CYCLES=0` disables
jar rebuilding during long runs if you only need the JSON policy exported.
`RL_NO_REWARD_SUMMARY=1` disables the per-step reward bucket accounting and is
the faster mode for long unattended runs.
If Ray still fails natively on Windows after switching to JDK 17 or 21, try
`set RL_RAY_NUM_CPUS=1` to reduce Ray's native worker pressure while diagnosing.
Set `RL_ALLOW_UNTESTED_JAVA=1` only when you intentionally want to bypass the
Java version guard.

## Export And Use In Game

After training, export the shared PPO actor into the lightweight JSON asset the
game can load:

```bash
python tools/rl/export_policy.py
```

That writes `assets/ai/rl_enemy_policy.json`. When this file is present in the
packaged assets, regular gameplay enemies use it for direct throttle and turn
decisions.
The Java training environment still keeps heuristic opponents by default, so
future training runs do not accidentally train only against the last exported
policy.

## Current Environment Contract

- Observation size: `30` floats per learner.
- Action size: `2` floats per learner, `[throttle, turn]`, each in `[-1, 1]`.
- Training opponents still use the existing Java AI.
- Rewards combine safe-circle approach/survival, staying near the circle center,
  slowing down inside the circle, edge recovery, opponent pressure, impact
  credit, eliminations, and final placement.
- `--objective navigation` removes heuristic opponents and pickups, allows a
  solo learner round to continue after each circle, scales circle and edge
  safety rewards higher, and gives a completion reward for surviving the full
  episode.
- Attack rewards are safety-gated. Unsafe impacts and attack-adjacent
  eliminations add negative `attack` reward so a car cannot make reckless hits
  look profitable by suiciding shortly afterward.

The policy used in game is deterministic: it uses the first two exported actor
outputs as direct control values, then clamps them into `[-1, 1]`.

## Evaluate

Run the exported policy in the Java headless environment and report win rate plus
direct throttle oscillation metrics:

```bash
python tools/rl/evaluate_policy.py --episodes 20 --field-size 10
```
