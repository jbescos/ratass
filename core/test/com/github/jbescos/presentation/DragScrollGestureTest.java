package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DragScrollGestureTest {
    @Test
    public void verticalDragReturnsContentMovement() {
        DragScrollGesture gesture = new DragScrollGesture();
        gesture.begin(0, 20f, 100f);

        assertEquals(0f, gesture.drag(0, 22f, 105f, 8f), 0.0001f);
        assertEquals(18f, gesture.drag(0, 22f, 118f, 8f), 0.0001f);
        assertEquals(7f, gesture.drag(0, 22f, 125f, 8f), 0.0001f);
        assertTrue(gesture.isDragging());
        assertFalse(gesture.end(0));
    }

    @Test
    public void shortGestureRemainsATap() {
        DragScrollGesture gesture = new DragScrollGesture();
        gesture.begin(2, 40f, 80f);

        assertEquals(0f, gesture.drag(2, 44f, 83f, 8f), 0.0001f);
        assertTrue(gesture.end(2));
        assertFalse(gesture.isActive());
    }

    @Test
    public void ignoresOtherPointers() {
        DragScrollGesture gesture = new DragScrollGesture();
        gesture.begin(1, 10f, 10f);

        assertEquals(0f, gesture.drag(0, 10f, 40f, 4f), 0.0001f);
        assertFalse(gesture.end(0));
        assertTrue(gesture.isActive());
    }
}
