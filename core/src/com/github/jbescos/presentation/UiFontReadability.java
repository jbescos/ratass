package com.github.jbescos.presentation;

/** Selects a bounded font-size boost for physically small mobile displays. */
public final class UiFontReadability {
    static final float PHONE_SCALE = 1.18f;
    static final float LARGE_SCREEN_SCALE = 1f;
    private static final float PHONE_DIAGONAL_INCHES = 6.8f;
    private static final float LARGE_SCREEN_DIAGONAL_INCHES = 10f;
    private static final float UNKNOWN_MOBILE_SCALE = 1.12f;

    private UiFontReadability() {}

    public static float scaleForDisplay(
            boolean mobile,
            int pixelWidth,
            int pixelHeight,
            float pixelsPerInchX,
            float pixelsPerInchY) {
        if (!mobile) {
            return LARGE_SCREEN_SCALE;
        }
        if (pixelWidth <= 0
                || pixelHeight <= 0
                || !isUsablePixelsPerInch(pixelsPerInchX)
                || !isUsablePixelsPerInch(pixelsPerInchY)) {
            return UNKNOWN_MOBILE_SCALE;
        }

        float widthInches = pixelWidth / pixelsPerInchX;
        float heightInches = pixelHeight / pixelsPerInchY;
        float diagonalInches = (float) Math.sqrt(
                widthInches * widthInches + heightInches * heightInches);
        if (diagonalInches <= PHONE_DIAGONAL_INCHES) {
            return PHONE_SCALE;
        }
        if (diagonalInches >= LARGE_SCREEN_DIAGONAL_INCHES) {
            return LARGE_SCREEN_SCALE;
        }

        float interpolation =
                (diagonalInches - PHONE_DIAGONAL_INCHES)
                        / (LARGE_SCREEN_DIAGONAL_INCHES - PHONE_DIAGONAL_INCHES);
        return PHONE_SCALE
                + (LARGE_SCREEN_SCALE - PHONE_SCALE) * interpolation;
    }

    private static boolean isUsablePixelsPerInch(float value) {
        return !Float.isNaN(value)
                && !Float.isInfinite(value)
                && value >= 72f
                && value <= 1000f;
    }
}
