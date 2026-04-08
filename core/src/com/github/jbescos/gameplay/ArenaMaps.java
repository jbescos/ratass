package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

public final class ArenaMaps {
    private ArenaMaps() {
    }

    public static Array<ArenaMap> createDefaultSet() {
        Array<ArenaMap> maps = new Array<ArenaMap>();
        maps.add(createBoilerDeck());
        maps.add(createCrosswindJunction());
        maps.add(createSplitShift());
        maps.add(createDonutBowl());
        return maps;
    }

    private static ArenaMap createBoilerDeck() {
        ArenaMap.Builder builder = ArenaMap.builder("boiler-deck", "Boiler Deck")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, 38f, 24f))
                .spawn(SpawnPoint.facingPoint(0f, -8.2f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-12f, 6.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(12f, 6.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, 8.2f, 0f, 0f));

        addGridRecoveryPoints(builder, 12f, 7.5f, 3, 3);
        return builder.build();
    }

    private static ArenaMap createCrosswindJunction() {
        ArenaMap.Builder builder = ArenaMap.builder("crosswind-junction", "Crosswind Junction")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, 36f, 9.2f))
                .solid(ArenaShape.rectangle(0f, 0f, 15.4f, 25.2f))
                .spawn(SpawnPoint.facingPoint(0f, -8.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-12.6f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(12.6f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, 8.8f, 0f, 0f));

        addAxisRecoveryPoints(builder, 12f, 8.5f, 4f);
        return builder.build();
    }

    private static ArenaMap createSplitShift() {
        ArenaMap.Builder builder = ArenaMap.builder("split-shift", "Split Shift")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(-9.8f, 0f, 13.2f, 13f))
                .solid(ArenaShape.rectangle(9.8f, 0f, 13.2f, 13f))
                .solid(ArenaShape.rectangle(0f, 0f, 8.8f, 5.8f))
                .spawn(SpawnPoint.facingPoint(-11.2f, -3.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-11.2f, 3.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(11.2f, -3.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(11.2f, 3.8f, 0f, 0f));

        builder.recoveryPoint(-12f, -4f)
                .recoveryPoint(-12f, 0f)
                .recoveryPoint(-12f, 4f)
                .recoveryPoint(-8f, 0f)
                .recoveryPoint(-4f, 0f)
                .recoveryPoint(0f, 0f)
                .recoveryPoint(4f, 0f)
                .recoveryPoint(8f, 0f)
                .recoveryPoint(12f, -4f)
                .recoveryPoint(12f, 0f)
                .recoveryPoint(12f, 4f);
        return builder.build();
    }

    private static ArenaMap createDonutBowl() {
        ArenaMap.Builder builder = ArenaMap.builder("donut-bowl", "Donut Bowl")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.circle(0f, 0f, 12.5f))
                .hole(ArenaShape.circle(0f, 0f, 4.4f))
                .spawn(SpawnPoint.facingPoint(0f, -8.4f, 1f, -8.4f))
                .spawn(SpawnPoint.facingPoint(8.4f, 0f, 8.4f, 1f))
                .spawn(SpawnPoint.facingPoint(0f, 8.4f, -1f, 8.4f))
                .spawn(SpawnPoint.facingPoint(-8.4f, 0f, -8.4f, -1f));

        addRingRecoveryPoints(builder, 8.2f, 12);
        return builder.build();
    }

    private static void addGridRecoveryPoints(
            ArenaMap.Builder builder,
            float halfWidth,
            float halfHeight,
            int columns,
            int rows) {
        for (int y = 0; y < rows; y++) {
            float normalizedY = rows == 1 ? 0.5f : (float) y / (rows - 1);
            float pointY = MathUtils.lerp(-halfHeight, halfHeight, normalizedY);
            for (int x = 0; x < columns; x++) {
                float normalizedX = columns == 1 ? 0.5f : (float) x / (columns - 1);
                float pointX = MathUtils.lerp(-halfWidth, halfWidth, normalizedX);
                builder.recoveryPoint(pointX, pointY);
            }
        }
    }

    private static void addAxisRecoveryPoints(
            ArenaMap.Builder builder,
            float horizontalExtent,
            float verticalExtent,
            float spacing) {
        builder.recoveryPoint(0f, 0f);

        for (float x = spacing; x <= horizontalExtent; x += spacing) {
            builder.recoveryPoint(-x, 0f);
            builder.recoveryPoint(x, 0f);
        }

        for (float y = spacing; y <= verticalExtent; y += spacing) {
            builder.recoveryPoint(0f, -y);
            builder.recoveryPoint(0f, y);
        }
    }

    private static void addRingRecoveryPoints(
            ArenaMap.Builder builder,
            float radius,
            int points) {
        for (int i = 0; i < points; i++) {
            float angle = MathUtils.PI2 * i / points;
            builder.recoveryPoint(MathUtils.cos(angle) * radius, MathUtils.sin(angle) * radius);
        }
    }
}
