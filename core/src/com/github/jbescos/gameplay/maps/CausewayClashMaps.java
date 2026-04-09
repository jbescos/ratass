package com.github.jbescos.gameplay.maps;

import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;
import com.github.jbescos.gameplay.SpawnPoint;

abstract class CausewayClashMapDefinition extends AbstractArenaMapDefinition {
    private final int variant;

    protected CausewayClashMapDefinition(int variant) {
        super(variantId("causeway-clash", variant), variantName("Causeway Clash", variant));
        this.variant = variant;
    }

    @Override
    protected final void define(ArenaMap.Builder builder) {
        float spineWidth = 36f + variant * 2.4f;
        float spineHeight = 11f + variant * 0.8f;
        float podWidth = 11f + variant * 0.9f;
        float podHeight = 10.5f + variant * 0.7f;
        float podOffsetX = 11.5f + variant * 0.9f;
        float podOffsetY = 7.2f + (variant % 3) * 0.8f;

        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, spineWidth, spineHeight))
                .solid(ArenaShape.rectangle(-podOffsetX, podOffsetY, podWidth, podHeight))
                .solid(ArenaShape.rectangle(podOffsetX, -podOffsetY, podWidth, podHeight));

        if (variant >= 2) {
            builder.solid(ArenaShape.rectangle(-podOffsetX, podOffsetY * 0.15f, podWidth * 0.70f, 6.4f))
                    .solid(ArenaShape.rectangle(podOffsetX, -podOffsetY * 0.15f, podWidth * 0.70f, 6.4f));
        }

        builder.spawn(SpawnPoint.facingPoint(-spineWidth * 0.34f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(spineWidth * 0.34f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-podOffsetX, podOffsetY, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(podOffsetX, -podOffsetY, 0f, 0f));

        builder.recoveryPoint(-podOffsetX, podOffsetY)
                .recoveryPoint(-spineWidth * 0.22f, 0f)
                .recoveryPoint(0f, 0f)
                .recoveryPoint(spineWidth * 0.22f, 0f)
                .recoveryPoint(podOffsetX, -podOffsetY);
        addGridRecoveryPoints(builder, spineWidth * 0.28f, spineHeight * 0.24f, 4, 2);
    }
}

final class CausewayClash1Map extends CausewayClashMapDefinition {
    CausewayClash1Map() {
        super(0);
    }
}

final class CausewayClash2Map extends CausewayClashMapDefinition {
    CausewayClash2Map() {
        super(1);
    }
}

final class CausewayClash3Map extends CausewayClashMapDefinition {
    CausewayClash3Map() {
        super(2);
    }
}

final class CausewayClash4Map extends CausewayClashMapDefinition {
    CausewayClash4Map() {
        super(3);
    }
}

final class CausewayClash5Map extends CausewayClashMapDefinition {
    CausewayClash5Map() {
        super(4);
    }
}
