package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import java.util.EnumSet;
import java.util.Set;

/** Optional per-card reward adjustments used to teach distinct strategy personalities. */
final class CardStrategyCardPreference {
    private final Set<RogueliteCardId> preferred;
    private final Set<RogueliteCardId> discouraged;
    private final float preferredReward;
    private final float discouragedPenalty;

    CardStrategyCardPreference(
            String preferredCardIds,
            float preferredReward,
            String discouragedCardIds,
            float discouragedPenalty) {
        preferred = parse(preferredCardIds);
        discouraged = parse(discouragedCardIds);
        this.preferredReward = finite(preferredReward);
        this.discouragedPenalty = Math.max(0f, finite(discouragedPenalty));
    }

    float reward(RogueliteLoadout loadout, RogueliteCardId cardId) {
        if (cardId == null) {
            return 0f;
        }
        float reward = preferred.contains(cardId)
                ? preferredReward(loadout, cardId) : 0f;
        return discouraged.contains(cardId) ? reward - discouragedPenalty : reward;
    }

    private float preferredReward(RogueliteLoadout loadout, RogueliteCardId cardId) {
        if (loadout == null) {
            return preferredReward;
        }
        RogueliteSlotType slotType = RogueliteCardCatalog.get(cardId).getSlotType();
        RogueliteCardId equipped = loadout.get(slotType);
        if (!preferred.contains(equipped)) {
            return preferredReward;
        }
        int equippedTier = RogueliteCardCatalog.get(equipped).getTier();
        int candidateTier = RogueliteCardCatalog.get(cardId).getTier();
        int tierGain = candidateTier - equippedTier;
        return tierGain > 0
                ? preferredReward * tierGain / 3f
                : -discouragedPenalty;
    }

    private static Set<RogueliteCardId> parse(String cardIds) {
        Set<RogueliteCardId> result = EnumSet.noneOf(RogueliteCardId.class);
        if (cardIds == null || cardIds.trim().isEmpty()) {
            return result;
        }
        String[] values = cardIds.split(",");
        for (String value : values) {
            String normalized = value.trim();
            if (!normalized.isEmpty()) {
                result.add(RogueliteCardId.valueOf(normalized));
            }
        }
        return result;
    }

    private static float finite(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) ? 0f : value;
    }
}
