package com.github.jbescos;

public final class TextFormat {
    private TextFormat() {
    }

    public static String twoDigits(int value) {
        return value >= 0 && value < 10 ? "0" + value : String.valueOf(value);
    }

    public static String fixed(float value, int decimalPlaces) {
        if (decimalPlaces < 0 || decimalPlaces > 6) {
            throw new IllegalArgumentException("decimalPlaces must be between 0 and 6");
        }
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return String.valueOf(value);
        }

        long scale = 1L;
        for (int i = 0; i < decimalPlaces; i++) {
            scale *= 10L;
        }
        long scaled = Math.round(Math.abs((double) value) * scale);
        long whole = scaled / scale;
        long fraction = scaled % scale;

        StringBuilder result = new StringBuilder();
        if (value < 0f && scaled != 0L) {
            result.append('-');
        }
        result.append(whole);
        if (decimalPlaces == 0) {
            return result.toString();
        }

        result.append('.');
        String fractionText = String.valueOf(fraction);
        for (int i = fractionText.length(); i < decimalPlaces; i++) {
            result.append('0');
        }
        result.append(fractionText);
        return result.toString();
    }
}
