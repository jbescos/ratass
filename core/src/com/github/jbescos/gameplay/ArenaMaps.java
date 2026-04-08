package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public final class ArenaMaps {
    private static final int VARIANTS_PER_FAMILY = 5;

    private ArenaMaps() {
    }

    public static Array<ArenaMap> createDefaultSet() {
        Array<ArenaMap> maps = new Array<ArenaMap>();
        for (int variant = 0; variant < VARIANTS_PER_FAMILY; variant++) {
            maps.add(createBoilerDeck(variant));
            maps.add(createCrosswindJunction(variant));
            maps.add(createSplitShift(variant));
            maps.add(createDonutBowl(variant));
            maps.add(createTwinCrater(variant));
            maps.add(createFrameRing(variant));
            maps.add(createCausewayClash(variant));
            maps.add(createCoreBreach(variant));
            maps.add(createPillboxLanes(variant));
            maps.add(createSatelliteCrown(variant));
        }
        return maps;
    }

    private static ArenaMap createBoilerDeck(int variant) {
        float width = 40f + variant * 2.4f;
        float height = 24f + variant * 1.1f;
        float sideTowerWidth = 5.4f + variant * 0.5f;
        float sideTowerHeight = height * 0.58f;
        float balconyWidth = 15f + variant * 1.5f;
        float balconyHeight = 5.2f + variant * 0.5f;

        ArenaMap.Builder builder = ArenaMap.builder(
                        variantId("boiler-deck", variant),
                        variantName("Boiler Deck", variant))
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, width, height));

        if (variant % 2 == 0) {
            builder.solid(ArenaShape.rectangle(0f, height * 0.50f, balconyWidth, balconyHeight))
                    .solid(ArenaShape.rectangle(0f, -height * 0.50f, balconyWidth * 0.8f, balconyHeight * 0.9f));
        } else {
            builder.solid(ArenaShape.rectangle(-width * 0.50f, 0f, sideTowerWidth, sideTowerHeight))
                    .solid(ArenaShape.rectangle(width * 0.50f, 0f, sideTowerWidth, sideTowerHeight));
        }

        addCardinalSpawns(builder, width * 0.28f, height * 0.28f);
        addGridRecoveryPoints(builder, width * 0.34f, height * 0.32f, 5, 4);
        return builder.build();
    }

    private static ArenaMap createCrosswindJunction(int variant) {
        float horizontalWidth = 36f + variant * 2.2f;
        float horizontalHeight = 9.6f + (variant % 2) * 0.9f;
        float verticalWidth = 15.5f + variant * 0.9f;
        float verticalHeight = 26f + variant * 1.9f;
        ArenaMap.Builder builder = ArenaMap.builder(
                        variantId("crosswind-junction", variant),
                        variantName("Crosswind Junction", variant))
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, horizontalWidth, horizontalHeight))
                .solid(ArenaShape.rectangle(0f, 0f, verticalWidth, verticalHeight));

        addCardinalSpawns(builder, horizontalWidth * 0.35f, verticalHeight * 0.35f);
        addAxisRecoveryPoints(builder, horizontalWidth * 0.34f, verticalHeight * 0.34f, 4.4f);
        builder.recoveryPoint(-verticalWidth * 0.20f, verticalHeight * 0.22f)
                .recoveryPoint(verticalWidth * 0.20f, -verticalHeight * 0.22f);
        return builder.build();
    }

    private static ArenaMap createSplitShift(int variant) {
        float islandOffset = 10.4f + variant * 0.9f;
        float islandWidth = 13.2f + variant * 0.8f;
        float islandHeight = 14.4f + variant * 1.0f;
        float bridgeWidth = 9.4f + variant * 0.8f;
        float bridgeHeight = 5.5f + (variant % 3) * 0.6f;
        float bridgeY = (variant - 2) * 0.9f;

        ArenaMap.Builder builder = ArenaMap.builder(
                        variantId("split-shift", variant),
                        variantName("Split Shift", variant))
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(-islandOffset, 0f, islandWidth, islandHeight))
                .solid(ArenaShape.rectangle(islandOffset, 0f, islandWidth, islandHeight))
                .solid(ArenaShape.rectangle(0f, bridgeY, bridgeWidth, bridgeHeight));

        if (variant >= 3) {
            builder.solid(ArenaShape.rectangle(0f, -bridgeY, bridgeWidth * 0.72f, bridgeHeight * 0.72f));
        }

        builder.spawn(SpawnPoint.facingPoint(-islandOffset, -islandHeight * 0.24f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-islandOffset, islandHeight * 0.24f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(islandOffset, -islandHeight * 0.24f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(islandOffset, islandHeight * 0.24f, 0f, 0f));

        builder.recoveryPoint(-islandOffset, -islandHeight * 0.30f)
                .recoveryPoint(-islandOffset, 0f)
                .recoveryPoint(-islandOffset, islandHeight * 0.30f)
                .recoveryPoint(-bridgeWidth * 0.45f, bridgeY)
                .recoveryPoint(0f, 0f)
                .recoveryPoint(bridgeWidth * 0.45f, bridgeY)
                .recoveryPoint(islandOffset, -islandHeight * 0.30f)
                .recoveryPoint(islandOffset, 0f)
                .recoveryPoint(islandOffset, islandHeight * 0.30f);
        return builder.build();
    }

    private static ArenaMap createDonutBowl(int variant) {
        float outerRadius = 13f + variant * 1.1f;
        float holeRadius = 4.2f + variant * 0.25f + (variant % 2) * 0.35f;
        float ringRadius = (outerRadius + holeRadius) * 0.5f;

        ArenaMap.Builder builder = ArenaMap.builder(
                        variantId("donut-bowl", variant),
                        variantName("Donut Bowl", variant))
                .focusPoint(0f, 0f)
                .solid(ArenaShape.circle(0f, 0f, outerRadius))
                .hole(ArenaShape.circle(0f, 0f, holeRadius));

        addCardinalSpawns(builder, ringRadius, ringRadius);
        addRingRecoveryPoints(builder, ringRadius, 12 + variant * 2);
        addRingRecoveryPoints(builder, ringRadius * 0.82f, 8 + variant);
        return builder.build();
    }

    private static ArenaMap createTwinCrater(int variant) {
        float width = 42f + variant * 2.4f;
        float height = 27f + variant * 1.2f;
        float holeRadius = 3.2f + variant * 0.24f;
        float holeOffset = 7.2f + variant * 0.55f;
        boolean verticalLayout = variant % 2 == 1;
        Vector2 craterA = verticalLayout
                ? new Vector2(0f, holeOffset * 0.78f)
                : new Vector2(-holeOffset, 0f);
        Vector2 craterB = verticalLayout
                ? new Vector2(0f, -holeOffset * 0.78f)
                : new Vector2(holeOffset, 0f);

        ArenaMap.Builder builder = ArenaMap.builder(
                        variantId("twin-crater", variant),
                        variantName("Twin Crater", variant))
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, width, height))
                .hole(ArenaShape.circle(craterA.x, craterA.y, holeRadius))
                .hole(ArenaShape.circle(craterB.x, craterB.y, holeRadius));

        addCardinalSpawns(builder, width * 0.30f, height * 0.30f);
        addGridRecoveryPointsAvoidingCircles(
                builder,
                width * 0.34f,
                height * 0.32f,
                5,
                4,
                holeRadius + 1.35f,
                craterA,
                craterB);
        builder.recoveryPoint(0f, height * 0.32f)
                .recoveryPoint(0f, -height * 0.32f);
        return builder.build();
    }

    private static ArenaMap createFrameRing(int variant) {
        float outerWidth = 46f + variant * 2.6f;
        float outerHeight = 30f + variant * 1.5f;
        float innerWidth = 15.5f + variant * 1.6f;
        float innerHeight = 10.5f + variant * 1.0f;
        if (variant % 2 == 0) {
            innerWidth += 1.6f;
        } else {
            innerHeight += 1.2f;
        }

        ArenaMap.Builder builder = ArenaMap.builder(
                        variantId("frame-ring", variant),
                        variantName("Frame Ring", variant))
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, outerWidth, outerHeight))
                .hole(ArenaShape.rectangle(0f, 0f, innerWidth, innerHeight));

        addCardinalSpawns(
                builder,
                innerWidth * 0.5f + 3.4f,
                innerHeight * 0.5f + 2.6f);
        addGridRecoveryPointsAvoidingRectangle(
                builder,
                outerWidth * 0.36f,
                outerHeight * 0.34f,
                6,
                5,
                innerWidth * 0.5f,
                innerHeight * 0.5f,
                1.2f);
        return builder.build();
    }

    private static ArenaMap createCausewayClash(int variant) {
        float spineWidth = 36f + variant * 2.4f;
        float spineHeight = 11f + variant * 0.8f;
        float podWidth = 11f + variant * 0.9f;
        float podHeight = 10.5f + variant * 0.7f;
        float podOffsetX = 11.5f + variant * 0.9f;
        float podOffsetY = 7.2f + (variant % 3) * 0.8f;

        ArenaMap.Builder builder = ArenaMap.builder(
                        variantId("causeway-clash", variant),
                        variantName("Causeway Clash", variant))
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, spineWidth, spineHeight))
                .solid(ArenaShape.rectangle(-podOffsetX, podOffsetY, podWidth, podHeight))
                .solid(ArenaShape.rectangle(podOffsetX, -podOffsetY, podWidth, podHeight));

        if (variant >= 2) {
            builder.solid(ArenaShape.rectangle(-podOffsetX, podOffsetY * 0.15f, podWidth * 0.70f, 6.4f))
                    .solid(ArenaShape.rectangle(podOffsetX, -podOffsetY * 0.15f, podWidth * 0.70f, 6.4f));
        }

        builder.spawn(SpawnPoint.facingPoint(-spineWidth * 0.34f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(spineWidth * 0.34f, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-podOffsetX, podOffsetY, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(podOffsetX, -podOffsetY, 0f, 0f));

        builder.recoveryPoint(-podOffsetX, podOffsetY)
                .recoveryPoint(-spineWidth * 0.22f, 0f)
                .recoveryPoint(0f, 0f)
                .recoveryPoint(spineWidth * 0.22f, 0f)
                .recoveryPoint(podOffsetX, -podOffsetY);
        addGridRecoveryPoints(builder, spineWidth * 0.28f, spineHeight * 0.24f, 4, 2);
        return builder.build();
    }

    private static ArenaMap createCoreBreach(int variant) {
        float horizontalWidth = 38f + variant * 2.1f;
        float horizontalHeight = 11f + (variant % 2) * 0.8f;
        float verticalWidth = 17f + variant * 0.8f;
        float verticalHeight = 28f + variant * 1.8f;

        ArenaMap.Builder builder = ArenaMap.builder(
                        variantId("core-breach", variant),
                        variantName("Core Breach", variant))
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, horizontalWidth, horizontalHeight))
                .solid(ArenaShape.rectangle(0f, 0f, verticalWidth, verticalHeight));

        addCardinalSpawns(builder, horizontalWidth * 0.30f, verticalHeight * 0.30f);

        if (variant % 2 == 0) {
            float breachRadius = 3.0f + variant * 0.25f;
            builder.hole(ArenaShape.circle(0f, 0f, breachRadius));
            addRingRecoveryPoints(builder, breachRadius + 3.7f, 10 + variant * 2);
        } else {
            float breachWidth = 6.4f + variant * 0.8f;
            float breachHeight = 5.2f + variant * 0.6f;
            builder.hole(ArenaShape.rectangle(0f, 0f, breachWidth, breachHeight));
            builder.recoveryPoint(-(breachWidth * 0.5f + 2.4f), 0f)
                    .recoveryPoint(0f, -(breachHeight * 0.5f + 2.4f))
                    .recoveryPoint(breachWidth * 0.5f + 2.4f, 0f)
                    .recoveryPoint(0f, breachHeight * 0.5f + 2.4f);
        }

        builder.recoveryPoint(0f, verticalHeight * 0.32f)
                .recoveryPoint(0f, -verticalHeight * 0.32f)
                .recoveryPoint(horizontalWidth * 0.32f, 0f)
                .recoveryPoint(-horizontalWidth * 0.32f, 0f);
        return builder.build();
    }

    private static ArenaMap createPillboxLanes(int variant) {
        float width = 48f + variant * 2.0f;
        float height = 30f + variant * 1.4f;
        float holeRadius = 2.6f + variant * 0.18f;
        float holeOffsetX = 10.2f + variant * 0.5f;
        float holeOffsetY = 6.4f + variant * 0.4f;
        Vector2 holeNorthWest = new Vector2(-holeOffsetX, holeOffsetY);
        Vector2 holeNorthEast = new Vector2(holeOffsetX, holeOffsetY);
        Vector2 holeSouthWest = new Vector2(-holeOffsetX, -holeOffsetY);
        Vector2 holeSouthEast = new Vector2(holeOffsetX, -holeOffsetY);

        ArenaMap.Builder builder = ArenaMap.builder(
                        variantId("pillbox-lanes", variant),
                        variantName("Pillbox Lanes", variant))
                .focusPoint(0f, 0f)
                .solid(ArenaShape.rectangle(0f, 0f, width, height))
                .hole(ArenaShape.circle(holeNorthWest.x, holeNorthWest.y, holeRadius))
                .hole(ArenaShape.circle(holeNorthEast.x, holeNorthEast.y, holeRadius))
                .hole(ArenaShape.circle(holeSouthWest.x, holeSouthWest.y, holeRadius))
                .hole(ArenaShape.circle(holeSouthEast.x, holeSouthEast.y, holeRadius));

        addCardinalSpawns(builder, width * 0.30f, height * 0.30f);
        addGridRecoveryPointsAvoidingCircles(
                builder,
                width * 0.36f,
                height * 0.32f,
                6,
                5,
                holeRadius + 1.15f,
                holeNorthWest,
                holeNorthEast,
                holeSouthWest,
                holeSouthEast);
        builder.recoveryPoint(0f, 0f);
        return builder.build();
    }

    private static ArenaMap createSatelliteCrown(int variant) {
        float centerRadius = 8.2f + variant * 0.55f;
        float satelliteRadius = 6.4f + variant * 0.35f;
        float satelliteOffsetX = 12.2f + variant * 0.7f;
        float satelliteOffsetY = (variant - 2) * 1.1f;

        ArenaMap.Builder builder = ArenaMap.builder(
                        variantId("satellite-crown", variant),
                        variantName("Satellite Crown", variant))
                .focusPoint(0f, 0f)
                .solid(ArenaShape.circle(0f, 0f, centerRadius))
                .solid(ArenaShape.circle(-satelliteOffsetX, satelliteOffsetY, satelliteRadius))
                .solid(ArenaShape.circle(satelliteOffsetX, -satelliteOffsetY, satelliteRadius));

        if (variant >= 2) {
            builder.solid(ArenaShape.circle(0f, centerRadius + satelliteRadius * 0.52f, 4.4f + variant * 0.25f));
        }

        builder.spawn(SpawnPoint.facingPoint(-satelliteOffsetX, satelliteOffsetY, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(satelliteOffsetX, -satelliteOffsetY, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, -centerRadius * 0.55f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, centerRadius * 0.55f, 0f, 0f));

        builder.recoveryPoint(0f, 0f)
                .recoveryPoint(-satelliteOffsetX, satelliteOffsetY)
                .recoveryPoint(satelliteOffsetX, -satelliteOffsetY);
        addRingRecoveryPoints(builder, 0f, 0f, centerRadius * 0.46f, 8);
        addRingRecoveryPoints(builder, -satelliteOffsetX, satelliteOffsetY, satelliteRadius * 0.42f, 6);
        addRingRecoveryPoints(builder, satelliteOffsetX, -satelliteOffsetY, satelliteRadius * 0.42f, 6);
        if (variant >= 2) {
            builder.recoveryPoint(0f, centerRadius + satelliteRadius * 0.52f);
        }
        return builder.build();
    }

    private static void addGridRecoveryPoints(
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

    private static void addGridRecoveryPointsAvoidingCircles(
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

    private static void addGridRecoveryPointsAvoidingRectangle(
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

    private static void addAxisRecoveryPoints(
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

    private static void addRingRecoveryPoints(
            ArenaMap.Builder builder,
            float radius,
            int points) {
        addRingRecoveryPoints(builder, 0f, 0f, radius, points);
    }

    private static void addRingRecoveryPoints(
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

    private static void addCardinalSpawns(ArenaMap.Builder builder, float radiusX, float radiusY) {
        builder.spawn(SpawnPoint.facingPoint(0f, -radiusY, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(-radiusX, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(radiusX, 0f, 0f, 0f))
                .spawn(SpawnPoint.facingPoint(0f, radiusY, 0f, 0f));
    }

    private static boolean isOutsideCircles(
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

    private static String variantId(String baseId, int variant) {
        return baseId + "-" + (variant + 1);
    }

    private static String variantName(String baseName, int variant) {
        return baseName + " " + (variant + 1);
    }
}
