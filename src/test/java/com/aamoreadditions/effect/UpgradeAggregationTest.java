package com.aamoreadditions.effect;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UpgradeAggregationTest {
    private static final double EPS = 1e-10;
    private static List<Double> repeat(int count, double value) { return Collections.nCopies(count, value); }

    @Test void noUpgradesPreserveEveryBaseMultiplier() {
        assertEquals(1, UpgradeAggregation.speed(List.of()), EPS);
        assertEquals(1, UpgradeAggregation.processorSpeedEnergy(List.of()), EPS);
        assertEquals(1, UpgradeAggregation.processorEfficiencyEnergy(List.of()), EPS);
        assertEquals(1, UpgradeAggregation.generatorUtilization(List.of(), List.of()), EPS);
        assertEquals(1, UpgradeAggregation.unpenalizedSum(List.of()), EPS);
        assertEquals(1, UpgradeAggregation.leafCooldown(0), EPS);
    }
    @Test void boundaryStartsAtFourthAndCapsAtThree() {
        int[] expected = {0,0,0,0,1,2,3,3,3,3,3};
        for (int count = 0; count <= 10; count++) assertEquals(expected[count], UpgradeAggregation.boundaryLayers(count));
    }
    @Test void fiveSpeedIIHasTwelveTimesSpeedAndOperationEnergyBut144TimesTickEnergy() {
        double speed = UpgradeAggregation.speed(repeat(5, 3));
        double energy = UpgradeAggregation.processorSpeedEnergy(repeat(5, 2));
        assertEquals(12, speed, EPS); assertEquals(12, energy, EPS);
        assertEquals(144, speed * energy, EPS);
        assertEquals(5760, UpgradeMath.energyPerTick(40, energy, speed));
    }
    @Test void mixedSpeedTiersUseSumNotHighestOrProduct() {
        assertEquals(10, UpgradeAggregation.speed(List.of(2.0,3.0,5.0)), EPS);
        assertEquals(6, UpgradeAggregation.processorSpeedEnergy(List.of(1.5,2.0,2.5)), EPS);
    }
    @Test void fiveEfficiencyIIProcessingEnergyUsesProductAndPenalty() {
        assertEquals(0.0625, UpgradeAggregation.processorEfficiencyEnergy(repeat(5, 0.5)), EPS);
    }
    @Test void efficiencyProcessingLossCapsAtThree() {
        assertEquals(Math.pow(0.8,10) * 2.5,
                UpgradeAggregation.processorEfficiencyEnergy(repeat(10, 0.8)), EPS);
    }
    @Test void fiveSpeedICoalExampleDoesNotHardcodeMachineBaseRate() {
        double speed = UpgradeAggregation.speed(repeat(5, 2));
        assertEquals(8, speed, EPS);
        assertEquals(0.531441, UpgradeAggregation.generatorUtilization(repeat(5, 0.9), List.of()), EPS);
        assertEquals(240, UpgradeMath.generatorOutput(30, speed));
        assertEquals(160, UpgradeMath.generatorOutput(20, speed));
    }
    @Test void fiveEfficiencyICoalExample() {
        assertEquals(1, UpgradeAggregation.speed(List.of()), EPS);
        assertEquals(6.075, UpgradeAggregation.generatorUtilization(List.of(), repeat(5, 1.5)), EPS);
    }
    @Test void mixedGeneratorExampleKeepsLineCountsSeparate() {
        assertEquals(5, UpgradeAggregation.speed(List.of(2.0,3.0)), EPS);
        assertEquals(1.62, UpgradeAggregation.generatorUtilization(List.of(0.9,0.8), List.of(1.5,1.5)), EPS);
    }
    @Test void mixedGeneratorLossesApplyOnlyToOwnLine() {
        assertEquals(Math.pow(0.9,4) * 0.95 * Math.pow(1.5,6) * 0.7,
                UpgradeAggregation.generatorUtilization(repeat(4,0.9), repeat(6,1.5)), EPS);
    }
    @Test void capacityUsesFullCoefficientSumWithNoBoundaryLoss() {
        assertEquals(10, UpgradeAggregation.unpenalizedSum(repeat(5,2)), EPS);
        assertEquals(20, UpgradeAggregation.unpenalizedSum(repeat(10,2)), EPS);
        assertEquals(3.5, UpgradeAggregation.unpenalizedSum(List.of(1.5,2.0)), EPS);
    }
    @Test void energizerKeepsOwnCapacityCoefficients() {
        assertEquals(25, UpgradeAggregation.unpenalizedSum(List.of(5.0,20.0)), EPS);
    }
    @Test void leafSpeedOnlyCountsPluginsAndNeverReceivesBoundaryPenalty() {
        double[] expected = {1,0.6,0.4,0.2,0.2,0.2,0.2,0.2,0.2,0.2,0.2};
        for (int count=0; count<=10; count++) assertEquals(expected[count],UpgradeAggregation.leafCooldown(count),EPS);
    }
    @Test void leafEfficiencyEnergyUsesSumAndNoBoundaryPenalty() {
        assertEquals(12,UpgradeAggregation.unpenalizedSum(List.of(2.0,2.0,2.0,3.0,3.0)),EPS);
    }
    @Test void casingCrossRetainsOwnSpeedCoefficientsAndNewLoss() {
        assertEquals(6.5,UpgradeAggregation.speed(List.of(1.5,2.0,3.0)),EPS);
        assertEquals(12,UpgradeAggregation.speed(repeat(5,3)),EPS);
    }
    @Test void tenSpeedPluginsDoNotExceedThreeLossLayers() {
        assertEquals(35,UpgradeAggregation.speed(repeat(10,5)),EPS);
        assertEquals(32.5,UpgradeAggregation.processorSpeedEnergy(repeat(10,2.5)),EPS);
    }
    @Test void mixedProcessorExampleHasTripleSpeedAndTickCostButBaseOperationCost() {
        double speed = UpgradeAggregation.speed(List.of(3.0));
        double energy = UpgradeAggregation.processorEnergy(List.of(2.0), List.of(0.5));
        assertEquals(3, speed, EPS);
        assertEquals(1, energy, EPS);
        assertEquals(120, UpgradeMath.energyPerTick(40, energy, speed));
    }
    @Test void mixedProcessingPenaltiesRemainMultiplicative() {
        assertEquals((4 * 2.0 * 1.1) * (Math.pow(0.5,4) * 1.5),
                UpgradeAggregation.processorEnergy(repeat(4,2),repeat(4,0.5)),EPS);
    }
    @Test void emptyMixedProcessorLinesRemainBaseCost() {
        assertEquals(1,UpgradeAggregation.processorEnergy(List.of(),List.of()),EPS);
        assertEquals(2,UpgradeAggregation.processorEnergy(List.of(2.0),List.of()),EPS);
        assertEquals(0.5,UpgradeAggregation.processorEnergy(List.of(),List.of(0.5)),EPS);
    }
    @Test void leafAndEnergizerNeverAdvertiseImaginaryBoundaryPenalties() {
        for(var kind:List.of(UpgradeAggregation.Kind.LEAF,UpgradeAggregation.Kind.ENERGIZER)) {
            var f=UpgradeAggregation.boundaryFactors(kind,5,5,false);
            assertEquals(1,f.speed(),EPS); assertEquals(1,f.energy(),EPS); assertEquals(1,f.utilization(),EPS);
        }
    }
    @Test void heatAndCrossOnlyExposeTheirActualSpeedPenalty() {
        for(var kind:List.of(UpgradeAggregation.Kind.HEAT,UpgradeAggregation.Kind.CROSS)) {
            var f=UpgradeAggregation.boundaryFactors(kind,4,6,false);
            assertEquals(0.9,f.speed(),EPS); assertEquals(1,f.energy(),EPS); assertEquals(1,f.utilization(),EPS);
        }
    }
    @Test void fuelGeneratorCombinesBothUtilizationPenaltySources() {
        var f=UpgradeAggregation.boundaryFactors(UpgradeAggregation.Kind.FUEL_GENERATOR,4,6,false);
        assertEquals(0.9,f.speed(),EPS); assertEquals(0.95*0.7,f.utilization(),EPS);
    }
    @Test void extremeConfiguredCoefficientsSaturateWithoutIntegerWraparound() {
        assertEquals(Integer.MAX_VALUE,UpgradeMath.generatorOutput(1000000,1000000));
        assertEquals(Integer.MAX_VALUE,UpgradeMath.generatorBurnTime(1000000,0.01,1e20));
        assertEquals(Integer.MAX_VALUE,UpgradeMath.time(1000000,1e-20));
    }
    @Test void displayUsesOnlyFinalBoundaryCoefficientsNotFullUpgradeEffects() {
        var f=UpgradeAggregation.boundaryFactors(UpgradeAggregation.Kind.FUEL_GENERATOR,4,4,false);
        assertEquals(0.9,f.speed(),EPS);assertEquals(0.855,f.utilization(),EPS);assertEquals(1,f.energy(),EPS);
    }
    @Test void processorEnergyBoundaryCombinesBothKindsOfPenalty() {
        var f=UpgradeAggregation.boundaryFactors(UpgradeAggregation.Kind.PROCESSOR,4,4,false);
        assertEquals(0.9,f.speed(),EPS);assertEquals(1.65,f.energy(),EPS);assertEquals(1,f.utilization(),EPS);
    }
    @Test void boundaryCoefficientOneMeansNoDisplayedPenalty() {
        for(var kind:UpgradeAggregation.Kind.values()){
            var f=UpgradeAggregation.boundaryFactors(kind,3,3,false);
            assertEquals(1,f.speed(),EPS);assertEquals(1,f.energy(),EPS);assertEquals(1,f.utilization(),EPS);
        }
    }
    @Test void legacyAlwaysHasNoBoundaryFactors() {
        for(var kind:UpgradeAggregation.Kind.values()){
            var f=UpgradeAggregation.boundaryFactors(kind,5,5,true);
            assertEquals(1,f.speed(),EPS);assertEquals(1,f.energy(),EPS);assertEquals(1,f.utilization(),EPS);
        }
    }
    @Test void negativeCountsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->UpgradeAggregation.boundaryLayers(-1));
        assertThrows(IllegalArgumentException.class,()->UpgradeAggregation.leafCooldown(-1));
    }
}
