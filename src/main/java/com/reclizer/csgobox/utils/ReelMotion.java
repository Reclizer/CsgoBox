package com.reclizer.csgobox.utils;

/** Continuous, gently accelerated spin and deceleration, with a guaranteed server-result landing. */
public final class ReelMotion {
    public static final int DURATION = 145;
    public static final int WINNER = 45;
    public static final int CARD_COUNT = 50;
    public static final float SPACING = 0.20F;
    public static final float CARD_WIDTH = 0.18F;
    private static final double[] DISTANCE = new double[DURATION + 1];
    static {
        for (int tick = 0; tick < DURATION; tick++) {
            // Simpson integration; no velocity threshold or early stationary tail.
            DISTANCE[tick + 1] = DISTANCE[tick]
                    + (velocity(tick) + 4 * velocity(tick + 0.5) + velocity(tick + 1)) / 6;
        }
    }

    private static double velocity(double tick) {
        double ramp = Math.min(1, tick / 6);
        double remaining = Math.max(0, 1 - tick / DURATION);
        return ramp * ramp * (3 - 2 * ramp) * remaining * remaining;
    }
    private final float start;
    private final float travel;

    /** initialPhase selects a position within one card spacing on an already populated strip. */
    public ReelMotion(float initialPhase, float landing) {
        if (!Float.isFinite(initialPhase) || initialPhase < 0 || initialPhase > 1
                || !Float.isFinite(landing) || landing < 0 || landing > 1)
            throw new IllegalArgumentException("Invalid reel position");
        this.start = -initialPhase * SPACING;
        this.travel = start + WINNER * SPACING - (0.5F - landing * CARD_WIDTH);
    }

    public float left(int index, float ticks) {
        float time = Math.max(0, Math.min(DURATION, ticks));
        int whole = (int) time;
        double distance = DISTANCE[whole];
        if (whole < DURATION) {
            // Hermite interpolation preserves velocity across tick boundaries, including the stop.
            double f = time - whole, f2 = f * f, f3 = f2 * f;
            distance = (2*f3 - 3*f2 + 1) * DISTANCE[whole]
                    + (f3 - 2*f2 + f) * velocity(whole)
                    + (-2*f3 + 3*f2) * DISTANCE[whole + 1]
                    + (f3 - f2) * velocity(whole + 1);
        }
        return start + index * SPACING - travel * (float)(distance / DISTANCE[DURATION]);
    }

    public int underPointer(float ticks) { return (int)Math.floor((0.5F - left(0, ticks)) / SPACING); }
}
