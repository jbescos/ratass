package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteAbilityVisualStyle;
import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardDefinition;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;

/** Selects a concurrent revenge effect that must remain visible below its amplifier. */
public final class RogueliteAbilityEffectLayers {
    private RogueliteAbilityEffectLayers() {
    }

    public static RogueliteCardId underlayFor(
            RogueliteCardId primaryCardId,
            RogueliteCardId activePowerupCardId,
            RogueliteCardId activeRevengeCardId) {
        if (activeRevengeCardId == null
                || activeRevengeCardId == primaryCardId
                || !isRevengeAmplifier(activePowerupCardId)
                || !hasCenteredArtwork(activeRevengeCardId)) {
            return null;
        }
        return activeRevengeCardId;
    }

    private static boolean isRevengeAmplifier(RogueliteCardId cardId) {
        RogueliteCardDefinition card = cardId == null
                ? null
                : RogueliteCardCatalog.get(cardId);
        if (card == null) {
            return false;
        }
        RogueliteAbilityVisualStyle style = card.getAbilityVisualStyle();
        return style == RogueliteAbilityVisualStyle.REVENGE_BOOST_T1
                || style == RogueliteAbilityVisualStyle.REVENGE_BOOST_T2
                || style == RogueliteAbilityVisualStyle.REVENGE_BOOST_T3;
    }

    private static boolean hasCenteredArtwork(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.DRAFT_VENDETTA
                || cardId == RogueliteCardId.TAR_TETHER
                || cardId == RogueliteCardId.EMP_SNARE
                || cardId == RogueliteCardId.VOID_ANCHOR
                || cardId == RogueliteCardId.CROWN_ENGINE) {
            return false;
        }
        RogueliteCardDefinition card = RogueliteCardCatalog.get(cardId);
        if (card == null) {
            return false;
        }
        RogueliteAbilityVisualStyle style = card.getAbilityVisualStyle();
        return style != RogueliteAbilityVisualStyle.MIRROR
                && style != RogueliteAbilityVisualStyle.CLOAK
                && RogueliteAbilityEffectAtlas.indexFor(style) >= 0;
    }
}
