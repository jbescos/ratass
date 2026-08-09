package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteAbilityVisualStyle;

/** Maps presentation-only ability styles to cells in the generated effect atlas. */
public final class RogueliteAbilityEffectAtlas {
    public static final int COLUMNS = 7;
    public static final int ROWS = 1;

    private RogueliteAbilityEffectAtlas() {
    }

    public static int indexFor(RogueliteAbilityVisualStyle style) {
        if (style == null) {
            return -1;
        }
        switch (style) {
            case NITRO:
                return 0;
            case GRIP:
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
            default:
                return -1;
        }
    }

    public static float sizeScale(RogueliteAbilityVisualStyle style) {
        if (style == null) {
            return 0f;
        }
        switch (style) {
            case NITRO:
            case RAM:
                return 1.70f;
            case MIRROR:
                return 1.92f;
            case GRIP:
            case DRAFT:
            case SHIELD:
            case CLOAK:
            default:
                return 1.82f;
        }
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
}
