package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.github.jbescos.gameplay.ArenaMap;
import com.github.jbescos.gameplay.SpawnPoint;

abstract class AbstractArenaMapDefinition implements ArenaMapDefinition {
    private final String id;
    private final String name;

    protected AbstractArenaMapDefinition(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public final ArenaMap create() {
        ArenaMap.Builder builder = ArenaMap.builder(id, name);
        define(builder);
        return builder.build();
    }

    protected abstract void define(ArenaMap.Builder builder);

    protected static String variantId(String baseId, int variant) {
        return baseId + "-" + (variant + 1);
    }

    protected static String variantName(String baseName, int variant) {
        return baseName + " " + (variant + 1);
    }

    protected final void addGridRecoveryPoints(
            ArenaMap.Builder builder,
            float halfWidth,
            float halfHeight,
            int columns,
            int rows) {
        for (int y = 0; y < rows; y++) {
            float normalizedY = rows == 1 ? 0.5f : (float) y / (rows - 1);
            float pointY = MathUtils.lerp(-halfHeight, halfHeight, normalizedY);
            for (int x = 0; x < columns; x++) {
                float normalizedX = columns == 1 ? 0.5f : (float) x / (columns - 1);
                float pointX = MathUtils.lerp(-halfWidth, halfWidth, normalizedX);
                builder.recoveryPoint(pointX, pointY);
            }
        }
    }

    protected final void addGridRecoveryPointsAvoidingCircles(
            ArenaMap.Builder builder,
            float halfWidth,
            float halfHeight,
            int columns,
            int rows,
            float safeRadius,
            Vector2... circleCenters) {
        float safeRadiusSq = safeRadius * safeRadius;
        for (int y = 0; y < rows; y++) {
            float normalizedY = rows == 1 ? 0.5f : (float) y / (rows - 1);
            float pointY = MathUtils.lerp(-halfHeight, halfHeight, normalizedY);
            for (int x = 0; x < columns; x++) {
                float normalizedX = columns == 1 ? 0.5f : (float) x / (columns - 1);
                float pointX = MathUtils.lerp(-halfWidth, halfWidth, normalizedX);
                if (isOutsideCircles(pointX, pointY, safeRadiusSq, circleCenters)) {
                    builder.recoveryPoint(pointX, pointY);
                }
            }
        }
    }

    protected final void addGridRecoveryPointsAvoidingRectangle(
            ArenaMap.Builder builder,
            float halfWidth,
            float halfHeight,
            int columns,
            int rows,
            float avoidedHalfWidth,
            float avoidedHalfHeight,
            float margin) {
        float marginX = avoidedHalfWidth + margin;
        float marginY = avoidedHalfHeight + margin;
        for (int y = 0; y < rows; y++) {
            float normalizedY = rows == 1 ? 0.5f : (float) y / (rows - 1);
            float pointY = MathUtils.lerp(-halfHeight, halfHeight, normalizedY);
            for (int x = 0; x < columns; x++) {
                float normalizedX = columns == 1 ? 0.5f : (float) x / (columns - 1);
                float pointX = MathUtils.lerp(-halfWidth, halfWidth, normalizedX);
                if (Math.abs(pointX) > marginX || Math.abs(pointY) > marginY) {
                    builder.recoveryPoint(pointX, pointY);
                }
            }
        }
    }

    protected final void addAxisRecoveryPoints(
            ArenaMap.Builder builder,
            float horizontalExtent,
            float verticalExtent,
            float spacing) {
        builder.recoveryPoint(0f, 0f);

        for (float x = spacing; x <= horizontalExtent; x += spacing) {
            builder.recoveryPoint(-x, 0f);
            builder.recoveryPoint(x, 0f);
        }

        for (float y = spacing; y <= verticalExtent; y += spacing) {
            builder.recoveryPoint(0f, -y);
            builder.recoveryPoint(0f, y);
        }
    }

    protected final void addRingRecoveryPoints(
            ArenaMap.Builder builder,
            float radius,
            int points) {
        addRingRecoveryPoints(builder, 0f, 0f, radius, points);
    }

    protected final void addRingRecoveryPoints(
            ArenaMap.Builder builder,
            float centerX,
            float centerY,
            float radius,
            int points) {
        for (int i = 0; i < points; i++) {
            float angle = MathUtils.PI2 * i / points;
            builder.recoveryPoint(
                    centerX + MathUtils.cos(angle) * radius,
                    centerY + MathUtils.sin(angle) * radius);
        }
    }

    protected final void addCardinalSpawns(
            ArenaMap.Builder builder,
            float radiusX,
            float radiusY) {
        builder.spawn(SpawnPoint.facingPoint(0f, -radiusY, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-radiusX, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(radiusX, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, radiusY, 0f, 0f));
    }

    private boolean isOutsideCircles(
            float x,
            float y,
            float safeRadiusSq,
            Vector2... circleCenters) {
        for (int i = 0; i < circleCenters.length; i++) {
            Vector2 circleCenter = circleCenters[i];
            if (Vector2.dst2(x, y, circleCenter.x, circleCenter.y) < safeRadiusSq) {
                return false;
            }
        }
        return true;
    }
}
