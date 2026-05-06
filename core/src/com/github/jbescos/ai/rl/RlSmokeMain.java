package com.github.jbescos.ai.rl;

import com.github.jbescos.RatassGame;
import java.util.Locale;
import java.util.Random;

public final class RlSmokeMain {
    private RlSmokeMain() {
    }

    public static void main(String[] args) {
        int episodes = 3;
        int maxSteps = 420;
        long seed = 1L;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--episodes".equals(arg) && i + 1 < args.length) {
                episodes = Integer.parseInt(args[++i]);
            } else if ("--steps".equals(arg) && i + 1 < args.length) {
                maxSteps = Integer.parseInt(args[++i]);
            } else if ("--seed".equals(arg) && i + 1 < args.length) {
                seed = Long.parseLong(args[++i]);
            } else {
                throw new IllegalArgumentException("Unknown or incomplete argument: " + arg);
            }
        }

        RatassGame.RlTrainingConfig config =
                new RatassGame.RlTrainingConfig()
                        .withControlledAgentCount(1)
                        .withFieldSize(8)
                        .withMaxActionSteps(maxSteps)
                        .withSeed(seed);
        Random random = new Random(seed ^ 0xC0FFEE);

        try (RatassGame.RlTrainingEnvironment environment =
                     new RatassGame.RlTrainingEnvironment(config)) {
            for (int episode = 0; episode < episodes; episode++) {
                RatassGame.RlStepResult result = environment.reset();
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

                    result = environment.step(actions);
                    for (int i = 0; i < result.rewards.length; i++) {
                        totalReward += result.rewards[i];
                    }
                    steps++;
                }

                System.out.printf(
                        Locale.US,
                        "episode=%d steps=%d reward=%.3f winner=%s winnerAgent=%d%n",
                        episode + 1,
                        steps,
                        totalReward,
                        result.winnerLabel,
                        result.winnerAgentIndex);
            }
        }
    }
}
