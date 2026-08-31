package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;

/** Presentation-only sprite selection for the Hunter revenge projectiles. */
public final class HunterProjectileSprite {
    public static final String BARRAGE_ASSET_PATH =
            "roguelite/effects/hunter_barrage_projectile.png";
    public static final String STORM_ASSET_PATH =
            "roguelite/effects/hunter_storm_projectile.png";

    private HunterProjectileSprite() {
    }

    public static boolean supports(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.HUNTER_BARRAGE
                || cardId == RogueliteCardId.HUNTER_STORM;
    }

    public static String assetPath(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.HUNTER_BARRAGE) {
            return BARRAGE_ASSET_PATH;
        }
        if (cardId == RogueliteCardId.HUNTER_STORM) {
            return STORM_ASSET_PATH;
        }
        return null;
    }

    public static float sizeScale(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.HUNTER_BARRAGE) {
            return 0.72f;
        }
        if (cardId == RogueliteCardId.HUNTER_STORM) {
            return 0.94f;
        }
        return 0f;
    }
}
