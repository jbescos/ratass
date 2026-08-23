package com.github.jbescos.presentation;

/** Formats car-stat multipliers as bonuses relative to the neutral 100% baseline. */
public final class CarStatBonusText {
    private CarStatBonusText() {
    }

    public static String format(float multiplier) {
        if (Float.isNaN(multiplier) || Float.isInfinite(multiplier)) {
            return "0%";
        }
        int tenths = Math.round((multiplier - 1f) * 1000f);
        if (tenths == 0) {
            return "0%";
        }

        String sign = tenths > 0 ? "+" : "-";
        int absoluteTenths = Math.abs(tenths);
        if (absoluteTenths % 10 == 0) {
            return sign + (absoluteTenths / 10) + "%";
        }
        return sign
                + (absoluteTenths / 10)
                + "."
                + (absoluteTenths % 10)
                + "%";
    }

    public static String formatMultiplier(float multiplier) {
        if (!Float.isFinite(multiplier) || multiplier < 1f) {
            return "x1";
        }
        int hundredths = Math.round(multiplier * 100f);
        if (hundredths % 100 == 0) {
            return "x" + (hundredths / 100);
        }
        if (hundredths % 10 == 0) {
            return "x" + (hundredths / 100) + "." + ((hundredths / 10) % 10);
        }
        int decimals = hundredths % 100;
        return "x"
                + (hundredths / 100)
                + "."
                + (decimals < 10 ? "0" : "")
                + decimals;
    }
}
