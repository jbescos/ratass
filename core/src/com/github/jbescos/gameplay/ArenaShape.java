package com.github.jbescos.gameplay;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public abstract class ArenaShape {
    public static ArenaShape rectangle(float centerX, float centerY, float width, float height) {
        return new RectangleShape(centerX, centerY, width, height);
    }

    public static ArenaShape circle(float centerX, float centerY, float radius) {
        return new CircleShape(centerX, centerY, radius);
    }

    public abstract boolean contains(float x, float y);

    public abstract float depthInside(float x, float y);

    public abstract float distanceOutside(float x, float y);

    public abstract void closestPointInside(float x, float y, float margin, Vector2 out);

    public abstract void closestPointOutside(float x, float y, float margin, Vector2 out);

    public abstract void draw(
            ShapeRenderer shapeRenderer,
            float offsetX,
            float offsetY,
            float expansion);

    public abstract float getMinX();

    public abstract float getMaxX();

    public abstract float getMinY();

    public abstract float getMaxY();

    private static final class RectangleShape extends ArenaShape {
        private final float centerX;
        private final float centerY;
        private final float halfWidth;
        private final float halfHeight;

        private RectangleShape(float centerX, float centerY, float width, float height) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.halfWidth = width * 0.5f;
            this.halfHeight = height * 0.5f;
        }

        @Override
        public boolean contains(float x, float y) {
            return Math.abs(x - centerX) <= halfWidth && Math.abs(y - centerY) <= halfHeight;
        }

        @Override
        public float depthInside(float x, float y) {
            if (!contains(x, y)) {
                return 0f;
            }

            float dx = halfWidth - Math.abs(x - centerX);
            float dy = halfHeight - Math.abs(y - centerY);
            return Math.min(dx, dy);
        }

        @Override
        public float distanceOutside(float x, float y) {
            if (contains(x, y)) {
                return 0f;
            }

            float dx = Math.max(Math.abs(x - centerX) - halfWidth, 0f);
            float dy = Math.max(Math.abs(y - centerY) - halfHeight, 0f);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        @Override
        public void closestPointInside(float x, float y, float margin, Vector2 out) {
            float effectiveHalfWidth = Math.max(0f, halfWidth - margin);
            float effectiveHalfHeight = Math.max(0f, halfHeight - margin);
            out.set(
                    MathUtils.clamp(x, centerX - effectiveHalfWidth, centerX + effectiveHalfWidth),
                    MathUtils.clamp(y, centerY - effectiveHalfHeight, centerY + effectiveHalfHeight));
        }

        @Override
        public void closestPointOutside(float x, float y, float margin, Vector2 out) {
            float effectiveHalfWidth = halfWidth + margin;
            float effectiveHalfHeight = halfHeight + margin;

            float leftX = centerX - effectiveHalfWidth;
            float rightX = centerX + effectiveHalfWidth;
            float bottomY = centerY - effectiveHalfHeight;
            float topY = centerY + effectiveHalfHeight;

            float leftDistance = Math.abs(x - leftX);
            float rightDistance = Math.abs(rightX - x);
            float bottomDistance = Math.abs(y - bottomY);
            float topDistance = Math.abs(topY - y);

            out.set(MathUtils.clamp(x, leftX, rightX), MathUtils.clamp(y, bottomY, topY));

            float bestDistance = leftDistance;
            out.x = leftX;

            if (rightDistance < bestDistance) {
                bestDistance = rightDistance;
                out.x = rightX;
            }

            if (bottomDistance < bestDistance) {
                bestDistance = bottomDistance;
                out.x = MathUtils.clamp(x, leftX, rightX);
                out.y = bottomY;
            }

            if (topDistance < bestDistance) {
                out.x = MathUtils.clamp(x, leftX, rightX);
                out.y = topY;
            }
        }

        @Override
        public void draw(
                ShapeRenderer shapeRenderer,
                float offsetX,
                float offsetY,
                float expansion) {
            float width = halfWidth * 2f + expansion * 2f;
            float height = halfHeight * 2f + expansion * 2f;
            if (width <= 0f || height <= 0f) {
                return;
            }

            shapeRenderer.rect(
                    centerX + offsetX - width * 0.5f,
                    centerY + offsetY - height * 0.5f,
                    width,
                    height);
        }

        @Override
        public float getMinX() {
            return centerX - halfWidth;
        }

        @Override
        public float getMaxX() {
            return centerX + halfWidth;
        }

        @Override
        public float getMinY() {
            return centerY - halfHeight;
        }

        @Override
        public float getMaxY() {
            return centerY + halfHeight;
        }
    }

    private static final class CircleShape extends ArenaShape {
        private final float centerX;
        private final float centerY;
        private final float radius;

        private CircleShape(float centerX, float centerY, float radius) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
        }

        @Override
        public boolean contains(float x, float y) {
            return Vector2.dst2(centerX, centerY, x, y) <= radius * radius;
        }

        @Override
        public float depthInside(float x, float y) {
            if (!contains(x, y)) {
                return 0f;
            }

            return radius - Vector2.dst(centerX, centerY, x, y);
        }

        @Override
        public float distanceOutside(float x, float y) {
            float distance = Vector2.dst(centerX, centerY, x, y);
            if (distance <= radius) {
                return 0f;
            }
            return distance - radius;
        }

        @Override
        public void closestPointInside(float x, float y, float margin, Vector2 out) {
            float effectiveRadius = Math.max(0f, radius - margin);
            out.set(x - centerX, y - centerY);
            if (out.isZero(0.0001f)) {
                out.set(centerX + effectiveRadius, centerY);
                return;
            }

            float currentLength = out.len();
            if (currentLength > effectiveRadius) {
                out.scl(effectiveRadius / currentLength);
            }
            out.add(centerX, centerY);
        }

        @Override
        public void closestPointOutside(float x, float y, float margin, Vector2 out) {
            float effectiveRadius = radius + margin;
            out.set(x - centerX, y - centerY);
            if (out.isZero(0.0001f)) {
                out.set(centerX + effectiveRadius, centerY);
                return;
            }

            float currentLength = out.len();
            if (currentLength < effectiveRadius) {
                out.scl(effectiveRadius / currentLength);
            }
            out.add(centerX, centerY);
        }

        @Override
        public void draw(
                ShapeRenderer shapeRenderer,
                float offsetX,
                float offsetY,
                float expansion) {
            float effectiveRadius = radius + expansion;
            if (effectiveRadius <= 0f) {
                return;
            }

            shapeRenderer.circle(centerX + offsetX, centerY + offsetY, effectiveRadius, 56);
        }

        @Override
        public float getMinX() {
            return centerX - radius;
        }

        @Override
        public float getMaxX() {
            return centerX + radius;
        }

        @Override
        public float getMinY() {
            return centerY - radius;
        }

        @Override
        public float getMaxY() {
            return centerY + radius;
        }
    }
}
