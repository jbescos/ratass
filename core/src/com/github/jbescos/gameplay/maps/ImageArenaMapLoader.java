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
import java.util.Comparator;
import java.util.HashSet;

final class ImageArenaMapLoader {
    private static final String MAPS_DIRECTORY = "maps";
    private static final String MASK_SUFFIX = "_mask.png";
    private static final String IMAGE_SUFFIX = ".png";
    private static final float BASE_WORLD_HEIGHT = 18f;
    private static final int MAX_SEQUENTIAL_MAP_SCAN = 1000;
    private static final int MAX_SHAPE_MASK_LONG_SIDE = 512;
    private static final int MIN_MARKER_PIXELS = 24;
    private static final int RECOVERY_COLUMNS = 18;
    private static final int RECOVERY_ROWS = 10;
    private static final float RECOVERY_SAFE_MARGIN = 0.85f;

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
                    "No picture map masks found. Add files like assets/maps/map000_mask.png.");
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

        for (int i = 0; i < MAX_SEQUENTIAL_MAP_SCAN; i++) {
            FileHandle handle = Gdx.files.internal(
                    MAPS_DIRECTORY + "/map" + zeroPad3(i) + MASK_SUFFIX);
            if (handle.exists()) {
                addMaskFile(maskFiles, seenPaths, handle);
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
            for (int i = 0; i < spawnPoints.size; i++) {
                SpawnPoint spawnPoint = spawnPoints.get(i);
                builder.spawn(spawnPoint);
                builder.recoveryPoint(spawnPoint.x, spawnPoint.y);
            }

            addRecoveryGrid(builder, shape, worldMinX, worldMinY, worldWidth, worldHeight, mapScale);
            return builder.build();
        } finally {
            mask.dispose();
        }
    }

    private static WorldSize calculateWorldSize(int imageWidth, int imageHeight, float mapScale) {
        float worldHeight = BASE_WORLD_HEIGHT * mapScale;
        float worldWidth = worldHeight * imageWidth / (float) imageHeight;
        return new WorldSize(worldWidth, worldHeight);
    }

    private static MaskParseResult parseMask(Pixmap mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        ShapeMaskDimensions shapeDimensions = calculateShapeMaskDimensions(width, height);
        boolean[] shapePlayable = new boolean[shapeDimensions.width * shapeDimensions.height];
        boolean[] redMarkers = new boolean[width * height];
        boolean[] blueMarkers = new boolean[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = mask.getPixel(x, y);
                boolean red = isRedMarker(pixel);
                boolean blue = isBlueMarker(pixel);
                int index = y * width + x;
                redMarkers[index] = red;
                blueMarkers[index] = blue;
            }
        }

        for (int y = 0; y < shapeDimensions.height; y++) {
            int sourceY = sampleSourceCoordinate(y, shapeDimensions.height, height);
            for (int x = 0; x < shapeDimensions.width; x++) {
                int sourceX = sampleSourceCoordinate(x, shapeDimensions.width, width);
                int pixel = mask.getPixel(sourceX, sourceY);
                shapePlayable[y * shapeDimensions.width + x] = isPlayableForShape(pixel);
            }
        }

        MaskParseResult result =
                new MaskParseResult(shapePlayable, shapeDimensions.width, shapeDimensions.height);
        collectMarkerComponents(redMarkers, width, height, result.redMarkers);
        collectMarkerComponents(blueMarkers, width, height, result.blueMarkers);
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
        parseResult.redMarkers.sort(new Comparator<MarkerComponent>() {
            @Override
            public int compare(MarkerComponent left, MarkerComponent right) {
                int rowCompare = Float.compare(left.centerY, right.centerY);
                return rowCompare != 0 ? rowCompare : Float.compare(left.centerX, right.centerX);
            }
        });

        for (int i = 0; i < parseResult.redMarkers.size; i++) {
            MarkerComponent red = parseResult.redMarkers.get(i);
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

    private static int findNearestUnusedMarker(
            MarkerComponent source,
            Array<MarkerComponent> candidates,
            boolean[] used) {
        int bestIndex = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < candidates.size; i++) {
            if (used[i]) {
                continue;
            }
            MarkerComponent candidate = candidates.get(i);
            float dx = candidate.centerX - source.centerX;
            float dy = candidate.centerY - source.centerY;
            float distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static void addRecoveryGrid(
            ArenaMap.Builder builder,
            MaskArenaShape shape,
            float worldMinX,
            float worldMinY,
            float worldWidth,
            float worldHeight,
            float mapScale) {
        int rows = Math.max(RECOVERY_ROWS, Math.round(RECOVERY_ROWS * mapScale));
        int columns = Math.max(RECOVERY_COLUMNS, Math.round(RECOVERY_COLUMNS * mapScale));
        for (int row = 0; row < rows; row++) {
            float y = worldMinY + (row + 0.5f) * worldHeight / rows;
            for (int column = 0; column < columns; column++) {
                float x = worldMinX + (column + 0.5f) * worldWidth / columns;
                if (shape.depthInside(x, y) >= RECOVERY_SAFE_MARGIN) {
                    builder.recoveryPoint(x, y);
                }
            }
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
                stack[stackSize++] = startIndex;
                visited[startIndex] = true;

                while (stackSize > 0) {
                    int index = stack[--stackSize];
                    int currentX = index % width;
                    int currentY = index / width;
                    area++;
                    sumX += currentX;
                    sumY += currentY;

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
                    out.add(new MarkerComponent(sumX / area, sumY / area, area));
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

    private static boolean isPlayableForShape(int pixel) {
        return isPlayable(pixel) || isRedMarker(pixel) || isBlueMarker(pixel);
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
        private final float centerX;
        private final float centerY;
        @SuppressWarnings("unused")
        private final int area;

        private MarkerComponent(float centerX, float centerY, int area) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.area = area;
        }
    }
}
