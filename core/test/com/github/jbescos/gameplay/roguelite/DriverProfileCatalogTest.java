package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public class DriverProfileCatalogTest {
    @Test
    public void sortsDriversBestToWorstByAverageLapOnly() {
        DriverProfileCatalog catalog =
                new DriverProfileCatalog(Arrays.asList(
                        metadata("fast", 30f),
                        metadata("middle", 36f),
                        metadata("slow", 45f)));

        assertEquals("fast", catalog.all().get(0).getProfileId());
        assertEquals("middle", catalog.all().get(1).getProfileId());
        assertEquals("slow", catalog.all().get(2).getProfileId());
        assertEquals("fast", catalog.getBest().getProfileId());
        assertEquals("slow", catalog.getWorst().getProfileId());
        assertEquals(1, catalog.getTier("slow"));
        assertEquals(2, catalog.getTier("middle"));
        assertEquals(3, catalog.getTier("fast"));
    }

    @Test
    public void preservesRawBenchmarkMeasurementsForDriverCards() {
        DriverProfileMetadata metadata =
                new DriverProfileMetadata(
                        "profile",
                        "hash",
                        "benchmark",
                        0f,
                        0f,
                        0f,
                        1f,
                        34f,
                        35.25f,
                        4f,
                        2.75f,
                        14.5f,
                        268.4f);

        assertEquals(35.25f, metadata.getAverageLapSeconds(), 0.001f);
        assertEquals(268.4f, metadata.getMaximumSpeedKph(), 0.001f);
        assertEquals(2.75f, metadata.getAverageOffRoadPercent(), 0.001f);
        assertEquals(14.5f, metadata.getAverageDriftPercent(), 0.001f);
    }

    @Test
    public void keepsProfileZeroAsFallbackWorstDriver() {
        DriverProfileCatalog catalog = DriverProfileCatalog.fallback();

        assertEquals("profile09", catalog.all().get(0).getProfileId());
        assertEquals("profile00", catalog.getWorst().getProfileId());
    }

    @Test
    public void explicitTierFourDoesNotReshuffleRankedDrivers() {
        DriverProfileMetadata tierFour =
                new DriverProfileMetadata(
                        "reflex",
                        "hash",
                        "benchmark",
                        100f,
                        100f,
                        100f,
                        1f,
                        25f,
                        26f,
                        0f,
                        0f,
                        0f,
                        280f,
                        4,
                        2);
        DriverProfileCatalog catalog =
                new DriverProfileCatalog(Arrays.asList(
                        tierFour,
                        metadata("fast", 30f),
                        metadata("middle", 36f),
                        metadata("slow", 45f)));

        assertEquals(4, catalog.getTier("reflex"));
        assertEquals(3, catalog.getTier("fast"));
        assertEquals(2, catalog.getTier("middle"));
        assertEquals(1, catalog.getTier("slow"));
        assertEquals(2, tierFour.getActionRepeat());
        assertEquals(1, catalog.eligibleThroughTier(4).stream()
                .filter(driver -> driver.getProfileId().equals("reflex"))
                .count());
        assertEquals("reflex", catalog.getBestInTier(4).getProfileId());
    }

    @Test
    public void bestTierFourIsSelectedEvenWhenAStandardDriverBenchmarksFaster() {
        DriverProfileMetadata tierFour =
                new DriverProfileMetadata(
                        "tier-four",
                        "hash",
                        "benchmark",
                        100f,
                        100f,
                        100f,
                        1f,
                        35f,
                        36f,
                        0f,
                        0f,
                        0f,
                        280f,
                        4,
                        2);
        DriverProfileCatalog catalog =
                new DriverProfileCatalog(Arrays.asList(
                        metadata("standard", 30f),
                        tierFour));

        assertEquals("standard", catalog.getBest().getProfileId());
        assertEquals("tier-four", catalog.getBestInTier(4).getProfileId());
    }

    @Test
    public void oldMetadataDefaultsToNormalReflexCadence() {
        DriverProfileMetadata.Data data = new DriverProfileMetadata.Data();
        data.schemaVersion = DriverProfileMetadata.SCHEMA_VERSION;
        data.profileId = "legacy";

        DriverProfileMetadata metadata =
                DriverProfileMetadata.fromData(data, "legacy");

        assertEquals(DriverProfileMetadata.DEFAULT_ACTION_REPEAT, metadata.getActionRepeat());
        assertEquals(0, metadata.getTier());
    }

    private static DriverProfileMetadata metadata(
            String profileId,
            float averageLapSeconds) {
        return new DriverProfileMetadata(
                profileId,
                "hash",
                "benchmark",
                50f,
                50f,
                50f,
                1f,
                averageLapSeconds - 1f,
                averageLapSeconds,
                0f);
    }
}
