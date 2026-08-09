package com.github.jbescos.presentation;

/** Text, layout, and visibility rules for the targeted Revenge label. */
public final class RevengeTargetLabel {
    private RevengeTargetLabel() {}

    public static boolean shouldDraw(
            boolean skullVisible,
            int targetVehicleId,
            boolean targetActive,
            String targetName) {
        return skullVisible
                && targetVehicleId >= 0
                && targetActive
                && targetName != null
                && hasVisibleCharacter(targetName);
    }

    public static float centeredX(
            float skullCenterX,
            float textWidth,
            float minimumX,
            float maximumRight) {
        float safeWidth = Math.max(0f, textWidth);
        float maximumX = Math.max(minimumX, maximumRight - safeWidth);
        return clamp(skullCenterX - safeWidth * 0.5f, minimumX, maximumX);
    }

    public static String buildText(String cardName, String targetName) {
        String safeCardName = cardName == null ? "" : cardName.trim();
        String safeTargetName = targetName == null ? "" : targetName.trim();
        if (safeCardName.length() == 0) {
            return safeTargetName;
        }
        if (safeTargetName.length() == 0) {
            return safeCardName;
        }
        return safeCardName + " on " + safeTargetName;
    }

    public static float baseline(
            float skullCenterY,
            float lineHeight,
            float minimumBaseline,
            float maximumBaseline) {
        float clearance = Math.max(10f, Math.max(0f, lineHeight) * 0.72f);
        return clamp(
                skullCenterY - clearance,
                minimumBaseline,
                Math.max(minimumBaseline, maximumBaseline));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean hasVisibleCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
