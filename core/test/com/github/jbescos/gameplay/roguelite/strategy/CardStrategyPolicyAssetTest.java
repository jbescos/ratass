package com.github.jbescos.gameplay.roguelite.strategy;

import static org.junit.Assert.assertTrue;

import com.github.jbescos.ai.rl.RlPolicy;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

public final class CardStrategyPolicyAssetTest {
    @Test
    public void everyInstalledNeuralStrategyMatchesTheCurrentEncoder() throws Exception {
        File strategyRoot = findStrategyRoot();
        for (String profileId : new String[] {"strategy00", "strategy01", "strategy02"}) {
            File policyFile = new File(
                    new File(strategyRoot, profileId),
                    "rl_card_strategy_policy.json");
            assertTrue("Missing strategy policy: " + policyFile, policyFile.isFile());
            String json = new String(
                    Files.readAllBytes(policyFile.toPath()),
                    StandardCharsets.UTF_8);

            new NeuralCardStrategy(profileId, RlPolicy.fromJson(json));
        }
    }

    private static File findStrategyRoot() {
        File fromRepositoryRoot = new File("assets/ai/card-strategies");
        if (fromRepositoryRoot.isDirectory()) {
            return fromRepositoryRoot;
        }
        return new File("../assets/ai/card-strategies");
    }
}
