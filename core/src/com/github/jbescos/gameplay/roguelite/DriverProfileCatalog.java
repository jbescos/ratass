package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DriverProfileCatalog {
    public static final int MAX_TIER = 4;
    public static final int MAX_STANDARD_TIER = 3;
    private static final String[] FALLBACK_PROFILE_IDS = {
            "profile00",
            "profile01",
            "profile02",
            "profile03",
            "profile04",
            "profile05",
            "profile06",
            "profile07",
            "profile08",
            "profile09"
    };

    private final List<DriverProfileMetadata> profiles;
    private final Map<String, DriverProfileMetadata> profilesById;

    public DriverProfileCatalog(List<DriverProfileMetadata> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            throw new IllegalArgumentException("At least one driver profile is required.");
        }
        List<DriverProfileMetadata> sorted =
                new ArrayList<DriverProfileMetadata>(metadata);
        Collections.sort(sorted, new Comparator<DriverProfileMetadata>() {
            @Override
            public int compare(
                    DriverProfileMetadata left,
                    DriverProfileMetadata right) {
                int averageLap = compareAverageLapForQuality(left, right);
                if (averageLap != 0) {
                    return averageLap;
                }
                return right.getProfileId().compareTo(left.getProfileId());
            }
        });

        Map<String, DriverProfileMetadata> byId =
                new LinkedHashMap<String, DriverProfileMetadata>();
        for (int i = 0; i < sorted.size(); i++) {
            DriverProfileMetadata profile = sorted.get(i);
            if (byId.put(profile.getProfileId(), profile) != null) {
                throw new IllegalArgumentException(
                        "Duplicate driver profile: " + profile.getProfileId());
            }
        }
        profiles = Collections.unmodifiableList(sorted);
        profilesById = Collections.unmodifiableMap(byId);
    }

    private static int compareAverageLapForQuality(
            DriverProfileMetadata left,
            DriverProfileMetadata right) {
        boolean leftValid = isValidAverageLap(left.getAverageLapSeconds());
        boolean rightValid = isValidAverageLap(right.getAverageLapSeconds());
        if (leftValid != rightValid) {
            return leftValid ? -1 : 1;
        }
        if (!leftValid) {
            return 0;
        }
        return Float.compare(
                left.getAverageLapSeconds(),
                right.getAverageLapSeconds());
    }

    private static boolean isValidAverageLap(float averageLap) {
        return averageLap > 0f
                && !Float.isNaN(averageLap)
                && !Float.isInfinite(averageLap);
    }

    public static DriverProfileCatalog fallback() {
        List<DriverProfileMetadata> profiles =
                new ArrayList<DriverProfileMetadata>(FALLBACK_PROFILE_IDS.length);
        for (int i = 0; i < FALLBACK_PROFILE_IDS.length; i++) {
            profiles.add(DriverProfileMetadata.fallback(FALLBACK_PROFILE_IDS[i], i));
        }
        return new DriverProfileCatalog(profiles);
    }

    public List<DriverProfileMetadata> all() {
        return profiles;
    }

    public DriverProfileMetadata get(String profileId) {
        return profilesById.get(profileId);
    }

    public DriverProfileMetadata getWorst() {
        return profiles.get(profiles.size() - 1);
    }

    public DriverProfileMetadata getBest() {
        return profiles.get(0);
    }

    public DriverProfileMetadata getBestInTier(int requestedTier) {
        int tier = Math.max(1, Math.min(MAX_TIER, requestedTier));
        for (int i = 0; i < profiles.size(); i++) {
            DriverProfileMetadata profile = profiles.get(i);
            if (getTier(profile.getProfileId()) == tier) {
                return profile;
            }
        }
        return null;
    }

    public int getTier(String profileId) {
        for (int i = 0; i < profiles.size(); i++) {
            DriverProfileMetadata profile = profiles.get(i);
            if (profile.getProfileId().equals(profileId)) {
                if (profile.getTier() > 0) {
                    return Math.min(MAX_TIER, profile.getTier());
                }
                int automaticCount = 0;
                int betterAutomaticCount = 0;
                for (int candidateIndex = 0; candidateIndex < profiles.size(); candidateIndex++) {
                    DriverProfileMetadata candidate = profiles.get(candidateIndex);
                    if (candidate.getTier() > 0) {
                        continue;
                    }
                    automaticCount++;
                    if (candidateIndex < i) {
                        betterAutomaticCount++;
                    }
                }
                int rankFromWorst = automaticCount - 1 - betterAutomaticCount;
                return Math.min(
                        MAX_STANDARD_TIER,
                        1 + rankFromWorst * MAX_STANDARD_TIER / automaticCount);
            }
        }
        return MAX_TIER;
    }

    public List<DriverProfileMetadata> eligibleThroughTier(int unlockedTier) {
        int tier = Math.max(1, Math.min(MAX_TIER, unlockedTier));
        List<DriverProfileMetadata> eligible =
                new ArrayList<DriverProfileMetadata>();
        for (int i = 0; i < profiles.size(); i++) {
            DriverProfileMetadata profile = profiles.get(i);
            if (getTier(profile.getProfileId()) <= tier) {
                eligible.add(profile);
            }
        }
        return eligible;
    }
}
