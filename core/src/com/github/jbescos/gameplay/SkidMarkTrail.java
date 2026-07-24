package com.github.jbescos.gameplay;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

/**
 * Collects short rear-tire segments from multiple cars and renders them as one bounded trail.
 */
public final class SkidMarkTrail {
    static final float LIFETIME = 14f;

    private static final float START_SLIP = 0.32f;
    private static final float STOP_SLIP = 0.25f;
    private static final float MIN_SPEED_RATIO = 0.18f;
    private static final float SAMPLE_INTERVAL = 1f / 30f;
    private static final float REAR_AXLE_OFFSET = 0.34f;
    private static final float WHEEL_OFFSET = 0.34f;
    private static final int MAX_SEGMENTS = 5000;
    private static final int TRIM_SEGMENTS = 256;

    private final Array<Segment> segments = new Array<Segment>(false, 512);
    private final IntMap<EmitterState> emitters = new IntMap<EmitterState>();

    public void updateEmitter(
            int emitterId,
            float delta,
            float now,
            float centerX,
            float centerY,
            float angleRad,
            float carWidth,
            float carHeight,
            float speedRatio,
            float slip) {
        EmitterState state = emitters.get(emitterId);
        boolean slipping =
                state != null && state.tracking ? slip >= STOP_SLIP : slip >= START_SLIP;
        if (!slipping || speedRatio < MIN_SPEED_RATIO) {
            stopEmitter(emitterId);
            return;
        }
        if (state == null) {
            state = new EmitterState();
            emitters.put(emitterId, state);
        }

        float forwardX = -MathUtils.sin(angleRad);
        float forwardY = MathUtils.cos(angleRad);
        float sideX = MathUtils.cos(angleRad);
        float sideY = MathUtils.sin(angleRad);
        float rearOffset = carHeight * REAR_AXLE_OFFSET;
        float wheelOffset = carWidth * WHEEL_OFFSET;
        float rearX = centerX - forwardX * rearOffset;
        float rearY = centerY - forwardY * rearOffset;
        float leftX = rearX - sideX * wheelOffset;
        float leftY = rearY - sideY * wheelOffset;
        float rightX = rearX + sideX * wheelOffset;
        float rightY = rearY + sideY * wheelOffset;

        if (!state.tracking) {
            state.tracking = true;
            state.sampleTimer = 0f;
            state.setPosition(leftX, leftY, rightX, rightY);
            return;
        }

        state.sampleTimer += delta;
        if (state.sampleTimer < SAMPLE_INTERVAL) {
            return;
        }
        state.sampleTimer = 0f;

        float leftDistanceSquared =
                distanceSquared(state.leftX, state.leftY, leftX, leftY);
        float rightDistanceSquared =
                distanceSquared(state.rightX, state.rightY, rightX, rightY);
        float maxContinuousDistance = carHeight * 4f;
        if (leftDistanceSquared > maxContinuousDistance * maxContinuousDistance
                || rightDistanceSquared > maxContinuousDistance * maxContinuousDistance) {
            state.setPosition(leftX, leftY, rightX, rightY);
            return;
        }

        float minimumDistance = Math.max(0.02f, carHeight * 0.015f);
        if (leftDistanceSquared < minimumDistance * minimumDistance
                && rightDistanceSquared < minimumDistance * minimumDistance) {
            return;
        }

        ensureCapacity();
        Segment segment = new Segment();
        segment.leftStartX = state.leftX;
        segment.leftStartY = state.leftY;
        segment.leftEndX = leftX;
        segment.leftEndY = leftY;
        segment.rightStartX = state.rightX;
        segment.rightStartY = state.rightY;
        segment.rightEndX = rightX;
        segment.rightEndY = rightY;
        segment.width = Math.max(0.055f, carWidth * 0.075f);
        segment.intensity = calculateIntensity(speedRatio, slip);
        segment.createdAt = now;
        segments.add(segment);
        state.setPosition(leftX, leftY, rightX, rightY);
    }

    public void stopEmitter(int emitterId) {
        EmitterState state = emitters.get(emitterId);
        if (state != null) {
            state.tracking = false;
            state.sampleTimer = 0f;
        }
    }

    public void prune(float now) {
        int expiredCount = 0;
        while (expiredCount < segments.size
                && now - segments.get(expiredCount).createdAt >= LIFETIME) {
            expiredCount++;
        }
        if (expiredCount > 0) {
            segments.removeRange(0, expiredCount - 1);
        }
    }

    public void draw(ShapeRenderer renderer, float now) {
        for (int i = 0; i < segments.size; i++) {
            Segment segment = segments.get(i);
            float age = Math.max(0f, now - segment.createdAt);
            float remaining = MathUtils.clamp(1f - age / LIFETIME, 0f, 1f);
            if (remaining <= 0f) {
                continue;
            }

            float alpha = 0.58f * segment.intensity * remaining * remaining;
            renderer.setColor(0.01f, 0.012f, 0.014f, alpha);
            renderer.rectLine(
                    segment.leftStartX,
                    segment.leftStartY,
                    segment.leftEndX,
                    segment.leftEndY,
                    segment.width);
            renderer.rectLine(
                    segment.rightStartX,
                    segment.rightStartY,
                    segment.rightEndX,
                    segment.rightEndY,
                    segment.width);
        }
    }

    public void clear() {
        segments.clear();
        emitters.clear();
    }

    int getSegmentCount() {
        return segments.size;
    }

    private void ensureCapacity() {
        if (segments.size < MAX_SEGMENTS) {
            return;
        }
        int trimCount = Math.min(TRIM_SEGMENTS, segments.size);
        segments.removeRange(0, trimCount - 1);
    }

    private static float calculateIntensity(float speedRatio, float slip) {
        float slipIntensity =
                MathUtils.clamp((slip - STOP_SLIP) / (0.72f - STOP_SLIP), 0f, 1f);
        float speedIntensity =
                MathUtils.clamp(
                        (speedRatio - MIN_SPEED_RATIO) / (0.55f - MIN_SPEED_RATIO),
                        0f,
                        1f);
        return MathUtils.lerp(0.42f, 1f, slipIntensity)
                * MathUtils.lerp(0.72f, 1f, speedIntensity);
    }

    private static float distanceSquared(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    private static final class EmitterState {
        private boolean tracking;
        private float sampleTimer;
        private float leftX;
        private float leftY;
        private float rightX;
        private float rightY;

        private void setPosition(float leftX, float leftY, float rightX, float rightY) {
            this.leftX = leftX;
            this.leftY = leftY;
            this.rightX = rightX;
            this.rightY = rightY;
        }
    }

    private static final class Segment {
        private float leftStartX;
        private float leftStartY;
        private float leftEndX;
        private float leftEndY;
        private float rightStartX;
        private float rightStartY;
        private float rightEndX;
        private float rightEndY;
        private float width;
        private float intensity;
        private float createdAt;
    }
}
