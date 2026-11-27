package com.aamoreadditions.digger;

import com.aamoreadditions.effect.FarmerUpgradeMath;
import com.aamoreadditions.effect.UpgradeConfig;
import com.aamoreadditions.upgrade.UpgradeLogic;
import com.aamoreadditions.upgrade.UpgradeSlots;
import com.aamoreadditions.upgrade.UpgradeStartup;
import com.aamoreadditions.upgrade.UpgradeType;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;

import java.util.List;

/**
 * Vertical Digger upgrade effects. Speed/Fortune drill upgrades are aliases of the AAMA speed/efficiency
 * lines, so the Farmer policy (FarmerUpgradeMath) is reused unchanged: scan period, energy multiplier,
 * capacity multiplier and fortune level (Fortune I/II = Efficiency I/II on the Farmer).
 */
public final class DiggerEffects {
    private DiggerEffects() {}

    /** AA's TileEntityVerticalDigger: one block every 5 ticks, 650 FE, x3 while only mining ores. */
    public static final int AA_PERIOD = 5;
    public static final int DEFAULT_MINING_FE = 650, DEFAULT_ORES_ONLY_MULTIPLIER = 3, DEFAULT_PLACING_FE = 500;

    public static FarmerUpgradeMath.Effects evaluate(List<UpgradeType> active) {
        return evaluate(active, UpgradeStartup.dependency());
    }

    public static FarmerUpgradeMath.Effects evaluate(List<UpgradeType> active, boolean dependency) {
        return FarmerUpgradeMath.evaluate(active.stream().map(t -> new UpgradeLogic.Plugin(t.line.name(), t.tier)).toList(), dependency);
    }

    public static FarmerUpgradeMath.Effects evaluate(TileEntityVerticalDigger tile) {
        return evaluate(UpgradeSlots.get(tile).activeUpgrades());
    }

    /** Ticks between two scanned positions (AA: 5). One position per tick at most. */
    public static int period(double speed) {
        return FarmerUpgradeMath.scanTicks(AA_PERIOD, speed);
    }

    /** Energy of one action; a base of 0 stays free (operationCost has a 1 FE floor). */
    public static int cost(int base, double energyMultiplier) {
        return base <= 0 ? 0 : FarmerUpgradeMath.operationCost(base, energyMultiplier);
    }

    public static int miningBase(boolean onlyOres, int miningFe, int oresOnlyMultiplier) {
        return (int) Math.min(Integer.MAX_VALUE, (long) miningFe * (onlyOres ? oresOnlyMultiplier : 1));
    }

    public static int miningCost(TileEntityVerticalDigger tile, double energyMultiplier) {
        return cost(miningBase(tile.onlyMineOres, UpgradeConfig.diggerMiningFe(), UpgradeConfig.diggerOresOnlyMultiplier()), energyMultiplier);
    }

    /**
     * AA requires {@code stored >= miningCost} at every position (including air) before the cursor
     * may advance; the cost is not extracted unless a block is actually mined. That gate applies
     * only when no range upgrade is installed ({@code span <= 1}). With a 3x3/5x5 upgrade, air
     * and skipped blocks do not need stored energy.
     */
    public static boolean hasBufferedMiningCost(int stored, int mineCost, int span) {
        return span > 1 || stored >= mineCost;
    }


    public static int placingCost(double energyMultiplier) {
        return cost(UpgradeConfig.diggerPlacingFe(), energyMultiplier);
    }

    public static boolean has(TileEntityVerticalDigger tile, DiggerUpgrade kind) {
        for (var stack : UpgradeSlots.get(tile).contents()) if (DiggerUpgrade.of(stack) == kind) return true;
        return false;
    }

    /** Silk Touch is a plain on/off plugin. Never together with Fortune (slot rules). */
    public static boolean silkTouch(TileEntityVerticalDigger tile) {
        return UpgradeSlots.get(tile).legal() && has(tile, DiggerUpgrade.SILK_TOUCH);
    }

    public static int fortune(TileEntityVerticalDigger tile) {
        return UpgradeSlots.get(tile).legal() && !has(tile, DiggerUpgrade.SILK_TOUCH) ? evaluate(tile).fortune() : 0;
    }

    /** True when the Drill Block Placing plugin is installed (mode is checked separately). */
    public static boolean fillersUnlocked(TileEntityVerticalDigger tile) {
        return has(tile, DiggerUpgrade.BLOCK_PLACING);
    }

    /**
     * The placing effect (and its FE cost) runs only when the Block Placing plugin is installed
     * and the machine is in AA's ores-only mode ({@code onlyMineOres}).
     */
    public static boolean placingEnabled(boolean unlocked, boolean onlyMineOres) {
        return unlocked && onlyMineOres;
    }

    public static boolean placingEnabled(TileEntityVerticalDigger tile) {
        return placingEnabled(fillersUnlocked(tile), tile.onlyMineOres);
    }

    /**
     * Blocks checked down the current column in one work action: 1, or 3 / 5 with a Range plugin.
     * Illegal machines (and machines with no range plugin) stay at 1.
     */
    public static int verticalSpan(boolean legal, boolean range3, boolean range5) {
        if (!legal) return 1;
        if (range5) return 5;
        if (range3) return 3;
        return 1;
    }

    public static int verticalSpan(TileEntityVerticalDigger tile) {
        boolean r3 = false, r5 = false;
        for (var stack : UpgradeSlots.get(tile).contents()) {
            var u = DiggerUpgrade.of(stack);
            if (u == DiggerUpgrade.RANGE_5) r5 = true;
            else if (u == DiggerUpgrade.RANGE_3) r3 = true;
        }
        return verticalSpan(UpgradeSlots.get(tile).legal(), r3, r5);
    }

    /** Inventory locked (no insert, barrier ghosts) when the placing effect cannot run. Extraction stays allowed. */
    public static boolean fillersLocked(boolean unlocked, boolean onlyMineOres) {
        return !placingEnabled(unlocked, onlyMineOres);
    }

    public static boolean fillersLocked(TileEntityVerticalDigger tile) {
        return !placingEnabled(tile);
    }
}
