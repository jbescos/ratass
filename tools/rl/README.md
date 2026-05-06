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

## Train

```bash
python tools/rl/train_rllib.py --iterations 25
```

The current policy is tactical, not direct throttle/turn. Its default checkpoint
directory is `rl-checkpoints-tactical`; do not resume older checkpoints from
`rl-checkpoints`, because those were trained with the previous action meaning.

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

For long curriculum training, the helper script rebuilds the desktop jar, warms
up on the easiest map, focuses on `map001`, drills the hard maps
(`map001,map003,map006`), then finishes on the full map set:

```bash
bash tools/rl/train_for_hours.sh
```

The phase lengths can be tuned with environment variables:
`RL_WARMUP_ITERATIONS`, `RL_MAP001_ITERATIONS`, `RL_HARD_ITERATIONS`, and
`RL_MIXED_ITERATIONS`.

The helper starts a fresh warmup phase, then resumes that same tactical checkpoint
for the later curriculum phases. It defaults to single-process Ray because that
has produced faster iteration feedback in this project. It also defaults to six
controlled learners so the shared policy practices
fighting around several RL-driven cars instead of learning mostly one-car
survival against heuristic opponents.

For quick experiments, one controlled learner is still useful. For the long run,
multiple controlled learners gives the shared policy more self-play pressure.

## Export And Use In Game

After training, export the shared PPO actor into the lightweight JSON asset the
game can load:

```bash
python tools/rl/export_policy.py
```

That writes `assets/ai/rl_enemy_policy.json`. When this file is present in the
packaged assets, regular gameplay enemies use it for tactical intent decisions.
The scripted car controller still handles throttle, steering, recovery, and
collision escape.
The Java training environment still keeps heuristic opponents by default, so
future training runs do not accidentally train only against the last exported
policy.

## Current Environment Contract

- Observation size: `30` floats per learner.
- Action size: `2` floats per learner, `[mode, style]`, each in `[-1, 1]`.
  The game maps `mode` to recover/flank/attack/hunt intent and uses `style` for
  flank side and aggression bias.
- Training opponents still use the existing Java AI.
- Rewards combine survival, edge recovery, opponent pressure, impact credit,
  eliminations, and winning.

The policy used in game is deterministic: it uses the first two exported actor
outputs as tactical intent values, then clamps them into `[-1, 1]`.

## Evaluate

Run the exported policy in the Java headless environment and report win rate plus
resulting scripted throttle oscillation metrics:

```bash
python tools/rl/evaluate_policy.py --episodes 20 --field-size 10
```
