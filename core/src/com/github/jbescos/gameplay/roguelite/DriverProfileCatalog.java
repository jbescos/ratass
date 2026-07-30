package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DriverProfileCatalog {
    public static final int MAX_TIER = 5;
    private static final int DRIVERS_PER_TIER = 2;
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
                int rating =
                        Float.compare(
                                left.getOverallRating(),
                                right.getOverallRating());
                return rating != 0
                        ? rating
                        : left.getProfileId().compareTo(right.getProfileId());
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
        return profiles.get(0);
    }

    public int getTier(String profileId) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getProfileId().equals(profileId)) {
                return Math.min(MAX_TIER, 1 + i / DRIVERS_PER_TIER);
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
