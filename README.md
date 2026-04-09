# ratass

Small LibGDX rooftop car-sumo prototype.

## Objective

Shove every other car off the platform and stay on the roof yourself. Last car
remaining wins the round.

## Controls

- `WASD` or arrow keys: drive and steer
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
