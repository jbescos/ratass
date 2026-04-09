package com.github.jbescos.gameplay.maps;

import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;
import com.github.jbescos.gameplay.SpawnPoint;

abstract class SplitShiftMapDefinition extends AbstractArenaMapDefinition {
    private final int variant;

    protected SplitShiftMapDefinition(int variant) {
        super(variantId("split-shift", variant), variantName("Split Shift", variant));
        this.variant = variant;
    }

    @Override
    protected final void define(ArenaMap.Builder builder) {
        float islandOffset = 10.4f + variant * 0.9f;
        float islandWidth = 13.2f + variant * 0.8f;
        float islandHeight = 14.4f + variant * 1.0f;
        float bridgeWidth = 9.4f + variant * 0.8f;
        float bridgeHeight = 5.5f + (variant % 3) * 0.6f;
        float bridgeY = (variant - 2) * 0.9f;

        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(-islandOffset, 0f, islandWidth, islandHeight))
                .solid(ArenaShape.rectangle(islandOffset, 0f, islandWidth, islandHeight))
                .solid(ArenaShape.rectangle(0f, bridgeY, bridgeWidth, bridgeHeight));

        if (variant >= 3) {
            builder.solid(ArenaShape.rectangle(0f, -bridgeY, bridgeWidth * 0.72f, bridgeHeight * 0.72f));
        }

        builder.spawn(SpawnPoint.facingPoint(-islandOffset, -islandHeight * 0.24f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-islandOffset, islandHeight * 0.24f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(islandOffset, -islandHeight * 0.24f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(islandOffset, islandHeight * 0.24f, 0f, 0f));

        builder.recoveryPoint(-islandOffset, -islandHeight * 0.30f)
                .recoveryPoint(-islandOffset, 0f)
                .recoveryPoint(-islandOffset, islandHeight * 0.30f)
                .recoveryPoint(-bridgeWidth * 0.45f, bridgeY)
                .recoveryPoint(0f, 0f)
                .recoveryPoint(bridgeWidth * 0.45f, bridgeY)
                .recoveryPoint(islandOffset, -islandHeight * 0.30f)
                .recoveryPoint(islandOffset, 0f)
                .recoveryPoint(islandOffset, islandHeight * 0.30f);
    }
}

final class SplitShift1Map extends SplitShiftMapDefinition {
    SplitShift1Map() {
        super(0);
    }
}

final class SplitShift2Map extends SplitShiftMapDefinition {
    SplitShift2Map() {
        super(1);
    }
}

final class SplitShift3Map extends SplitShiftMapDefinition {
    SplitShift3Map() {
        super(2);
    }
}

final class SplitShift4Map extends SplitShiftMapDefinition {
    SplitShift4Map() {
        super(3);
    }
}

final class SplitShift5Map extends SplitShiftMapDefinition {
    SplitShift5Map() {
        super(4);
    }
}
