package com.reclizer.csgobox.utils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroundAnimationTest {
    @Test void lidReversalStartsAtItsCurrentPositionAndFinishesNaturally() {
        float midway=GroundAnimation.lid(0,true,8);
        assertEquals(.5F,midway,1e-6);
        assertEquals(midway,GroundAnimation.lid(midway,false,0));
        assertEquals(0,GroundAnimation.lid(midway,false,16));
        assertEquals(1,GroundAnimation.lid(midway,true,16));
        for (int i=0;i<100;i++) assertTrue(GroundAnimation.lid(midway,false,i)>=0);
    }
    @Test void seededWorldMotionAgreesAcrossObserversAndStopsOnWinner() {
        for (long seed:new long[]{0,1,Long.MAX_VALUE,-52}) {
            var a=GroundAnimation.motion(seed,16); var b=GroundAnimation.motion(seed,16);
            for (int tick=0;tick<=ReelMotion.DURATION;tick++) assertEquals(a.left(45,tick),b.left(45,tick));
            float left=a.left(ReelMotion.WINNER,ReelMotion.DURATION);
            assertTrue(left<.5 && left+ReelMotion.CARD_WIDTH>.5);
        }
    }
}
