# ratass

Small LibGDX rooftop car-sumo prototype.

## Objective

Shove every other car off the platform and stay on the roof yourself. Last car
remaining wins the round.

## Controls

- `WASD` or arrow keys: drive and steer
- `R` or `Enter`: restart the round
- `Esc`: quit

## Build for desktop

`bash ./gradlew desktop:dist`

The current Gradle wrapper works cleanly with a compatible JDK such as Java 17.

## Run on desktop

`java -jar desktop/build/libs/desktop-1.0.jar`
