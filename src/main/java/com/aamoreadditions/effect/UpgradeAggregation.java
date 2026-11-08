package com.aamoreadditions.effect;

import java.util.List;

/** Confirmed independent-mode arithmetic, separate from Minecraft and slot/UI state.
 * Callers pass only the relevant upgrade line: neutral fields from other lines must not
 * be included. Empty lines retain the machine's base multiplier (one).
 */
public final class UpgradeAggregation {
    private UpgradeAggregation() {}

    public static int boundaryLayers(int count) {
        if (count < 0) throw new IllegalArgumentException("Negative upgrade count");
        return Math.min(3, Math.max(0, count - 3));
    }

    public static double sumOrBase(List<Double> coefficients) {
        if (coefficients.isEmpty()) return 1.0;
        double sum = 0;
        for (double coefficient : coefficients) sum += coefficient;
        return sum;
    }

    private static double product(List<Double> coefficients) {
        double product = 1;
        for (double coefficient : coefficients) product *= coefficient;
        return product;
    }

    /** Processors, coal/oil/heat generators and the Casing Cross use the same speed law. */
    public static double speed(List<Double> coefficients) {
        return sumOrBase(coefficients) * speedBoundary(coefficients.size());
    }

    /** Speed line's total energy per processing operation, not FE/t. */
    public static double processorSpeedEnergy(List<Double> coefficients) {
        return sumOrBase(coefficients) * processorSpeedEnergyBoundary(coefficients.size());
    }

    /** Efficiency line's total processing energy; processorEnergy applies the agreed mixed policy. */
    public static double processorEfficiencyEnergy(List<Double> coefficients) {
        return product(coefficients) * processorEfficiencyEnergyBoundary(coefficients.size());
    }

    /** Confirmed mixed processor policy: the two operation-energy factors multiply. */
    public static double processorEnergy(List<Double> speed, List<Double> efficiency) {
        return processorSpeedEnergy(speed) * processorEfficiencyEnergy(efficiency);
    }

    public static double generatorSpeedUtilization(List<Double> coefficients) {
        return product(coefficients) * generatorSpeedUtilizationBoundary(coefficients.size());
    }

    public static double generatorEfficiencyUtilization(List<Double> coefficients) {
        return product(coefficients) * generatorEfficiencyUtilizationBoundary(coefficients.size());
    }

    public static double generatorUtilization(List<Double> speed, List<Double> efficiency) {
        return generatorSpeedUtilization(speed) * generatorEfficiencyUtilization(efficiency);
    }

    /** Capacity and leaf energy-per-leaf have no boundary penalty. */
    public static double unpenalizedSum(List<Double> coefficients) {
        return sumOrBase(coefficients);
    }

    public enum Kind { PROCESSOR, FUEL_GENERATOR, HEAT, LEAF, CROSS, ENERGIZER }

    // The machine formulas and info UI use the very same boundary factors.
    private static double speedBoundary(int count) { return 1 - 0.1 * boundaryLayers(count); }
    private static double processorSpeedEnergyBoundary(int count) { return 1 + 0.1 * boundaryLayers(count); }
    private static double processorEfficiencyEnergyBoundary(int count) { return 1 + 0.5 * boundaryLayers(count); }
    private static double generatorSpeedUtilizationBoundary(int count) { return 1 - 0.05 * boundaryLayers(count); }
    private static double generatorEfficiencyUtilizationBoundary(int count) { return 1 - 0.1 * boundaryLayers(count); }

    /** Final boundary coefficients only, never the plugin's base effect. */
    public record BoundaryFactors(double speed, double energy, double utilization) {}
    public static BoundaryFactors boundaryFactors(Kind kind, int speedCount, int efficiencyCount, boolean dependency) {
        if (dependency) return new BoundaryFactors(1,1,1);
        return switch(kind) {
            case PROCESSOR -> new BoundaryFactors(speedBoundary(speedCount),processorSpeedEnergyBoundary(speedCount)*processorEfficiencyEnergyBoundary(efficiencyCount),1);
            case FUEL_GENERATOR -> new BoundaryFactors(speedBoundary(speedCount),1,generatorSpeedUtilizationBoundary(speedCount)*generatorEfficiencyUtilizationBoundary(efficiencyCount));
            case HEAT, CROSS -> new BoundaryFactors(speedBoundary(speedCount),1,1);
            case LEAF, ENERGIZER -> new BoundaryFactors(1,1,1);
        };
    }

    /** Leaf speed tiers do not matter in independent mode, only installed quantity. */
    public static double leafCooldown(int speedCount) {
        if (speedCount < 0) throw new IllegalArgumentException("Negative upgrade count");
        return switch (speedCount) {
            case 0 -> 1;
            case 1 -> 0.6;
            case 2 -> 0.4;
            default -> 0.2;
        };
    }
}
