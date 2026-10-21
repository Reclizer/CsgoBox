package com.reclizer.csgobox.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReelMotionTest {
    @Test void everyLandingKeepsTheServerWinnerUnderThePointerAtAnyResolution() {
        for (float start : new float[]{0.935F, 1, 1.11F}) {
            for (float landing : new float[]{0.12F, 0.5F, 0.88F}) {
                var motion = new ReelMotion(start, landing);
                for (int width : new int[]{320, 854, 1920, 3840}) {
                    float left = motion.left(ReelMotion.WINNER, ReelMotion.DURATION) * width;
                    float right = left + width * ReelMotion.CARD_WIDTH;
                    assertTrue(left < width/2F && width/2F < right);
                }
                assertEquals(ReelMotion.WINNER, motion.underPointer(ReelMotion.DURATION));
            }
        }
    }

    @Test void animationMovesContinuouslyAndStopsWithoutOvershoot() {
        var motion = new ReelMotion(1, 0.5F);
        float previous = motion.left(ReelMotion.WINNER, -1);
        for (float time = 0; time <= ReelMotion.DURATION + 10; time += 0.25F) {
            float next = motion.left(ReelMotion.WINNER, time);
            assertTrue(next <= previous + 0.00001F);
            previous = next;
        }
        assertEquals(motion.left(ReelMotion.WINNER, ReelMotion.DURATION), previous);
        assertTrue(motion.left(0, 10) - motion.left(0, 20) > motion.left(0, 120) - motion.left(0, 130));
    }
}
