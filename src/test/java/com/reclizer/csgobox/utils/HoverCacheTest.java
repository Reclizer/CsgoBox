package com.reclizer.csgobox.utils;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class HoverCacheTest {
    @Test void stationaryAndMovingHoverReuseDescriptionUntilRefreshDeadline() {
        var cache = new HoverCache<Integer>();
        var calls = new AtomicInteger();
        var item = new Object();
        for (int frame = 0; frame < 250; frame++)
            assertEquals(1, cache.get(item, 0, frame, calls::incrementAndGet));
        assertEquals(2, cache.get(item, 0, 250, calls::incrementAndGet));
        assertEquals(2, calls.get());
    }

    @Test void changingItemOrModifierImmediatelyRefreshesWithoutShowingStaleText() {
        var cache = new HoverCache<Integer>();
        var calls = new AtomicInteger();
        var first = new Object();
        var second = new Object();
        assertEquals(1, cache.get(first, 0, 0, calls::incrementAndGet));
        assertEquals(2, cache.get(first, 1, 1, calls::incrementAndGet));
        assertEquals(3, cache.get(second, 1, 2, calls::incrementAndGet));
        assertEquals(4, cache.get(second, 8, 3, calls::incrementAndGet));
    }

    @Test void leavingOrResizingTheScreenInvalidatesTheCachedDescription() {
        var cache = new HoverCache<Integer>();
        var calls = new AtomicInteger();
        var item = new Object();
        cache.get(item, 0, 0, calls::incrementAndGet);
        cache.clear();
        assertEquals(2, cache.get(item, 0, 1, calls::incrementAndGet));
    }
}
