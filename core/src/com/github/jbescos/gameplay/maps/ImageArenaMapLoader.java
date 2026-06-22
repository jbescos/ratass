package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.MaskArenaShape;
import com.github.jbescos.gameplay.SpawnPoint;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class ImageArenaMapLoader {
    private static final String MAPS_DIRECTORY = "maps";
    private static final String TRAINING_MAPS_DIRECTORY = "tools/rl/trainingMaps";
    private static final String MASK_SUFFIX = "_mask.png";
    private static final String IMAGE_SUFFIX = ".png";
    private static final String CACHE_SUFFIX = ".json.gz";
    private static final int CACHE_VERSION = 118;
    private static final boolean USE_ROUTE_LINE_MARKERS = false;
    private static final float BASE_WORLD_HEIGHT = 22f;
    private static final String MAP005_ID = "map005";
    private static final float MAP005_WORLD_SCALE = 1.25f;
    private static final int MAX_SEQUENTIAL_MAP_SCAN = 1000;
    private static final int MAX_SHAPE_MASK_LONG_SIDE = 512;
    private static final int MIN_MARKER_PIXELS = 24;
    private static final int MIN_SPAWN_HINT_PIXELS = 8;
    private static final int ROUTE_LINE_MARKER_MIN_PIXELS = 8;
    private static final int ROUTE_LINE_CONTINUOUS_MIN_PIXELS = 96;
    private static final int ROUTE_LINE_SKELETON_MAX_ITERATIONS = 96;
    private static final float ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD = 0.75f;
    private static final float ROUTE_LINE_ORDER_MIN_VISITED_FRACTION = 0.55f;
    private static final int ROUTE_LINE_GAP_SEARCH_RADIUS = 5;
    private static final float ROUTE_LINE_VERTEX_SMOOTHING_STEP_WORLD = 0.55f;
    private static final float ROUTE_LINE_CARDINAL_TENSION = 0.18f;
    private static final float ROUTE_LINE_MARKER_SWEEP_MAX_LATERAL = 10.0f;
    private static final float ROUTE_LINE_MARKER_SWEEP_GATE_PADDING = 0.85f;
    private static final float ROUTE_LINE_MARKER_SWEEP_CLEARANCE_STEP = 0.35f;
    private static final int ROUTE_LINE_DENSE_MARKER_ROUTE_SHAPE_LIMIT = 20;
    private static final int MARKER_PLAYABLE_CONTEXT_RADIUS = 10;
    private static final int EXPANDED_SPAWN_GRID_COUNT = 20;
    private static final int EXPANDED_SPAWN_GRID_COLUMNS = 2;
    private static final float EXPANDED_SPAWN_ROW_SPACING_WORLD = 6.32f;
    private static final float EXPANDED_SPAWN_MIN_ROW_SPACING_PIXELS = 8f;
    private static final int CHECKPOINT_CENTER_GATE_SAMPLES = 25;
    private static final int RECOVERY_COLUMNS = 18;
    private static final int RECOVERY_ROWS = 10;
    private static final float RECOVERY_SAFE_MARGIN = 0.85f;
    private static final float CHECKPOINT_ORDER_ROUTE_MARGIN = 1.20f;
    private static final float CHECKPOINT_ORDER_SCORE_EPSILON = 0.001f;
    private static final int CHECKPOINT_ORDER_TRACE_DISTANCE_PIXELS = 8;
    private static final int CHECKPOINT_ORDER_DOT_RADIUS = 4;
    private static final int CHECKPOINT_ORDER_LABEL_SCALE = 3;
    private static final int CHECKPOINT_ORDER_LABEL_GAP = 9;
    private static final int CHECKPOINT_ORDER_LABEL_COLOR = rgba8888(88, 88, 88);
    private static final int CHECKPOINT_ORDER_MARKER_MIN_PIXELS = 8;
    private static final int CHECKPOINT_ORDER_MANUAL_DISTANCE_PIXELS = 120;
    private static final float ROUTE_CENTERLINE_STEP_WORLD = 2.0f;
    private static final float ROUTE_CENTERLINE_MIN_CLEARANCE = 0.42f;
    private static final float ROUTE_CENTERLINE_START_CLOSE_DISTANCE = 5.5f;
    private static final float ROUTE_CENTERLINE_BORDER_SCAN_STEP = 0.25f;
    private static final float ROUTE_CENTERLINE_BORDER_SCAN_MAX_DISTANCE = 28f;
    private static final float ROUTE_CENTERLINE_MIN_BOUNDS_COVERAGE = 0.52f;
    private static final float ROUTE_CENTERLINE_PLAYABLE_BOUNDS_SAMPLES_PER_WORLD_UNIT = 3.0f;
    private static final int ROUTE_CENTERLINE_PLAYABLE_BOUNDS_MIN_SAMPLES = 64;
    private static final int ROUTE_CENTERLINE_PLAYABLE_BOUNDS_MAX_SAMPLES = 360;
    private static final float ROUTE_CENTERLINE_LOOP_PRUNE_DISTANCE = 4.8f;
    private static final int ROUTE_CENTERLINE_LOOP_PRUNE_MIN_SPAN = 5;
    private static final int ROUTE_CENTERLINE_LOOP_PRUNE_MAX_SPAN = 42;
    private static final int ROUTE_CENTERLINE_LOOP_PRUNE_PASSES = 8;
    private static final int ROUTE_CENTERLINE_MAX_STEPS = 1400;
    private static final int ROUTE_CENTERLINE_VISITED_IGNORE_STEPS = 14;
    private static final float ROUTE_CENTERLINE_VISITED_DISTANCE_FACTOR = 1.30f;
    private static final int ROUTE_CENTERLINE_SMOOTHING_PASSES = 2;
    private static final float ROUTE_CENTERLINE_SMOOTHING_SAMPLE_STEP = 0.65f;
    private static final float ROUTE_CENTERLINE_SMOOTHING_MIN_CLEARANCE = 0.32f;
    private static final float ROUTE_CENTERLINE_EXACT_MIN_CLEARANCE = 0.04f;
    private static final float ROUTE_CENTERLINE_EXACT_SAMPLE_STEP = 0.22f;
    private static final float ROUTE_CENTERLINE_CLEARANCE_WEIGHT = 2.70f;
    private static final float ROUTE_CENTERLINE_FORWARD_WEIGHT = 2.20f;
    private static final float ROUTE_CENTERLINE_TURN_WEIGHT = 1.50f;
    private static final float ROUTE_CENTERLINE_WAYPOINT_REACHED_DISTANCE = 0.65f;
    private static final int ROUTE_CENTERLINE_WAYPOINT_MAX_STEPS = 180;
    private static final float ROUTE_LINE_START_LOOKAHEAD_WORLD = 4.0f;
    private static final float ROUTE_HINT_DIRECT_STEP_WORLD = 1.25f;
    private static final float ROUTE_HINT_SEGMENT_MARGIN_WORLD = 1.75f;
    private static final float ROUTE_HINT_GENERATED_LOOKAHEAD_WORLD = ROUTE_CENTERLINE_STEP_WORLD * 6f;
    private static final float ROUTE_HINT_GENERATED_ENTRY_DISTANCE_WORLD = ROUTE_CENTERLINE_STEP_WORLD * 3.5f;
    private static final float ROUTE_HINT_MIN_RADIUS_PIXELS = 5f;
    private static final int ROUTE_HINT_COMPONENT_MERGE_GAP_PIXELS = 8;
    private static final int BLUE_ROUTE_HINT_MIN_AREA = 96;
    private static final int BLUE_ROUTE_HINT_MIN_DIAMETER_PIXELS = 18;
    private static final float ROUTE_METADATA_SAMPLE_STEP_WORLD = 0.75f;
    private static final float ROUTE_METADATA_TANGENT_SAMPLE_DISTANCE = 1.50f;
    private static final float ROUTE_METADATA_CURVATURE_SAMPLE_DISTANCE = 3.20f;
    private static final float ROUTE_METADATA_CLEARANCE_DISTANCE = 24f;
    private static final float ROUTE_METADATA_CLEARANCE_STEP = 0.35f;
    private static final float ROUTE_METADATA_CLEARANCE_EDGE_MARGIN = 0.10f;
    private static final float ROUTE_METADATA_CORNER_THRESHOLD = 0.14f;
    private static final float ROUTE_METADATA_CORNER_DISTANCE_NORMALIZER = 44f;
    private static final float ROUTE_METADATA_LOOKUP_CELLS_PER_WORLD_UNIT = 3.0f;
    private static final int ROUTE_METADATA_LOOKUP_MIN_SIZE = 64;
    private static final int ROUTE_METADATA_LOOKUP_MAX_SIZE = 256;
    private static final float[] ROUTE_CENTERLINE_ANGLES_DEG = {
        -70f, -52f, -36f, -22f, -10f, 0f, 10f, 22f, 36f, 52f, 70f
    };
    private static final float[] ROUTE_CENTERLINE_FALLBACK_ANGLES_DEG = {
        -145f, -120f, -95f, -70f, -48f, -28f, -12f, 0f, 12f, 28f, 48f, 70f, 95f, 120f, 145f
    };
    private static final String[][] DIGIT_BITMAPS = {
        {"111", "101", "101", "101", "101", "101", "111"},
        {"010", "110", "010", "010", "010", "010", "111"},
        {"111", "001", "001", "111", "100", "100", "111"},
        {"111", "001", "001", "111", "001", "001", "111"},
        {"101", "101", "101", "111", "001", "001", "001"},
        {"111", "100", "100", "111", "001", "001", "111"},
        {"111", "100", "100", "111", "101", "101", "111"},
        {"111", "001", "001", "010", "010", "100", "100"},
        {"111", "101", "101", "111", "101", "101", "111"},
        {"111", "101", "101", "111", "001", "001", "111"}
    };
    private static final boolean DEBUG_CHECKPOINT_ORDER =
            Boolean.getBoolean("ratass.debugCheckpointOrder");

    private ImageArenaMapLoader() {
    }

    static Array<ArenaMap> loadDefaultMaps(float mapScale) {
        return loadMapsFromDirectory(MAPS_DIRECTORY, mapScale, true);
    }

    static Array<ArenaMap> loadTrainingMaps(float mapScale) {
        return loadMapsFromDirectory(TRAINING_MAPS_DIRECTORY, mapScale, false);
    }

    private static Array<ArenaMap> loadMapsFromDirectory(
            String directoryPath,
            float mapScale,
            boolean requireMaps) {
        if (Gdx.files == null) {
            throw new IllegalStateException("Picture maps require Gdx.files to be available.");
        }

        float scale = Math.max(0.1f, mapScale);
        Array<FileHandle> maskFiles = findMaskFiles(directoryPath);
        if (maskFiles.size == 0) {
            if (!requireMaps) {
                return new Array<ArenaMap>();
            }
            throw new IllegalStateException(
                    "No picture map masks found. Add files like "
                            + directoryPath
                            + "/<name>_mask.png.");
        }

        Array<ArenaMap> maps = new Array<ArenaMap>();
        for (int i = 0; i < maskFiles.size; i++) {
            maps.add(loadMap(maskFiles.get(i), scale));
        }
        return maps;
    }

    private static Array<FileHandle> findMaskFiles(String directoryPath) {
        Array<FileHandle> maskFiles = new Array<FileHandle>();
        HashSet<String> seenPaths = new HashSet<String>();
        collectMaskFiles(maskFiles, seenPaths, Gdx.files.local(directoryPath));
        collectMaskFiles(maskFiles, seenPaths, Gdx.files.internal(directoryPath));

        if (maskFiles.size == 0) {
            for (int i = 0; i < MAX_SEQUENTIAL_MAP_SCAN; i++) {
                FileHandle handle = Gdx.files.internal(
                        directoryPath + "/map" + zeroPad3(i) + MASK_SUFFIX);
                if (handle.exists()) {
                    addMaskFile(maskFiles, seenPaths, handle);
                }
            }
        }

        maskFiles.sort(new Comparator<FileHandle>() {
            @Override
            public int compare(FileHandle left, FileHandle right) {
                return left.path().compareTo(right.path());
            }
        });
        return maskFiles;
    }

    private static void collectMaskFiles(
            Array<FileHandle> maskFiles,
            HashSet<String> seenPaths,
            FileHandle directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }
        FileHandle[] children = directory.list();
        for (int i = 0; i < children.length; i++) {
            FileHandle child = children[i];
            if (child.name().endsWith(MASK_SUFFIX)) {
                addMaskFile(maskFiles, seenPaths, child);
            }
        }
    }

    private static void addMaskFile(
            Array<FileHandle> maskFiles,
            HashSet<String> seenPaths,
            FileHandle maskFile) {
        if (seenPaths.add(maskFile.path())) {
            maskFiles.add(maskFile);
        }
    }

    private static ArenaMap loadMap(FileHandle maskFile, float mapScale) {
        String baseName =
                maskFile.name().substring(0, maskFile.name().length() - MASK_SUFFIX.length());
        FileHandle surfaceFile = maskFile.sibling(baseName + IMAGE_SUFFIX);
        String surfacePath = surfaceFile.path();
        if (!surfaceFile.exists()) {
            surfacePath = maskFile.path();
        }

        CachedMapData cachedMap = readCachedMap(maskFile, baseName, surfacePath, mapScale);
        if (cachedMap != null) {
            return buildCachedMap(cachedMap);
        }

        Pixmap mask = new Pixmap(maskFile);
        try {
            Array<CheckpointOrderMarker> checkpointOrderMarkers = collectCheckpointOrderMarkers(mask);
            removeCheckpointOrderAnnotations(mask);
            WorldSize worldSize = calculateWorldSize(baseName, mask.getWidth(), mask.getHeight(), mapScale);
            float worldWidth = worldSize.width;
            float worldHeight = worldSize.height;
            float worldMinX = -worldWidth * 0.5f;
            float worldMinY = -worldHeight * 0.5f;
            MaskParseResult parseResult = parseMask(mask);
            parseResult.checkpointOrderMarkers.addAll(checkpointOrderMarkers);
            MaskArenaShape shape = new MaskArenaShape(
                    parseResult.shapePlayable,
                    parseResult.shapeWidth,
                    parseResult.shapeHeight,
                    worldMinX,
                    worldMinY,
                    worldWidth,
                    worldHeight);

            ArenaMap.Builder builder = ArenaMap.builder(baseName, displayName(baseName))
                    .focusPoint(0f, 0f)
                    .surfaceImagePath(surfacePath)
                    .solid(shape);

            Array<SpawnPoint> spawnPoints =
                    buildSpawnPoints(parseResult, mask.getWidth(), mask.getHeight(), worldMinX, worldMinY,
                            worldWidth, worldHeight, shape);
            Array<Vector2> recoveryPoints = buildRecoveryPoints(
                    spawnPoints,
                    shape,
                    worldMinX,
                    worldMinY,
                    worldWidth,
                    worldHeight,
                    mapScale);
            ArenaMap routeOrderingMap =
                    buildRouteOrderingMap(baseName, surfacePath, shape, spawnPoints, recoveryPoints);
            Array<SpawnPoint> checkpoints =
                    buildCheckpoints(parseResult, mask.getWidth(), mask.getHeight(), worldMinX, worldMinY,
                            worldWidth, worldHeight, shape);
            alignCheckpointPathWithSpawns(spawnPoints, checkpoints);
            if (!parseResult.checkpointOrderByBorder) {
                orderCheckpointPathByRoute(checkpoints, routeOrderingMap);
            }
            alignCheckpointPathWithSpawns(spawnPoints, checkpoints);
            Array<RouteHintCircle> routeHints =
                    buildRouteHintCircles(
                            parseResult,
                            mask.getWidth(),
                            mask.getHeight(),
                            worldMinX,
                            worldMinY,
                            worldWidth,
                            worldHeight);
            boolean[] routeLinePixelsForRoute =
                    USE_ROUTE_LINE_MARKERS ? parseResult.routeLinePixels : null;
            Array<MarkerComponent> routeLineMarkersForRoute =
                    USE_ROUTE_LINE_MARKERS ? parseResult.routeLineMarkers : null;
            Array<Vector2> routePoints =
                    buildRouteCenterline(
                            routeOrderingMap,
                            spawnPoints,
                            checkpoints,
                            routeHints,
                            routeLinePixelsForRoute,
                            routeLineMarkersForRoute,
                            checkpointOrderMarkers,
                            mask.getWidth(),
                            mask.getHeight(),
                            worldMinX,
                            worldMinY,
                            worldWidth,
                            worldHeight);
            alignRouteCenterlineWithSpawnDirection(routePoints, spawnPoints, averageSpawnForward(spawnPoints));
            ArenaMap.RouteMetadata routeMetadata =
                    buildRouteMetadata(
                            routePoints,
                            shape,
                            worldMinX,
                            worldMinY,
                            worldWidth,
                            worldHeight);
            Array<RouteLineWaypoint> routeMarkers =
                    buildOrderedRouteMarkers(
                            routeOrderingMap,
                            routeLineMarkersForRoute,
                            routePoints,
                            mask.getWidth(),
                            mask.getHeight(),
                            worldMinX,
                            worldMinY,
                            worldWidth,
                            worldHeight);
            warnIfRouteLineIntersectsOffroad(routeOrderingMap, routePoints, routeMarkers, routeHints);
            for (int i = 0; i < spawnPoints.size; i++) {
                SpawnPoint spawnPoint = spawnPoints.get(i);
                builder.spawn(spawnPoint);
            }
            addRecoveryPoints(builder, recoveryPoints);
            for (int i = 0; i < checkpoints.size; i++) {
                builder.checkpoint(checkpoints.get(i));
            }
            for (int i = 0; i < routePoints.size; i++) {
                builder.routePoint(routePoints.get(i));
            }
            for (int i = 0; i < routeMarkers.size; i++) {
                RouteLineWaypoint routeMarker = routeMarkers.get(i);
                builder.routeMarkerPoint(routeMarker.point, routeMarker.progress);
            }
            builder.routeMetadata(routeMetadata);

            ArenaMap map = builder.build();
            writeCachedMap(maskFile, buildCachedMapData(
                    maskFile,
                    baseName,
                    surfacePath,
                    mapScale,
                    mask.getWidth(),
                    mask.getHeight(),
                    worldMinX,
                    worldMinY,
                    worldWidth,
                    worldHeight,
                    shape,
                    spawnPoints,
                    checkpoints,
                    routePoints,
                    routeMarkers,
                    routeMetadata,
                    recoveryPoints));
            return map;
        } finally {
            mask.dispose();
        }
    }

    private static ArenaMap buildCachedMap(CachedMapData cached) {
        MaskArenaShape shape = MaskArenaShape.precomputed(
                cached.shapePlayable,
                cached.shapeWidth,
                cached.shapeHeight,
                cached.distanceToVoidPixels,
                cached.distanceToPlayablePixels,
                cached.nearestPlayableX,
                cached.nearestPlayableY,
                cached.nearestVoidX,
                cached.nearestVoidY,
                cached.worldMinX,
                cached.worldMinY,
                cached.worldWidth,
                cached.worldHeight);

        ArenaMap.Builder builder = ArenaMap.builder(cached.baseName, displayName(cached.baseName))
                .focusPoint(0f, 0f)
                .surfaceImagePath(cached.surfacePath)
                .solid(shape);
        for (int i = 0; i < cached.spawnX.length; i++) {
            builder.spawn(new SpawnPoint(cached.spawnX[i], cached.spawnY[i], cached.spawnAngle[i]));
        }
        for (int i = 0; i < cached.checkpointX.length; i++) {
            boolean hasGate =
                    cached.checkpointHasGate != null
                            && cached.checkpointGateStartX != null
                            && cached.checkpointGateStartY != null
                            && cached.checkpointGateEndX != null
                            && cached.checkpointGateEndY != null
                            && i < cached.checkpointHasGate.length
                            && i < cached.checkpointGateStartX.length
                            && i < cached.checkpointGateStartY.length
                            && i < cached.checkpointGateEndX.length
                            && i < cached.checkpointGateEndY.length
                            && cached.checkpointHasGate[i];
            if (hasGate) {
                builder.checkpoint(SpawnPoint.facingGate(
                        cached.checkpointX[i],
                        cached.checkpointY[i],
                        cached.checkpointX[i] - MathUtils.sin(cached.checkpointAngle[i]),
                        cached.checkpointY[i] + MathUtils.cos(cached.checkpointAngle[i]),
                        cached.checkpointGateStartX[i],
                        cached.checkpointGateStartY[i],
                        cached.checkpointGateEndX[i],
                        cached.checkpointGateEndY[i]));
            } else {
                builder.checkpoint(new SpawnPoint(
                        cached.checkpointX[i],
                        cached.checkpointY[i],
                        cached.checkpointAngle[i]));
            }
        }
        if (cached.routeX != null && cached.routeY != null) {
            int routeCount = Math.min(cached.routeX.length, cached.routeY.length);
            for (int i = 0; i < routeCount; i++) {
                builder.routePoint(cached.routeX[i], cached.routeY[i]);
            }
        }
        if (cached.routeMarkerX != null && cached.routeMarkerY != null) {
            int routeMarkerCount = Math.min(cached.routeMarkerX.length, cached.routeMarkerY.length);
            for (int i = 0; i < routeMarkerCount; i++) {
                float progress =
                        cached.routeMarkerProgress != null && i < cached.routeMarkerProgress.length
                                ? cached.routeMarkerProgress[i]
                                : 0f;
                builder.routeMarkerPoint(cached.routeMarkerX[i], cached.routeMarkerY[i], progress);
            }
        }
        if (cached.routeSampleX != null && cached.routeSampleY != null) {
            builder.routeMetadata(
                    new ArenaMap.RouteMetadata(
                            cached.routeSampleStep,
                            cached.routeLength,
                            cached.routeSampleX,
                            cached.routeSampleY,
                            cached.routeTangentX,
                            cached.routeTangentY,
                            cached.routeCurvature,
                            cached.routeNextCornerDistance,
                            cached.routeNextCornerDirection,
                            cached.routeNextCornerSeverity,
                            cached.routeLeftClearance,
                            cached.routeRightClearance,
                            cached.routeRoadWidth,
                            cached.routeLookupWidth,
                            cached.routeLookupHeight,
                            cached.routeLookupMinX,
                            cached.routeLookupMinY,
                            cached.routeLookupCellWidth,
                            cached.routeLookupCellHeight,
                            cached.routeLookupSampleIndex));
        }
        for (int i = 0; i < cached.recoveryX.length; i++) {
            builder.recoveryPoint(cached.recoveryX[i], cached.recoveryY[i]);
        }
        return builder.build();
    }

    private static WorldSize calculateWorldSize(String baseName, int imageWidth, int imageHeight, float mapScale) {
        float worldHeight = BASE_WORLD_HEIGHT * mapScale * worldScaleForMap(baseName);
        float worldWidth = worldHeight * imageWidth / (float) imageHeight;
        return new WorldSize(worldWidth, worldHeight);
    }

    private static float worldScaleForMap(String baseName) {
        return MAP005_ID.equals(baseName) ? MAP005_WORLD_SCALE : 1f;
    }

    private static CachedMapData readCachedMap(
            FileHandle maskFile,
            String baseName,
            String surfacePath,
            float mapScale) {
        String maskSha256 = calculateFileSha256(maskFile);
        if (maskSha256 == null) {
            return null;
        }
        Array<FileHandle> cacheFiles = readableCacheFiles(maskFile, baseName);
        for (int i = 0; i < cacheFiles.size; i++) {
            FileHandle cacheFile = cacheFiles.get(i);
            if (!cacheFile.exists()) {
                continue;
            }
            Reader reader = null;
            try {
                reader = openCachedMapReader(cacheFile);
                CachedMapData cached = createCacheJson().fromJson(CachedMapData.class, reader);
                if (isCacheValid(cached, maskFile, baseName, surfacePath, mapScale, maskSha256)) {
                    return cached;
                }
            } catch (RuntimeException ignored) {
                // Bad caches are regenerated from the authoritative mask PNG.
            } catch (IOException ignored) {
                // Bad caches are regenerated from the authoritative mask PNG.
            } finally {
                closeQuietly(reader);
            }
        }
        return null;
    }

    private static boolean isCacheValid(
            CachedMapData cached,
            FileHandle maskFile,
            String baseName,
            String surfacePath,
            float mapScale,
            String maskSha256) {
        if (cached == null
                || cached.version != CACHE_VERSION
                || !baseName.equals(cached.baseName)
                || !surfacePath.equals(cached.surfacePath)
                || Math.abs(cached.mapScale - mapScale) > 0.0001f
                || cached.maskLength != maskFile.length()
                || cached.maskSha256 == null
                || !cached.maskSha256.equals(maskSha256)
                || cached.imageWidth <= 0
                || cached.imageHeight <= 0) {
            return false;
        }

        WorldSize expectedWorldSize = calculateWorldSize(baseName, cached.imageWidth, cached.imageHeight, mapScale);
        if (Math.abs(cached.worldWidth - expectedWorldSize.width) > 0.0001f
                || Math.abs(cached.worldHeight - expectedWorldSize.height) > 0.0001f) {
            return false;
        }

        long maskLastModified = maskFile.lastModified();
        return maskLastModified <= 0L
                || cached.maskLastModified <= 0L
                || cached.maskLastModified == maskLastModified;
    }

    private static void writeCachedMap(FileHandle maskFile, CachedMapData cached) {
        Array<FileHandle> cacheFiles = writableCacheFiles(maskFile, cached.baseName);
        for (int i = 0; i < cacheFiles.size; i++) {
            FileHandle cacheFile = cacheFiles.get(i);
            Writer writer = null;
            try {
                cacheFile.parent().mkdirs();
                writer = new OutputStreamWriter(
                        new GZIPOutputStream(cacheFile.write(false)),
                        StandardCharsets.UTF_8);
                createCacheJson().toJson(cached, CachedMapData.class, writer);
                return;
            } catch (RuntimeException ignored) {
                // Try the next writable cache location.
            } catch (IOException ignored) {
                // Try the next writable cache location.
            } finally {
                closeQuietly(writer);
            }
        }
    }

    private static void removeCheckpointOrderAnnotations(Pixmap mask) {
        boolean[] annotationPixels = new boolean[mask.getWidth() * mask.getHeight()];
        for (int y = 0; y < mask.getHeight(); y++) {
            for (int x = 0; x < mask.getWidth(); x++) {
                int index = y * mask.getWidth() + x;
                int pixel = mask.getPixel(x, y);
                if (isCheckpointOrderMarker(pixel) || isCheckpointLabelPixel(pixel)) {
                    annotationPixels[index] = true;
                    mask.drawPixel(x, y, rgba8888(255, 255, 255));
                } else if (!isCanonicalMaskPixel(pixel)) {
                    mask.drawPixel(x, y, isBrightPixel(pixel) ? rgba8888(255, 255, 255) : rgba8888(0, 0, 0));
                }
            }
        }
        repairGreenMarkerGaps(mask, annotationPixels);
    }

    private static void repairGreenMarkerGaps(Pixmap mask, boolean[] candidates) {
        boolean[] repair = new boolean[mask.getWidth() * mask.getHeight()];
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int y = 1; y < mask.getHeight() - 1; y++) {
            for (int x = 1; x < mask.getWidth() - 1; x++) {
                int index = y * mask.getWidth() + x;
                if (!candidates[index]) {
                    continue;
                }
                int pixel = mask.getPixel(x, y);
                if (!isWhitePixel(pixel)) {
                    continue;
                }
                for (int d = 0; d < directions.length && !repair[index]; d++) {
                    int dx = directions[d][0];
                    int dy = directions[d][1];
                    if (hasGreenOnBothSides(mask, x, y, dx, dy, 12)) {
                        repair[index] = true;
                        break;
                    }
                }
            }
        }
        int green = rgba8888(35, 210, 85);
        for (int i = 0; i < repair.length; i++) {
            if (repair[i]) {
                mask.drawPixel(i % mask.getWidth(), i / mask.getWidth(), green);
            }
        }
    }

    private static Array<CheckpointOrderMarker> collectCheckpointOrderMarkers(Pixmap mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        boolean[] visited = new boolean[width * height];
        int[] stack = new int[width * height];
        Array<CheckpointOrderMarker> markers = new Array<CheckpointOrderMarker>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int startIndex = y * width + x;
                if (visited[startIndex] || !isCheckpointOrderMarker(mask.getPixel(x, y))) {
                    continue;
                }

                int stackSize = 0;
                int area = 0;
                int orderSum = 0;
                float sumX = 0f;
                float sumY = 0f;
                stack[stackSize++] = startIndex;
                visited[startIndex] = true;
                while (stackSize > 0) {
                    int index = stack[--stackSize];
                    int currentX = index % width;
                    int currentY = index / width;
                    int pixel = mask.getPixel(currentX, currentY);
                    area++;
                    sumX += currentX;
                    sumY += currentY;
                    orderSum += checkpointOrderValue(pixel);

                    for (int offsetY = -1; offsetY <= 1; offsetY++) {
                        int nextY = currentY + offsetY;
                        if (nextY < 0 || nextY >= height) {
                            continue;
                        }
                        for (int offsetX = -1; offsetX <= 1; offsetX++) {
                            if (offsetX == 0 && offsetY == 0) {
                                continue;
                            }
                            int nextX = currentX + offsetX;
                            if (nextX < 0 || nextX >= width) {
                                continue;
                            }
                            int nextIndex = nextY * width + nextX;
                            if (visited[nextIndex] || !isCheckpointOrderMarker(mask.getPixel(nextX, nextY))) {
                                continue;
                            }
                            visited[nextIndex] = true;
                            stack[stackSize++] = nextIndex;
                        }
                    }
                }

                if (area >= CHECKPOINT_ORDER_MARKER_MIN_PIXELS) {
                    markers.add(new CheckpointOrderMarker(
                            MathUtils.clamp(Math.round(orderSum / (float) area), 1, 250),
                            sumX / area,
                            sumY / area));
                }
            }
        }

        return markers;
    }

    private static boolean hasGreenOnBothSides(
            Pixmap mask,
            int x,
            int y,
            int dx,
            int dy,
            int maximumDistance) {
        boolean negative = false;
        boolean positive = false;
        for (int distance = 1; distance <= maximumDistance; distance++) {
            int negativeX = x - dx * distance;
            int negativeY = y - dy * distance;
            if (negativeX >= 0
                    && negativeY >= 0
                    && negativeX < mask.getWidth()
                    && negativeY < mask.getHeight()
                    && isGreenMarker(mask.getPixel(negativeX, negativeY))) {
                negative = true;
            }

            int positiveX = x + dx * distance;
            int positiveY = y + dy * distance;
            if (positiveX >= 0
                    && positiveY >= 0
                    && positiveX < mask.getWidth()
                    && positiveY < mask.getHeight()
                    && isGreenMarker(mask.getPixel(positiveX, positiveY))) {
                positive = true;
            }
            if (negative && positive) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCanonicalMaskPixel(int pixel) {
        return isBlackPixel(pixel)
                || isWhitePixel(pixel)
                || isRedMarker(pixel)
                || isBlueMarker(pixel)
                || isGreenMarker(pixel)
                || isRouteLineMarker(pixel)
                || isRouteHintMarker(pixel)
                || isCheckpointOrderMarker(pixel)
                || isCheckpointLabelPixel(pixel);
    }

    private static boolean isBlackPixel(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        int alpha = pixel & 0xff;
        return alpha >= 96 && red <= 32 && green <= 32 && blue <= 32;
    }

    private static boolean isWhitePixel(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        int alpha = pixel & 0xff;
        return alpha >= 96 && red >= 220 && green >= 220 && blue >= 220;
    }

    private static boolean isBrightPixel(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        return red + green + blue >= 240;
    }

    private static void writeCheckpointOrderAnnotations(
            FileHandle maskFile,
            Pixmap mask,
            Array<SpawnPoint> checkpoints,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight,
            boolean preserveExistingOrderAnnotations) {
        if (!canWriteMaskAnnotations(maskFile)) {
            return;
        }

        if (preserveExistingOrderAnnotations) {
            return;
        }

        removeCheckpointOrderAnnotations(mask);
        if (checkpoints.size <= 1) {
            writeMaskPng(maskFile, mask);
            return;
        }
        for (int i = 0; i < checkpoints.size; i++) {
            SpawnPoint checkpoint = checkpoints.get(i);
            int centerX = worldToImageX(checkpoint.x, mask.getWidth(), worldMinX, worldWidth);
            int centerY = worldToImageY(checkpoint.y, mask.getHeight(), worldMinY, worldHeight);
            Vector2 tangent = checkpointAnnotationTangent(checkpoint, mask.getWidth(), mask.getHeight(),
                    worldMinX, worldMinY, worldWidth, worldHeight);
            Vector2 normal = new Vector2(-tangent.y, tangent.x);
            AnnotationAnchor anchor = findCheckpointAnnotationAnchor(mask, centerX, centerY, tangent, normal, i + 1);
            drawOrderDot(mask, anchor.dotX, anchor.dotY, i + 1);
            drawNumber(mask, Integer.toString(i + 1), anchor.labelX, anchor.labelY, CHECKPOINT_ORDER_LABEL_SCALE);
        }

        try {
            writeMaskPng(maskFile, mask);
        } catch (RuntimeException ignored) {
            // Some packaged/internal file handles are intentionally not writable.
        }
    }

    private static void writeMaskPng(FileHandle maskFile, Pixmap mask) {
        PixmapIO.PNG writer = new PixmapIO.PNG(Math.max(1024, mask.getWidth() * mask.getHeight() * 4));
        try {
            writer.setFlipY(false);
            writer.setCompression(9);
            writer.write(maskFile, mask);
        } catch (IOException ignored) {
            // Some packaged/internal file handles are intentionally not writable.
        } finally {
            writer.dispose();
        }
    }

    private static boolean canWriteMaskAnnotations(FileHandle maskFile) {
        return maskFile.type() == FileType.Local || maskFile.type() == FileType.Absolute;
    }

    private static AnnotationAnchor findCheckpointAnnotationAnchor(
            Pixmap mask,
            int centerX,
            int centerY,
            Vector2 tangent,
            Vector2 normal,
            int order) {
        int labelWidth = numberPixelWidth(Integer.toString(order), CHECKPOINT_ORDER_LABEL_SCALE);
        int labelHeight = 7 * CHECKPOINT_ORDER_LABEL_SCALE;
        int bestDotX = centerX;
        int bestDotY = centerY;
        int bestLabelX = centerX + CHECKPOINT_ORDER_LABEL_GAP;
        int bestLabelY = centerY - labelHeight / 2;
        int bestScore = Integer.MIN_VALUE;
        float[] distances = {10f, 16f, 23f, 31f, 40f, 52f};
        Vector2[] directions = {
            new Vector2(normal),
            new Vector2(-normal.x, -normal.y),
            new Vector2(tangent),
            new Vector2(-tangent.x, -tangent.y),
            new Vector2(normal.x + tangent.x, normal.y + tangent.y),
            new Vector2(normal.x - tangent.x, normal.y - tangent.y),
            new Vector2(-normal.x + tangent.x, -normal.y + tangent.y),
            new Vector2(-normal.x - tangent.x, -normal.y - tangent.y)
        };

        for (int d = 0; d < distances.length; d++) {
            for (int directionIndex = 0; directionIndex < directions.length; directionIndex++) {
                Vector2 direction = directions[directionIndex];
                if (direction.isZero(0.0001f)) {
                    continue;
                }
                direction.nor();
                int dotX = Math.round(centerX + direction.x * distances[d]);
                int dotY = Math.round(centerY + direction.y * distances[d]);
                int labelX = dotX + CHECKPOINT_ORDER_DOT_RADIUS + CHECKPOINT_ORDER_LABEL_GAP;
                int labelY = dotY - labelHeight / 2;
                int score = annotationRoadScore(mask, dotX, dotY, labelX, labelY, labelWidth, labelHeight);
                if (score > bestScore) {
                    bestScore = score;
                    bestDotX = dotX;
                    bestDotY = dotY;
                    bestLabelX = labelX;
                    bestLabelY = labelY;
                }
            }
        }
        return new AnnotationAnchor(bestDotX, bestDotY, bestLabelX, bestLabelY);
    }

    private static int annotationRoadScore(
            Pixmap mask,
            int dotX,
            int dotY,
            int labelX,
            int labelY,
            int labelWidth,
            int labelHeight) {
        int score = 0;
        for (int y = dotY - CHECKPOINT_ORDER_DOT_RADIUS; y <= dotY + CHECKPOINT_ORDER_DOT_RADIUS; y++) {
            for (int x = dotX - CHECKPOINT_ORDER_DOT_RADIUS; x <= dotX + CHECKPOINT_ORDER_DOT_RADIUS; x++) {
                if (x < 0 || y < 0 || x >= mask.getWidth() || y >= mask.getHeight()) {
                    score -= 12;
                } else if (isDrawableAnnotationPixel(mask.getPixel(x, y))) {
                    score += 2;
                } else if (isRedMarker(mask.getPixel(x, y))
                        || isBlueMarker(mask.getPixel(x, y))
                        || isGreenMarker(mask.getPixel(x, y))) {
                    score -= 20;
                } else {
                    score -= 4;
                }
            }
        }
        for (int y = labelY; y < labelY + labelHeight; y++) {
            for (int x = labelX; x < labelX + labelWidth; x++) {
                if (x < 0 || y < 0 || x >= mask.getWidth() || y >= mask.getHeight()) {
                    score -= 4;
                } else if (isDrawableAnnotationPixel(mask.getPixel(x, y))) {
                    score++;
                } else if (isRedMarker(mask.getPixel(x, y))
                        || isBlueMarker(mask.getPixel(x, y))
                        || isGreenMarker(mask.getPixel(x, y))) {
                    score -= 8;
                }
            }
        }
        return score;
    }

    private static Vector2 checkpointAnnotationTangent(
            SpawnPoint checkpoint,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        if (checkpoint.hasGate) {
            int startX = worldToImageX(checkpoint.gateStartX, imageWidth, worldMinX, worldWidth);
            int startY = worldToImageY(checkpoint.gateStartY, imageHeight, worldMinY, worldHeight);
            int endX = worldToImageX(checkpoint.gateEndX, imageWidth, worldMinX, worldWidth);
            int endY = worldToImageY(checkpoint.gateEndY, imageHeight, worldMinY, worldHeight);
            Vector2 tangent = new Vector2(endX - startX, endY - startY);
            if (!tangent.isZero(0.0001f)) {
                return tangent.nor();
            }
        }
        return new Vector2(-MathUtils.sin(checkpoint.angleRad), -MathUtils.cos(checkpoint.angleRad)).nor();
    }

    private static void drawOrderDot(Pixmap mask, int centerX, int centerY, int order) {
        int color = checkpointOrderColor(order);
        int radiusSquared = CHECKPOINT_ORDER_DOT_RADIUS * CHECKPOINT_ORDER_DOT_RADIUS;
        for (int y = centerY - CHECKPOINT_ORDER_DOT_RADIUS; y <= centerY + CHECKPOINT_ORDER_DOT_RADIUS; y++) {
            for (int x = centerX - CHECKPOINT_ORDER_DOT_RADIUS; x <= centerX + CHECKPOINT_ORDER_DOT_RADIUS; x++) {
                int dx = x - centerX;
                int dy = y - centerY;
                if (dx * dx + dy * dy <= radiusSquared) {
                    drawPixelIfInside(mask, x, y, color);
                }
            }
        }
    }

    private static int checkpointOrderColor(int order) {
        return rgba8888(255, MathUtils.clamp(order, 1, 250), 255);
    }

    private static void drawNumber(Pixmap mask, String text, int x, int y, int scale) {
        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            int digit = text.charAt(i) - '0';
            if (digit >= 0 && digit <= 9) {
                drawDigit(mask, digit, cursorX, y, scale);
                cursorX += 4 * scale;
            }
        }
    }

    private static int numberPixelWidth(String text, int scale) {
        return Math.max(1, text.length() * 4 * scale - scale);
    }

    private static void drawDigit(Pixmap mask, int digit, int x, int y, int scale) {
        String[] rows = DIGIT_BITMAPS[digit];
        for (int row = 0; row < rows.length; row++) {
            String pattern = rows[row];
            for (int column = 0; column < pattern.length(); column++) {
                if (pattern.charAt(column) != '1') {
                    continue;
                }
                for (int sy = 0; sy < scale; sy++) {
                    for (int sx = 0; sx < scale; sx++) {
                        drawPixelIfInside(
                                mask,
                                x + column * scale + sx,
                                y + row * scale + sy,
                                CHECKPOINT_ORDER_LABEL_COLOR);
                    }
                }
            }
        }
    }

    private static void drawPixelIfInside(Pixmap mask, int x, int y, int color) {
        if (x >= 0
                && y >= 0
                && x < mask.getWidth()
                && y < mask.getHeight()
                && isDrawableAnnotationPixel(mask.getPixel(x, y))) {
            mask.drawPixel(x, y, color);
        }
    }

    private static boolean isDrawableAnnotationPixel(int pixel) {
        return isWhitePixel(pixel) || isCheckpointOrderMarker(pixel) || isCheckpointLabelPixel(pixel);
    }

    private static int worldToImageX(float x, int imageWidth, float worldMinX, float worldWidth) {
        return MathUtils.clamp(Math.round((x - worldMinX) * imageWidth / worldWidth - 0.5f), 0, imageWidth - 1);
    }

    private static int worldToImageY(float y, int imageHeight, float worldMinY, float worldHeight) {
        return MathUtils.clamp(
                Math.round((worldMinY + worldHeight - y) * imageHeight / worldHeight - 0.5f),
                0,
                imageHeight - 1);
    }

    private static Json createCacheJson() {
        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setIgnoreUnknownFields(true);
        return json;
    }

    private static Reader openCachedMapReader(FileHandle cacheFile) throws IOException {
        return new InputStreamReader(new GZIPInputStream(cacheFile.read()), StandardCharsets.UTF_8);
    }

    private static String calculateFileSha256(FileHandle file) {
        InputStream input = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            input = new BufferedInputStream(file.read());
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) {
                    digest.update(buffer, 0, count);
                }
            }
            return toHex(digest.digest());
        } catch (RuntimeException ignored) {
            return null;
        } catch (IOException ignored) {
            return null;
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        } finally {
            closeQuietly(input);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] output = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            output[i * 2] = digits[value >>> 4];
            output[i * 2 + 1] = digits[value & 0x0f];
        }
        return new String(output);
    }

    private static Array<FileHandle> readableCacheFiles(FileHandle maskFile, String baseName) {
        Array<FileHandle> cacheFiles = new Array<FileHandle>();
        HashSet<String> seenPaths = new HashSet<String>();
        addCacheFile(cacheFiles, seenPaths, maskFile.sibling(baseName + CACHE_SUFFIX));
        if (isDefaultMapMask(maskFile)) {
            addCacheFile(cacheFiles, seenPaths,
                    Gdx.files.local("assets/" + MAPS_DIRECTORY + "/" + baseName + CACHE_SUFFIX));
            addCacheFile(cacheFiles, seenPaths, Gdx.files.local(MAPS_DIRECTORY + "/" + baseName + CACHE_SUFFIX));
        } else {
            addCacheFile(cacheFiles, seenPaths, localCacheBesideMask(maskFile, baseName));
        }
        return cacheFiles;
    }

    private static Array<FileHandle> writableCacheFiles(FileHandle maskFile, String baseName) {
        Array<FileHandle> cacheFiles = new Array<FileHandle>();
        HashSet<String> seenPaths = new HashSet<String>();
        addCacheFile(cacheFiles, seenPaths, maskFile.sibling(baseName + CACHE_SUFFIX));
        if (isDefaultMapMask(maskFile)) {
            addCacheFile(cacheFiles, seenPaths,
                    Gdx.files.local("assets/" + MAPS_DIRECTORY + "/" + baseName + CACHE_SUFFIX));
            addCacheFile(cacheFiles, seenPaths, Gdx.files.local(MAPS_DIRECTORY + "/" + baseName + CACHE_SUFFIX));
        } else {
            addCacheFile(cacheFiles, seenPaths, localCacheBesideMask(maskFile, baseName));
        }
        return cacheFiles;
    }

    private static FileHandle localCacheBesideMask(FileHandle maskFile, String baseName) {
        if (maskFile == null || maskFile.type() == FileType.Absolute) {
            return null;
        }
        FileHandle parent = maskFile.parent();
        if (parent == null || parent.path() == null || parent.path().length() == 0) {
            return null;
        }
        return Gdx.files.local(parent.path() + "/" + baseName + CACHE_SUFFIX);
    }

    private static boolean isDefaultMapMask(FileHandle maskFile) {
        if (maskFile == null) {
            return false;
        }
        String parentPath = normalizePath(maskFile.parent().path());
        return MAPS_DIRECTORY.equals(parentPath)
                || ("assets/" + MAPS_DIRECTORY).equals(parentPath);
    }

    private static String normalizePath(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }

    private static void addCacheFile(
            Array<FileHandle> cacheFiles,
            HashSet<String> seenPaths,
            FileHandle cacheFile) {
        if (cacheFile != null && seenPaths.add(cacheFile.type() + ":" + cacheFile.path())) {
            cacheFiles.add(cacheFile);
        }
    }

    private static void closeQuietly(InputStream input) {
        if (input == null) {
            return;
        }
        try {
            input.close();
        } catch (IOException ignored) {
        }
    }

    private static void closeQuietly(Reader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (IOException ignored) {
        }
    }

    private static void closeQuietly(Writer writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException ignored) {
        }
    }

    private static CachedMapData buildCachedMapData(
            FileHandle maskFile,
            String baseName,
            String surfacePath,
            float mapScale,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight,
            MaskArenaShape shape,
            Array<SpawnPoint> spawnPoints,
            Array<SpawnPoint> checkpoints,
            Array<Vector2> routePoints,
            Array<RouteLineWaypoint> routeMarkers,
            ArenaMap.RouteMetadata routeMetadata,
            Array<Vector2> recoveryPoints) {
        CachedMapData cached = new CachedMapData();
        cached.version = CACHE_VERSION;
        cached.baseName = baseName;
        cached.surfacePath = surfacePath;
        cached.mapScale = mapScale;
        cached.maskLastModified = maskFile.lastModified();
        cached.maskLength = maskFile.length();
        cached.maskSha256 = calculateFileSha256(maskFile);
        cached.imageWidth = imageWidth;
        cached.imageHeight = imageHeight;
        cached.worldMinX = worldMinX;
        cached.worldMinY = worldMinY;
        cached.worldWidth = worldWidth;
        cached.worldHeight = worldHeight;
        cached.shapePlayable = shape.copyPlayableMask();
        cached.shapeWidth = shape.getMaskWidth();
        cached.shapeHeight = shape.getMaskHeight();
        cached.distanceToVoidPixels = shape.copyDistanceToVoidPixels();
        cached.distanceToPlayablePixels = shape.copyDistanceToPlayablePixels();
        cached.nearestPlayableX = shape.copyNearestPlayableX();
        cached.nearestPlayableY = shape.copyNearestPlayableY();
        cached.nearestVoidX = shape.copyNearestVoidX();
        cached.nearestVoidY = shape.copyNearestVoidY();
        cached.spawnX = new float[spawnPoints.size];
        cached.spawnY = new float[spawnPoints.size];
        cached.spawnAngle = new float[spawnPoints.size];
        for (int i = 0; i < spawnPoints.size; i++) {
            SpawnPoint spawnPoint = spawnPoints.get(i);
            cached.spawnX[i] = spawnPoint.x;
            cached.spawnY[i] = spawnPoint.y;
            cached.spawnAngle[i] = spawnPoint.angleRad;
        }
        cached.checkpointX = new float[checkpoints.size];
        cached.checkpointY = new float[checkpoints.size];
        cached.checkpointAngle = new float[checkpoints.size];
        cached.checkpointHasGate = new boolean[checkpoints.size];
        cached.checkpointGateStartX = new float[checkpoints.size];
        cached.checkpointGateStartY = new float[checkpoints.size];
        cached.checkpointGateEndX = new float[checkpoints.size];
        cached.checkpointGateEndY = new float[checkpoints.size];
        for (int i = 0; i < checkpoints.size; i++) {
            SpawnPoint checkpoint = checkpoints.get(i);
            cached.checkpointX[i] = checkpoint.x;
            cached.checkpointY[i] = checkpoint.y;
            cached.checkpointAngle[i] = checkpoint.angleRad;
            cached.checkpointHasGate[i] = checkpoint.hasGate;
            cached.checkpointGateStartX[i] = checkpoint.gateStartX;
            cached.checkpointGateStartY[i] = checkpoint.gateStartY;
            cached.checkpointGateEndX[i] = checkpoint.gateEndX;
            cached.checkpointGateEndY[i] = checkpoint.gateEndY;
        }
        cached.routeX = new float[routePoints.size];
        cached.routeY = new float[routePoints.size];
        for (int i = 0; i < routePoints.size; i++) {
            Vector2 routePoint = routePoints.get(i);
            cached.routeX[i] = routePoint.x;
            cached.routeY[i] = routePoint.y;
        }
        cached.routeMarkerX = new float[routeMarkers.size];
        cached.routeMarkerY = new float[routeMarkers.size];
        cached.routeMarkerProgress = new float[routeMarkers.size];
        for (int i = 0; i < routeMarkers.size; i++) {
            RouteLineWaypoint routeMarker = routeMarkers.get(i);
            Vector2 routeMarkerPoint = routeMarker.point;
            cached.routeMarkerX[i] = routeMarkerPoint.x;
            cached.routeMarkerY[i] = routeMarkerPoint.y;
            cached.routeMarkerProgress[i] = routeMarker.progress;
        }
        if (routeMetadata != null) {
            cached.routeSampleStep = routeMetadata.sampleStep;
            cached.routeLength = routeMetadata.routeLength;
            cached.routeSampleX = copy(routeMetadata.sampleX);
            cached.routeSampleY = copy(routeMetadata.sampleY);
            cached.routeTangentX = copy(routeMetadata.tangentX);
            cached.routeTangentY = copy(routeMetadata.tangentY);
            cached.routeCurvature = copy(routeMetadata.curvature);
            cached.routeNextCornerDistance = copy(routeMetadata.nextCornerDistance);
            cached.routeNextCornerDirection = copy(routeMetadata.nextCornerDirection);
            cached.routeNextCornerSeverity = copy(routeMetadata.nextCornerSeverity);
            cached.routeLeftClearance = copy(routeMetadata.leftClearance);
            cached.routeRightClearance = copy(routeMetadata.rightClearance);
            cached.routeRoadWidth = copy(routeMetadata.roadWidth);
            cached.routeLookupWidth = routeMetadata.lookupWidth;
            cached.routeLookupHeight = routeMetadata.lookupHeight;
            cached.routeLookupMinX = routeMetadata.lookupMinX;
            cached.routeLookupMinY = routeMetadata.lookupMinY;
            cached.routeLookupCellWidth = routeMetadata.lookupCellWidth;
            cached.routeLookupCellHeight = routeMetadata.lookupCellHeight;
            cached.routeLookupSampleIndex = copy(routeMetadata.lookupSampleIndex);
        }
        cached.recoveryX = new float[recoveryPoints.size];
        cached.recoveryY = new float[recoveryPoints.size];
        for (int i = 0; i < recoveryPoints.size; i++) {
            Vector2 recoveryPoint = recoveryPoints.get(i);
            cached.recoveryX[i] = recoveryPoint.x;
            cached.recoveryY[i] = recoveryPoint.y;
        }
        return cached;
    }

    private static ArenaMap.RouteMetadata buildRouteMetadata(
            Array<Vector2> routePoints,
            MaskArenaShape shape,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        if (routePoints == null || routePoints.size < 2 || shape == null) {
            return null;
        }
        float[] cumulative = buildRouteCumulativeDistances(routePoints);
        float routeLength = cumulative.length == 0 ? 0f : cumulative[cumulative.length - 1];
        if (routeLength <= 0.001f) {
            return null;
        }

        int sampleCount = Math.max(2, MathUtils.ceil(routeLength / ROUTE_METADATA_SAMPLE_STEP_WORLD));
        float sampleStep = routeLength / sampleCount;
        float[] sampleX = new float[sampleCount];
        float[] sampleY = new float[sampleCount];
        float[] tangentX = new float[sampleCount];
        float[] tangentY = new float[sampleCount];
        float[] curvature = new float[sampleCount];
        float[] nextCornerDistance = new float[sampleCount];
        float[] nextCornerDirection = new float[sampleCount];
        float[] nextCornerSeverity = new float[sampleCount];
        float[] leftClearance = new float[sampleCount];
        float[] rightClearance = new float[sampleCount];
        float[] roadWidth = new float[sampleCount];

        Vector2 point = new Vector2();
        Vector2 tangent = new Vector2();
        Vector2 leftNormal = new Vector2();
        Vector2 beforeTangent = new Vector2();
        Vector2 afterTangent = new Vector2();
        for (int i = 0; i < sampleCount; i++) {
            float progress = i * sampleStep;
            sampleRoutePoint(routePoints, cumulative, routeLength, progress, point);
            sampleRouteTangent(routePoints, cumulative, routeLength, progress, tangent);
            sampleX[i] = point.x;
            sampleY[i] = point.y;
            tangentX[i] = tangent.x;
            tangentY[i] = tangent.y;

            leftNormal.set(-tangent.y, tangent.x);
            leftClearance[i] =
                    sampleShapeRayClearance(
                            shape,
                            point.x,
                            point.y,
                            leftNormal.x,
                            leftNormal.y,
                            ROUTE_METADATA_CLEARANCE_DISTANCE);
            rightClearance[i] =
                    sampleShapeRayClearance(
                            shape,
                            point.x,
                            point.y,
                            -leftNormal.x,
                            -leftNormal.y,
                            ROUTE_METADATA_CLEARANCE_DISTANCE);
            roadWidth[i] = leftClearance[i] + rightClearance[i];

            sampleRouteTangent(
                    routePoints,
                    cumulative,
                    routeLength,
                    progress - ROUTE_METADATA_CURVATURE_SAMPLE_DISTANCE,
                    beforeTangent);
            sampleRouteTangent(
                    routePoints,
                    cumulative,
                    routeLength,
                    progress + ROUTE_METADATA_CURVATURE_SAMPLE_DISTANCE,
                    afterTangent);
            float cross = beforeTangent.x * afterTangent.y - beforeTangent.y * afterTangent.x;
            float dot = beforeTangent.x * afterTangent.x + beforeTangent.y * afterTangent.y;
            float signedAngle = (float) Math.atan2(cross, dot);
            curvature[i] = MathUtils.clamp(signedAngle / 1.35f, -1f, 1f);
        }

        for (int i = 0; i < sampleCount; i++) {
            float bestSeverity = 0f;
            float bestDirection = 0f;
            float distance = routeLength;
            for (int offset = 0; offset < sampleCount; offset++) {
                int index = (i + offset) % sampleCount;
                float severity = Math.abs(curvature[index]);
                if (severity >= ROUTE_METADATA_CORNER_THRESHOLD) {
                    bestSeverity = severity;
                    bestDirection = Math.signum(curvature[index]);
                    distance = offset * sampleStep;
                    break;
                }
            }
            nextCornerDistance[i] =
                    MathUtils.clamp(distance / ROUTE_METADATA_CORNER_DISTANCE_NORMALIZER, 0f, 1f);
            nextCornerDirection[i] = bestDirection;
            nextCornerSeverity[i] = MathUtils.clamp(bestSeverity, 0f, 1f);
        }

        int lookupWidth =
                MathUtils.clamp(
                        MathUtils.ceil(worldWidth * ROUTE_METADATA_LOOKUP_CELLS_PER_WORLD_UNIT),
                        ROUTE_METADATA_LOOKUP_MIN_SIZE,
                        ROUTE_METADATA_LOOKUP_MAX_SIZE);
        int lookupHeight =
                MathUtils.clamp(
                        MathUtils.ceil(worldHeight * ROUTE_METADATA_LOOKUP_CELLS_PER_WORLD_UNIT),
                        ROUTE_METADATA_LOOKUP_MIN_SIZE,
                        ROUTE_METADATA_LOOKUP_MAX_SIZE);
        float lookupCellWidth = worldWidth / lookupWidth;
        float lookupCellHeight = worldHeight / lookupHeight;
        int[] lookupSampleIndex = new int[lookupWidth * lookupHeight];
        for (int y = 0; y < lookupHeight; y++) {
            float cellY = worldMinY + (y + 0.5f) * lookupCellHeight;
            for (int x = 0; x < lookupWidth; x++) {
                float cellX = worldMinX + (x + 0.5f) * lookupCellWidth;
                lookupSampleIndex[y * lookupWidth + x] =
                        findNearestRouteSampleIndex(cellX, cellY, sampleX, sampleY);
            }
        }

        return new ArenaMap.RouteMetadata(
                sampleStep,
                routeLength,
                sampleX,
                sampleY,
                tangentX,
                tangentY,
                curvature,
                nextCornerDistance,
                nextCornerDirection,
                nextCornerSeverity,
                leftClearance,
                rightClearance,
                roadWidth,
                lookupWidth,
                lookupHeight,
                worldMinX,
                worldMinY,
                lookupCellWidth,
                lookupCellHeight,
                lookupSampleIndex);
    }

    private static float[] buildRouteCumulativeDistances(Array<Vector2> routePoints) {
        if (routePoints == null || routePoints.size < 2) {
            return new float[0];
        }
        float[] cumulative = new float[routePoints.size + 1];
        for (int i = 0; i < routePoints.size; i++) {
            Vector2 current = routePoints.get(i);
            Vector2 next = routePoints.get((i + 1) % routePoints.size);
            cumulative[i + 1] = cumulative[i] + current.dst(next);
        }
        return cumulative;
    }

    private static void sampleRoutePoint(
            Array<Vector2> routePoints,
            float[] cumulative,
            float routeLength,
            float progress,
            Vector2 out) {
        if (routePoints == null || routePoints.size == 0 || routeLength <= 0.001f) {
            out.setZero();
            return;
        }
        float wrapped = wrapRouteProgress(progress, routeLength);
        int segmentIndex = findRouteSegmentIndex(cumulative, routeLength, wrapped);
        Vector2 start = routePoints.get(segmentIndex);
        Vector2 end = routePoints.get((segmentIndex + 1) % routePoints.size);
        float segmentLength = Math.max(0.0001f, cumulative[segmentIndex + 1] - cumulative[segmentIndex]);
        float alpha = MathUtils.clamp((wrapped - cumulative[segmentIndex]) / segmentLength, 0f, 1f);
        out.set(start).lerp(end, alpha);
    }

    private static void sampleRouteTangent(
            Array<Vector2> routePoints,
            float[] cumulative,
            float routeLength,
            float progress,
            Vector2 out) {
        Vector2 before = new Vector2();
        Vector2 after = new Vector2();
        float distance = Math.min(ROUTE_METADATA_TANGENT_SAMPLE_DISTANCE, routeLength * 0.10f);
        sampleRoutePoint(routePoints, cumulative, routeLength, progress - distance, before);
        sampleRoutePoint(routePoints, cumulative, routeLength, progress + distance, after);
        out.set(after).sub(before);
        if (out.isZero(0.0001f)) {
            int segmentIndex = findRouteSegmentIndex(cumulative, routeLength, wrapRouteProgress(progress, routeLength));
            Vector2 start = routePoints.get(segmentIndex);
            Vector2 end = routePoints.get((segmentIndex + 1) % routePoints.size);
            out.set(end).sub(start);
        }
        if (out.isZero(0.0001f)) {
            out.set(0f, 1f);
        } else {
            out.nor();
        }
    }

    private static int findRouteSegmentIndex(float[] cumulative, float routeLength, float progress) {
        if (cumulative == null || cumulative.length <= 2) {
            return 0;
        }
        int low = 0;
        int high = cumulative.length - 2;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            float start = cumulative[mid];
            float end = mid + 1 < cumulative.length ? cumulative[mid + 1] : routeLength;
            if (progress < start) {
                high = mid - 1;
            } else if (progress >= end && mid + 1 < cumulative.length - 1) {
                low = mid + 1;
            } else {
                return MathUtils.clamp(mid, 0, cumulative.length - 2);
            }
        }
        return MathUtils.clamp(low, 0, cumulative.length - 2);
    }

    private static float wrapRouteProgress(float progress, float routeLength) {
        if (routeLength <= 0.001f) {
            return 0f;
        }
        float wrapped = progress % routeLength;
        return wrapped < 0f ? wrapped + routeLength : wrapped;
    }

    private static float sampleShapeRayClearance(
            MaskArenaShape shape,
            float startX,
            float startY,
            float directionX,
            float directionY,
            float maxDistance) {
        float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        if (shape == null || length <= 0.0001f || maxDistance <= 0f) {
            return 0f;
        }
        float unitX = directionX / length;
        float unitY = directionY / length;
        for (float distance = 0f;
                distance <= maxDistance;
                distance += ROUTE_METADATA_CLEARANCE_STEP) {
            float x = startX + unitX * distance;
            float y = startY + unitY * distance;
            if (!shape.contains(x, y)
                    || shape.depthInside(x, y) < ROUTE_METADATA_CLEARANCE_EDGE_MARGIN) {
                return MathUtils.clamp(distance, 0f, maxDistance);
            }
        }
        return maxDistance;
    }

    private static int findNearestRouteSampleIndex(
            float x,
            float y,
            float[] sampleX,
            float[] sampleY) {
        int bestIndex = 0;
        float bestDistance = Float.MAX_VALUE;
        int count = Math.min(sampleX.length, sampleY.length);
        for (int i = 0; i < count; i++) {
            float dx = sampleX[i] - x;
            float dy = sampleY[i] - y;
            float distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static float[] copy(float[] values) {
        return values == null ? null : Arrays.copyOf(values, values.length);
    }

    private static int[] copy(int[] values) {
        return values == null ? null : Arrays.copyOf(values, values.length);
    }

    private static MaskParseResult parseMask(Pixmap mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        ShapeMaskDimensions shapeDimensions = calculateShapeMaskDimensions(width, height);
        boolean[] shapePlayable = new boolean[shapeDimensions.width * shapeDimensions.height];
        boolean[] redMarkers = new boolean[width * height];
        boolean[] blueMarkers = new boolean[width * height];
        boolean[] greenMarkers = new boolean[width * height];
        boolean[] routeHintMarkers = new boolean[width * height];
        boolean[] routeLineMarkers = new boolean[width * height];
        boolean[] basePlayable = new boolean[width * height];
        boolean[] pathPlayable = new boolean[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = mask.getPixel(x, y);
                boolean routeLine = isRouteLineMarker(pixel);
                boolean routeHint = !routeLine && isRouteHintMarker(pixel);
                boolean red = !routeLine && isRedMarker(pixel);
                boolean blue = !routeLine && !routeHint && isBlueMarker(pixel);
                boolean green = !routeLine && isGreenMarker(pixel);
                int index = y * width + x;
                redMarkers[index] = red;
                blueMarkers[index] = blue;
                greenMarkers[index] = green;
                routeHintMarkers[index] = routeHint;
                routeLineMarkers[index] = routeLine;
                basePlayable[index] = isPlayable(pixel);
            }
        }

        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int index = rowOffset + x;
                pathPlayable[index] =
                        basePlayable[index]
                                || ((redMarkers[index]
                                                || blueMarkers[index]
                                                || greenMarkers[index]
                                                || routeLineMarkers[index])
                                        && hasNearbyBasePlayablePixel(basePlayable, x, y, width, height));
            }
        }

        for (int y = 0; y < shapeDimensions.height; y++) {
            int sourceY = sampleSourceCoordinate(y, shapeDimensions.height, height);
            for (int x = 0; x < shapeDimensions.width; x++) {
                int sourceX = sampleSourceCoordinate(x, shapeDimensions.width, width);
                shapePlayable[y * shapeDimensions.width + x] =
                        isPlayableForShape(mask, basePlayable, sourceX, sourceY, width, height);
            }
        }

        MaskParseResult result =
                new MaskParseResult(
                        shapePlayable,
                        shapeDimensions.width,
                        shapeDimensions.height,
                        pathPlayable,
                        routeLineMarkers,
                        width,
                        height);
        collectMarkerComponents(redMarkers, width, height, result.redMarkers, MIN_SPAWN_HINT_PIXELS);
        collectMarkerComponents(blueMarkers, width, height, result.blueMarkers, MIN_MARKER_PIXELS);
        collectMarkerComponents(greenMarkers, width, height, result.greenMarkers, MIN_MARKER_PIXELS);
        collectMarkerComponents(routeHintMarkers, width, height, result.routeHintMarkers, MIN_MARKER_PIXELS);
        collectMarkerComponents(
                routeLineMarkers,
                width,
                height,
                result.routeLineMarkers,
                ROUTE_LINE_MARKER_MIN_PIXELS);
        moveBlueRouteHintMarkers(result);
        return result;
    }

    private static void moveBlueRouteHintMarkers(MaskParseResult result) {
        for (int i = result.blueMarkers.size - 1; i >= 0; i--) {
            MarkerComponent marker = result.blueMarkers.get(i);
            if (!isBlueRouteHintMarker(marker)) {
                continue;
            }
            result.blueMarkers.removeIndex(i);
            result.routeHintMarkers.add(marker);
        }
    }

    private static boolean isBlueRouteHintMarker(MarkerComponent marker) {
        int width = marker.maxX - marker.minX + 1;
        int height = marker.maxY - marker.minY + 1;
        return marker.area >= BLUE_ROUTE_HINT_MIN_AREA
                || Math.max(width, height) >= BLUE_ROUTE_HINT_MIN_DIAMETER_PIXELS;
    }

    private static ShapeMaskDimensions calculateShapeMaskDimensions(int sourceWidth, int sourceHeight) {
        int sourceLongSide = Math.max(sourceWidth, sourceHeight);
        if (sourceLongSide <= MAX_SHAPE_MASK_LONG_SIDE) {
            return new ShapeMaskDimensions(sourceWidth, sourceHeight);
        }

        float scale = MAX_SHAPE_MASK_LONG_SIDE / (float) sourceLongSide;
        return new ShapeMaskDimensions(
                Math.max(1, Math.round(sourceWidth * scale)),
                Math.max(1, Math.round(sourceHeight * scale)));
    }

    private static int sampleSourceCoordinate(int targetCoordinate, int targetSize, int sourceSize) {
        return MathUtils.clamp(
                Math.round((targetCoordinate + 0.5f) * sourceSize / targetSize - 0.5f),
                0,
                sourceSize - 1);
    }

    private static Array<SpawnPoint> buildSpawnPoints(
            MaskParseResult parseResult,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight,
            MaskArenaShape shape) {
        Array<SpawnPoint> spawns = new Array<SpawnPoint>();
        boolean[] blueUsed = new boolean[parseResult.blueMarkers.size];
        Array<MarkerComponent> orderedRedMarkers = orderSpawnMarkers(parseResult);
        if (shouldBuildExpandedSpawnGrid(parseResult, orderedRedMarkers)) {
            return buildExpandedSpawnGrid(
                    parseResult,
                    orderedRedMarkers,
                    imageWidth,
                    imageHeight,
                    worldMinX,
                    worldMinY,
                    worldWidth,
                    worldHeight,
                    shape);
        }
        MarkerDirectionBasis basis = calculateMarkerDirectionBasis(parseResult);

        for (int i = 0; i < orderedRedMarkers.size; i++) {
            MarkerComponent red = orderedRedMarkers.get(i);
            int blueIndex = findSpawnDirectionMarker(red, parseResult.blueMarkers, blueUsed, basis);
            Vector2 redWorld = imageToWorld(red.centerX, red.centerY, imageWidth, imageHeight,
                    worldMinX, worldMinY, worldWidth, worldHeight);
            if (parseResult.greenMarkers.size > 0) {
                Vector2 directionTarget = findNearestGreenTargetPoint(parseResult, red.centerX, red.centerY);
                Vector2 targetWorld = imageToWorld(
                        directionTarget.x,
                        directionTarget.y,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
                spawns.add(SpawnPoint.facingPoint(redWorld.x, redWorld.y, targetWorld.x, targetWorld.y));
            } else if (blueIndex >= 0) {
                MarkerComponent blue = parseResult.blueMarkers.get(blueIndex);
                blueUsed[blueIndex] = true;
                Vector2 blueWorld = imageToWorld(blue.centerX, blue.centerY, imageWidth, imageHeight,
                        worldMinX, worldMinY, worldWidth, worldHeight);
                spawns.add(SpawnPoint.facingPoint(redWorld.x, redWorld.y, blueWorld.x, blueWorld.y));
            } else {
                spawns.add(SpawnPoint.facingPoint(redWorld.x, redWorld.y, 0f, 0f));
            }
        }

        if (spawns.size == 0) {
            throw new IllegalStateException("Picture map mask requires red spawn markers.");
        }
        return spawns;
    }

    private static boolean shouldBuildExpandedSpawnGrid(
            MaskParseResult parseResult,
            Array<MarkerComponent> orderedRedMarkers) {
        return orderedRedMarkers.size == EXPANDED_SPAWN_GRID_COLUMNS
                && parseResult.blueMarkers.size == 0;
    }

    private static Array<SpawnPoint> buildExpandedSpawnGrid(
            MaskParseResult parseResult,
            Array<MarkerComponent> redMarkers,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight,
            MaskArenaShape shape) {
        if (parseResult.greenMarkers.size == 0) {
            throw new IllegalStateException(
                    "Two-dot spawn grid masks require a green goal marker to infer the start direction.");
        }

        MarkerComponent left = redMarkers.get(0);
        MarkerComponent right = redMarkers.get(1);
        float centerX = (left.centerX + right.centerX) * 0.5f;
        float centerY = (left.centerY + right.centerY) * 0.5f;
        Vector2 directionTarget = findNearestGreenTargetPoint(parseResult, centerX, centerY);
        float forwardX = directionTarget.x - centerX;
        float forwardY = directionTarget.y - centerY;
        float forwardLength = (float) Math.sqrt(forwardX * forwardX + forwardY * forwardY);
        if (forwardLength <= 0.0001f) {
            throw new IllegalStateException(
                    "Could not infer two-dot spawn grid direction from the green goal marker.");
        }
        forwardX /= forwardLength;
        forwardY /= forwardLength;

        float columnX = right.centerX - left.centerX;
        float columnY = right.centerY - left.centerY;
        float projectedForward = columnX * forwardX + columnY * forwardY;
        columnX -= forwardX * projectedForward;
        columnY -= forwardY * projectedForward;
        float columnLength = (float) Math.sqrt(columnX * columnX + columnY * columnY);
        if (columnLength <= 0.0001f) {
            columnX = -forwardY;
            columnY = forwardX;
            columnLength = distanceBetween(left, right);
        } else {
            columnX /= columnLength;
            columnY /= columnLength;
        }

        float markerGap = Math.max(distanceBetween(left, right), columnLength);
        float halfColumnGap = Math.max(1f, markerGap * 0.5f);
        float pixelsPerWorldUnit = imageHeight / Math.max(0.0001f, worldHeight);
        float rowSpacing =
                Math.max(
                        EXPANDED_SPAWN_MIN_ROW_SPACING_PIXELS,
                        EXPANDED_SPAWN_ROW_SPACING_WORLD * pixelsPerWorldUnit);
        float snapRadius = Math.max(rowSpacing * 1.5f, markerGap * 0.75f);
        int rows = MathUtils.ceil(EXPANDED_SPAWN_GRID_COUNT / (float) EXPANDED_SPAWN_GRID_COLUMNS);
        Array<SpawnPoint> spawns = new Array<SpawnPoint>(rows * EXPANDED_SPAWN_GRID_COLUMNS);
        for (int row = 0; row < rows && spawns.size < EXPANDED_SPAWN_GRID_COUNT; row++) {
            float rowCenterX = centerX - forwardX * rowSpacing * row;
            float rowCenterY = centerY - forwardY * rowSpacing * row;
            for (int column = 0; column < EXPANDED_SPAWN_GRID_COLUMNS
                    && spawns.size < EXPANDED_SPAWN_GRID_COUNT; column++) {
                float sideSign = column == 0 ? -1f : 1f;
                float spawnX = rowCenterX + columnX * halfColumnGap * sideSign;
                float spawnY = rowCenterY + columnY * halfColumnGap * sideSign;
                Vector2 snappedSpawn = snapSpawnToPlayable(parseResult, spawnX, spawnY, snapRadius);
                spawnX = snappedSpawn.x;
                spawnY = snappedSpawn.y;
                Vector2 spawnWorld = imageToWorld(
                        spawnX,
                        spawnY,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
                Vector2 targetWorld = imageToWorld(
                        spawnX + forwardX * rowSpacing,
                        spawnY + forwardY * rowSpacing,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
                if (shape != null && shape.depthInside(spawnWorld.x, spawnWorld.y) <= 0f) {
                    shape.closestPointInside(spawnWorld.x, spawnWorld.y, 0.05f, spawnWorld);
                }
                spawns.add(SpawnPoint.facingPoint(spawnWorld.x, spawnWorld.y, targetWorld.x, targetWorld.y));
            }
        }
        return spawns;
    }

    private static Vector2 snapSpawnToPlayable(
            MaskParseResult parseResult,
            float x,
            float y,
            float maximumDistance) {
        int roundedX = MathUtils.round(x);
        int roundedY = MathUtils.round(y);
        if (hasNearbyPlayablePixel(
                parseResult.pathPlayable,
                roundedX,
                roundedY,
                parseResult.imageWidth,
                parseResult.imageHeight,
                2)) {
            return new Vector2(x, y);
        }

        int radius = Math.max(1, MathUtils.ceil(maximumDistance));
        int minX = Math.max(0, roundedX - radius);
        int maxX = Math.min(parseResult.imageWidth - 1, roundedX + radius);
        int minY = Math.max(0, roundedY - radius);
        int maxY = Math.min(parseResult.imageHeight - 1, roundedY + radius);
        int bestX = roundedX;
        int bestY = roundedY;
        float bestDistance = Float.MAX_VALUE;
        for (int sampleY = minY; sampleY <= maxY; sampleY++) {
            int rowOffset = sampleY * parseResult.imageWidth;
            for (int sampleX = minX; sampleX <= maxX; sampleX++) {
                if (!parseResult.pathPlayable[rowOffset + sampleX]) {
                    continue;
                }
                float distance = Vector2.dst2(x, y, sampleX, sampleY);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = sampleX;
                    bestY = sampleY;
                }
            }
        }

        if (bestDistance == Float.MAX_VALUE) {
            return new Vector2(x, y);
        }
        return new Vector2(bestX, bestY);
    }

    private static Vector2 findNearestGreenTargetPoint(
            MaskParseResult parseResult,
            float sourceX,
            float sourceY) {
        Vector2 best = new Vector2();
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < parseResult.greenMarkers.size; i++) {
            MarkerComponent marker = parseResult.greenMarkers.get(i);
            Vector2 candidate = closestPointOnMarker(marker, sourceX, sourceY);
            float distance = Vector2.dst2(sourceX, sourceY, candidate.x, candidate.y);
            if (distance < bestDistance) {
                bestDistance = distance;
                best.set(candidate);
            }
        }
        return best;
    }

    private static Vector2 closestPointOnMarker(
            MarkerComponent marker,
            float sourceX,
            float sourceY) {
        if (marker != null && marker.hasUsableGate()) {
            float dx = marker.gateEndX - marker.gateStartX;
            float dy = marker.gateEndY - marker.gateStartY;
            float lengthSquared = dx * dx + dy * dy;
            if (lengthSquared > 0.0001f) {
                float t =
                        ((sourceX - marker.gateStartX) * dx + (sourceY - marker.gateStartY) * dy)
                                / lengthSquared;
                t = MathUtils.clamp(t, 0f, 1f);
                return new Vector2(marker.gateStartX + dx * t, marker.gateStartY + dy * t);
            }
        }
        return new Vector2(marker.centerX, marker.centerY);
    }

    private static float distanceBetween(MarkerComponent first, MarkerComponent second) {
        float dx = second.centerX - first.centerX;
        float dy = second.centerY - first.centerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static Array<SpawnPoint> buildCheckpoints(
            MaskParseResult parseResult,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight,
            MaskArenaShape shape) {
        Array<MarkerComponent> orderedMarkers = orderCheckpointMarkers(parseResult);
        if (orderedMarkers.size == 0) {
            return new Array<SpawnPoint>();
        }

        Array<CheckpointDefinition> definitions = new Array<CheckpointDefinition>();
        for (int i = 0; i < orderedMarkers.size; i++) {
            MarkerComponent marker = orderedMarkers.get(i);
            Vector2 markerPosition = imageToWorld(
                    marker.centerX,
                    marker.centerY,
                    imageWidth,
                    imageHeight,
                    worldMinX,
                    worldMinY,
                    worldWidth,
                    worldHeight);
            if (marker.hasUsableGate()) {
                Vector2 gateStart = imageToWorld(
                        marker.gateStartX,
                        marker.gateStartY,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
                Vector2 gateEnd = imageToWorld(
                        marker.gateEndX,
                        marker.gateEndY,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
                definitions.add(new CheckpointDefinition(markerPosition, gateStart, gateEnd, true));
            } else {
                definitions.add(new CheckpointDefinition(markerPosition, null, null, false));
            }
        }

        Array<SpawnPoint> checkpoints = new Array<SpawnPoint>();
        for (int i = 0; i < definitions.size; i++) {
            CheckpointDefinition definition = definitions.get(i);
            Vector2 current = definition.position;
            Vector2 next = definitions.get((i + 1) % definitions.size).position;
            if (definition.hasGate) {
                checkpoints.add(SpawnPoint.facingGate(
                        current.x,
                        current.y,
                        next.x,
                        next.y,
                        definition.gateStart.x,
                        definition.gateStart.y,
                        definition.gateEnd.x,
                        definition.gateEnd.y));
            } else {
                checkpoints.add(SpawnPoint.facingPoint(current.x, current.y, next.x, next.y));
            }
        }
        return checkpoints;
    }

    private static Array<RouteHintCircle> buildRouteHintCircles(
            MaskParseResult parseResult,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        Array<RouteHintCircle> hints = new Array<RouteHintCircle>();
        Array<MarkerComponent> markers = mergeRouteHintMarkers(parseResult.routeHintMarkers);
        for (int i = 0; i < markers.size; i++) {
            MarkerComponent marker = markers.get(i);
            Vector2 center =
                    imageToWorld(
                            marker.centerX,
                            marker.centerY,
                            imageWidth,
                            imageHeight,
                            worldMinX,
                            worldMinY,
                            worldWidth,
                            worldHeight);
            Vector2 gateStart =
                    imageToWorld(
                            marker.gateStartX,
                            marker.gateStartY,
                            imageWidth,
                            imageHeight,
                            worldMinX,
                            worldMinY,
                            worldWidth,
                            worldHeight);
            Vector2 gateEnd =
                    imageToWorld(
                            marker.gateEndX,
                            marker.gateEndY,
                            imageWidth,
                            imageHeight,
                            worldMinX,
                            worldMinY,
                            worldWidth,
                            worldHeight);
            float radius = Math.max(center.dst(gateStart), center.dst(gateEnd));
            if (radius > 2.0f) {
                Array<Vector2> intersections =
                        findRouteHintIntersections(
                                parseResult,
                                marker,
                                imageWidth,
                                imageHeight,
                                worldMinX,
                                worldMinY,
                                worldWidth,
                                worldHeight);
                hints.add(new RouteHintCircle(center.x, center.y, radius, intersections));
            }
        }
        return hints;
    }

    private static Array<MarkerComponent> mergeRouteHintMarkers(Array<MarkerComponent> markers) {
        Array<MarkerComponent> merged = new Array<MarkerComponent>();
        if (markers == null || markers.size == 0) {
            return merged;
        }

        boolean[] used = new boolean[markers.size];
        for (int i = 0; i < markers.size; i++) {
            if (used[i]) {
                continue;
            }

            MarkerComponent seed = markers.get(i);
            used[i] = true;
            int area = seed.area;
            int minX = seed.minX;
            int minY = seed.minY;
            int maxX = seed.maxX;
            int maxY = seed.maxY;

            boolean expanded;
            do {
                expanded = false;
                for (int j = i + 1; j < markers.size; j++) {
                    if (used[j]) {
                        continue;
                    }
                    MarkerComponent candidate = markers.get(j);
                    if (!markerBoxesNear(
                            minX,
                            minY,
                            maxX,
                            maxY,
                            candidate,
                            ROUTE_HINT_COMPONENT_MERGE_GAP_PIXELS)) {
                        continue;
                    }

                    used[j] = true;
                    expanded = true;
                    area += candidate.area;
                    minX = Math.min(minX, candidate.minX);
                    minY = Math.min(minY, candidate.minY);
                    maxX = Math.max(maxX, candidate.maxX);
                    maxY = Math.max(maxY, candidate.maxY);
                }
            } while (expanded);

            float centerX = (minX + maxX) * 0.5f;
            float centerY = (minY + maxY) * 0.5f;
            merged.add(new MarkerComponent(centerX, centerY, area, minX, minY, maxX, maxY, 0f, 0f, 0f));
        }
        return merged;
    }

    private static boolean markerBoxesNear(
            int minX,
            int minY,
            int maxX,
            int maxY,
            MarkerComponent marker,
            int gap) {
        return marker.minX <= maxX + gap
                && marker.maxX >= minX - gap
                && marker.minY <= maxY + gap
                && marker.maxY >= minY - gap;
    }

    private static Array<Vector2> findRouteHintIntersections(
            MaskParseResult parseResult,
            MarkerComponent marker,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        Array<Vector2> intersections = new Array<Vector2>();
        float radiusPixels =
                Math.max(marker.maxX - marker.minX, marker.maxY - marker.minY) * 0.5f;
        if (radiusPixels < ROUTE_HINT_MIN_RADIUS_PIXELS) {
            return intersections;
        }

        int sampleCount = MathUtils.clamp(MathUtils.round(radiusPixels * 8f), 96, 360);
        int searchRadius = MathUtils.clamp(MathUtils.round(radiusPixels * 0.10f), 3, 8);
        boolean[] hits = new boolean[sampleCount];
        float[] sampleX = new float[sampleCount];
        float[] sampleY = new float[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            float angle = MathUtils.PI2 * i / sampleCount;
            float x = marker.centerX + MathUtils.cos(angle) * radiusPixels;
            float y = marker.centerY + MathUtils.sin(angle) * radiusPixels;
            sampleX[i] = x;
            sampleY[i] = y;
            hits[i] =
                    hasNearbyPlayablePixel(
                            parseResult.pathPlayable,
                            MathUtils.round(x),
                            MathUtils.round(y),
                            imageWidth,
                            imageHeight,
                            searchRadius);
        }

        int firstGap = -1;
        for (int i = 0; i < sampleCount; i++) {
            if (!hits[i]) {
                firstGap = i;
                break;
            }
        }
        if (firstGap < 0) {
            return intersections;
        }

        int minimumGroupSamples = Math.max(2, sampleCount / 120);
        int index = firstGap + 1;
        int end = firstGap + 1 + sampleCount;
        while (index < end) {
            int sample = index % sampleCount;
            if (!hits[sample]) {
                index++;
                continue;
            }

            int count = 0;
            float sumX = 0f;
            float sumY = 0f;
            while (index < end && hits[index % sampleCount]) {
                int groupSample = index % sampleCount;
                sumX += sampleX[groupSample];
                sumY += sampleY[groupSample];
                count++;
                index++;
            }

            if (count >= minimumGroupSamples) {
                intersections.add(
                        imageToWorld(
                                sumX / count,
                                sumY / count,
                                imageWidth,
                                imageHeight,
                                worldMinX,
                                worldMinY,
                                worldWidth,
                                worldHeight));
            }
        }
        return intersections;
    }

    private static Vector2 chooseGateCheckpointCenter(
            Vector2 markerPosition,
            Vector2 gateStart,
            Vector2 gateEnd,
            MaskArenaShape shape) {
        Vector2 best = new Vector2(markerPosition);
        float bestClearance = shape == null ? 0f : shape.depthInside(best.x, best.y);
        float bestDistanceToMarker = 0f;
        for (int i = 0; i < CHECKPOINT_CENTER_GATE_SAMPLES; i++) {
            float alpha = i / (float) (CHECKPOINT_CENTER_GATE_SAMPLES - 1);
            float x = MathUtils.lerp(gateStart.x, gateEnd.x, alpha);
            float y = MathUtils.lerp(gateStart.y, gateEnd.y, alpha);
            float clearance = shape == null ? 0f : shape.depthInside(x, y);
            float distanceToMarker = Vector2.dst2(x, y, markerPosition.x, markerPosition.y);
            if (clearance > bestClearance + 0.001f
                    || (Math.abs(clearance - bestClearance) <= 0.001f
                            && distanceToMarker < bestDistanceToMarker)) {
                best.set(x, y);
                bestClearance = clearance;
                bestDistanceToMarker = distanceToMarker;
            }
        }
        return best;
    }

    private static ArenaMap buildRouteOrderingMap(
            String baseName,
            String surfacePath,
            MaskArenaShape shape,
            Array<SpawnPoint> spawnPoints,
            Array<Vector2> recoveryPoints) {
        ArenaMap.Builder builder = ArenaMap.builder(
                        baseName + "-checkpoint-order",
                        displayName(baseName))
                .focusPoint(0f, 0f)
                .surfaceImagePath(surfacePath)
                .solid(shape);
        for (int i = 0; i < spawnPoints.size; i++) {
            builder.spawn(spawnPoints.get(i));
        }
        addRecoveryPoints(builder, recoveryPoints);
        return builder.build();
    }

    private static Array<Vector2> buildRouteCenterline(
            ArenaMap routeMap,
            Array<SpawnPoint> spawnPoints,
            Array<SpawnPoint> checkpoints,
            Array<RouteHintCircle> routeHints,
            boolean[] routeLinePixels,
            Array<MarkerComponent> routeLineMarkers,
            Array<CheckpointOrderMarker> routeOrderMarkers,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        Array<Vector2> baselineRoute =
                buildRouteCenterlineIgnoringRouteLine(
                        routeMap,
                        spawnPoints,
                        checkpoints,
                        routeHints,
                        routeOrderMarkers,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
        Array<Vector2> routeLine =
                buildRouteFromRouteLineMarkers(
                        routeMap,
                        spawnPoints,
                        baselineRoute,
                        routeLinePixels,
                        routeLineMarkers,
                        routeHints,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
        if (routeLine.size >= 4) {
            if (routeLineMarkers != null
                    && routeLineMarkers.size >= ROUTE_LINE_DENSE_MARKER_ROUTE_SHAPE_LIMIT
                    && baselineRoute.size >= 4
                    && findUnsafeRouteSample(routeMap, routeLine, routeHints) != null) {
                return baselineRoute;
            }
            return routeLine;
        }
        return baselineRoute;
    }

    private static Array<Vector2> buildRouteCenterlineIgnoringRouteLine(
            ArenaMap routeMap,
            Array<SpawnPoint> spawnPoints,
            Array<SpawnPoint> checkpoints,
            Array<RouteHintCircle> routeHints,
            Array<CheckpointOrderMarker> routeOrderMarkers,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        boolean routeOnlyOrderedMap =
                routeMap.getId().startsWith("route")
                        && (checkpoints == null || checkpoints.size <= 1)
                        && routeOrderMarkers != null;
        if (routeOnlyOrderedMap) {
            Array<Vector2> orderedRoute =
                    buildRouteFromOrderMarkers(
                            routeMap,
                            spawnPoints,
                            routeOrderMarkers,
                            routeHints,
                            imageWidth,
                            imageHeight,
                            worldMinX,
                            worldMinY,
                            worldWidth,
                            worldHeight);
            if (orderedRoute.size >= 4) {
                return orderedRoute;
            }
        }

        if (checkpoints != null && checkpoints.size >= 4) {
            return buildRouteFromCheckpoints(checkpoints);
        }

        if (routeMap == null || spawnPoints.size == 0) {
            return buildRouteFromCheckpoints(checkpoints);
        }

        Vector2 forward = averageSpawnForward(spawnPoints);
        if (forward.isZero(0.0001f)) {
            return buildRouteFromCheckpoints(checkpoints);
        }

        com.badlogic.gdx.math.Rectangle bounds = calculatePlayableRouteBounds(routeMap);
        Array<Vector2> generatedRoute =
                buildGeneratedRouteCenterlineAttempt(
                        routeMap,
                        spawnPoints,
                        checkpoints,
                        routeHints,
                        bounds,
                        forward);
        if (generatedRoute.size >= 4) {
            return generatedRoute;
        }

        Vector2 reverseForward = new Vector2(forward).scl(-1f);
        generatedRoute =
                buildGeneratedRouteCenterlineAttempt(
                        routeMap,
                        spawnPoints,
                        checkpoints,
                        routeHints,
                        bounds,
                        reverseForward);
        if (generatedRoute.size >= 4) {
            return generatedRoute;
        }

        return buildRouteFromCheckpoints(checkpoints);
    }

    private static Array<Vector2> buildGeneratedRouteCenterlineAttempt(
            ArenaMap routeMap,
            Array<SpawnPoint> spawnPoints,
            Array<SpawnPoint> checkpoints,
            Array<RouteHintCircle> routeHints,
            com.badlogic.gdx.math.Rectangle bounds,
            Vector2 forward) {
        Array<Vector2> route = new Array<Vector2>();
        if (routeMap == null || spawnPoints == null || spawnPoints.size == 0 || forward == null
                || forward.isZero(0.0001f)) {
            return route;
        }

        Vector2 start = averageSpawnPosition(spawnPoints);
        routeMap.clampToPlayable(start, ROUTE_CENTERLINE_MIN_CLEARANCE);
        Vector2 side = new Vector2(-forward.y, forward.x);
        centerRoutePoint(routeMap, start, side);
        route.add(new Vector2(start));

        float minimumLoopDistance = Math.max(32f, (bounds.width + bounds.height) * 1.15f);
        float visitedDistanceSquared =
                ROUTE_CENTERLINE_STEP_WORLD
                        * ROUTE_CENTERLINE_STEP_WORLD
                        * ROUTE_CENTERLINE_VISITED_DISTANCE_FACTOR
                        * ROUTE_CENTERLINE_VISITED_DISTANCE_FACTOR;

        Vector2 current = new Vector2(start);
        Vector2 direction = new Vector2(forward);
        Vector2 candidate = new Vector2();
        float travelled = 0f;
        for (int step = 0; step < ROUTE_CENTERLINE_MAX_STEPS; step++) {
            RouteHintPass routeHintPass =
                    findGeneratedRouteHintPass(
                            routeMap,
                            routeHints,
                            current,
                            direction);
            if (routeHintPass != null) {
                addDirectRouteHintSegment(route, current, routeHintPass.entry);
                addDirectRouteHintSegment(route, routeHintPass.entry, routeHintPass.exit);
                travelled += current.dst(routeHintPass.entry) + routeHintPass.entry.dst(routeHintPass.exit);
                direction.set(routeHintPass.exit).sub(routeHintPass.entry);
                if (direction.isZero(0.0001f)) {
                    direction.set(forward);
                } else {
                    direction.nor();
                }
                current.set(routeHintPass.exit);
                continue;
            }

            RouteStep next =
                    chooseRouteCenterlineStep(
                            routeMap,
                            route,
                            current,
                            direction,
                            start,
                            travelled,
                            minimumLoopDistance,
                            bounds,
                            visitedDistanceSquared,
                            ROUTE_CENTERLINE_ANGLES_DEG,
                            candidate);
            if (next == null) {
                next =
                        chooseRouteCenterlineStep(
                                routeMap,
                                route,
                                current,
                                direction,
                                start,
                                travelled,
                                minimumLoopDistance,
                                bounds,
                                visitedDistanceSquared,
                                ROUTE_CENTERLINE_FALLBACK_ANGLES_DEG,
                                candidate);
            }
            if (next == null) {
                break;
            }
            if (next.closesLoop) {
                route.add(new Vector2(next.point));
                return finishGeneratedRouteCenterline(
                        routeMap,
                        route,
                        spawnPoints,
                        forward,
                        checkpoints,
                        bounds,
                        routeHints == null || routeHints.size == 0);
            }

            travelled += current.dst(next.point);
            direction.set(next.direction);
            current.set(next.point);
            route.add(new Vector2(current));
        }

        return finishGeneratedRouteCenterline(
                routeMap,
                route,
                spawnPoints,
                forward,
                checkpoints,
                bounds,
                routeHints == null || routeHints.size == 0);
    }

    private static Array<Vector2> buildRouteFromRouteLineMarkers(
            ArenaMap routeMap,
            Array<SpawnPoint> spawnPoints,
            Array<Vector2> baselineRoute,
            boolean[] routeLinePixels,
            Array<MarkerComponent> routeLineMarkers,
            Array<RouteHintCircle> routeHints,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        Array<Vector2> route = new Array<Vector2>();
        if (routeMap == null
                || spawnPoints == null
                || spawnPoints.size == 0
                || baselineRoute == null
                || baselineRoute.size < 4) {
            return route;
        }

        Array<Vector2> markers =
                collectRouteLineWaypoints(
                        routeMap,
                        routeLinePixels,
                        routeLineMarkers,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
        if (markers.size < 4) {
            return route;
        }

        Vector2 forward = averageSpawnForward(spawnPoints);
        Vector2 start = routeLineStartPosition(spawnPoints, forward);
        routeMap.clampToPlayable(start, ROUTE_CENTERLINE_MIN_CLEARANCE);
        if (forward.isZero(0.0001f)) {
            forward.set(baselineRoute.get(1)).sub(baselineRoute.first());
            if (forward.isZero(0.0001f)) {
                return route;
            }
            forward.nor();
        }
        Vector2 side = new Vector2(-forward.y, forward.x);
        centerRoutePoint(routeMap, start, side);

        Array<RouteLineWaypoint> orderedMarkers =
                orderRouteLineWaypointsByBaseline(
                        markers,
                        baselineRoute,
                        routeMap,
                        start,
                        forward,
                        routeHints);
        if (orderedMarkers.size < 4) {
            return route;
        }

        route = buildRouteLineRoute(start, orderedMarkers);
        if (route.size < 4) {
            return route;
        }

        route = smoothRouteLineVertices(route);
        clampRouteLineToPlayable(routeMap, route);
        if (findUnsafeRouteSample(routeMap, route) != null) {
            Array<Vector2> waypointRoute = buildRouteFromRouteLineWaypoints(routeMap, orderedMarkers, routeHints);
            if (waypointRoute.size >= 4) {
                route = waypointRoute;
                clampRouteLineToPlayable(routeMap, route);
            }
        }
        orientRouteLineAwayFromSpawnGrid(route, spawnPoints);
        return route;
    }

    private static Array<Vector2> buildRouteFromRouteLineWaypoints(
            ArenaMap routeMap,
            Array<RouteLineWaypoint> orderedMarkers,
            Array<RouteHintCircle> routeHints) {
        Array<Vector2> route = new Array<Vector2>();
        if (routeMap == null || orderedMarkers == null || orderedMarkers.size < 4) {
            return route;
        }
        float[][] waypoints = new float[orderedMarkers.size + 1][2];
        for (int i = 0; i < orderedMarkers.size; i++) {
            Vector2 point = orderedMarkers.get(i).point;
            waypoints[i][0] = point.x;
            waypoints[i][1] = point.y;
        }
        waypoints[orderedMarkers.size][0] = orderedMarkers.first().point.x;
        waypoints[orderedMarkers.size][1] = orderedMarkers.first().point.y;
        route = buildRouteFromWaypoints(routeMap, waypoints, routeHints);
        if (route.size < 4) {
            return route;
        }
        return smoothRouteCenterline(routeMap, route);
    }

    private static Array<RouteLineWaypoint> buildOrderedRouteMarkers(
            ArenaMap routeMap,
            Array<MarkerComponent> routeLineMarkers,
            Array<Vector2> routePoints,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        Array<RouteLineWaypoint> orderedMarkers = new Array<RouteLineWaypoint>();
        if (routeMap == null
                || routeLineMarkers == null
                || routeLineMarkers.size == 0
                || routePoints == null
                || routePoints.size < 2) {
            return orderedMarkers;
        }

        Array<Vector2> markers = new Array<Vector2>();
        for (int i = 0; i < routeLineMarkers.size; i++) {
            MarkerComponent marker = routeLineMarkers.get(i);
            addRouteLineWaypoint(
                    markers,
                    routeMap,
                    marker.centerX,
                    marker.centerY,
                    imageWidth,
                    imageHeight,
                    worldMinX,
                    worldMinY,
                    worldWidth,
                    worldHeight);
        }

        Array<RouteLineWaypoint> sweptMarkers =
                orderRouteLineWaypointsByRouteSweep(routeMap, markers, routePoints);
        if (sweptMarkers.size == markers.size) {
            return sweptMarkers;
        }
        return alignRouteMarkersWithRouteStart(
                orderRouteLineWaypointsByRoute(markers, routePoints),
                routePoints);
    }

    private static Array<Vector2> collectRouteLineWaypoints(
            ArenaMap routeMap,
            boolean[] routeLinePixels,
            Array<MarkerComponent> routeLineMarkers,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        Array<Vector2> markers = new Array<Vector2>();
        if (routeMap == null) {
            return markers;
        }

        if (routeLineMarkers != null && routeLineMarkers.size >= 4) {
            for (int i = 0; i < routeLineMarkers.size; i++) {
                MarkerComponent marker = routeLineMarkers.get(i);
                addRouteLineWaypoint(
                        markers,
                        routeMap,
                        marker.centerX,
                        marker.centerY,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
            }
            return markers;
        }

        if (routeLinePixels == null || routeLinePixels.length != imageWidth * imageHeight) {
            return markers;
        }

        RouteLineComponent component =
                findLargestRouteLineComponent(routeLinePixels, imageWidth, imageHeight);
        if (component.count < ROUTE_LINE_CONTINUOUS_MIN_PIXELS) {
            return markers;
        }

        boolean[] skeleton = skeletonizeRouteLine(component.pixels, imageWidth, imageHeight);
        for (int index = 0; index < skeleton.length; index++) {
            if (!skeleton[index]) {
                continue;
            }
            addRouteLineWaypoint(
                    markers,
                    routeMap,
                    index % imageWidth,
                    index / imageWidth,
                    imageWidth,
                    imageHeight,
                    worldMinX,
                    worldMinY,
                    worldWidth,
                    worldHeight);
        }
        return markers;
    }

    private static void addRouteLineWaypoint(
            Array<Vector2> markers,
            ArenaMap routeMap,
            float imageX,
            float imageY,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        Vector2 point =
                imageToWorld(
                        imageX,
                        imageY,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
        routeMap.clampToPlayable(point, ROUTE_CENTERLINE_EXACT_MIN_CLEARANCE);
        if (routeMap.approximateSupports(point)) {
            markers.add(point);
        }
    }

    private static Array<RouteLineWaypoint> orderRouteLineWaypointsByBaseline(
            Array<Vector2> markers,
            Array<Vector2> baselineRoute,
            ArenaMap routeMap,
            Vector2 start,
            Vector2 forward,
            Array<RouteHintCircle> routeHints) {
        Array<RouteLineWaypoint> ordered = new Array<RouteLineWaypoint>();
        if (markers == null || markers.size == 0 || baselineRoute == null || baselineRoute.size < 4) {
            return ordered;
        }

        float[] cumulative = buildRouteCumulativeDistances(baselineRoute);
        if (cumulative.length == 0) {
            return ordered;
        }
        float routeLength = cumulative[cumulative.length - 1];
        if (routeLength <= 0.001f) {
            return ordered;
        }

        Array<RouteLineWaypoint> sweptMarkers =
                orderRouteLineWaypointsByRouteSweep(routeMap, markers, baselineRoute);
        if (sweptMarkers.size == markers.size) {
            ordered.addAll(sweptMarkers);
        } else {
            Array<RouteLineWaypoint> candidates = new Array<RouteLineWaypoint>(markers.size);
            for (int i = 0; i < markers.size; i++) {
                Vector2 marker = markers.get(i);
                candidates.add(new RouteLineWaypoint(
                        marker,
                        routeProgressOnRoute(baselineRoute, cumulative, routeLength, marker)));
            }
            sortRouteLineWaypointsByProgress(candidates);

            float lastProgress = -Float.MAX_VALUE;
            Vector2 lastPoint = null;
            float minimumDistanceSquared =
                    ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * 0.25f;
            for (int i = 0; i < candidates.size; i++) {
                RouteLineWaypoint candidate = candidates.get(i);
                if (lastPoint != null
                        && candidate.progress - lastProgress < ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * 0.50f
                        && candidate.point.dst2(lastPoint) < minimumDistanceSquared) {
                    continue;
                }
                ordered.add(candidate);
                lastProgress = candidate.progress;
                lastPoint = candidate.point;
            }
        }
        ordered = chooseRouteLineDirectionAndStart(ordered, routeMap, start, forward);
        if (routeHints != null && routeHints.size > 0) {
            ordered = repairRouteHintBranchOrder(ordered, routeHints);
        } else {
            Array<RouteLineWaypoint> continuityOrder =
                    orderRouteLineWaypointsByContinuity(ordered, routeMap, start, forward);
            if (continuityOrder.size >= 4
                    && shouldPreferRouteLineContinuityOrder(
                            ordered,
                            continuityOrder,
                            routeMap,
                            start,
                            forward)) {
                ordered = continuityOrder;
            }
        }
        ordered = repairUnsafeFirstRouteLineConnection(ordered, routeMap, start, forward);
        return truncateRouteLineAtEarlyClosure(ordered, routeMap);
    }

    private static Array<RouteLineWaypoint> repairRouteHintBranchOrder(
            Array<RouteLineWaypoint> ordered,
            Array<RouteHintCircle> routeHints) {
        if (ordered == null || ordered.size < 6 || routeHints == null || routeHints.size == 0) {
            return ordered;
        }
        Array<RouteLineWaypoint> crossingRepaired = repairRouteHintCrossingOrder(ordered, routeHints);
        if (crossingRepaired != ordered) {
            return crossingRepaired;
        }
        RouteLineWaypoint first = ordered.first();
        RouteHintCircle nearestHint = nearestRouteHint(first.point, routeHints);
        if (nearestHint == null) {
            return ordered;
        }
        Vector2 hintDirection = new Vector2(nearestHint.centerX, nearestHint.centerY).sub(first.point);
        if (hintDirection.isZero(0.0001f)) {
            return ordered;
        }
        hintDirection.nor();

        int splitIndex = -1;
        float bestScore = Float.MAX_VALUE;
        float baselineFirstDistance = first.point.dst(ordered.get(1).point);
        for (int i = 2; i < ordered.size - 1; i++) {
            RouteLineWaypoint candidate = ordered.get(i);
            Vector2 delta = new Vector2(candidate.point).sub(first.point);
            float distance = delta.len();
            if (distance <= 0.0001f || distance > baselineFirstDistance * 1.25f) {
                continue;
            }
            delta.scl(1f / distance);
            float alignment = delta.dot(hintDirection);
            if (alignment < 0.60f) {
                continue;
            }
            float score = distance * MathUtils.lerp(1.0f, 0.45f, alignment);
            if (score < bestScore) {
                bestScore = score;
                splitIndex = i;
            }
        }
        if (splitIndex < 2) {
            return ordered;
        }

        Array<RouteLineWaypoint> repaired = new Array<RouteLineWaypoint>(ordered.size);
        repaired.add(ordered.get(0));
        for (int i = splitIndex; i >= 1; i--) {
            repaired.add(ordered.get(i));
        }
        for (int i = ordered.size - 1; i > splitIndex; i--) {
            repaired.add(ordered.get(i));
        }
        return repaired;
    }

    private static Array<RouteLineWaypoint> repairRouteHintCrossingOrder(
            Array<RouteLineWaypoint> ordered,
            Array<RouteHintCircle> routeHints) {
        for (int hintIndex = 0; hintIndex < routeHints.size; hintIndex++) {
            RouteHintCircle hint = routeHints.get(hintIndex);
            if (hint.intersections.size < 4) {
                continue;
            }
            Array<RouteHintPortalMarker> portalMarkers =
                    findRouteHintAdjacentCrossingMarkers(ordered, hint);
            if (portalMarkers.size < 4) {
                portalMarkers = findRouteHintPortalMarkers(ordered, hint);
            }
            if (portalMarkers.size < 4) {
                continue;
            }
            portalMarkers.sort(new Comparator<RouteHintPortalMarker>() {
                @Override
                public int compare(RouteHintPortalMarker left, RouteHintPortalMarker right) {
                    return left.orderIndex - right.orderIndex;
                }
            });

            RouteHintPortalMarker first = portalMarkers.get(0);
            RouteHintPortalMarker second = portalMarkers.get(1);
            RouteHintPortalMarker third = portalMarkers.get(2);
            RouteHintPortalMarker fourth = portalMarkers.get(3);
            if (!areOppositeRouteHintPortalMarkers(hint, first, third)
                    || !areOppositeRouteHintPortalMarkers(hint, second, fourth)
                    || areOppositeRouteHintPortalMarkers(hint, first, second)
                    || areOppositeRouteHintPortalMarkers(hint, third, fourth)) {
                continue;
            }

            Array<RouteLineWaypoint> repaired = new Array<RouteLineWaypoint>(ordered.size);
            for (int i = 0; i <= first.orderIndex; i++) {
                repaired.add(ordered.get(i));
            }
            for (int i = third.orderIndex; i >= second.orderIndex; i--) {
                repaired.add(ordered.get(i));
            }
            for (int i = fourth.orderIndex; i < ordered.size; i++) {
                repaired.add(ordered.get(i));
            }
            return repaired;
        }
        return ordered;
    }

    private static Array<RouteHintPortalMarker> findRouteHintAdjacentCrossingMarkers(
            Array<RouteLineWaypoint> ordered,
            RouteHintCircle hint) {
        Array<RouteHintPortalMarker> markers = new Array<RouteHintPortalMarker>();
        boolean[] used = new boolean[ordered.size];
        for (int i = 0; i < ordered.size; i++) {
            int nextIndex = (i + 1) % ordered.size;
            Vector2 start = ordered.get(i).point;
            Vector2 end = ordered.get(nextIndex).point;
            if (!hint.segmentIntersects(start, end, ROUTE_HINT_SEGMENT_MARGIN_WORLD)) {
                continue;
            }
            if (!used[i]) {
                markers.add(new RouteHintPortalMarker(i, -1, start));
                used[i] = true;
            }
            if (!used[nextIndex]) {
                markers.add(new RouteHintPortalMarker(nextIndex, -1, end));
                used[nextIndex] = true;
            }
            if (markers.size >= 4) {
                break;
            }
        }
        return markers;
    }

    private static Array<RouteHintPortalMarker> findRouteHintPortalMarkers(
            Array<RouteLineWaypoint> ordered,
            RouteHintCircle hint) {
        Array<RouteHintPortalCandidate> candidates = new Array<RouteHintPortalCandidate>();
        float maximumDistance =
                Math.max(
                        ROUTE_LINE_MARKER_SWEEP_MAX_LATERAL * 2.0f,
                        hint.radius * 2.4f);
        float maximumDistanceSquared = maximumDistance * maximumDistance;
        for (int portalIndex = 0; portalIndex < hint.intersections.size; portalIndex++) {
            Vector2 portal = hint.intersections.get(portalIndex);
            for (int orderIndex = 0; orderIndex < ordered.size; orderIndex++) {
                float distance = ordered.get(orderIndex).point.dst2(portal);
                if (distance <= maximumDistanceSquared) {
                    candidates.add(new RouteHintPortalCandidate(orderIndex, portalIndex, distance));
                }
            }
        }
        candidates.sort(new Comparator<RouteHintPortalCandidate>() {
            @Override
            public int compare(RouteHintPortalCandidate left, RouteHintPortalCandidate right) {
                return Float.compare(left.distanceSquared, right.distanceSquared);
            }
        });

        boolean[] usedPortals = new boolean[hint.intersections.size];
        boolean[] usedMarkers = new boolean[ordered.size];
        Array<RouteHintPortalMarker> markers = new Array<RouteHintPortalMarker>();
        for (int i = 0; i < candidates.size; i++) {
            RouteHintPortalCandidate candidate = candidates.get(i);
            if (usedPortals[candidate.portalIndex] || usedMarkers[candidate.orderIndex]) {
                continue;
            }
            usedPortals[candidate.portalIndex] = true;
            usedMarkers[candidate.orderIndex] = true;
            markers.add(new RouteHintPortalMarker(
                    candidate.orderIndex,
                    candidate.portalIndex,
                    hint.intersections.get(candidate.portalIndex)));
            if (markers.size >= 4) {
                break;
            }
        }
        if (markers.size < 4) {
            return findNearbyRouteHintPortalMarkersByAngle(ordered, hint);
        }
        return markers;
    }

    private static Array<RouteHintPortalMarker> findNearbyRouteHintPortalMarkersByAngle(
            Array<RouteLineWaypoint> ordered,
            RouteHintCircle hint) {
        Array<RouteHintPortalCandidate> candidates = new Array<RouteHintPortalCandidate>();
        float maximumDistance = Math.max(
                ROUTE_LINE_MARKER_SWEEP_MAX_LATERAL * 2.0f,
                hint.radius * 2.4f);
        float maximumDistanceSquared = maximumDistance * maximumDistance;
        for (int orderIndex = 0; orderIndex < ordered.size; orderIndex++) {
            Vector2 point = ordered.get(orderIndex).point;
            float distance = Vector2.dst2(point.x, point.y, hint.centerX, hint.centerY);
            if (distance <= maximumDistanceSquared) {
                candidates.add(new RouteHintPortalCandidate(orderIndex, -1, distance));
            }
        }
        candidates.sort(new Comparator<RouteHintPortalCandidate>() {
            @Override
            public int compare(RouteHintPortalCandidate left, RouteHintPortalCandidate right) {
                return Float.compare(left.distanceSquared, right.distanceSquared);
            }
        });

        Array<RouteHintPortalMarker> markers = new Array<RouteHintPortalMarker>();
        for (int i = 0; i < candidates.size && markers.size < 4; i++) {
            RouteHintPortalCandidate candidate = candidates.get(i);
            Vector2 point = ordered.get(candidate.orderIndex).point;
            Vector2 direction = new Vector2(point.x - hint.centerX, point.y - hint.centerY);
            if (direction.isZero(0.0001f)) {
                continue;
            }
            direction.nor();
            boolean sameArm = false;
            for (int j = 0; j < markers.size; j++) {
                Vector2 selected = markers.get(j).directionFromHint(hint);
                if (!selected.isZero(0.0001f) && direction.dot(selected) > 0.82f) {
                    sameArm = true;
                    break;
                }
            }
            if (!sameArm) {
                markers.add(new RouteHintPortalMarker(candidate.orderIndex, -1, point));
            }
        }
        return markers;
    }

    private static boolean areOppositeRouteHintPortalMarkers(
            RouteHintCircle hint,
            RouteHintPortalMarker first,
            RouteHintPortalMarker second) {
        if (first == second || first == null || second == null) {
            return false;
        }
        Vector2 firstDirection = first.directionFromHint(hint);
        Vector2 secondDirection = second.directionFromHint(hint);
        if (firstDirection.isZero(0.0001f) || secondDirection.isZero(0.0001f)) {
            return false;
        }
        return firstDirection.dot(secondDirection) <= -0.70f;
    }

    private static RouteHintCircle nearestRouteHint(Vector2 point, Array<RouteHintCircle> routeHints) {
        RouteHintCircle best = null;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < routeHints.size; i++) {
            RouteHintCircle hint = routeHints.get(i);
            float distance = Vector2.dst2(point.x, point.y, hint.centerX, hint.centerY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = hint;
            }
        }
        return best;
    }

    private static boolean shouldPreferRouteLineContinuityOrder(
            Array<RouteLineWaypoint> baseline,
            Array<RouteLineWaypoint> continuity,
            ArenaMap routeMap,
            Vector2 start,
            Vector2 forward) {
        float continuityScore = scoreRouteLineWaypointOrder(continuity, routeMap, start, forward);
        float baselineScore = scoreRouteLineWaypointOrder(baseline, routeMap, start, forward);
        if (continuityScore < baselineScore) {
            return true;
        }
        if (baseline == null || baseline.size < 2 || continuity == null || continuity.size < 2) {
            return false;
        }
        if (findUnsafeRouteSegment(routeMap, continuity.get(0).point, continuity.get(1).point, 0) != null) {
            return false;
        }
        float baselineFirstDistance = baseline.get(0).point.dst(baseline.get(1).point);
        float continuityFirstDistance = continuity.get(0).point.dst(continuity.get(1).point);
        return continuityFirstDistance <= baselineFirstDistance * 0.70f;
    }

    private static Array<RouteLineWaypoint> orderRouteLineWaypointsByRoute(
            Array<Vector2> markers,
            Array<Vector2> route) {
        Array<RouteLineWaypoint> ordered = new Array<RouteLineWaypoint>();
        if (markers == null || markers.size == 0 || route == null || route.size < 2) {
            return ordered;
        }

        float[] cumulative = buildRouteCumulativeDistances(route);
        if (cumulative.length == 0) {
            return ordered;
        }
        float routeLength = cumulative[cumulative.length - 1];
        if (routeLength <= 0.001f) {
            return ordered;
        }

        for (int i = 0; i < markers.size; i++) {
            Vector2 marker = markers.get(i);
            ordered.add(new RouteLineWaypoint(
                    marker,
                    routeProgressOnRoute(route, cumulative, routeLength, marker)));
        }
        ordered.sort(new Comparator<RouteLineWaypoint>() {
            @Override
            public int compare(RouteLineWaypoint left, RouteLineWaypoint right) {
                int progressCompare = Float.compare(left.progress, right.progress);
                if (progressCompare != 0) {
                    return progressCompare;
                }
                int yCompare = Float.compare(left.point.y, right.point.y);
                if (yCompare != 0) {
                    return yCompare;
                }
                return Float.compare(left.point.x, right.point.x);
            }
        });
        return ordered;
    }

    private static Array<RouteLineWaypoint> orderRouteLineWaypointsByRouteSweep(
            ArenaMap routeMap,
            Array<Vector2> markers,
            Array<Vector2> route) {
        Array<RouteLineWaypoint> ordered = new Array<RouteLineWaypoint>();
        if (routeMap == null || markers == null || markers.size == 0 || route == null || route.size < 2) {
            return ordered;
        }

        float[] cumulative = buildRouteCumulativeDistances(route);
        if (cumulative.length == 0) {
            return ordered;
        }
        float routeLength = cumulative[cumulative.length - 1];
        if (routeLength <= 0.001f) {
            return ordered;
        }

        boolean[] assigned = new boolean[markers.size];
        Vector2 routeStart = route.first();
        float startMarkerDistance =
                ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * 4f;
        for (int markerIndex = 0; markerIndex < markers.size; markerIndex++) {
            Vector2 marker = markers.get(markerIndex);
            if (marker.dst2(routeStart) <= startMarkerDistance) {
                ordered.add(new RouteLineWaypoint(marker, 0f));
                assigned[markerIndex] = true;
            }
        }

        for (int segmentIndex = 0; segmentIndex < route.size; segmentIndex++) {
            Vector2 start = route.get(segmentIndex);
            Vector2 end = route.get((segmentIndex + 1) % route.size);
            float segmentX = end.x - start.x;
            float segmentY = end.y - start.y;
            float segmentLength = (float) Math.sqrt(segmentX * segmentX + segmentY * segmentY);
            if (segmentLength <= 0.0001f) {
                continue;
            }
            float tangentX = segmentX / segmentLength;
            float tangentY = segmentY / segmentLength;
            float normalX = -tangentY;
            float normalY = tangentX;
            float alongTolerance = Math.min(ROUTE_LINE_MARKER_SWEEP_GATE_PADDING, segmentLength * 0.50f);

            for (int markerIndex = 0; markerIndex < markers.size; markerIndex++) {
                if (assigned[markerIndex]) {
                    continue;
                }
                Vector2 marker = markers.get(markerIndex);
                float deltaX = marker.x - start.x;
                float deltaY = marker.y - start.y;
                float along = deltaX * tangentX + deltaY * tangentY;
                if (along < -alongTolerance || along > segmentLength + alongTolerance) {
                    continue;
                }

                float clampedAlong = MathUtils.clamp(along, 0f, segmentLength);
                float projectedX = start.x + tangentX * clampedAlong;
                float projectedY = start.y + tangentY * clampedAlong;
                float lateralX = marker.x - projectedX;
                float lateralY = marker.y - projectedY;
                float signedLateral = lateralX * normalX + lateralY * normalY;
                float lateralDistance = Math.abs(signedLateral);
                float gateLimit =
                        routeLineMarkerSweepGateLimit(
                                routeMap,
                                projectedX,
                                projectedY,
                                normalX,
                                normalY,
                                signedLateral);
                if (lateralDistance > gateLimit) {
                    continue;
                }

                ordered.add(new RouteLineWaypoint(
                        marker,
                        wrapRouteProgress(cumulative[segmentIndex] + clampedAlong, routeLength)));
                assigned[markerIndex] = true;
            }
        }

        sortRouteLineWaypointsByProgress(ordered);
        return ordered;
    }

    private static float routeLineMarkerSweepGateLimit(
            ArenaMap routeMap,
            float x,
            float y,
            float normalX,
            float normalY,
            float signedLateral) {
        float side = signedLateral >= 0f ? 1f : -1f;
        float clearance = 0f;
        for (float distance = ROUTE_LINE_MARKER_SWEEP_CLEARANCE_STEP;
                distance <= ROUTE_LINE_MARKER_SWEEP_MAX_LATERAL;
                distance += ROUTE_LINE_MARKER_SWEEP_CLEARANCE_STEP) {
            if (!routeMap.approximateSupports(
                    x + normalX * side * distance,
                    y + normalY * side * distance)) {
                break;
            }
            clearance = distance;
        }
        return Math.min(
                ROUTE_LINE_MARKER_SWEEP_MAX_LATERAL,
                clearance + ROUTE_LINE_MARKER_SWEEP_GATE_PADDING);
    }

    private static void sortRouteLineWaypointsByProgress(Array<RouteLineWaypoint> waypoints) {
        waypoints.sort(new Comparator<RouteLineWaypoint>() {
            @Override
            public int compare(RouteLineWaypoint left, RouteLineWaypoint right) {
                int progressCompare = Float.compare(left.progress, right.progress);
                if (progressCompare != 0) {
                    return progressCompare;
                }
                int yCompare = Float.compare(left.point.y, right.point.y);
                if (yCompare != 0) {
                    return yCompare;
                }
                return Float.compare(left.point.x, right.point.x);
            }
        });
    }

    private static Array<RouteLineWaypoint> alignRouteMarkersWithRouteStart(
            Array<RouteLineWaypoint> ordered,
            Array<Vector2> route) {
        if (ordered == null || ordered.size < 2 || route == null || route.size < 2) {
            return ordered;
        }

        Vector2 routeStart = route.first();
        int startMarkerIndex = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < ordered.size; i++) {
            float distance = ordered.get(i).point.dst2(routeStart);
            if (distance < bestDistance) {
                bestDistance = distance;
                startMarkerIndex = i;
            }
        }
        float startMarkerDistance =
                ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * 4f;
        if (startMarkerIndex <= 0 || bestDistance > startMarkerDistance) {
            return ordered;
        }

        Array<RouteLineWaypoint> adjusted = new Array<RouteLineWaypoint>(ordered.size);
        for (int i = 0; i < ordered.size; i++) {
            RouteLineWaypoint marker = ordered.get(i);
            adjusted.add(new RouteLineWaypoint(
                    marker.point,
                    i == startMarkerIndex ? 0f : marker.progress));
        }
        adjusted.sort(new Comparator<RouteLineWaypoint>() {
            @Override
            public int compare(RouteLineWaypoint left, RouteLineWaypoint right) {
                int progressCompare = Float.compare(left.progress, right.progress);
                if (progressCompare != 0) {
                    return progressCompare;
                }
                int yCompare = Float.compare(left.point.y, right.point.y);
                if (yCompare != 0) {
                    return yCompare;
                }
                return Float.compare(left.point.x, right.point.x);
            }
        });
        return adjusted;
    }

    private static Array<RouteLineWaypoint> chooseRouteLineDirectionAndStart(
            Array<RouteLineWaypoint> sorted,
            ArenaMap routeMap,
            Vector2 start,
            Vector2 forward) {
        Array<RouteLineWaypoint> best = new Array<RouteLineWaypoint>();
        if (sorted == null || sorted.size == 0) {
            return best;
        }

        float bestScore = Float.MAX_VALUE;
        for (int reversed = 0; reversed < 2; reversed++) {
            Array<RouteLineWaypoint> candidate = copyRouteLineWaypoints(sorted, reversed == 1);
            int startIndex = findRouteLineWaypointStartIndex(candidate, start, forward);
            if (startIndex < 0) {
                continue;
            }
            candidate = rotateRouteLineWaypoints(candidate, startIndex);
            float score = scoreRouteLineWaypointOrder(candidate, routeMap, start, forward);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static Array<RouteLineWaypoint> copyRouteLineWaypoints(
            Array<RouteLineWaypoint> source,
            boolean reversed) {
        Array<RouteLineWaypoint> copy = new Array<RouteLineWaypoint>(source.size);
        if (reversed) {
            for (int i = source.size - 1; i >= 0; i--) {
                copy.add(source.get(i));
            }
        } else {
            for (int i = 0; i < source.size; i++) {
                copy.add(source.get(i));
            }
        }
        return copy;
    }

    private static Array<RouteLineWaypoint> rotateRouteLineWaypoints(
            Array<RouteLineWaypoint> source,
            int startIndex) {
        Array<RouteLineWaypoint> rotated = new Array<RouteLineWaypoint>(source.size);
        if (source.size == 0) {
            return rotated;
        }
        int normalizedStart = MathUtils.clamp(startIndex, 0, source.size - 1);
        for (int offset = 0; offset < source.size; offset++) {
            rotated.add(source.get((normalizedStart + offset) % source.size));
        }
        return rotated;
    }

    private static Array<RouteLineWaypoint> orderRouteLineWaypointsByContinuity(
            Array<RouteLineWaypoint> ordered,
            ArenaMap routeMap,
            Vector2 start,
            Vector2 forward) {
        Array<RouteLineWaypoint> result = new Array<RouteLineWaypoint>();
        if (ordered == null || ordered.size < 4 || routeMap == null) {
            return result;
        }
        int startIndex = findRouteLineWaypointStartIndex(ordered, start, forward);
        if (startIndex < 0) {
            return result;
        }

        boolean[] used = new boolean[ordered.size];
        RouteLineWaypoint first = ordered.get(startIndex);
        result.add(first);
        used[startIndex] = true;

        Vector2 previousDirection = new Vector2(first.point).sub(start);
        if (previousDirection.isZero(0.0001f) && forward != null) {
            previousDirection.set(forward);
        }
        if (!previousDirection.isZero(0.0001f)) {
            previousDirection.nor();
        }

        int currentIndex = startIndex;
        while (result.size < ordered.size) {
            RouteLineWaypoint current = ordered.get(currentIndex);
            int bestIndex = -1;
            float bestScore = Float.MAX_VALUE;
            for (int i = 0; i < ordered.size; i++) {
                if (used[i]) {
                    continue;
                }
                RouteLineWaypoint candidate = ordered.get(i);
                float score =
                        routeLineContinuityStepScore(
                                routeMap,
                                current.point,
                                candidate.point,
                                previousDirection);
                if (score < bestScore) {
                    bestScore = score;
                    bestIndex = i;
                }
            }

            if (bestIndex < 0) {
                break;
            }

            if (result.size >= 4) {
                float closeScore =
                        routeLineContinuityStepScore(
                                routeMap,
                                current.point,
                                first.point,
                                previousDirection);
                if (closeScore + 8.0f < bestScore) {
                    break;
                }
            }

            RouteLineWaypoint next = ordered.get(bestIndex);
            Vector2 nextDirection = new Vector2(next.point).sub(current.point);
            if (!nextDirection.isZero(0.0001f)) {
                nextDirection.nor();
                previousDirection.set(nextDirection);
            }
            result.add(next);
            used[bestIndex] = true;
            currentIndex = bestIndex;
        }
        return result;
    }

    private static float routeLineContinuityStepScore(
            ArenaMap routeMap,
            Vector2 from,
            Vector2 to,
            Vector2 previousDirection) {
        float distance = from.dst(to);
        float unsafePenalty = findUnsafeRouteSegment(routeMap, from, to, 0) == null ? 0f : 100000f;
        float alignmentPenalty = 0f;
        if (previousDirection != null && !previousDirection.isZero(0.0001f) && distance > 0.0001f) {
            float dx = (to.x - from.x) / distance;
            float dy = (to.y - from.y) / distance;
            float alignment = dx * previousDirection.x + dy * previousDirection.y;
            alignmentPenalty = (1f - alignment) * 18.0f;
            if (alignment < -0.25f) {
                alignmentPenalty += 2500f;
            }
        }
        return unsafePenalty + distance + alignmentPenalty;
    }

    private static Array<RouteLineWaypoint> repairUnsafeFirstRouteLineConnection(
            Array<RouteLineWaypoint> ordered,
            ArenaMap routeMap,
            Vector2 start,
            Vector2 forward) {
        if (ordered == null || ordered.size < 4 || routeMap == null) {
            return ordered;
        }
        if (findUnsafeRouteSegment(routeMap, ordered.get(0).point, ordered.get(1).point, 0) == null) {
            return ordered;
        }

        Array<RouteLineWaypoint> best = ordered;
        float bestScore = scoreRouteLineWaypointOrder(ordered, routeMap, start, forward);
        for (int splitIndex = 2; splitIndex < ordered.size; splitIndex++) {
            Array<RouteLineWaypoint> candidate =
                    moveRouteLineTailAfterFirst(ordered, splitIndex);
            if (candidate.size != ordered.size) {
                continue;
            }
            float score = scoreRouteLineWaypointOrder(candidate, routeMap, start, forward);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static Array<RouteLineWaypoint> moveRouteLineTailAfterFirst(
            Array<RouteLineWaypoint> ordered,
            int splitIndex) {
        Array<RouteLineWaypoint> result = new Array<RouteLineWaypoint>(ordered.size);
        if (ordered.size == 0) {
            return result;
        }
        result.add(ordered.get(0));
        for (int i = splitIndex; i < ordered.size; i++) {
            result.add(ordered.get(i));
        }
        for (int i = 1; i < splitIndex; i++) {
            result.add(ordered.get(i));
        }
        return result;
    }

    private static Array<RouteLineWaypoint> truncateRouteLineAtEarlyClosure(
            Array<RouteLineWaypoint> ordered,
            ArenaMap routeMap) {
        if (ordered == null || ordered.size < 6 || routeMap == null) {
            return ordered;
        }

        for (int i = 3; i < ordered.size - 1; i++) {
            RouteLineWaypoint previous = ordered.get(i - 1);
            RouteLineWaypoint current = ordered.get(i);
            RouteLineWaypoint next = ordered.get(i + 1);
            RouteLineWaypoint first = ordered.get(0);
            if (findUnsafeRouteSegment(routeMap, current.point, first.point, i) != null) {
                continue;
            }

            float closeAlignment = routeLineTurnAlignment(previous.point, current.point, first.point);
            float nextAlignment = routeLineTurnAlignment(previous.point, current.point, next.point);
            float closeDistance = current.point.dst(first.point);
            float nextDistance = current.point.dst(next.point);
            boolean closingIsContinuous = closeAlignment >= 0.55f && closeAlignment >= nextAlignment + 0.65f;
            boolean closingIsCompetitive = closeDistance <= Math.max(nextDistance * 1.35f, nextDistance + 4.0f);
            if (!closingIsContinuous || !closingIsCompetitive) {
                continue;
            }

            Array<RouteLineWaypoint> truncated = new Array<RouteLineWaypoint>(i + 1);
            for (int copyIndex = 0; copyIndex <= i; copyIndex++) {
                truncated.add(ordered.get(copyIndex));
            }
            return truncated;
        }
        return ordered;
    }

    private static float routeLineTurnAlignment(Vector2 previous, Vector2 current, Vector2 next) {
        float incomingX = current.x - previous.x;
        float incomingY = current.y - previous.y;
        float outgoingX = next.x - current.x;
        float outgoingY = next.y - current.y;
        float incomingLength = (float) Math.sqrt(incomingX * incomingX + incomingY * incomingY);
        float outgoingLength = (float) Math.sqrt(outgoingX * outgoingX + outgoingY * outgoingY);
        if (incomingLength <= 0.0001f || outgoingLength <= 0.0001f) {
            return -1f;
        }
        return (incomingX * outgoingX + incomingY * outgoingY) / (incomingLength * outgoingLength);
    }

    private static int findRouteLineWaypointStartIndex(
            Array<RouteLineWaypoint> waypoints,
            Vector2 start,
            Vector2 forward) {
        if (waypoints == null || waypoints.size == 0 || start == null) {
            return -1;
        }
        boolean hasForward = forward != null && !forward.isZero(0.0001f);
        int bestIndex = -1;
        float bestScore = Float.MAX_VALUE;
        Vector2 delta = new Vector2();
        for (int i = 0; i < waypoints.size; i++) {
            RouteLineWaypoint waypoint = waypoints.get(i);
            delta.set(waypoint.point).sub(start);
            float distanceSquared = delta.len2();
            if (distanceSquared <= 0.0001f) {
                continue;
            }

            float score = distanceSquared;
            if (hasForward) {
                float distance = (float) Math.sqrt(distanceSquared);
                float alignment = delta.dot(forward) / distance;
                score *= MathUtils.lerp(2.80f, 0.45f, (alignment + 1f) * 0.5f);
                if (alignment < -0.10f) {
                    score += 100000f + -alignment * 10000f;
                }
            }
            if (score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static float scoreRouteLineWaypointOrder(
            Array<RouteLineWaypoint> waypoints,
            ArenaMap routeMap,
            Vector2 start,
            Vector2 forward) {
        if (waypoints == null || waypoints.size == 0 || start == null) {
            return Float.MAX_VALUE;
        }
        Array<Vector2> route = smoothRouteLineVertices(buildRouteLineRoute(start, waypoints));
        UnsafeRouteSample unsafe = findUnsafeRouteSample(routeMap, route);

        Vector2 first = waypoints.first().point;
        Vector2 last = waypoints.peek().point;
        Vector2 firstDelta = new Vector2(first).sub(start);
        float firstDistance = firstDelta.len();
        float alignment = 0f;
        if (forward != null && !forward.isZero(0.0001f) && firstDistance > 0.0001f) {
            alignment = firstDelta.dot(forward) / firstDistance;
        }
        float closeDistance = last.dst(start);
        float score =
                Math.max(0f, 1f - alignment) * 2500f
                        + firstDistance * 2.0f
                        + closeDistance * 0.35f;
        if (unsafe != null) {
            score += 100000f;
        }
        return score;
    }

    private static Array<Vector2> buildRouteLineRoute(
            Vector2 start,
            Array<RouteLineWaypoint> waypoints) {
        Array<Vector2> route = new Array<Vector2>();
        if (waypoints == null) {
            return route;
        }
        for (int i = 0; i < waypoints.size; i++) {
            addRoutePoint(route, waypoints.get(i).point);
        }
        return route;
    }

    private static Vector2 routeLineStartPosition(
            Array<SpawnPoint> spawnPoints,
            Vector2 forward) {
        SpawnPoint frontSpawn = findFrontSpawnPoint(spawnPoints, forward);
        if (frontSpawn != null) {
            return new Vector2(frontSpawn.x, frontSpawn.y);
        }
        return averageSpawnPosition(spawnPoints);
    }

    private static void orientRouteLineAwayFromSpawnGrid(
            Array<Vector2> route,
            Array<SpawnPoint> spawnPoints) {
        if (route == null || route.size < 3 || spawnPoints == null || spawnPoints.size == 0) {
            return;
        }
        Vector2 spawnCenter = averageSpawnPosition(spawnPoints);
        Vector2 next = route.get(1);
        Vector2 previous = route.peek();
        float nextDistance = next.dst2(spawnCenter);
        float previousDistance = previous.dst2(spawnCenter);
        float orientationMargin = ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD;
        if (nextDistance + orientationMargin < previousDistance) {
            reverseRouteCenterline(route);
        }
    }

    private static float routeProgressOnRoute(
            Array<Vector2> route,
            float[] cumulative,
            float routeLength,
            Vector2 point) {
        float routeStartDistance = point.dst2(route.first());
        float bestDistance = Float.MAX_VALUE;
        float bestProgress = 0f;
        for (int i = 0; i < route.size; i++) {
            Vector2 start = route.get(i);
            Vector2 end = route.get((i + 1) % route.size);
            float segmentX = end.x - start.x;
            float segmentY = end.y - start.y;
            float segmentLengthSquared = segmentX * segmentX + segmentY * segmentY;
            if (segmentLengthSquared <= 0.0001f) {
                continue;
            }
            float alpha =
                    MathUtils.clamp(
                            ((point.x - start.x) * segmentX + (point.y - start.y) * segmentY)
                                    / segmentLengthSquared,
                            0f,
                            1f);
            float projectedX = start.x + segmentX * alpha;
            float projectedY = start.y + segmentY * alpha;
            float distance = Vector2.dst2(point.x, point.y, projectedX, projectedY);
            if (distance < bestDistance) {
                bestDistance = distance;
                float segmentLength = (float) Math.sqrt(segmentLengthSquared);
                bestProgress = wrapRouteProgress(cumulative[i] + segmentLength * alpha, routeLength);
            }
        }
        float startBiasDistance =
                ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * 4f;
        if (bestProgress > routeLength * 0.50f
                && routeStartDistance <= Math.max(bestDistance + 0.02f, startBiasDistance)) {
            return 0f;
        }
        return bestProgress;
    }

    private static void warnIfRouteLineIntersectsOffroad(
            ArenaMap routeMap,
            Array<Vector2> route,
            Array<RouteLineWaypoint> orderedMarkers,
            Array<RouteHintCircle> routeHints) {
        UnsafeRouteSample unsafe = findUnsafeRouteSample(routeMap, route, routeHints);
        if (unsafe == null) {
            return;
        }
        if ((unsafe.fromOrangeIndex <= 0 || unsafe.toOrangeIndex <= 0)
                && orderedMarkers != null
                && orderedMarkers.size >= 2) {
            assignNearestRouteLineMarkerConnection(unsafe, orderedMarkers);
        }
        if (unsafe.fromOrangeIndex > 0 && unsafe.toOrangeIndex > 0) {
            System.out.printf(
                    Locale.ROOT,
                    "route_line_warning map=%s issue=intersects_offroad orange_from=%d orange_to=%d x=%.3f y=%.3f%n",
                    routeLogMapId(routeMap),
                    unsafe.fromOrangeIndex,
                    unsafe.toOrangeIndex,
                    unsafe.x,
                    unsafe.y);
        } else {
            System.out.printf(
                    Locale.ROOT,
                    "route_line_warning map=%s issue=intersects_offroad segment=%d x=%.3f y=%.3f%n",
                    routeLogMapId(routeMap),
                    unsafe.segmentIndex,
                    unsafe.x,
                    unsafe.y);
        }
    }

    private static void assignNearestRouteLineMarkerConnection(
            UnsafeRouteSample unsafe,
            Array<RouteLineWaypoint> orderedMarkers) {
        float bestDistance = Float.MAX_VALUE;
        int bestIndex = -1;
        for (int i = 0; i < orderedMarkers.size; i++) {
            Vector2 start = orderedMarkers.get(i).point;
            Vector2 end = orderedMarkers.get((i + 1) % orderedMarkers.size).point;
            float distance =
                    distanceToSegmentSquared(
                            unsafe.x,
                            unsafe.y,
                            start.x,
                            start.y,
                            end.x,
                            end.y);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        if (bestIndex >= 0) {
            unsafe.fromOrangeIndex = bestIndex + 1;
            unsafe.toOrangeIndex = ((bestIndex + 1) % orderedMarkers.size) + 1;
        }
    }

    private static UnsafeRouteSample findUnsafeRouteLineConnection(
            ArenaMap routeMap,
            Array<RouteLineWaypoint> orderedMarkers) {
        if (routeMap == null || orderedMarkers == null || orderedMarkers.size < 2) {
            return null;
        }
        for (int i = 0; i < orderedMarkers.size; i++) {
            Vector2 p0 = orderedMarkers.get((i + orderedMarkers.size - 1) % orderedMarkers.size).point;
            Vector2 p1 = orderedMarkers.get(i).point;
            Vector2 p2 = orderedMarkers.get((i + 1) % orderedMarkers.size).point;
            Vector2 p3 = orderedMarkers.get((i + 2) % orderedMarkers.size).point;
            int samples =
                    Math.max(
                            2,
                            MathUtils.ceil(p1.dst(p2) / ROUTE_LINE_VERTEX_SMOOTHING_STEP_WORLD));
            Vector2 previous = new Vector2(p1);
            for (int sample = 1; sample <= samples; sample++) {
                Vector2 current =
                        sample == samples
                                ? new Vector2(p2)
                                : cardinalRoutePoint(p0, p1, p2, p3, sample / (float) samples);
                UnsafeRouteSample unsafe = findUnsafeRouteSegment(routeMap, previous, current, i);
                if (unsafe != null) {
                    unsafe.fromOrangeIndex = i + 1;
                    unsafe.toOrangeIndex = ((i + 1) % orderedMarkers.size) + 1;
                    return unsafe;
                }
                previous.set(current);
            }
        }
        return null;
    }

    private static UnsafeRouteSample findUnsafeRouteSample(
            ArenaMap routeMap,
            Array<Vector2> route) {
        return findUnsafeRouteSample(routeMap, route, null);
    }

    private static UnsafeRouteSample findUnsafeRouteSample(
            ArenaMap routeMap,
            Array<Vector2> route,
            Array<RouteHintCircle> routeHints) {
        if (routeMap == null || route == null || route.size < 2) {
            return null;
        }
        for (int i = 0; i < route.size; i++) {
            Vector2 start = route.get(i);
            Vector2 end = route.get((i + 1) % route.size);
            float distance = start.dst(end);
            int samples = Math.max(1, MathUtils.ceil(distance / ROUTE_CENTERLINE_EXACT_SAMPLE_STEP));
            for (int sample = 0; sample <= samples; sample++) {
                float alpha = sample / (float) samples;
                float x = start.x + (end.x - start.x) * alpha;
                float y = start.y + (end.y - start.y) * alpha;
                if (!routeMap.approximateSupports(x, y)
                        && !isInsideRouteHint(routeHints, x, y)) {
                    return new UnsafeRouteSample(i, x, y);
                }
            }
        }
        return null;
    }

    private static boolean isInsideRouteHint(Array<RouteHintCircle> routeHints, float x, float y) {
        if (routeHints == null) {
            return false;
        }
        Vector2 point = new Vector2(x, y);
        for (int i = 0; i < routeHints.size; i++) {
            if (routeHints.get(i).contains(point, ROUTE_HINT_SEGMENT_MARGIN_WORLD)) {
                return true;
            }
        }
        return false;
    }

    private static UnsafeRouteSample findUnsafeRouteSegment(
            ArenaMap routeMap,
            Vector2 start,
            Vector2 end,
            int segmentIndex) {
        float distance = start.dst(end);
        int samples = Math.max(1, MathUtils.ceil(distance / ROUTE_CENTERLINE_EXACT_SAMPLE_STEP));
        for (int sample = 0; sample <= samples; sample++) {
            float alpha = sample / (float) samples;
            float x = start.x + (end.x - start.x) * alpha;
            float y = start.y + (end.y - start.y) * alpha;
            if (!routeMap.approximateSupports(x, y)) {
                return new UnsafeRouteSample(segmentIndex, x, y);
            }
        }
        return null;
    }

    private static String routeLogMapId(ArenaMap routeMap) {
        if (routeMap == null || routeMap.getId() == null) {
            return "unknown";
        }
        String id = routeMap.getId();
        String suffix = "-checkpoint-order";
        return id.endsWith(suffix) ? id.substring(0, id.length() - suffix.length()) : id;
    }

    private static Array<Vector2> smoothRouteLineVertices(Array<Vector2> route) {
        if (route == null || route.size < 4) {
            return route;
        }

        Array<Vector2> smoothed = new Array<Vector2>(route.size * 4);
        for (int i = 0; i < route.size; i++) {
            Vector2 p0 = route.get((i + route.size - 1) % route.size);
            Vector2 p1 = route.get(i);
            Vector2 p2 = route.get((i + 1) % route.size);
            Vector2 p3 = route.get((i + 2) % route.size);
            addRoutePoint(smoothed, p1);

            int samples =
                    Math.max(
                            2,
                            MathUtils.ceil(p1.dst(p2) / ROUTE_LINE_VERTEX_SMOOTHING_STEP_WORLD));
            for (int sample = 1; sample < samples; sample++) {
                float t = sample / (float) samples;
                addRoutePoint(smoothed, cardinalRoutePoint(p0, p1, p2, p3, t));
            }
        }
        return smoothed;
    }

    private static void clampRouteLineToPlayable(ArenaMap routeMap, Array<Vector2> route) {
        if (routeMap == null || route == null) {
            return;
        }
        for (int i = 0; i < route.size; i++) {
            routeMap.clampToPlayable(route.get(i), ROUTE_CENTERLINE_MIN_CLEARANCE);
        }
    }

    private static Vector2 cardinalRoutePoint(
            Vector2 p0,
            Vector2 p1,
            Vector2 p2,
            Vector2 p3,
            float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        float tangent1X = (p2.x - p0.x) * ROUTE_LINE_CARDINAL_TENSION;
        float tangent1Y = (p2.y - p0.y) * ROUTE_LINE_CARDINAL_TENSION;
        float tangent2X = (p3.x - p1.x) * ROUTE_LINE_CARDINAL_TENSION;
        float tangent2Y = (p3.y - p1.y) * ROUTE_LINE_CARDINAL_TENSION;
        float h00 = 2f * t3 - 3f * t2 + 1f;
        float h10 = t3 - 2f * t2 + t;
        float h01 = -2f * t3 + 3f * t2;
        float h11 = t3 - t2;
        float x =
                h00 * p1.x
                        + h10 * tangent1X
                        + h01 * p2.x
                        + h11 * tangent2X;
        float y =
                h00 * p1.y
                        + h10 * tangent1Y
                        + h01 * p2.y
                        + h11 * tangent2Y;
        return new Vector2(x, y);
    }

    private static Array<Vector2> buildRouteFromContinuousRouteLine(
            ArenaMap routeMap,
            Array<SpawnPoint> spawnPoints,
            boolean[] routeLinePixels,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        Array<Vector2> route = new Array<Vector2>();
        if (routeMap == null
                || spawnPoints == null
                || spawnPoints.size == 0
                || routeLinePixels == null
                || routeLinePixels.length != imageWidth * imageHeight) {
            return route;
        }

        RouteLineComponent component =
                findLargestRouteLineComponent(routeLinePixels, imageWidth, imageHeight);
        if (component.count < ROUTE_LINE_CONTINUOUS_MIN_PIXELS) {
            return route;
        }

        boolean[] skeleton =
                skeletonizeRouteLine(component.pixels, imageWidth, imageHeight);
        int skeletonCount = countRouteLinePixels(skeleton);
        if (skeletonCount < 4) {
            return route;
        }

        Vector2 forward = averageSpawnForward(spawnPoints);
        if (forward.isZero(0.0001f)) {
            return route;
        }

        Vector2 start = averageSpawnPosition(spawnPoints);
        routeMap.clampToPlayable(start, ROUTE_CENTERLINE_MIN_CLEARANCE);
        Vector2 side = new Vector2(-forward.y, forward.x);
        centerRoutePoint(routeMap, start, side);

        float startX = worldToImageX(start.x, imageWidth, worldMinX, worldWidth);
        float startY = worldToImageY(start.y, imageHeight, worldMinY, worldHeight);
        float imageForwardX = forward.x * imageWidth / worldWidth;
        float imageForwardY = -forward.y * imageHeight / worldHeight;
        float imageForwardLength =
                (float) Math.sqrt(imageForwardX * imageForwardX + imageForwardY * imageForwardY);
        if (imageForwardLength <= 0.0001f) {
            return route;
        }
        imageForwardX /= imageForwardLength;
        imageForwardY /= imageForwardLength;

        int[] orderedPixels =
                orderRouteLineSkeletonPixels(
                        skeleton,
                        skeletonCount,
                        imageWidth,
                        imageHeight,
                        startX,
                        startY,
                        imageForwardX,
                        imageForwardY);
        if (orderedPixels.length < 4
                || orderedPixels.length
                        < MathUtils.ceil(skeletonCount * ROUTE_LINE_ORDER_MIN_VISITED_FRACTION)) {
            return route;
        }

        route =
                routeFromOrderedRoutePixels(
                        routeMap,
                        orderedPixels,
                        imageWidth,
                        imageHeight,
                        worldMinX,
                        worldMinY,
                        worldWidth,
                        worldHeight);
        if (route.size < 4 || !isRouteCenterlineSafe(routeMap, route)) {
            return new Array<Vector2>();
        }

        alignRouteCenterlineWithSpawnDirection(route, spawnPoints, forward);
        return smoothRouteCenterline(routeMap, route);
    }

    private static RouteLineComponent findLargestRouteLineComponent(
            boolean[] routeLinePixels,
            int width,
            int height) {
        boolean[] visited = new boolean[routeLinePixels.length];
        int[] queue = new int[routeLinePixels.length];
        int[] bestPixels = new int[0];
        int bestCount = 0;

        for (int index = 0; index < routeLinePixels.length; index++) {
            if (!routeLinePixels[index] || visited[index]) {
                continue;
            }

            int head = 0;
            int tail = 0;
            visited[index] = true;
            queue[tail++] = index;
            while (head < tail) {
                int current = queue[head++];
                int x = current % width;
                int y = current / width;
                for (int dy = -1; dy <= 1; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= height) {
                        continue;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        int nx = x + dx;
                        if (nx < 0 || nx >= width) {
                            continue;
                        }
                        int neighbor = ny * width + nx;
                        if (routeLinePixels[neighbor] && !visited[neighbor]) {
                            visited[neighbor] = true;
                            queue[tail++] = neighbor;
                        }
                    }
                }
            }

            if (tail > bestCount) {
                bestCount = tail;
                bestPixels = Arrays.copyOf(queue, tail);
            }
        }

        boolean[] pixels = new boolean[routeLinePixels.length];
        for (int i = 0; i < bestCount; i++) {
            pixels[bestPixels[i]] = true;
        }
        return new RouteLineComponent(pixels, bestCount);
    }

    private static boolean[] skeletonizeRouteLine(
            boolean[] routeLinePixels,
            int width,
            int height) {
        boolean[] skeleton = Arrays.copyOf(routeLinePixels, routeLinePixels.length);
        boolean[] remove = new boolean[routeLinePixels.length];
        for (int iteration = 0; iteration < ROUTE_LINE_SKELETON_MAX_ITERATIONS; iteration++) {
            boolean changed = false;
            int firstPassRemovals =
                    markRouteLineSkeletonRemovals(skeleton, remove, width, height, false);
            if (firstPassRemovals > 0) {
                applyRouteLineSkeletonRemovals(skeleton, remove);
                changed = true;
            }

            int secondPassRemovals =
                    markRouteLineSkeletonRemovals(skeleton, remove, width, height, true);
            if (secondPassRemovals > 0) {
                applyRouteLineSkeletonRemovals(skeleton, remove);
                changed = true;
            }

            if (!changed) {
                break;
            }
        }
        return skeleton;
    }

    private static int markRouteLineSkeletonRemovals(
            boolean[] skeleton,
            boolean[] remove,
            int width,
            int height,
            boolean secondPass) {
        Arrays.fill(remove, false);
        int removals = 0;
        for (int y = 1; y < height - 1; y++) {
            int rowOffset = y * width;
            for (int x = 1; x < width - 1; x++) {
                int index = rowOffset + x;
                if (!skeleton[index]) {
                    continue;
                }

                boolean p2 = skeleton[index - width];
                boolean p3 = skeleton[index - width + 1];
                boolean p4 = skeleton[index + 1];
                boolean p5 = skeleton[index + width + 1];
                boolean p6 = skeleton[index + width];
                boolean p7 = skeleton[index + width - 1];
                boolean p8 = skeleton[index - 1];
                boolean p9 = skeleton[index - width - 1];
                int neighborCount =
                        bool(p2) + bool(p3) + bool(p4) + bool(p5)
                                + bool(p6) + bool(p7) + bool(p8) + bool(p9);
                if (neighborCount < 2 || neighborCount > 6) {
                    continue;
                }
                int transitions =
                        transitionCount(p2, p3, p4, p5, p6, p7, p8, p9);
                if (transitions != 1) {
                    continue;
                }

                if (!secondPass) {
                    if ((p2 && p4 && p6) || (p4 && p6 && p8)) {
                        continue;
                    }
                } else if ((p2 && p4 && p8) || (p2 && p6 && p8)) {
                    continue;
                }
                remove[index] = true;
                removals++;
            }
        }
        return removals;
    }

    private static int bool(boolean value) {
        return value ? 1 : 0;
    }

    private static int transitionCount(
            boolean p2,
            boolean p3,
            boolean p4,
            boolean p5,
            boolean p6,
            boolean p7,
            boolean p8,
            boolean p9) {
        int transitions = 0;
        transitions += !p2 && p3 ? 1 : 0;
        transitions += !p3 && p4 ? 1 : 0;
        transitions += !p4 && p5 ? 1 : 0;
        transitions += !p5 && p6 ? 1 : 0;
        transitions += !p6 && p7 ? 1 : 0;
        transitions += !p7 && p8 ? 1 : 0;
        transitions += !p8 && p9 ? 1 : 0;
        transitions += !p9 && p2 ? 1 : 0;
        return transitions;
    }

    private static void applyRouteLineSkeletonRemovals(
            boolean[] skeleton,
            boolean[] remove) {
        for (int i = 0; i < skeleton.length; i++) {
            if (remove[i]) {
                skeleton[i] = false;
            }
        }
    }

    private static int countRouteLinePixels(boolean[] pixels) {
        int count = 0;
        if (pixels == null) {
            return count;
        }
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i]) {
                count++;
            }
        }
        return count;
    }

    private static int[] orderRouteLineSkeletonPixels(
            boolean[] skeleton,
            int skeletonCount,
            int width,
            int height,
            float startX,
            float startY,
            float forwardX,
            float forwardY) {
        int startIndex =
                findRouteLineSkeletonStartIndex(
                        skeleton,
                        width,
                        startX,
                        startY,
                        forwardX,
                        forwardY);
        if (startIndex < 0) {
            return new int[0];
        }

        int[] ordered = new int[skeletonCount];
        boolean[] used = new boolean[skeleton.length];
        int count = 0;
        int previous = -1;
        int current = startIndex;
        float directionX = forwardX;
        float directionY = forwardY;

        while (count < ordered.length && current >= 0) {
            ordered[count++] = current;
            used[current] = true;

            int next =
                    findNextRouteLineSkeletonPixel(
                            skeleton,
                            used,
                            current,
                            previous,
                            width,
                            height,
                            directionX,
                            directionY);
            if (next < 0) {
                break;
            }

            int currentX = current % width;
            int currentY = current / width;
            int nextX = next % width;
            int nextY = next / width;
            float dx = nextX - currentX;
            float dy = nextY - currentY;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length > 0.0001f) {
                directionX = dx / length;
                directionY = dy / length;
            }
            previous = current;
            current = next;
        }
        return Arrays.copyOf(ordered, count);
    }

    private static int findRouteLineSkeletonStartIndex(
            boolean[] skeleton,
            int width,
            float startX,
            float startY,
            float forwardX,
            float forwardY) {
        float targetX = startX + forwardX * ROUTE_LINE_START_LOOKAHEAD_WORLD;
        float targetY = startY + forwardY * ROUTE_LINE_START_LOOKAHEAD_WORLD;
        int bestIndex = -1;
        float bestScore = Float.MAX_VALUE;
        for (int index = 0; index < skeleton.length; index++) {
            if (!skeleton[index]) {
                continue;
            }
            int x = index % width;
            int y = index / width;
            float dx = x - startX;
            float dy = y - startY;
            float projection = dx * forwardX + dy * forwardY;
            float lateralSquared = Math.max(0f, dx * dx + dy * dy - projection * projection);
            float targetDx = x - targetX;
            float targetDy = y - targetY;
            float score = targetDx * targetDx + targetDy * targetDy + lateralSquared * 0.30f;
            if (projection < 0f) {
                score += 100000f + -projection * 200f;
            }
            if (score < bestScore) {
                bestScore = score;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private static int findNextRouteLineSkeletonPixel(
            boolean[] skeleton,
            boolean[] used,
            int current,
            int previous,
            int width,
            int height,
            float directionX,
            float directionY) {
        int currentX = current % width;
        int currentY = current / width;
        for (int radius = 1; radius <= ROUTE_LINE_GAP_SEARCH_RADIUS; radius++) {
            int bestIndex = -1;
            float bestScore = Float.MAX_VALUE;
            for (int dy = -radius; dy <= radius; dy++) {
                int y = currentY + dy;
                if (y < 0 || y >= height) {
                    continue;
                }
                for (int dx = -radius; dx <= radius; dx++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) {
                        continue;
                    }
                    int x = currentX + dx;
                    if (x < 0 || x >= width) {
                        continue;
                    }
                    int index = y * width + x;
                    if (!skeleton[index] || used[index] || index == previous) {
                        continue;
                    }
                    float distanceSquared = dx * dx + dy * dy;
                    float distance = (float) Math.sqrt(distanceSquared);
                    float alignment = (dx * directionX + dy * directionY) / distance;
                    float score =
                            distanceSquared
                                    * MathUtils.lerp(1.85f, 0.60f, (alignment + 1f) * 0.5f);
                    if (alignment < -0.35f) {
                        score *= 4f;
                    }
                    if (score < bestScore) {
                        bestScore = score;
                        bestIndex = index;
                    }
                }
            }
            if (bestIndex >= 0) {
                return bestIndex;
            }
        }
        return -1;
    }

    private static Array<Vector2> routeFromOrderedRoutePixels(
            ArenaMap routeMap,
            int[] orderedPixels,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        Array<Vector2> route = new Array<Vector2>();
        float minDistanceSquared =
                ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD * ROUTE_LINE_PIXEL_SAMPLE_STEP_WORLD;
        Vector2 point = new Vector2();
        for (int i = 0; i < orderedPixels.length; i++) {
            int pixelIndex = orderedPixels[i];
            int x = pixelIndex % imageWidth;
            int y = pixelIndex / imageWidth;
            point.set(
                    imageToWorld(
                            x,
                            y,
                            imageWidth,
                            imageHeight,
                            worldMinX,
                            worldMinY,
                            worldWidth,
                            worldHeight));
            routeMap.clampToPlayable(point, ROUTE_CENTERLINE_EXACT_MIN_CLEARANCE);
            if (!routeMap.approximateSupports(point)) {
                continue;
            }
            if (route.size == 0 || route.peek().dst2(point) >= minDistanceSquared) {
                route.add(new Vector2(point));
            }
        }
        if (route.size > 4 && route.first().dst2(route.peek()) < minDistanceSquared * 0.50f) {
            route.removeIndex(route.size - 1);
        }
        return route;
    }

    private static Array<Vector2> orderRouteLineMarkers(
            Array<Vector2> markers,
            Vector2 start,
            Vector2 forward) {
        Array<Vector2> ordered = new Array<Vector2>(markers.size);
        if (markers == null || markers.size == 0 || start == null) {
            return ordered;
        }

        int startIndex = findRouteLineStartIndex(markers, start, forward);
        if (startIndex < 0) {
            return ordered;
        }

        boolean[] used = new boolean[markers.size];
        Vector2 current = new Vector2(markers.get(startIndex));
        Vector2 direction = new Vector2(forward);
        if (direction.isZero(0.0001f)) {
            direction.set(current).sub(start);
        }
        if (direction.isZero(0.0001f)) {
            direction.set(1f, 0f);
        } else {
            direction.nor();
        }

        used[startIndex] = true;
        ordered.add(new Vector2(current));
        for (int count = 1; count < markers.size; count++) {
            int nextIndex = findNextRouteLineMarkerIndex(markers, used, current, direction);
            if (nextIndex < 0) {
                break;
            }
            Vector2 next = markers.get(nextIndex);
            Vector2 nextDirection = new Vector2(next).sub(current);
            if (!nextDirection.isZero(0.0001f)) {
                direction.set(nextDirection).nor();
            }
            current.set(next);
            used[nextIndex] = true;
            ordered.add(new Vector2(current));
        }
        return ordered;
    }

    private static int findRouteLineStartIndex(
            Array<Vector2> markers,
            Vector2 start,
            Vector2 forward) {
        if (markers == null || markers.size == 0) {
            return -1;
        }
        Vector2 target = new Vector2(start);
        boolean hasForward = forward != null && !forward.isZero(0.0001f);
        if (hasForward) {
            target.mulAdd(forward, ROUTE_LINE_START_LOOKAHEAD_WORLD);
        }

        int bestIndex = -1;
        float bestScore = Float.MAX_VALUE;
        Vector2 delta = new Vector2();
        for (int i = 0; i < markers.size; i++) {
            Vector2 marker = markers.get(i);
            delta.set(marker).sub(start);
            float score = marker.dst2(target);
            if (hasForward) {
                float projection = delta.dot(forward);
                float lateralSquared = Math.max(0f, delta.len2() - projection * projection);
                score += lateralSquared * 0.30f;
                if (projection < 0f) {
                    score += 100000f + -projection * 200f;
                }
            }
            if (score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static int findNextRouteLineMarkerIndex(
            Array<Vector2> markers,
            boolean[] used,
            Vector2 current,
            Vector2 direction) {
        int bestIndex = -1;
        float bestScore = Float.MAX_VALUE;
        boolean hasDirection = direction != null && !direction.isZero(0.0001f);
        Vector2 delta = new Vector2();
        for (int i = 0; i < markers.size; i++) {
            if (used[i]) {
                continue;
            }
            Vector2 marker = markers.get(i);
            delta.set(marker).sub(current);
            float distanceSquared = delta.len2();
            if (distanceSquared <= 0.0001f) {
                continue;
            }
            float score = distanceSquared;
            if (hasDirection) {
                float distance = (float) Math.sqrt(distanceSquared);
                float alignment = delta.dot(direction) / distance;
                score *= MathUtils.lerp(1.60f, 0.75f, (alignment + 1f) * 0.5f);
                if (alignment < -0.25f) {
                    score *= 3.0f;
                }
            }
            if (score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static Array<Vector2> buildRouteFromOrderMarkers(
            ArenaMap routeMap,
            Array<SpawnPoint> spawnPoints,
            Array<CheckpointOrderMarker> routeOrderMarkers,
            Array<RouteHintCircle> routeHints,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        Array<Vector2> route = new Array<Vector2>();
        if (routeMap == null || routeOrderMarkers == null || routeOrderMarkers.size < 4) {
            return route;
        }

        Array<CheckpointOrderMarker> markers =
                new Array<CheckpointOrderMarker>(routeOrderMarkers);
        markers.sort(new Comparator<CheckpointOrderMarker>() {
            @Override
            public int compare(CheckpointOrderMarker left, CheckpointOrderMarker right) {
                int orderCompare = Integer.compare(left.order, right.order);
                if (orderCompare != 0) {
                    return orderCompare;
                }
                int yCompare = Float.compare(left.centerY, right.centerY);
                if (yCompare != 0) {
                    return yCompare;
                }
                return Float.compare(left.centerX, right.centerX);
            }
        });

        float[][] waypoints = new float[markers.size + 1][2];
        for (int i = 0; i < markers.size; i++) {
            CheckpointOrderMarker marker = markers.get(i);
            Vector2 point =
                    imageToWorld(
                            marker.centerX,
                            marker.centerY,
                            imageWidth,
                            imageHeight,
                            worldMinX,
                            worldMinY,
                            worldWidth,
                            worldHeight);
            waypoints[i][0] = point.x;
            waypoints[i][1] = point.y;
        }
        waypoints[markers.size][0] = waypoints[0][0];
        waypoints[markers.size][1] = waypoints[0][1];

        Array<Vector2> directRoute = buildDirectRouteFromWaypoints(routeMap, waypoints);
        if (directRoute.size >= 4 && isRouteCenterlineSafe(routeMap, directRoute)) {
            Vector2 forward = averageSpawnForward(spawnPoints);
            alignRouteCenterlineWithSpawnDirection(directRoute, spawnPoints, forward);
            return smoothRouteCenterline(routeMap, directRoute);
        }

        route = buildRouteFromWaypoints(routeMap, waypoints, routeHints);
        if (route.size < 4) {
            return route;
        }
        Vector2 forward = averageSpawnForward(spawnPoints);
        alignRouteCenterlineWithSpawnDirection(route, spawnPoints, forward);
        return smoothRouteCenterline(routeMap, route);
    }

    private static Array<Vector2> buildDirectRouteFromWaypoints(ArenaMap routeMap, float[][] waypoints) {
        Array<Vector2> route = new Array<Vector2>();
        if (routeMap == null || waypoints == null || waypoints.length < 4) {
            return route;
        }
        int count = waypoints.length;
        if (sameWaypoint(waypoints[0], waypoints[count - 1])) {
            count--;
        }
        for (int i = 0; i < count; i++) {
            Vector2 point = new Vector2(waypoints[i][0], waypoints[i][1]);
            routeMap.clampToPlayable(point, ROUTE_CENTERLINE_MIN_CLEARANCE);
            addRoutePoint(route, point);
        }
        return route;
    }

    private static boolean sameWaypoint(float[] first, float[] second) {
        if (first == null || second == null || first.length < 2 || second.length < 2) {
            return false;
        }
        float dx = first[0] - second[0];
        float dy = first[1] - second[1];
        return dx * dx + dy * dy <= 0.01f;
    }

    private static Array<Vector2> buildRouteFromWaypoints(
            ArenaMap routeMap,
            float[][] waypoints,
            Array<RouteHintCircle> routeHints) {
        Array<Vector2> route = new Array<Vector2>();
        if (routeMap == null || waypoints == null || waypoints.length < 2) {
            return route;
        }

        Vector2 current = new Vector2(waypoints[0][0], waypoints[0][1]);
        routeMap.clampToPlayable(current, ROUTE_CENTERLINE_MIN_CLEARANCE);
        route.add(new Vector2(current));

        Vector2 goal = new Vector2();
        Vector2 nextWaypoint = new Vector2();
        Vector2 target = new Vector2();
        Vector2 delta = new Vector2();
        Vector2 previousDirection = new Vector2(1f, 0f);
        int waypointIndex = 1;
        while (waypointIndex < waypoints.length) {
            int goalIndex = waypointIndex;
            goal.set(waypoints[goalIndex][0], waypoints[goalIndex][1]);
            RouteHintCircle waypointHint = findContainingRouteHint(routeHints, goal);
            if (waypointHint == null && waypointIndex + 1 < waypoints.length) {
                nextWaypoint.set(waypoints[waypointIndex + 1][0], waypoints[waypointIndex + 1][1]);
                RouteHintCircle upcomingHint = findContainingRouteHint(routeHints, nextWaypoint);
                if (upcomingHint != null && !upcomingHint.contains(current, ROUTE_HINT_SEGMENT_MARGIN_WORLD)) {
                    int outsideIndex =
                            findNextWaypointOutsideHint(waypoints, waypointIndex + 2, upcomingHint);
                    if (outsideIndex > waypointIndex + 1) {
                        nextWaypoint.set(waypoints[outsideIndex][0], waypoints[outsideIndex][1]);
                        if (upcomingHint.findPass(current, nextWaypoint, ROUTE_HINT_SEGMENT_MARGIN_WORLD)
                                != null) {
                            waypointHint = upcomingHint;
                            goalIndex = outsideIndex;
                            goal.set(nextWaypoint);
                        }
                    }
                }
            }
            if (waypointHint != null && !waypointHint.contains(current, ROUTE_HINT_SEGMENT_MARGIN_WORLD)) {
                int outsideIndex = findNextWaypointOutsideHint(waypoints, waypointIndex + 1, waypointHint);
                if (outsideIndex > waypointIndex) {
                    goalIndex = outsideIndex;
                    goal.set(waypoints[goalIndex][0], waypoints[goalIndex][1]);
                }
            }
            routeMap.clampToPlayable(goal, ROUTE_CENTERLINE_MIN_CLEARANCE);
            RouteHintPass routeHintPass = findRouteHintPass(routeHints, current, goal);
            if (routeHintPass != null
                    && isRouteSegmentExactSafe(routeMap, current, routeHintPass.entry)
                    && isRouteSegmentExactSafe(routeMap, routeHintPass.entry, routeHintPass.exit)) {
                addDirectRouteHintSegment(route, current, routeHintPass.entry);
                addDirectRouteHintSegment(route, routeHintPass.entry, routeHintPass.exit);
                delta.set(routeHintPass.exit).sub(current);
                if (!delta.isZero(0.0001f)) {
                    previousDirection.set(delta).nor();
                }
                current.set(routeHintPass.exit);
                if (current.dst2(goal)
                        <= ROUTE_CENTERLINE_WAYPOINT_REACHED_DISTANCE
                                * ROUTE_CENTERLINE_WAYPOINT_REACHED_DISTANCE) {
                    addRoutePoint(route, goal);
                    delta.set(goal).sub(current);
                    if (!delta.isZero(0.0001f)) {
                        previousDirection.set(delta).nor();
                    }
                    current.set(goal);
                    waypointIndex = goalIndex + 1;
                    continue;
                }
            }
            for (int step = 0; step < ROUTE_CENTERLINE_WAYPOINT_MAX_STEPS; step++) {
                if (current.dst2(goal)
                        <= ROUTE_CENTERLINE_WAYPOINT_REACHED_DISTANCE
                                * ROUTE_CENTERLINE_WAYPOINT_REACHED_DISTANCE) {
                    addRoutePoint(route, goal);
                    current.set(goal);
                    break;
                }

                routeMap.findDriveTarget(current, goal, ROUTE_CENTERLINE_MIN_CLEARANCE, target);
                delta.set(target).sub(current);
                float distance = delta.len();
                if (distance <= 0.05f) {
                    target.set(goal);
                    delta.set(target).sub(current);
                    distance = delta.len();
                    if (distance <= 0.05f) {
                        break;
                    }
                }
                if (distance > ROUTE_CENTERLINE_STEP_WORLD) {
                    target.set(current).mulAdd(delta, ROUTE_CENTERLINE_STEP_WORLD / distance);
                    routeMap.clampToPlayable(target, ROUTE_CENTERLINE_MIN_CLEARANCE);
                }
                if (!isRouteSegmentExactSafe(routeMap, current, target)) {
                    if (!findExactSafeWaypointStep(
                            routeMap,
                            current,
                            goal,
                            delta,
                            previousDirection,
                            target)) {
                        break;
                    }
                    delta.set(target).sub(current);
                    distance = delta.len();
                    if (distance <= 0.05f) {
                        break;
                    }
                }
                if (!delta.isZero(0.0001f)) {
                    previousDirection.set(delta).nor();
                }
                addRoutePoint(route, target);
                current.set(target);
            }
            waypointIndex = goalIndex + 1;
        }
        return route;
    }

    private static boolean findExactSafeWaypointStep(
            ArenaMap routeMap,
            Vector2 current,
            Vector2 goal,
            Vector2 desiredDirection,
            Vector2 previousDirection,
            Vector2 out) {
        if (routeMap == null || current == null || goal == null || out == null) {
            return false;
        }

        Vector2 baseDirection = new Vector2(goal).sub(current);
        if (baseDirection.isZero(0.0001f)) {
            if (desiredDirection != null && !desiredDirection.isZero(0.0001f)) {
                baseDirection.set(desiredDirection);
            } else if (previousDirection != null && !previousDirection.isZero(0.0001f)) {
                baseDirection.set(previousDirection);
            } else {
                return false;
            }
        }
        baseDirection.nor();

        Vector2 candidate = new Vector2();
        float bestScore = -Float.MAX_VALUE;
        boolean found = false;
        float currentGoalDistance = current.dst(goal);
        float[] stepScales = {1f, 0.65f, 0.35f};
        for (int scaleIndex = 0; scaleIndex < stepScales.length; scaleIndex++) {
            float stepDistance = ROUTE_CENTERLINE_STEP_WORLD * stepScales[scaleIndex];
            for (int i = 0; i < ROUTE_CENTERLINE_FALLBACK_ANGLES_DEG.length; i++) {
                float angle = ROUTE_CENTERLINE_FALLBACK_ANGLES_DEG[i] * MathUtils.degreesToRadians;
                float cos = MathUtils.cos(angle);
                float sin = MathUtils.sin(angle);
                float dirX = baseDirection.x * cos - baseDirection.y * sin;
                float dirY = baseDirection.x * sin + baseDirection.y * cos;
                candidate.set(current.x + dirX * stepDistance, current.y + dirY * stepDistance);
                if (!isRoutePointExactSafe(routeMap, candidate)
                        || !isRouteSegmentExactSafe(routeMap, current, candidate)) {
                    continue;
                }

                float distanceToGoal = candidate.dst(goal);
                float progress = currentGoalDistance - distanceToGoal;
                float estimatedDistance =
                        routeMap.estimateDriveDistance(
                                candidate,
                                goal,
                                ROUTE_CENTERLINE_MIN_CLEARANCE);
                if (estimatedDistance < 0f) {
                    estimatedDistance = distanceToGoal;
                }
                float previousAlignment = 0f;
                if (previousDirection != null && !previousDirection.isZero(0.0001f)) {
                    previousAlignment = dirX * previousDirection.x + dirY * previousDirection.y;
                }
                float clearance =
                        Math.min(
                                2.0f,
                                routeMap.distanceToHazard(candidate.x, candidate.y));
                float turnPenalty = Math.abs(ROUTE_CENTERLINE_FALLBACK_ANGLES_DEG[i]) / 145f;
                float score =
                        -estimatedDistance
                                + progress * 1.75f
                                + previousAlignment * 1.10f
                                + clearance * 0.55f
                                - turnPenalty * 0.45f
                                - scaleIndex * 0.08f;
                if (score > bestScore) {
                    bestScore = score;
                    out.set(candidate);
                    found = true;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRouteSegmentExactSafe(ArenaMap routeMap, Vector2 start, Vector2 end) {
        if (routeMap == null || start == null || end == null) {
            return false;
        }
        if (!isRoutePointExactSafe(routeMap, start) || !isRoutePointExactSafe(routeMap, end)) {
            return false;
        }
        float distance = start.dst(end);
        int samples = Math.max(1, MathUtils.ceil(distance / ROUTE_CENTERLINE_EXACT_SAMPLE_STEP));
        for (int i = 1; i < samples; i++) {
            float alpha = i / (float) samples;
            float x = start.x + (end.x - start.x) * alpha;
            float y = start.y + (end.y - start.y) * alpha;
            if (!isRoutePointExactSafe(routeMap, x, y)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRoutePointExactSafe(ArenaMap routeMap, Vector2 point) {
        return point != null && isRoutePointExactSafe(routeMap, point.x, point.y);
    }

    private static boolean isRoutePointExactSafe(ArenaMap routeMap, float x, float y) {
        return routeMap != null
                && routeMap.supports(x, y)
                && routeMap.distanceToHazard(x, y) >= ROUTE_CENTERLINE_EXACT_MIN_CLEARANCE;
    }

    private static RouteHintCircle findContainingRouteHint(Array<RouteHintCircle> routeHints, Vector2 point) {
        if (routeHints == null || routeHints.size == 0 || point == null) {
            return null;
        }
        for (int i = 0; i < routeHints.size; i++) {
            RouteHintCircle hint = routeHints.get(i);
            if (hint.contains(point, ROUTE_HINT_SEGMENT_MARGIN_WORLD)) {
                return hint;
            }
        }
        return null;
    }

    private static int findNextWaypointOutsideHint(
            float[][] waypoints,
            int startIndex,
            RouteHintCircle hint) {
        if (waypoints == null || hint == null) {
            return -1;
        }
        Vector2 waypoint = new Vector2();
        for (int i = startIndex; i < waypoints.length; i++) {
            waypoint.set(waypoints[i][0], waypoints[i][1]);
            if (!hint.contains(waypoint, ROUTE_HINT_SEGMENT_MARGIN_WORLD)) {
                return i;
            }
        }
        return -1;
    }

    private static RouteHintPass findRouteHintPass(
            Array<RouteHintCircle> routeHints,
            Vector2 start,
            Vector2 end) {
        if (routeHints == null || routeHints.size == 0 || start == null || end == null) {
            return null;
        }
        for (int i = 0; i < routeHints.size; i++) {
            RouteHintCircle hint = routeHints.get(i);
            RouteHintPass pass = hint.findPass(start, end, ROUTE_HINT_SEGMENT_MARGIN_WORLD);
            if (pass != null) {
                return pass;
            }
        }
        return null;
    }

    private static RouteHintPass findGeneratedRouteHintPass(
            ArenaMap routeMap,
            Array<RouteHintCircle> routeHints,
            Vector2 current,
            Vector2 direction) {
        if (routeMap == null
                || routeHints == null
                || routeHints.size == 0
                || current == null
                || direction == null
                || direction.isZero(0.0001f)) {
            return null;
        }

        Vector2 probe =
                new Vector2(
                        current.x + direction.x * ROUTE_HINT_GENERATED_LOOKAHEAD_WORLD,
                        current.y + direction.y * ROUTE_HINT_GENERATED_LOOKAHEAD_WORLD);
        RouteHintPass best = null;
        float bestEntryDistance = Float.MAX_VALUE;
        for (int i = 0; i < routeHints.size; i++) {
            RouteHintCircle hint = routeHints.get(i);
            RouteHintPass pass =
                    hint.findGeneratedPass(
                            current,
                            direction,
                            ROUTE_HINT_GENERATED_LOOKAHEAD_WORLD,
                            ROUTE_HINT_SEGMENT_MARGIN_WORLD);
            if (pass == null) {
                continue;
            }

            Vector2 entry = new Vector2(pass.entry);
            Vector2 exit = new Vector2(pass.exit);
            routeMap.clampToPlayable(entry, ROUTE_CENTERLINE_EXACT_MIN_CLEARANCE);
            routeMap.clampToPlayable(exit, ROUTE_CENTERLINE_EXACT_MIN_CLEARANCE);
            pass = new RouteHintPass(entry, exit);

            float entryDistance = current.dst(pass.entry);
            if (entryDistance > ROUTE_HINT_GENERATED_ENTRY_DISTANCE_WORLD) {
                continue;
            }
            if (!isRouteSegmentExactSafe(routeMap, current, pass.entry)
                    || !isRouteSegmentExactSafe(routeMap, pass.entry, pass.exit)) {
                continue;
            }
            if (entryDistance < bestEntryDistance) {
                bestEntryDistance = entryDistance;
                best = pass;
            }
        }
        return best;
    }

    private static void addDirectRouteHintSegment(Array<Vector2> route, Vector2 start, Vector2 end) {
        float distance = start.dst(end);
        int steps = Math.max(1, MathUtils.ceil(distance / ROUTE_HINT_DIRECT_STEP_WORLD));
        Vector2 point = new Vector2();
        for (int i = 1; i <= steps; i++) {
            float alpha = i / (float) steps;
            point.set(start).lerp(end, alpha);
            addRoutePoint(route, point);
        }
    }

    private static void addRoutePoint(Array<Vector2> route, Vector2 point) {
        if (route.size == 0 || route.peek().dst2(point) > 0.04f) {
            route.add(new Vector2(point));
        }
    }

    private static Array<Vector2> finishGeneratedRouteCenterline(
            ArenaMap routeMap,
            Array<Vector2> route,
            Array<SpawnPoint> spawnPoints,
            Vector2 forward,
            Array<SpawnPoint> checkpoints,
            com.badlogic.gdx.math.Rectangle routeBounds,
            boolean pruneLocalLoops) {
        if (route.size < 8) {
            return buildRouteFromCheckpoints(checkpoints);
        }
        if (pruneLocalLoops) {
            pruneGeneratedRouteLocalLoops(routeMap, route);
        }
        if (!routeCoversEnoughBounds(route, routeBounds)) {
            return buildRouteFromCheckpoints(checkpoints);
        }
        if (!closeGeneratedRouteCenterline(routeMap, route)) {
            return buildRouteFromCheckpoints(checkpoints);
        }
        alignRouteCenterlineWithSpawnDirection(route, spawnPoints, forward);
        return smoothRouteCenterline(routeMap, route);
    }

    private static void pruneGeneratedRouteLocalLoops(ArenaMap routeMap, Array<Vector2> route) {
        if (routeMap == null || route == null || route.size < ROUTE_CENTERLINE_LOOP_PRUNE_MIN_SPAN + 2) {
            return;
        }
        float pruneDistanceSquared =
                ROUTE_CENTERLINE_LOOP_PRUNE_DISTANCE * ROUTE_CENTERLINE_LOOP_PRUNE_DISTANCE;
        for (int pass = 0; pass < ROUTE_CENTERLINE_LOOP_PRUNE_PASSES; pass++) {
            boolean changed = false;
            for (int from = 0; from < route.size - ROUTE_CENTERLINE_LOOP_PRUNE_MIN_SPAN - 1; from++) {
                int maxTo =
                        Math.min(
                                route.size - 1,
                                from + ROUTE_CENTERLINE_LOOP_PRUNE_MAX_SPAN);
                for (int to = from + ROUTE_CENTERLINE_LOOP_PRUNE_MIN_SPAN; to <= maxTo; to++) {
                    Vector2 start = route.get(from);
                    Vector2 end = route.get(to);
                    if (start.dst2(end) > pruneDistanceSquared) {
                        continue;
                    }
                    if (!isRouteSegmentExactSafe(routeMap, start, end)) {
                        continue;
                    }
                    removeRouteRange(route, from + 1, to - 1);
                    changed = true;
                    break;
                }
                if (changed) {
                    break;
                }
            }
            if (!changed) {
                return;
            }
        }
    }

    private static void removeRouteRange(Array<Vector2> route, int start, int end) {
        for (int i = end; i >= start; i--) {
            route.removeIndex(i);
        }
    }

    private static boolean closeGeneratedRouteCenterline(ArenaMap routeMap, Array<Vector2> route) {
        if (routeMap == null || route == null || route.size < 4) {
            return false;
        }

        Vector2 start = route.first();
        Vector2 current = new Vector2(route.peek());
        if (current.dst2(start)
                <= ROUTE_CENTERLINE_WAYPOINT_REACHED_DISTANCE
                        * ROUTE_CENTERLINE_WAYPOINT_REACHED_DISTANCE) {
            return isRouteSegmentExactSafe(routeMap, current, start);
        }

        Vector2 previousDirection = new Vector2(current).sub(route.get(route.size - 2));
        if (previousDirection.isZero(0.0001f)) {
            previousDirection.set(1f, 0f);
        } else {
            previousDirection.nor();
        }

        Vector2 target = new Vector2();
        Vector2 delta = new Vector2();
        int maxSteps = ROUTE_CENTERLINE_WAYPOINT_MAX_STEPS * 5;
        for (int step = 0; step < maxSteps; step++) {
            if (current.dst2(start)
                    <= ROUTE_CENTERLINE_WAYPOINT_REACHED_DISTANCE
                            * ROUTE_CENTERLINE_WAYPOINT_REACHED_DISTANCE
                    && isRouteSegmentExactSafe(routeMap, current, start)) {
                addRoutePoint(route, start);
                return true;
            }

            routeMap.findDriveTarget(current, start, ROUTE_CENTERLINE_MIN_CLEARANCE, target);
            delta.set(target).sub(current);
            float distance = delta.len();
            if (distance <= 0.05f) {
                target.set(start);
                delta.set(target).sub(current);
                distance = delta.len();
                if (distance <= 0.05f) {
                    break;
                }
            }
            if (distance > ROUTE_CENTERLINE_STEP_WORLD) {
                target.set(current).mulAdd(delta, ROUTE_CENTERLINE_STEP_WORLD / distance);
                routeMap.clampToPlayable(target, ROUTE_CENTERLINE_MIN_CLEARANCE);
            }
            if (!isRouteSegmentExactSafe(routeMap, current, target)) {
                if (!findExactSafeWaypointStep(
                        routeMap,
                        current,
                        start,
                        delta,
                        previousDirection,
                        target)) {
                    return false;
                }
                delta.set(target).sub(current);
                distance = delta.len();
                if (distance <= 0.05f) {
                    break;
                }
            }
            if (!delta.isZero(0.0001f)) {
                previousDirection.set(delta).nor();
            }
            addRoutePoint(route, target);
            current.set(target);
        }

        return current.dst2(start)
                <= ROUTE_CENTERLINE_START_CLOSE_DISTANCE
                        * ROUTE_CENTERLINE_START_CLOSE_DISTANCE
                && isRouteSegmentExactSafe(routeMap, current, start);
    }

    private static Array<Vector2> smoothRouteCenterline(ArenaMap routeMap, Array<Vector2> route) {
        if (routeMap == null || route == null || route.size < 4) {
            return route;
        }

        Array<Vector2> smoothed = copyRoutePoints(route);
        for (int pass = 0; pass < ROUTE_CENTERLINE_SMOOTHING_PASSES; pass++) {
            Array<Vector2> candidate = new Array<Vector2>(smoothed.size * 2);
            for (int i = 0; i < smoothed.size; i++) {
                Vector2 current = smoothed.get(i);
                Vector2 next = smoothed.get((i + 1) % smoothed.size);
                candidate.add(routeLerp(current, next, 0.25f));
                candidate.add(routeLerp(current, next, 0.75f));
            }
            if (!isRouteCenterlineSafe(routeMap, candidate)) {
                break;
            }
            smoothed = candidate;
        }
        return smoothed;
    }

    private static Array<Vector2> copyRoutePoints(Array<Vector2> route) {
        Array<Vector2> copy = new Array<Vector2>(route.size);
        for (int i = 0; i < route.size; i++) {
            copy.add(new Vector2(route.get(i)));
        }
        return copy;
    }

    private static Vector2 routeLerp(Vector2 first, Vector2 second, float alpha) {
        return new Vector2(
                first.x + (second.x - first.x) * alpha,
                first.y + (second.y - first.y) * alpha);
    }

    private static boolean isRouteCenterlineSafe(ArenaMap routeMap, Array<Vector2> route) {
        if (routeMap == null || route == null || route.size < 2) {
            return false;
        }
        for (int i = 0; i < route.size; i++) {
            Vector2 start = route.get(i);
            Vector2 end = route.get((i + 1) % route.size);
            if (!isRouteCenterlineSampleSafe(routeMap, start.x, start.y)) {
                return false;
            }
            float distance = start.dst(end);
            int samples =
                    Math.max(1, MathUtils.ceil(distance / ROUTE_CENTERLINE_SMOOTHING_SAMPLE_STEP));
            for (int sample = 1; sample <= samples; sample++) {
                float alpha = sample / (float) samples;
                float x = start.x + (end.x - start.x) * alpha;
                float y = start.y + (end.y - start.y) * alpha;
                if (!isRouteCenterlineSampleSafe(routeMap, x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isRouteCenterlineSampleSafe(ArenaMap routeMap, float x, float y) {
        return routeMap.approximateSupports(x, y)
                && routeMap.approximateDistanceToHazard(x, y)
                        >= ROUTE_CENTERLINE_SMOOTHING_MIN_CLEARANCE;
    }

    private static void alignRouteCenterlineWithSpawnDirection(
            Array<Vector2> route,
            Array<SpawnPoint> spawnPoints,
            Vector2 forward) {
        if (route.size < 2 || forward == null || forward.isZero(0.0001f)) {
            return;
        }
        SpawnPoint frontSpawn = findFrontSpawnPoint(spawnPoints, forward);
        if (frontSpawn == null) {
            return;
        }
        float alignment = routeTangentAlignmentNear(route, frontSpawn.x, frontSpawn.y, forward);
        if (alignment < -0.05f) {
            reverseRouteCenterline(route);
        }
    }

    private static float routeTangentAlignmentNear(
            Array<Vector2> route,
            float x,
            float y,
            Vector2 forward) {
        float bestDistance = Float.MAX_VALUE;
        float bestAlignment = 0f;
        for (int i = 0; i < route.size; i++) {
            Vector2 start = route.get(i);
            Vector2 end = route.get((i + 1) % route.size);
            float segmentX = end.x - start.x;
            float segmentY = end.y - start.y;
            float segmentLength = (float) Math.sqrt(segmentX * segmentX + segmentY * segmentY);
            if (segmentLength <= 0.0001f) {
                continue;
            }
            float distance =
                    distanceToSegmentSquared(
                            x,
                            y,
                            start.x,
                            start.y,
                            end.x,
                            end.y);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestAlignment =
                        (segmentX * forward.x + segmentY * forward.y) / segmentLength;
            }
        }
        return bestAlignment;
    }

    private static void reverseRouteCenterline(Array<Vector2> route) {
        for (int left = 0, right = route.size - 1; left < right; left++, right--) {
            Vector2 temporary = route.get(left);
            route.set(left, route.get(right));
            route.set(right, temporary);
        }
    }

    private static Vector2 averageSpawnPosition(Array<SpawnPoint> spawnPoints) {
        Vector2 average = new Vector2();
        for (int i = 0; i < spawnPoints.size; i++) {
            SpawnPoint spawnPoint = spawnPoints.get(i);
            average.x += spawnPoint.x;
            average.y += spawnPoint.y;
        }
        if (spawnPoints.size > 0) {
            average.scl(1f / spawnPoints.size);
        }
        return average;
    }

    private static void centerRoutePoint(ArenaMap routeMap, Vector2 point, Vector2 side) {
        if (routeMap == null || point == null || side == null || side.isZero(0.0001f)) {
            return;
        }
        Vector2 candidate = new Vector2();
        Vector2 best = new Vector2(point);
        float bestClearance = routeMap.approximateDistanceToHazard(point);
        float searchDistance = ROUTE_CENTERLINE_STEP_WORLD * 2.5f;
        for (int i = -8; i <= 8; i++) {
            float offset = searchDistance * i / 8f;
            candidate.set(point.x + side.x * offset, point.y + side.y * offset);
            if (!routeMap.approximateSupports(candidate)) {
                continue;
            }
            float clearance = routeMap.approximateDistanceToHazard(candidate);
            if (clearance > bestClearance) {
                bestClearance = clearance;
                best.set(candidate);
            }
        }
        point.set(best);
    }

    private static boolean centerRoutePointBetweenBorders(
            ArenaMap routeMap,
            Vector2 point,
            float sideX,
            float sideY) {
        if (routeMap == null || point == null) {
            return false;
        }
        float sideLength = (float) Math.sqrt(sideX * sideX + sideY * sideY);
        if (sideLength <= 0.0001f || !routeMap.approximateSupports(point)) {
            return false;
        }
        sideX /= sideLength;
        sideY /= sideLength;

        float positiveDistance = roadDistanceToBorder(routeMap, point, sideX, sideY);
        float negativeDistance = roadDistanceToBorder(routeMap, point, -sideX, -sideY);
        if (positiveDistance <= 0.001f || negativeDistance <= 0.001f) {
            return false;
        }

        float midpointOffset = (positiveDistance - negativeDistance) * 0.5f;
        point.add(sideX * midpointOffset, sideY * midpointOffset);
        return routeMap.approximateSupports(point);
    }

    private static float roadDistanceToBorder(
            ArenaMap routeMap,
            Vector2 start,
            float dirX,
            float dirY) {
        float lastSupported = 0f;
        Vector2 sample = new Vector2();
        for (float distance = ROUTE_CENTERLINE_BORDER_SCAN_STEP;
                distance <= ROUTE_CENTERLINE_BORDER_SCAN_MAX_DISTANCE;
                distance += ROUTE_CENTERLINE_BORDER_SCAN_STEP) {
            sample.set(start.x + dirX * distance, start.y + dirY * distance);
            if (!routeMap.approximateSupports(sample)) {
                return lastSupported;
            }
            lastSupported = distance;
        }
        return lastSupported;
    }

    private static boolean routeCoversEnoughBounds(
            Array<Vector2> route,
            com.badlogic.gdx.math.Rectangle bounds) {
        if (route == null || route.size < 4 || bounds == null) {
            return false;
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = 0; i < route.size; i++) {
            Vector2 point = route.get(i);
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
        }
        float widthCoverage = bounds.width <= 0.001f ? 1f : (maxX - minX) / bounds.width;
        float heightCoverage = bounds.height <= 0.001f ? 1f : (maxY - minY) / bounds.height;
        return widthCoverage >= ROUTE_CENTERLINE_MIN_BOUNDS_COVERAGE
                && heightCoverage >= ROUTE_CENTERLINE_MIN_BOUNDS_COVERAGE;
    }

    private static com.badlogic.gdx.math.Rectangle calculatePlayableRouteBounds(ArenaMap routeMap) {
        com.badlogic.gdx.math.Rectangle bounds =
                routeMap.getBounds(new com.badlogic.gdx.math.Rectangle());
        int samplesX =
                MathUtils.clamp(
                        MathUtils.ceil(
                                bounds.width
                                        * ROUTE_CENTERLINE_PLAYABLE_BOUNDS_SAMPLES_PER_WORLD_UNIT),
                        ROUTE_CENTERLINE_PLAYABLE_BOUNDS_MIN_SAMPLES,
                        ROUTE_CENTERLINE_PLAYABLE_BOUNDS_MAX_SAMPLES);
        int samplesY =
                MathUtils.clamp(
                        MathUtils.ceil(
                                bounds.height
                                        * ROUTE_CENTERLINE_PLAYABLE_BOUNDS_SAMPLES_PER_WORLD_UNIT),
                        ROUTE_CENTERLINE_PLAYABLE_BOUNDS_MIN_SAMPLES,
                        ROUTE_CENTERLINE_PLAYABLE_BOUNDS_MAX_SAMPLES);

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int y = 0; y < samplesY; y++) {
            float worldY = bounds.y + bounds.height * (y + 0.5f) / samplesY;
            for (int x = 0; x < samplesX; x++) {
                float worldX = bounds.x + bounds.width * (x + 0.5f) / samplesX;
                if (!routeMap.approximateSupports(worldX, worldY)) {
                    continue;
                }
                minX = Math.min(minX, worldX);
                minY = Math.min(minY, worldY);
                maxX = Math.max(maxX, worldX);
                maxY = Math.max(maxY, worldY);
            }
        }

        if (minX == Float.MAX_VALUE) {
            return bounds;
        }

        float padding =
                Math.max(
                        ROUTE_CENTERLINE_STEP_WORLD,
                        Math.min(bounds.width, bounds.height) * 0.015f);
        minX = Math.max(bounds.x, minX - padding);
        minY = Math.max(bounds.y, minY - padding);
        maxX = Math.min(bounds.x + bounds.width, maxX + padding);
        maxY = Math.min(bounds.y + bounds.height, maxY + padding);
        return new com.badlogic.gdx.math.Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private static RouteStep chooseRouteCenterlineStep(
            ArenaMap routeMap,
            Array<Vector2> route,
            Vector2 current,
            Vector2 direction,
            Vector2 start,
            float travelled,
            float minimumLoopDistance,
            com.badlogic.gdx.math.Rectangle routeBounds,
            float visitedDistanceSquared,
            float[] angleOffsetsDeg,
            Vector2 scratch) {
        RouteStep best = null;
        float bestScore = -Float.MAX_VALUE;
        for (int i = 0; i < angleOffsetsDeg.length; i++) {
            float angle = angleOffsetsDeg[i] * MathUtils.degreesToRadians;
            float cos = MathUtils.cos(angle);
            float sin = MathUtils.sin(angle);
            float dirX = direction.x * cos - direction.y * sin;
            float dirY = direction.x * sin + direction.y * cos;
            float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
            if (length <= 0.0001f) {
                continue;
            }
            dirX /= length;
            dirY /= length;
            scratch.set(
                    current.x + dirX * ROUTE_CENTERLINE_STEP_WORLD,
                    current.y + dirY * ROUTE_CENTERLINE_STEP_WORLD);
            if (!routeMap.approximateSupports(scratch)) {
                continue;
            }
            if (!centerRoutePointBetweenBorders(routeMap, scratch, -dirY, dirX)) {
                continue;
            }
            float actualDirX = scratch.x - current.x;
            float actualDirY = scratch.y - current.y;
            float actualLength = (float) Math.sqrt(actualDirX * actualDirX + actualDirY * actualDirY);
            if (actualLength <= 0.0001f) {
                continue;
            }
            actualDirX /= actualLength;
            actualDirY /= actualLength;
            float clearance = routeMap.approximateDistanceToHazard(scratch);
            if (clearance < ROUTE_CENTERLINE_MIN_CLEARANCE) {
                continue;
            }

            boolean closesLoop =
                    travelled > minimumLoopDistance
                            && routeCoversEnoughBounds(route, routeBounds)
                            && scratch.dst2(start)
                                    <= ROUTE_CENTERLINE_START_CLOSE_DISTANCE
                                            * ROUTE_CENTERLINE_START_CLOSE_DISTANCE;
            float visitedPenalty =
                    closesLoop
                            ? 0f
                            : routeVisitedPenalty(route, scratch, visitedDistanceSquared);
            if (visitedPenalty >= 100f) {
                continue;
            }

            float forwardAlignment = actualDirX * direction.x + actualDirY * direction.y;
            float turnPenalty = Math.abs(angleOffsetsDeg[i]) / 90f;
            float centerScore = Math.min(clearance, ROUTE_CENTERLINE_STEP_WORLD * 3f);
            float score =
                    centerScore * ROUTE_CENTERLINE_CLEARANCE_WEIGHT
                            + forwardAlignment * ROUTE_CENTERLINE_FORWARD_WEIGHT
                            - turnPenalty * ROUTE_CENTERLINE_TURN_WEIGHT
                            - visitedPenalty;
            if (closesLoop) {
                score += 120f;
            }
            if (score > bestScore) {
                if (best == null) {
                    best = new RouteStep();
                }
                bestScore = score;
                best.point.set(scratch);
                best.direction.set(actualDirX, actualDirY);
                best.closesLoop = closesLoop;
            }
        }
        return best;
    }

    private static float routeVisitedPenalty(
            Array<Vector2> route,
            Vector2 candidate,
            float visitedDistanceSquared) {
        int lastIndexToCheck = route.size - ROUTE_CENTERLINE_VISITED_IGNORE_STEPS;
        if (lastIndexToCheck <= 0) {
            return 0f;
        }
        float penalty = 0f;
        for (int i = 1; i < lastIndexToCheck; i++) {
            float distanceSquared = candidate.dst2(route.get(i));
            if (distanceSquared < visitedDistanceSquared * 0.36f) {
                return 100f;
            }
            if (distanceSquared < visitedDistanceSquared) {
                penalty = Math.max(
                        penalty,
                        12f * (1f - distanceSquared / visitedDistanceSquared));
            }
        }
        return penalty;
    }

    private static Array<Vector2> buildRouteFromCheckpoints(Array<SpawnPoint> checkpoints) {
        Array<Vector2> route = new Array<Vector2>();
        if (checkpoints == null) {
            return route;
        }
        for (int i = 0; i < checkpoints.size; i++) {
            SpawnPoint checkpoint = checkpoints.get(i);
            route.add(new Vector2(checkpoint.x, checkpoint.y));
        }
        return route;
    }

    private static void orderCheckpointPathByRoute(
            Array<SpawnPoint> checkpoints,
            ArenaMap routeMap) {
        if (checkpoints.size <= 2 || routeMap == null) {
            return;
        }

        float[][] routeDistances = calculateCheckpointRouteDistances(checkpoints, routeMap);
        int[] originalOrder = buildIdentityOrder(checkpoints.size);
        RouteOrderScore originalScore = scoreRouteOrder(originalOrder, routeDistances);
        int[] bestOrder = null;
        RouteOrderScore bestScore = null;
        for (int start = 0; start < checkpoints.size; start++) {
            int[] order = buildGreedyRouteOrder(routeDistances, start);
            improveRouteOrder(order, routeDistances);
            RouteOrderScore score = scoreRouteOrder(order, routeDistances);
            if (bestScore == null || score.isBetterThan(bestScore)) {
                bestScore = score;
                bestOrder = order;
            }

            order = buildCheapestInsertionRouteOrder(routeDistances, start);
            improveRouteOrder(order, routeDistances);
            score = scoreRouteOrder(order, routeDistances);
            if (bestScore == null || score.isBetterThan(bestScore)) {
                bestScore = score;
                bestOrder = order;
            }
        }

        if (bestOrder == null) {
            return;
        }

        if (DEBUG_CHECKPOINT_ORDER) {
            System.out.println(
                    "checkpoint-order map="
                            + routeMap.getId()
                            + " original="
                            + originalScore
                            + " best="
                            + bestScore
                            + " order="
                            + formatOrder(bestOrder));
        }

        Array<SpawnPoint> original = new Array<SpawnPoint>(checkpoints);
        for (int i = 0; i < checkpoints.size; i++) {
            checkpoints.set(i, original.get(bestOrder[i]));
        }
        refreshCheckpointDirections(checkpoints);
    }

    private static float[][] calculateCheckpointRouteDistances(
            Array<SpawnPoint> checkpoints,
            ArenaMap routeMap) {
        int count = checkpoints.size;
        float[][] routeDistances = new float[count][count];
        Vector2 from = new Vector2();
        Vector2 goal = new Vector2();
        for (int fromIndex = 0; fromIndex < count; fromIndex++) {
            SpawnPoint fromCheckpoint = checkpoints.get(fromIndex);
            from.set(fromCheckpoint.x, fromCheckpoint.y);
            for (int toIndex = 0; toIndex < count; toIndex++) {
                if (fromIndex == toIndex) {
                    routeDistances[fromIndex][toIndex] = 0f;
                    continue;
                }
                SpawnPoint toCheckpoint = checkpoints.get(toIndex);
                goal.set(toCheckpoint.x, toCheckpoint.y);
                routeDistances[fromIndex][toIndex] =
                        routeMap.estimateDriveDistance(
                                from,
                                goal,
                                CHECKPOINT_ORDER_ROUTE_MARGIN);
            }
        }
        return routeDistances;
    }

    private static int[] buildGreedyRouteOrder(float[][] routeDistances, int start) {
        int count = routeDistances.length;
        int[] order = new int[count];
        boolean[] used = new boolean[count];
        order[0] = start;
        used[start] = true;
        for (int orderIndex = 1; orderIndex < count; orderIndex++) {
            int current = order[orderIndex - 1];
            int bestIndex = -1;
            float bestDistance = Float.MAX_VALUE;
            for (int candidate = 0; candidate < count; candidate++) {
                if (used[candidate]) {
                    continue;
                }
                float distance = routeDistances[current][candidate];
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = candidate;
                }
            }
            if (bestIndex < 0) {
                bestIndex = 0;
            }
            order[orderIndex] = bestIndex;
            used[bestIndex] = true;
        }
        return order;
    }

    private static int[] buildIdentityOrder(int count) {
        int[] order = new int[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        return order;
    }

    private static int[] buildCheapestInsertionRouteOrder(float[][] routeDistances, int start) {
        int count = routeDistances.length;
        int[] order = new int[count];
        boolean[] used = new boolean[count];
        order[0] = start;
        used[start] = true;
        int orderSize = 1;

        while (orderSize < count) {
            int bestCandidate = -1;
            int bestInsertIndex = orderSize;
            RouteOrderScore bestScore = null;
            for (int candidate = 0; candidate < count; candidate++) {
                if (used[candidate]) {
                    continue;
                }

                for (int insertIndex = 0; insertIndex <= orderSize; insertIndex++) {
                    int[] candidateOrder = copyWithInsertedIndex(order, orderSize, candidate, insertIndex);
                    RouteOrderScore score = scoreRouteOrder(candidateOrder, orderSize + 1, routeDistances);
                    if (bestScore == null || score.isBetterThan(bestScore)) {
                        bestScore = score;
                        bestCandidate = candidate;
                        bestInsertIndex = insertIndex;
                    }
                }
            }

            if (bestCandidate < 0) {
                break;
            }

            insertIndex(order, orderSize, bestCandidate, bestInsertIndex);
            used[bestCandidate] = true;
            orderSize++;
        }

        return order;
    }

    private static void improveRouteOrder(int[] order, float[][] routeDistances) {
        if (order.length <= 3) {
            return;
        }

        boolean improved = true;
        int passes = 0;
        int maxPasses = order.length * order.length * 2;
        while (improved && passes++ < maxPasses) {
            improved = false;
            RouteOrderScore baseScore = scoreRouteOrder(order, routeDistances);
            for (int start = 0; start < order.length - 1 && !improved; start++) {
                for (int end = start + 1; end < order.length; end++) {
                    reverseOrderSegment(order, start, end);
                    RouteOrderScore candidateScore = scoreRouteOrder(order, routeDistances);
                    if (candidateScore.isBetterThan(baseScore)) {
                        improved = true;
                        break;
                    }
                    reverseOrderSegment(order, start, end);
                }
            }
            if (!improved) {
                improved = improveRouteOrderByRelocation(order, routeDistances, baseScore);
            }
        }
    }

    private static RouteOrderScore scoreRouteOrder(int[] order, float[][] routeDistances) {
        return scoreRouteOrder(order, order.length, routeDistances);
    }

    private static RouteOrderScore scoreRouteOrder(int[] order, int orderSize, float[][] routeDistances) {
        float maxSegment = 0f;
        float total = 0f;
        for (int i = 0; i < orderSize; i++) {
            float distance = routeDistances[order[i]][order[(i + 1) % orderSize]];
            maxSegment = Math.max(maxSegment, distance);
            total += distance;
        }
        return new RouteOrderScore(maxSegment, total);
    }

    private static boolean improveRouteOrderByRelocation(
            int[] order,
            float[][] routeDistances,
            RouteOrderScore baseScore) {
        for (int fromIndex = 0; fromIndex < order.length; fromIndex++) {
            for (int insertIndex = 0; insertIndex < order.length; insertIndex++) {
                int[] candidateOrder = copyWithMovedIndex(order, fromIndex, insertIndex);
                RouteOrderScore candidateScore = scoreRouteOrder(candidateOrder, routeDistances);
                if (candidateScore.isBetterThan(baseScore)) {
                    System.arraycopy(candidateOrder, 0, order, 0, order.length);
                    return true;
                }
            }
        }
        return false;
    }

    private static int[] copyWithInsertedIndex(
            int[] order,
            int orderSize,
            int value,
            int insertIndex) {
        int[] result = new int[order.length];
        int outputIndex = 0;
        for (int i = 0; i < orderSize; i++) {
            if (outputIndex == insertIndex) {
                result[outputIndex++] = value;
            }
            result[outputIndex++] = order[i];
        }
        if (outputIndex == insertIndex) {
            result[outputIndex++] = value;
        }
        return result;
    }

    private static void insertIndex(
            int[] order,
            int orderSize,
            int value,
            int insertIndex) {
        for (int i = orderSize; i > insertIndex; i--) {
            order[i] = order[i - 1];
        }
        order[insertIndex] = value;
    }

    private static int[] copyWithMovedIndex(int[] order, int fromIndex, int insertIndex) {
        int[] compact = new int[order.length - 1];
        int compactIndex = 0;
        int value = order[fromIndex];
        for (int i = 0; i < order.length; i++) {
            if (i != fromIndex) {
                compact[compactIndex++] = order[i];
            }
        }

        int[] result = new int[order.length];
        int outputIndex = 0;
        for (int i = 0; i < compact.length; i++) {
            if (outputIndex == insertIndex) {
                result[outputIndex++] = value;
            }
            result[outputIndex++] = compact[i];
        }
        if (outputIndex == insertIndex) {
            result[outputIndex++] = value;
        }
        return result;
    }

    private static String formatOrder(int[] order) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < order.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(order[i]);
        }
        return builder.append(']').toString();
    }

    private static void reverseOrderSegment(int[] order, int start, int end) {
        while (start < end) {
            int temporary = order[start];
            order[start] = order[end];
            order[end] = temporary;
            start++;
            end--;
        }
    }

    private static void alignCheckpointPathWithSpawns(
            Array<SpawnPoint> spawnPoints,
            Array<SpawnPoint> checkpoints) {
        if (spawnPoints.size == 0 || checkpoints.size <= 2) {
            return;
        }

        Vector2 forward = averageSpawnForward(spawnPoints);
        if (forward.isZero(0.0001f)) {
            return;
        }

        SpawnPoint frontSpawn = findFrontSpawnPoint(spawnPoints, forward);
        if (frontSpawn == null) {
            return;
        }

        float averageGap = averageCheckpointGap(checkpoints);
        float[] lookaheads = {
            MathUtils.clamp(averageGap * 0.30f, 8f, 18f),
            MathUtils.clamp(averageGap * 0.45f, 10f, 26f),
            MathUtils.clamp(averageGap * 0.65f, 12f, 34f),
            Math.max(12f, averageGap * 1.35f)
        };
        int forwardVotes = 0;
        int reverseVotes = 0;
        for (int i = 0; i < lookaheads.length; i++) {
            float lookahead = lookaheads[i];
            int checkpointIndex = findNearestCheckpointIndex(
                    checkpoints,
                    frontSpawn.x + forward.x * lookahead,
                    frontSpawn.y + forward.y * lookahead);
            if (checkpointIndex < 0) {
                continue;
            }

            SpawnPoint current = checkpoints.get(checkpointIndex);
            SpawnPoint next = checkpoints.get((checkpointIndex + 1) % checkpoints.size);
            SpawnPoint previous = checkpoints.get((checkpointIndex + checkpoints.size - 1) % checkpoints.size);
            float nextAlignment = checkpointDirectionAlignment(current, next, forward);
            float previousAlignment = checkpointDirectionAlignment(current, previous, forward);
            if (previousAlignment > nextAlignment + 0.10f && previousAlignment > 0.05f) {
                reverseVotes++;
            } else if (nextAlignment > previousAlignment + 0.10f && nextAlignment > 0.05f) {
                forwardVotes++;
            }
        }
        if (reverseVotes > forwardVotes) {
            reverseCheckpointPath(checkpoints);
        }
    }

    private static Vector2 averageSpawnForward(Array<SpawnPoint> spawnPoints) {
        Vector2 forward = new Vector2();
        for (int i = 0; i < spawnPoints.size; i++) {
            SpawnPoint spawnPoint = spawnPoints.get(i);
            forward.x += -MathUtils.sin(spawnPoint.angleRad);
            forward.y += MathUtils.cos(spawnPoint.angleRad);
        }
        if (!forward.isZero(0.0001f)) {
            forward.nor();
        }
        return forward;
    }

    private static SpawnPoint findFrontSpawnPoint(
            Array<SpawnPoint> spawnPoints,
            Vector2 forward) {
        SpawnPoint frontSpawn = null;
        float bestProjection = -Float.MAX_VALUE;
        for (int i = 0; i < spawnPoints.size; i++) {
            SpawnPoint spawnPoint = spawnPoints.get(i);
            float projection = spawnPoint.x * forward.x + spawnPoint.y * forward.y;
            if (frontSpawn == null || projection > bestProjection) {
                frontSpawn = spawnPoint;
                bestProjection = projection;
            }
        }
        return frontSpawn;
    }

    private static float averageCheckpointGap(Array<SpawnPoint> checkpoints) {
        float total = 0f;
        for (int i = 0; i < checkpoints.size; i++) {
            SpawnPoint current = checkpoints.get(i);
            SpawnPoint next = checkpoints.get((i + 1) % checkpoints.size);
            total += Vector2.dst(current.x, current.y, next.x, next.y);
        }
        return total / checkpoints.size;
    }

    private static int findNearestCheckpointIndex(
            Array<SpawnPoint> checkpoints,
            float x,
            float y) {
        int bestIndex = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < checkpoints.size; i++) {
            SpawnPoint checkpoint = checkpoints.get(i);
            float dx = checkpoint.x - x;
            float dy = checkpoint.y - y;
            float distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static float checkpointDirectionAlignment(
            SpawnPoint from,
            SpawnPoint to,
            Vector2 forward) {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001f) {
            return -1f;
        }
        return (dx * forward.x + dy * forward.y) / length;
    }

    private static void reverseCheckpointPath(Array<SpawnPoint> checkpoints) {
        for (int left = 0, right = checkpoints.size - 1; left < right; left++, right--) {
            SpawnPoint temporary = checkpoints.get(left);
            checkpoints.set(left, checkpoints.get(right));
            checkpoints.set(right, temporary);
        }
        refreshCheckpointDirections(checkpoints);
    }

    private static void refreshCheckpointDirections(Array<SpawnPoint> checkpoints) {
        for (int i = 0; i < checkpoints.size; i++) {
            SpawnPoint checkpoint = checkpoints.get(i);
            SpawnPoint next = checkpoints.get((i + 1) % checkpoints.size);
            checkpoints.set(
                    i,
                    SpawnPoint.facingGate(
                            checkpoint.x,
                            checkpoint.y,
                            next.x,
                            next.y,
                            checkpoint.gateStartX,
                            checkpoint.gateStartY,
                            checkpoint.gateEndX,
                            checkpoint.gateEndY));
        }
    }

    private static Array<MarkerComponent> orderSpawnMarkers(MaskParseResult parseResult) {
        Array<MarkerComponent> ordered = new Array<MarkerComponent>();
        ordered.addAll(parseResult.redMarkers);
        if (ordered.size <= 1) {
            return ordered;
        }

        final MarkerDirectionBasis basis = calculateMarkerDirectionBasis(parseResult);
        if (!basis.valid) {
            ordered.sort(new Comparator<MarkerComponent>() {
                @Override
                public int compare(MarkerComponent left, MarkerComponent right) {
                    int rowCompare = Float.compare(left.centerY, right.centerY);
                    return rowCompare != 0 ? rowCompare : Float.compare(left.centerX, right.centerX);
                }
            });
            return ordered;
        }

        ordered.sort(new Comparator<MarkerComponent>() {
            @Override
            public int compare(MarkerComponent left, MarkerComponent right) {
                float leftForward = project(left, basis.tangentX, basis.tangentY);
                float rightForward = project(right, basis.tangentX, basis.tangentY);
                int forwardCompare = Float.compare(rightForward, leftForward);
                if (forwardCompare != 0) {
                    return forwardCompare;
                }

                float leftSide = project(left, basis.sideX, basis.sideY);
                float rightSide = project(right, basis.sideX, basis.sideY);
                return Float.compare(leftSide, rightSide);
            }
        });
        return ordered;
    }

    private static Array<MarkerComponent> orderCheckpointMarkers(MaskParseResult parseResult) {
        Array<MarkerComponent> markers = parseResult.greenMarkers;
        Array<MarkerComponent> ordered = new Array<MarkerComponent>();
        if (markers.size <= 1) {
            ordered.addAll(markers);
            return ordered;
        }

        Array<MarkerComponent> manuallyOrdered = orderCheckpointMarkersByManualOrder(parseResult);
        if (manuallyOrdered.size == markers.size) {
            parseResult.checkpointOrderByBorder = true;
            return manuallyOrdered;
        }

        MarkerDirectionBasis basis = calculateMarkerDirectionBasis(parseResult);
        Array<MarkerComponent> borderOrdered = orderCheckpointMarkersByBorder(parseResult);
        if (borderOrdered.size == markers.size) {
            alignCheckpointOrderWithSpawnDirection(parseResult, basis, borderOrdered);
            alignCheckpointOrderWithSpawnGridPosition(parseResult, borderOrdered);
            parseResult.checkpointOrderByBorder = true;
            return borderOrdered;
        }

        boolean[] used = new boolean[markers.size];
        int currentIndex = findStartCheckpointIndex(parseResult, markers, basis);
        if (currentIndex < 0) {
            currentIndex = findLargestMarker(markers);
        }

        float directionX = basis.valid ? basis.tangentX : 1f;
        float directionY = basis.valid ? basis.tangentY : 0f;
        for (int step = 0; step < markers.size; step++) {
            MarkerComponent current = markers.get(currentIndex);
            ordered.add(current);
            used[currentIndex] = true;
            if (step == markers.size - 1) {
                break;
            }

            int nextIndex = findNextMarkerByDirection(current, markers, used, directionX, directionY);
            if (nextIndex < 0) {
                nextIndex = findNearestUnusedMarkerIndex(current, markers, used);
            }
            MarkerComponent next = markers.get(nextIndex);
            float dx = next.centerX - current.centerX;
            float dy = next.centerY - current.centerY;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length > 0.0001f) {
                directionX = dx / length;
                directionY = dy / length;
            }
            currentIndex = nextIndex;
        }
        alignCheckpointOrderWithSpawnDirection(parseResult, basis, ordered);
        alignCheckpointOrderWithSpawnGridPosition(parseResult, ordered);
        return ordered;
    }

    private static Array<MarkerComponent> orderCheckpointMarkersByManualOrder(MaskParseResult parseResult) {
        Array<MarkerComponent> ordered = new Array<MarkerComponent>();
        if (parseResult.checkpointOrderMarkers.size != parseResult.greenMarkers.size) {
            return ordered;
        }

        Array<CheckpointOrderMarker> orderMarkers =
                new Array<CheckpointOrderMarker>(parseResult.checkpointOrderMarkers);
        orderMarkers.sort(new Comparator<CheckpointOrderMarker>() {
            @Override
            public int compare(CheckpointOrderMarker left, CheckpointOrderMarker right) {
                return Integer.compare(left.order, right.order);
            }
        });

        boolean[] used = new boolean[parseResult.greenMarkers.size];
        int previousOrder = -1;
        float maximumDistanceSquared =
                CHECKPOINT_ORDER_MANUAL_DISTANCE_PIXELS * CHECKPOINT_ORDER_MANUAL_DISTANCE_PIXELS;
        for (int i = 0; i < orderMarkers.size; i++) {
            CheckpointOrderMarker orderMarker = orderMarkers.get(i);
            if (orderMarker.order == previousOrder) {
                ordered.clear();
                return ordered;
            }
            previousOrder = orderMarker.order;

            int markerIndex = findNearestManualOrderCheckpoint(orderMarker, parseResult.greenMarkers, used);
            if (markerIndex < 0) {
                ordered.clear();
                return ordered;
            }

            MarkerComponent marker = parseResult.greenMarkers.get(markerIndex);
            if (Vector2.dst2(orderMarker.centerX, orderMarker.centerY, marker.centerX, marker.centerY)
                    > maximumDistanceSquared) {
                ordered.clear();
                return ordered;
            }
            used[markerIndex] = true;
            ordered.add(marker);
        }

        return ordered;
    }

    private static int findNearestManualOrderCheckpoint(
            CheckpointOrderMarker orderMarker,
            Array<MarkerComponent> checkpoints,
            boolean[] used) {
        int bestIndex = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < checkpoints.size; i++) {
            if (used[i]) {
                continue;
            }
            MarkerComponent checkpoint = checkpoints.get(i);
            float distance = Vector2.dst2(
                    orderMarker.centerX,
                    orderMarker.centerY,
                    checkpoint.centerX,
                    checkpoint.centerY);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static Array<MarkerComponent> orderCheckpointMarkersByBorder(MaskParseResult parseResult) {
        Array<MarkerComponent> ordered = new Array<MarkerComponent>();
        Array<Integer> border = tracePlayableBorder(parseResult.pathPlayable, parseResult.imageWidth, parseResult.imageHeight);
        if (border.size == 0) {
            return ordered;
        }

        boolean[] used = new boolean[parseResult.greenMarkers.size];
        int lastMarkerIndex = -1;
        for (int i = 0; i < border.size; i++) {
            int point = border.get(i);
            int x = point % parseResult.imageWidth;
            int y = point / parseResult.imageWidth;
            int markerIndex = findCheckpointAtBorderPoint(x, y, parseResult.greenMarkers, used);
            if (markerIndex < 0 || markerIndex == lastMarkerIndex) {
                continue;
            }
            used[markerIndex] = true;
            ordered.add(parseResult.greenMarkers.get(markerIndex));
            lastMarkerIndex = markerIndex;
            if (ordered.size == parseResult.greenMarkers.size) {
                break;
            }
        }

        if (ordered.size != parseResult.greenMarkers.size && DEBUG_CHECKPOINT_ORDER) {
            System.out.println(
                    "checkpoint-border-order incomplete checkpoints="
                            + ordered.size
                            + "/"
                            + parseResult.greenMarkers.size);
        }
        return ordered;
    }

    private static int findCheckpointAtBorderPoint(
            int x,
            int y,
            Array<MarkerComponent> markers,
            boolean[] used) {
        int bestIndex = -1;
        float bestDistance = Float.MAX_VALUE;
        float maximumDistance = CHECKPOINT_ORDER_TRACE_DISTANCE_PIXELS * CHECKPOINT_ORDER_TRACE_DISTANCE_PIXELS;
        for (int i = 0; i < markers.size; i++) {
            if (used[i]) {
                continue;
            }
            MarkerComponent marker = markers.get(i);
            if (!marker.hasUsableGate()) {
                continue;
            }
            float distance = distanceToSegmentSquared(
                    x,
                    y,
                    marker.gateStartX,
                    marker.gateStartY,
                    marker.gateEndX,
                    marker.gateEndY);
            if (distance <= maximumDistance && distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static float distanceToSegmentSquared(
            float x,
            float y,
            float startX,
            float startY,
            float endX,
            float endY) {
        float dx = endX - startX;
        float dy = endY - startY;
        float lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 0.0001f) {
            return Vector2.dst2(x, y, startX, startY);
        }
        float t = ((x - startX) * dx + (y - startY) * dy) / lengthSquared;
        t = MathUtils.clamp(t, 0f, 1f);
        float projectionX = startX + dx * t;
        float projectionY = startY + dy * t;
        return Vector2.dst2(x, y, projectionX, projectionY);
    }

    private static Array<Integer> tracePlayableBorder(boolean[] playable, int width, int height) {
        boolean[] border = buildBorderMask(playable, width, height);
        int start = findTopLeftBorderPixel(border, width, height);
        Array<Integer> contour = new Array<Integer>();
        if (start < 0) {
            return contour;
        }

        final int[] offsetX = {-1, -1, 0, 1, 1, 1, 0, -1};
        final int[] offsetY = {0, -1, -1, -1, 0, 1, 1, 1};
        int current = start;
        int backtrack = start - 1;
        int initialBacktrack = backtrack;
        int maximumSteps = width * height * 4;
        for (int step = 0; step < maximumSteps; step++) {
            contour.add(current);
            int currentX = current % width;
            int currentY = current / width;
            int backtrackDirection = neighborDirection(
                    currentX,
                    currentY,
                    backtrack % width,
                    backtrack / width,
                    offsetX,
                    offsetY);
            if (backtrackDirection < 0) {
                backtrackDirection = 0;
            }

            int next = -1;
            int nextBacktrack = backtrack;
            for (int i = 1; i <= 8; i++) {
                int direction = (backtrackDirection + i) & 7;
                int nx = currentX + offsetX[direction];
                int ny = currentY + offsetY[direction];
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }
                int candidate = ny * width + nx;
                if (!border[candidate]) {
                    continue;
                }
                int previousDirection = (direction + 7) & 7;
                next = candidate;
                nextBacktrack = (currentY + offsetY[previousDirection]) * width + currentX + offsetX[previousDirection];
                break;
            }

            if (next < 0) {
                break;
            }
            current = next;
            backtrack = nextBacktrack;
            if (current == start && backtrack == initialBacktrack && contour.size > 1) {
                break;
            }
        }
        return contour;
    }

    private static boolean[] buildBorderMask(boolean[] playable, int width, int height) {
        boolean[] border = new boolean[playable.length];
        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int index = rowOffset + x;
                if (!playable[index]) {
                    continue;
                }
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1
                        || !playable[index - 1]
                        || !playable[index + 1]
                        || !playable[index - width]
                        || !playable[index + width]) {
                    border[index] = true;
                }
            }
        }
        return border;
    }

    private static int findTopLeftBorderPixel(boolean[] border, int width, int height) {
        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int index = rowOffset + x;
                if (border[index]) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static int neighborDirection(
            int centerX,
            int centerY,
            int neighborX,
            int neighborY,
            int[] offsetX,
            int[] offsetY) {
        int dx = neighborX - centerX;
        int dy = neighborY - centerY;
        for (int i = 0; i < offsetX.length; i++) {
            if (offsetX[i] == dx && offsetY[i] == dy) {
                return i;
            }
        }
        return -1;
    }

    private static void alignCheckpointOrderWithSpawnDirection(
            MaskParseResult parseResult,
            MarkerDirectionBasis basis,
            Array<MarkerComponent> ordered) {
        if (!basis.valid || ordered.size <= 2 || parseResult.redMarkers.size == 0) {
            return;
        }

        MarkerComponent frontSpawn = findFrontSpawnMarker(parseResult, basis);
        if (frontSpawn == null) {
            return;
        }

        float lookahead = Math.max(42f, basis.averageMarkerGap * 2.65f);
        int startIndex = findNearestMarkerToPoint(
                frontSpawn.centerX + basis.tangentX * lookahead,
                frontSpawn.centerY + basis.tangentY * lookahead,
                ordered,
                null);
        if (startIndex < 0) {
            return;
        }

        rotateMarkerOrder(ordered, startIndex);
        MarkerComponent current = ordered.get(0);
        MarkerComponent next = ordered.get(1);
        MarkerComponent previous = ordered.get(ordered.size - 1);
        float nextAlignment = checkpointDirectionAlignment(current, next, basis);
        float previousAlignment = checkpointDirectionAlignment(current, previous, basis);
        if (previousAlignment > nextAlignment + 0.10f && previousAlignment > 0.05f) {
            reverseMarkerOrderAfterFirst(ordered);
        }
    }

    private static void alignCheckpointOrderWithSpawnGridPosition(
            MaskParseResult parseResult,
            Array<MarkerComponent> ordered) {
        if (ordered.size <= 2 || parseResult.redMarkers.size == 0) {
            return;
        }

        float spawnCenterX = 0f;
        float spawnCenterY = 0f;
        for (int i = 0; i < parseResult.redMarkers.size; i++) {
            MarkerComponent marker = parseResult.redMarkers.get(i);
            spawnCenterX += marker.centerX;
            spawnCenterY += marker.centerY;
        }
        spawnCenterX /= parseResult.redMarkers.size;
        spawnCenterY /= parseResult.redMarkers.size;

        int bestSegmentIndex = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < ordered.size; i++) {
            MarkerComponent current = ordered.get(i);
            MarkerComponent next = ordered.get((i + 1) % ordered.size);
            float distance = distanceToSegmentSquared(
                    spawnCenterX,
                    spawnCenterY,
                    current.centerX,
                    current.centerY,
                    next.centerX,
                    next.centerY);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestSegmentIndex = i;
            }
        }

        if (bestSegmentIndex >= 0) {
            rotateMarkerOrder(ordered, (bestSegmentIndex + 1) % ordered.size);
        }
    }

    private static MarkerComponent findFrontSpawnMarker(
            MaskParseResult parseResult,
            MarkerDirectionBasis basis) {
        MarkerComponent frontSpawn = null;
        float bestProjection = -Float.MAX_VALUE;
        for (int i = 0; i < parseResult.redMarkers.size; i++) {
            MarkerComponent red = parseResult.redMarkers.get(i);
            float projection = project(red, basis.tangentX, basis.tangentY);
            if (frontSpawn == null || projection > bestProjection) {
                frontSpawn = red;
                bestProjection = projection;
            }
        }
        return frontSpawn;
    }

    private static float checkpointDirectionAlignment(
            MarkerComponent from,
            MarkerComponent to,
            MarkerDirectionBasis basis) {
        float dx = to.centerX - from.centerX;
        float dy = to.centerY - from.centerY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001f) {
            return -1f;
        }
        return (dx * basis.tangentX + dy * basis.tangentY) / length;
    }

    private static void rotateMarkerOrder(Array<MarkerComponent> ordered, int startIndex) {
        if (startIndex <= 0 || startIndex >= ordered.size) {
            return;
        }
        Array<MarkerComponent> original = new Array<MarkerComponent>(ordered);
        for (int i = 0; i < ordered.size; i++) {
            ordered.set(i, original.get((startIndex + i) % ordered.size));
        }
    }

    private static void reverseMarkerOrderAfterFirst(Array<MarkerComponent> ordered) {
        for (int left = 1, right = ordered.size - 1; left < right; left++, right--) {
            MarkerComponent temporary = ordered.get(left);
            ordered.set(left, ordered.get(right));
            ordered.set(right, temporary);
        }
    }

    private static int findStartCheckpointIndex(
            MaskParseResult parseResult,
            Array<MarkerComponent> checkpoints,
            MarkerDirectionBasis basis) {
        if (!basis.valid || parseResult.redMarkers.size == 0) {
            return -1;
        }

        MarkerComponent frontSpawn = findFrontSpawnMarker(parseResult, basis);
        if (frontSpawn == null) {
            return -1;
        }

        float lookahead = Math.max(42f, basis.averageMarkerGap * 2.65f);
        return findNearestMarkerToPoint(
                frontSpawn.centerX + basis.tangentX * lookahead,
                frontSpawn.centerY + basis.tangentY * lookahead,
                checkpoints,
                null);
    }

    private static int findNextMarkerByDirection(
            MarkerComponent current,
            Array<MarkerComponent> markers,
            boolean[] used,
            float directionX,
            float directionY) {
        int bestIndex = -1;
        float bestScore = Float.MAX_VALUE;
        for (int i = 0; i < markers.size; i++) {
            if (used[i]) {
                continue;
            }
            MarkerComponent candidate = markers.get(i);
            float dx = candidate.centerX - current.centerX;
            float dy = candidate.centerY - current.centerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance <= 0.0001f) {
                continue;
            }
            float unitX = dx / distance;
            float unitY = dy / distance;
            float forward = directionX * unitX + directionY * unitY;
            float sideways = Math.abs(directionX * unitY - directionY * unitX);
            float score = distance * (1f + sideways * 1.8f) - forward * distance * 0.35f;
            if (forward < -0.1f) {
                score += 100000f + distance * 12f;
            } else if (forward < 0.15f) {
                score += distance * 2.4f;
            }
            if (score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static MarkerDirectionBasis calculateMarkerDirectionBasis(MaskParseResult parseResult) {
        float sumX = 0f;
        float sumY = 0f;
        float gapSum = 0f;
        int count = 0;
        for (int i = 0; i < parseResult.redMarkers.size; i++) {
            MarkerComponent red = parseResult.redMarkers.get(i);
            int blueIndex = findNearestUnusedMarker(red, parseResult.blueMarkers, null);
            if (blueIndex < 0) {
                continue;
            }
            MarkerComponent blue = parseResult.blueMarkers.get(blueIndex);
            float dx = blue.centerX - red.centerX;
            float dy = blue.centerY - red.centerY;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length <= 0.0001f) {
                continue;
            }
            sumX += dx / length;
            sumY += dy / length;
            gapSum += length;
            count++;
        }

        float length = (float) Math.sqrt(sumX * sumX + sumY * sumY);
        if (count == 0 || length <= 0.0001f) {
            return MarkerDirectionBasis.invalid();
        }
        float tangentX = sumX / length;
        float tangentY = sumY / length;
        return new MarkerDirectionBasis(
                true,
                tangentX,
                tangentY,
                -tangentY,
                tangentX,
                gapSum / count);
    }

    private static float project(MarkerComponent marker, float axisX, float axisY) {
        return marker.centerX * axisX + marker.centerY * axisY;
    }

    private static int findLargestMarker(Array<MarkerComponent> markers) {
        int bestIndex = 0;
        int bestArea = markers.get(0).area;
        for (int i = 1; i < markers.size; i++) {
            int area = markers.get(i).area;
            if (area > bestArea) {
                bestArea = area;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static int findNearestUnusedMarkerIndex(
            MarkerComponent source,
            Array<MarkerComponent> candidates,
            boolean[] used) {
        return findNearestUnusedMarker(source, candidates, used);
    }

    private static int findNearestUnusedMarker(
            MarkerComponent source,
            Array<MarkerComponent> candidates,
            boolean[] used) {
        return findNearestMarkerToPoint(source.centerX, source.centerY, candidates, used);
    }

    private static int findSpawnDirectionMarker(
            MarkerComponent red,
            Array<MarkerComponent> blueMarkers,
            boolean[] used,
            MarkerDirectionBasis basis) {
        if (red == null || blueMarkers == null || blueMarkers.size == 0) {
            return -1;
        }
        if (basis == null || !basis.valid) {
            return findNearestUnusedMarker(red, blueMarkers, used);
        }

        int bestIndex = -1;
        float bestScore = Float.MAX_VALUE;
        float expectedGap = Math.max(1f, basis.averageMarkerGap);
        float minimumForward = Math.max(1f, expectedGap * 0.20f);
        float maximumForward = expectedGap * 2.80f;
        for (int i = 0; i < blueMarkers.size; i++) {
            if (used != null && used[i]) {
                continue;
            }
            MarkerComponent blue = blueMarkers.get(i);
            float dx = blue.centerX - red.centerX;
            float dy = blue.centerY - red.centerY;
            float forward = dx * basis.tangentX + dy * basis.tangentY;
            if (forward < minimumForward || forward > maximumForward) {
                continue;
            }

            float side = dx * basis.sideX + dy * basis.sideY;
            float gapError = forward - expectedGap;
            float distance = dx * dx + dy * dy;
            float score = side * side * 8f + gapError * gapError * 1.6f + distance * 0.02f;
            if (score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex >= 0 ? bestIndex : findNearestUnusedMarker(red, blueMarkers, used);
    }

    private static int findNearestMarkerToPoint(
            float sourceX,
            float sourceY,
            Array<MarkerComponent> candidates,
            boolean[] used) {
        int bestIndex = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < candidates.size; i++) {
            if (used != null && used[i]) {
                continue;
            }
            MarkerComponent candidate = candidates.get(i);
            float dx = candidate.centerX - sourceX;
            float dy = candidate.centerY - sourceY;
            float distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static Array<Vector2> buildRecoveryPoints(
            Array<SpawnPoint> spawnPoints,
            MaskArenaShape shape,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight,
            float mapScale) {
        Array<Vector2> recoveryPoints = new Array<Vector2>();
        for (int i = 0; i < spawnPoints.size; i++) {
            SpawnPoint spawnPoint = spawnPoints.get(i);
            recoveryPoints.add(new Vector2(spawnPoint.x, spawnPoint.y));
        }

        int rows = Math.max(RECOVERY_ROWS, Math.round(RECOVERY_ROWS * mapScale));
        int columns = Math.max(RECOVERY_COLUMNS, Math.round(RECOVERY_COLUMNS * mapScale));
        for (int row = 0; row < rows; row++) {
            float y = worldMinY + (row + 0.5f) * worldHeight / rows;
            for (int column = 0; column < columns; column++) {
                float x = worldMinX + (column + 0.5f) * worldWidth / columns;
                if (shape.depthInside(x, y) >= RECOVERY_SAFE_MARGIN) {
                    recoveryPoints.add(new Vector2(x, y));
                }
            }
        }
        return recoveryPoints;
    }

    private static void addRecoveryPoints(
            ArenaMap.Builder builder,
            Array<Vector2> recoveryPoints) {
        for (int i = 0; i < recoveryPoints.size; i++) {
            Vector2 recoveryPoint = recoveryPoints.get(i);
            builder.recoveryPoint(recoveryPoint.x, recoveryPoint.y);
        }
    }

    private static void collectMarkerComponents(
            boolean[] mask,
            int width,
            int height,
            Array<MarkerComponent> out,
            int minimumArea) {
        boolean[] visited = new boolean[mask.length];
        int[] stack = new int[mask.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int startIndex = y * width + x;
                if (!mask[startIndex] || visited[startIndex]) {
                    continue;
                }

                int stackSize = 0;
                int area = 0;
                float sumX = 0f;
                float sumY = 0f;
                double sumXX = 0.0;
                double sumYY = 0.0;
                double sumXY = 0.0;
                int minX = width;
                int minY = height;
                int maxX = 0;
                int maxY = 0;
                stack[stackSize++] = startIndex;
                visited[startIndex] = true;

                while (stackSize > 0) {
                    int index = stack[--stackSize];
                    int currentX = index % width;
                    int currentY = index / width;
                    area++;
                    sumX += currentX;
                    sumY += currentY;
                    sumXX += (double) currentX * currentX;
                    sumYY += (double) currentY * currentY;
                    sumXY += (double) currentX * currentY;
                    minX = Math.min(minX, currentX);
                    minY = Math.min(minY, currentY);
                    maxX = Math.max(maxX, currentX);
                    maxY = Math.max(maxY, currentY);

                    for (int offsetY = -1; offsetY <= 1; offsetY++) {
                        int nextY = currentY + offsetY;
                        if (nextY < 0 || nextY >= height) {
                            continue;
                        }
                        for (int offsetX = -1; offsetX <= 1; offsetX++) {
                            if (offsetX == 0 && offsetY == 0) {
                                continue;
                            }
                            int nextX = currentX + offsetX;
                            if (nextX < 0 || nextX >= width) {
                                continue;
                            }
                            int nextIndex = nextY * width + nextX;
                            if (!mask[nextIndex] || visited[nextIndex]) {
                                continue;
                            }
                            visited[nextIndex] = true;
                            stack[stackSize++] = nextIndex;
                        }
                    }
                }

                if (area >= minimumArea) {
                    float centerX = sumX / area;
                    float centerY = sumY / area;
                    float covarianceXX = (float) (sumXX / area - centerX * centerX);
                    float covarianceYY = (float) (sumYY / area - centerY * centerY);
                    float covarianceXY = (float) (sumXY / area - centerX * centerY);
                    out.add(new MarkerComponent(
                            centerX,
                            centerY,
                            area,
                            minX,
                            minY,
                            maxX,
                            maxY,
                            covarianceXX,
                            covarianceYY,
                            covarianceXY));
                }
            }
        }
    }

    private static boolean isPlayable(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        int alpha = pixel & 0xff;
        return alpha >= 96
                && ((red >= 145 && green >= 145 && blue >= 145)
                        || isCheckpointLabelPixel(red, green, blue)
                        || isCheckpointOrderMarker(red, green, blue));
    }

    private static boolean isCheckpointLabelPixel(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        int alpha = pixel & 0xff;
        return alpha >= 96 && isCheckpointLabelPixel(red, green, blue);
    }

    private static boolean isCheckpointLabelPixel(int red, int green, int blue) {
        return red >= 70
                && red <= 115
                && Math.abs(red - green) <= 3
                && Math.abs(green - blue) <= 3;
    }

    private static boolean isCheckpointOrderMarker(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        int alpha = pixel & 0xff;
        return alpha >= 96 && isCheckpointOrderMarker(red, green, blue);
    }

    private static boolean isCheckpointOrderMarker(int red, int green, int blue) {
        return red >= 245
                && blue >= 245
                && green >= 1
                && green <= 250
                && red >= green + 3
                && blue >= green + 3;
    }

    private static boolean isRouteHintMarker(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        int alpha = pixel & 0xff;
        return alpha >= 96
                && red <= 130
                && green >= 35
                && blue >= 35
                && Math.abs(green - blue) <= 24
                && green >= red + 28
                && blue >= red + 28;
    }

    private static boolean isRouteLineMarker(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        int alpha = pixel & 0xff;
        return alpha >= 96
                && red >= 210
                && green >= 95
                && green <= 190
                && blue <= 95
                && red >= green + 45
                && green >= blue + 35;
    }

    private static int checkpointOrderValue(int pixel) {
        return (pixel >>> 16) & 0xff;
    }

    private static boolean isPlayableForShape(
            Pixmap mask,
            boolean[] basePlayable,
            int x,
            int y,
            int width,
            int height) {
        int pixel = mask.getPixel(x, y);
        if (isPlayable(pixel)) {
            return true;
        }
        if (!isRedMarker(pixel)
                && !isBlueMarker(pixel)
                && !isGreenMarker(pixel)
                && !isRouteLineMarker(pixel)) {
            return false;
        }
        return hasNearbyBasePlayablePixel(basePlayable, x, y, width, height);
    }

    private static boolean hasNearbyBasePlayablePixel(
            boolean[] basePlayable,
            int x,
            int y,
            int width,
            int height) {
        return hasNearbyPlayablePixel(
                basePlayable,
                x,
                y,
                width,
                height,
                MARKER_PLAYABLE_CONTEXT_RADIUS);
    }

    private static boolean hasNearbyPlayablePixel(
            boolean[] playable,
            int x,
            int y,
            int width,
            int height,
            int radius) {
        int minX = Math.max(0, x - radius);
        int maxX = Math.min(width - 1, x + radius);
        int minY = Math.max(0, y - radius);
        int maxY = Math.min(height - 1, y + radius);
        for (int sampleY = minY; sampleY <= maxY; sampleY++) {
            int rowOffset = sampleY * width;
            for (int sampleX = minX; sampleX <= maxX; sampleX++) {
                if (playable[rowOffset + sampleX]) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRedMarker(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        int alpha = pixel & 0xff;
        return alpha >= 96 && red >= 150 && green <= 120 && blue <= 120;
    }

    private static boolean isBlueMarker(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        int alpha = pixel & 0xff;
        return alpha >= 96 && blue >= 140 && red <= 120 && green <= 170;
    }

    private static boolean isGreenMarker(int pixel) {
        int red = (pixel >>> 24) & 0xff;
        int green = (pixel >>> 16) & 0xff;
        int blue = (pixel >>> 8) & 0xff;
        int alpha = pixel & 0xff;
        return alpha >= 96 && green >= 145 && red <= 125 && blue <= 135;
    }

    private static Vector2 imageToWorld(
            float x,
            float y,
            int imageWidth,
            int imageHeight,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight) {
        return new Vector2(
                worldMinX + (x + 0.5f) * worldWidth / imageWidth,
                worldMinY + worldHeight - (y + 0.5f) * worldHeight / imageHeight);
    }

    private static String displayName(String baseName) {
        if (baseName.startsWith("map") && baseName.length() > 3) {
            return "Map " + baseName.substring(3);
        }
        return baseName;
    }

    private static int rgba8888(int red, int green, int blue) {
        return ((red & 0xff) << 24) | ((green & 0xff) << 16) | ((blue & 0xff) << 8) | 0xff;
    }

    private static String zeroPad3(int value) {
        return (value < 10 ? "00" : value < 100 ? "0" : "") + value;
    }

    private static final class MaskParseResult {
        private final boolean[] shapePlayable;
        private final int shapeWidth;
        private final int shapeHeight;
        private final boolean[] pathPlayable;
        private final boolean[] routeLinePixels;
        private final int imageWidth;
        private final int imageHeight;
        private final Array<MarkerComponent> redMarkers = new Array<MarkerComponent>();
        private final Array<MarkerComponent> blueMarkers = new Array<MarkerComponent>();
        private final Array<MarkerComponent> greenMarkers = new Array<MarkerComponent>();
        private final Array<MarkerComponent> routeHintMarkers = new Array<MarkerComponent>();
        private final Array<MarkerComponent> routeLineMarkers = new Array<MarkerComponent>();
        private final Array<CheckpointOrderMarker> checkpointOrderMarkers = new Array<CheckpointOrderMarker>();
        private boolean checkpointOrderByBorder;

        private MaskParseResult(
                boolean[] shapePlayable,
                int shapeWidth,
                int shapeHeight,
                boolean[] pathPlayable,
                boolean[] routeLinePixels,
                int imageWidth,
                int imageHeight) {
            this.shapePlayable = shapePlayable;
            this.shapeWidth = shapeWidth;
            this.shapeHeight = shapeHeight;
            this.pathPlayable = pathPlayable;
            this.routeLinePixels = routeLinePixels;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }
    }

    private static final class CheckpointOrderMarker {
        private final int order;
        private final float centerX;
        private final float centerY;

        private CheckpointOrderMarker(int order, float centerX, float centerY) {
            this.order = order;
            this.centerX = centerX;
            this.centerY = centerY;
        }
    }

    private static final class RouteLineComponent {
        private final boolean[] pixels;
        private final int count;

        private RouteLineComponent(boolean[] pixels, int count) {
            this.pixels = pixels;
            this.count = count;
        }
    }

    private static final class RouteLineWaypoint {
        private final Vector2 point;
        private final float progress;

        private RouteLineWaypoint(Vector2 point, float progress) {
            this.point = new Vector2(point);
            this.progress = progress;
        }
    }

    private static final class UnsafeRouteSample {
        private final int segmentIndex;
        private final float x;
        private final float y;
        private int fromOrangeIndex;
        private int toOrangeIndex;

        private UnsafeRouteSample(int segmentIndex, float x, float y) {
            this.segmentIndex = segmentIndex;
            this.x = x;
            this.y = y;
        }
    }

    private static final class ShapeMaskDimensions {
        private final int width;
        private final int height;

        private ShapeMaskDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    private static final class WorldSize {
        private final float width;
        private final float height;

        private WorldSize(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }

    private static final class MarkerComponent {
        private static final float MIN_GATE_PIXEL_LENGTH = 8f;

        private final float centerX;
        private final float centerY;
        @SuppressWarnings("unused")
        private final int area;
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;
        private final float gateStartX;
        private final float gateStartY;
        private final float gateEndX;
        private final float gateEndY;

        private MarkerComponent(
                float centerX,
                float centerY,
                int area,
                int minX,
                int minY,
                int maxX,
                int maxY,
                float covarianceXX,
                float covarianceYY,
                float covarianceXY) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.area = area;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;

            float axisAngle = 0.5f * MathUtils.atan2(2f * covarianceXY, covarianceXX - covarianceYY);
            float axisX = MathUtils.cos(axisAngle);
            float axisY = MathUtils.sin(axisAngle);
            if (Math.abs(covarianceXX - covarianceYY) < 0.0001f
                    && Math.abs(covarianceXY) < 0.0001f) {
                if ((maxY - minY) > (maxX - minX)) {
                    axisX = 0f;
                    axisY = 1f;
                } else {
                    axisX = 1f;
                    axisY = 0f;
                }
            }

            float minProjection = Float.MAX_VALUE;
            float maxProjection = -Float.MAX_VALUE;
            minProjection = Math.min(minProjection, projectCorner(minX, minY, centerX, centerY, axisX, axisY));
            minProjection = Math.min(minProjection, projectCorner(minX, maxY, centerX, centerY, axisX, axisY));
            minProjection = Math.min(minProjection, projectCorner(maxX, minY, centerX, centerY, axisX, axisY));
            minProjection = Math.min(minProjection, projectCorner(maxX, maxY, centerX, centerY, axisX, axisY));
            maxProjection = Math.max(maxProjection, projectCorner(minX, minY, centerX, centerY, axisX, axisY));
            maxProjection = Math.max(maxProjection, projectCorner(minX, maxY, centerX, centerY, axisX, axisY));
            maxProjection = Math.max(maxProjection, projectCorner(maxX, minY, centerX, centerY, axisX, axisY));
            maxProjection = Math.max(maxProjection, projectCorner(maxX, maxY, centerX, centerY, axisX, axisY));

            gateStartX = centerX + axisX * minProjection;
            gateStartY = centerY + axisY * minProjection;
            gateEndX = centerX + axisX * maxProjection;
            gateEndY = centerY + axisY * maxProjection;
        }

        private boolean hasUsableGate() {
            float dx = gateEndX - gateStartX;
            float dy = gateEndY - gateStartY;
            return dx * dx + dy * dy >= MIN_GATE_PIXEL_LENGTH * MIN_GATE_PIXEL_LENGTH;
        }

        private static float projectCorner(
                float x,
                float y,
                float centerX,
                float centerY,
                float axisX,
                float axisY) {
            return (x - centerX) * axisX + (y - centerY) * axisY;
        }
    }

    private static final class MarkerDirectionBasis {
        private final boolean valid;
        private final float tangentX;
        private final float tangentY;
        private final float sideX;
        private final float sideY;
        private final float averageMarkerGap;

        private MarkerDirectionBasis(
                boolean valid,
                float tangentX,
                float tangentY,
                float sideX,
                float sideY,
                float averageMarkerGap) {
            this.valid = valid;
            this.tangentX = tangentX;
            this.tangentY = tangentY;
            this.sideX = sideX;
            this.sideY = sideY;
            this.averageMarkerGap = averageMarkerGap;
        }

        private static MarkerDirectionBasis invalid() {
            return new MarkerDirectionBasis(false, 1f, 0f, 0f, 1f, 0f);
        }
    }

    private static final class CheckpointDefinition {
        private final Vector2 position;
        private final Vector2 gateStart;
        private final Vector2 gateEnd;
        private final boolean hasGate;

        private CheckpointDefinition(
                Vector2 position,
                Vector2 gateStart,
                Vector2 gateEnd,
                boolean hasGate) {
            this.position = position;
            this.gateStart = gateStart;
            this.gateEnd = gateEnd;
            this.hasGate = hasGate;
        }
    }

    private static final class RouteStep {
        private final Vector2 point = new Vector2();
        private final Vector2 direction = new Vector2();
        private boolean closesLoop;
    }

    private static final class RouteHintCircle {
        private final float centerX;
        private final float centerY;
        private final float radius;
        private final Array<Vector2> intersections = new Array<Vector2>();

        private RouteHintCircle(
                float centerX,
                float centerY,
                float radius,
                Array<Vector2> intersections) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
            if (intersections != null) {
                for (int i = 0; i < intersections.size; i++) {
                    this.intersections.add(new Vector2(intersections.get(i)));
                }
            }
        }

        private RouteHintPass findPass(Vector2 start, Vector2 end, float margin) {
            if (intersections.size < 2 || !segmentIntersects(start, end, margin)) {
                return null;
            }
            if (contains(start, margin) || contains(end, margin)) {
                return null;
            }
            Vector2 entry = null;
            float bestEntryDistance = Float.MAX_VALUE;
            for (int i = 0; i < intersections.size; i++) {
                Vector2 candidate = intersections.get(i);
                float distance = candidate.dst2(start);
                if (distance < bestEntryDistance) {
                    bestEntryDistance = distance;
                    entry = candidate;
                }
            }
            if (entry == null) {
                return null;
            }

            Vector2 exit = null;
            float bestExitDistance = -1f;
            for (int i = 0; i < intersections.size; i++) {
                Vector2 candidate = intersections.get(i);
                float distance = candidate.dst2(entry);
                if (distance > bestExitDistance) {
                    bestExitDistance = distance;
                    exit = candidate;
                }
            }
            if (exit == null || bestExitDistance <= 0.25f) {
                return null;
            }
            if (exit.dst2(end) >= entry.dst2(end) - 0.25f) {
                return null;
            }
            return new RouteHintPass(entry, exit);
        }

        private RouteHintPass findGeneratedPass(
                Vector2 start,
                Vector2 direction,
                float lookaheadDistance,
                float margin) {
            if (intersections.size < 2
                    || start == null
                    || direction == null
                    || direction.isZero(0.0001f)) {
                return null;
            }

            Vector2 approachEnd =
                    new Vector2(
                            start.x + direction.x * lookaheadDistance,
                            start.y + direction.y * lookaheadDistance);
            boolean startsInside = contains(start, margin);
            if (!startsInside && !segmentIntersects(start, approachEnd, margin)) {
                return null;
            }
            if (startsInside) {
                float toCenterX = centerX - start.x;
                float toCenterY = centerY - start.y;
                if (toCenterX * direction.x + toCenterY * direction.y <= 0f) {
                    return null;
                }
            }

            Vector2 entryPortal = nearestIntersection(start);
            if (entryPortal == null) {
                return null;
            }
            Vector2 exit = farthestIntersection(entryPortal);
            if (exit == null || exit.dst2(entryPortal) <= 0.25f) {
                return null;
            }

            Vector2 entry = startsInside ? start : entryPortal;
            return new RouteHintPass(entry, exit);
        }

        private Vector2 nearestIntersection(Vector2 point) {
            Vector2 nearest = null;
            float bestDistance = Float.MAX_VALUE;
            for (int i = 0; i < intersections.size; i++) {
                Vector2 candidate = intersections.get(i);
                float distance = candidate.dst2(point);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearest = candidate;
                }
            }
            return nearest;
        }

        private Vector2 farthestIntersection(Vector2 point) {
            Vector2 farthest = null;
            float bestDistance = -1f;
            for (int i = 0; i < intersections.size; i++) {
                Vector2 candidate = intersections.get(i);
                float distance = candidate.dst2(point);
                if (distance > bestDistance) {
                    bestDistance = distance;
                    farthest = candidate;
                }
            }
            return farthest;
        }

        private boolean contains(Vector2 point, float margin) {
            if (point == null) {
                return false;
            }
            float effectiveRadius = radius + margin;
            return Vector2.dst2(point.x, point.y, centerX, centerY)
                    <= effectiveRadius * effectiveRadius;
        }

        private boolean segmentIntersects(Vector2 start, Vector2 end, float margin) {
            float effectiveRadius = radius + margin;
            return distanceToSegmentSquared(centerX, centerY, start.x, start.y, end.x, end.y)
                    <= effectiveRadius * effectiveRadius;
        }
    }

    private static final class RouteHintPass {
        private final Vector2 entry;
        private final Vector2 exit;

        private RouteHintPass(Vector2 entry, Vector2 exit) {
            this.entry = new Vector2(entry);
            this.exit = new Vector2(exit);
        }
    }

    private static final class RouteHintPortalCandidate {
        private final int orderIndex;
        private final int portalIndex;
        private final float distanceSquared;

        private RouteHintPortalCandidate(int orderIndex, int portalIndex, float distanceSquared) {
            this.orderIndex = orderIndex;
            this.portalIndex = portalIndex;
            this.distanceSquared = distanceSquared;
        }
    }

    private static final class RouteHintPortalMarker {
        private final int orderIndex;
        private final int portalIndex;
        private final Vector2 portalPoint;

        private RouteHintPortalMarker(int orderIndex, int portalIndex, Vector2 portalPoint) {
            this.orderIndex = orderIndex;
            this.portalIndex = portalIndex;
            this.portalPoint = new Vector2(portalPoint);
        }

        private Vector2 directionFromHint(RouteHintCircle hint) {
            Vector2 direction = new Vector2(portalPoint.x - hint.centerX, portalPoint.y - hint.centerY);
            if (!direction.isZero(0.0001f)) {
                direction.nor();
            }
            return direction;
        }
    }

    private static final class AnnotationAnchor {
        private final int dotX;
        private final int dotY;
        private final int labelX;
        private final int labelY;

        private AnnotationAnchor(int dotX, int dotY, int labelX, int labelY) {
            this.dotX = dotX;
            this.dotY = dotY;
            this.labelX = labelX;
            this.labelY = labelY;
        }
    }

    private static final class RouteOrderScore {
        private final float maxSegment;
        private final float total;

        private RouteOrderScore(float maxSegment, float total) {
            this.maxSegment = maxSegment;
            this.total = total;
        }

        private boolean isBetterThan(RouteOrderScore other) {
            if (maxSegment < other.maxSegment - CHECKPOINT_ORDER_SCORE_EPSILON) {
                return true;
            }
            return Math.abs(maxSegment - other.maxSegment) <= CHECKPOINT_ORDER_SCORE_EPSILON
                    && total < other.total - CHECKPOINT_ORDER_SCORE_EPSILON;
        }

        @Override
        public String toString() {
            return "{max=" + maxSegment + ", total=" + total + "}";
        }
    }

    private static final class CachedMapData {
        private int version;
        private String baseName;
        private String surfacePath;
        private float mapScale;
        private long maskLastModified;
        private long maskLength;
        private String maskSha256;
        @SuppressWarnings("unused")
        private int imageWidth;
        @SuppressWarnings("unused")
        private int imageHeight;
        private float worldMinX;
        private float worldMinY;
        private float worldWidth;
        private float worldHeight;
        private boolean[] shapePlayable;
        private int shapeWidth;
        private int shapeHeight;
        private float[] distanceToVoidPixels;
        private float[] distanceToPlayablePixels;
        private int[] nearestPlayableX;
        private int[] nearestPlayableY;
        private int[] nearestVoidX;
        private int[] nearestVoidY;
        private float[] spawnX;
        private float[] spawnY;
        private float[] spawnAngle;
        private float[] checkpointX;
        private float[] checkpointY;
        private float[] checkpointAngle;
        private boolean[] checkpointHasGate;
        private float[] checkpointGateStartX;
        private float[] checkpointGateStartY;
        private float[] checkpointGateEndX;
        private float[] checkpointGateEndY;
        private float[] routeX;
        private float[] routeY;
        private float[] routeMarkerX;
        private float[] routeMarkerY;
        private float[] routeMarkerProgress;
        private float routeSampleStep;
        private float routeLength;
        private float[] routeSampleX;
        private float[] routeSampleY;
        private float[] routeTangentX;
        private float[] routeTangentY;
        private float[] routeCurvature;
        private float[] routeNextCornerDistance;
        private float[] routeNextCornerDirection;
        private float[] routeNextCornerSeverity;
        private float[] routeLeftClearance;
        private float[] routeRightClearance;
        private float[] routeRoadWidth;
        private int routeLookupWidth;
        private int routeLookupHeight;
        private float routeLookupMinX;
        private float routeLookupMinY;
        private float routeLookupCellWidth;
        private float routeLookupCellHeight;
        private int[] routeLookupSampleIndex;
        private float[] recoveryX;
        private float[] recoveryY;
    }
}
