package com.reclizer.csgobox.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReelPresentationTest {
    @Test void everyAllowedWinnerLandingStaysUndistortedAndFullyVisible() {
        for (float landing = 0.12F; landing <= 0.88F; landing += 0.01F) {
            var motion = new ReelMotion(1, landing);
            float center = motion.left(ReelMotion.WINNER, ReelMotion.DURATION) + ReelMotion.CARD_WIDTH/2;
            assertEquals(0, ReelPresentation.depth(center));
            assertEquals(1, ReelPresentation.edgeAlpha(center));
        }
    }

    @Test void edgeFadeIsSymmetricBoundedAndKeepsTheCenterClear() {
        for (int i = 0; i <= 100; i++) {
            float x = i/100F, alpha = ReelPresentation.edgeAlpha(x);
            assertTrue(alpha >= 0 && alpha <= 1);
            assertEquals(alpha, ReelPresentation.edgeAlpha(1-x), 0.00001);
            assertEquals(ReelPresentation.depth(x), ReelPresentation.depth(1-x), 0.00001);
        }
        assertEquals(0, ReelPresentation.edgeAlpha(0));
        assertEquals(0, ReelPresentation.edgeAlpha(1));
        assertEquals(1, ReelPresentation.edgeAlpha(0.5F));
    }

    @Test void revealWaitsForTheStopAndHoldAndEndsAtTheInspectionPose() {
        assertTrue(ReelPresentation.REVEAL_START > ReelPresentation.LEAD_IN + ReelMotion.DURATION);
        assertEquals(0, ReelPresentation.reveal(ReelPresentation.REVEAL_START));
        assertEquals(1, ReelPresentation.reveal(ReelPresentation.END));
        float previous = 0;
        for (float tick = ReelPresentation.REVEAL_START; tick <= ReelPresentation.END; tick += 0.25F) {
            float next = ReelPresentation.reveal(tick);
            assertTrue(next >= previous && next <= 1);
            previous = next;
        }
    }
}
