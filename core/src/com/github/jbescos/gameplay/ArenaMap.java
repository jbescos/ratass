package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import java.util.Arrays;

public final class ArenaMap {
    public static final float ROUTE_CURVATURE_SAMPLE_RADIUS = 3.20f;
    public static final float ROUTE_CURVATURE_ANGLE_NORMALIZER = 1.35f;
    private static final float APPROXIMATE_SAMPLES_PER_WORLD_UNIT = 12f;
    private static final int APPROXIMATE_MIN_GRID_SIZE = 96;
    private static final int APPROXIMATE_MAX_GRID_SIZE = 384;
    private static final float APPROXIMATE_PATH_MARGIN = 0.04f;
    private static final float ROUTE_FIELD_MARGIN_EPSILON = 0.0001f;
    private static final float ROUTE_FIELD_LOOKAHEAD_DISTANCE = 4.20f;
    private static final float ROUTE_FIELD_MIN_TARGET_DISTANCE = 1.25f;
    private static final float ROUTE_FIELD_BLIND_LOOKAHEAD_DISTANCE = 1.70f;
    private static final int ROUTE_FIELD_LOOKAHEAD_STEPS = 56;
    private static final int ROUTE_FIELD_NEAREST_SEARCH_RADIUS = 18;
    private static final float ROUTE_DIRECTION_ALIGNMENT_PENALTY = 10f;
    private static final float ROUTE_PROGRESS_CONTINUITY_PENALTY = 0.08f;
    private static final float ROUTE_PROGRESS_BACKTRACK_PENALTY = 0.22f;
    private static final float ROUTE_TANGENT_SAMPLE_DISTANCE = 1.80f;
    private static final int ROUTE_SAMPLE_LOOKUP_SEARCH_RADIUS = 28;
    private static final int ROUTE_SAMPLE_REFERENCE_SEARCH_RADIUS = 40;

    private final String id;
    private final String name;
    private final String surfaceImagePath;
    private final Vector2 focusPoint = new Vector2();
    private final Array<ArenaShape> solidZones = new Array<ArenaShape>();
    private final Array<ArenaShape> holeZones = new Array<ArenaShape>();
    private final Array<SpawnPoint> spawnPoints = new Array<SpawnPoint>();
    private final Array<SpawnPoint> checkpoints = new Array<SpawnPoint>();
    private final Array<Vector2> routePoints = new Array<Vector2>();
    private final Array<Vector2> routeMarkerPoints = new Array<Vector2>();
    private float[] routeMarkerProgresses = new float[0];
    private final Array<Vector2> recoveryPoints = new Array<Vector2>();
    private final Rectangle bounds = new Rectangle();
    private final Vector2 scratchCandidate = new Vector2();
    private final Vector2 scratchAdjusted = new Vector2();
    private final Vector2 scratchBest = new Vector2();
    private final Vector2 scratchGoal = new Vector2();
    private final Vector2 scratchRouteTangentStart = new Vector2();
    private final Vector2 scratchRouteTangentEnd = new Vector2();
    private final int approximateHazardWidth;
    private final int approximateHazardHeight;
    private final float approximateHazardCellWidth;
    private final float approximateHazardCellHeight;
    private final float[] approximateHazardSamples;
    private final short[] approximateRecoverySamples;
    private final RouteMetadata routeMetadata;
    private final boolean[] routeFieldSafe;
    private final float[] routeFieldCosts;
    private final int[] routeFieldHeap;
    private final int[] routeFieldHeapPositions;
    private int routeFieldHeapSize;
    private float cachedRouteFieldMargin = -1f;
    private int cachedRouteFieldGoalIndex = -1;
    private final float[] routeCumulativeDistances;
    private final float routeLength;
    private float scratchRouteSearchBestScore;
    private float scratchRouteSearchBestProgress;
    private boolean scratchRouteSearchScored;

    private ArenaMap(
            String id,
            String name,
            String surfaceImagePath,
            Vector2 focusPoint,
            Array<ArenaShape> solidZones,
            Array<ArenaShape> holeZones,
            Array<SpawnPoint> spawnPoints,
            Array<SpawnPoint> checkpoints,
            Array<Vector2> routePoints,
            Array<Vector2> routeMarkerPoints,
            float[] routeMarkerProgresses,
            Array<Vector2> recoveryPoints,
            RouteMetadata routeMetadata) {
        this.id = id;
        this.name = name;
        this.surfaceImagePath = surfaceImagePath;
        this.focusPoint.set(focusPoint);

        this.solidZones.addAll(solidZones);
        this.holeZones.addAll(holeZones);
        this.spawnPoints.addAll(spawnPoints);
        this.checkpoints.addAll(checkpoints);
        for (int i = 0; i < routePoints.size; i++) {
            this.routePoints.add(new Vector2(routePoints.get(i)));
        }
        for (int i = 0; i < routeMarkerPoints.size; i++) {
            this.routeMarkerPoints.add(new Vector2(routeMarkerPoints.get(i)));
        }
        this.routeMarkerProgresses = new float[this.routeMarkerPoints.size];
        for (int i = 0; i < this.routeMarkerProgresses.length; i++) {
            float progress =
                    routeMarkerProgresses != null && i < routeMarkerProgresses.length
                            ? routeMarkerProgresses[i]
                            : 0f;
            this.routeMarkerProgresses[i] = progress;
        }

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

        int routeFieldCellCount = approximateHazardSamples.length;
        routeFieldSafe = new boolean[routeFieldCellCount];
        routeFieldCosts = new float[routeFieldCellCount];
        routeFieldHeap = new int[routeFieldCellCount];
        routeFieldHeapPositions = new int[routeFieldCellCount];

        routeCumulativeDistances = new float[this.routePoints.size];
        routeLength = buildRouteDistanceCache();
        this.routeMetadata =
                routeMetadata != null && routeMetadata.isUsable(routeLength)
                        ? new RouteMetadata(routeMetadata)
                        : null;
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

    public String getSurfaceImagePath() {
        return surfaceImagePath;
    }

    public boolean hasSurfaceImage() {
        return surfaceImagePath != null && surfaceImagePath.length() > 0;
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

    public int getCheckpointCount() {
        return checkpoints.size;
    }

    public SpawnPoint getCheckpoint(int index) {
        return checkpoints.get(index);
    }

    public int getRoutePointCount() {
        return routePoints.size;
    }

    public int getRouteMarkerPointCount() {
        return routeMarkerPoints.size;
    }

    public void getRouteMarkerPoint(int index, Vector2 out) {
        if (out == null) {
            return;
        }
        if (index < 0 || index >= routeMarkerPoints.size) {
            out.setZero();
            return;
        }
        out.set(routeMarkerPoints.get(index));
    }

    public float getRouteMarkerProgress(int index) {
        if (index < 0 || index >= routeMarkerProgresses.length) {
            return 0f;
        }
        return wrapRouteProgress(routeMarkerProgresses[index]);
    }

    public boolean hasRoute() {
        return routePoints.size >= 2 && routeLength > 0.001f;
    }

    public float getRouteLength() {
        return routeLength;
    }

    public int getRouteProgressIndexCount() {
        if (routeMetadata != null && routeMetadata.hasSamples()) {
            return routeMetadata.sampleCount();
        }
        return Math.max(1, routePoints.size);
    }

    public int getRouteProgressIndex(float progress) {
        if (!hasRoute()) {
            return 0;
        }
        if (routeMetadata != null && routeMetadata.hasSamples()) {
            return routeMetadata.sampleIndexForProgress(progress);
        }
        return findRouteSegmentIndex(wrapRouteProgress(progress));
    }

    public float findRouteProgress(Vector2 position) {
        return position == null ? 0f : findRouteProgress(position.x, position.y);
    }

    public float findRouteProgress(Vector2 position, Vector2 preferredDirection) {
        return position == null
                ? 0f
                : findRouteProgress(
                        position.x,
                        position.y,
                        preferredDirection == null ? 0f : preferredDirection.x,
                        preferredDirection == null ? 0f : preferredDirection.y);
    }

    public float findRouteProgressNear(
            Vector2 position,
            Vector2 preferredDirection,
            float referenceProgress) {
        return position == null
                ? wrapRouteProgress(referenceProgress)
                : findRouteProgress(
                        position.x,
                        position.y,
                        preferredDirection == null ? 0f : preferredDirection.x,
                        preferredDirection == null ? 0f : preferredDirection.y,
                        true,
                        referenceProgress);
    }

    public float findRouteProgress(float x, float y) {
        return findRouteProgress(x, y, 0f, 0f);
    }

    public float findRouteProgress(float x, float y, float preferredDirectionX, float preferredDirectionY) {
        return findRouteProgress(x, y, preferredDirectionX, preferredDirectionY, false, 0f);
    }

    private float findRouteProgress(
            float x,
            float y,
            float preferredDirectionX,
            float preferredDirectionY,
            boolean useReferenceProgress,
            float referenceProgress) {
        if (!hasRoute()) {
            return 0f;
        }

        float preferredLength =
                (float)
                        Math.sqrt(
                                preferredDirectionX * preferredDirectionX
                                        + preferredDirectionY * preferredDirectionY);
        boolean usePreferredDirection = preferredLength > 0.0001f;
        if (usePreferredDirection) {
            preferredDirectionX /= preferredLength;
            preferredDirectionY /= preferredLength;
        }

        if (routeMetadata != null && routeMetadata.hasSamples()) {
            return findRouteProgressFromMetadata(
                    x,
                    y,
                    preferredDirectionX,
                    preferredDirectionY,
                    usePreferredDirection,
                    useReferenceProgress,
                    referenceProgress);
        }

        float bestScore = Float.MAX_VALUE;
        float bestProgress = 0f;
        for (int i = 0; i < routePoints.size; i++) {
            Vector2 start = routePoints.get(i);
            Vector2 end = routePoints.get((i + 1) % routePoints.size);
            float segmentX = end.x - start.x;
            float segmentY = end.y - start.y;
            float segmentLengthSquared = segmentX * segmentX + segmentY * segmentY;
            if (segmentLengthSquared <= 0.0001f) {
                continue;
            }

            float segmentLength = (float) Math.sqrt(segmentLengthSquared);
            float alpha =
                    MathUtils.clamp(
                            ((x - start.x) * segmentX + (y - start.y) * segmentY)
                                    / segmentLengthSquared,
                            0f,
                            1f);
            float projectedX = start.x + segmentX * alpha;
            float projectedY = start.y + segmentY * alpha;
            float distance = Vector2.dst2(x, y, projectedX, projectedY);
            float score = distance;
            if (usePreferredDirection) {
                float alignment =
                        (segmentX * preferredDirectionX + segmentY * preferredDirectionY)
                                / segmentLength;
                score += (1f - alignment) * ROUTE_DIRECTION_ALIGNMENT_PENALTY;
            }
            float candidateProgress =
                    wrapRouteProgress(routeCumulativeDistances[i] + segmentLength * alpha);
            if (useReferenceProgress) {
                float routeDelta = routeProgressDelta(referenceProgress, candidateProgress);
                score += Math.min(40f, Math.abs(routeDelta) * ROUTE_PROGRESS_CONTINUITY_PENALTY);
                if (routeDelta < -1.25f) {
                    score += Math.min(60f, -routeDelta * ROUTE_PROGRESS_BACKTRACK_PENALTY);
                }
            }
            if (score < bestScore) {
                bestScore = score;
                bestProgress = candidateProgress;
            }
        }
        return bestProgress;
    }

    private float findRouteProgressFromMetadata(
            float x,
            float y,
            float preferredDirectionX,
            float preferredDirectionY,
            boolean usePreferredDirection,
            boolean useReferenceProgress,
            float referenceProgress) {
        scratchRouteSearchBestScore = Float.MAX_VALUE;
        scratchRouteSearchBestProgress = wrapRouteProgress(referenceProgress);
        scratchRouteSearchScored = false;

        if (useReferenceProgress) {
            scanRouteSampleWindow(
                    routeMetadata.sampleIndexForProgress(referenceProgress),
                    ROUTE_SAMPLE_REFERENCE_SEARCH_RADIUS,
                    x,
                    y,
                    preferredDirectionX,
                    preferredDirectionY,
                    usePreferredDirection,
                    true,
                    referenceProgress);
        }

        int lookupIndex = routeMetadata.lookupSampleIndex(x, y);
        if (lookupIndex >= 0) {
            scanRouteSampleWindow(
                    lookupIndex,
                    ROUTE_SAMPLE_LOOKUP_SEARCH_RADIUS,
                    x,
                    y,
                    preferredDirectionX,
                    preferredDirectionY,
                    usePreferredDirection,
                    useReferenceProgress,
                    referenceProgress);
        }

        if (!scratchRouteSearchScored) {
            for (int i = 0; i < routeMetadata.sampleCount(); i++) {
                considerRouteSampleSegment(
                        i,
                        x,
                        y,
                        preferredDirectionX,
                        preferredDirectionY,
                        usePreferredDirection,
                        useReferenceProgress,
                        referenceProgress);
            }
        }

        return scratchRouteSearchBestProgress;
    }

    private void scanRouteSampleWindow(
            int centerIndex,
            int radius,
            float x,
            float y,
            float preferredDirectionX,
            float preferredDirectionY,
            boolean usePreferredDirection,
            boolean useReferenceProgress,
            float referenceProgress) {
        if (routeMetadata == null || !routeMetadata.hasSamples() || centerIndex < 0) {
            return;
        }
        int sampleCount = routeMetadata.sampleCount();
        int effectiveRadius = Math.min(Math.max(0, radius), Math.max(0, sampleCount - 1));
        for (int offset = -effectiveRadius; offset <= effectiveRadius; offset++) {
            int index = routeMetadata.wrapSampleIndex(centerIndex + offset);
            considerRouteSampleSegment(
                    index,
                    x,
                    y,
                    preferredDirectionX,
                    preferredDirectionY,
                    usePreferredDirection,
                    useReferenceProgress,
                    referenceProgress);
        }
    }

    private void considerRouteSampleSegment(
            int segmentIndex,
            float x,
            float y,
            float preferredDirectionX,
            float preferredDirectionY,
            boolean usePreferredDirection,
            boolean useReferenceProgress,
            float referenceProgress) {
        int nextIndex = routeMetadata.wrapSampleIndex(segmentIndex + 1);
        float startX = routeMetadata.sampleX[segmentIndex];
        float startY = routeMetadata.sampleY[segmentIndex];
        float endX = routeMetadata.sampleX[nextIndex];
        float endY = routeMetadata.sampleY[nextIndex];
        float segmentX = endX - startX;
        float segmentY = endY - startY;
        float segmentLengthSquared = segmentX * segmentX + segmentY * segmentY;
        if (segmentLengthSquared <= 0.0001f) {
            return;
        }

        float segmentLength = (float) Math.sqrt(segmentLengthSquared);
        float alpha =
                MathUtils.clamp(
                        ((x - startX) * segmentX + (y - startY) * segmentY)
                                / segmentLengthSquared,
                        0f,
                        1f);
        float projectedX = startX + segmentX * alpha;
        float projectedY = startY + segmentY * alpha;
        float distance = Vector2.dst2(x, y, projectedX, projectedY);
        float score = distance;
        if (usePreferredDirection) {
            float alignment =
                    (segmentX * preferredDirectionX + segmentY * preferredDirectionY)
                            / segmentLength;
            score += (1f - alignment) * ROUTE_DIRECTION_ALIGNMENT_PENALTY;
        }
        float candidateProgress =
                wrapRouteProgress(routeMetadata.sampleProgress(segmentIndex) + routeMetadata.sampleStep * alpha);
        if (useReferenceProgress) {
            float routeDelta = routeProgressDelta(referenceProgress, candidateProgress);
            score += Math.min(40f, Math.abs(routeDelta) * ROUTE_PROGRESS_CONTINUITY_PENALTY);
            if (routeDelta < -1.25f) {
                score += Math.min(60f, -routeDelta * ROUTE_PROGRESS_BACKTRACK_PENALTY);
            }
        }
        if (score < scratchRouteSearchBestScore) {
            scratchRouteSearchBestScore = score;
            scratchRouteSearchBestProgress = candidateProgress;
            scratchRouteSearchScored = true;
        }
    }

    public float findNormalizedRouteProgress(Vector2 position) {
        return !hasRoute() ? 0f : findRouteProgress(position) / routeLength;
    }

    public float routeProgressDelta(float fromProgress, float toProgress) {
        if (!hasRoute()) {
            return 0f;
        }
        float delta = wrapRouteProgress(toProgress) - wrapRouteProgress(fromProgress);
        float halfLength = routeLength * 0.5f;
        if (delta < -halfLength) {
            delta += routeLength;
        } else if (delta > halfLength) {
            delta -= routeLength;
        }
        return delta;
    }

    public void findRoutePoint(float progress, Vector2 out) {
        if (out == null) {
            return;
        }
        if (!hasRoute()) {
            out.set(focusPoint);
            return;
        }

        float wrappedProgress = wrapRouteProgress(progress);
        if (routeMetadata != null && routeMetadata.hasSamples()) {
            routeMetadata.findPoint(wrappedProgress, out);
            return;
        }
        int segmentIndex = findRouteSegmentIndex(wrappedProgress);
        Vector2 start = routePoints.get(segmentIndex);
        Vector2 end = routePoints.get((segmentIndex + 1) % routePoints.size);
        float segmentLength =
                Vector2.dst(start.x, start.y, end.x, end.y);
        float alpha =
                segmentLength <= 0.0001f
                        ? 0f
                        : MathUtils.clamp(
                                (wrappedProgress - routeCumulativeDistances[segmentIndex])
                                        / segmentLength,
                                0f,
                                1f);
        out.set(start).lerp(end, alpha);
    }

    public void findRouteTangent(float progress, Vector2 out) {
        if (out == null) {
            return;
        }
        if (!hasRoute()) {
            out.set(0f, 1f);
            return;
        }

        if (routeMetadata != null && routeMetadata.hasSamples()) {
            routeMetadata.findTangent(wrapRouteProgress(progress), out);
            return;
        }

        float sampleDistance = Math.min(ROUTE_TANGENT_SAMPLE_DISTANCE, routeLength * 0.10f);
        findRoutePoint(progress - sampleDistance, scratchRouteTangentStart);
        findRoutePoint(progress + sampleDistance, scratchRouteTangentEnd);
        out.set(scratchRouteTangentEnd).sub(scratchRouteTangentStart);
        if (out.isZero(0.0001f)) {
            int segmentIndex = findRouteSegmentIndex(wrapRouteProgress(progress));
            Vector2 start = routePoints.get(segmentIndex);
            Vector2 end = routePoints.get((segmentIndex + 1) % routePoints.size);
            out.set(end).sub(start);
            if (out.isZero(0.0001f)) {
                out.set(0f, 1f);
                return;
            }
        }
        out.nor();
    }

    public void findRouteTangentAt(Vector2 position, Vector2 out) {
        findRouteTangent(findRouteProgress(position), out);
    }

    public void findRouteTangentAt(Vector2 position, Vector2 preferredDirection, Vector2 out) {
        findRouteTangent(findRouteProgress(position, preferredDirection), out);
    }

    public float getRouteCurvature(float progress) {
        return routeMetadata == null ? 0f : routeMetadata.sampleValue(progress, routeMetadata.curvature, 0f);
    }

    public float getRouteCurvaturePerWorldUnit(float progress) {
        return getRouteCurvature(progress)
                * ROUTE_CURVATURE_ANGLE_NORMALIZER
                / (ROUTE_CURVATURE_SAMPLE_RADIUS * 2f);
    }

    public float getRouteNextCornerDistance(float progress) {
        return routeMetadata == null
                ? 1f
                : routeMetadata.sampleValue(progress, routeMetadata.nextCornerDistance, 1f);
    }

    public float getRouteNextCornerDirection(float progress) {
        return routeMetadata == null
                ? 0f
                : routeMetadata.sampleValue(progress, routeMetadata.nextCornerDirection, 0f);
    }

    public float getRouteNextCornerSeverity(float progress) {
        return routeMetadata == null
                ? 0f
                : routeMetadata.sampleValue(progress, routeMetadata.nextCornerSeverity, 0f);
    }

    public float getRouteLeftClearance(float progress) {
        return routeMetadata == null
                ? 0f
                : routeMetadata.sampleValue(progress, routeMetadata.leftClearance, 0f);
    }

    public float getRouteRightClearance(float progress) {
        return routeMetadata == null
                ? 0f
                : routeMetadata.sampleValue(progress, routeMetadata.rightClearance, 0f);
    }

    public float getRouteRoadWidth(float progress) {
        return routeMetadata == null
                ? 0f
                : routeMetadata.sampleValue(progress, routeMetadata.roadWidth, 0f);
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

        if (findRouteFieldTarget(from, scratchGoal, margin, out)) {
            return;
        }

        findRecoveryPoint(from, out);
    }

    public float estimateDriveDistance(Vector2 from, Vector2 goal, float margin) {
        if (from == null || goal == null) {
            return 0f;
        }

        scratchGoal.set(goal);
        clampToPlayable(scratchGoal, margin);
        if (isPathPlayableApprox(from, scratchGoal, margin)) {
            return from.dst(scratchGoal);
        }

        float routeDistance = estimateRouteFieldDistance(from, scratchGoal, margin);
        if (routeDistance >= 0f) {
            return routeDistance;
        }

        findRecoveryPoint(from, scratchBest);
        return from.dst(scratchBest) + scratchBest.dst(scratchGoal);
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

    private boolean findRouteFieldTarget(Vector2 from, Vector2 goal, float margin, Vector2 out) {
        if (from == null || goal == null || approximateHazardSamples.length == 0) {
            return false;
        }

        ensureRouteField(goal, margin);
        if (cachedRouteFieldGoalIndex < 0) {
            return false;
        }

        int startIndex = findNearestReachableRouteFieldCell(from.x, from.y);
        if (startIndex < 0) {
            return false;
        }

        if (startIndex == cachedRouteFieldGoalIndex) {
            out.set(goal);
            return true;
        }

        int currentIndex = startIndex;
        int bestVisibleIndex = startIndex;
        float bestVisibleDistance = 0f;
        int blindProgressIndex = startIndex;
        float blindProgressDistance = 0f;
        float followedDistance = 0f;
        for (int step = 0; step < ROUTE_FIELD_LOOKAHEAD_STEPS
                && followedDistance < ROUTE_FIELD_LOOKAHEAD_DISTANCE; step++) {
            int nextIndex = findNextRouteFieldCell(currentIndex);
            if (nextIndex < 0) {
                break;
            }

            float stepDistance = routeFieldCellDistance(currentIndex, nextIndex);
            followedDistance += stepDistance;
            currentIndex = nextIndex;
            if (blindProgressDistance < ROUTE_FIELD_BLIND_LOOKAHEAD_DISTANCE) {
                blindProgressDistance += stepDistance;
                blindProgressIndex = currentIndex;
            }

            if (currentIndex == cachedRouteFieldGoalIndex) {
                out.set(goal);
                return true;
            }

            scratchCandidate.set(routeFieldCenterX(currentIndex), routeFieldCenterY(currentIndex));
            if (isPathPlayableApprox(from, scratchCandidate, margin)) {
                bestVisibleIndex = currentIndex;
                bestVisibleDistance = followedDistance;
            } else if (bestVisibleIndex != startIndex) {
                break;
            }
        }

        if ((bestVisibleIndex == startIndex
                        || bestVisibleDistance < ROUTE_FIELD_MIN_TARGET_DISTANCE)
                && blindProgressIndex != startIndex) {
            out.set(routeFieldCenterX(blindProgressIndex), routeFieldCenterY(blindProgressIndex));
            return true;
        }
        out.set(routeFieldCenterX(bestVisibleIndex), routeFieldCenterY(bestVisibleIndex));
        return true;
    }

    private float estimateRouteFieldDistance(Vector2 from, Vector2 goal, float margin) {
        if (from == null || goal == null || approximateHazardSamples.length == 0) {
            return -1f;
        }

        ensureRouteField(goal, margin);
        if (cachedRouteFieldGoalIndex < 0) {
            return -1f;
        }

        int startIndex = findNearestReachableRouteFieldCell(from.x, from.y);
        if (startIndex < 0) {
            return -1f;
        }

        float routeCost = routeFieldCosts[startIndex];
        if (routeCost == Float.MAX_VALUE) {
            return -1f;
        }

        return Vector2.dst(
                        from.x,
                        from.y,
                        routeFieldCenterX(startIndex),
                        routeFieldCenterY(startIndex))
                + routeCost;
    }

    private void ensureRouteField(Vector2 goal, float margin) {
        float effectiveMargin = margin + APPROXIMATE_PATH_MARGIN;
        boolean marginChanged =
                cachedRouteFieldMargin < 0f
                        || Math.abs(cachedRouteFieldMargin - effectiveMargin)
                                > ROUTE_FIELD_MARGIN_EPSILON;
        if (marginChanged) {
            for (int i = 0; i < routeFieldSafe.length; i++) {
                routeFieldSafe[i] = approximateHazardSamples[i] >= effectiveMargin;
            }
        }

        int goalIndex = findNearestSafeRouteFieldCell(goal.x, goal.y);
        if (goalIndex < 0) {
            cachedRouteFieldMargin = effectiveMargin;
            cachedRouteFieldGoalIndex = -1;
            return;
        }

        if (!marginChanged && goalIndex == cachedRouteFieldGoalIndex) {
            return;
        }

        buildRouteField(goalIndex);
        cachedRouteFieldMargin = effectiveMargin;
        cachedRouteFieldGoalIndex = goalIndex;
    }

    private void buildRouteField(int goalIndex) {
        routeFieldHeapSize = 0;
        for (int i = 0; i < routeFieldCosts.length; i++) {
            routeFieldCosts[i] = Float.MAX_VALUE;
            routeFieldHeapPositions[i] = -1;
        }

        if (goalIndex < 0 || !routeFieldSafe[goalIndex]) {
            return;
        }

        routeFieldCosts[goalIndex] = 0f;
        updateRouteFieldHeap(goalIndex);
        while (routeFieldHeapSize > 0) {
            int currentIndex = pollRouteFieldHeap();
            int currentX = routeFieldSampleX(currentIndex);
            int currentY = routeFieldSampleY(currentIndex);
            float currentCost = routeFieldCosts[currentIndex];

            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                int neighborY = currentY + offsetY;
                if (neighborY < 0 || neighborY >= approximateHazardHeight) {
                    continue;
                }
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    if (offsetX == 0 && offsetY == 0) {
                        continue;
                    }

                    int neighborX = currentX + offsetX;
                    if (!isRouteFieldNeighborAllowed(currentX, currentY, neighborX, neighborY)) {
                        continue;
                    }

                    int neighborIndex = sampleIndex(neighborX, neighborY);
                    float candidateCost =
                            currentCost
                                    + routeFieldMoveCost(
                                            Math.abs(offsetX),
                                            Math.abs(offsetY));
                    if (candidateCost >= routeFieldCosts[neighborIndex]) {
                        continue;
                    }

                    routeFieldCosts[neighborIndex] = candidateCost;
                    updateRouteFieldHeap(neighborIndex);
                }
            }
        }
    }

    private int findNextRouteFieldCell(int currentIndex) {
        int currentX = routeFieldSampleX(currentIndex);
        int currentY = routeFieldSampleY(currentIndex);
        float bestCost = routeFieldCosts[currentIndex];
        int bestIndex = -1;

        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            int neighborY = currentY + offsetY;
            if (neighborY < 0 || neighborY >= approximateHazardHeight) {
                continue;
            }
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                if (offsetX == 0 && offsetY == 0) {
                    continue;
                }

                int neighborX = currentX + offsetX;
                if (!isRouteFieldNeighborAllowed(currentX, currentY, neighborX, neighborY)) {
                    continue;
                }

                int neighborIndex = sampleIndex(neighborX, neighborY);
                float neighborCost = routeFieldCosts[neighborIndex];
                if (neighborCost < bestCost - ROUTE_FIELD_MARGIN_EPSILON) {
                    bestCost = neighborCost;
                    bestIndex = neighborIndex;
                }
            }
        }

        return bestIndex;
    }

    private boolean isRouteFieldNeighborAllowed(
            int currentX,
            int currentY,
            int neighborX,
            int neighborY) {
        if (neighborX < 0
                || neighborX >= approximateHazardWidth
                || neighborY < 0
                || neighborY >= approximateHazardHeight) {
            return false;
        }

        int neighborIndex = sampleIndex(neighborX, neighborY);
        if (!routeFieldSafe[neighborIndex]) {
            return false;
        }

        if (neighborX != currentX && neighborY != currentY) {
            return routeFieldSafe[sampleIndex(neighborX, currentY)]
                    && routeFieldSafe[sampleIndex(currentX, neighborY)];
        }

        return true;
    }

    private int findNearestSafeRouteFieldCell(float x, float y) {
        return findNearestRouteFieldCell(x, y, false);
    }

    private int findNearestReachableRouteFieldCell(float x, float y) {
        return findNearestRouteFieldCell(x, y, true);
    }

    private int findNearestRouteFieldCell(float x, float y, boolean requireReachable) {
        if (!isWithinBounds(x, y) || routeFieldSafe.length == 0) {
            return -1;
        }

        int originX = sampleX(x);
        int originY = sampleY(y);
        int bestIndex = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int radius = 0; radius <= ROUTE_FIELD_NEAREST_SEARCH_RADIUS; radius++) {
            int minX = Math.max(0, originX - radius);
            int maxX = Math.min(approximateHazardWidth - 1, originX + radius);
            int minY = Math.max(0, originY - radius);
            int maxY = Math.min(approximateHazardHeight - 1, originY + radius);

            for (int sampleY = minY; sampleY <= maxY; sampleY++) {
                for (int sampleX = minX; sampleX <= maxX; sampleX++) {
                    if (radius > 0
                            && sampleX > minX
                            && sampleX < maxX
                            && sampleY > minY
                            && sampleY < maxY) {
                        continue;
                    }
                    int index = sampleIndex(sampleX, sampleY);
                    if (!isUsableRouteFieldCell(index, requireReachable)) {
                        continue;
                    }

                    float distance =
                            Vector2.dst2(
                                    x,
                                    y,
                                    routeFieldCenterForSampleX(sampleX),
                                    routeFieldCenterForSampleY(sampleY));
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestIndex = index;
                    }
                }
            }

            if (bestIndex >= 0) {
                return bestIndex;
            }
        }

        for (int index = 0; index < routeFieldSafe.length; index++) {
            if (!isUsableRouteFieldCell(index, requireReachable)) {
                continue;
            }
            float distance =
                    Vector2.dst2(
                            x,
                            y,
                            routeFieldCenterX(index),
                            routeFieldCenterY(index));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private boolean isUsableRouteFieldCell(int index, boolean requireReachable) {
        return routeFieldSafe[index]
                && (!requireReachable || routeFieldCosts[index] < Float.MAX_VALUE);
    }

    private void updateRouteFieldHeap(int index) {
        int heapPosition = routeFieldHeapPositions[index];
        if (heapPosition < 0) {
            heapPosition = routeFieldHeapSize++;
            routeFieldHeap[heapPosition] = index;
            routeFieldHeapPositions[index] = heapPosition;
        }
        siftRouteFieldHeapUp(heapPosition);
    }

    private int pollRouteFieldHeap() {
        int result = routeFieldHeap[0];
        routeFieldHeapPositions[result] = -1;
        routeFieldHeapSize--;
        if (routeFieldHeapSize > 0) {
            int replacement = routeFieldHeap[routeFieldHeapSize];
            routeFieldHeap[0] = replacement;
            routeFieldHeapPositions[replacement] = 0;
            siftRouteFieldHeapDown(0);
        }
        return result;
    }

    private void siftRouteFieldHeapUp(int index) {
        int current = index;
        while (current > 0) {
            int parent = (current - 1) >>> 1;
            if (routeFieldHeapCost(parent) <= routeFieldHeapCost(current)) {
                break;
            }
            swapRouteFieldHeap(parent, current);
            current = parent;
        }
    }

    private void siftRouteFieldHeapDown(int index) {
        int current = index;
        while (true) {
            int left = current * 2 + 1;
            int right = left + 1;
            int smallest = current;
            if (left < routeFieldHeapSize && routeFieldHeapCost(left) < routeFieldHeapCost(smallest)) {
                smallest = left;
            }
            if (right < routeFieldHeapSize && routeFieldHeapCost(right) < routeFieldHeapCost(smallest)) {
                smallest = right;
            }
            if (smallest == current) {
                return;
            }
            swapRouteFieldHeap(current, smallest);
            current = smallest;
        }
    }

    private float routeFieldHeapCost(int heapIndex) {
        return routeFieldCosts[routeFieldHeap[heapIndex]];
    }

    private void swapRouteFieldHeap(int first, int second) {
        int firstValue = routeFieldHeap[first];
        int secondValue = routeFieldHeap[second];
        routeFieldHeap[first] = secondValue;
        routeFieldHeap[second] = firstValue;
        routeFieldHeapPositions[firstValue] = second;
        routeFieldHeapPositions[secondValue] = first;
    }

    private float routeFieldCellDistance(int firstIndex, int secondIndex) {
        int deltaX = Math.abs(routeFieldSampleX(firstIndex) - routeFieldSampleX(secondIndex));
        int deltaY = Math.abs(routeFieldSampleY(firstIndex) - routeFieldSampleY(secondIndex));
        return routeFieldMoveCost(deltaX, deltaY);
    }

    private float routeFieldMoveCost(int deltaX, int deltaY) {
        if (deltaX == 0) {
            return approximateHazardCellHeight;
        }
        if (deltaY == 0) {
            return approximateHazardCellWidth;
        }
        return (float)
                Math.sqrt(
                        approximateHazardCellWidth * approximateHazardCellWidth
                                + approximateHazardCellHeight * approximateHazardCellHeight);
    }

    private int routeFieldSampleX(int index) {
        return index % approximateHazardWidth;
    }

    private int routeFieldSampleY(int index) {
        return index / approximateHazardWidth;
    }

    private float routeFieldCenterX(int index) {
        return routeFieldCenterForSampleX(routeFieldSampleX(index));
    }

    private float routeFieldCenterForSampleX(int sampleX) {
        return bounds.x + (sampleX + 0.5f) * approximateHazardCellWidth;
    }

    private float routeFieldCenterY(int index) {
        return routeFieldCenterForSampleY(routeFieldSampleY(index));
    }

    private float routeFieldCenterForSampleY(int sampleY) {
        return bounds.y + (sampleY + 0.5f) * approximateHazardCellHeight;
    }

    private boolean isPathPlayableApprox(Vector2 from, Vector2 to, float margin) {
        return isPathPlayable(from, to, margin, true);
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
        return sampleIndex(sampleX(x), sampleY(y));
    }

    private int sampleIndex(int sampleX, int sampleY) {
        return sampleY * approximateHazardWidth + sampleX;
    }

    private int sampleX(float x) {
        return MathUtils.clamp(
                (int) ((x - bounds.x) / approximateHazardCellWidth),
                0,
                approximateHazardWidth - 1);
    }

    private int sampleY(float y) {
        return MathUtils.clamp(
                (int) ((y - bounds.y) / approximateHazardCellHeight),
                0,
                approximateHazardHeight - 1);
    }

    private float buildRouteDistanceCache() {
        if (routePoints.size < 2) {
            return 0f;
        }

        float cumulative = 0f;
        for (int i = 0; i < routePoints.size; i++) {
            routeCumulativeDistances[i] = cumulative;
            Vector2 current = routePoints.get(i);
            Vector2 next = routePoints.get((i + 1) % routePoints.size);
            cumulative += Vector2.dst(current.x, current.y, next.x, next.y);
        }
        return cumulative;
    }

    private int findRouteSegmentIndex(float progress) {
        if (routeCumulativeDistances.length <= 1) {
            return 0;
        }

        int low = 0;
        int high = routeCumulativeDistances.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            float start = routeCumulativeDistances[mid];
            float end =
                    mid + 1 < routeCumulativeDistances.length
                            ? routeCumulativeDistances[mid + 1]
                            : routeLength;
            if (progress < start) {
                high = mid - 1;
            } else if (progress >= end && mid + 1 < routeCumulativeDistances.length) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return MathUtils.clamp(low, 0, routeCumulativeDistances.length - 1);
    }

    private float wrapRouteProgress(float progress) {
        if (routeLength <= 0.001f) {
            return 0f;
        }
        float wrapped = progress % routeLength;
        return wrapped < 0f ? wrapped + routeLength : wrapped;
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private String surfaceImagePath;
        private final Vector2 focusPoint = new Vector2();
        private final Array<ArenaShape> solidZones = new Array<ArenaShape>();
        private final Array<ArenaShape> holeZones = new Array<ArenaShape>();
        private final Array<SpawnPoint> spawnPoints = new Array<SpawnPoint>();
        private final Array<SpawnPoint> checkpoints = new Array<SpawnPoint>();
        private final Array<Vector2> routePoints = new Array<Vector2>();
        private final Array<Vector2> routeMarkerPoints = new Array<Vector2>();
        private final Array<Float> routeMarkerProgresses = new Array<Float>();
        private final Array<Vector2> recoveryPoints = new Array<Vector2>();
        private RouteMetadata routeMetadata;

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder focusPoint(float x, float y) {
            focusPoint.set(x, y);
            return this;
        }

        public Builder surfaceImagePath(String surfaceImagePath) {
            this.surfaceImagePath = surfaceImagePath;
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

        public Builder checkpoint(SpawnPoint checkpoint) {
            checkpoints.add(checkpoint);
            return this;
        }

        public Builder routePoint(float x, float y) {
            routePoints.add(new Vector2(x, y));
            return this;
        }

        public Builder routePoint(Vector2 point) {
            if (point != null) {
                routePoint(point.x, point.y);
            }
            return this;
        }

        public Builder routeMarkerPoint(float x, float y) {
            return routeMarkerPoint(x, y, 0f);
        }

        public Builder routeMarkerPoint(float x, float y, float progress) {
            routeMarkerPoints.add(new Vector2(x, y));
            routeMarkerProgresses.add(progress);
            return this;
        }

        public Builder routeMarkerPoint(Vector2 point) {
            if (point != null) {
                routeMarkerPoint(point.x, point.y);
            }
            return this;
        }

        public Builder routeMarkerPoint(Vector2 point, float progress) {
            if (point != null) {
                routeMarkerPoint(point.x, point.y, progress);
            }
            return this;
        }

        public Builder routeMetadata(RouteMetadata routeMetadata) {
            this.routeMetadata = routeMetadata == null ? null : new RouteMetadata(routeMetadata);
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
            for (int i = 0; i < checkpoints.size; i++) {
                checkpoints.set(i, checkpoints.get(i).scale(factor));
            }
            for (int i = 0; i < routePoints.size; i++) {
                routePoints.get(i).scl(factor);
            }
            for (int i = 0; i < routeMarkerPoints.size; i++) {
                routeMarkerPoints.get(i).scl(factor);
                routeMarkerProgresses.set(i, routeMarkerProgresses.get(i) * factor);
            }
            if (routeMetadata != null) {
                routeMetadata = routeMetadata.scale(factor);
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
                    surfaceImagePath,
                    focusPoint,
                    solidZones,
                    holeZones,
                    spawnPoints,
                    checkpoints,
                    routePoints,
                    routeMarkerPoints,
                    toFloatArray(routeMarkerProgresses),
                    recoveryPoints,
                    routeMetadata);
        }

        private static float[] toFloatArray(Array<Float> values) {
            float[] out = new float[values.size];
            for (int i = 0; i < values.size; i++) {
                out[i] = values.get(i);
            }
            return out;
        }
    }

    public static final class RouteMetadata {
        public final float sampleStep;
        public final float routeLength;
        public final float[] sampleX;
        public final float[] sampleY;
        public final float[] tangentX;
        public final float[] tangentY;
        public final float[] curvature;
        public final float[] nextCornerDistance;
        public final float[] nextCornerDirection;
        public final float[] nextCornerSeverity;
        public final float[] leftClearance;
        public final float[] rightClearance;
        public final float[] roadWidth;
        public final int lookupWidth;
        public final int lookupHeight;
        public final float lookupMinX;
        public final float lookupMinY;
        public final float lookupCellWidth;
        public final float lookupCellHeight;
        public final int[] lookupSampleIndex;

        public RouteMetadata(
                float sampleStep,
                float routeLength,
                float[] sampleX,
                float[] sampleY,
                float[] tangentX,
                float[] tangentY,
                float[] curvature,
                float[] nextCornerDistance,
                float[] nextCornerDirection,
                float[] nextCornerSeverity,
                float[] leftClearance,
                float[] rightClearance,
                float[] roadWidth,
                int lookupWidth,
                int lookupHeight,
                float lookupMinX,
                float lookupMinY,
                float lookupCellWidth,
                float lookupCellHeight,
                int[] lookupSampleIndex) {
            this.sampleStep = sampleStep;
            this.routeLength = routeLength;
            this.sampleX = copy(sampleX);
            this.sampleY = copy(sampleY);
            this.tangentX = copy(tangentX);
            this.tangentY = copy(tangentY);
            this.curvature = copy(curvature);
            this.nextCornerDistance = copy(nextCornerDistance);
            this.nextCornerDirection = copy(nextCornerDirection);
            this.nextCornerSeverity = copy(nextCornerSeverity);
            this.leftClearance = copy(leftClearance);
            this.rightClearance = copy(rightClearance);
            this.roadWidth = copy(roadWidth);
            this.lookupWidth = lookupWidth;
            this.lookupHeight = lookupHeight;
            this.lookupMinX = lookupMinX;
            this.lookupMinY = lookupMinY;
            this.lookupCellWidth = lookupCellWidth;
            this.lookupCellHeight = lookupCellHeight;
            this.lookupSampleIndex = copy(lookupSampleIndex);
        }

        private RouteMetadata(RouteMetadata other) {
            this(
                    other.sampleStep,
                    other.routeLength,
                    other.sampleX,
                    other.sampleY,
                    other.tangentX,
                    other.tangentY,
                    other.curvature,
                    other.nextCornerDistance,
                    other.nextCornerDirection,
                    other.nextCornerSeverity,
                    other.leftClearance,
                    other.rightClearance,
                    other.roadWidth,
                    other.lookupWidth,
                    other.lookupHeight,
                    other.lookupMinX,
                    other.lookupMinY,
                    other.lookupCellWidth,
                    other.lookupCellHeight,
                    other.lookupSampleIndex);
        }

        private boolean isUsable(float expectedRouteLength) {
            int count = sampleCount();
            return count >= 2
                    && sampleStep > 0.0001f
                    && routeLength > 0.001f
                    && Math.abs(routeLength - expectedRouteLength) <= Math.max(0.25f, expectedRouteLength * 0.03f)
                    && tangentX.length == count
                    && tangentY.length == count
                    && curvature.length == count
                    && nextCornerDistance.length == count
                    && nextCornerDirection.length == count
                    && nextCornerSeverity.length == count
                    && leftClearance.length == count
                    && rightClearance.length == count
                    && roadWidth.length == count;
        }

        private boolean hasSamples() {
            return sampleCount() >= 2;
        }

        private int sampleCount() {
            return Math.min(sampleX.length, sampleY.length);
        }

        private float sampleProgress(int index) {
            return wrapProgress(index * sampleStep);
        }

        private int sampleIndexForProgress(float progress) {
            if (!hasSamples()) {
                return 0;
            }
            return wrapSampleIndex(MathUtils.floor(wrapProgress(progress) / sampleStep));
        }

        private int wrapSampleIndex(int index) {
            int count = sampleCount();
            if (count <= 0) {
                return 0;
            }
            int wrapped = index % count;
            return wrapped < 0 ? wrapped + count : wrapped;
        }

        private int lookupSampleIndex(float x, float y) {
            if (lookupSampleIndex == null
                    || lookupWidth <= 0
                    || lookupHeight <= 0
                    || lookupCellWidth <= 0f
                    || lookupCellHeight <= 0f
                    || lookupSampleIndex.length != lookupWidth * lookupHeight) {
                return -1;
            }
            int cellX = MathUtils.clamp((int) ((x - lookupMinX) / lookupCellWidth), 0, lookupWidth - 1);
            int cellY = MathUtils.clamp((int) ((y - lookupMinY) / lookupCellHeight), 0, lookupHeight - 1);
            return lookupSampleIndex[cellY * lookupWidth + cellX];
        }

        private void findPoint(float progress, Vector2 out) {
            int index = sampleIndexForProgress(progress);
            int nextIndex = wrapSampleIndex(index + 1);
            float alpha = sampleAlpha(progress, index);
            out.set(
                    MathUtils.lerp(sampleX[index], sampleX[nextIndex], alpha),
                    MathUtils.lerp(sampleY[index], sampleY[nextIndex], alpha));
        }

        private void findTangent(float progress, Vector2 out) {
            int index = sampleIndexForProgress(progress);
            int nextIndex = wrapSampleIndex(index + 1);
            float alpha = sampleAlpha(progress, index);
            out.set(
                    MathUtils.lerp(tangentX[index], tangentX[nextIndex], alpha),
                    MathUtils.lerp(tangentY[index], tangentY[nextIndex], alpha));
            if (out.isZero(0.0001f)) {
                out.set(tangentX[index], tangentY[index]);
            }
            if (out.isZero(0.0001f)) {
                out.set(0f, 1f);
            } else {
                out.nor();
            }
        }

        private float sampleValue(float progress, float[] values, float fallback) {
            int count = sampleCount();
            if (values == null || values.length < count || count < 2) {
                return fallback;
            }
            int index = sampleIndexForProgress(progress);
            int nextIndex = wrapSampleIndex(index + 1);
            return MathUtils.lerp(values[index], values[nextIndex], sampleAlpha(progress, index));
        }

        private float sampleAlpha(float progress, int index) {
            return MathUtils.clamp((wrapProgress(progress) - sampleProgress(index)) / sampleStep, 0f, 1f);
        }

        private float wrapProgress(float progress) {
            if (routeLength <= 0.001f) {
                return 0f;
            }
            float wrapped = progress % routeLength;
            return wrapped < 0f ? wrapped + routeLength : wrapped;
        }

        private RouteMetadata scale(float factor) {
            float[] scaledSampleX = copy(sampleX);
            float[] scaledSampleY = copy(sampleY);
            float[] scaledLeftClearance = copy(leftClearance);
            float[] scaledRightClearance = copy(rightClearance);
            float[] scaledRoadWidth = copy(roadWidth);
            for (int i = 0; i < scaledSampleX.length; i++) {
                scaledSampleX[i] *= factor;
                scaledSampleY[i] *= factor;
            }
            for (int i = 0; i < scaledLeftClearance.length; i++) {
                scaledLeftClearance[i] *= factor;
                scaledRightClearance[i] *= factor;
                scaledRoadWidth[i] *= factor;
            }
            return new RouteMetadata(
                    sampleStep * factor,
                    routeLength * factor,
                    scaledSampleX,
                    scaledSampleY,
                    tangentX,
                    tangentY,
                    curvature,
                    nextCornerDistance,
                    nextCornerDirection,
                    nextCornerSeverity,
                    scaledLeftClearance,
                    scaledRightClearance,
                    scaledRoadWidth,
                    lookupWidth,
                    lookupHeight,
                    lookupMinX * factor,
                    lookupMinY * factor,
                    lookupCellWidth * factor,
                    lookupCellHeight * factor,
                    lookupSampleIndex);
        }

        private static float[] copy(float[] values) {
            return values == null ? new float[0] : Arrays.copyOf(values, values.length);
        }

        private static int[] copy(int[] values) {
            return values == null ? new int[0] : Arrays.copyOf(values, values.length);
        }
    }
}
