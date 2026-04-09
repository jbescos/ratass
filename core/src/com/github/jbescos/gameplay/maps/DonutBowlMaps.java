package com.github.jbescos.gameplay.maps;

import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;

abstract class DonutBowlMapDefinition extends AbstractArenaMapDefinition {
    private final int variant;

    protected DonutBowlMapDefinition(int variant) {
        super(variantId("donut-bowl", variant), variantName("Donut Bowl", variant));
        this.variant = variant;
    }

    @Override
    protected final void define(ArenaMap.Builder builder) {
        float outerRadius = 13f + variant * 1.1f;
        float holeRadius = 4.2f + variant * 0.25f + (variant % 2) * 0.35f;
        float ringRadius = (outerRadius + holeRadius) * 0.5f;

        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.circle(0f, 0f, outerRadius))
                .hole(ArenaShape.circle(0f, 0f, holeRadius));

        addCardinalSpawns(builder, ringRadius, ringRadius);
        addRingRecoveryPoints(builder, ringRadius, 12 + variant * 2);
        addRingRecoveryPoints(builder, ringRadius * 0.82f, 8 + variant);
    }
}

final class DonutBowl1Map extends DonutBowlMapDefinition {
    DonutBowl1Map() {
        super(0);
    }
}

final class DonutBowl2Map extends DonutBowlMapDefinition {
    DonutBowl2Map() {
        super(1);
    }
}

final class DonutBowl3Map extends DonutBowlMapDefinition {
    DonutBowl3Map() {
        super(2);
    }
}

final class DonutBowl4Map extends DonutBowlMapDefinition {
    DonutBowl4Map() {
        super(3);
    }
}

final class DonutBowl5Map extends DonutBowlMapDefinition {
    DonutBowl5Map() {
        super(4);
    }
}
