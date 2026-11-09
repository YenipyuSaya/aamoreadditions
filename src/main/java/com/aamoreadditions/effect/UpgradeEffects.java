package com.aamoreadditions.effect;

import com.aamoreadditions.upgrade.UpgradeSlots;
import com.aamoreadditions.upgrade.UpgradeType;
import net.minecraft.world.level.block.entity.BlockEntity;


/**
 * Resolves the combined multipliers for a machine. Combination rule (confirmed):
 * dependency mode uses the highest tier per line and legacy multiplication;
 * independent mode delegates all arithmetic to UpgradeAggregation.
 * Active upgrades are cached in the slot handler and invalidated on slot changes;
 * config values are read live (cheap, cached by NeoForge).
 */
public final class UpgradeEffects {
    private UpgradeEffects() {}

    private static java.util.List<Double> coefficients(java.util.List<UpgradeType> active, UpgradeType.Line line,
                                                       java.util.function.ToDoubleFunction<UpgradeType> value) {
        return active.stream().filter(t -> t.line == line).map(t -> value.applyAsDouble(t)).toList();
    }

    public static double speed(BlockEntity be) {
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer t) return FarmerEffects.evaluate(t).speed();
        MachineKind m = MachineKind.of(be);
        if (m == null || !be.hasData(UpgradeSlots.UPGRADE_INV)) return 1.0;
        if (!com.aamoreadditions.upgrade.UpgradeStartup.dependency())
            return UpgradeAggregation.speed(coefficients(active(be), UpgradeType.Line.SPEED, t -> UpgradeConfig.speed(m,t)));
        java.util.List<Double> mults = new java.util.ArrayList<>(2);
        for (UpgradeType t : active(be)) mults.add(UpgradeConfig.speed(m, t));
        return Math.max(0.01, EnergyCombine.combine(mults));
    }

    public static double energy(BlockEntity be) {
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer t) return FarmerEffects.evaluate(t).energy();
        MachineKind m = MachineKind.of(be);
        if (m == null || !be.hasData(UpgradeSlots.UPGRADE_INV)) return 1.0;
        if (!com.aamoreadditions.upgrade.UpgradeStartup.dependency()) {
            var s = coefficients(active(be), UpgradeType.Line.SPEED, t -> UpgradeConfig.energy(m,t));
            var e = coefficients(active(be), UpgradeType.Line.EFFICIENCY, t -> UpgradeConfig.energy(m,t));
            return switch (m) {
                case CRUSHER, CRUSHER_DOUBLE, POWERED_FURNACE -> UpgradeAggregation.processorEnergy(s,e);
                case COAL_GENERATOR, OIL_GENERATOR -> UpgradeAggregation.generatorUtilization(s,e);
                case LEAF_GENERATOR -> UpgradeAggregation.unpenalizedSum(e);
                default -> 1.0;
            };
        }
        java.util.List<Double> mults = new java.util.ArrayList<>(2);
        for (UpgradeType t : active(be)) mults.add(UpgradeConfig.energy(m, t));
        return EnergyCombine.combine(mults);
    }

    /** Takes the active set directly: called from the handler itself (also during attachment deserialization). */
    public static double capacity(BlockEntity be, java.util.List<UpgradeType> active) {
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer) return FarmerEffects.evaluate(active).capacity();
        MachineKind m = MachineKind.of(be);
        if (m == null) return 1.0;
        java.util.List<Double> mults = new java.util.ArrayList<>(2);
        if (!com.aamoreadditions.upgrade.UpgradeStartup.dependency())
            return UpgradeAggregation.unpenalizedSum(coefficients(active, UpgradeType.Line.EFFICIENCY, t -> UpgradeConfig.capacity(m,t)));
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
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer t) return t.storage;
        return null;
    }

    /**
     * Sets the storage's per-instance capacity to round(AA's original capacity * multiplier) and clamps the
     * stored energy if it dropped. AA's constants/constructor values are untouched; the original capacity is
     * captured from the storage before we ever modify it. All readers (GUI bar, receiveEnergy, Jade) see it.
     */
    /** Hidden: receive-limit multiplier of the highest installed efficiency tier only. */
    public static double inputRate(BlockEntity be, java.util.List<UpgradeType> active) {
        MachineKind m = MachineKind.of(be);
        if (m == null) return 1.0;
        for (UpgradeType t : active) if (t.line == UpgradeType.Line.EFFICIENCY) return UpgradeConfig.inputRate(m, t);
        return 1.0;
    }

    /** Sets maxReceive = max(1, round(AA's original maxReceive * multiplier)); originals of 0 (generators) stay 0. */
    public static void applyInputRate(BlockEntity be, int baseMaxReceive, java.util.List<UpgradeType> active) {
        var st = storage(be);
        if (st == null) return;
        if (!com.aamoreadditions.upgrade.UpgradeStartup.dependency()) {
            ((com.aamoreadditions.mixin.EnergyStorageAccessor) st).aamoreadditions$setMaxReceive(Integer.MAX_VALUE); return;
        }
        if (baseMaxReceive <= 0) return;
        ((com.aamoreadditions.mixin.EnergyStorageAccessor) st).aamoreadditions$setMaxReceive(UpgradeMath.capacity(baseMaxReceive, inputRate(be, active)));
    }

    public static void applyCapacity(BlockEntity be, int baseCapacity, java.util.List<UpgradeType> active) {
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

    private static java.util.List<UpgradeType> active(BlockEntity be) {
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
        if (!com.aamoreadditions.upgrade.UpgradeStartup.dependency())
            return UpgradeMath.capacity(original, UpgradeAggregation.leafCooldown((int) active(be).stream().filter(t -> t.line == UpgradeType.Line.SPEED).count()));
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
    public static double outputRate(BlockEntity be, java.util.List<UpgradeType> active) {
        MachineKind m = MachineKind.of(be);
        if (m == null) return 1.0;
        for (UpgradeType t : active) if (t.line == UpgradeType.Line.SPEED) return UpgradeConfig.outputRate(m, t);
        return 1.0;
    }

    /** Sets maxExtract = max(1, round(AA's original maxExtract * multiplier)); originals of 0 stay 0. */
    public static void applyOutputRate(BlockEntity be, int baseMaxExtract, java.util.List<UpgradeType> active) {
        var st = storage(be);
        if (st == null) return;
        if (!com.aamoreadditions.upgrade.UpgradeStartup.dependency()) {
            ((com.aamoreadditions.mixin.EnergyStorageAccessor) st).aamoreadditions$setMaxExtract(Integer.MAX_VALUE); return;
        }
        if (baseMaxExtract <= 0) return;
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
