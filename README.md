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

- Browser module packaging: `mvn -Phtml -pl html -am package`
- Browser GWT compile attempt: `mvn -Phtml -pl html -am -Dgwt.compiler.skip=false package`
- iOS module: `mvn -Pios -pl ios -am package`

The browser GWT compile is still failing at the existing `jsinterop`
module-resolution issue, so the Maven migration keeps that step optional.
The Android module is still kept in the repository but is not part of the Maven
reactor yet.
