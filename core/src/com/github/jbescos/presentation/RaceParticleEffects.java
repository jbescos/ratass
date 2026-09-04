package com.github.jbescos.presentation;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntMap;
import java.util.Arrays;

/** Bounded, deterministic particle state used only by the rendered game. */
public final class RaceParticleEffects {
    static final int MAX_PARTICLES = 480;

    private static final int SMOKE = 1;
    private static final int SPARK = 2;
    private static final int FLASH = 3;
    private static final int SURFACE_DEBRIS = 4;
    private static final float DRIFT_START_SLIP = 0.34f;
    private static final float DRIFT_STOP_SLIP = 0.27f;
    private static final float MIN_DRIFT_SPEED_RATIO = 0.16f;
    private static final float IMPACT_COOLDOWN = 0.055f;

    private final boolean[] active = new boolean[MAX_PARTICLES];
    private final int[] kind = new int[MAX_PARTICLES];
    private final float[] x = new float[MAX_PARTICLES];
    private final float[] y = new float[MAX_PARTICLES];
    private final float[] velocityX = new float[MAX_PARTICLES];
    private final float[] velocityY = new float[MAX_PARTICLES];
    private final float[] age = new float[MAX_PARTICLES];
    private final float[] lifetime = new float[MAX_PARTICLES];
    private final float[] startSize = new float[MAX_PARTICLES];
    private final float[] endSize = new float[MAX_PARTICLES];
    private final float[] red = new float[MAX_PARTICLES];
    private final float[] green = new float[MAX_PARTICLES];
    private final float[] blue = new float[MAX_PARTICLES];
    private final float[] opacity = new float[MAX_PARTICLES];
    private final IntMap<EmitterState> emitters = new IntMap<EmitterState>();

    private int nextSlot;
    private int variationSequence;
    private float impactCooldown;

    public void reset() {
        Arrays.fill(active, false);
        emitters.clear();
        nextSlot = 0;
        variationSequence = 0;
        impactCooldown = 0f;
    }

    public void update(float deltaSeconds) {
        float delta = sanitizeDelta(deltaSeconds);
        if (delta <= 0f) {
            return;
        }
        impactCooldown = Math.max(0f, impactCooldown - delta);
        for (int i = 0; i < active.length; i++) {
            if (!active[i]) {
                continue;
            }
            age[i] += delta;
            if (age[i] >= lifetime[i]) {
                active[i] = false;
                continue;
            }

            x[i] += velocityX[i] * delta;
            y[i] += velocityY[i] * delta;
            if (kind[i] == SMOKE) {
                float damping = (float) Math.exp(-2.1f * delta);
                velocityX[i] *= damping;
                velocityY[i] *= damping;
            } else if (kind[i] == SPARK) {
                float damping = (float) Math.exp(-3.6f * delta);
                velocityX[i] *= damping;
                velocityY[i] *= damping;
            } else if (kind[i] == SURFACE_DEBRIS) {
                float damping = (float) Math.exp(-1.6f * delta);
                velocityX[i] *= damping;
                velocityY[i] *= damping;
            }
        }
    }

    public void updateDriftEmitter(
            int emitterId,
            float deltaSeconds,
            float centerX,
            float centerY,
            float angleRad,
            float carWidth,
            float carHeight,
            float velocityX,
            float velocityY,
            float speedRatio,
            float slip) {
        EmitterState state = emitters.get(emitterId);
        boolean drifting =
                state != null && state.drifting
                        ? slip >= DRIFT_STOP_SLIP
                        : slip >= DRIFT_START_SLIP;
        if (!drifting || speedRatio < MIN_DRIFT_SPEED_RATIO) {
            stopDriftEmitter(emitterId);
            return;
        }
        if (state == null) {
            state = new EmitterState();
            emitters.put(emitterId, state);
        }
        state.drifting = true;
        state.smokeTimer -= sanitizeDelta(deltaSeconds);
        if (state.smokeTimer > 0f) {
            return;
        }

        float severity =
                MathUtils.clamp(
                        (slip - DRIFT_STOP_SLIP) / (0.78f - DRIFT_STOP_SLIP),
                        0f,
                        1f);
        float interval = MathUtils.lerp(0.115f, 0.040f, severity);
        state.smokeTimer += interval;
        emitTireSmoke(
                emitterId,
                centerX,
                centerY,
                angleRad,
                carWidth,
                carHeight,
                velocityX,
                velocityY,
                severity);
    }

    public void stopEmitter(int emitterId) {
        EmitterState state = emitters.get(emitterId);
        if (state != null) {
            state.drifting = false;
            state.smokeTimer = 0f;
            state.surfaceTimer = 0f;
        }
    }

    public void updateOffRoadEmitter(
            int emitterId,
            float deltaSeconds,
            float centerX,
            float centerY,
            float angleRad,
            float carWidth,
            float carHeight,
            float velocityX,
            float velocityY,
            float speedRatio,
            boolean offRoad) {
        EmitterState state = emitters.get(emitterId);
        if (!offRoad || speedRatio < 0.08f) {
            if (state != null) {
                state.surfaceTimer = 0f;
            }
            return;
        }
        if (state == null) {
            state = new EmitterState();
            emitters.put(emitterId, state);
        }
        state.surfaceTimer -= sanitizeDelta(deltaSeconds);
        if (state.surfaceTimer > 0f) {
            return;
        }

        float intensity = MathUtils.clamp((speedRatio - 0.08f) / 0.62f, 0f, 1f);
        state.surfaceTimer += MathUtils.lerp(0.105f, 0.040f, intensity);
        emitSurfaceDebris(
                emitterId,
                centerX,
                centerY,
                angleRad,
                carWidth,
                carHeight,
                velocityX,
                velocityY,
                intensity);
    }

    public void emitImpact(
            float centerX,
            float centerY,
            float normalX,
            float normalY,
            float impactStrength,
            float colorRed,
            float colorGreen,
            float colorBlue) {
        if (impactCooldown > 0f) {
            return;
        }
        float severity = MathUtils.clamp((impactStrength - 2f) / 30f, 0f, 1f);
        int sparkCount = 6 + Math.round(severity * 12f);
        float normalLength = (float) Math.sqrt(normalX * normalX + normalY * normalY);
        if (normalLength <= 0.0001f) {
            normalX = 0f;
            normalY = 1f;
        } else {
            normalX /= normalLength;
            normalY /= normalLength;
        }
        float tangentX = -normalY;
        float tangentY = normalX;

        for (int i = 0; i < sparkCount; i++) {
            int slot = allocate(SPARK);
            float spread = signedNoise(variationSequence + i * 19);
            float side = (i & 1) == 0 ? -1f : 1f;
            float speed = MathUtils.lerp(2.2f, 6.8f, severity)
                    * (0.72f + noise01(variationSequence + i * 31) * 0.42f);
            x[slot] = centerX + tangentX * spread * 0.10f;
            y[slot] = centerY + tangentY * spread * 0.10f;
            velocityX[slot] =
                    (normalX * (0.22f + Math.abs(spread) * 0.38f)
                                    + tangentX * side * (0.55f + Math.abs(spread) * 0.65f))
                            * speed;
            velocityY[slot] =
                    (normalY * (0.22f + Math.abs(spread) * 0.38f)
                                    + tangentY * side * (0.55f + Math.abs(spread) * 0.65f))
                            * speed;
            lifetime[slot] = 0.22f + noise01(variationSequence + i * 43) * 0.24f;
            startSize[slot] = 0.075f + severity * 0.045f;
            endSize[slot] = 0.018f;
            red[slot] = MathUtils.lerp(1f, colorRed, 0.18f);
            green[slot] = MathUtils.lerp(0.84f, colorGreen, 0.18f);
            blue[slot] = MathUtils.lerp(0.24f, colorBlue, 0.12f);
            opacity[slot] = 0.92f;
        }

        int flash = allocate(FLASH);
        x[flash] = centerX;
        y[flash] = centerY;
        lifetime[flash] = 0.16f;
        startSize[flash] = 0.18f;
        endSize[flash] = 0.74f + severity * 0.42f;
        red[flash] = 1f;
        green[flash] = 0.78f;
        blue[flash] = 0.26f;
        opacity[flash] = 0.58f;

        variationSequence += sparkCount * 47 + 1;
        impactCooldown = IMPACT_COOLDOWN;
    }

    public void drawSmoke(ShapeRenderer renderer) {
        for (int i = 0; i < active.length; i++) {
            if (!active[i] || kind[i] != SMOKE) {
                continue;
            }
            float progress = normalizedAge(i);
            float fadeIn = MathUtils.clamp(progress / 0.14f, 0f, 1f);
            float alpha = opacity[i] * fadeIn * (1f - progress) * (1f - progress);
            float radius = MathUtils.lerp(startSize[i], endSize[i], progress);
            renderer.setColor(red[i], green[i], blue[i], alpha * 0.34f);
            renderer.circle(x[i], y[i], radius * 1.32f, 12);
            renderer.setColor(red[i], green[i], blue[i], alpha);
            renderer.circle(x[i], y[i], radius, 12);
        }
    }

    public void drawSparks(ShapeRenderer renderer) {
        for (int i = 0; i < active.length; i++) {
            if (!active[i] || (kind[i] != SPARK && kind[i] != FLASH)) {
                continue;
            }
            float progress = normalizedAge(i);
            float alpha = opacity[i] * (1f - progress) * (1f - progress);
            float size = MathUtils.lerp(startSize[i], endSize[i], progress);
            if (kind[i] == FLASH) {
                drawRing(
                        renderer,
                        x[i],
                        y[i],
                        size * 1.45f,
                        Math.max(0.012f, size * 0.16f),
                        18,
                        red[i],
                        green[i],
                        blue[i],
                        alpha * 0.24f);
                drawRing(
                        renderer,
                        x[i],
                        y[i],
                        size * 0.48f,
                        Math.max(0.012f, size * 0.14f),
                        14,
                        1f,
                        0.95f,
                        0.72f,
                        alpha * 0.72f);
                continue;
            }

            float speed = (float) Math.sqrt(
                    velocityX[i] * velocityX[i] + velocityY[i] * velocityY[i]);
            float tailScale = speed > 0.001f ? Math.min(0.12f, 0.045f + speed * 0.010f) : 0f;
            renderer.setColor(red[i], green[i], blue[i], alpha * 0.56f);
            renderer.rectLine(
                    x[i],
                    y[i],
                    x[i] - velocityX[i] * tailScale,
                    y[i] - velocityY[i] * tailScale,
                    size * 1.55f);
            drawRing(
                    renderer,
                    x[i],
                    y[i],
                    size,
                    Math.max(0.010f, size * 0.32f),
                    7,
                    1f,
                    0.95f,
                    0.70f,
                    alpha);
        }
    }

    public void drawSurfaceDebris(ShapeRenderer renderer) {
        for (int i = 0; i < active.length; i++) {
            if (!active[i] || kind[i] != SURFACE_DEBRIS) {
                continue;
            }
            float progress = normalizedAge(i);
            float alpha = opacity[i] * (1f - progress) * (1f - progress);
            float size = MathUtils.lerp(startSize[i], endSize[i], progress);
            float speed = (float) Math.sqrt(
                    velocityX[i] * velocityX[i] + velocityY[i] * velocityY[i]);
            float tail = speed > 0.001f ? Math.min(0.065f, 0.018f + speed * 0.006f) : 0f;
            renderer.setColor(red[i], green[i], blue[i], alpha);
            renderer.rectLine(
                    x[i],
                    y[i],
                    x[i] - velocityX[i] * tail,
                    y[i] - velocityY[i] * tail,
                    Math.max(0.018f, size));
            renderer.circle(x[i], y[i], Math.max(0.012f, size * 0.72f), 6);
        }
    }

    private static void drawRing(
            ShapeRenderer renderer,
            float centerX,
            float centerY,
            float radius,
            float width,
            int segments,
            float red,
            float green,
            float blue,
            float alpha) {
        renderer.setColor(red, green, blue, alpha);
        float previousX = centerX + radius;
        float previousY = centerY;
        for (int segment = 1; segment <= segments; segment++) {
            float angle = MathUtils.PI2 * segment / segments;
            float currentX = centerX + MathUtils.cos(angle) * radius;
            float currentY = centerY + MathUtils.sin(angle) * radius;
            renderer.rectLine(previousX, previousY, currentX, currentY, width);
            previousX = currentX;
            previousY = currentY;
        }
    }

    int getActiveCount() {
        int count = 0;
        for (boolean value : active) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    int getSmokeCount() {
        return countKind(SMOKE);
    }

    int getSparkCount() {
        return countKind(SPARK);
    }

    int getFlashCount() {
        return countKind(FLASH);
    }

    int getSurfaceDebrisCount() {
        return countKind(SURFACE_DEBRIS);
    }

    private void emitTireSmoke(
            int emitterId,
            float centerX,
            float centerY,
            float angleRad,
            float carWidth,
            float carHeight,
            float carVelocityX,
            float carVelocityY,
            float severity) {
        float forwardX = -MathUtils.sin(angleRad);
        float forwardY = MathUtils.cos(angleRad);
        float sideX = MathUtils.cos(angleRad);
        float sideY = MathUtils.sin(angleRad);
        float rearX = centerX - forwardX * carHeight * 0.38f;
        float rearY = centerY - forwardY * carHeight * 0.38f;
        for (int side = -1; side <= 1; side += 2) {
            int slot = allocate(SMOKE);
            int seed = variationSequence + emitterId * 53 + side * 17;
            float lateralNoise = signedNoise(seed) * carWidth * 0.09f;
            float wheelOffset = side * carWidth * 0.31f + lateralNoise;
            x[slot] = rearX + sideX * wheelOffset;
            y[slot] = rearY + sideY * wheelOffset;
            velocityX[slot] =
                    carVelocityX * 0.055f
                            - forwardX * (0.22f + severity * 0.22f)
                            + sideX * signedNoise(seed + 7) * 0.18f;
            velocityY[slot] =
                    carVelocityY * 0.055f
                            - forwardY * (0.22f + severity * 0.22f)
                            + sideY * signedNoise(seed + 7) * 0.18f;
            lifetime[slot] = 0.58f + severity * 0.38f + noise01(seed + 13) * 0.16f;
            startSize[slot] = carWidth * (0.075f + severity * 0.025f);
            endSize[slot] = carWidth * (0.30f + severity * 0.12f);
            float shade = 0.48f + noise01(seed + 29) * 0.10f;
            red[slot] = shade;
            green[slot] = shade * 1.01f;
            blue[slot] = shade * 1.04f;
            opacity[slot] = 0.18f + severity * 0.20f;
            variationSequence++;
        }
    }

    private void emitSurfaceDebris(
            int emitterId,
            float centerX,
            float centerY,
            float angleRad,
            float carWidth,
            float carHeight,
            float carVelocityX,
            float carVelocityY,
            float intensity) {
        float forwardX = -MathUtils.sin(angleRad);
        float forwardY = MathUtils.cos(angleRad);
        float sideX = MathUtils.cos(angleRad);
        float sideY = MathUtils.sin(angleRad);
        float rearX = centerX - forwardX * carHeight * 0.40f;
        float rearY = centerY - forwardY * carHeight * 0.40f;
        int countPerWheel = intensity > 0.55f ? 3 : 2;
        for (int side = -1; side <= 1; side += 2) {
            for (int piece = 0; piece < countPerWheel; piece++) {
                int slot = allocate(SURFACE_DEBRIS);
                int seed = variationSequence + emitterId * 71 + side * 23 + piece * 41;
                float wheelOffset = side * carWidth * 0.32f;
                x[slot] = rearX + sideX * (wheelOffset + signedNoise(seed) * carWidth * 0.08f);
                y[slot] = rearY + sideY * (wheelOffset + signedNoise(seed) * carWidth * 0.08f);
                float sideBurst = side * (0.35f + intensity * 0.55f)
                        + signedNoise(seed + 5) * 0.28f;
                float rearBurst = 0.48f + intensity * 1.15f + noise01(seed + 11) * 0.40f;
                velocityX[slot] =
                        carVelocityX * 0.10f - forwardX * rearBurst + sideX * sideBurst;
                velocityY[slot] =
                        carVelocityY * 0.10f - forwardY * rearBurst + sideY * sideBurst;
                lifetime[slot] = 0.32f + intensity * 0.34f + noise01(seed + 17) * 0.18f;
                startSize[slot] = carWidth * (0.030f + noise01(seed + 29) * 0.025f);
                endSize[slot] = startSize[slot] * 0.38f;
                boolean grass = ((seed >>> 1) & 3) == 0;
                if (grass) {
                    red[slot] = 0.23f + noise01(seed + 31) * 0.08f;
                    green[slot] = 0.40f + noise01(seed + 37) * 0.12f;
                    blue[slot] = 0.12f;
                } else {
                    red[slot] = 0.34f + noise01(seed + 31) * 0.12f;
                    green[slot] = 0.23f + noise01(seed + 37) * 0.09f;
                    blue[slot] = 0.10f + noise01(seed + 43) * 0.04f;
                }
                opacity[slot] = 0.72f + intensity * 0.20f;
                variationSequence++;
            }
        }
    }

    private void stopDriftEmitter(int emitterId) {
        EmitterState state = emitters.get(emitterId);
        if (state != null) {
            state.drifting = false;
            state.smokeTimer = 0f;
        }
    }

    private int allocate(int particleKind) {
        int slot = nextSlot;
        nextSlot = (nextSlot + 1) % active.length;
        active[slot] = true;
        kind[slot] = particleKind;
        x[slot] = 0f;
        y[slot] = 0f;
        velocityX[slot] = 0f;
        velocityY[slot] = 0f;
        age[slot] = 0f;
        lifetime[slot] = 1f;
        startSize[slot] = 0.1f;
        endSize[slot] = 0.1f;
        red[slot] = 1f;
        green[slot] = 1f;
        blue[slot] = 1f;
        opacity[slot] = 1f;
        return slot;
    }

    private int countKind(int particleKind) {
        int count = 0;
        for (int i = 0; i < active.length; i++) {
            if (active[i] && kind[i] == particleKind) {
                count++;
            }
        }
        return count;
    }

    private float normalizedAge(int index) {
        return lifetime[index] <= 0f
                ? 1f
                : MathUtils.clamp(age[index] / lifetime[index], 0f, 1f);
    }

    private static float sanitizeDelta(float deltaSeconds) {
        if (deltaSeconds <= 0f
                || Float.isNaN(deltaSeconds)
                || Float.isInfinite(deltaSeconds)) {
            return 0f;
        }
        return Math.min(deltaSeconds, 1f);
    }

    private static float signedNoise(int seed) {
        return noise01(seed) * 2f - 1f;
    }

    private static float noise01(int seed) {
        int value = seed;
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        value ^= value >>> 16;
        return (value & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private static final class EmitterState {
        private boolean drifting;
        private float smokeTimer;
        private float surfaceTimer;
    }
}
