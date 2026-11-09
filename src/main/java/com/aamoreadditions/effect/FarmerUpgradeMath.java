package com.aamoreadditions.effect;

import java.util.List;

/** Farmer policy built from the same activation, processor defaults and aggregation
 * core used by the other machines. Runtime, info and numerical examples share it. */
public final class FarmerUpgradeMath {
    private FarmerUpgradeMath() {}

    public record Effects(double speed, int cooldown, double energy, double capacity, int fortune) {}

    /** Activation always uses the shared mode policy, not a separate Farmer prerequisite rule. */
    public static Effects evaluate(java.util.List<com.aamoreadditions.upgrade.UpgradeLogic.Plugin> installed, boolean dependency) {
        var active = com.aamoreadditions.upgrade.UpgradeLogic.active(installed, dependency,
                com.aamoreadditions.upgrade.UpgradeLogic.Plugin::line,
                com.aamoreadditions.upgrade.UpgradeLogic.Plugin::tier);
        var speeds = new java.util.ArrayList<Double>();
        var speedEnergy = new java.util.ArrayList<Double>();
        var efficiencyEnergy = new java.util.ArrayList<Double>();
        var capacities = new java.util.ArrayList<Double>();
        int e1=0, e2=0, highestEfficiency=0;
        for (var plugin : active) {
            if (plugin.line().equals("SPEED")) {
                var c=ProcessorUpgradeDefaults.speed(plugin.tier());
                speeds.add(c.speed()); speedEnergy.add(c.energy());
            } else if (plugin.line().equals("EFFICIENCY")) {
                var c=ProcessorUpgradeDefaults.efficiency(plugin.tier());
                efficiencyEnergy.add(c.energy()); capacities.add(c.capacity());
                if(plugin.tier()==1)e1++; else e2++;
                highestEfficiency=Math.max(highestEfficiency,plugin.tier());
            }
        }
        double energy = dependency ? EnergyCombine.combine(java.util.stream.Stream.concat(
                speedEnergy.stream(),efficiencyEnergy.stream()).toList()) : operationEnergy(speedEnergy,efficiencyEnergy);
        int fortune = dependency ? (highestEfficiency==2?3:highestEfficiency) : fortuneLevel(e1,e2);
        double speed=scanSpeed(speeds);
        return new Effects(speed, scanTicks(6,speed),energy,capacity(capacities),fortune);
    }

    /** Actual scan-to-scan period. AA's old waitTime=5 represented six ticks. */
    public static int scanTicks(int originalPeriod, double speed) {
        if(originalPeriod<1 || !Double.isFinite(speed) || speed<=0)throw new IllegalArgumentException("Invalid Farmer scan timing");
        return (int)Math.max(1,Math.min(Integer.MAX_VALUE,Math.floor(originalPeriod/speed)));
    }
    /** Arm the existing AA decrement branch without adding a reset-only idle tick. */
    public static int armCounter(int remaining,int period) {
        if(period<1)throw new IllegalArgumentException("Invalid Farmer period");
        return remaining<=0?period:Math.min(remaining,period);
    }
    public static int operationCost(int base, double multiplier) {
        if(base<0 || !Double.isFinite(multiplier) || multiplier<=0)throw new IllegalArgumentException("Invalid Farmer operation cost");
        // All combinations of the fixed decimal defaults in <=10 slots have at most
        // 12 decimal places. Remove IEEE-754 accumulation noise before HALF_UP:
        // e.g. 1x efficiency I + 3x II is exactly .115, so 500 FE must cost 58, not 57.
        var decimal=java.math.BigDecimal.valueOf(multiplier).setScale(12,java.math.RoundingMode.HALF_UP);
        return decimal.multiply(java.math.BigDecimal.valueOf(base)).setScale(0,java.math.RoundingMode.HALF_UP)
                .max(java.math.BigDecimal.ONE).min(java.math.BigDecimal.valueOf(Integer.MAX_VALUE)).intValueExact();
    }
    /** Preserve the original two-operation planting reservation after rounding each charge. */
    public static int plantingReserve(int base, double multiplier) {
        return (int)Math.min(Integer.MAX_VALUE,2L*operationCost(base,multiplier));
    }

    /** Unlike processors, Farmer scan speed itself has no boundary penalty. */
    public static double scanSpeed(List<Double> speedCoefficients) {
        return UpgradeAggregation.unpenalizedSum(speedCoefficients);
    }

    /** Per harvest / planting / tilling operation; not an additional FE/t speed product. */
    public static double operationEnergy(List<Double> speedEnergy, List<Double> efficiencyEnergy) {
        return UpgradeAggregation.processorEnergy(speedEnergy, efficiencyEnergy);
    }

    public static double capacity(List<Double> efficiencyCapacity) {
        return UpgradeAggregation.unpenalizedSum(efficiencyCapacity);
    }

    /** Unrounded value retained for tests; runtime uses scanTicks and its confirmed floor rule. */
    public static double idealScanTicks(int originalPeriod, List<Double> speedCoefficients) {
        double speed = scanSpeed(speedCoefficients);
        if (originalPeriod < 1 || !Double.isFinite(speed) || speed <= 0)
            throw new IllegalArgumentException("Invalid Farmer scan timing");
        return Math.max(1, originalPeriod / speed);
    }

    /** Split a doubled, already-rolled drop into legal stack sizes before capacity checks.
     * This is not a second random roll and makes no decision about crop classification. */
    public static List<Integer> doubledStackCounts(int count, int maxStackSize) {
        if (count < 0 || maxStackSize < 1) throw new IllegalArgumentException("Invalid drop stack");
        int remaining = Math.multiplyExact(count, 2);
        var result = new java.util.ArrayList<Integer>();
        while (remaining > 0) {
            int part = Math.min(remaining, maxStackSize);
            result.add(part);
            remaining -= part;
        }
        return List.copyOf(result);
    }

    /** Independent-mode counts. Integer tenths make exact thresholds immune to
     * accumulation errors (e.g. 2*0.7+3*1.2=5). Dependency mode uses fixed levels. */
    public static int fortuneTenths(int efficiencyI, int efficiencyII) {
        if (efficiencyI < 0 || efficiencyII < 0) throw new IllegalArgumentException("Negative plugin count");
        return Math.addExact(Math.multiplyExact(7, efficiencyI), Math.multiplyExact(12, efficiencyII));
    }
    public static double fortuneBonus(int efficiencyI, int efficiencyII) {
        return fortuneTenths(efficiencyI, efficiencyII) / 10.0;
    }
    public static int fortuneLevel(int efficiencyI, int efficiencyII) {
        int value = fortuneTenths(efficiencyI, efficiencyII);
        return value < 10 ? 0 : value < 20 ? 1 : value < 30 ? 2 : value < 50 ? 3 : 4;
    }
}
