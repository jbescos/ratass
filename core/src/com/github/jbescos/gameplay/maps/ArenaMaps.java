package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;
import com.github.jbescos.gameplay.SpawnPoint;

public final class ArenaMaps {
    private static final float DEFAULT_MAP_SCALE = 4f;
    private static final int TRAINING_SPAWN_COUNT = 24;

    private ArenaMaps() {
    }

    public static Array<ArenaMap> createDefaultSet() {
        return createDefaultSet(DEFAULT_MAP_SCALE);
    }

    public static Array<ArenaMap> createDefaultSet(float mapScale) {
        return ImageArenaMapLoader.loadDefaultMaps(mapScale);
    }

    public static Array<ArenaMap> createHeadlessTrainingSet() {
        return createHeadlessTrainingSet(DEFAULT_MAP_SCALE);
    }

    public static Array<ArenaMap> createHeadlessTrainingSet(float mapScale) {
        float scale = Math.max(0.1f, mapScale);
        if (Gdx.files != null) {
            try {
                Array<ArenaMap> pictureMaps = ImageArenaMapLoader.loadDefaultMaps(scale);
                if (pictureMaps.size > 0) {
                    return pictureMaps;
                }
            } catch (RuntimeException ignored) {
                // Headless unit/smoke runs can execute without LibGDX file services.
            }
        }

        Array<ArenaMap> maps = new Array<ArenaMap>();
        maps.add(createTrainingBox(scale));
        maps.add(createTrainingBowl(scale));
        return maps;
    }

    private static ArenaMap createTrainingBox(float scale) {
        ArenaMap.Builder builder = ArenaMap.builder("training_box", "Training Box")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, 42f, 28f))
                .scale(scale);
        addRingSpawns(builder, 10.5f * scale, TRAINING_SPAWN_COUNT);
        addRecoveryGrid(builder, 14f * scale, 8f * scale, 5, 3);
        return builder.build();
    }

    private static ArenaMap createTrainingBowl(float scale) {
        ArenaMap.Builder builder = ArenaMap.builder("training_bowl", "Training Bowl")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.circle(0f, 0f, 17.5f))
                .scale(scale);
        addRingSpawns(builder, 10f * scale, TRAINING_SPAWN_COUNT);
        addRecoveryGrid(builder, 9f * scale, 9f * scale, 5, 5);
        return builder.build();
    }

    private static void addRingSpawns(ArenaMap.Builder builder, float radius, int count) {
        for (int i = 0; i < count; i++) {
            float angle = MathUtils.PI2 * i / count;
            float x = MathUtils.cos(angle) * radius;
            float y = MathUtils.sin(angle) * radius;
            builder.spawn(SpawnPoint.facingPoint(x, y, 0f, 0f));
            builder.recoveryPoint(x * 0.62f, y * 0.62f);
        }
    }

    private static void addRecoveryGrid(
            ArenaMap.Builder builder,
            float halfWidth,
            float halfHeight,
            int columns,
            int rows) {
        for (int row = 0; row < rows; row++) {
            float y =
                    rows == 1
                            ? 0f
                            : MathUtils.lerp(-halfHeight, halfHeight, row / (float) (rows - 1));
            for (int column = 0; column < columns; column++) {
                float x =
                        columns == 1
                                ? 0f
                                : MathUtils.lerp(
                                        -halfWidth,
                                        halfWidth,
                                        column / (float) (columns - 1));
                builder.recoveryPoint(x, y);
            }
        }
    }
}
