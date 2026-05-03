package com.github.jbescos.gameplay;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public final class MaskArenaShape extends ArenaShape {
    private static final float MAX_DISTANCE = 1_000_000_000f;
    private static final int RENDER_COLUMNS = 192;

    private final int maskWidth;
    private final int maskHeight;
    private final boolean[] playable;
    private final float[] distanceToVoidPixels;
    private final float[] distanceToPlayablePixels;
    private final int[] nearestPlayableX;
    private final int[] nearestPlayableY;
    private final int[] nearestVoidX;
    private final int[] nearestVoidY;
    private final Array<RenderSpan> renderSpans;
    private final float minX;
    private final float minY;
    private final float width;
    private final float height;
    private final float maxX;
    private final float maxY;
    private final float worldUnitsPerPixel;

    public MaskArenaShape(boolean[] playable, int maskWidth, int maskHeight,
            float minX, float minY, float width, float height) {
        if (playable == null || playable.length != maskWidth * maskHeight) {
            throw new IllegalArgumentException("Mask shape requires one playable flag per pixel.");
        }
        if (maskWidth <= 0 || maskHeight <= 0 || width <= 0f || height <= 0f) {
            throw new IllegalArgumentException("Mask shape dimensions must be positive.");
        }

        this.playable = playable;
        this.maskWidth = maskWidth;
        this.maskHeight = maskHeight;
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
        this.maxX = minX + width;
        this.maxY = minY + height;
        this.worldUnitsPerPixel = Math.min(width / maskWidth, height / maskHeight);

        distanceToVoidPixels = new float[playable.length];
        distanceToPlayablePixels = new float[playable.length];
        nearestPlayableX = new int[playable.length];
        nearestPlayableY = new int[playable.length];
        nearestVoidX = new int[playable.length];
        nearestVoidY = new int[playable.length];

        buildDistanceField(false, distanceToVoidPixels, nearestVoidX, nearestVoidY);
        buildDistanceField(true, distanceToPlayablePixels, nearestPlayableX, nearestPlayableY);
        renderSpans = buildRenderSpans();
    }

    private MaskArenaShape(
            boolean[] playable,
            int maskWidth,
            int maskHeight,
            float[] distanceToVoidPixels,
            float[] distanceToPlayablePixels,
            int[] nearestPlayableX,
            int[] nearestPlayableY,
            int[] nearestVoidX,
            int[] nearestVoidY,
            Array<RenderSpan> renderSpans,
            float minX,
            float minY,
            float width,
            float height) {
        this.playable = playable;
        this.maskWidth = maskWidth;
        this.maskHeight = maskHeight;
        this.distanceToVoidPixels = distanceToVoidPixels;
        this.distanceToPlayablePixels = distanceToPlayablePixels;
        this.nearestPlayableX = nearestPlayableX;
        this.nearestPlayableY = nearestPlayableY;
        this.nearestVoidX = nearestVoidX;
        this.nearestVoidY = nearestVoidY;
        this.renderSpans = renderSpans;
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
        this.maxX = minX + width;
        this.maxY = minY + height;
        this.worldUnitsPerPixel = Math.min(width / maskWidth, height / maskHeight);
    }

    @Override
    public boolean contains(float x, float y) {
        if (!isInsideWorldBounds(x, y)) {
            return false;
        }
        return playable[pixelIndex(worldToPixelX(x), worldToPixelY(y))];
    }

    @Override
    public float depthInside(float x, float y) {
        if (!isInsideWorldBounds(x, y)) {
            return 0f;
        }
        int index = pixelIndex(worldToPixelX(x), worldToPixelY(y));
        if (!playable[index]) {
            return 0f;
        }
        return distanceToVoidPixels[index] * worldUnitsPerPixel;
    }

    @Override
    public float distanceOutside(float x, float y) {
        float outsideBoundsDistance = distanceOutsideWorldBounds(x, y);
        float clampedX = MathUtils.clamp(x, minX, maxX);
        float clampedY = MathUtils.clamp(y, minY, maxY);
        int index = pixelIndex(worldToPixelX(clampedX), worldToPixelY(clampedY));
        if (outsideBoundsDistance <= 0f && playable[index]) {
            return 0f;
        }
        return outsideBoundsDistance + distanceToPlayablePixels[index] * worldUnitsPerPixel;
    }

    @Override
    public void closestPointInside(float x, float y, float margin, Vector2 out) {
        float clampedX = MathUtils.clamp(x, minX, maxX);
        float clampedY = MathUtils.clamp(y, minY, maxY);
        int index = pixelIndex(worldToPixelX(clampedX), worldToPixelY(clampedY));
        if (playable[index] && depthInside(clampedX, clampedY) >= margin) {
            out.set(clampedX, clampedY);
            return;
        }

        int nearestX = nearestPlayableX[index];
        int nearestY = nearestPlayableY[index];
        if (nearestX < 0 || nearestY < 0) {
            out.set((minX + maxX) * 0.5f, (minY + maxY) * 0.5f);
            return;
        }

        out.set(pixelCenterWorldX(nearestX), pixelCenterWorldY(nearestY));
    }

    @Override
    public void closestPointOutside(float x, float y, float margin, Vector2 out) {
        float clampedX = MathUtils.clamp(x, minX, maxX);
        float clampedY = MathUtils.clamp(y, minY, maxY);
        int index = pixelIndex(worldToPixelX(clampedX), worldToPixelY(clampedY));
        if (!playable[index]) {
            out.set(clampedX, clampedY);
            return;
        }

        int nearestX = nearestVoidX[index];
        int nearestY = nearestVoidY[index];
        if (nearestX < 0 || nearestY < 0) {
            out.set(clampedX, clampedY);
            return;
        }

        out.set(pixelCenterWorldX(nearestX), pixelCenterWorldY(nearestY));
    }

    @Override
    public void draw(ShapeRenderer shapeRenderer, float offsetX, float offsetY, float expansion) {
        for (int i = 0; i < renderSpans.size; i++) {
            RenderSpan span = renderSpans.get(i);
            shapeRenderer.rect(
                    span.x + offsetX - expansion,
                    span.y + offsetY - expansion,
                    span.width + expansion * 2f,
                    span.height + expansion * 2f);
        }
    }

    @Override
    public ArenaShape scale(float factor) {
        if (factor <= 0f) {
            throw new IllegalArgumentException("Mask shape scale must be positive.");
        }
        if (MathUtils.isEqual(factor, 1f)) {
            return this;
        }
        return new MaskArenaShape(
                playable,
                maskWidth,
                maskHeight,
                distanceToVoidPixels,
                distanceToPlayablePixels,
                nearestPlayableX,
                nearestPlayableY,
                nearestVoidX,
                nearestVoidY,
                buildScaledRenderSpans(factor),
                minX * factor,
                minY * factor,
                width * factor,
                height * factor);
    }

    @Override
    public float getMinX() {
        return minX;
    }

    @Override
    public float getMaxX() {
        return maxX;
    }

    @Override
    public float getMinY() {
        return minY;
    }

    @Override
    public float getMaxY() {
        return maxY;
    }

    private void buildDistanceField(
            boolean targetPlayable,
            float[] distances,
            int[] nearestX,
            int[] nearestY) {
        for (int y = 0; y < maskHeight; y++) {
            for (int x = 0; x < maskWidth; x++) {
                int index = pixelIndex(x, y);
                if (playable[index] == targetPlayable) {
                    distances[index] = 0f;
                    nearestX[index] = x;
                    nearestY[index] = y;
                } else {
                    distances[index] = MAX_DISTANCE;
                    nearestX[index] = -1;
                    nearestY[index] = -1;
                }
            }
        }

        for (int y = 0; y < maskHeight; y++) {
            for (int x = 0; x < maskWidth; x++) {
                relaxDistance(x, y, x - 1, y, distances, nearestX, nearestY);
                relaxDistance(x, y, x, y - 1, distances, nearestX, nearestY);
                relaxDistance(x, y, x - 1, y - 1, distances, nearestX, nearestY);
                relaxDistance(x, y, x + 1, y - 1, distances, nearestX, nearestY);
            }
        }

        for (int y = maskHeight - 1; y >= 0; y--) {
            for (int x = maskWidth - 1; x >= 0; x--) {
                relaxDistance(x, y, x + 1, y, distances, nearestX, nearestY);
                relaxDistance(x, y, x, y + 1, distances, nearestX, nearestY);
                relaxDistance(x, y, x + 1, y + 1, distances, nearestX, nearestY);
                relaxDistance(x, y, x - 1, y + 1, distances, nearestX, nearestY);
            }
        }

        for (int i = 0; i < distances.length; i++) {
            if (distances[i] >= MAX_DISTANCE) {
                distances[i] = 0f;
            } else {
                distances[i] = (float) Math.sqrt(distances[i]);
            }
        }
    }

    private void relaxDistance(
            int x,
            int y,
            int neighborX,
            int neighborY,
            float[] distances,
            int[] nearestX,
            int[] nearestY) {
        if (neighborX < 0 || neighborX >= maskWidth || neighborY < 0 || neighborY >= maskHeight) {
            return;
        }

        int neighborIndex = pixelIndex(neighborX, neighborY);
        int sourceX = nearestX[neighborIndex];
        int sourceY = nearestY[neighborIndex];
        if (sourceX < 0 || sourceY < 0) {
            return;
        }

        float dx = x - sourceX;
        float dy = y - sourceY;
        float candidate = dx * dx + dy * dy;
        int index = pixelIndex(x, y);
        if (candidate < distances[index]) {
            distances[index] = candidate;
            nearestX[index] = sourceX;
            nearestY[index] = sourceY;
        }
    }

    private Array<RenderSpan> buildRenderSpans() {
        int columns = Math.min(RENDER_COLUMNS, maskWidth);
        int rows = Math.max(1, Math.round(columns * maskHeight / (float) maskWidth));
        float cellWidth = width / columns;
        float cellHeight = height / rows;
        Array<RenderSpan> spans = new Array<RenderSpan>();

        for (int row = 0; row < rows; row++) {
            int start = -1;
            for (int column = 0; column <= columns; column++) {
                boolean supported = false;
                if (column < columns) {
                    int sampleX = MathUtils.clamp(
                            Math.round((column + 0.5f) * maskWidth / columns - 0.5f),
                            0,
                            maskWidth - 1);
                    int sampleY = MathUtils.clamp(
                            Math.round((row + 0.5f) * maskHeight / rows - 0.5f),
                            0,
                            maskHeight - 1);
                    supported = playable[pixelIndex(sampleX, sampleY)];
                }

                if (supported && start < 0) {
                    start = column;
                } else if (!supported && start >= 0) {
                    float spanX = minX + start * cellWidth;
                    float spanY = minY + (rows - row - 1) * cellHeight;
                    spans.add(new RenderSpan(
                            spanX,
                            spanY,
                            (column - start) * cellWidth,
                            cellHeight));
                    start = -1;
                }
            }
        }

        return spans;
    }

    private Array<RenderSpan> buildScaledRenderSpans(float factor) {
        Array<RenderSpan> scaled = new Array<RenderSpan>(renderSpans.size);
        for (int i = 0; i < renderSpans.size; i++) {
            RenderSpan span = renderSpans.get(i);
            scaled.add(new RenderSpan(
                    span.x * factor,
                    span.y * factor,
                    span.width * factor,
                    span.height * factor));
        }
        return scaled;
    }

    private boolean isInsideWorldBounds(float x, float y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    private float distanceOutsideWorldBounds(float x, float y) {
        float dx = 0f;
        if (x < minX) {
            dx = minX - x;
        } else if (x > maxX) {
            dx = x - maxX;
        }

        float dy = 0f;
        if (y < minY) {
            dy = minY - y;
        } else if (y > maxY) {
            dy = y - maxY;
        }

        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private int worldToPixelX(float x) {
        return MathUtils.clamp(
                (int) ((x - minX) * maskWidth / width),
                0,
                maskWidth - 1);
    }

    private int worldToPixelY(float y) {
        return MathUtils.clamp(
                (int) ((maxY - y) * maskHeight / height),
                0,
                maskHeight - 1);
    }

    private float pixelCenterWorldX(int pixelX) {
        return minX + (pixelX + 0.5f) * width / maskWidth;
    }

    private float pixelCenterWorldY(int pixelY) {
        return maxY - (pixelY + 0.5f) * height / maskHeight;
    }

    private int pixelIndex(int x, int y) {
        return y * maskWidth + x;
    }

    private static final class RenderSpan {
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        private RenderSpan(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
