package com.aamoreadditions.effect;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Native-flow model around the actual shared gate; source contracts check Mixin integration. */
class OilBurnPauseTest {
    static class Oil {
        int stored,capacity=50000,burn=20,maxBurn=20,rate=1000,fluid=1000;
        void tick(){
            if(burn>0 && rate>0){
                if(OilBurnPause.mayAdvance(stored,capacity))burn--;
                if(OilBurnPause.mayAdvance(stored,capacity))stored+=Math.min(capacity-stored,rate);
            } else if(stored<capacity && fluid>=50){fluid-=50;burn=maxBurn;}
        }
    }
    @Test void fullBeforeFirstFuelDoesNotStartOrConsumeFluid(){
        var o=new Oil();o.burn=0;o.stored=o.capacity;o.tick();assertEquals(0,o.burn);assertEquals(1000,o.fluid);
    }
    @Test void alreadyBurningFullKeepsRemainingTimeProductionAndFuelMetadata(){
        var o=new Oil();o.stored=o.capacity;
        for(int n=0;n<1000;n++)o.tick();
        assertEquals(20,o.burn);assertEquals(20,o.maxBurn);assertEquals(1000,o.rate);assertEquals(1000,o.fluid);assertEquals(o.capacity,o.stored);
    }
    @Test void aSingleFeGapCostsAWholeBurnTickAndDiscardsAllOverflow(){
        var o=new Oil();o.stored=o.capacity-1;o.tick();assertEquals(19,o.burn);assertEquals(o.capacity,o.stored);
        o.tick();assertEquals(19,o.burn);
    }
    @Test void exactOneTickRoomAdvancesExactlyOnce(){
        var o=new Oil();o.stored=o.capacity-o.rate;o.tick();assertEquals(19,o.burn);assertEquals(o.capacity,o.stored);
    }
    @Test void externalExtractionResumesRetainedFuelWithoutStartingAnotherPortion(){
        var o=new Oil();o.stored=o.capacity;o.tick();o.stored-=500;o.tick();
        assertEquals(19,o.burn);assertEquals(o.capacity,o.stored);assertEquals(1000,o.fluid);
    }
    @Test void repeatedExtractFillPauseDoesNotConsumeExtraTicks(){
        var o=new Oil();o.stored=o.capacity;
        for(int n=0;n<5;n++){for(int idle=0;idle<50;idle++)o.tick();o.stored--;o.tick();}
        assertEquals(15,o.burn);assertEquals(o.capacity,o.stored);
    }
    @Test void bothUpgradeModesKeepExistingRatesAndRoundedFuelDuration(){
        for(boolean dependency:new boolean[]{true,false}){
            var o=new Oil();
            double speed=dependency?5:UpgradeAggregation.speed(java.util.List.of(2.,3.,5.,2.));
            double utilization=dependency?.75*2:UpgradeAggregation.generatorUtilization(java.util.List.of(.9,.8,.75,.9),java.util.List.of(1.5,2.));
            o.rate=UpgradeMath.generatorOutput(100,speed);o.burn=o.maxBurn=UpgradeMath.generatorBurnTime(120,speed,utilization);
            int before=o.burn;o.stored=o.capacity;o.tick();assertEquals(before,o.burn);
            o.stored--;o.tick();assertEquals(before-1,o.burn);assertEquals(o.capacity,o.stored);
        }
    }
    @Test void pausePredicateNeverRequiresAnEntireUpgradedTickToFit(){
        assertTrue(OilBurnPause.mayAdvance(Integer.MAX_VALUE-1,Integer.MAX_VALUE));
        assertFalse(OilBurnPause.mayAdvance(Integer.MAX_VALUE,Integer.MAX_VALUE));
        assertFalse(OilBurnPause.mayAdvance(50001,50000));
    }
}
