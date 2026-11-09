package com.aamoreadditions.farmer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FarmerRipeningClockTest {
    static boolean step(FarmerRipeningClock clock){return clock.step(30,9,2,42,true,true);}
    @Test void exactlyThirtyRunnableTicksPerBaseCycle(){
        var c=new FarmerRipeningClock();for(int n=1;n<30;n++)assertFalse(step(c));assertTrue(step(c));
        for(int n=1;n<30;n++)assertFalse(step(c));assertTrue(step(c));
    }
    @Test void tenTickMinimumIsAnActualPeriodNotEleven(){
        var c=new FarmerRipeningClock();for(int n=1;n<=30;n++)assertEquals(n%10==0,c.step(10,9,2,42,true,true));
    }
    @Test void redstoneAndNoMachineEnergyDoNotAdvanceClock(){
        var c=new FarmerRipeningClock();for(int n=0;n<15;n++)assertFalse(step(c));
        for(int n=0;n<1000;n++)assertFalse(c.step(30,9,2,42,true,false));
        for(int n=0;n<14;n++)assertFalse(step(c));assertTrue(step(c));
    }
    @Test void harvestSleepingDoesNotPauseThisSeparateClock(){
        var c=new FarmerRipeningClock();for(int n=1;n<=90;n++)assertEquals(n%30==0,step(c));
    }
    @Test void disableResetsButDoesNotDeleteOrSpendRingEnergy(){
        var c=new FarmerRipeningClock();for(int n=0;n<29;n++)assertFalse(step(c));
        assertFalse(c.step(30,9,2,42,false,true));
        for(int n=0;n<29;n++)assertFalse(step(c));assertTrue(step(c));
    }
    @Test void areaFacingPositionAndSpeedChangesRestartFullPeriod(){
        var c=new FarmerRipeningClock();for(int n=0;n<29;n++)assertFalse(step(c));
        assertFalse(c.step(30,11,2,42,true,true));
        assertFalse(c.step(30,11,3,42,true,true));
        assertFalse(c.step(30,11,3,43,true,true));
        for(int n=1;n<10;n++)assertFalse(c.step(10,11,3,43,true,true));
        assertTrue(c.step(10,11,3,43,true,true));
    }
    @Test void loadRingReplacementUpgradeAndIllegalResetStartFresh(){
        var c=new FarmerRipeningClock();for(int n=0;n<29;n++)assertFalse(step(c));c.reset();
        for(int n=0;n<29;n++)assertFalse(step(c));assertTrue(step(c));
    }
}
