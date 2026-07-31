package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public class DriverProfileCatalogTest {
    @Test
    public void scoresDriversRelativeToTheBestAverageLapOnly() {
        DriverProfileCatalog catalog =
                new DriverProfileCatalog(Arrays.asList(
                        metadata("fast", 5f, 30f),
                        metadata("middle", 99f, 36f),
                        metadata("slow", 50f, 45f)));

        assertEquals(100f, catalog.get("fast").getOverallRating(), 0.001f);
        assertEquals(83.333f, catalog.get("middle").getOverallRating(), 0.001f);
        assertEquals(66.667f, catalog.get("slow").getOverallRating(), 0.001f);
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
                        88f,
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
    public void keepsFallbackRatingsWhenNoDriverHasBenchmarkTimes() {
        DriverProfileCatalog catalog = DriverProfileCatalog.fallback();

        assertEquals(5f, catalog.get("profile00").getOverallRating(), 0.001f);
        assertEquals("profile00", catalog.getWorst().getProfileId());
    }

    private static DriverProfileMetadata metadata(
            String profileId,
            float oldOverallRating,
            float averageLapSeconds) {
        return new DriverProfileMetadata(
                profileId,
                "hash",
                "benchmark",
                oldOverallRating,
                50f,
                50f,
                50f,
                1f,
                averageLapSeconds - 1f,
                averageLapSeconds,
                0f);
    }
}
