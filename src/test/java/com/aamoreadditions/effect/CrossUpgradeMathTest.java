package com.aamoreadditions.effect;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CrossUpgradeMathTest {
    @Test void defaultCrossSpeedIsNotProcessorSpeed() {
        assertEquals(6.5,CrossUpgradeMath.speed(List.of(1,2,3),false,CrossUpgradeMath::defaultSpeed));
        assertEquals(3,CrossUpgradeMath.speed(List.of(1,2,3),true,CrossUpgradeMath::defaultSpeed));
        assertEquals(7.2,CrossUpgradeMath.speed(List.of(1,2,3,1),false,CrossUpgradeMath::defaultSpeed),1e-12);
    }
    @Test void highestTierMeansTierNotLargestConfiguredCoefficient() {
        assertEquals(.5,CrossUpgradeMath.speed(List.of(1,2,3),true,t->t==3?.5:50));
        assertEquals(100.5,CrossUpgradeMath.speed(List.of(1,2,3),false,t->t==3?.5:50));
    }
    @Test void totalFeMatchesFourNativeIntegerDividedStandsAtEveryFixedSpeed() {
        for(int base:List.of(1,7,100,997)) for(int perStand:List.of(1,5000,100003)) {
            long total=CrossUpgradeMath.totalEnergy(perStand,base);
            assertEquals(4L*(perStand/base)*base,total);
            for(double speed:List.of(.01,1.,1.5,2.,3.,6.5,7.2,10.5,100000.)) {
                int ticks=UpgradeMath.time(base,speed); long charged=0;
                for(int p=0;p<ticks;p++) {
                    long cost=CrossUpgradeMath.tickEnergy(total,ticks,p);
                    assertTrue(cost>=0); charged+=cost;
                }
                assertEquals(total,charged,"base="+base+" speed="+speed);
            }
        }
    }
    @Test void extremeIntRecipeAndProgressDoNotOverflowLongMultiplication() {
        long total=CrossUpgradeMath.totalEnergy(Integer.MAX_VALUE,Integer.MAX_VALUE);
        assertEquals(8589934588L,total);
        assertEquals(4,CrossUpgradeMath.tickEnergy(total,Integer.MAX_VALUE,Integer.MAX_VALUE-1));
        assertEquals(4,CrossUpgradeMath.tickEnergy(total,Integer.MAX_VALUE,Integer.MAX_VALUE/2));
        assertEquals(total,CrossUpgradeMath.tickEnergy(total,1,0));
    }
    @Test void longGuiProgressUsesSharedOverflowSafeBarFraction() {
        assertEquals(14,UpgradeMath.energyBarHeight(100000,200000,28));
        assertEquals(28,UpgradeMath.energyBarHeight(Integer.MAX_VALUE,Integer.MAX_VALUE,28));
        assertEquals(0,UpgradeMath.energyBarHeight(0,200000,28));
    }
}
