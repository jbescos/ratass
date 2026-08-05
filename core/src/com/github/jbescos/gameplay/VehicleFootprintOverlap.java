package com.github.jbescos.gameplay;

/** Allocation-free overlap test for two oriented rectangular vehicle footprints. */
public final class VehicleFootprintOverlap {
    private VehicleFootprintOverlap() {}

    public static boolean overlaps(
            float centerAX,
            float centerAY,
            float angleA,
            float halfWidthA,
            float halfLengthA,
            float centerBX,
            float centerBY,
            float angleB,
            float halfWidthB,
            float halfLengthB,
            float clearanceMargin) {
        float dx = centerBX - centerAX;
        float dy = centerBY - centerAY;
        float sideAX = (float) Math.cos(angleA);
        float sideAY = (float) Math.sin(angleA);
        float forwardAX = -sideAY;
        float forwardAY = sideAX;
        float sideBX = (float) Math.cos(angleB);
        float sideBY = (float) Math.sin(angleB);
        float forwardBX = -sideBY;
        float forwardBY = sideBX;
        float margin = Math.max(0f, clearanceMargin);

        return overlapsOnAxis(
                        dx, dy, sideAX, sideAY,
                        halfWidthA, halfLengthA, sideAX, sideAY, forwardAX, forwardAY,
                        halfWidthB, halfLengthB, sideBX, sideBY, forwardBX, forwardBY,
                        margin)
                && overlapsOnAxis(
                        dx, dy, forwardAX, forwardAY,
                        halfWidthA, halfLengthA, sideAX, sideAY, forwardAX, forwardAY,
                        halfWidthB, halfLengthB, sideBX, sideBY, forwardBX, forwardBY,
                        margin)
                && overlapsOnAxis(
                        dx, dy, sideBX, sideBY,
                        halfWidthA, halfLengthA, sideAX, sideAY, forwardAX, forwardAY,
                        halfWidthB, halfLengthB, sideBX, sideBY, forwardBX, forwardBY,
                        margin)
                && overlapsOnAxis(
                        dx, dy, forwardBX, forwardBY,
                        halfWidthA, halfLengthA, sideAX, sideAY, forwardAX, forwardAY,
                        halfWidthB, halfLengthB, sideBX, sideBY, forwardBX, forwardBY,
                        margin);
    }

    private static boolean overlapsOnAxis(
            float dx,
            float dy,
            float axisX,
            float axisY,
            float halfWidthA,
            float halfLengthA,
            float sideAX,
            float sideAY,
            float forwardAX,
            float forwardAY,
            float halfWidthB,
            float halfLengthB,
            float sideBX,
            float sideBY,
            float forwardBX,
            float forwardBY,
            float clearanceMargin) {
        float separation = Math.abs(dx * axisX + dy * axisY);
        float radiusA = halfWidthA * Math.abs(sideAX * axisX + sideAY * axisY)
                + halfLengthA * Math.abs(forwardAX * axisX + forwardAY * axisY);
        float radiusB = halfWidthB * Math.abs(sideBX * axisX + sideBY * axisY)
                + halfLengthB * Math.abs(forwardBX * axisX + forwardBY * axisY);
        return separation < radiusA + radiusB + clearanceMargin;
    }
}
