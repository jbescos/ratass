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
        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(-10.5f, 0f, 11.5f, 11f))
                .solid(ArenaShape.rectangle(10.5f, 0f, 11.5f, 11f))
                .solid(ArenaShape.rectangle(0f, 0f, 10.5f, 3.2f));

        builder.spawn(SpawnPoint.facingPoint(-10.5f, -2.7f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-10.5f, 2.7f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(10.5f, -2.7f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(10.5f, 2.7f, 0f, 0f));

        builder.recoveryPoint(-10.5f, -2.8f)
                .recoveryPoint(-10.5f, 0f)
                .recoveryPoint(-10.5f, 2.8f)
                .recoveryPoint(-4.4f, 0f)
                .recoveryPoint(0f, 0f)
                .recoveryPoint(4.4f, 0f)
                .recoveryPoint(10.5f, -2.8f)
                .recoveryPoint(10.5f, 0f)
                .recoveryPoint(10.5f, 2.8f);
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
                .hole(ArenaShape.circle(-11f, 0f, 3.3f))
                .hole(ArenaShape.circle(0f, 0f, 4.2f))
                .hole(ArenaShape.circle(11f, 0f, 3.3f))
                .hole(ArenaShape.circle(-5.5f, 7.6f, 2.5f))
                .hole(ArenaShape.circle(5.5f, -7.6f, 2.5f));

        addCardinalSpawns(builder, 13.2f, 9.4f);
        builder.recoveryPoint(-15f, 0f)
                .recoveryPoint(-7f, -8f)
                .recoveryPoint(-7f, 8f)
                .recoveryPoint(0f, -11f)
                .recoveryPoint(0f, 11f)
                .recoveryPoint(7f, -8f)
                .recoveryPoint(7f, 8f)
                .recoveryPoint(15f, 0f);
    }
}

final class SwitchbackMap extends AbstractArenaMapDefinition {
    SwitchbackMap() {
        super("switchback", "Switchback");
    }

    @Override
    protected void define(ArenaMap.Builder builder) {
        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(-12f, -7.5f, 13f, 8f))
                .solid(ArenaShape.rectangle(-4f, -1.5f, 7f, 4.2f))
                .solid(ArenaShape.rectangle(4f, 4.2f, 7f, 4.2f))
                .solid(ArenaShape.rectangle(12f, 9f, 13f, 8f))
                .solid(ArenaShape.rectangle(0f, 1.4f, 4.5f, 12f));

        builder.spawn(SpawnPoint.facingPoint(-14f, -7.2f, -6f, -1f))
                .spawn(SpawnPoint.facingPoint(-10f, -7.2f, -4f, 1f))
                .spawn(SpawnPoint.facingPoint(10f, 8.8f, 4f, 3f))
                .spawn(SpawnPoint.facingPoint(14f, 8.8f, 6f, 1f));

        builder.recoveryPoint(-12f, -7.5f)
                .recoveryPoint(-8f, -6f)
                .recoveryPoint(-4f, -2f)
                .recoveryPoint(0f, 0f)
                .recoveryPoint(4f, 3.8f)
                .recoveryPoint(8f, 6.2f)
                .recoveryPoint(12f, 9f);
    }
}

final class LastStandMap extends AbstractArenaMapDefinition {
    LastStandMap() {
        super("last-stand", "Last Stand");
    }

    @Override
    protected void define(ArenaMap.Builder builder) {
        builder.focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, 9.2f, 9.2f))
                .solid(ArenaShape.rectangle(0f, 11.2f, 7.8f, 7.8f))
                .solid(ArenaShape.rectangle(0f, -11.2f, 7.8f, 7.8f))
                .solid(ArenaShape.rectangle(-11.2f, 0f, 7.8f, 7.8f))
                .solid(ArenaShape.rectangle(11.2f, 0f, 7.8f, 7.8f))
                .solid(ArenaShape.rectangle(0f, 5.8f, 2.6f, 5f))
                .solid(ArenaShape.rectangle(0f, -5.8f, 2.6f, 5f))
                .solid(ArenaShape.rectangle(-5.8f, 0f, 5f, 2.6f))
                .solid(ArenaShape.rectangle(5.8f, 0f, 5f, 2.6f));

        builder.spawn(SpawnPoint.facingPoint(0f, 11.2f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, -11.2f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-11.2f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(11.2f, 0f, 0f, 0f));

        builder.recoveryPoint(0f, 0f)
                .recoveryPoint(0f, 5.6f)
                .recoveryPoint(0f, -5.6f)
                .recoveryPoint(-5.6f, 0f)
                .recoveryPoint(5.6f, 0f)
                .recoveryPoint(0f, 11.2f)
                .recoveryPoint(0f, -11.2f)
                .recoveryPoint(-11.2f, 0f)
                .recoveryPoint(11.2f, 0f);
    }
}
