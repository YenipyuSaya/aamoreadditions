package com.aamoreadditions.effect;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Confirmed arithmetic only, not a runtime Farmer or Fortune loot integration test. */
class FarmerUpgradeMathTest {
    private static final double EPS=1e-10;
    @Test void absentLinesKeepBaseEffectsAndSixTickPeriod() {
        assertEquals(1,FarmerUpgradeMath.scanSpeed(List.of()),EPS);
        assertEquals(1,FarmerUpgradeMath.operationEnergy(List.of(),List.of()),EPS);
        assertEquals(1,FarmerUpgradeMath.capacity(List.of()),EPS);
        assertEquals(6,FarmerUpgradeMath.idealScanTicks(6,List.of()),EPS);
        assertEquals(0,FarmerUpgradeMath.fortuneBonus(0,0),EPS);
        assertEquals(0,FarmerUpgradeMath.fortuneLevel(0,0));
    }
    @Test void fiveSpeedIIsHaveUnpenalizedSpeedButPenalizedOperationCost() {
        assertEquals(15,FarmerUpgradeMath.scanSpeed(Collections.nCopies(5,3.0)),EPS);
        assertEquals(12,FarmerUpgradeMath.operationEnergy(Collections.nCopies(5,2.0),List.of()),EPS);
        assertEquals(1,FarmerUpgradeMath.idealScanTicks(6,Collections.nCopies(5,3.0)),EPS);
    }
    @Test void mixedGradesCountTogetherOnlyForTheEnergyPenalty() {
        assertEquals(12,FarmerUpgradeMath.scanSpeed(List.of(2.0,3.0,5.0,2.0)),EPS);
        assertEquals(7.5*1.1,FarmerUpgradeMath.operationEnergy(List.of(1.5,2.0,2.5,1.5),List.of()),EPS);
        assertEquals(0.8*0.5*0.8*0.5*1.15,FarmerUpgradeMath.operationEnergy(List.of(),List.of(0.8,0.5,0.8,0.5)),EPS);
    }
    @Test void processorStyleMixedEnergyFactorsMultiplyWithoutExtraScanMultiplier() {
        assertEquals(3,FarmerUpgradeMath.scanSpeed(List.of(3.0)),EPS);
        assertEquals(1,FarmerUpgradeMath.operationEnergy(List.of(2.0),List.of(0.5)),EPS);
        assertEquals(2,FarmerUpgradeMath.idealScanTicks(6,List.of(3.0)),EPS);
    }
    @Test void fiveEfficiencyIIsUseCorrect015PenaltyAndUnpenalizedCapacity() {
        assertEquals(0.040625,FarmerUpgradeMath.operationEnergy(List.of(),Collections.nCopies(5,0.5)),EPS);
        assertEquals(10,FarmerUpgradeMath.capacity(Collections.nCopies(5,2.0)),EPS);
        assertEquals(6,FarmerUpgradeMath.idealScanTicks(6,List.of()),EPS);
    }
    @Test void layersCapAtThreeWhileSpeedAndCapacityContinueToSum() {
        for(int n=6;n<=10;n++) {
            assertEquals(n*3,FarmerUpgradeMath.scanSpeed(Collections.nCopies(n,3.0)),EPS);
            assertEquals(n*2*1.3,FarmerUpgradeMath.operationEnergy(Collections.nCopies(n,2.0),List.of()),EPS);
            assertEquals(Math.pow(0.5,n)*1.45,FarmerUpgradeMath.operationEnergy(List.of(),Collections.nCopies(n,0.5)),EPS);
            assertEquals(n*2,FarmerUpgradeMath.capacity(Collections.nCopies(n,2.0)),EPS);
        }
    }
    @Test void infoCoreCannotInventAnyFarmerSpeedPenalty() {
        for(int n=0;n<=10;n++) {
            var f=UpgradeAggregation.boundaryFactors(UpgradeAggregation.Kind.FARMER,n,n,false);
            int l=UpgradeAggregation.boundaryLayers(n);
            assertEquals(1,f.speed(),EPS);assertEquals(1,f.utilization(),EPS);
            assertEquals((1+0.1*l)*(1+0.15*l),f.energy(),EPS);
            assertEquals(new UpgradeAggregation.BoundaryFactors(1,1,1),UpgradeAggregation.boundaryFactors(UpgradeAggregation.Kind.FARMER,n,n,true));
        }
    }
    @Test void idealAndRuntimeTimingUseTheConfirmedFloorRule() {
        assertEquals(1.2,FarmerUpgradeMath.idealScanTicks(6,List.of(5.0)),EPS);
        assertEquals(1,FarmerUpgradeMath.scanTicks(6,5));
        assertEquals(3,FarmerUpgradeMath.scanTicks(6,2));
        assertEquals(2,FarmerUpgradeMath.scanTicks(6,3));
        assertEquals(1,FarmerUpgradeMath.idealScanTicks(6,List.of(1000.0)),EPS);
        assertThrows(IllegalArgumentException.class,()->FarmerUpgradeMath.idealScanTicks(6,List.of(0.0)));
    }
    @Test void fortuneThresholdExamples() {
        assertEquals(0,FarmerUpgradeMath.fortuneLevel(1,0)); // .7
        assertEquals(1,FarmerUpgradeMath.fortuneLevel(0,1)); // 1.2
        assertEquals(1,FarmerUpgradeMath.fortuneLevel(2,0)); // 1.4
        assertEquals(2,FarmerUpgradeMath.fortuneLevel(3,0)); // 2.1
        assertEquals(2,FarmerUpgradeMath.fortuneLevel(4,0)); // 2.8
        assertEquals(3,FarmerUpgradeMath.fortuneLevel(1,2)); // 3.1
        assertEquals(3,FarmerUpgradeMath.fortuneLevel(0,4)); // 4.8
        assertEquals(4,FarmerUpgradeMath.fortuneLevel(2,3)); // exactly 5
        assertEquals(4,FarmerUpgradeMath.fortuneLevel(0,5)); // 6
    }
    @Test void allLegalSlotCountCombinationsUseExactTenthsIncludingBoundaryFive() {
        for(int a=0;a<=10;a++) for(int b=0;a+b<=10;b++) {
            int tenths=a*7+b*12;
            assertEquals(tenths,FarmerUpgradeMath.fortuneTenths(a,b));
            assertEquals(tenths/10.0,FarmerUpgradeMath.fortuneBonus(a,b),EPS);
            assertEquals(tenths<10?0:tenths<20?1:tenths<30?2:tenths<50?3:4,FarmerUpgradeMath.fortuneLevel(a,b));
        }
        assertEquals(50,FarmerUpgradeMath.fortuneTenths(2,3));
        assertEquals(4,FarmerUpgradeMath.fortuneLevel(2,3));
        assertThrows(IllegalArgumentException.class,()->FarmerUpgradeMath.fortuneBonus(-1,0));
    }
    @Test void doubledRollIsSplitBeforeTheOriginalAllOrNothingCapacityCheck() {
        assertEquals(List.of(2),FarmerUpgradeMath.doubledStackCounts(1,64));
        assertEquals(List.of(64,64),FarmerUpgradeMath.doubledStackCounts(64,64));
        assertEquals(List.of(16,16,8),FarmerUpgradeMath.doubledStackCounts(20,16));
        assertTrue(FarmerUpgradeMath.doubledStackCounts(0,64).isEmpty());
        assertThrows(IllegalArgumentException.class,()->FarmerUpgradeMath.doubledStackCounts(1,0));
    }
    @Test void harvestAdapterUsesActualFortuneToolAndNeverLuckOrSecondRollComparison() throws Exception {
        var text=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/aamoreadditions/effect/FarmerHarvestLoot.java"));
        assertTrue(text.contains("new ItemStack(Items.DIAMOND_HOE)"));
        assertTrue(text.contains(".getOrThrow(Enchantments.FORTUNE), fortuneLevel)"));
        assertTrue(text.contains("context.withParameter(LootContextParams.TOOL, tool)"));
        assertFalse(text.contains("withLuck"));assertFalse(text.contains("RandomSource"));
        assertTrue(text.contains("fortuneLevel < 4 || !fortuneUnaffected || state.is(Blocks.MELON)"));
        assertTrue(text.contains("drop.copyWithCount(count)"));
        assertTrue(text.contains("state.is(Blocks.SUGAR_CANE) || state.is(Blocks.PUMPKIN) || state.is(Blocks.CACTUS)"));
    }

    static com.aamoreadditions.upgrade.UpgradeLogic.Plugin speed(int tier) {
        return new com.aamoreadditions.upgrade.UpgradeLogic.Plugin("SPEED",tier);
    }
    static com.aamoreadditions.upgrade.UpgradeLogic.Plugin efficiency(int tier) {
        return new com.aamoreadditions.upgrade.UpgradeLogic.Plugin("EFFICIENCY",tier);
    }
    @Test void fiveSpeedOneExampleComesFromTheActualRuntimePolicyAndSharedDefaults() {
        var e=FarmerUpgradeMath.evaluate(Collections.nCopies(5,speed(1)),false);
        assertEquals(10,e.speed(),EPS);assertEquals(1,e.cooldown());
        assertEquals(9,e.energy(),EPS);assertEquals(1,e.capacity(),EPS);assertEquals(0,e.fortune());
        assertEquals(2250,FarmerUpgradeMath.operationCost(250,e.energy()));
        assertEquals(3150,FarmerUpgradeMath.operationCost(350,e.energy()));
        assertEquals(4500,FarmerUpgradeMath.operationCost(500,e.energy()));
        assertEquals(6300,FarmerUpgradeMath.plantingReserve(350,e.energy()));
        assertEquals(100000,UpgradeMath.capacity(100000,e.capacity()));
    }
    @Test void fiveEfficiencyOneExampleComesFromTheActualRuntimePolicyAndSharedDefaults() {
        var e=FarmerUpgradeMath.evaluate(Collections.nCopies(5,efficiency(1)),false);
        assertEquals(1,e.speed(),EPS);assertEquals(6,e.cooldown());
        assertEquals(0.425984,e.energy(),EPS);assertEquals(7.5,e.capacity(),EPS);assertEquals(3,e.fortune());
        assertEquals(106,FarmerUpgradeMath.operationCost(250,e.energy()));
        assertEquals(149,FarmerUpgradeMath.operationCost(350,e.energy()));
        assertEquals(213,FarmerUpgradeMath.operationCost(500,e.energy()));
        assertEquals(298,FarmerUpgradeMath.plantingReserve(350,e.energy()));
        assertEquals(750000,UpgradeMath.capacity(100000,e.capacity()));
        assertEquals(3.5,FarmerUpgradeMath.fortuneBonus(5,0),EPS);
    }
    @Test void trueModeUsesSharedHighestGradesAndFixedFortuneOneOrThree() {
        var e1=FarmerUpgradeMath.evaluate(List.of(efficiency(1)),true);
        assertEquals(1,e1.fortune());assertEquals(0.8,e1.energy(),EPS);assertEquals(1.5,e1.capacity(),EPS);
        var both=FarmerUpgradeMath.evaluate(List.of(efficiency(1),efficiency(2)),true);
        assertEquals(3,both.fortune());assertEquals(0.5,both.energy(),EPS);assertEquals(2,both.capacity(),EPS);
        var mixed=FarmerUpgradeMath.evaluate(List.of(speed(1),speed(2),speed(3),efficiency(1),efficiency(2)),true);
        assertEquals(5,mixed.speed(),EPS);assertEquals(1,mixed.cooldown());
        assertEquals(1.25,mixed.energy(),EPS);assertEquals(3,mixed.fortune());
    }
    @Test void independentMixedLinesUseConfirmedMultiplicationAndThreshold() {
        var e=FarmerUpgradeMath.evaluate(List.of(speed(2),efficiency(2)),false);
        assertEquals(2,e.cooldown());assertEquals(1,e.energy(),EPS);assertEquals(1,e.fortune());
        var exactFive=FarmerUpgradeMath.evaluate(List.of(efficiency(1),efficiency(1),efficiency(2),efficiency(2),efficiency(2)),false);
        assertEquals(4,exactFive.fortune());
    }
    @Test void feesRoundHalfUpWithMinimumOneAndTwoIndividuallyRoundedReservations() {
        assertEquals(1,FarmerUpgradeMath.operationCost(250,0.00001));
        assertEquals(3,FarmerUpgradeMath.operationCost(250,0.01)); // 2.5 -> 3, not ceil as an implicit rule
        assertEquals(2,FarmerUpgradeMath.operationCost(250,0.009)); // 2.25 -> 2
        assertEquals(4,FarmerUpgradeMath.plantingReserve(250,0.009));
        assertEquals(175,FarmerUpgradeMath.operationCost(350,0.5));
        assertEquals(350,FarmerUpgradeMath.plantingReserve(350,0.5));
        assertEquals(700,FarmerUpgradeMath.plantingReserve(350,1));
    }
    @Test void actualCounterSimulationPreservesBaselineAndReallyScansEveryTickAtMinimum() {
        for(int period:new int[]{1,2,3,6}) {
            int wait=0,last=-1,checks=0;
            for(int tick=0;tick<60;tick++) {
                wait=FarmerUpgradeMath.armCounter(wait,period);
                wait--; // original AA branch
                if(wait<=0) {
                    if(last>=0)assertEquals(period,tick-last);
                    else assertEquals(period-1,tick); // baseline first scan tick index5
                    last=tick;checks++;
                }
            }
            assertEquals(60/period,checks);
        }
        assertEquals(1,FarmerUpgradeMath.armCounter(5,1)); // hot speed change clamps remaining wait
    }
    @Test void allFalseModeMixedGradeCountsAgreeWithTheSharedCoreAndOnlyCountTheirOwnLine() {
        for(int s1=0;s1<=4;s1++)for(int s2=0;s2<=3;s2++)for(int e1=0;e1<=3;e1++)for(int e2=0;e2<=3;e2++) {
            if(s1+s2+e1+e2>10)continue;
            var plugins=new ArrayList<com.aamoreadditions.upgrade.UpgradeLogic.Plugin>();
            plugins.addAll(Collections.nCopies(s1,speed(1)));plugins.addAll(Collections.nCopies(s2,speed(2)));
            plugins.addAll(Collections.nCopies(e1,efficiency(1)));plugins.addAll(Collections.nCopies(e2,efficiency(2)));
            var e=FarmerUpgradeMath.evaluate(plugins,false);
            double speed=s1+s2==0?1:s1*2+s2*3;
            assertEquals(speed,e.speed(),EPS);assertEquals(Math.max(1,(int)Math.floor(6/speed)),e.cooldown());
            double se=(s1+s2==0?1:s1*1.5+s2*2)*(1+0.1*UpgradeAggregation.boundaryLayers(s1+s2));
            double ee=Math.pow(0.8,e1)*Math.pow(0.5,e2)*(1+0.15*UpgradeAggregation.boundaryLayers(e1+e2));
            assertEquals(se*ee,e.energy(),EPS);
            assertEquals(e1+e2==0?1:e1*1.5+e2*2,e.capacity(),EPS);
            assertEquals(FarmerUpgradeMath.fortuneLevel(e1,e2),e.fortune());
        }
    }

    @Test void storedEnergySurvivesReloadWithSameUpgradesButClampsRealCapacityLoss() {
        var e=FarmerUpgradeMath.evaluate(Collections.nCopies(5,efficiency(1)),false);
        int restoredCapacity=UpgradeMath.capacity(100000,e.capacity());
        assertEquals(600000,UpgradeMath.clampEnergy(600000,restoredCapacity));
        assertEquals(100000,UpgradeMath.clampEnergy(600000,100000));
    }

    @Test void operationRoundingMatchesExactDecimalDefaultsForEveryTenSlotCombination() {
        var one=java.math.BigDecimal.ONE;
        for(int a=0;a<=10;a++)for(int b=0;a+b<=10;b++)for(int c=0;a+b+c<=10;c++)
            for(int d=0;a+b+c+d<=10;d++)for(int e=0;a+b+c+d+e<=10;e++) {
                var plugins=new ArrayList<com.aamoreadditions.upgrade.UpgradeLogic.Plugin>();
                plugins.addAll(Collections.nCopies(a,speed(1)));plugins.addAll(Collections.nCopies(b,speed(2)));
                plugins.addAll(Collections.nCopies(c,speed(3)));plugins.addAll(Collections.nCopies(d,efficiency(1)));
                plugins.addAll(Collections.nCopies(e,efficiency(2)));
                var effects=FarmerUpgradeMath.evaluate(plugins,false);
                var speedCost=a+b+c==0?one:java.math.BigDecimal.valueOf(a*1.5+b*2+c*2.5);
                speedCost=speedCost.multiply(one.add(new java.math.BigDecimal("0.1").multiply(java.math.BigDecimal.valueOf(UpgradeAggregation.boundaryLayers(a+b+c)))));
                var exact=speedCost.multiply(new java.math.BigDecimal("0.8").pow(d)).multiply(new java.math.BigDecimal("0.5").pow(e))
                        .multiply(one.add(new java.math.BigDecimal("0.15").multiply(java.math.BigDecimal.valueOf(UpgradeAggregation.boundaryLayers(d+e)))));
                for(int base:new int[]{250,350,500}) {
                    int expected=exact.multiply(java.math.BigDecimal.valueOf(base)).setScale(0,java.math.RoundingMode.HALF_UP).max(one).intValueExact();
                    assertEquals(expected,FarmerUpgradeMath.operationCost(base,effects.energy()),
                            "counts="+List.of(a,b,c,d,e)+", base="+base+", exact="+exact);
                }
            }
    }

}
