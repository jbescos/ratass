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
}
