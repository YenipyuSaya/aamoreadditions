package com.aamoreadditions.effect;

import com.aamoreadditions.upgrade.UpgradeLogic;
import java.util.List;
import java.util.function.IntToDoubleFunction;

/** Cross-specific coefficients, shared activation and boundary arithmetic. No processor configuration. */
public final class CrossUpgradeMath {
    private CrossUpgradeMath() {}
    public static double defaultSpeed(int tier) {
        return switch (tier) {
            case 1 -> 1.5;
            case 2 -> 2.0;
            case 3 -> 3.0;
            default -> throw new IllegalArgumentException("Cross speed tier");
        };
    }
    public static double speed(List<Integer> installedTiers, boolean dependency, IntToDoubleFunction coefficient) {
        var active = UpgradeLogic.active(installedTiers, dependency, tier -> "SPEED", Integer::intValue);
        var values = active.stream().map(tier -> {
            double value = coefficient.applyAsDouble(tier);
            if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("Cross speed coefficient");
            return value;
        }).toList();
        return dependency ? EnergyCombine.combine(values) : UpgradeAggregation.speed(values);
    }
    /** Match AA's per-stand integer division before combining four stands. */
    public static long totalEnergy(int energyPerStand, int baseTicks) {
        int base = Math.max(1, baseTicks);
        return 4L * (energyPerStand / base) * base;
    }
    /** Fixed-plan tick cost. Split cumulative FE without overflowing total*progress. */
    public static long tickEnergy(long total, int ticks, int progress) {
        int p = Math.min(progress, ticks - 1);
        return cumulative(total, p + 1, ticks) - cumulative(total, p, ticks);
    }
    private static long cumulative(long total, int progress, int ticks) {
        return (total / ticks) * progress + (total % ticks) * progress / ticks;
    }
}
