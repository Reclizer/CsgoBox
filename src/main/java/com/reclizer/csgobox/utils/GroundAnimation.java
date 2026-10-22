package com.reclizer.csgobox.utils;
import java.util.Random;

/** Shared timing and seeded motion for every observer of the same placed box. */
public final class GroundAnimation {
    public static final int LID_TICKS=16, SETTLE_TICKS=10;
    public static final int TOTAL_TICKS=LID_TICKS+ReelMotion.DURATION+SETTLE_TICKS;
    private GroundAnimation() {}
    public static ReelMotion motion(long seed,int samples) {
        var random=new Random(seed);
        for (int i=0;i<ReelMotion.CARD_COUNT;i++) random.nextInt(Math.max(1,samples));
        return new ReelMotion(random.nextFloat(),random.nextFloat(.12F,.88F));
    }
    public static float lid(float from,boolean open,float elapsed) {
        float t=Math.max(0,Math.min(1,elapsed/LID_TICKS)); t=t*t*(3-2*t);
        return from+((open?1:0)-from)*t;
    }
}
