package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.MaskArenaShape;
import com.github.jbescos.gameplay.SpawnPoint;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class ImageArenaMapLoader {
    private static final String MAPS_DIRECTORY = "maps";
    private static final String MASK_SUFFIX = "_mask.png";
    private static final String IMAGE_SUFFIX = ".png";
    private static final String CACHE_SUFFIX = ".ser";
    private static final int CACHE_VERSION = 18;
    private static final float BASE_WORLD_HEIGHT = 22f;
    private static final int MAX_SEQUENTIAL_MAP_SCAN = 1000;
    private static final int MAX_SHAPE_MASK_LONG_SIDE = 512;
    private static final int MIN_MARKER_PIXELS = 24;
    private static final int MARKER_PLAYABLE_CONTEXT_RADIUS = 10;
    private static final int CHECKPOINT_CENTER_GATE_SAMPLES = 25;
    private static final int RECOVERY_COLUMNS = 18;
    private static final int RECOVERY_ROWS = 10;
    private static final float RECOVERY_SAFE_MARGIN = 0.85f;
    private static final float CHECKPOINT_ORDER_ROUTE_MARGIN = 1.20f;
    private static final float CHECKPOINT_ORDER_SCORE_EPSILON = 0.001f;
    private static final boolean DEBUG_CHECKPOINT_ORDER =
            Boolean.getBoolean("ratass.debugCheckpointOrder");

    private ImageArenaMapLoader() {
    }

    static Array<ArenaMap> loadDefaultMaps(float mapScale) {
        if (Gdx.files == null) {
            throw new IllegalStateException("Picture maps require Gdx.files to be available.");
        }

        float scale = Math.max(0.1f, mapScale);
        Array<FileHandle> maskFiles = findMaskFiles();
        if (maskFiles.size == 0) {
            throw new IllegalStateException(
                    "No picture map masks found. Add files like assets/maps/<name>_mask.png.");
        }

        Array<ArenaMap> maps = new Array<ArenaMap>();
        for (int i = 0; i < maskFiles.size; i++) {
            maps.add(loadMap(maskFiles.get(i), scale));
        }
        return maps;
    }

    private static Array<FileHandle> findMaskFiles() {
        Array<FileHandle> maskFiles = new Array<FileHandle>();
        HashSet<String> seenPaths = new HashSet<String>();
        FileHandle directory = Gdx.files.internal(MAPS_DIRECTORY);
        if (directory.exists() && directory.isDirectory()) {
            FileHandle[] children = directory.list();
            for (int i = 0; i < children.length; i++) {
                FileHandle child = children[i];
                if (child.name().endsWith(MASK_SUFFIX)) {
                    addMaskFile(maskFiles, seenPaths, child);
                }
            }
        }

        if (maskFiles.size == 0) {
            for (int i = 0; i < MAX_SEQUENTIAL_MAP_SCAN; i++) {
                FileHandle handle = Gdx.files.internal(
                        MAPS_DIRECTORY + "/map" + zeroPad3(i) + MASK_SUFFIX);
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
        String surfacePath = MAPS_DIRECTORY + "/" + baseName + IMAGE_SUFFIX;
        FileHandle surfaceFile = Gdx.files.internal(surfacePath);
        if (!surfaceFile.exists()) {
            surfacePath = maskFile.path();
        }

        CachedMapData cachedMap = readCachedMap(maskFile, baseName, surfacePath, mapScale);
        if (cachedMap != null) {
            return buildCachedMap(cachedMap);
        }

        Pixmap mask = new Pixmap(maskFile);
        try {
            WorldSize worldSize = calculateWorldSize(mask.getWidth(), mask.getHeight(), mapScale);
            float worldWidth = worldSize.width;
            float worldHeight = worldSize.height;
            float worldMinX = -worldWidth * 0.5f;
            float worldMinY = -worldHeight * 0.5f;
            MaskParseResult parseResult = parseMask(mask);
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
                            worldWidth, worldHeight);
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
            orderCheckpointPathByRoute(checkpoints, routeOrderingMap);
            alignCheckpointPathWithSpawns(spawnPoints, checkpoints);
            for (int i = 0; i < spawnPoints.size; i++) {
                SpawnPoint spawnPoint = spawnPoints.get(i);
                builder.spawn(spawnPoint);
            }
            addRecoveryPoints(builder, recoveryPoints);
            for (int i = 0; i < checkpoints.size; i++) {
                builder.checkpoint(checkpoints.get(i));
            }

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
        for (int i = 0; i < cached.recoveryX.length; i++) {
            builder.recoveryPoint(cached.recoveryX[i], cached.recoveryY[i]);
        }
        return builder.build();
    }

    private static WorldSize calculateWorldSize(int imageWidth, int imageHeight, float mapScale) {
        float worldHeight = BASE_WORLD_HEIGHT * mapScale;
        float worldWidth = worldHeight * imageWidth / (float) imageHeight;
        return new WorldSize(worldWidth, worldHeight);
    }

    private static CachedMapData readCachedMap(
            FileHandle maskFile,
            String baseName,
            String surfacePath,
            float mapScale) {
        Array<FileHandle> cacheFiles = readableCacheFiles(maskFile, baseName);
        for (int i = 0; i < cacheFiles.size; i++) {
            FileHandle cacheFile = cacheFiles.get(i);
            if (!cacheFile.exists()) {
                continue;
            }
            ObjectInputStream input = null;
            try {
                input = openCachedMapInput(cacheFile);
                Object value = input.readObject();
                if (value instanceof CachedMapData) {
                    CachedMapData cached = (CachedMapData) value;
                    if (isCacheValid(cached, maskFile, baseName, surfacePath, mapScale)) {
                        return cached;
                    }
                }
            } catch (RuntimeException ignored) {
                // Bad caches are regenerated from the authoritative mask PNG.
            } catch (IOException ignored) {
                // Bad caches are regenerated from the authoritative mask PNG.
            } catch (ClassNotFoundException ignored) {
                // Bad caches are regenerated from the authoritative mask PNG.
            } finally {
                closeQuietly(input);
            }
        }
        return null;
    }

    private static boolean isCacheValid(
            CachedMapData cached,
            FileHandle maskFile,
            String baseName,
            String surfacePath,
            float mapScale) {
        if (cached.version != CACHE_VERSION
                || !baseName.equals(cached.baseName)
                || !surfacePath.equals(cached.surfacePath)
                || Math.abs(cached.mapScale - mapScale) > 0.0001f
                || cached.maskLength != maskFile.length()) {
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
            ObjectOutputStream output = null;
            try {
                cacheFile.parent().mkdirs();
                output = new ObjectOutputStream(new GZIPOutputStream(cacheFile.write(false)));
                output.writeObject(cached);
                return;
            } catch (RuntimeException ignored) {
                // Try the next writable cache location.
            } catch (IOException ignored) {
                // Try the next writable cache location.
            } finally {
                closeQuietly(output);
            }
        }
    }

    private static ObjectInputStream openCachedMapInput(FileHandle cacheFile) throws IOException {
        BufferedInputStream bufferedInput = new BufferedInputStream(cacheFile.read());
        bufferedInput.mark(2);
        int firstByte = bufferedInput.read();
        int secondByte = bufferedInput.read();
        bufferedInput.reset();
        InputStream input = bufferedInput;
        if (firstByte == 0x1f && secondByte == 0x8b) {
            input = new GZIPInputStream(bufferedInput);
        }
        return new ObjectInputStream(input);
    }

    private static Array<FileHandle> readableCacheFiles(FileHandle maskFile, String baseName) {
        Array<FileHandle> cacheFiles = new Array<FileHandle>();
        HashSet<String> seenPaths = new HashSet<String>();
        addCacheFile(cacheFiles, seenPaths, maskFile.sibling(baseName + CACHE_SUFFIX));
        addCacheFile(cacheFiles, seenPaths, Gdx.files.local("assets/" + MAPS_DIRECTORY + "/" + baseName + CACHE_SUFFIX));
        addCacheFile(cacheFiles, seenPaths, Gdx.files.local(MAPS_DIRECTORY + "/" + baseName + CACHE_SUFFIX));
        return cacheFiles;
    }

    private static Array<FileHandle> writableCacheFiles(FileHandle maskFile, String baseName) {
        Array<FileHandle> cacheFiles = new Array<FileHandle>();
        HashSet<String> seenPaths = new HashSet<String>();
        addCacheFile(cacheFiles, seenPaths, Gdx.files.local("assets/" + MAPS_DIRECTORY + "/" + baseName + CACHE_SUFFIX));
        addCacheFile(cacheFiles, seenPaths, Gdx.files.local(MAPS_DIRECTORY + "/" + baseName + CACHE_SUFFIX));
        addCacheFile(cacheFiles, seenPaths, maskFile.sibling(baseName + CACHE_SUFFIX));
        return cacheFiles;
    }

    private static void addCacheFile(
            Array<FileHandle> cacheFiles,
            HashSet<String> seenPaths,
            FileHandle cacheFile) {
        if (cacheFile != null && seenPaths.add(cacheFile.type() + ":" + cacheFile.path())) {
            cacheFiles.add(cacheFile);
        }
    }

    private static void closeQuietly(ObjectInputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    private static void closeQuietly(ObjectOutputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
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
            Array<Vector2> recoveryPoints) {
        CachedMapData cached = new CachedMapData();
        cached.version = CACHE_VERSION;
        cached.baseName = baseName;
        cached.surfacePath = surfacePath;
        cached.mapScale = mapScale;
        cached.maskLastModified = maskFile.lastModified();
        cached.maskLength = maskFile.length();
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
        cached.recoveryX = new float[recoveryPoints.size];
        cached.recoveryY = new float[recoveryPoints.size];
        for (int i = 0; i < recoveryPoints.size; i++) {
            Vector2 recoveryPoint = recoveryPoints.get(i);
            cached.recoveryX[i] = recoveryPoint.x;
            cached.recoveryY[i] = recoveryPoint.y;
        }
        return cached;
    }

    private static MaskParseResult parseMask(Pixmap mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        ShapeMaskDimensions shapeDimensions = calculateShapeMaskDimensions(width, height);
        boolean[] shapePlayable = new boolean[shapeDimensions.width * shapeDimensions.height];
        boolean[] redMarkers = new boolean[width * height];
        boolean[] blueMarkers = new boolean[width * height];
        boolean[] greenMarkers = new boolean[width * height];
        boolean[] basePlayable = new boolean[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = mask.getPixel(x, y);
                boolean red = isRedMarker(pixel);
                boolean blue = isBlueMarker(pixel);
                boolean green = isGreenMarker(pixel);
                int index = y * width + x;
                redMarkers[index] = red;
                blueMarkers[index] = blue;
                greenMarkers[index] = green;
                basePlayable[index] = isPlayable(pixel);
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
                new MaskParseResult(shapePlayable, shapeDimensions.width, shapeDimensions.height);
        collectMarkerComponents(redMarkers, width, height, result.redMarkers);
        collectMarkerComponents(blueMarkers, width, height, result.blueMarkers);
        collectMarkerComponents(greenMarkers, width, height, result.greenMarkers);
        return result;
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
            float worldHeight) {
        Array<SpawnPoint> spawns = new Array<SpawnPoint>();
        boolean[] blueUsed = new boolean[parseResult.blueMarkers.size];
        Array<MarkerComponent> orderedRedMarkers = orderSpawnMarkers(parseResult);

        for (int i = 0; i < orderedRedMarkers.size; i++) {
            MarkerComponent red = orderedRedMarkers.get(i);
            int blueIndex = findNearestUnusedMarker(red, parseResult.blueMarkers, blueUsed);
            Vector2 redWorld = imageToWorld(red.centerX, red.centerY, imageWidth, imageHeight,
                    worldMinX, worldMinY, worldWidth, worldHeight);
            if (blueIndex >= 0) {
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
            throw new IllegalStateException("Picture map mask requires green race checkpoint markers.");
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
                definitions.add(new CheckpointDefinition(
                        chooseGateCheckpointCenter(markerPosition, gateStart, gateEnd, shape),
                        gateStart,
                        gateEnd,
                        true));
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

        MarkerDirectionBasis basis = calculateMarkerDirectionBasis(parseResult);
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
        return ordered;
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

        MarkerComponent current = ordered.get(startIndex);
        MarkerComponent next = ordered.get((startIndex + 1) % ordered.size);
        MarkerComponent previous = ordered.get((startIndex + ordered.size - 1) % ordered.size);
        float nextAlignment = checkpointDirectionAlignment(current, next, basis);
        float previousAlignment = checkpointDirectionAlignment(current, previous, basis);
        if (previousAlignment > nextAlignment + 0.10f && previousAlignment > 0.05f) {
            reverseMarkerOrder(ordered);
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

    private static void reverseMarkerOrder(Array<MarkerComponent> ordered) {
        for (int left = 0, right = ordered.size - 1; left < right; left++, right--) {
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
            Array<MarkerComponent> out) {
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

                if (area >= MIN_MARKER_PIXELS) {
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
        return alpha >= 96 && red >= 145 && green >= 145 && blue >= 145;
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
        if (!isRedMarker(pixel) && !isBlueMarker(pixel) && !isGreenMarker(pixel)) {
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
        int minX = Math.max(0, x - MARKER_PLAYABLE_CONTEXT_RADIUS);
        int maxX = Math.min(width - 1, x + MARKER_PLAYABLE_CONTEXT_RADIUS);
        int minY = Math.max(0, y - MARKER_PLAYABLE_CONTEXT_RADIUS);
        int maxY = Math.min(height - 1, y + MARKER_PLAYABLE_CONTEXT_RADIUS);
        for (int sampleY = minY; sampleY <= maxY; sampleY++) {
            int rowOffset = sampleY * width;
            for (int sampleX = minX; sampleX <= maxX; sampleX++) {
                if (basePlayable[rowOffset + sampleX]) {
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

    private static String zeroPad3(int value) {
        return (value < 10 ? "00" : value < 100 ? "0" : "") + value;
    }

    private static final class MaskParseResult {
        private final boolean[] shapePlayable;
        private final int shapeWidth;
        private final int shapeHeight;
        private final Array<MarkerComponent> redMarkers = new Array<MarkerComponent>();
        private final Array<MarkerComponent> blueMarkers = new Array<MarkerComponent>();
        private final Array<MarkerComponent> greenMarkers = new Array<MarkerComponent>();

        private MaskParseResult(boolean[] shapePlayable, int shapeWidth, int shapeHeight) {
            this.shapePlayable = shapePlayable;
            this.shapeWidth = shapeWidth;
            this.shapeHeight = shapeHeight;
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

    private static final class CachedMapData implements Serializable {
        private static final long serialVersionUID = 1L;

        private int version;
        private String baseName;
        private String surfacePath;
        private float mapScale;
        private long maskLastModified;
        private long maskLength;
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
        private float[] recoveryX;
        private float[] recoveryY;
    }
}
