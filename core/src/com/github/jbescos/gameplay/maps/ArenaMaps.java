package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;
import com.github.jbescos.gameplay.SpawnPoint;
import java.util.HashSet;

public final class ArenaMaps {
    private static final float DEFAULT_MAP_SCALE = 8f;
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
                Array<ArenaMap> trainingMaps = ImageArenaMapLoader.loadTrainingMaps(scale);
                if (trainingMaps.size > 0) {
                    return trainingMaps;
                }
            } catch (RuntimeException ignored) {
                // Synthetic training maps are optional in smoke runs and packaged builds.
            }
            try {
                Array<ArenaMap> gameMaps = ImageArenaMapLoader.loadDefaultMaps(scale);
                if (gameMaps.size > 0) {
                    return gameMaps;
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

    public static Array<ArenaMap> createSelectedSet(String commaSeparatedMapIds) {
        HashSet<String> requestedIds = new HashSet<String>();
        if (commaSeparatedMapIds != null) {
            String[] tokens = commaSeparatedMapIds.split(",");
            for (int i = 0; i < tokens.length; i++) {
                String mapId = tokens[i].trim();
                if (!mapId.isEmpty()) {
                    requestedIds.add(mapId);
                }
            }
        }
        if (requestedIds.isEmpty()) {
            return createHeadlessTrainingSet();
        }
        if (Gdx.files == null) {
            throw new IllegalStateException("Selected picture maps require Gdx.files.");
        }

        Array<ArenaMap> selected = new Array<ArenaMap>();
        HashSet<String> foundIds = new HashSet<String>();
        Array<ArenaMap> trainingMaps =
                ImageArenaMapLoader.loadTrainingMaps(DEFAULT_MAP_SCALE, requestedIds);
        Array<ArenaMap> gameMaps =
                ImageArenaMapLoader.loadDefaultMaps(DEFAULT_MAP_SCALE, requestedIds);
        addSelectedMaps(selected, foundIds, trainingMaps, requestedIds);
        addSelectedMaps(selected, foundIds, gameMaps, requestedIds);

        HashSet<String> missingIds = new HashSet<String>(requestedIds);
        missingIds.removeAll(foundIds);
        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("Unknown map id(s): " + missingIds);
        }
        return selected;
    }

    private static void addSelectedMaps(
            Array<ArenaMap> destination,
            HashSet<String> foundIds,
            Array<ArenaMap> candidates,
            HashSet<String> requestedIds) {
        for (int i = 0; i < candidates.size; i++) {
            ArenaMap map = candidates.get(i);
            if (requestedIds.contains(map.getId()) && foundIds.add(map.getId())) {
                destination.add(map);
            }
        }
    }

    private static ArenaMap createTrainingBox(float scale) {
        ArenaMap.Builder builder = ArenaMap.builder("training_box", "Training Box")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, 42f, 28f))
                .scale(scale);
        addRectangleRoute(builder, 14f * scale, 8f * scale);
        addRingSpawns(builder, 10.5f * scale, TRAINING_SPAWN_COUNT);
        addRecoveryGrid(builder, 14f * scale, 8f * scale, 5, 3);
        return builder.build();
    }

    private static ArenaMap createTrainingBowl(float scale) {
        ArenaMap.Builder builder = ArenaMap.builder("training_bowl", "Training Bowl")
                .focusPoint(0f, 0f)
                .solid(ArenaShape.circle(0f, 0f, 17.5f))
                .scale(scale);
        addCircleRoute(builder, 10f * scale, 24);
        addRingSpawns(builder, 10f * scale, TRAINING_SPAWN_COUNT);
        addRecoveryGrid(builder, 9f * scale, 9f * scale, 5, 5);
        return builder.build();
    }

    private static void addRectangleRoute(ArenaMap.Builder builder, float halfWidth, float halfHeight) {
        builder.routePoint(-halfWidth, -halfHeight);
        builder.routePoint(halfWidth, -halfHeight);
        builder.routePoint(halfWidth, halfHeight);
        builder.routePoint(-halfWidth, halfHeight);
    }

    private static void addCircleRoute(ArenaMap.Builder builder, float radius, int count) {
        for (int i = 0; i < count; i++) {
            float angle = MathUtils.PI2 * i / count;
            builder.routePoint(MathUtils.cos(angle) * radius, MathUtils.sin(angle) * radius);
        }
    }

    private static void addRingSpawns(ArenaMap.Builder builder, float radius, int count) {
        for (int i = 0; i < count; i++) {
            float angle = MathUtils.PI2 * i / count;
            float x = MathUtils.cos(angle) * radius;
            float y = MathUtils.sin(angle) * radius;
            builder.spawn(SpawnPoint.facingPoint(x, y, 0f, 0f));
            builder.checkpoint(SpawnPoint.facingPoint(x, y, 0f, 0f));
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
