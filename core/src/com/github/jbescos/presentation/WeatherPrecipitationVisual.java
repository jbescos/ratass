package com.github.jbescos.presentation;

/** Presentation-only precipitation density and sizing. */
public final class WeatherPrecipitationVisual {
    public static final int RAIN_STREAK_COUNT = 112;
    public static final int RAIN_SPLASH_COUNT = 28;
    public static final int SNOWFLAKE_COUNT = 156;

    private WeatherPrecipitationVisual() {
    }

    public static float snowflakeRadius(float coverSize, float sizeVariation, float depth) {
        float baseRadius = Math.max(0.035f, coverSize * (0.00075f + sizeVariation * 0.00075f));
        return baseRadius * (0.82f + Math.max(0f, depth) * 0.28f);
    }
}
