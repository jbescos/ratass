package com.github.jbescos.gameplay.roguelite;

/** Pure geometry checks used by contextual card abilities. */
public final class RogueliteOpponentGeometry {
    private static final float FORWARD_RANGE_CAR_LENGTHS = 3.5f;
    private static final float BLOCKING_WIDTH_RATIO = 0.42f;

    private RogueliteOpponentGeometry() {
    }

    public static boolean blocksStraightPowerup(
            float deltaX,
            float deltaY,
            float forwardX,
            float forwardY,
            float sideX,
            float sideY,
            float carWidth,
            float carHeight,
            float opponentWidth,
            float opponentHeight) {
        float forward = deltaX * forwardX + deltaY * forwardY;
        float maximumForward =
                Math.max(carHeight, opponentHeight) * FORWARD_RANGE_CAR_LENGTHS;
        if (forward <= 0f || forward >= maximumForward) {
            return false;
        }
        float lateral = Math.abs(deltaX * sideX + deltaY * sideY);
        float blockingHalfWidth =
                Math.max(0f, carWidth + opponentWidth) * BLOCKING_WIDTH_RATIO;
        return lateral < blockingHalfWidth;
    }
}
