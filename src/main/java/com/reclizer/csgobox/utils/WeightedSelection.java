package com.reclizer.csgobox.utils;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.random.RandomGenerator;

public final class WeightedSelection {
    private WeightedSelection() {}

    /** Empty grades have zero probability. Luck only adjusts enabled rare grades. */
    public static int grade(RandomGenerator random, List<Integer> weights, IntPredicate available, float luck) {
        long[] effective = effectiveWeights(weights, available, luck);
        long total = java.util.Arrays.stream(effective).sum();
        long roll = random.nextLong(total);
        for (int i = 0; i < 5; i++) {
            if (roll < effective[i]) return 5 - i;
            roll -= effective[i];
        }
        throw new AssertionError("Unreachable weighted selection");
    }

    /** Shared by actual draws and the editor probability preview. Gold-to-blue order. */
    public static long[] effectiveWeights(List<Integer> weights, IntPredicate available, float luck) {
        if (weights.size() != 5) throw new IllegalArgumentException("Expected five weights");
        long[] effective = new long[5];
        long total = 0;
        long bonus = Float.isFinite(luck) ? (long) Math.max(-Integer.MAX_VALUE, Math.min(Integer.MAX_VALUE, luck * 2.0)) : 0;
        for (int i = 0; i < 5; i++) {
            int weight = weights.get(i);
            if (weight < 0) throw new IllegalArgumentException("Negative weight");
            if (weight > 0 && available.test(5 - i)) {
                effective[i] = Math.max(0L, weight + (i < 4 ? bonus : 0));
                total += effective[i];
            }
        }
        // Negative luck must not make an otherwise usable box impossible to open.
        if (total == 0 && bonus < 0) return effectiveWeights(weights, available, 0);
        if (total == 0) throw new IllegalArgumentException("No weighted rewards available");
        return effective;
    }
}
