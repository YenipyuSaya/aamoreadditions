package com.aamoreadditions.effect;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Actual scan-state class, without Minecraft/game dependencies. */
class FarmerScanStateTest {
    static FarmerScanState fresh(int side){var s=new FarmerScanState();s.configure(side,0,1,true);return s;}
    static void emptyRound(FarmerScanState s,int size,int start){for(int n=0;n<size;n++)s.observe((start+n)%size,false);}
    @Test void completeEmptyRoundAtCenterOffsetSleepsOnlyAfterAllNineActualCells() {
        var s=fresh(3);
        for(int n=0;n<8;n++){s.observe((4+n)%9,false);assertFalse(s.sleeping());}
        assertEquals(8,s.checked());s.observe(3,false);assertTrue(s.sleeping());
    }
    @Test void aMatureCellAnywherePreventsSleepForThatWholeRound() {
        var s=fresh(3);
        for(int n=0;n<9;n++)s.observe(n,n==2);
        assertFalse(s.sleeping());emptyRound(s,9,0);assertTrue(s.sleeping());
    }
    @Test void slowScanWakesOnKnownProcessableMaturity() {
        var s=fresh(3);emptyRound(s,9,0);assertTrue(s.sleeping());
        s.observe(0,true,false,true);assertFalse(s.sleeping()); // actual harvest drops fit
        for(int n=1;n<9;n++)s.observe(n,false);
        assertFalse(s.sleeping());
        emptyRound(s,9,0);assertTrue(s.sleeping());
    }
    @Test void oneCellAreaSleepsAndWakesWithoutWholeAreaExtraScans() {
        var s=fresh(1);s.observe(0,false);assertTrue(s.sleeping());
        s.observe(0,true);assertFalse(s.sleeping());
        assertEquals(20,FarmerScanState.SLOW_TICKS);
    }
    @Test void knownProcessableCandidatesNeverCompleteAnEmptyRound() {
        var s=fresh(3);for(int n=0;n<60;n++){s.observe(n%9,true);assertFalse(s.sleeping());}
    }
    @Test void redstoneOrNoEnergyPauseWithoutAdvancingActualCellAccounting() {
        var s=fresh(3);s.observe(4,false);s.observe(5,false);
        for(int tick=0;tick<1000;tick++)assertFalse(s.configure(3,0,1,true));
        assertEquals(2,s.checked());assertFalse(s.sleeping());
        for(int n=0;n<7;n++)s.observe((6+n)%9,false);assertTrue(s.sleeping());
    }
    @Test void rangeChangeWakesAndRequiresANewFullLargerRound() {
        var s=fresh(3);emptyRound(s,9,4);assertTrue(s.sleeping());
        assertTrue(s.configure(5,0,1,true));assertFalse(s.sleeping());assertEquals(0,s.checked());
        for(int n=0;n<24;n++){s.observe((12+n)%25,false);assertFalse(s.sleeping());}
        s.observe(11,false);assertTrue(s.sleeping());
    }
    @Test void facingAndPositionChangesResetEvenAtTheSameAreaSize() {
        var s=fresh(3);s.observe(0,false);s.configure(3,1,1,true);assertEquals(0,s.checked());
        emptyRound(s,9,0);assertTrue(s.sleeping());
        s.configure(3,1,2,true);assertFalse(s.sleeping());
    }
    @Test void upgradeResetWakesAndDoesNotCarryPartialRoundEvidence() {
        var s=fresh(3);emptyRound(s,9,0);s.reset();assertFalse(s.sleeping());assertEquals(0,s.checked());
        for(int n=0;n<8;n++)s.observe(n,false);assertFalse(s.sleeping());
    }
    @Test void loadAlwaysStartsAwakeAndForgetsThePreviousPartialRound() {
        var s=fresh(3);emptyRound(s,9,0);s.loaded();assertFalse(s.sleeping());
        assertTrue(s.configure(3,0,1,true));assertEquals(0,s.checked());
    }
    @Test void cursorJumpDoesNotPretendEarlierCellsCompletedTheRound() {
        var s=fresh(3);s.observe(0,false);s.observe(1,false);s.observe(7,false);
        assertEquals(1,s.checked());
        for(int n=1;n<8;n++){s.observe((7+n)%9,false);assertFalse(s.sleeping());}
        s.observe(6,false);assertTrue(s.sleeping());
    }
    @Test void disablingIsLiveClearsSleepAndNeverCollectsEmptyRounds() {
        var s=fresh(3);emptyRound(s,9,0);assertTrue(s.sleeping());
        assertTrue(s.configure(3,0,1,false));assertFalse(s.sleeping());
        for(int n=0;n<100;n++)s.observe(n%9,false);
        assertFalse(s.sleeping());assertEquals(0,s.checked());
        assertFalse(s.configure(3,0,1,false)); // must not restart normal cooldown every tick
        assertTrue(s.configure(3,0,1,true));emptyRound(s,9,0);assertTrue(s.sleeping());
    }
    @Test void outOfRangeQueryIsNotCountedAsAWorkingCell() {
        var s=fresh(3);s.observe(0,false);s.observe(-1,false);assertEquals(0,s.checked());
        s.observe(9,false);assertFalse(s.sleeping());assertEquals(0,s.checked());
    }
    @Test void syncedClientSleepFlagDoesNotRunOrInventScans() {
        var s=new FarmerScanState();s.syncedSleeping(true);assertTrue(s.sleeping());assertEquals(0,s.checked());
        s.syncedSleeping(false);assertFalse(s.sleeping());
    }
    @Test void nativeCounterArmingProducesExactlyOneSleepingCellEveryTwentyTicks() {
        var s=fresh(1);s.observe(0,false);int remaining=0,scans=0;
        for(int tick=1;tick<=60;tick++){
            remaining=FarmerUpgradeMath.armCounter(remaining,s.sleeping()?FarmerScanState.SLOW_TICKS:1);
            if(--remaining==0){scans++;s.observe(0,false);assertEquals(0,tick%20);}
        }
        assertEquals(3,scans);
        s.observe(0,true);assertFalse(s.sleeping());
        assertEquals(1,FarmerUpgradeMath.armCounter(remaining,1));
    }
    @Test void mixedPlantingRejectedAThenProcessableBDoesNotSleep() {
        var s=fresh(3);s.observe(4,true,true,false);assertFalse(s.sleeping());assertEquals(1,s.checked());
        s.observe(5,true,false,true);assertFalse(s.sleeping());
        for(int n=2;n<9;n++){s.observe((4+n)%9,true,true,false);assertFalse(s.sleeping());}
        assertEquals(0,s.checked());
    }
    @Test void allRejectedSleepsOnlyAfterLastCellOfTheCompleteCenterOffsetRound() {
        var s=fresh(3);
        for(int n=0;n<8;n++){s.observe((4+n)%9,true,true,false);assertFalse(s.sleeping());}
        s.observe(3,true,true,false);assertTrue(s.sleeping());
    }
    @Test void earlySuccessCannotBeOverriddenByLateRejectedTargets() {
        var s=fresh(3);s.observe(0,true,false,true);
        for(int n=1;n<9;n++){s.observe(n,true,true,false);assertFalse(s.sleeping());}
    }
    @Test void matureButStillBlockedCannotWakeEvenAtAnotherRoundEnd() {
        var s=fresh(3);for(int n=0;n<9;n++)s.observe(n,true,true,false);
        assertTrue(s.sleeping());
        for(int n=0;n<27;n++){s.observe(n%9,true,true,false);assertTrue(s.sleeping());}
    }
    @Test void actualFitWakesAndStartsAnEntireNewNormalRoundAfterTheWakeCell() {
        var s=fresh(3);emptyRound(s,9,0);assertTrue(s.sleeping());
        s.observe(0,true,true,false);assertTrue(s.sleeping());
        s.observe(1,true,false,true);assertFalse(s.sleeping());assertEquals(0,s.checked());
        for(int n=0;n<8;n++){s.observe((2+n)%9,true,true,false);assertFalse(s.sleeping());}
        s.observe(1,true,true,false);assertTrue(s.sleeping());
    }
    @Test void noEnergyForHarvestDoesNotMeanStorageBlockedOrSufficientCapacity() {
        var s=fresh(3);for(int n=0;n<9;n++)s.observe(n,true,false,false);
        assertFalse(s.sleeping()); // positive but insufficient FE: preserve normal speed
        for(int n=0;n<9;n++)s.observe(n,true,true,false);
        assertTrue(s.sleeping());
        for(int n=0;n<18;n++){s.observe(n%9,true,false,false);assertTrue(s.sleeping());}
        s.observe(0,true,false,true);assertFalse(s.sleeping());
    }
    @Test void disabledSwitchPreventsStorageSlowdownAsWellAsEmptyRoundSleep() {
        var s=fresh(3);s.configure(3,0,1,false);
        for(int n=0;n<18;n++){s.observe(n%9,true,true,false);assertFalse(s.sleeping());}
    }
    @Test void partiallyProcessableTallCropCountsEvenIfAnotherSegmentIsRejected() {
        var s=fresh(3);s.observe(0,true,true,true);
        for(int n=1;n<9;n++)s.observe(n,true,true,false);
        assertFalse(s.sleeping());
    }
    @Test void occupiedButNotFullStacksUseTheRealFitResultNotSlotOccupancy() {
        var s=fresh(3);for(int n=0;n<9;n++)s.observe(n,true,false,true);
        assertFalse(s.sleeping()); // result=true also when every inventory slot is nonempty
    }
    @Test void unknownExtensionNeverClaimsACompleteUnprocessableRound() {
        var s=fresh(3);for(int n=0;n<18;n++){s.observe(n%9,true,true,false,true);assertFalse(s.sleeping());}
        emptyRound(s,9,0);assertTrue(s.sleeping());
        s.observe(0,false,false,false,true);assertFalse(s.sleeping());assertEquals(0,s.checked());
    }
    @Test void successfulPlantingWakesAndRestartsAFullNormalRound() {
        var s=fresh(3);emptyRound(s,9,4);assertTrue(s.sleeping());
        s.planted(4);assertFalse(s.sleeping());assertEquals(0,s.checked());
        for(int n=0;n<9;n++){s.observe((5+n)%9,false);assertFalse(s.sleeping());}
        emptyRound(s,9,5);assertTrue(s.sleeping());
    }
    @Test void plantingAtFormerLastCellCannotImmediatelyResleep() {
        var s=fresh(3);for(int n=0;n<8;n++)s.observe((4+n)%9,false);
        s.planted(3);assertEquals(0,s.checked());assertFalse(s.sleeping());
        for(int n=0;n<9;n++){s.observe((4+n)%9,true,true,false);assertFalse(s.sleeping());}
        for(int n=0;n<9;n++)s.observe((4+n)%9,true,true,false);
        assertTrue(s.sleeping());
    }
    @Test void repeatedSuccessfulPlantingMaintainsNormalSpeedWithoutHarvest() {
        var s=fresh(3);
        for(int n=0;n<100;n++) {
            if(n%5==0)s.planted(n%9);else s.observe(n%9,false);
            assertFalse(s.sleeping());
        }
    }
    @Test void plantingDoesNotEnableAccountingWhenSmartSleepIsDisabled() {
        var s=fresh(3);s.configure(3,0,1,false);
        for(int n=0;n<100;n++){s.planted(n%9);s.observe(n%9,false);}
        assertFalse(s.sleeping());assertEquals(0,s.checked());
    }
}
