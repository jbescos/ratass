package com.github.jbescos.gameplay;

import com.badlogic.gdx.utils.Array;

public final class MapProgression {
    private final Array<ArenaMap> maps = new Array<ArenaMap>();
    private int currentIndex;

    public MapProgression(Array<ArenaMap> maps) {
        if (maps == null || maps.size == 0) {
            throw new IllegalArgumentException("Map progression requires at least one map.");
        }
        this.maps.addAll(maps);
    }

    public ArenaMap getCurrentMap() {
        return maps.get(currentIndex);
    }

    public ArenaMap getNextMap() {
        return maps.get((currentIndex + 1) % maps.size);
    }

    public void advanceIfPlayerWon(boolean playerWon) {
        if (playerWon) {
            currentIndex = (currentIndex + 1) % maps.size;
        }
    }

    public int getCurrentMapNumber() {
        return currentIndex + 1;
    }

    public int getMapCount() {
        return maps.size;
    }
}
