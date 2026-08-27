package com.github.jbescos.gameplay.roguelite.save;

import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RogueliteSaveCompatibility {
    private static final String CARD_CATALOG_SIGNATURE = buildCardCatalogSignature();

    private RogueliteSaveCompatibility() {
    }

    public static String currentCardCatalogSignature() {
        return CARD_CATALOG_SIGNATURE;
    }

    public static boolean hasCurrentCardCatalog(String signature) {
        return CARD_CATALOG_SIGNATURE.equals(signature);
    }

    private static String buildCardCatalogSignature() {
        List<String> entries = new ArrayList<String>();
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            entries.add(
                    card.getId().name()
                            + ":"
                            + card.getTier()
                            + ":"
                            + card.getSlotType().name());
        }
        Collections.sort(entries);

        int hash = 0x811c9dc5;
        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            for (int characterIndex = 0;
                    characterIndex < entry.length();
                    characterIndex++) {
                hash ^= entry.charAt(characterIndex);
                hash *= 0x01000193;
            }
            hash ^= '\n';
            hash *= 0x01000193;
        }
        return "cards-v1-" + entries.size() + "-" + Integer.toHexString(hash);
    }
}
