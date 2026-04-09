package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.utils.Array;
import com.github.jbescos.gameplay.ArenaMap;

public final class ArenaMaps {
    private ArenaMaps() {
    }

    public static Array<ArenaMap> createDefaultSet() {
        Array<ArenaMap> maps = new Array<ArenaMap>();

        addAll(maps,
                new CrosswindJunction5Map(),
                new SplitShift5Map());

        addAll(maps,
                new DonutBowl4Map(),
                new TwinCrater4Map(),
                new FrameRing4Map(),
                new CausewayClash4Map(),
                new CoreBreach5Map());

        addAll(maps,
                new PillboxLanes5Map(),
                new SatelliteCrown4Map(),
                new KnifeEdgeMap(),
                new DeadfallMap(),
                new SwitchbackMap(),
                new LastStandMap());

        return maps;
    }

    private static void addAll(Array<ArenaMap> maps, ArenaMapDefinition... definitions) {
        for (int i = 0; i < definitions.length; i++) {
            maps.add(definitions[i].create());
        }
    }
}
