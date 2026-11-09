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
        if (be instanceof TileEntityFarmer tile) {
            var effects = com.aamoreadditions.effect.FarmerEffects.evaluate(tile);
            return List.of(
                    Component.translatable("gui.aamoreadditions.info.farmer.cooldown",
                            Component.literal(String.valueOf(com.aamoreadditions.effect.FarmerScanRuntime.cooldown(tile))).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.WHITE),
                    Component.translatable("gui.aamoreadditions.info.farmer.energy",
                            aqua(effects.energy())).withStyle(ChatFormatting.WHITE),
                    Component.translatable("gui.aamoreadditions.info.farmer.fortune",
                            Component.literal(String.valueOf(com.aamoreadditions.effect.FarmerEffects.fortune(tile)))
                                    .withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.WHITE));
        }
        var lines = new java.util.ArrayList<>(baseLines(be));
        if (be != null && com.aamoreadditions.upgrade.UpgradeSlots.supports(be))
            appendBoundary(lines, com.aamoreadditions.upgrade.UpgradeSlots.get(be));
        return lines;
    }

    public static void appendBoundary(java.util.List<Component> lines, com.aamoreadditions.upgrade.UpgradeSlots.Handler h) {
        if (com.aamoreadditions.upgrade.UpgradeSlots.isFarmer(h.holder())) return;
        var active = h.activeUpgrades();
        int speed = (int) active.stream().filter(t -> t.line == com.aamoreadditions.upgrade.UpgradeType.Line.SPEED).count();
        int efficiency = (int) active.stream().filter(t -> t.line == com.aamoreadditions.upgrade.UpgradeType.Line.EFFICIENCY).count();
        com.aamoreadditions.effect.UpgradeAggregation.Kind kind;
        if (h.holder() instanceof com.aamoreadditions.upgrade.SpeedOnlyHolder) kind = com.aamoreadditions.effect.UpgradeAggregation.Kind.CROSS;
        else {
            var machine = MachineKind.of((BlockEntity) h.holder());
            if (machine == null) return;
            kind = switch(machine) {
                case COAL_GENERATOR, OIL_GENERATOR -> com.aamoreadditions.effect.UpgradeAggregation.Kind.FUEL_GENERATOR;
                case HEAT_COLLECTOR -> com.aamoreadditions.effect.UpgradeAggregation.Kind.HEAT;
                case LEAF_GENERATOR -> com.aamoreadditions.effect.UpgradeAggregation.Kind.LEAF;
                case ENERGIZER -> com.aamoreadditions.effect.UpgradeAggregation.Kind.ENERGIZER;
                default -> com.aamoreadditions.effect.UpgradeAggregation.Kind.PROCESSOR;
            };
        }
        var f = com.aamoreadditions.effect.UpgradeAggregation.boundaryFactors(kind,speed,efficiency,com.aamoreadditions.upgrade.UpgradeStartup.dependency());
        if (f.speed()==1 && f.energy()==1 && f.utilization()==1) return;
        lines.add(Component.empty());
        lines.add(Component.translatable("gui.aamoreadditions.info.boundary"));
        boundaryLine(lines,"speed",f.speed());
        boundaryLine(lines,"energy",f.energy());
        boundaryLine(lines,"utilization",f.utilization());
    }
    private static void boundaryLine(List<Component> lines,String key,double value) {
        if (value == 1) return;
        String number = BigDecimal.valueOf(value).setScale(8,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        lines.add(Component.translatable("gui.aamoreadditions.info.boundary."+key,
                Component.literal(number).withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.WHITE));
    }

    private static List<Component> baseLines(BlockEntity be) {
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
