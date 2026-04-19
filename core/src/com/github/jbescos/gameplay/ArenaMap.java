package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public final class ArenaMap {
    private static final float APPROXIMATE_SAMPLES_PER_WORLD_UNIT = 12f;
    private static final int APPROXIMATE_MIN_GRID_SIZE = 96;
    private static final int APPROXIMATE_MAX_GRID_SIZE = 384;
    private static final float APPROXIMATE_PATH_MARGIN = 0.04f;

    private final String id;
    private final String name;
    private final Vector2 focusPoint = new Vector2();
    private final Array<ArenaShape> solidZones = new Array<ArenaShape>();
    private final Array<ArenaShape> holeZones = new Array<ArenaShape>();
    private final Array<SpawnPoint> spawnPoints = new Array<SpawnPoint>();
    private final Array<Vector2> recoveryPoints = new Array<Vector2>();
    private final Rectangle bounds = new Rectangle();
    private final Vector2 scratchCandidate = new Vector2();
    private final Vector2 scratchAdjusted = new Vector2();
    private final Vector2 scratchBest = new Vector2();
    private final Vector2 scratchGoal = new Vector2();
    private final int approximateHazardWidth;
    private final int approximateHazardHeight;
    private final float approximateHazardCellWidth;
    private final float approximateHazardCellHeight;
    private final float[] approximateHazardSamples;
    private final short[] approximateRecoverySamples;
    private final boolean[] navigationNodeSafe;
    private final boolean[][] navigationNodeVisible;
    private final float[][] navigationNodeTravelCost;
    private final float[] navigationRouteCosts;
    private final int[] navigationRoutePrevious;
    private final boolean[] navigationRouteVisited;
    private float cachedNavigationMargin = -1f;

    private ArenaMap(
            String id,
            String name,
            Vector2 focusPoint,
            Array<ArenaShape> solidZones,
            Array<ArenaShape> holeZones,
            Array<SpawnPoint> spawnPoints,
            Array<Vector2> recoveryPoints) {
        this.id = id;
        this.name = name;
        this.focusPoint.set(focusPoint);

        this.solidZones.addAll(solidZones);
        this.holeZones.addAll(holeZones);
        this.spawnPoints.addAll(spawnPoints);

        for (int i = 0; i < recoveryPoints.size; i++) {
            this.recoveryPoints.add(new Vector2(recoveryPoints.get(i)));
        }

        ArenaShape firstSolid = this.solidZones.first();
        float minX = firstSolid.getMinX();
        float maxX = firstSolid.getMaxX();
        float minY = firstSolid.getMinY();
        float maxY = firstSolid.getMaxY();

        for (int i = 1; i < this.solidZones.size; i++) {
            ArenaShape solid = this.solidZones.get(i);
            minX = Math.min(minX, solid.getMinX());
            maxX = Math.max(maxX, solid.getMaxX());
            minY = Math.min(minY, solid.getMinY());
            maxY = Math.max(maxY, solid.getMaxY());
        }

        bounds.set(minX, minY, maxX - minX, maxY - minY);

        approximateHazardWidth =
                MathUtils.clamp(
                        MathUtils.ceil(bounds.width * APPROXIMATE_SAMPLES_PER_WORLD_UNIT),
                        APPROXIMATE_MIN_GRID_SIZE,
                        APPROXIMATE_MAX_GRID_SIZE);
        approximateHazardHeight =
                MathUtils.clamp(
                        MathUtils.ceil(bounds.height * APPROXIMATE_SAMPLES_PER_WORLD_UNIT),
                        APPROXIMATE_MIN_GRID_SIZE,
                        APPROXIMATE_MAX_GRID_SIZE);
        approximateHazardCellWidth = bounds.width / approximateHazardWidth;
        approximateHazardCellHeight = bounds.height / approximateHazardHeight;
        approximateHazardSamples = new float[approximateHazardWidth * approximateHazardHeight];
        approximateRecoverySamples = new short[approximateHazardSamples.length];
        buildApproximationCache();

        int navigationNodeCount = this.recoveryPoints.size;
        navigationNodeSafe = new boolean[navigationNodeCount];
        navigationNodeVisible = new boolean[navigationNodeCount][navigationNodeCount];
        navigationNodeTravelCost = new float[navigationNodeCount][navigationNodeCount];
        navigationRouteCosts = new float[navigationNodeCount];
        navigationRoutePrevious = new int[navigationNodeCount];
        navigationRouteVisited = new boolean[navigationNodeCount];
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSolidZoneCount() {
        return solidZones.size;
    }

    public ArenaShape getSolidZone(int index) {
        return solidZones.get(index);
    }

    public int getHoleZoneCount() {
        return holeZones.size;
    }

    public ArenaShape getHoleZone(int index) {
        return holeZones.get(index);
    }

    public int getSpawnCount() {
        return spawnPoints.size;
    }

    public SpawnPoint getSpawn(int index) {
        return spawnPoints.get(index);
    }

    public Rectangle getBounds(Rectangle out) {
        return out.set(bounds);
    }

    public Vector2 getFocusPoint(Vector2 out) {
        return out.set(focusPoint);
    }

    public boolean supports(Vector2 point) {
        return supports(point.x, point.y);
    }

    public boolean supports(float x, float y) {
        return supportsExact(x, y);
    }

    public boolean approximateSupports(Vector2 point) {
        return point != null && approximateSupports(point.x, point.y);
    }

    public boolean approximateSupports(float x, float y) {
        if (!isWithinBounds(x, y) || approximateHazardSamples.length == 0) {
            return false;
        }

        return approximateHazardSamples[sampleIndex(x, y)] > 0f;
    }

    public float approximateDistanceToHazard(Vector2 point) {
        return point == null ? 0f : approximateDistanceToHazard(point.x, point.y);
    }

    public float approximateDistanceToHazard(float x, float y) {
        if (!isWithinBounds(x, y) || approximateHazardSamples.length == 0) {
            return 0f;
        }

        float sampleX = (x - bounds.x) / approximateHazardCellWidth - 0.5f;
        float sampleY = (y - bounds.y) / approximateHazardCellHeight - 0.5f;

        int x0 = MathUtils.clamp(MathUtils.floor(sampleX), 0, approximateHazardWidth - 1);
        int y0 = MathUtils.clamp(MathUtils.floor(sampleY), 0, approximateHazardHeight - 1);
        int x1 = Math.min(x0 + 1, approximateHazardWidth - 1);
        int y1 = Math.min(y0 + 1, approximateHazardHeight - 1);
        float alphaX = MathUtils.clamp(sampleX - x0, 0f, 1f);
        float alphaY = MathUtils.clamp(sampleY - y0, 0f, 1f);

        float bottom =
                MathUtils.lerp(
                        approximateHazardSamples[sampleIndex(x0, y0)],
                        approximateHazardSamples[sampleIndex(x1, y0)],
                        alphaX);
        float top =
                MathUtils.lerp(
                        approximateHazardSamples[sampleIndex(x0, y1)],
                        approximateHazardSamples[sampleIndex(x1, y1)],
                        alphaX);
        return MathUtils.lerp(bottom, top, alphaY);
    }

    public float distanceToHazard(Vector2 point) {
        return distanceToHazard(point.x, point.y);
    }

    public float distanceToHazard(float x, float y) {
        return distanceToHazardExact(x, y);
    }

    public float distanceToSafety(Vector2 point) {
        return distanceToSafety(point.x, point.y);
    }

    public float distanceToSafety(float x, float y) {
        if (supports(x, y)) {
            return 0f;
        }

        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < holeZones.size; i++) {
            ArenaShape hole = holeZones.get(i);
            if (hole.contains(x, y)) {
                bestDistance = Math.min(bestDistance, hole.depthInside(x, y));
            }
        }

        if (bestDistance < Float.MAX_VALUE) {
            return bestDistance;
        }

        for (int i = 0; i < solidZones.size; i++) {
            bestDistance = Math.min(bestDistance, solidZones.get(i).distanceOutside(x, y));
        }

        return bestDistance == Float.MAX_VALUE ? 0f : bestDistance;
    }

    public void findRecoveryPoint(Vector2 from, Vector2 out) {
        if (recoveryPoints.size == 0) {
            out.set(focusPoint);
            return;
        }

        if (from == null || approximateRecoverySamples.length == 0) {
            out.set(recoveryPoints.first());
            return;
        }

        int recoveryIndex = approximateRecoverySamples[sampleIndex(from.x, from.y)];
        if (recoveryIndex < 0 || recoveryIndex >= recoveryPoints.size) {
            out.set(recoveryPoints.first());
            return;
        }

        out.set(recoveryPoints.get(recoveryIndex));
    }

    public void clampToPlayable(Vector2 point, float margin) {
        if (distanceToHazard(point) >= margin) {
            return;
        }

        boolean found = false;
        float bestDistance = Float.MAX_VALUE;

        for (int i = 0; i < solidZones.size; i++) {
            solidZones.get(i).closestPointInside(point.x, point.y, margin, scratchCandidate);
            moveOutsideHoles(scratchCandidate, margin, scratchAdjusted);

            if (distanceToHazard(scratchAdjusted) >= margin) {
                float distance = point.dst2(scratchAdjusted);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    scratchBest.set(scratchAdjusted);
                    found = true;
                }
            }
        }

        for (int i = 0; i < recoveryPoints.size; i++) {
            scratchAdjusted.set(recoveryPoints.get(i));
            if (distanceToHazard(scratchAdjusted) >= margin) {
                float distance = point.dst2(scratchAdjusted);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    scratchBest.set(scratchAdjusted);
                    found = true;
                }
            }
        }

        if (found) {
            point.set(scratchBest);
            return;
        }

        findRecoveryPoint(point, point);
    }

    public void findDriveTarget(Vector2 from, Vector2 goal, float margin, Vector2 out) {
        if (goal == null) {
            findRecoveryPoint(from, out);
            return;
        }

        scratchGoal.set(goal);
        clampToPlayable(scratchGoal, margin);
        if (isPathPlayableApprox(from, scratchGoal, margin)) {
            out.set(scratchGoal);
            return;
        }

        int navigationHop = findNavigationHop(from, scratchGoal, margin);
        if (navigationHop >= 0) {
            out.set(recoveryPoints.get(navigationHop));
            return;
        }

        findRecoveryPoint(from, out);
    }

    private boolean supportsExact(float x, float y) {
        if (!isWithinBounds(x, y)) {
            return false;
        }

        boolean insideSolid = false;
        for (int i = 0; i < solidZones.size; i++) {
            if (solidZones.get(i).contains(x, y)) {
                insideSolid = true;
                break;
            }
        }

        if (!insideSolid) {
            return false;
        }

        for (int i = 0; i < holeZones.size; i++) {
            if (holeZones.get(i).contains(x, y)) {
                return false;
            }
        }

        return true;
    }

    private float distanceToHazardExact(float x, float y) {
        if (!isWithinBounds(x, y)) {
            return 0f;
        }

        float safeDepth = 0f;

        for (int i = 0; i < solidZones.size; i++) {
            ArenaShape solid = solidZones.get(i);
            if (solid.contains(x, y)) {
                safeDepth = Math.max(safeDepth, solid.depthInside(x, y));
            }
        }

        if (safeDepth <= 0f) {
            return 0f;
        }

        float hazardDistance = safeDepth;
        for (int i = 0; i < holeZones.size; i++) {
            ArenaShape hole = holeZones.get(i);
            if (hole.contains(x, y)) {
                return 0f;
            }
            hazardDistance = Math.min(hazardDistance, hole.distanceOutside(x, y));
        }

        return hazardDistance;
    }

    private int findNavigationHop(Vector2 from, Vector2 goal, float margin) {
        if (from == null || goal == null || recoveryPoints.size == 0) {
            return -1;
        }

        ensureNavigationGraph(margin);

        float bestGoalCost = Float.MAX_VALUE;
        int bestGoalIndex = -1;
        float bestFallbackScore = Float.MAX_VALUE;
        int bestFallbackIndex = -1;

        for (int i = 0; i < recoveryPoints.size; i++) {
            navigationRouteCosts[i] = Float.MAX_VALUE;
            navigationRoutePrevious[i] = -1;
            navigationRouteVisited[i] = !navigationNodeSafe[i];
            if (!navigationNodeSafe[i]) {
                continue;
            }

            Vector2 candidate = recoveryPoints.get(i);
            if (!isPathPlayableApprox(from, candidate, margin)) {
                continue;
            }

            float travelCost = from.dst(candidate);
            navigationRouteCosts[i] = travelCost;

            float fallbackScore = travelCost + candidate.dst(goal);
            if (fallbackScore < bestFallbackScore) {
                bestFallbackScore = fallbackScore;
                bestFallbackIndex = i;
            }

            if (!isPathPlayableApprox(candidate, goal, margin)) {
                continue;
            }

            float goalCost = travelCost + candidate.dst(goal);
            if (goalCost < bestGoalCost) {
                bestGoalCost = goalCost;
                bestGoalIndex = i;
            }
        }

        while (true) {
            int current = -1;
            float currentCost = bestGoalCost;
            if (currentCost == Float.MAX_VALUE) {
                currentCost = Float.POSITIVE_INFINITY;
            }

            for (int i = 0; i < recoveryPoints.size; i++) {
                if (!navigationRouteVisited[i] && navigationRouteCosts[i] < currentCost) {
                    current = i;
                    currentCost = navigationRouteCosts[i];
                }
            }

            if (current < 0) {
                break;
            }

            navigationRouteVisited[current] = true;
            Vector2 currentPoint = recoveryPoints.get(current);

            float fallbackScore = currentCost + currentPoint.dst(goal);
            if (fallbackScore < bestFallbackScore) {
                bestFallbackScore = fallbackScore;
                bestFallbackIndex = current;
            }

            if (isPathPlayableApprox(currentPoint, goal, margin)) {
                float goalCost = currentCost + currentPoint.dst(goal);
                if (goalCost < bestGoalCost) {
                    bestGoalCost = goalCost;
                    bestGoalIndex = current;
                }
            }

            for (int i = 0; i < recoveryPoints.size; i++) {
                if (navigationRouteVisited[i] || !navigationNodeVisible[current][i]) {
                    continue;
                }

                float routeCost = currentCost + navigationNodeTravelCost[current][i];
                if (routeCost >= navigationRouteCosts[i]) {
                    continue;
                }

                navigationRouteCosts[i] = routeCost;
                navigationRoutePrevious[i] = current;
            }
        }

        if (bestGoalIndex >= 0) {
            return unwindNavigationHop(bestGoalIndex);
        }
        if (bestFallbackIndex >= 0) {
            return unwindNavigationHop(bestFallbackIndex);
        }
        return -1;
    }

    private int unwindNavigationHop(int targetIndex) {
        int current = targetIndex;
        while (current >= 0 && navigationRoutePrevious[current] >= 0) {
            current = navigationRoutePrevious[current];
        }
        return current;
    }

    private void ensureNavigationGraph(float margin) {
        if (cachedNavigationMargin >= 0f
                && Math.abs(cachedNavigationMargin - margin) <= 0.0001f) {
            return;
        }

        for (int i = 0; i < recoveryPoints.size; i++) {
            Vector2 source = recoveryPoints.get(i);
            navigationNodeSafe[i] = distanceToHazard(source) >= margin;
            navigationNodeVisible[i][i] = false;
            navigationNodeTravelCost[i][i] = 0f;

            for (int j = i + 1; j < recoveryPoints.size; j++) {
                Vector2 target = recoveryPoints.get(j);
                boolean connected =
                        navigationNodeSafe[i]
                                && distanceToHazard(target) >= margin
                                && isPathPlayableExact(source, target, margin);
                navigationNodeVisible[i][j] = connected;
                navigationNodeVisible[j][i] = connected;
                float travelCost = connected ? source.dst(target) : 0f;
                navigationNodeTravelCost[i][j] = travelCost;
                navigationNodeTravelCost[j][i] = travelCost;
            }
        }

        cachedNavigationMargin = margin;
    }

    private boolean isPathPlayableApprox(Vector2 from, Vector2 to, float margin) {
        return isPathPlayable(from, to, margin, true);
    }

    private boolean isPathPlayableExact(Vector2 from, Vector2 to, float margin) {
        return isPathPlayable(from, to, margin, false);
    }

    private boolean isPathPlayable(Vector2 from, Vector2 to, float margin, boolean approximate) {
        if (from == null || to == null) {
            return false;
        }

        float effectiveMargin = approximate ? margin + APPROXIMATE_PATH_MARGIN : margin;
        if (samplePathHazardDistance(to, approximate) < effectiveMargin) {
            return false;
        }

        float distance = from.dst(to);
        if (distance <= 0.0001f) {
            return samplePathHazardDistance(from, approximate) > 0f;
        }

        int steps = Math.max(2, MathUtils.ceil(distance / Math.max(0.28f, margin * 0.85f)));
        for (int i = 1; i <= steps; i++) {
            float alpha = (float) i / steps;
            scratchAdjusted.set(from).lerp(to, alpha);
            if (samplePathHazardDistance(scratchAdjusted, approximate) < effectiveMargin) {
                return false;
            }
        }

        return true;
    }

    private void moveOutsideHoles(Vector2 point, float margin, Vector2 out) {
        out.set(point);
        for (int i = 0; i < holeZones.size; i++) {
            ArenaShape hole = holeZones.get(i);
            if (hole.contains(out.x, out.y) || hole.distanceOutside(out.x, out.y) < margin) {
                hole.closestPointOutside(out.x, out.y, margin, out);
            }
        }
    }

    private void buildApproximationCache() {
        int recoveryCount = recoveryPoints.size;
        for (int sampleY = 0; sampleY < approximateHazardHeight; sampleY++) {
            float worldY = bounds.y + (sampleY + 0.5f) * approximateHazardCellHeight;
            for (int sampleX = 0; sampleX < approximateHazardWidth; sampleX++) {
                float worldX = bounds.x + (sampleX + 0.5f) * approximateHazardCellWidth;
                int sampleIndex = sampleIndex(sampleX, sampleY);
                approximateHazardSamples[sampleIndex] = distanceToHazardExact(worldX, worldY);
                approximateRecoverySamples[sampleIndex] =
                        (short) (recoveryCount == 0 ? -1 : findNearestRecoveryPointIndex(worldX, worldY));
            }
        }
    }

    private float samplePathHazardDistance(Vector2 point, boolean approximate) {
        return approximate ? approximateDistanceToHazard(point) : distanceToHazard(point);
    }

    private int findNearestRecoveryPointIndex(float x, float y) {
        if (recoveryPoints.size == 0) {
            return -1;
        }

        Vector2 bestPoint = recoveryPoints.first();
        float bestDistance = Vector2.dst2(x, y, bestPoint.x, bestPoint.y);
        int bestIndex = 0;

        for (int i = 1; i < recoveryPoints.size; i++) {
            Vector2 candidate = recoveryPoints.get(i);
            float distance = Vector2.dst2(x, y, candidate.x, candidate.y);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private boolean isWithinBounds(float x, float y) {
        return x >= bounds.x
                && x <= bounds.x + bounds.width
                && y >= bounds.y
                && y <= bounds.y + bounds.height;
    }

    private int sampleIndex(float x, float y) {
        int sampleX =
                MathUtils.clamp(
                        (int) ((x - bounds.x) / approximateHazardCellWidth),
                        0,
                        approximateHazardWidth - 1);
        int sampleY =
                MathUtils.clamp(
                        (int) ((y - bounds.y) / approximateHazardCellHeight),
                        0,
                        approximateHazardHeight - 1);
        return sampleIndex(sampleX, sampleY);
    }

    private int sampleIndex(int sampleX, int sampleY) {
        return sampleY * approximateHazardWidth + sampleX;
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private final Vector2 focusPoint = new Vector2();
        private final Array<ArenaShape> solidZones = new Array<ArenaShape>();
        private final Array<ArenaShape> holeZones = new Array<ArenaShape>();
        private final Array<SpawnPoint> spawnPoints = new Array<SpawnPoint>();
        private final Array<Vector2> recoveryPoints = new Array<Vector2>();

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder focusPoint(float x, float y) {
            focusPoint.set(x, y);
            return this;
        }

        public Builder solid(ArenaShape shape) {
            solidZones.add(shape);
            return this;
        }

        public Builder hole(ArenaShape shape) {
            holeZones.add(shape);
            return this;
        }

        public Builder spawn(SpawnPoint spawnPoint) {
            spawnPoints.add(spawnPoint);
            return this;
        }

        public Builder recoveryPoint(float x, float y) {
            recoveryPoints.add(new Vector2(x, y));
            return this;
        }

        public Builder scale(float factor) {
            if (factor <= 0f) {
                throw new IllegalArgumentException("Arena map scale must be positive.");
            }
            if (MathUtils.isEqual(factor, 1f)) {
                return this;
            }

            focusPoint.scl(factor);

            for (int i = 0; i < solidZones.size; i++) {
                solidZones.set(i, solidZones.get(i).scale(factor));
            }
            for (int i = 0; i < holeZones.size; i++) {
                holeZones.set(i, holeZones.get(i).scale(factor));
            }
            for (int i = 0; i < spawnPoints.size; i++) {
                spawnPoints.set(i, spawnPoints.get(i).scale(factor));
            }
            for (int i = 0; i < recoveryPoints.size; i++) {
                recoveryPoints.get(i).scl(factor);
            }

            return this;
        }

        public ArenaMap build() {
            if (solidZones.size == 0) {
                throw new IllegalStateException("Arena map requires at least one solid zone.");
            }
            if (spawnPoints.size == 0) {
                throw new IllegalStateException("Arena map requires at least one spawn point.");
            }
            if (recoveryPoints.size == 0) {
                recoveryPoints.add(new Vector2(focusPoint));
                for (int i = 0; i < spawnPoints.size; i++) {
                    SpawnPoint spawnPoint = spawnPoints.get(i);
                    recoveryPoints.add(new Vector2(spawnPoint.x, spawnPoint.y));
                }
            }

            return new ArenaMap(
                    id,
                    name,
                    focusPoint,
                    solidZones,
                    holeZones,
                    spawnPoints,
                    recoveryPoints);
        }
    }
}
