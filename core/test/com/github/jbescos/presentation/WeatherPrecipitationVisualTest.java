package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class WeatherPrecipitationVisualTest {
    @Test
    public void usesDenseButBoundedPrecipitationLayers() {
        assertEquals(112, WeatherPrecipitationVisual.RAIN_STREAK_COUNT);
        assertEquals(28, WeatherPrecipitationVisual.RAIN_SPLASH_COUNT);
        assertEquals(156, WeatherPrecipitationVisual.SNOWFLAKE_COUNT);
    }

    @Test
    public void foregroundSnowflakesAreLargerThanDistantSnowflakes() {
        float distant = WeatherPrecipitationVisual.snowflakeRadius(100f, 0.5f, 0.55f);
        float foreground = WeatherPrecipitationVisual.snowflakeRadius(100f, 0.5f, 1.20f);

        assertTrue(foreground > distant);
    }
}
