package com.reclizer.csgobox.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReelMotionTest {
    @Test void spinStartsAndEndsWithNearZeroVelocityWithoutEarlyCutoff() {
        var motion = new ReelMotion(1, 0.5F);
        float initial = motion.left(0, 0) - motion.left(0, 0.25F);
        float cruising = motion.left(0, 6) - motion.left(0, 6.25F);
        assertTrue(initial < cruising / 20);
        assertTrue(motion.left(0, ReelMotion.DURATION - 4) > motion.left(0, ReelMotion.DURATION));
        float finalStep = motion.left(0, ReelMotion.DURATION-1) - motion.left(0, ReelMotion.DURATION);
        float precedingStep = motion.left(0, ReelMotion.DURATION-2) - motion.left(0, ReelMotion.DURATION-1);
        assertTrue(finalStep < precedingStep && finalStep >= 0);
    }

    @Test void everyLandingKeepsTheServerWinnerUnderThePointerAtAnyResolution() {
        for (float phase : new float[]{0, 0.5F, 1}) {
            for (float landing : new float[]{0.12F, 0.5F, 0.88F}) {
                var motion = new ReelMotion(phase, landing);
                for (int width : new int[]{320, 854, 1920, 3840}) {
                    float left = motion.left(ReelMotion.WINNER, ReelMotion.DURATION) * width;
                    float right = left + width * ReelMotion.CARD_WIDTH;
                    assertTrue(left < width/2F && width/2F < right);
                }
                assertEquals(ReelMotion.WINNER, motion.underPointer(ReelMotion.DURATION));
            }
        }
    }

    @Test void populatedStripCoversTheViewportFromTheFirstFrameUntilTheStop() {
        for (float phase : new float[]{0, 0.25F, 0.5F, 0.75F, 1}) {
            for (float landing : new float[]{0.12F, 0.5F, 0.88F}) {
                var motion = new ReelMotion(phase, landing);
                assertTrue(motion.underPointer(0) > 0);
                assertEquals(motion.left(0, 0), motion.left(0, -ReelPresentation.LEAD_IN));
                for (float tick = 0; tick <= ReelMotion.DURATION; tick += 0.25F) {
                    assertTrue(motion.left(0, tick) <= 0, "No empty leading section");
                    assertTrue(motion.left(ReelMotion.CARD_COUNT - 1, tick) + ReelMotion.CARD_WIDTH >= 1,
                            "The tail must also cover the right edge at the stop");
                    int visible = 0;
                    for (int i = 0; i < ReelMotion.CARD_COUNT; i++) {
                        float x = motion.left(i, tick);
                        if (x < 1 && x + ReelMotion.CARD_WIDTH > 0) visible++;
                    }
                    assertTrue(visible >= 5, "Always show a full row of cards");
                }
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
