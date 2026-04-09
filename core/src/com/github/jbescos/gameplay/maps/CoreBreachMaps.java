package com.github.jbescos.gameplay.maps;

import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;

abstract class CoreBreachMapDefinition extends AbstractArenaMapDefinition {
    private final int variant;

    protected CoreBreachMapDefinition(int variant) {
        super(variantId("core-breach", variant), variantName("Core Breach", variant));
        this.variant = variant;
    }

    @Override
    protected final void define(ArenaMap.Builder builder) {
        float horizontalWidth = 38f + variant * 2.1f;
        float horizontalHeight = 11f + (variant % 2) * 0.8f;
        float verticalWidth = 17f + variant * 0.8f;
        float verticalHeight = 28f + variant * 1.8f;

        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, horizontalWidth, horizontalHeight))
                .solid(ArenaShape.rectangle(0f, 0f, verticalWidth, verticalHeight));

        addCardinalSpawns(builder, horizontalWidth * 0.30f, verticalHeight * 0.30f);

        if (variant % 2 == 0) {
            float breachRadius = 3.0f + variant * 0.25f;
            builder.hole(ArenaShape.circle(0f, 0f, breachRadius));
            addRingRecoveryPoints(builder, breachRadius + 3.7f, 10 + variant * 2);
        } else {
            float breachWidth = 6.4f + variant * 0.8f;
            float breachHeight = 5.2f + variant * 0.6f;
            builder.hole(ArenaShape.rectangle(0f, 0f, breachWidth, breachHeight));
            builder.recoveryPoint(-(breachWidth * 0.5f + 2.4f), 0f)
                    .recoveryPoint(0f, -(breachHeight * 0.5f + 2.4f))
                    .recoveryPoint(breachWidth * 0.5f + 2.4f, 0f)
                    .recoveryPoint(0f, breachHeight * 0.5f + 2.4f);
        }

        builder.recoveryPoint(0f, verticalHeight * 0.32f)
                .recoveryPoint(0f, -verticalHeight * 0.32f)
                .recoveryPoint(horizontalWidth * 0.32f, 0f)
                .recoveryPoint(-horizontalWidth * 0.32f, 0f);
    }
}

final class CoreBreach1Map extends CoreBreachMapDefinition {
    CoreBreach1Map() {
        super(0);
    }
}

final class CoreBreach2Map extends CoreBreachMapDefinition {
    CoreBreach2Map() {
        super(1);
    }
}

final class CoreBreach3Map extends CoreBreachMapDefinition {
    CoreBreach3Map() {
        super(2);
    }
}

final class CoreBreach4Map extends CoreBreachMapDefinition {
    CoreBreach4Map() {
        super(3);
    }
}

final class CoreBreach5Map extends CoreBreachMapDefinition {
    CoreBreach5Map() {
        super(4);
    }
}
