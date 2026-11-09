package com.aamoreadditions.farmer;

import com.aamoreadditions.upgrade.UpgradeLogic.Plugin;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class FarmerRipeningMathTest {
    static Plugin speed(int tier){return new Plugin("SPEED",tier);}
    static Plugin efficiency(int tier){return new Plugin("EFFICIENCY",tier);}
    @Test void noPluginDefaultsThirtyTicksFortyFiveDrawsInBothModes(){
        for(boolean mode:new boolean[]{false,true})assertEquals(new FarmerRipeningMath.Plan(30,45),FarmerRipeningMath.evaluate(List.of(),mode));
    }
    @Test void independentFiveSpeedIUsesSharedSpeedTenAndRoundsToTwentySeven(){
        assertEquals(new FarmerRipeningMath.Plan(10,27),FarmerRipeningMath.evaluate(Collections.nCopies(5,speed(1)),false));
    }
    @Test void singleTiersUseCeilingAndHalfUpNotHarvestFloor(){
        assertEquals(new FarmerRipeningMath.Plan(15,41),FarmerRipeningMath.evaluate(List.of(speed(1)),false));
        assertEquals(new FarmerRipeningMath.Plan(10,36),FarmerRipeningMath.evaluate(List.of(speed(2)),false));
        assertEquals(new FarmerRipeningMath.Plan(10,34),FarmerRipeningMath.evaluate(List.of(speed(3)),false));
    }
    @Test void dependencyUsesOnlyHighestSpeedNotAllPrerequisiteMultipliers(){
        assertEquals(new FarmerRipeningMath.Plan(10,34),FarmerRipeningMath.evaluate(List.of(speed(1),speed(2),speed(3)),true));
    }
    @Test void mixedIndependentGradesMultiplyUtilizationWithoutBoundaryLoss(){
        assertEquals(new FarmerRipeningMath.Plan(10,25),FarmerRipeningMath.evaluate(List.of(speed(1),speed(2),speed(3),speed(1)),false));
        assertEquals(new FarmerRipeningMath.Plan(10,32),FarmerRipeningMath.evaluate(List.of(speed(1),speed(2)),false));
    }
    @Test void efficiencyDoesNotChangeRipeningBudgetOrSpeed(){
        assertEquals(new FarmerRipeningMath.Plan(30,45),FarmerRipeningMath.evaluate(List.of(efficiency(1),efficiency(2)),false));
        assertEquals(FarmerRipeningMath.evaluate(List.of(speed(1)),true),FarmerRipeningMath.evaluate(List.of(speed(1),efficiency(1),efficiency(2)),true));
    }
    @Test void tenSpeedPluginsHitBothLowerBounds(){
        assertEquals(new FarmerRipeningMath.Plan(10,25),FarmerRipeningMath.evaluate(Collections.nCopies(10,speed(3)),false));
    }
    @Test void everyActualInvocationCostsFixedThreeHundredEvenWithNoGrowth(){
        var energy=new AtomicInteger(900);var calls=new AtomicInteger();
        assertEquals(3,FarmerRipeningMath.run(45,1,energy::get,n->0,i->{calls.incrementAndGet();return true;},amount->energy.addAndGet(-amount)));
        assertEquals(0,energy.get());assertEquals(3,calls.get());
    }
    @Test void belowThreeHundredCannotDrawOrTick(){
        assertEquals(0,FarmerRipeningMath.run(45,1,()->299,n->{fail("no draw");return 0;},i->{fail("no tick");return true;},n->fail("no charge")));
    }
    @Test void noCandidatesNeverChargeOrSelect(){
        assertEquals(0,FarmerRipeningMath.run(45,0,()->100000,n->{fail("no draw");return 0;},i->true,n->fail("no charge")));
    }
    @Test void staleMatureOrAttachedDrawsAreSkippedWithoutFreeRerolls(){
        var draws=new AtomicInteger();
        assertEquals(0,FarmerRipeningMath.run(45,1,()->100000,n->{draws.incrementAndGet();return 0;},i->false,n->fail("no charge")));
        assertEquals(45,draws.get());
    }
    @Test void selectionIsWithReplacementAndEachDrawRevalidates(){
        var energy=new AtomicInteger(900);var state=new AtomicInteger();
        assertEquals(1,FarmerRipeningMath.run(45,1,energy::get,n->0,i->state.getAndIncrement()==0,n->energy.addAndGet(-n)));
        assertEquals(600,energy.get());assertEquals(45,state.get());
    }
    @Test void actualRunNeverExceedsTheDrawBudgetEvenWithFullRing(){
        var energy=new AtomicInteger(1000000);
        assertEquals(27,FarmerRipeningMath.run(27,1,energy::get,n->0,i->true,n->energy.addAndGet(-n)));
        assertEquals(1000000-27*300,energy.get());
    }
    @Test void insufficientRemainderStopsBeforeAnotherSelection(){
        var energy=new AtomicInteger(899);var selections=new AtomicInteger();
        assertEquals(2,FarmerRipeningMath.run(45,1,energy::get,n->{selections.incrementAndGet();return 0;},i->true,n->energy.addAndGet(-n)));
        assertEquals(299,energy.get());assertEquals(2,selections.get());
    }
    @Test void arrowDependsOnlyOnRingPresenceAndPositiveMachineFe(){
        assertFalse(FarmerRipeningMath.arrowActive(false,1000));assertFalse(FarmerRipeningMath.arrowActive(true,0));
        assertTrue(FarmerRipeningMath.arrowActive(true,1));assertTrue(FarmerRipeningMath.arrowActive(true,100000));
    }
}
