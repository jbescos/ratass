package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.math.Vector2;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;

abstract class PillboxLanesMapDefinition extends AbstractArenaMapDefinition {
    private final int variant;

    protected PillboxLanesMapDefinition(int variant) {
        super(variantId("pillbox-lanes", variant), variantName("Pillbox Lanes", variant));
        this.variant = variant;
    }

    @Override
    protected final void define(ArenaMap.Builder builder) {
        float width = 48f + variant * 2.0f;
        float height = 30f + variant * 1.4f;
        float holeRadius = 2.6f + variant * 0.18f;
        float holeOffsetX = 10.2f + variant * 0.5f;
        float holeOffsetY = 6.4f + variant * 0.4f;
        Vector2 holeNorthWest = new Vector2(-holeOffsetX, holeOffsetY);
        Vector2 holeNorthEast = new Vector2(holeOffsetX, holeOffsetY);
        Vector2 holeSouthWest = new Vector2(-holeOffsetX, -holeOffsetY);
        Vector2 holeSouthEast = new Vector2(holeOffsetX, -holeOffsetY);

        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, width, height))
                .hole(ArenaShape.circle(holeNorthWest.x, holeNorthWest.y, holeRadius))
                .hole(ArenaShape.circle(holeNorthEast.x, holeNorthEast.y, holeRadius))
                .hole(ArenaShape.circle(holeSouthWest.x, holeSouthWest.y, holeRadius))
                .hole(ArenaShape.circle(holeSouthEast.x, holeSouthEast.y, holeRadius));

        addCardinalSpawns(builder, width * 0.30f, height * 0.30f);
        addGridRecoveryPointsAvoidingCircles(
                builder,
                width * 0.36f,
                height * 0.32f,
                6,
                5,
                holeRadius + 1.15f,
                holeNorthWest,
                holeNorthEast,
                holeSouthWest,
                holeSouthEast);
        builder.recoveryPoint(0f, 0f);
    }
}

final class PillboxLanes1Map extends PillboxLanesMapDefinition {
    PillboxLanes1Map() {
        super(0);
    }
}

final class PillboxLanes2Map extends PillboxLanesMapDefinition {
    PillboxLanes2Map() {
        super(1);
    }
}

final class PillboxLanes3Map extends PillboxLanesMapDefinition {
    PillboxLanes3Map() {
        super(2);
    }
}

final class PillboxLanes4Map extends PillboxLanesMapDefinition {
    PillboxLanes4Map() {
        super(3);
    }
}

final class PillboxLanes5Map extends PillboxLanesMapDefinition {
    PillboxLanes5Map() {
        super(4);
    }
}
