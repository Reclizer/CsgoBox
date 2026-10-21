package com.reclizer.csgobox.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoxViewportTest {
    @Test void canvasStaysCenteredAndFitsWideTallAndSmallWindows() {
        for (int[] size : new int[][]{{320, 240}, {427, 240}, {960, 540}, {1920, 1080},
                {2560, 1080}, {800, 1200}, {3840, 2160}}) {
            var view = BoxViewport.fit(size[0], size[1]);
            assertTrue(view.left() >= -1e-9 && view.top() >= -1e-9);
            assertTrue(view.screenX(BoxViewport.WIDTH) <= size[0] + 1e-9);
            assertTrue(view.screenY(BoxViewport.HEIGHT) <= size[1] + 1e-9);
            assertEquals(size[0] / 2.0, view.screenX(BoxViewport.WIDTH / 2.0), 1e-9);
            assertEquals(size[1] / 2.0, view.screenY(BoxViewport.HEIGHT / 2.0), 1e-9);
            assertEquals(view.screenX(100) - view.screenX(0), view.screenY(100) - view.screenY(0), 1e-9);
        }
    }

    @Test void buttonsAndDragDistancesMapBackToTheSameDesignCoordinates() {
        for (int[] size : new int[][]{{320, 240}, {960, 540}, {2560, 1080}, {800, 1200}}) {
            var view = BoxViewport.fit(size[0], size[1]);
            double buttonX = BoxViewport.WIDTH * 0.69, buttonY = BoxViewport.HEIGHT * 0.965;
            assertEquals(buttonX, view.localX(view.screenX(buttonX)), 1e-9);
            assertEquals(buttonY, view.localY(view.screenY(buttonY)), 1e-9);
            assertEquals(120, view.localDelta(view.screenX(600) - view.screenX(480)), 1e-9);
            assertEquals(-60, view.localDelta(view.screenY(200) - view.screenY(260)), 1e-9);
            assertTrue(view.localX(view.left() - 1) < 0);
            assertTrue(view.localY(view.top() - 1) < 0);
        }
    }

    @Test void minecraftGuiScaleDoesNotChangePhysicalLayoutOrDragSensitivity() {
        var baseline = BoxViewport.fit(1920, 1080);
        for (int guiScale : new int[]{1, 2, 3, 4}) {
            var view = BoxViewport.fit(1920 / guiScale, 1080 / guiScale);
            assertEquals(baseline.screenX(480), view.screenX(480) * guiScale, 1e-9);
            assertEquals(baseline.screenY(520), view.screenY(520) * guiScale, 1e-9);
            assertEquals(baseline.localDelta(120), view.localDelta(120.0 / guiScale), 1e-9);
        }
    }

    @Test void aTemporarilyZeroSizedWindowNeverProducesInvalidCoordinates() {
        var view = BoxViewport.fit(0, 0);
        assertTrue(view.scale() > 0);
        assertTrue(Double.isFinite(view.localX(0)));
        assertTrue(Double.isFinite(view.localY(0)));
    }
}
