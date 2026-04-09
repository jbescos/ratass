package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.math.Vector2;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;

abstract class TwinCraterMapDefinition extends AbstractArenaMapDefinition {
    private final int variant;

    protected TwinCraterMapDefinition(int variant) {
        super(variantId("twin-crater", variant), variantName("Twin Crater", variant));
        this.variant = variant;
    }

    @Override
    protected final void define(ArenaMap.Builder builder) {
        float width = 42f + variant * 2.4f;
        float height = 27f + variant * 1.2f;
        float holeRadius = 3.2f + variant * 0.24f;
        float holeOffset = 7.2f + variant * 0.55f;
        boolean verticalLayout = variant % 2 == 1;
        Vector2 craterA = verticalLayout
                ? new Vector2(0f, holeOffset * 0.78f)
                : new Vector2(-holeOffset, 0f);
        Vector2 craterB = verticalLayout
                ? new Vector2(0f, -holeOffset * 0.78f)
                : new Vector2(holeOffset, 0f);

        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, width, height))
                .hole(ArenaShape.circle(craterA.x, craterA.y, holeRadius))
                .hole(ArenaShape.circle(craterB.x, craterB.y, holeRadius));

        addCardinalSpawns(builder, width * 0.30f, height * 0.30f);
        addGridRecoveryPointsAvoidingCircles(
                builder,
                width * 0.34f,
                height * 0.32f,
                5,
                4,
                holeRadius + 1.35f,
                craterA,
                craterB);
        builder.recoveryPoint(0f, height * 0.32f)
                .recoveryPoint(0f, -height * 0.32f);
    }
}

final class TwinCrater1Map extends TwinCraterMapDefinition {
    TwinCrater1Map() {
        super(0);
    }
}

final class TwinCrater2Map extends TwinCraterMapDefinition {
    TwinCrater2Map() {
        super(1);
    }
}

final class TwinCrater3Map extends TwinCraterMapDefinition {
    TwinCrater3Map() {
        super(2);
    }
}

final class TwinCrater4Map extends TwinCraterMapDefinition {
    TwinCrater4Map() {
        super(3);
    }
}

final class TwinCrater5Map extends TwinCraterMapDefinition {
    TwinCrater5Map() {
        super(4);
    }
}
