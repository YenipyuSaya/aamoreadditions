package com.aamoreadditions.effect;

import com.aamoreadditions.upgrade.UpgradeSlots;
import com.aamoreadditions.upgrade.UpgradeType;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;

/**
 * Resolves the combined multipliers for a machine. Combination rule (confirmed):
 * per line only the highest installed tier counts; lines are multiplied together (EnergyCombine).
 * Active upgrades are cached in the slot handler and invalidated on slot changes;
 * config values are read live (cheap, cached by NeoForge).
 */
public final class UpgradeEffects {
    private UpgradeEffects() {}

    public static double speed(BlockEntity be) {
        MachineKind m = MachineKind.of(be);
        if (m == null || !be.hasData(UpgradeSlots.UPGRADE_INV)) return 1.0;
        java.util.List<Double> mults = new java.util.ArrayList<>(2);
        for (UpgradeType t : active(be)) mults.add(UpgradeConfig.speed(m, t));
        return Math.max(0.01, EnergyCombine.combine(mults));
    }

    public static double energy(BlockEntity be) {
        MachineKind m = MachineKind.of(be);
        if (m == null || !be.hasData(UpgradeSlots.UPGRADE_INV)) return 1.0;
        java.util.List<Double> mults = new java.util.ArrayList<>(2);
        for (UpgradeType t : active(be)) mults.add(UpgradeConfig.energy(m, t));
        return EnergyCombine.combine(mults);
    }

    /** Takes the active set directly: called from the handler itself (also during attachment deserialization). */
    public static double capacity(BlockEntity be, Set<UpgradeType> active) {
        MachineKind m = MachineKind.of(be);
        if (m == null) return 1.0;
        java.util.List<Double> mults = new java.util.ArrayList<>(2);
        for (UpgradeType t : active) mults.add(UpgradeConfig.capacity(m, t));
        return EnergyCombine.combine(mults);
    }

    public static de.ellpeck.actuallyadditions.mod.tile.CustomEnergyStorage storage(BlockEntity be) {
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityCrusher t) return t.storage;
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityPoweredFurnace t) return t.storage;
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityCoalGenerator t) return t.storage;
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityOilGenerator t) return t.storage;
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityHeatCollector t) return t.storage;
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityLeafGenerator t) return t.storage;
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityEnergizer t) return t.storage;
        return null;
    }

    /**
     * Sets the storage's per-instance capacity to round(AA's original capacity * multiplier) and clamps the
     * stored energy if it dropped. AA's constants/constructor values are untouched; the original capacity is
     * captured from the storage before we ever modify it. All readers (GUI bar, receiveEnergy, Jade) see it.
     */
    /** Hidden: receive-limit multiplier of the highest installed efficiency tier only. */
    public static double inputRate(BlockEntity be, Set<UpgradeType> active) {
        MachineKind m = MachineKind.of(be);
        if (m == null) return 1.0;
        for (UpgradeType t : active) if (t.line == UpgradeType.Line.EFFICIENCY) return UpgradeConfig.inputRate(m, t);
        return 1.0;
    }

    /** Sets maxReceive = max(1, round(AA's original maxReceive * multiplier)); originals of 0 (generators) stay 0. */
    public static void applyInputRate(BlockEntity be, int baseMaxReceive, Set<UpgradeType> active) {
        var st = storage(be);
        if (st == null || baseMaxReceive <= 0) return;
        ((com.aamoreadditions.mixin.EnergyStorageAccessor) st).aamoreadditions$setMaxReceive(UpgradeMath.capacity(baseMaxReceive, inputRate(be, active)));
    }

    public static void applyCapacity(BlockEntity be, int baseCapacity, Set<UpgradeType> active) {
        var st = storage(be);
        if (st == null || baseCapacity <= 0) return;
        int cap = UpgradeMath.capacity(baseCapacity, capacity(be, active));
        var acc = (com.aamoreadditions.mixin.EnergyStorageAccessor) st;
        acc.aamoreadditions$setCapacity(cap);
        // Server is authoritative: never clamp on the client. The client handler may briefly have no/partial upgrades
        // (attachment isn't in AA's sync tag; slots arrive one by one via the menu), which would cut the synced energy.
        boolean client = be.getLevel() != null && be.getLevel().isClientSide;
        int clamped = UpgradeMath.clampEnergy(acc.aamoreadditions$getEnergy(), cap);
        if (!client && clamped != acc.aamoreadditions$getEnergy()) {
            acc.aamoreadditions$setEnergy(clamped);
            be.setChanged();
        }
    }

    private static Set<UpgradeType> active(BlockEntity be) {
        return UpgradeSlots.get(be).activeUpgrades();
    }

    /** Only coal + oil generators have hooks; utilization = the section's energy_multiplier. */
    public static int generatorOutput(BlockEntity be, int original) {
        return UpgradeMath.generatorOutput(original, speed(be));
    }

    /** Leaf generator cooldown: max(1, round(original * cooldown_multiplier of the highest SPEED tier)). */
    public static int cooldown(BlockEntity be, int original) {
        MachineKind m = MachineKind.of(be);
        if (m == null || !be.hasData(UpgradeSlots.UPGRADE_INV)) return original;
        for (UpgradeType t : active(be)) {
            if (t.line == UpgradeType.Line.SPEED) return UpgradeMath.capacity(original, UpgradeConfig.cooldown(m, t));
        }
        return original;
    }

    /** Leaf generator FE per leaf: max(1, round(original * energy)). */
    public static int leafEnergy(BlockEntity be, int original) {
        return UpgradeMath.capacity(original, energy(be));
    }

    /** True if any installed (active) upgrade has prevents_lava_consumption for this machine. */
    public static boolean preventsLavaConsumption(BlockEntity be) {
        MachineKind m = MachineKind.of(be);
        if (m == null || !be.hasData(UpgradeSlots.UPGRADE_INV)) return false;
        for (UpgradeType t : active(be)) if (UpgradeConfig.preventsLava(m, t)) return true;
        return false;
    }

    public static int generatorBurnTime(BlockEntity be, int original) {
        return UpgradeMath.generatorBurnTime(original, speed(be), energy(be));
    }

    /** Output (maxExtract) multiplier of the highest installed SPEED tier only. */
    public static double outputRate(BlockEntity be, Set<UpgradeType> active) {
        MachineKind m = MachineKind.of(be);
        if (m == null) return 1.0;
        for (UpgradeType t : active) if (t.line == UpgradeType.Line.SPEED) return UpgradeConfig.outputRate(m, t);
        return 1.0;
    }

    /** Sets maxExtract = max(1, round(AA's original maxExtract * multiplier)); originals of 0 stay 0. */
    public static void applyOutputRate(BlockEntity be, int baseMaxExtract, Set<UpgradeType> active) {
        var st = storage(be);
        if (st == null || baseMaxExtract <= 0) return;
        ((com.aamoreadditions.mixin.EnergyStorageAccessor) st).aamoreadditions$setMaxExtract(UpgradeMath.capacity(baseMaxExtract, outputRate(be, active)));
    }

    public static int time(BlockEntity be, int original) {
        return UpgradeMath.time(original, speed(be));
    }

    public static int energyPerTick(BlockEntity be, int original) {
        return UpgradeMath.energyPerTick(UpgradeConfig.processorFePerTick(MachineKind.of(be), original), energy(be), speed(be));
    }

    /** Alloy Furnace: one FE/t value for the whole machine. */
    public static int alloyEnergyPerTick(BlockEntity be, boolean extraSlotFilled) {
        return UpgradeMath.alloyEnergyPerTick(UpgradeConfig.alloyFePerTick(), UpgradeConfig.alloyExtraFePerTick(), extraSlotFilled, energy(be), speed(be));
    }
}
