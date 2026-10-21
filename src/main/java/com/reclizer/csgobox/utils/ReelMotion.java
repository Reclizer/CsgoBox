package com.reclizer.csgobox.utils;

/** Original deceleration profile in screen-width units, with a guaranteed server-result landing. */
public final class ReelMotion {
    public static final int DURATION = 145;
    public static final int WINNER = 45;
    public static final float SPACING = 0.20F;
    public static final float CARD_WIDTH = 0.18F;
    private static final double[] DISTANCE = new double[DURATION + 1];
    static {
        for (int tick = 0; tick < DURATION; tick++) {
            double time = tick / 20.0;
            double velocity = (1.6 * time + 0.8) / Math.pow(2, 1.5 * time - 5.2);
            DISTANCE[tick + 1] = DISTANCE[tick] + (velocity < 0.3 ? 0 : velocity);
        }
    }
    private final float start;
    private final float travel;

    public ReelMotion(float start, float landing) {
        if (!Float.isFinite(start) || !Float.isFinite(landing) || landing < 0 || landing > 1)
            throw new IllegalArgumentException("Invalid reel position");
        this.start = start;
        this.travel = start + WINNER * SPACING - (0.5F - landing * CARD_WIDTH);
    }

    public float left(int index, float ticks) {
        float time = Math.max(0, Math.min(DURATION, ticks));
        int whole = (int) time;
        double distance = DISTANCE[whole];
        if (whole < DURATION) distance += (DISTANCE[whole + 1] - distance) * (time - whole);
        return start + index * SPACING - travel * (float)(distance / DISTANCE[DURATION]);
    }

    public int underPointer(float ticks) { return (int)Math.floor((0.5F - left(0, ticks)) / SPACING); }
}
