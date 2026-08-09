package com.github.jbescos.gameplay.roguelite;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Calculates the attacker velocity produced by Impact Reversal. */
public final class ImpactReversalBounce {
    static final float MIN_REBOUND_SPEED = 12f;
    static final float MAX_REBOUND_SPEED = 32f;
    static final float INCOMING_SPEED_MULTIPLIER = 1.50f;
    static final float CLOSING_SPEED_MULTIPLIER = 1.25f;
    static final float TANGENTIAL_VELOCITY_RETENTION = 0.20f;

    private ImpactReversalBounce() {
    }

    public static Vector2 calculateVelocity(
            Vector2 out,
            Vector2 currentVelocity,
            Vector2 contactNormal,
            float awayDirection,
            float closingSpeed,
            float incomingSpeed) {
        if (out == null) {
            throw new IllegalArgumentException("out is required");
        }
        if (currentVelocity == null
                || contactNormal == null
                || !isFinite(contactNormal.x)
                || !isFinite(contactNormal.y)) {
            return out.setZero();
        }

        float normalLength = contactNormal.len();
        if (!isFinite(normalLength) || normalLength <= 0.0001f) {
            return out.set(currentVelocity);
        }
        float direction = awayDirection < 0f ? -1f : 1f;
        float awayX = contactNormal.x / normalLength * direction;
        float awayY = contactNormal.y / normalLength * direction;
        float currentAwaySpeed = currentVelocity.x * awayX + currentVelocity.y * awayY;
        float tangentX = currentVelocity.x - awayX * currentAwaySpeed;
        float tangentY = currentVelocity.y - awayY * currentAwaySpeed;
        float reboundSpeed = reboundSpeed(closingSpeed, incomingSpeed);
        return out.set(
                tangentX * TANGENTIAL_VELOCITY_RETENTION + awayX * reboundSpeed,
                tangentY * TANGENTIAL_VELOCITY_RETENTION + awayY * reboundSpeed);
    }

    static float reboundSpeed(float closingSpeed, float incomingSpeed) {
        float safeClosingSpeed = sanitizeSpeed(closingSpeed);
        float safeIncomingSpeed = sanitizeSpeed(incomingSpeed);
        return MathUtils.clamp(
                Math.max(
                        MIN_REBOUND_SPEED,
                        Math.max(
                                safeIncomingSpeed * INCOMING_SPEED_MULTIPLIER,
                                safeClosingSpeed * CLOSING_SPEED_MULTIPLIER)),
                MIN_REBOUND_SPEED,
                MAX_REBOUND_SPEED);
    }

    private static float sanitizeSpeed(float speed) {
        return isFinite(speed) ? Math.max(0f, speed) : 0f;
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
