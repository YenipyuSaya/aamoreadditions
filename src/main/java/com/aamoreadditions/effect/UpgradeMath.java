package com.aamoreadditions.effect;

/**
 * The single place where upgrade results are turned into integers (no Minecraft classes, unit-tested):
 * time = max(1, round(orig / speed)); per-tick energy = max(1, ceil(base * energyMult * speed)).
 */
public final class UpgradeMath {
    private UpgradeMath() {}

    /** Processing time: original / speed, rounded, at least 1 tick. */
    public static int time(int originalTicks, double speedMult) {
        if (speedMult == 1.0) return originalTicks;
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.round(originalTicks / speedMult)));
    }

    /** Per-tick energy = base * totalEnergyMult * speedMult, rounded up. */
    public static int energyPerTick(int baseTick, double energyMult, double speedMult) {
        double v = baseTick * energyMult * speedMult;
        if (v == baseTick) return baseTick;
        return Math.max(1, (int) Math.ceil(v - 1e-9));
    }

    /** Max storage: max(1, round(original * mult)). */
    public static int capacity(int original, double mult) {
        if (mult == 1.0) return original;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(original * mult)));
    }

    /** Generator FE/t: max(1, round(original * S)). */
    public static int generatorOutput(int original, double speed) {
        if (speed == 1.0) return original;
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.round(original * speed)));
    }

    /** Generator burn time: max(1, round(original / S * U)). */
    public static int generatorBurnTime(int original, double speed, double utilization) {
        if (speed == 1.0 && utilization == 1.0) return original;
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.round(original / speed * utilization)));
    }

    /** Alloy Furnace FE/t (single line, charged once per tick): (base + extra if Extra slot not empty) with upgrade multipliers. */
    public static int alloyEnergyPerTick(int base, int extraAdd, boolean extraSlotFilled, double energyMult, double speedMult) {
        int b = Math.max(0, base) + (extraSlotFilled ? Math.max(0, extraAdd) : 0);
        if (b == 0) return 0;
        return energyPerTick(b, energyMult, speedMult);
    }

    /** Alloy Furnace energy charged this tick: the whole cost once if working and affordable, else 0 (no progress). */
    public static int alloyTickCharge(boolean working, int stored, int cost) {
        return working && stored >= cost ? cost : 0;
    }

    /** Whether the Alloy Furnace advances this tick. */
    public static boolean alloyTickProgresses(boolean working, int stored, int cost) {
        return working && stored >= cost;
    }

    /** Stored energy after a capacity change: kept if it fits, otherwise clamped to the capacity (never negative). */
    public static int clampEnergy(int energy, int capacity) {
        return Math.max(0, Math.min(energy, Math.max(1, capacity)));
    }

    /** Alloy Furnace code defaults (also the config defaults). */
    public static final int ALLOY_DEFAULT_FE = 80, ALLOY_DEFAULT_EXTRA_FE = 40, ALLOY_DEFAULT_CAPACITY = 50000, ALLOY_DEFAULT_MAX_RECEIVE = 1000;

    /** Energy bar fill in pixels (0..full), overflow-safe and clamped. */
    public static int energyBarHeight(int energy, int max, int full) {
        if (energy <= 0 || max <= 0) return 0;
        long h = (long) Math.min(energy, max) * full / max;
        return (int) Math.min(full, h);
    }
}
