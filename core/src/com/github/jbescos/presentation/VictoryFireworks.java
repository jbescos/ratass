package com.github.jbescos.presentation;

import java.util.Arrays;

/** Deterministic, rendering-agnostic particle state for the victory overlay. */
public final class VictoryFireworks {
    public static final int MAX_PARTICLES = 144;
    static final int PARTICLES_PER_BURST = 24;

    private static final float BURST_INTERVAL_SECONDS = 0.68f;
    private static final float GRAVITY = 0.22f;
    private static final float[] BURST_X = {0.18f, 0.80f, 0.34f, 0.68f, 0.50f};
    private static final float[] BURST_Y = {0.72f, 0.76f, 0.84f, 0.69f, 0.80f};

    private final boolean[] active = new boolean[MAX_PARTICLES];
    private final float[] x = new float[MAX_PARTICLES];
    private final float[] y = new float[MAX_PARTICLES];
    private final float[] velocityX = new float[MAX_PARTICLES];
    private final float[] velocityY = new float[MAX_PARTICLES];
    private final float[] age = new float[MAX_PARTICLES];
    private final float[] lifetime = new float[MAX_PARTICLES];
    private final float[] size = new float[MAX_PARTICLES];
    private final int[] colorIndex = new int[MAX_PARTICLES];

    private float burstCountdown;
    private int launchedBurstCount;

    public VictoryFireworks() {
        reset();
    }

    public void reset() {
        Arrays.fill(active, false);
        burstCountdown = BURST_INTERVAL_SECONDS;
        launchedBurstCount = 0;
        launchBurst();
    }

    public void update(float deltaSeconds) {
        if (deltaSeconds <= 0f
                || Float.isNaN(deltaSeconds)
                || Float.isInfinite(deltaSeconds)) {
            return;
        }

        float remaining = Math.min(deltaSeconds, 1f);
        while (remaining > 0f) {
            float step = Math.min(remaining, 1f / 30f);
            updateParticles(step);
            burstCountdown -= step;
            if (burstCountdown <= 0f) {
                launchBurst();
                burstCountdown += BURST_INTERVAL_SECONDS;
            }
            remaining -= step;
        }
    }

    public int getCapacity() {
        return MAX_PARTICLES;
    }

    public int getActiveCount() {
        int count = 0;
        for (int i = 0; i < active.length; i++) {
            if (active[i]) {
                count++;
            }
        }
        return count;
    }

    public int getLaunchedBurstCount() {
        return launchedBurstCount;
    }

    public boolean isActive(int index) {
        return active[index];
    }

    public float getX(int index) {
        return x[index];
    }

    public float getY(int index) {
        return y[index];
    }

    public float getAlpha(int index) {
        if (!active[index] || lifetime[index] <= 0f) {
            return 0f;
        }
        return Math.max(
                0f,
                Math.min(1f, (1f - age[index] / lifetime[index]) * 1.8f));
    }

    public float getSize(int index) {
        return size[index];
    }

    public int getColorIndex(int index) {
        return colorIndex[index];
    }

    private void updateParticles(float deltaSeconds) {
        for (int i = 0; i < active.length; i++) {
            if (!active[i]) {
                continue;
            }
            age[i] += deltaSeconds;
            if (age[i] >= lifetime[i]) {
                active[i] = false;
                continue;
            }
            x[i] += velocityX[i] * deltaSeconds;
            y[i] += velocityY[i] * deltaSeconds;
            velocityY[i] -= GRAVITY * deltaSeconds;
        }
    }

    private void launchBurst() {
        int burst = launchedBurstCount;
        float centerX = BURST_X[burst % BURST_X.length];
        float centerY = BURST_Y[burst % BURST_Y.length];
        for (int particle = 0; particle < PARTICLES_PER_BURST; particle++) {
            int slot = findInactiveSlot();
            if (slot < 0) {
                break;
            }
            int pattern = burst * 37 + particle * 17;
            double angle =
                    Math.PI * 2.0 * particle / PARTICLES_PER_BURST
                            + burst * 0.29;
            float speed = 0.105f + (pattern % 13) * 0.007f;
            active[slot] = true;
            x[slot] = centerX;
            y[slot] = centerY;
            velocityX[slot] = (float) Math.cos(angle) * speed;
            velocityY[slot] = (float) Math.sin(angle) * speed + 0.045f;
            age[slot] = 0f;
            lifetime[slot] = 1.20f + (pattern % 9) * 0.075f;
            size[slot] = 0.72f + (pattern % 5) * 0.12f;
            colorIndex[slot] = (burst + particle / 6) % 5;
        }
        launchedBurstCount++;
    }

    private int findInactiveSlot() {
        for (int i = 0; i < active.length; i++) {
            if (!active[i]) {
                return i;
            }
        }
        return -1;
    }
}
