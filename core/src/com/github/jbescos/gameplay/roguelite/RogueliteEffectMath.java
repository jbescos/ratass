package com.github.jbescos.gameplay.roguelite;

final class RogueliteEffectMath {
    private RogueliteEffectMath() {
    }

    static float levelValue(
            int level,
            float levelZero,
            float levelOne,
            float levelTwo) {
        switch (level) {
            case 0:
                return levelZero;
            case 1:
                return levelOne;
            case 2:
                return levelTwo;
            default:
                return 0f;
        }
    }

    static float circularDelta(float from, float to, float length) {
        float delta = to - from;
        if (length <= 0f) {
            return delta;
        }
        float halfLength = length * 0.5f;
        if (delta > halfLength) {
            delta -= length;
        } else if (delta < -halfLength) {
            delta += length;
        }
        return delta;
    }

    static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
