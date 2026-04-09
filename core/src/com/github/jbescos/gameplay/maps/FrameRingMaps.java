package com.github.jbescos.gameplay.maps;

import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;

abstract class FrameRingMapDefinition extends AbstractArenaMapDefinition {
    private final int variant;

    protected FrameRingMapDefinition(int variant) {
        super(variantId("frame-ring", variant), variantName("Frame Ring", variant));
        this.variant = variant;
    }

    @Override
    protected final void define(ArenaMap.Builder builder) {
        float outerWidth = 46f + variant * 2.6f;
        float outerHeight = 30f + variant * 1.5f;
        float innerWidth = 15.5f + variant * 1.6f;
        float innerHeight = 10.5f + variant * 1.0f;
        if (variant % 2 == 0) {
            innerWidth += 1.6f;
        } else {
            innerHeight += 1.2f;
        }

        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, outerWidth, outerHeight))
                .hole(ArenaShape.rectangle(0f, 0f, innerWidth, innerHeight));

        addCardinalSpawns(
                builder,
                innerWidth * 0.5f + 3.4f,
                innerHeight * 0.5f + 2.6f);
        addGridRecoveryPointsAvoidingRectangle(
                builder,
                outerWidth * 0.36f,
                outerHeight * 0.34f,
                6,
                5,
                innerWidth * 0.5f,
                innerHeight * 0.5f,
                1.2f);
    }
}

final class FrameRing1Map extends FrameRingMapDefinition {
    FrameRing1Map() {
        super(0);
    }
}

final class FrameRing2Map extends FrameRingMapDefinition {
    FrameRing2Map() {
        super(1);
    }
}

final class FrameRing3Map extends FrameRingMapDefinition {
    FrameRing3Map() {
        super(2);
    }
}

final class FrameRing4Map extends FrameRingMapDefinition {
    FrameRing4Map() {
        super(3);
    }
}

final class FrameRing5Map extends FrameRingMapDefinition {
    FrameRing5Map() {
        super(4);
    }
}
