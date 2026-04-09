package com.github.jbescos.gameplay.maps;

import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;

abstract class CrosswindJunctionMapDefinition extends AbstractArenaMapDefinition {
    private final int variant;

    protected CrosswindJunctionMapDefinition(int variant) {
        super(
                variantId("crosswind-junction", variant),
                variantName("Crosswind Junction", variant));
        this.variant = variant;
    }

    @Override
    protected final void define(ArenaMap.Builder builder) {
        float horizontalWidth = 36f + variant * 2.2f;
        float horizontalHeight = 9.6f + (variant % 2) * 0.9f;
        float verticalWidth = 15.5f + variant * 0.9f;
        float verticalHeight = 26f + variant * 1.9f;

        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, horizontalWidth, horizontalHeight))
                .solid(ArenaShape.rectangle(0f, 0f, verticalWidth, verticalHeight));

        addCardinalSpawns(builder, horizontalWidth * 0.35f, verticalHeight * 0.35f);
        addAxisRecoveryPoints(builder, horizontalWidth * 0.34f, verticalHeight * 0.34f, 4.4f);
        builder.recoveryPoint(-verticalWidth * 0.20f, verticalHeight * 0.22f)
                .recoveryPoint(verticalWidth * 0.20f, -verticalHeight * 0.22f);
    }
}

final class CrosswindJunction1Map extends CrosswindJunctionMapDefinition {
    CrosswindJunction1Map() {
        super(0);
    }
}

final class CrosswindJunction2Map extends CrosswindJunctionMapDefinition {
    CrosswindJunction2Map() {
        super(1);
    }
}

final class CrosswindJunction3Map extends CrosswindJunctionMapDefinition {
    CrosswindJunction3Map() {
        super(2);
    }
}

final class CrosswindJunction4Map extends CrosswindJunctionMapDefinition {
    CrosswindJunction4Map() {
        super(3);
    }
}

final class CrosswindJunction5Map extends CrosswindJunctionMapDefinition {
    CrosswindJunction5Map() {
        super(4);
    }
}
