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
        return ArenaMap.builder("boiler-deck", "Boiler Deck")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, 22f, 12f))
                .spawn(SpawnPoint.facingPoint(0f, -3.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-6.4f, 3.4f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(6.2f, 3.0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, 4.4f, 0f, 0f))
                .recoveryPoint(0f, 0f)
                .recoveryPoint(-5.8f, 0f)
                .recoveryPoint(5.8f, 0f)
                .recoveryPoint(0f, 3.2f)
                .recoveryPoint(0f, -3.2f)
                .build();
    }

    private static ArenaMap createCrosswindJunction() {
        return ArenaMap.builder("crosswind-junction", "Crosswind Junction")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, 20.6f, 4.8f))
                .solid(ArenaShape.rectangle(0f, 0f, 8.8f, 13.2f))
                .spawn(SpawnPoint.facingPoint(0f, -4.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-6.6f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(6.6f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, 4.8f, 0f, 0f))
                .recoveryPoint(0f, 0f)
                .recoveryPoint(-6.2f, 0f)
                .recoveryPoint(6.2f, 0f)
                .recoveryPoint(0f, 4.1f)
                .recoveryPoint(0f, -4.1f)
                .build();
    }

    private static ArenaMap createSplitShift() {
        return ArenaMap.builder("split-shift", "Split Shift")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(-5.4f, 0f, 8.2f, 7.8f))
                .solid(ArenaShape.rectangle(5.4f, 0f, 8.2f, 7.8f))
                .solid(ArenaShape.rectangle(0f, 0f, 4.8f, 3.4f))
                .spawn(SpawnPoint.facingPoint(-6.2f, -2.2f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-6.2f, 2.2f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(6.2f, -2.2f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(6.2f, 2.2f, 0f, 0f))
                .recoveryPoint(-5.4f, 0f)
                .recoveryPoint(-1.6f, 0f)
                .recoveryPoint(0f, 0f)
                .recoveryPoint(1.6f, 0f)
                .recoveryPoint(5.4f, 0f)
                .build();
    }

    private static ArenaMap createDonutBowl() {
        ArenaMap.Builder builder = ArenaMap.builder("donut-bowl", "Donut Bowl")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.circle(0f, 0f, 6.9f))
                .hole(ArenaShape.circle(0f, 0f, 2.35f))
                .spawn(SpawnPoint.facingPoint(0f, -4.85f, 1f, -4.85f))
                .spawn(SpawnPoint.facingPoint(4.85f, 0f, 4.85f, 1f))
                .spawn(SpawnPoint.facingPoint(0f, 4.85f, -1f, 4.85f))
                .spawn(SpawnPoint.facingPoint(-4.85f, 0f, -4.85f, -1f));

        addRingRecoveryPoints(builder, 4.75f, 8);
        return builder.build();
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
