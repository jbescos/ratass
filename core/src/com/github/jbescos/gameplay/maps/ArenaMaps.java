package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.utils.Array;
import com.github.jbescos.gameplay.ArenaMap;

public final class ArenaMaps {
    private ArenaMaps() {
    }

    public static Array<ArenaMap> createDefaultSet() {
        Array<ArenaMap> maps = new Array<ArenaMap>();

        // Add new fixed maps here after creating a matching ArenaMapDefinition class.
        addAll(maps,
                new BoilerDeck1Map(),
                new BoilerDeck2Map(),
                new BoilerDeck3Map(),
                new BoilerDeck4Map(),
                new BoilerDeck5Map());

        addAll(maps,
                new CrosswindJunction1Map(),
                new CrosswindJunction2Map(),
                new CrosswindJunction3Map(),
                new CrosswindJunction4Map(),
                new CrosswindJunction5Map());

        addAll(maps,
                new SplitShift1Map(),
                new SplitShift2Map(),
                new SplitShift3Map(),
                new SplitShift4Map(),
                new SplitShift5Map());

        addAll(maps,
                new DonutBowl1Map(),
                new DonutBowl2Map(),
                new DonutBowl3Map(),
                new DonutBowl4Map(),
                new DonutBowl5Map());

        addAll(maps,
                new TwinCrater1Map(),
                new TwinCrater2Map(),
                new TwinCrater3Map(),
                new TwinCrater4Map(),
                new TwinCrater5Map());

        addAll(maps,
                new FrameRing1Map(),
                new FrameRing2Map(),
                new FrameRing3Map(),
                new FrameRing4Map(),
                new FrameRing5Map());

        addAll(maps,
                new CausewayClash1Map(),
                new CausewayClash2Map(),
                new CausewayClash3Map(),
                new CausewayClash4Map(),
                new CausewayClash5Map());

        addAll(maps,
                new CoreBreach1Map(),
                new CoreBreach2Map(),
                new CoreBreach3Map(),
                new CoreBreach4Map(),
                new CoreBreach5Map());

        addAll(maps,
                new PillboxLanes1Map(),
                new PillboxLanes2Map(),
                new PillboxLanes3Map(),
                new PillboxLanes4Map(),
                new PillboxLanes5Map());

        addAll(maps,
                new SatelliteCrown1Map(),
                new SatelliteCrown2Map(),
                new SatelliteCrown3Map(),
                new SatelliteCrown4Map(),
                new SatelliteCrown5Map());

        return maps;
    }

    private static void addAll(Array<ArenaMap> maps, ArenaMapDefinition... definitions) {
        for (int i = 0; i < definitions.length; i++) {
            maps.add(definitions[i].create());
        }
    }
}
