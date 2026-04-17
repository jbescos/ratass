package com.github.jbescos.gameplay.maps;

import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.ArenaShape;
import com.github.jbescos.gameplay.SpawnPoint;

final class KnifeEdgeMap extends AbstractArenaMapDefinition {
    KnifeEdgeMap() {
        super("knife-edge", "Knife Edge");
    }

    @Override
    protected void define(ArenaMap.Builder builder) {
        float bridgeWidth = 15.6f;
        float bridgeHeight = 5.2f;
        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(-10.5f, 0f, 11.8f, 11.6f))
                .solid(ArenaShape.rectangle(10.5f, 0f, 11.8f, 11.6f))
                .solid(ArenaShape.rectangle(-4.8f, 0f, 5.2f, 6.2f))
                .solid(ArenaShape.rectangle(4.8f, 0f, 5.2f, 6.2f))
                .solid(ArenaShape.rectangle(0f, 0f, bridgeWidth, bridgeHeight));

        builder.spawn(SpawnPoint.facingPoint(-12.8f, -3.6f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-12.8f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-12.8f, 3.6f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-8.2f, -2.1f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-8.2f, 2.1f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-6f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(6f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(8.2f, -2.1f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(8.2f, 2.1f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(12.8f, -3.6f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(12.8f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(12.8f, 3.6f, 0f, 0f));

        builder.recoveryPoint(-12.8f, -3.6f)
                .recoveryPoint(-12.8f, 0f)
                .recoveryPoint(-12.8f, 3.6f)
                .recoveryPoint(-9.6f, -3f)
                .recoveryPoint(-9.6f, 0f)
                .recoveryPoint(-9.6f, 3f)
                .recoveryPoint(-7.8f, -3f)
                .recoveryPoint(-7.8f, 0f)
                .recoveryPoint(-7.8f, 3f)
                .recoveryPoint(-7f, -1.8f)
                .recoveryPoint(-7f, 0f)
                .recoveryPoint(-7f, 1.8f)
                .recoveryPoint(-6f, -1.6f)
                .recoveryPoint(-6f, 1.6f)
                .recoveryPoint(-4.8f, 0f)
                .recoveryPoint(-4.2f, -1f)
                .recoveryPoint(-4.2f, 1f)
                .recoveryPoint(-3f, -1.4f)
                .recoveryPoint(-3f, 0f)
                .recoveryPoint(-3f, 1.4f)
                .recoveryPoint(-1.4f, -1.3f)
                .recoveryPoint(-1.8f, 0f)
                .recoveryPoint(-1.4f, 1.3f)
                .recoveryPoint(0f, -1.3f)
                .recoveryPoint(0f, 0f)
                .recoveryPoint(0f, 1.3f)
                .recoveryPoint(1.4f, -1.3f)
                .recoveryPoint(1.8f, 0f)
                .recoveryPoint(1.4f, 1.3f)
                .recoveryPoint(3f, -1.4f)
                .recoveryPoint(3f, 0f)
                .recoveryPoint(3f, 1.4f)
                .recoveryPoint(4.2f, -1f)
                .recoveryPoint(4.2f, 1f)
                .recoveryPoint(4.8f, 0f)
                .recoveryPoint(6f, -1.6f)
                .recoveryPoint(6f, 1.6f)
                .recoveryPoint(7f, -1.8f)
                .recoveryPoint(7f, 0f)
                .recoveryPoint(7f, 1.8f)
                .recoveryPoint(7.8f, -3f)
                .recoveryPoint(7.8f, 0f)
                .recoveryPoint(7.8f, 3f)
                .recoveryPoint(9.6f, -3f)
                .recoveryPoint(9.6f, 0f)
                .recoveryPoint(9.6f, 3f)
                .recoveryPoint(12.8f, -3.6f)
                .recoveryPoint(12.8f, 0f)
                .recoveryPoint(12.8f, 3.6f);
    }
}

final class DeadfallMap extends AbstractArenaMapDefinition {
    DeadfallMap() {
        super("deadfall", "Deadfall");
    }

    @Override
    protected void define(ArenaMap.Builder builder) {
        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, 40f, 28f))
                .hole(ArenaShape.circle(-11f, 0f, 3f))
                .hole(ArenaShape.circle(0f, 0f, 3.7f))
                .hole(ArenaShape.circle(11f, 0f, 3f))
                .hole(ArenaShape.circle(-6.2f, 8f, 2.3f))
                .hole(ArenaShape.circle(6.2f, -8f, 2.3f));

        addCardinalSpawns(builder, 13.2f, 9.4f);
        builder.spawn(SpawnPoint.facingPoint(-13.8f, -7.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-13.8f, 7.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(13.8f, -7.8f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(13.8f, 7.8f, 0f, 0f));

        builder.recoveryPoint(-15.2f, 0f)
                .recoveryPoint(-13.2f, -6.8f)
                .recoveryPoint(-13.2f, 6.8f)
                .recoveryPoint(-10.2f, -7.4f)
                .recoveryPoint(-10.2f, 7.4f)
                .recoveryPoint(-7.2f, -5f)
                .recoveryPoint(-7.2f, 0f)
                .recoveryPoint(-7.2f, 5f)
                .recoveryPoint(-2.8f, -8.8f)
                .recoveryPoint(-2.8f, 8.8f)
                .recoveryPoint(0f, -11.2f)
                .recoveryPoint(0f, -6.9f)
                .recoveryPoint(0f, 6.9f)
                .recoveryPoint(0f, 11.2f)
                .recoveryPoint(2.8f, -8.8f)
                .recoveryPoint(2.8f, 8.8f)
                .recoveryPoint(7.2f, -5f)
                .recoveryPoint(7.2f, 0f)
                .recoveryPoint(7.2f, 5f)
                .recoveryPoint(10.2f, -7.4f)
                .recoveryPoint(10.2f, 7.4f)
                .recoveryPoint(13.2f, -6.8f)
                .recoveryPoint(13.2f, 6.8f)
                .recoveryPoint(15.2f, 0f);
    }
}

final class SwitchbackMap extends AbstractArenaMapDefinition {
    SwitchbackMap() {
        super("switchback", "Switchback");
    }

    @Override
    protected void define(ArenaMap.Builder builder) {
        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(-12f, -7.5f, 14.0f, 8.8f))
                .solid(ArenaShape.rectangle(-4.1f, -1.6f, 8.2f, 5.0f))
                .solid(ArenaShape.rectangle(-6.25f, -3.4f, 4.4f, 3.8f))
                .solid(ArenaShape.rectangle(-1.2f, -0.5f, 6.2f, 3.2f))
                .solid(ArenaShape.rectangle(0f, 1.4f, 6.2f, 13.0f))
                .solid(ArenaShape.rectangle(1.35f, 3.65f, 6.2f, 3.2f))
                .solid(ArenaShape.rectangle(4.1f, 4.3f, 8.2f, 5.0f))
                .solid(ArenaShape.rectangle(12f, 9f, 14.0f, 8.8f));

        builder.spawn(SpawnPoint.facingPoint(-14.2f, -7.5f, -7f, -3f))
                .spawn(SpawnPoint.facingPoint(-11f, -7.1f, -5f, -1f))
                .spawn(SpawnPoint.facingPoint(-8f, -5.3f, -3f, 0f))
                .spawn(SpawnPoint.facingPoint(-6.3f, -3.8f, -2.4f, -0.4f))
                .spawn(SpawnPoint.facingPoint(-4.6f, -1.8f, 0f, 1.4f))
                .spawn(SpawnPoint.facingPoint(-1.4f, -0.2f, 0.8f, 1.8f))
                .spawn(SpawnPoint.facingPoint(1.8f, 2.8f, 3.8f, 4.6f))
                .spawn(SpawnPoint.facingPoint(4.6f, 4.1f, 0f, 1.4f))
                .spawn(SpawnPoint.facingPoint(5.8f, 7f, 8.6f, 7.8f))
                .spawn(SpawnPoint.facingPoint(8f, 6.3f, 3f, 3f))
                .spawn(SpawnPoint.facingPoint(11f, 8.2f, 5f, 4f))
                .spawn(SpawnPoint.facingPoint(14.2f, 8.8f, 7f, 5f));

        builder.recoveryPoint(-13.5f, -8.3f)
                .recoveryPoint(-13.5f, -6.5f)
                .recoveryPoint(-11.2f, -7.8f)
                .recoveryPoint(-10.4f, -5.9f)
                .recoveryPoint(-8.4f, -6f)
                .recoveryPoint(-7.2f, -4.9f)
                .recoveryPoint(-6.4f, -3.5f)
                .recoveryPoint(-6f, -2f)
                .recoveryPoint(-4.9f, -2.6f)
                .recoveryPoint(-4.1f, -1.6f)
                .recoveryPoint(-3f, -1f)
                .recoveryPoint(-2.6f, 0.3f)
                .recoveryPoint(-1.4f, -0.3f)
                .recoveryPoint(-1f, 0.9f)
                .recoveryPoint(0f, 0f)
                .recoveryPoint(0f, 1.6f)
                .recoveryPoint(0.8f, 0.9f)
                .recoveryPoint(1.2f, 2.4f)
                .recoveryPoint(1.8f, 3.6f)
                .recoveryPoint(2.8f, 3f)
                .recoveryPoint(3.4f, 4.4f)
                .recoveryPoint(4.6f, 4.8f)
                .recoveryPoint(5.4f, 4f)
                .recoveryPoint(5.8f, 6f)
                .recoveryPoint(6.8f, 6.2f)
                .recoveryPoint(8.2f, 7f)
                .recoveryPoint(9.6f, 7.8f)
                .recoveryPoint(11.2f, 8.6f)
                .recoveryPoint(13.2f, 9.2f);
    }
}

final class LastStandMap extends AbstractArenaMapDefinition {
    LastStandMap() {
        super("last-stand", "Last Stand");
    }

    @Override
    protected void define(ArenaMap.Builder builder) {
        float spokeWidth = 4.2f;
        float spokeLength = 7.1f;
        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, 10.4f, 10.4f))
                .solid(ArenaShape.rectangle(0f, 11.2f, 8.6f, 8.6f))
                .solid(ArenaShape.rectangle(0f, -11.2f, 8.6f, 8.6f))
                .solid(ArenaShape.rectangle(-11.2f, 0f, 8.6f, 8.6f))
                .solid(ArenaShape.rectangle(11.2f, 0f, 8.6f, 8.6f))
                .solid(ArenaShape.rectangle(0f, 5.8f, spokeWidth, spokeLength))
                .solid(ArenaShape.rectangle(0f, -5.8f, spokeWidth, spokeLength))
                .solid(ArenaShape.rectangle(-5.8f, 0f, spokeLength, spokeWidth))
                .solid(ArenaShape.rectangle(5.8f, 0f, spokeLength, spokeWidth));

        builder.spawn(SpawnPoint.facingPoint(0f, 11.2f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, 8.2f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, -11.2f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, -8.2f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-11.2f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-8.2f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(11.2f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(8.2f, 0f, 0f, 0f));

        builder.recoveryPoint(0f, 0f)
                .recoveryPoint(-2.4f, -2.4f)
                .recoveryPoint(-2.4f, 2.4f)
                .recoveryPoint(2.4f, -2.4f)
                .recoveryPoint(2.4f, 2.4f)
                .recoveryPoint(0f, 3.4f)
                .recoveryPoint(0f, -3.4f)
                .recoveryPoint(-3.4f, 0f)
                .recoveryPoint(3.4f, 0f)
                .recoveryPoint(-2.4f, 5.8f)
                .recoveryPoint(2.4f, 5.8f)
                .recoveryPoint(-2.4f, -5.8f)
                .recoveryPoint(2.4f, -5.8f)
                .recoveryPoint(-5.8f, -2.4f)
                .recoveryPoint(-5.8f, 2.4f)
                .recoveryPoint(5.8f, -2.4f)
                .recoveryPoint(5.8f, 2.4f)
                .recoveryPoint(0f, 8.2f)
                .recoveryPoint(0f, -8.2f)
                .recoveryPoint(-8.2f, 0f)
                .recoveryPoint(8.2f, 0f)
                .recoveryPoint(-2.8f, 11.2f)
                .recoveryPoint(0f, 11.2f)
                .recoveryPoint(2.8f, 11.2f)
                .recoveryPoint(-2.8f, -11.2f)
                .recoveryPoint(0f, -11.2f)
                .recoveryPoint(2.8f, -11.2f)
                .recoveryPoint(-11.2f, -2.8f)
                .recoveryPoint(-11.2f, 0f)
                .recoveryPoint(-11.2f, 2.8f)
                .recoveryPoint(11.2f, -2.8f)
                .recoveryPoint(11.2f, 0f)
                .recoveryPoint(11.2f, 2.8f);
    }
}
