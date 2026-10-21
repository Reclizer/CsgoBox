package com.reclizer.csgobox.utils;

/** Resolution-independent presentation. The central landing region is never distorted. */
public final class ReelPresentation {
    public static final int HOLD = 9, REVEAL = 14;
    public static final int REVEAL_START = ReelMotion.DURATION + HOLD;
    public static final int END = REVEAL_START + REVEAL;
    private ReelPresentation() {}

    public static float smooth(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    public static float reveal(float ticks) { return smooth((ticks - REVEAL_START) / REVEAL); }
    public static float depth(float centerX) { return smooth((Math.abs(centerX - 0.5F) - 0.12F) / 0.38F); }
    public static float edgeAlpha(float x) { return smooth(x / 0.14F) * smooth((1 - x) / 0.14F); }
    public static float brightness(float x) { return 1 - 0.32F * depth(x); }
    public static float mix(float a, float b, float t) { return a + (b - a) * t; }
}
