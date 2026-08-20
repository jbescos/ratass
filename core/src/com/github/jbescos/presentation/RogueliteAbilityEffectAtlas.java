package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteAbilityVisualStyle;

/** Maps presentation-only ability styles to cells in the generated effect atlas. */
public final class RogueliteAbilityEffectAtlas {
    public static final int COLUMNS = 19;
    public static final int ROWS = 1;

    private RogueliteAbilityEffectAtlas() {
    }

    public static int indexFor(RogueliteAbilityVisualStyle style) {
        if (style == null) {
            return -1;
        }
        switch (style) {
            case NITRO_T1:
                return 0;
            case GRIP_T1:
                return 1;
            case RAM:
                return 2;
            case DRAFT:
                return 3;
            case SHIELD:
                return 4;
            case MIRROR:
                return 5;
            case CLOAK:
                return 6;
            case REVENGE_BOOST_T1:
                return 7;
            case REVENGE_BOOST_T2:
                return 8;
            case REVENGE_BOOST_T3:
                return 9;
            case NITRO_T2:
                return 10;
            case NITRO_T3:
                return 11;
            case GRIP_T2:
                return 12;
            case GRIP_T3:
                return 13;
            case TIME_T1:
                return 14;
            case TIME_T2:
                return 15;
            case TIME_T3:
                return 16;
            case HOTLINE_T1:
                return 17;
            case HOTLINE_T2:
                return 18;
            case ICON_ONLY:
            default:
                return -1;
        }
    }

    public static float sizeScale(RogueliteAbilityVisualStyle style) {
        if (style == null) {
            return 0f;
        }
        switch (style) {
            case ICON_ONLY:
                return 0f;
            case NITRO_T1:
            case RAM:
                return 1.70f;
            case NITRO_T2:
                return 2.20f;
            case NITRO_T3:
                return 2.70f;
            case MIRROR:
                return 1.92f;
            case REVENGE_BOOST_T1:
            case REVENGE_BOOST_T2:
            case REVENGE_BOOST_T3:
                return 2.05f;
            case GRIP_T1:
                return 1.82f;
            case GRIP_T2:
                return 1.96f;
            case GRIP_T3:
                return 2.12f;
            case TIME_T1:
                return 1.90f;
            case TIME_T2:
                return 2.15f;
            case TIME_T3:
                return 2.40f;
            case HOTLINE_T1:
                return 2.10f;
            case HOTLINE_T2:
                return 2.35f;
            case DRAFT:
            case SHIELD:
            case CLOAK:
            default:
                return 1.82f;
        }
    }

    public static boolean usesDedicatedHotlineIcon(RogueliteAbilityVisualStyle style) {
        return style == RogueliteAbilityVisualStyle.HOTLINE_T1
                || style == RogueliteAbilityVisualStyle.HOTLINE_T2;
    }

    /** Places directional flame artwork behind the car instead of beneath its body. */
    public static float localCenterOffsetY(
            RogueliteAbilityVisualStyle style,
            float carHeight,
            float effectSize) {
        if (!usesRearExhaustAnchor(style)) {
            return 0f;
        }
        float safeCarHeight = sanitizePositive(carHeight);
        float safeEffectSize = sanitizePositive(effectSize);
        return -safeCarHeight * 0.49f - safeEffectSize * 0.34f;
    }

    public static boolean usesRearExhaustAnchor(RogueliteAbilityVisualStyle style) {
        return style == RogueliteAbilityVisualStyle.NITRO_T1
                || style == RogueliteAbilityVisualStyle.NITRO_T2
                || style == RogueliteAbilityVisualStyle.NITRO_T3;
    }

    /** Keeps clock artwork visibly turning clockwise, with stronger motion by tier. */
    public static float timeRotationDegrees(
            RogueliteAbilityVisualStyle style,
            float elapsedSeconds) {
        float speed;
        if (style == RogueliteAbilityVisualStyle.TIME_T1) {
            speed = 120f;
        } else if (style == RogueliteAbilityVisualStyle.TIME_T2) {
            speed = 180f;
        } else if (style == RogueliteAbilityVisualStyle.TIME_T3) {
            speed = 240f;
        } else {
            return 0f;
        }
        return sanitizeNonNegative(elapsedSeconds) * speed;
    }

    /** Turns every suction spiral in the same direction, with stronger motion by tier. */
    public static float gripRotationDegrees(
            RogueliteAbilityVisualStyle style,
            float elapsedSeconds) {
        float speed;
        if (style == RogueliteAbilityVisualStyle.GRIP_T1) {
            speed = 80f;
        } else if (style == RogueliteAbilityVisualStyle.GRIP_T2) {
            speed = 100f;
        } else if (style == RogueliteAbilityVisualStyle.GRIP_T3) {
            speed = 120f;
        } else {
            return 0f;
        }
        return sanitizeNonNegative(elapsedSeconds) * speed;
    }

    /** Uses the real affected diameter for circular fields instead of a car-relative size. */
    public static float worldSize(
            RogueliteAbilityVisualStyle style,
            float carRelativeSize,
            float effectRadius) {
        if (style == RogueliteAbilityVisualStyle.DRAFT
                && effectRadius > 0f
                && !Float.isNaN(effectRadius)
                && !Float.isInfinite(effectRadius)) {
            return effectRadius * 2f;
        }
        return carRelativeSize;
    }

    private static float sanitizePositive(float value) {
        return value > 0f && !Float.isNaN(value) && !Float.isInfinite(value)
                ? value : 0f;
    }

    private static float sanitizeNonNegative(float value) {
        return value >= 0f && !Float.isNaN(value) && !Float.isInfinite(value)
                ? value : 0f;
    }
}
