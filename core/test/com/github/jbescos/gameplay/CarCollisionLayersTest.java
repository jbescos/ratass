package com.github.jbescos.gameplay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CarCollisionLayersTest {
    @Test
    public void enlargedEnvelopeTouchesCarsButNeverMapWalls() {
        short normalCategory = CarCollisionLayers.roadFixtureCategory(false);
        short normalMask = CarCollisionLayers.roadFixtureMask(false);
        short enlargedRoadCategory = CarCollisionLayers.roadFixtureCategory(true);
        short enlargedRoadMask = CarCollisionLayers.roadFixtureMask(true);

        assertTrue(CarCollisionLayers.canCollide(
                normalCategory,
                normalMask,
                normalCategory,
                normalMask));
        assertTrue(CarCollisionLayers.canCollide(
                normalCategory,
                normalMask,
                CarCollisionLayers.MAP_WALLS,
                CarCollisionLayers.ROAD));
        assertTrue(CarCollisionLayers.canCollide(
                enlargedRoadCategory,
                enlargedRoadMask,
                CarCollisionLayers.MAP_WALLS,
                CarCollisionLayers.ROAD));
        assertFalse(CarCollisionLayers.canCollide(
                enlargedRoadCategory,
                enlargedRoadMask,
                normalCategory,
                normalMask));
        assertTrue(CarCollisionLayers.canCollide(
                CarCollisionLayers.CARS,
                CarCollisionLayers.CARS,
                normalCategory,
                normalMask));
        assertTrue(CarCollisionLayers.canCollide(
                CarCollisionLayers.CARS,
                CarCollisionLayers.CARS,
                CarCollisionLayers.CARS,
                CarCollisionLayers.CARS));
        assertFalse(CarCollisionLayers.canCollide(
                CarCollisionLayers.CARS,
                CarCollisionLayers.CARS,
                CarCollisionLayers.MAP_WALLS,
                CarCollisionLayers.ROAD));
    }
}
