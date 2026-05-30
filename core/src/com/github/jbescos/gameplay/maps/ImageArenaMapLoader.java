package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
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
    private static final String TRAINING_MAPS_DIRECTORY = "tools/rl/trainingMaps";
    private static final String MASK_SUFFIX = "_mask.png";
    private static final String IMAGE_SUFFIX = ".png";
    private static final String CACHE_SUFFIX = ".ser";
    private static final int CACHE_VERSION = 26;
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
    private static final int CHECKPOINT_ORDER_TRACE_DISTANCE_PIXELS = 8;
    private static final int CHECKPOINT_ORDER_DOT_RADIUS = 4;
    private static final int CHECKPOINT_ORDER_LABEL_SCALE = 3;
    private static final int CHECKPOINT_ORDER_LABEL_GAP = 9;
    private static final int CHECKPOINT_ORDER_LABEL_COLOR = rgba8888(88, 88, 88);
    private static final int CHECKPOINT_ORDER_MARKER_MIN_PIXELS = 8;
    private static final int CHECKPOINT_ORDER_MANUAL_DISTANCE_PIXELS = 120;
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
            WorldSize worldSize = calculateWorldSize(mask.getWidth(), mask.getHeight(), mapScale);
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
            if (!parseResult.checkpointOrderByBorder) {
                orderCheckpointPathByRoute(checkpoints, routeOrderingMap);
            }
            alignCheckpointPathWithSpawns(spawnPoints, checkpoints);
            writeCheckpointOrderAnnotations(maskFile, mask, checkpoints, worldMinX, worldMinY, worldWidth, worldHeight);
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
            float worldHeight) {
        if (!canWriteMaskAnnotations(maskFile) || checkpoints.size == 0) {
            return;
        }

        removeCheckpointOrderAnnotations(mask);
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
            PixmapIO.writePNG(maskFile, mask);
        } catch (RuntimeException ignored) {
            // Some packaged/internal file handles are intentionally not writable.
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
        boolean[] pathPlayable = new boolean[width * height];

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

        for (int y = 0; y < height; y++) {
            int rowOffset = y * width;
            for (int x = 0; x < width; x++) {
                int index = rowOffset + x;
                pathPlayable[index] =
                        basePlayable[index]
                                || ((redMarkers[index] || blueMarkers[index] || greenMarkers[index])
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
                        width,
                        height);
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
        return red >= 245 && blue >= 245 && green >= 1 && green <= 250;
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
        private final int imageWidth;
        private final int imageHeight;
        private final Array<MarkerComponent> redMarkers = new Array<MarkerComponent>();
        private final Array<MarkerComponent> blueMarkers = new Array<MarkerComponent>();
        private final Array<MarkerComponent> greenMarkers = new Array<MarkerComponent>();
        private final Array<CheckpointOrderMarker> checkpointOrderMarkers = new Array<CheckpointOrderMarker>();
        private boolean checkpointOrderByBorder;

        private MaskParseResult(
                boolean[] shapePlayable,
                int shapeWidth,
                int shapeHeight,
                boolean[] pathPlayable,
                int imageWidth,
                int imageHeight) {
            this.shapePlayable = shapePlayable;
            this.shapeWidth = shapeWidth;
            this.shapeHeight = shapeHeight;
            this.pathPlayable = pathPlayable;
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
