package com.github.jbescos.gameplay.maps;

import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;

abstract class BoilerDeckMapDefinition extends AbstractArenaMapDefinition {
    private final int variant;

    protected BoilerDeckMapDefinition(int variant) {
        super(variantId("boiler-deck", variant), variantName("Boiler Deck", variant));
        this.variant = variant;
    }

    @Override
    protected final void define(ArenaMap.Builder builder) {
        float width = 40f + variant * 2.4f;
        float height = 24f + variant * 1.1f;
        float sideTowerWidth = 5.4f + variant * 0.5f;
        float sideTowerHeight = height * 0.58f;
        float balconyWidth = 15f + variant * 1.5f;
        float balconyHeight = 5.2f + variant * 0.5f;

        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, width, height));

        if (variant % 2 == 0) {
            builder.solid(ArenaShape.rectangle(0f, height * 0.50f, balconyWidth, balconyHeight))
                    .solid(ArenaShape.rectangle(0f, -height * 0.50f, balconyWidth * 0.8f, balconyHeight * 0.9f));
        } else {
            builder.solid(ArenaShape.rectangle(-width * 0.50f, 0f, sideTowerWidth, sideTowerHeight))
                    .solid(ArenaShape.rectangle(width * 0.50f, 0f, sideTowerWidth, sideTowerHeight));
        }

        addCardinalSpawns(builder, width * 0.28f, height * 0.28f);
        addGridRecoveryPoints(builder, width * 0.34f, height * 0.32f, 5, 4);
    }
}

final class BoilerDeck1Map extends BoilerDeckMapDefinition {
    BoilerDeck1Map() {
        super(0);
    }
}

final class BoilerDeck2Map extends BoilerDeckMapDefinition {
    BoilerDeck2Map() {
        super(1);
    }
}

final class BoilerDeck3Map extends BoilerDeckMapDefinition {
    BoilerDeck3Map() {
        super(2);
    }
}

final class BoilerDeck4Map extends BoilerDeckMapDefinition {
    BoilerDeck4Map() {
        super(3);
    }
}

final class BoilerDeck5Map extends BoilerDeckMapDefinition {
    BoilerDeck5Map() {
        super(4);
    }
}
