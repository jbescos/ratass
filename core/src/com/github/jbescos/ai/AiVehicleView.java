package com.github.jbescos.ai;

import com.badlogic.gdx.physics.box2d.Body;

public interface AiVehicleView {
    Body getBody();

    boolean isActive();

    boolean isPlayerControlled();

    boolean hasGrowthBoost();

    boolean hasRamCharge();

    int getVehicleId();

    int getScore();

    int getLastAttackerId();

    float getRecentImpactTime();
}
