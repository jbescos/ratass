package com.github.jbescos.ai.rl;

import com.github.jbescos.ai.AiControlDecision;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public final class RlPolicySmokeMain {
    private static final String DEFAULT_POLICY_RESOURCE = "ai/rl_enemy_policy.json";

    private RlPolicySmokeMain() {
    }

    public static void main(String[] args) throws IOException {
        String resource = args.length == 0 ? DEFAULT_POLICY_RESOURCE : args[0];
        InputStream input =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (input == null) {
            throw new IllegalArgumentException("Missing RL policy resource: " + resource);
        }

        RlPolicy policy = RlPolicy.fromJson(readString(input));
        float[] observation = new float[policy.getObservationSize()];
        setObservation(observation, 7, 0.35f);
        setObservation(observation, 8, 0.75f);
        setObservation(observation, 9, 0.20f);
        setObservation(observation, 17, 1f);
        setObservation(observation, 18, 0.18f);
        setObservation(observation, 41, 0.75f);
        setObservation(observation, 42, 0.75f);

        float[] scratchA = new float[policy.getScratchSize()];
        float[] scratchB = new float[policy.getScratchSize()];
        AiControlDecision decision =
                policy.computeAction(observation, scratchA, scratchB, new AiControlDecision());

        System.out.printf(
                Locale.US,
                "policy=%s observationSize=%d actionSize=%d throttle=%.3f turn=%.3f%n",
                resource,
                policy.getObservationSize(),
                policy.getActionSize(),
                decision.throttle,
                decision.turn);
    }

    private static void setObservation(float[] observation, int index, float value) {
        if (index >= 0 && index < observation.length) {
            observation[index] = value;
        }
    }

    private static String readString(InputStream input) throws IOException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toString("UTF-8");
        } finally {
            input.close();
        }
    }
}
