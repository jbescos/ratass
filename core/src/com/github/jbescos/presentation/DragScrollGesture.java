package com.github.jbescos.presentation;

/** Tracks a pointer drag while preserving short gestures as taps. */
public final class DragScrollGesture {
    private int pointer = -1;
    private float startX;
    private float startY;
    private float lastY;
    private boolean dragging;

    public void begin(int pointer, float x, float y) {
        this.pointer = pointer;
        startX = x;
        startY = y;
        lastY = y;
        dragging = false;
    }

    public float drag(int pointer, float x, float y, float dragSlop) {
        if (pointer != this.pointer) {
            return 0f;
        }
        if (!dragging) {
            float deltaX = x - startX;
            float deltaY = y - startY;
            float safeSlop = Math.max(0f, dragSlop);
            if (deltaX * deltaX + deltaY * deltaY <= safeSlop * safeSlop) {
                return 0f;
            }
            dragging = true;
            lastY = y;
            return deltaY;
        }
        float deltaY = y - lastY;
        lastY = y;
        return deltaY;
    }

    public boolean end(int pointer) {
        if (pointer != this.pointer) {
            return false;
        }
        boolean tap = !dragging;
        cancel();
        return tap;
    }

    public void cancel() {
        pointer = -1;
        dragging = false;
    }

    public boolean isActive() {
        return pointer >= 0;
    }

    public boolean isDragging() {
        return dragging;
    }

    public int getPointer() {
        return pointer;
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }
}
