package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public final class MapProgression {
    private final ArrayList<ArenaMap> maps = new ArrayList<ArenaMap>();
    private final Random random;
    private int currentIndex;

    public MapProgression(Array<ArenaMap> maps) {
        this(maps, null);
    }

    public MapProgression(Array<ArenaMap> maps, Random random) {
        if (maps == null || maps.size == 0) {
            throw new IllegalArgumentException("Map progression requires at least one map.");
        }
        for (int i = 0; i < maps.size; i++) {
            this.maps.add(maps.get(i));
        }
        this.random = random;
        shuffleCycle(null);
    }

    public ArenaMap getCurrentMap() {
        return maps.get(currentIndex);
    }

    public ArenaMap getNextMap() {
        if (maps.size() == 1) {
            return maps.get(0);
        }
        if (currentIndex + 1 < maps.size()) {
            return maps.get(currentIndex + 1);
        }
        return previewFirstMapOfNextCycle(maps.get(currentIndex));
    }

    public void advance() {
        if (maps.size() == 1) {
            return;
        }

        if (currentIndex < maps.size() - 1) {
            currentIndex++;
            return;
        }

        ArenaMap lastMap = maps.get(currentIndex);
        shuffleCycle(lastMap);
        currentIndex = 0;
    }

    public int getCurrentMapNumber() {
        return currentIndex + 1;
    }

    public int getMapCount() {
        return maps.size();
    }

    private void shuffleCycle(ArenaMap lastMap) {
        if (random == null) {
            Collections.shuffle(maps);
        } else {
            Collections.shuffle(maps, random);
        }

        if (lastMap != null && maps.size() > 1 && maps.get(0) == lastMap) {
            int swapIndex = 1 + randomIndex(maps.size() - 1);
            Collections.swap(maps, 0, swapIndex);
        }
    }

    private ArenaMap previewFirstMapOfNextCycle(ArenaMap lastMap) {
        ArrayList<ArenaMap> preview = new ArrayList<ArenaMap>(maps);
        if (random == null) {
            Collections.shuffle(preview);
        } else {
            Collections.shuffle(preview, random);
        }

        if (preview.size() > 1 && preview.get(0) == lastMap) {
            int swapIndex = 1 + randomIndex(preview.size() - 1);
            Collections.swap(preview, 0, swapIndex);
        }

        return preview.get(0);
    }

    private int randomIndex(int boundExclusive) {
        if (random != null) {
            return random.nextInt(boundExclusive);
        }
        return MathUtils.random(boundExclusive - 1);
    }
}
