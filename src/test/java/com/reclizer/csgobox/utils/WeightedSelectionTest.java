package com.reclizer.csgobox.utils;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class WeightedSelectionTest {
    private static Random roll(long value) {
        return new Random(1) {
            @Override public long nextLong(long bound) {
                assertTrue(value >= 0 && value < bound);
                return value;
            }
        };
    }

    @Test void everyBoundaryHasExactWeightAndZeroHasNoChance() {
        List<Integer> weights = List.of(0, 2, 0, 3, 1);
        int[] expected = {4,4,2,2,2,1};
        for (int i = 0; i < expected.length; i++)
            assertEquals(expected[i], WeightedSelection.grade(roll(i), weights, g -> true, 0));
    }

    @Test void emptyGradesAreExcludedIncludingAllButOne() {
        for (int i = 0; i < 1000; i++)
            assertEquals(3, WeightedSelection.grade(new Random(i), List.of(2,5,6,20,625), g -> g == 3, 0));
        assertThrows(IllegalArgumentException.class, () -> WeightedSelection.grade(new Random(0), List.of(1,1,1,1,1), g -> false, 0));
    }

    @Test void rejectsNegativeAndZeroWeightsAndHandlesLargeTotals() {
        assertThrows(IllegalArgumentException.class, () -> WeightedSelection.grade(roll(0), List.of(0,0,0,0,0), g -> true, 0));
        assertThrows(IllegalArgumentException.class, () -> WeightedSelection.grade(roll(0), List.of(-1,1,1,1,1), g -> true, 0));
        var weights = List.of(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE);
        assertEquals(1, WeightedSelection.grade(roll(5L * Integer.MAX_VALUE - 1), weights, g -> true, 0));
        assertDoesNotThrow(() -> WeightedSelection.grade(new Random(0), weights, g -> true, Float.MAX_VALUE));
    }

    @Test void luckCannotEnableZeroWeightsOrEmptyGrades() {
        assertEquals(1, WeightedSelection.grade(roll(0), List.of(0,0,0,0,1), g -> true, 100));
        assertEquals(5, WeightedSelection.grade(roll(0), List.of(1,0,0,0,0), g -> g == 5, -100));
    }
}
