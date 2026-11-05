package com.aamoreadditions.client;

import com.aamoreadditions.effect.MachineKind;
import com.aamoreadditions.effect.UpgradeEffects;
import com.aamoreadditions.effect.UpgradeMath;
import de.ellpeck.actuallyadditions.mod.tile.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Builds the info tooltip from the same UpgradeEffects/UpgradeMath calls the machines use. */
public final class UpgradeInfo {
    private UpgradeInfo() {}

    public static String fmt(double v) {
        BigDecimal d = BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        if (d.scale() < 0) d = d.setScale(0);
        return "\u00d7" + d.toPlainString();
    }

    private static MutableComponent aqua(double v) { return Component.literal(fmt(v)).withStyle(ChatFormatting.AQUA); }

    public static List<Component> lines(BlockEntity be) {
        MachineKind m = be == null ? null : MachineKind.of(be);
        if (m == null) return List.of();
        double speed = UpgradeEffects.speed(be), energy = UpgradeEffects.energy(be);
        switch (m) {
            case CRUSHER, CRUSHER_DOUBLE, POWERED_FURNACE -> {
                int base = m == MachineKind.POWERED_FURNACE ? TileEntityPoweredFurnace.ENERGY_USE : TileEntityCrusher.ENERGY_USE;
                int fe = be instanceof com.aamoreadditions.block.AlloyFurnaceBlockEntity af ? af.energyPerTick() : UpgradeEffects.energyPerTick(be, base);
                return List.of(
                        Component.translatable("gui.aamoreadditions.info.speed", aqua(speed)).withStyle(ChatFormatting.WHITE),
                        Component.translatable("gui.aamoreadditions.info.energy", aqua(energy), Component.literal(String.valueOf(fe))).withStyle(ChatFormatting.WHITE));
            }
            case COAL_GENERATOR, OIL_GENERATOR -> {
                int fe = m == MachineKind.COAL_GENERATOR
                        ? UpgradeEffects.generatorOutput(be, com.aamoreadditions.effect.UpgradeConfig.coalFePerTick(TileEntityCoalGenerator.ENERGY_PER_TICK))
                        : oilOutput((TileEntityOilGenerator) be);
                return List.of(
                        Component.translatable("gui.aamoreadditions.info.gen_speed", aqua(speed),
                                Component.literal(String.valueOf(fe)).withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.WHITE),
                        Component.translatable("gui.aamoreadditions.info.utilization", aqua(energy)).withStyle(ChatFormatting.WHITE));
            }
            default -> { return List.of(); }
        }
    }

    /** Running: the synced (already upgraded) currentEnergyProduce. Idle: what the fluid in the tank would give. Empty: 0. */
    private static int oilOutput(TileEntityOilGenerator t) {
        if (t.currentEnergyProduce > 0) return t.currentEnergyProduce;
        var fluid = t.tank.getFluid();
        if (fluid.isEmpty()) return 0;
        try {
            for (var r : de.ellpeck.actuallyadditions.api.ActuallyAdditionsAPI.LIQUID_FUEL_RECIPES) {
                if (r != null && r.value().matches(fluid)) {
                    var f = r.value().getFuel();
                    int burn = com.aamoreadditions.effect.UpgradeConfig.oil(f, 0, r.value().getBurnTime());
                    int total = com.aamoreadditions.effect.UpgradeConfig.oil(f, 1, r.value().getTotalEnergy());
                    if (burn > 0) return UpgradeEffects.generatorOutput(t, total / burn);
                }
            }
        } catch (RuntimeException ignored) {}
        return 0;
    }
}
