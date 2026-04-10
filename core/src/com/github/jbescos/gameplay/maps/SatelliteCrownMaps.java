package com.github.jbescos.gameplay.maps;

import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;
import com.github.jbescos.gameplay.SpawnPoint;

abstract class SatelliteCrownMapDefinition extends AbstractArenaMapDefinition {
    private final int variant;

    protected SatelliteCrownMapDefinition(int variant) {
        super(variantId("satellite-crown", variant), variantName("Satellite Crown", variant));
        this.variant = variant;
    }

    @Override
    protected final void define(ArenaMap.Builder builder) {
        float centerRadius = 8.2f + variant * 0.55f;
        float satelliteRadius = 6.4f + variant * 0.35f;
        float satelliteOffsetX = 12.2f + variant * 0.7f;
        float satelliteOffsetY = (variant - 2) * 1.1f;
        float focusY = 0f;
        float topCrownY = centerRadius + satelliteRadius * 0.52f;
        float topCrownRadius = 4.4f + variant * 0.25f;
        if (variant >= 2) {
            float maxY = Math.max(centerRadius, Math.max(satelliteOffsetY + satelliteRadius, topCrownY + topCrownRadius));
            float minY = Math.min(-centerRadius, -satelliteOffsetY - satelliteRadius);
            focusY = (maxY + minY) * 0.5f;
        }

        builder.focusPoint(0f, focusY)
                .solid(ArenaShape.circle(0f, 0f, centerRadius))
                .solid(ArenaShape.circle(-satelliteOffsetX, satelliteOffsetY, satelliteRadius))
                .solid(ArenaShape.circle(satelliteOffsetX, -satelliteOffsetY, satelliteRadius));

        if (variant >= 2) {
            builder.solid(ArenaShape.circle(0f, topCrownY, topCrownRadius));
        }

        builder.spawn(SpawnPoint.facingPoint(-satelliteOffsetX, satelliteOffsetY, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(satelliteOffsetX, -satelliteOffsetY, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, -centerRadius * 0.55f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, centerRadius * 0.55f, 0f, 0f));

        builder.recoveryPoint(0f, 0f)
                .recoveryPoint(-satelliteOffsetX * 0.48f, satelliteOffsetY * 0.48f)
                .recoveryPoint(-satelliteOffsetX, satelliteOffsetY)
                .recoveryPoint(satelliteOffsetX * 0.48f, -satelliteOffsetY * 0.48f)
                .recoveryPoint(satelliteOffsetX, -satelliteOffsetY);
        addRingRecoveryPoints(builder, 0f, 0f, centerRadius * 0.46f, 8);
        addRingRecoveryPoints(builder, -satelliteOffsetX, satelliteOffsetY, satelliteRadius * 0.42f, 6);
        addRingRecoveryPoints(builder, satelliteOffsetX, -satelliteOffsetY, satelliteRadius * 0.42f, 6);
        if (variant >= 2) {
            builder.recoveryPoint(0f, centerRadius * 0.72f)
                    .recoveryPoint(0f, topCrownY);
        }
    }
}

final class SatelliteCrown1Map extends SatelliteCrownMapDefinition {
    SatelliteCrown1Map() {
        super(0);
    }
}

final class SatelliteCrown2Map extends SatelliteCrownMapDefinition {
    SatelliteCrown2Map() {
        super(1);
    }
}

final class SatelliteCrown3Map extends SatelliteCrownMapDefinition {
    SatelliteCrown3Map() {
        super(2);
    }
}

final class SatelliteCrown4Map extends SatelliteCrownMapDefinition {
    SatelliteCrown4Map() {
        super(3);
    }
}

final class SatelliteCrown5Map extends SatelliteCrownMapDefinition {
    SatelliteCrown5Map() {
        super(4);
    }
}
