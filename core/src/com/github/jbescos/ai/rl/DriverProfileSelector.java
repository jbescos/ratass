package com.github.jbescos.ai.rl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class DriverProfileSelector {
    private final Random random;

    public DriverProfileSelector() {
        this(new Random());
    }

    DriverProfileSelector(Random random) {
        this.random = random;
    }

    public List<String> selectUnique(
            List<String> availableProfileIds,
            String reservedProfileId,
            int requestedCount) {
        if (availableProfileIds == null || requestedCount <= 0) {
            return Collections.emptyList();
        }

        Set<String> distinctProfileIds =
                new LinkedHashSet<String>(availableProfileIds);
        if (reservedProfileId != null) {
            distinctProfileIds.remove(reservedProfileId);
        }

        List<String> shuffledProfileIds =
                new ArrayList<String>(distinctProfileIds);
        Collections.shuffle(shuffledProfileIds, random);
        if (shuffledProfileIds.size() > requestedCount) {
            return new ArrayList<String>(
                    shuffledProfileIds.subList(0, requestedCount));
        }
        return shuffledProfileIds;
    }
}
