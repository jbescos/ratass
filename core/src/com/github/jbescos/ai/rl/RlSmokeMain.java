package com.github.jbescos.ai.rl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.github.jbescos.RatassGame;
import java.util.Locale;
import java.util.Random;

public final class RlSmokeMain {
    private RlSmokeMain() {
    }

    public static void main(String[] args) {
        int episodes = 3;
        int maxSteps = 420;
        int controlledAgents = 1;
        int fieldSize = 1;
        int actionRepeat = 4;
        int maxCheckpoints = 6;
        float checkpointRadius = 3.0f;
        boolean raceMode = true;
        long seed = 1L;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--episodes".equals(arg) && i + 1 < args.length) {
                episodes = Integer.parseInt(args[++i]);
            } else if ("--steps".equals(arg) && i + 1 < args.length) {
                maxSteps = Integer.parseInt(args[++i]);
            } else if ("--controlled-agents".equals(arg) && i + 1 < args.length) {
                controlledAgents = Integer.parseInt(args[++i]);
            } else if ("--field-size".equals(arg) && i + 1 < args.length) {
                fieldSize = Integer.parseInt(args[++i]);
            } else if ("--action-repeat".equals(arg) && i + 1 < args.length) {
                actionRepeat = Integer.parseInt(args[++i]);
            } else if ("--max-checkpoints".equals(arg) && i + 1 < args.length) {
                maxCheckpoints = Integer.parseInt(args[++i]);
            } else if ("--checkpoint-radius".equals(arg) && i + 1 < args.length) {
                checkpointRadius = Float.parseFloat(args[++i]);
            } else if ("--objective".equals(arg) && i + 1 < args.length) {
                String objective = args[++i];
                if ("race".equals(objective)) {
                    raceMode = true;
                } else {
                    throw new IllegalArgumentException("Only race objective is supported: " + objective);
                }
            } else if ("--seed".equals(arg) && i + 1 < args.length) {
                seed = Long.parseLong(args[++i]);
            } else {
                throw new IllegalArgumentException("Unknown or incomplete argument: " + arg);
            }
        }

        configureFiles();

        RatassGame.RlTrainingConfig config =
                new RatassGame.RlTrainingConfig()
                        .withControlledAgentCount(controlledAgents)
                        .withFieldSize(fieldSize)
                        .withActionRepeat(actionRepeat)
                        .withMaxActionSteps(maxSteps)
                        .withMaxCheckpoints(maxCheckpoints)
                        .withCheckpointRadius(checkpointRadius)
                        .withRaceMode(raceMode)
                        .withSeed(seed);
        Random random = new Random(seed ^ 0xC0FFEE);

        try (RatassGame.RlTrainingEnvironment environment =
                     new RatassGame.RlTrainingEnvironment(config)) {
            long totalStartNanos = System.nanoTime();
            long resetNanos = 0L;
            long stepNanos = 0L;
            int totalSteps = 0;
            for (int episode = 0; episode < episodes; episode++) {
                long resetStartNanos = System.nanoTime();
                RatassGame.RlStepResult result = environment.reset();
                resetNanos += System.nanoTime() - resetStartNanos;
                float totalReward = 0f;
                int steps = 0;

                while (!result.episodeDone && steps < maxSteps) {
                    float[] actions =
                            new float[
                                    environment.getControlledAgentCount()
                                            * environment.getActionSize()];
                    for (int i = 0; i < actions.length; i++) {
                        actions[i] = random.nextFloat() * 2f - 1f;
                    }

                    long stepStartNanos = System.nanoTime();
                    result = environment.step(actions);
                    stepNanos += System.nanoTime() - stepStartNanos;
                    for (int i = 0; i < result.rewards.length; i++) {
                        totalReward += result.rewards[i];
                    }
                    steps++;
                    totalSteps++;
                }

                System.out.printf(
                        Locale.US,
                        "episode=%d steps=%d reward=%.3f checkpoints=%d progress=%.3f success=%s%n",
                        episode + 1,
                        steps,
                        totalReward,
                        result.checkpointsReached.length > 0 ? result.checkpointsReached[0] : 0,
                        result.progressTowardCheckpoint.length > 0
                                ? result.progressTowardCheckpoint[0]
                                : 0f,
                        result.winnerAgentIndex == 0 ? "true" : "false");
            }
            long totalNanos = System.nanoTime() - totalStartNanos;
            System.out.printf(
                    Locale.US,
                    "summary episodes=%d steps=%d controlled=%d field=%d actionRepeat=%d "
                            + "resetAvgMs=%.3f stepAvgMs=%.3f stepsPerSecond=%.1f totalSeconds=%.3f%n",
                    episodes,
                    totalSteps,
                    environment.getControlledAgentCount(),
                    fieldSize,
                    actionRepeat,
                    resetNanos / 1_000_000.0 / Math.max(1, episodes),
                    stepNanos / 1_000_000.0 / Math.max(1, totalSteps),
                    totalSteps * 1_000_000_000.0 / Math.max(1L, stepNanos),
                    totalNanos / 1_000_000_000.0);
        }
    }

    private static void configureFiles() {
        GdxNativesLoader.load();
        if (Gdx.files == null) {
            Gdx.files = createDesktopFiles();
        }
    }

    private static Files createDesktopFiles() {
        try {
            return (Files) Class.forName("com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files")
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create LibGDX desktop file service.", e);
        }
    }
}
