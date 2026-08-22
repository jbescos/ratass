package com.github.jbescos.gameplay;

/** Box2D layers that separate road geometry from car-only collision envelopes. */
public final class CarCollisionLayers {
    public static final short ROAD = 0x0001;
    public static final short CARS = 0x0002;
    public static final short MAP_WALLS = 0x0004;

    private CarCollisionLayers() {
    }

    public static short roadFixtureCategory(boolean enlargedCarCollision) {
        return enlargedCarCollision ? ROAD : (short) (ROAD | CARS);
    }

    public static short roadFixtureMask(boolean enlargedCarCollision) {
        return enlargedCarCollision ? MAP_WALLS : (short) (MAP_WALLS | CARS);
    }

    public static boolean canCollide(
            short categoryA,
            short maskA,
            short categoryB,
            short maskB) {
        return (maskA & categoryB) != 0 && (maskB & categoryA) != 0;
    }
}
