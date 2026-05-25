# ratass

Small LibGDX self-driving racing prototype.

## Objective

Cars race through ordered checkpoints on circuit maps. The first car to complete
the configured laps starts a short finish timeout for the rest of the field.

## Controls

- `WASD` or arrow keys: drive and steer
- `Space`: handbrake for drifting
- `Space + W + A/D`: pivot-spin from a stop
- `R` or `Enter`: restart the round
- `Esc`: quit

## Build for desktop with Maven

`mvn -pl desktop -am package`

The Maven build uses Java 8 source compatibility and works cleanly with a
modern JDK such as Java 17.

## Run on desktop

`java -jar desktop/target/ratass-desktop-1.0.jar`

## Optional Maven modules

- Browser build: `mvn -Phtml -pl html -am package`
- iOS module: `mvn -Pios -pl ios -am package`

The browser build output is written to `html/target/ratass-html-1.0/`.
The Android module is still kept in the repository but is not part of the Maven
reactor yet.

## RL race training

The old scripted/evolution AI has been removed. The remaining training path is
the checkpoint-race reinforcement-learning environment under `tools/rl/`.

Build the desktop jar, then run a race-training smoke or curriculum:

`mvn -pl desktop -am package`

`python tools/rl/train_rllib.py --iterations 1 --max-action-steps 200`

`bash tools/rl/train_forever.sh curriculum`
