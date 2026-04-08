package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public final class ArenaMap {
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

    public float distanceToHazard(Vector2 point) {
        return distanceToHazard(point.x, point.y);
    }

    public float distanceToHazard(float x, float y) {
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

        Vector2 bestPoint = recoveryPoints.first();
        float bestDistance = from.dst2(bestPoint);

        for (int i = 1; i < recoveryPoints.size; i++) {
            Vector2 candidate = recoveryPoints.get(i);
            float distance = from.dst2(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPoint = candidate;
            }
        }

        out.set(bestPoint);
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
        if (isPathPlayable(from, scratchGoal, margin)) {
            out.set(scratchGoal);
            return;
        }

        boolean found = false;
        float bestScore = Float.MAX_VALUE;

        for (int i = 0; i < recoveryPoints.size; i++) {
            scratchCandidate.set(recoveryPoints.get(i));
            if (distanceToHazard(scratchCandidate) < margin) {
                continue;
            }
            if (!isPathPlayable(from, scratchCandidate, margin)) {
                continue;
            }

            float score = scratchCandidate.dst2(scratchGoal) + from.dst2(scratchCandidate) * 0.22f;
            if (isPathPlayable(scratchCandidate, scratchGoal, margin)) {
                score *= 0.35f;
            }

            if (!found || score < bestScore) {
                bestScore = score;
                scratchBest.set(scratchCandidate);
                found = true;
            }
        }

        if (found) {
            out.set(scratchBest);
            return;
        }

        findRecoveryPoint(from, out);
    }

    private boolean isPathPlayable(Vector2 from, Vector2 to, float margin) {
        if (from == null || to == null) {
            return false;
        }
        if (distanceToHazard(to) < margin) {
            return false;
        }

        float distance = from.dst(to);
        if (distance <= 0.0001f) {
            return distanceToHazard(from) > 0f;
        }

        int steps = Math.max(2, MathUtils.ceil(distance / Math.max(0.28f, margin * 0.85f)));
        for (int i = 1; i <= steps; i++) {
            float alpha = (float) i / steps;
            scratchAdjusted.set(from).lerp(to, alpha);
            if (distanceToHazard(scratchAdjusted) < margin) {
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
