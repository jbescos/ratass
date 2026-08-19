package com.github.jbescos.ai.rl;

import static org.junit.Assert.assertEquals;

import com.github.jbescos.ai.AiControlDecision;
import org.junit.Test;

public class RlPolicyTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void oneActionPolicyLeavesSecondControlNeutral() {
        RlPolicy policy = RlPolicy.fromJson(
                "{\"format\":\"ratass-rl-policy-v3\","
                        + "\"observationSize\":1,\"actionSize\":1,\"layers\":[{"
                        + "\"inputSize\":1,\"outputSize\":1,\"activation\":\"linear\","
                        + "\"weights\":[0.5],\"bias\":[0.1]}]}");
        AiControlDecision result = policy.computeAction(
                new float[] {0.8f},
                new float[policy.getScratchSize()],
                new float[policy.getScratchSize()],
                new AiControlDecision());

        assertEquals(0.5f, result.throttle, EPSILON);
        assertEquals(0f, result.turn, EPSILON);
    }

    @Test
    public void rawOutputIsNotClampedForCandidateScoring() {
        RlPolicy policy = RlPolicy.fromJson(
                "{\"format\":\"ratass-rl-policy-v3\","
                        + "\"observationSize\":1,\"actionSize\":1,\"layers\":[{"
                        + "\"inputSize\":1,\"outputSize\":1,\"activation\":\"linear\","
                        + "\"weights\":[2.0],\"bias\":[0.5]}]}");
        float[] output = new float[1];

        policy.computeOutputs(
                new float[] {0.75f},
                new float[policy.getScratchSize()],
                new float[policy.getScratchSize()],
                output);

        assertEquals(2f, output[0], EPSILON);
    }
}
