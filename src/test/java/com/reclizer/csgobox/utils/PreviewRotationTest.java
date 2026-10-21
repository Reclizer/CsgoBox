package com.reclizer.csgobox.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PreviewRotationTest {
    @Test void draggingDownTiltsTheFrontDownAndDraggingRightTurnsItRight() {
        var motion = new PreviewRotation();
        motion.grab(0);
        motion.drag(20, 20, 0);
        motion.update(300);
        assertTrue(motion.yaw() > 0);
        assertTrue(motion.pitch() < 0);
        motion.drag(-40, -40, 300);
        motion.update(600);
        assertTrue(motion.yaw() < 0);
        assertTrue(motion.pitch() > 0);
    }

    @Test void releaseHoldsBrieflyThenReturnsSmoothlyToExactlyNeutral() {
        var motion = settledDrag();
        float before = motion.yaw();
        motion.release(500);
        assertEquals(before, motion.yaw());
        motion.update(600);
        assertEquals(before, motion.yaw(), 0.0001);
        float previous = motion.yaw();
        for (int time = 630; time <= 2000; time += 10) {
            motion.update(time);
            assertTrue(motion.yaw() <= previous + 0.0001);
            assertTrue(motion.yaw() >= 0);
            assertTrue(previous - motion.yaw() < 0.06, "No snap between frames");
            previous = motion.yaw();
        }
        assertEquals(0, motion.yaw());
        assertEquals(0, motion.pitch());
    }

    @Test void returnTimingIsIndependentOfFrameRateAndHandlesAStall() {
        var direct = settledDrag();
        direct.release(500);
        direct.update(1000);
        for (int fps : new int[]{30, 60, 144}) {
            var motion = settledDrag();
            motion.release(500);
            for (int frame = 1; 500 + frame * 1000.0 / fps < 1000; frame++)
                motion.update(500 + Math.round(frame * 1000.0 / fps));
            motion.update(1000);
            assertEquals(direct.yaw(), motion.yaw(), 0.00001);
            assertEquals(direct.pitch(), motion.pitch(), 0.00001);
            motion.update(100000);
            assertEquals(0, motion.yaw());
        }
    }

    @Test void regrabbingDuringReturnDoesNotJumpAndReleaseCanBeRepeated() {
        var motion = settledDrag();
        motion.release(500);
        motion.update(800);
        float angle = motion.yaw();
        motion.grab(800);
        motion.update(900);
        assertEquals(angle, motion.yaw());
        motion.release(900);
        motion.release(1100); // Focus loss must not postpone return indefinitely.
        motion.update(2500);
        assertEquals(0, motion.yaw());
    }

    @Test void pointerEventFrequencyDoesNotChangeRotationDistance() {
        var one = new PreviewRotation();
        var many = new PreviewRotation();
        one.grab(0); many.grab(0);
        one.drag(40, -30, 0);
        for (int i = 0; i < 20; i++) many.drag(2, -1.5, 0);
        one.update(300); many.update(300);
        assertEquals(one.yaw(), many.yaw(), 0.00001);
        assertEquals(one.pitch(), many.pitch(), 0.00001);
    }

    @Test void extremeDragsAreBoundedAndUnpressedMouseMovementDoesNothing() {
        var motion = new PreviewRotation();
        assertFalse(motion.drag(10000, 10000, 0));
        motion.grab(0);
        motion.drag(10000, 10000, 0);
        motion.update(500);
        assertTrue(motion.yaw() <= 3);
        assertTrue(motion.pitch() >= -1.15F);
        motion.release(500);
        assertFalse(motion.drag(10000, 10000, 600));
        motion.update(3000);
        assertEquals(0, motion.yaw());
        assertEquals(0, motion.pitch());
    }

    private PreviewRotation settledDrag() {
        var motion = new PreviewRotation();
        motion.grab(0);
        motion.drag(40, -20, 0);
        motion.update(500);
        return motion;
    }
}
