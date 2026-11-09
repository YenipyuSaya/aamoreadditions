package com.aamoreadditions.effect;

import com.aamoreadditions.upgrade.UpgradeType;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Server config: supported [machine.upgrade] sections contain only consumed effects,
 * as plain multipliers applied on top of AA's current values at runtime.
 */
public final class UpgradeConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue LEAF_IDLE_SLEEP;
    private static final ModConfigSpec.IntValue COAL_FE, HEAT_FE, LEAF_FE, RX_CRUSHER, RX_CRUSHER_DOUBLE, RX_FURNACE, RX_ENERGIZER, TX_COAL, TX_HEAT, TX_OIL, TX_LEAF, CRUSHER_FE, CRUSHER_DOUBLE_FE, FURNACE_FE, ALLOY_FE, ALLOY_EXTRA_FE, ALLOY_CAPACITY, ALLOY_RX;
    /** Oil fuels: fluid id -> {burn_ticks, total_fe, mb_per_use}. */
    private static final Map<String, ModConfigSpec.IntValue[]> OIL = new java.util.LinkedHashMap<>();
    private static final ModConfigSpec.DoubleValue[] CROSS_SPEED = new ModConfigSpec.DoubleValue[3];
    private static final Map<MachineKind, Map<UpgradeType, Entry>> VALUES = new EnumMap<>(MachineKind.class);

    /** Unsupported effects have fixed neutral suppliers, not registered config values or nulls. */
    public record Entry(Supplier<Double> speed, Supplier<Double> energy, Supplier<Double> capacity,
                        Supplier<Double> inputRate, Supplier<Double> outputRate,
                        Supplier<Boolean> preventsLava, Supplier<Double> cooldown) {}
    private static final Supplier<Double> ONE = () -> 1.0;
    private static final Supplier<Boolean> FALSE = () -> false;
    private static final Entry NEUTRAL = new Entry(ONE, ONE, ONE, ONE, ONE, FALSE, ONE);

    private static final ModConfigSpec.BooleanValue FARMER_SMART_SLEEP;
    private static final ModConfigSpec.BooleanValue FARMER_ACCELERATES_RIPENING;
    public static boolean farmerAcceleratesRipening(){return !SPEC.isLoaded() || FARMER_ACCELERATES_RIPENING.get();}
    public static boolean farmerSmartSleep(){return !SPEC.isLoaded() || FARMER_SMART_SLEEP.get();}

    private static boolean isProcessor(MachineKind m) {
        return m == MachineKind.CRUSHER || m == MachineKind.CRUSHER_DOUBLE || m == MachineKind.POWERED_FURNACE;
    }

    private static boolean isGenerator(MachineKind m) {
        return m == MachineKind.COAL_GENERATOR || m == MachineKind.OIL_GENERATOR || m == MachineKind.HEAT_COLLECTOR || m == MachineKind.LEAF_GENERATOR;
    }

    /** Code defaults {speed, energy, capacity, input, output}; anything not listed = 1.0 (no effect). */
    private static double[] defaults(MachineKind m, UpgradeType t) {
        if (isProcessor(m)) { // same values for crusher, double crusher, double furnace; each has its own section
            var d = t.line == UpgradeType.Line.SPEED ? ProcessorUpgradeDefaults.speed(t.tier)
                    : ProcessorUpgradeDefaults.efficiency(t.tier);
            return new double[]{d.speed(), d.energy(), d.capacity(),
                    t.line == UpgradeType.Line.EFFICIENCY ? (t.tier == 1 ? 10 : 20) : 1, 1};
        }
        if (m == MachineKind.COAL_GENERATOR || m == MachineKind.OIL_GENERATOR) { // {S, U (utilization), 1, 1, output}
            switch (t) {
                case SPEED_1: return new double[]{2.0, 0.9, 1, 1, 2};
                case SPEED_2: return new double[]{3.0, 0.8, 1, 1, 5};
                case SPEED_3: return new double[]{5.0, 0.75, 1, 1, 10};
                case EFFICIENCY_1: return new double[]{1, 1.5, 2.0, 1, 1}; // utilization x1.5, storage x2
                case EFFICIENCY_2: return new double[]{1, 2.0, 3.0, 1, 1}; // utilization x2, storage x3
                default: break;
            }
        }
        if (m == MachineKind.ENERGIZER) { // efficiency only: storage x5 / x20; speed upgrades are not accepted
            switch (t) {
                case EFFICIENCY_1: return new double[]{1, 1, 5.0, 1, 1};
                case EFFICIENCY_2: return new double[]{1, 1, 20.0, 1, 1};
                default: return new double[]{1, 1, 1, 1, 1};
            }
        }
        if (m == MachineKind.HEAT_COLLECTOR) { // speed: FE/t only (assumed values); efficiency: storage only
            switch (t) {
                case SPEED_1: return new double[]{2.0, 1, 1, 1, 2};
                case SPEED_2: return new double[]{3.0, 1, 1, 1, 5};
                case SPEED_3: return new double[]{5.0, 1, 1, 1, 10};
                case EFFICIENCY_1: return new double[]{1, 1, 2.0, 1, 1};
                case EFFICIENCY_2: return new double[]{1, 1, 3.0, 1, 1};
                default: break;
            }
        }
        if (m == MachineKind.LEAF_GENERATOR) { // speed: cooldown only (see cooldownDefault); efficiency: FE per leaf + storage
            switch (t) {
                case SPEED_1: return new double[]{1, 1, 1, 1, 2};
                case SPEED_2: return new double[]{1, 1, 1, 1, 5};
                case SPEED_3: return new double[]{1, 1, 1, 1, 10};
                case EFFICIENCY_1: return new double[]{1, 2.0, 2.0, 1, 1};
                case EFFICIENCY_2: return new double[]{1, 3.0, 3.0, 1, 1};
                default: break;
            }
        }
        return new double[]{1, 1, 1, 1, 1};
    }

    private static double cooldownDefault(MachineKind m, UpgradeType t) {
        if (m != MachineKind.LEAF_GENERATOR) return 1.0;
        return switch (t) {
            case SPEED_1 -> 0.6;
            case SPEED_2 -> 0.4;
            case SPEED_3 -> 0.2;
            default -> 1.0;
        };
    }

    static {
        ModConfigSpec.Builder b = new com.aamoreadditions.config.EnglishConfigBuilder();
        b.push("base_values");
        CRUSHER_FE = b.comment("Crusher FE/t while crushing (AA: 40).").defineInRange("crusher_fe_per_tick", 40, 1, 1000000);
        CRUSHER_DOUBLE_FE = b.comment("Double crusher FE/t PER LANE (AA: 40; both lanes running = 2x).").defineInRange("crusher_double_fe_per_tick", 50, 1, 1000000);
        FURNACE_FE = b.comment("Powered (double) furnace FE/t PER LANE (AA: 25; both lanes = 2x).").defineInRange("powered_furnace_fe_per_tick", 30, 1, 1000000);
        ALLOY_FE = b.comment("Alloy Furnace FE/t while working (charged once per tick). Upgrade multipliers (powered_furnace section) apply on top.").defineInRange("alloy_furnace_fe_per_tick", UpgradeMath.ALLOY_DEFAULT_FE, 0, 1000000);
        ALLOY_EXTRA_FE = b.comment("Extra FE/t added to the Alloy Furnace's base while its Extra slot is not empty (80 + 40 = 120 by default).").defineInRange("alloy_furnace_extra_slot_fe_per_tick", UpgradeMath.ALLOY_DEFAULT_EXTRA_FE, 0, 1000000);
        ALLOY_CAPACITY = b.comment("Alloy Furnace energy storage capacity in FE (AA double furnace: 30000). Efficiency capacity_multiplier applies on top.",
                "Applied when the block entity loads; stored energy above the new capacity is clamped down.").defineInRange("alloy_furnace_capacity", UpgradeMath.ALLOY_DEFAULT_CAPACITY, 1, Integer.MAX_VALUE);
        ALLOY_RX = b.comment("Alloy Furnace max input rate from cables/pipes in FE/t (AA double furnace: 150). Efficiency input_rate_multiplier applies on top.").defineInRange("alloy_furnace_max_receive", UpgradeMath.ALLOY_DEFAULT_MAX_RECEIVE, 1, Integer.MAX_VALUE);
        RX_CRUSHER = b.comment("Input cap (maxReceive, FE/t) base values; efficiency input_rate_multiplier applies on top.").defineInRange("crusher_max_receive", 1000, 1, 100000000);
        RX_CRUSHER_DOUBLE = b.defineInRange("crusher_double_max_receive", 1000, 1, 100000000);
        RX_FURNACE = b.defineInRange("powered_furnace_max_receive", 1000, 1, 100000000);
        RX_ENERGIZER = b.comment("Energizer input cap (maxReceive, FE/t; AA: 1000). Default 2147483647 = effectively unlimited. Efficiency does not change it.").defineInRange("energizer_max_receive", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        TX_COAL = b.comment("Output cap (maxExtract, FE/t) base values; speed output_rate_multiplier applies on top.").defineInRange("coal_generator_max_extract", 4000, 1, 100000000);
        TX_HEAT = b.defineInRange("heat_collector_max_extract", 4000, 1, 100000000);
        TX_OIL = b.comment("Replaces AA's oilGeneratorTransfer.").defineInRange("oil_generator_max_extract", 4000, 1, 100000000);
        TX_LEAF = b.defineInRange("leaf_generator_max_extract", 1000, 1, 100000000);
        COAL_FE = b.comment("Coal generator FE/t (AA: 20). Burn times unchanged.").defineInRange("coal_generator_fe_per_tick", 30, 1, 1000000);
        HEAT_FE = b.comment("Heat collector FE/t (AA: 40).").defineInRange("heat_collector_fe_per_tick", 80, 1, 1000000);
        LEAF_FE = b.comment("Leaf generator FE per leaf (replaces AA's leafGeneratorCPPerLeaf, default 300).").defineInRange("leaf_generator_fe_per_leaf", 500, 1, 100000000);
        b.push("oil_generator");
        for (Object[] f : new Object[][]{{"canola_oil", 100, 4000}, {"refined_canola_oil", 120, 9600}, {"crystallized_oil", 280, 56000}, {"empowered_oil", 400, 140000}}) {
            b.push((String) f[0]);
            OIL.put("actuallyadditions:" + f[0], new ModConfigSpec.IntValue[]{
                    b.defineInRange("burn_ticks", (int) f[1], 1, 1000000),
                    b.defineInRange("total_fe", (int) f[2], 1, Integer.MAX_VALUE),
                    b.comment("mB consumed per burn cycle.").defineInRange("mb_per_use", 50, 1, 2000)});
            b.pop();
        }
        b.pop().pop();
        LEAF_IDLE_SLEEP = b.comment("Leaf generator: when a scan finds no leaves, skip scanning for this many ticks (0 = off). Applies regardless of upgrades.")
                .defineInRange("leaf_generator_idle_sleep_ticks", 100, 0, 72000);
        FARMER_SMART_SLEEP = b.comment("Enable Farmer smart sleep: after a complete normal-speed round finds no processable mature harvest, check one cell every 20 ticks. Capacity uses actual harvest drops, not empty slots; individual rejected crops do not slow an unfinished round. A mature harvest whose drops fit or successful planting restores normal cooldown. Disabling clears sleep; cheap harvest filtering remains enabled.")
                .define("farmer_smart_sleep", true);
        FARMER_ACCELERATES_RIPENING = b.comment("Enable the Farmer growth-ring slot and independent ripening loop. Disabling hides the slot and arrow, stops charging and ripening, and keeps the stored ring until re-enabled.")
                .define("farmer_accelerates_ripening", true);
        for (MachineKind m : MachineKind.values()) {
            b.push(m.key);
            Map<UpgradeType, Entry> map = new EnumMap<>(UpgradeType.class);
            for (UpgradeType t : UpgradeType.values()) {
                // Energizers cannot accept speed plugins in either mode. Keep a neutral runtime
                // entry for safe generic lookups, but do not create an empty config category.
                if (m == MachineKind.ENERGIZER && t.line == UpgradeType.Line.SPEED) {
                    map.put(t, NEUTRAL);
                    continue;
                }
                double[] d = defaults(m, t);
                boolean speedLine = t.line == UpgradeType.Line.SPEED;
                b.push(t.id);
                map.put(t, new Entry(
                        m != MachineKind.LEAF_GENERATOR && m != MachineKind.ENERGIZER
                                ? b.comment("Processing speed; time = original / value.").defineInRange("speed_multiplier", d[0], 0.01, 100000.0) : ONE,
                        m != MachineKind.HEAT_COLLECTOR && m != MachineKind.ENERGIZER
                                ? b.comment("Processing machines: TOTAL energy per operation (0 allowed; per-tick use never drops below 1 FE/t).", "Generators: UTILIZATION, multiplies the burn time per fuel after the speed division.").defineInRange("energy_multiplier", d[1], 0.0, 100000.0) : ONE,
                        b.comment("Max energy storage; stored energy is clamped when it drops.").defineInRange("capacity_multiplier", d[2], 0.01, 100000.0),
                        !isGenerator(m) && !speedLine ? b.comment("Hidden effect: external FE receive limit (AA's original maxReceive).",
                                "Dependency mode only: highest EFFICIENCY tier counts. Independent mode uses unlimited I/O.")
                                .defineInRange("input_rate_multiplier", d[3], 0.01, 100000.0) : ONE,
                        isGenerator(m) && speedLine ? b.comment("Dependency mode: generators' FE output limit, highest SPEED tier. Independent mode uses unlimited I/O.")
                                .defineInRange("output_rate_multiplier", d[4], 0.01, 100000.0) : ONE,
                        m == MachineKind.HEAT_COLLECTOR ? b.comment("Heat collector only: while this upgrade is installed, surrounding lava/magma is never consumed.")
                                .define("prevents_lava_consumption", m == MachineKind.HEAT_COLLECTOR && t.line == UpgradeType.Line.EFFICIENCY) : FALSE,
                        m == MachineKind.LEAF_GENERATOR && speedLine ? b.comment("Dependency mode: leaf cooldown multiplier of highest SPEED tier. Independent mode uses installed speed plugin count.")
                                .defineInRange("cooldown_multiplier", cooldownDefault(m, t), 0.0, 1000.0) : ONE));
                b.pop();
            }
            VALUES.put(m, map);
            b.pop();
        }
        b.push("casing_cross");
        for (UpgradeType t : UpgradeType.values()) if (t.line == UpgradeType.Line.SPEED) {
            b.push(t.id);
            CROSS_SPEED[t.tier - 1] = b.comment("Casing Cross processing speed. Dependency mode uses the highest active tier; independent mode sums all speed plugins and applies its speed boundary loss. Total recipe FE is unchanged.")
                    .defineInRange("speed_multiplier", CrossUpgradeMath.defaultSpeed(t.tier), 0.01, 100000.0);
            b.pop();
        }
        b.pop();
        SPEC = b.build();
    }

    private UpgradeConfig() {}

    private static Entry e(MachineKind m, UpgradeType t) {
        var machine = VALUES.get(m);
        return machine == null ? NEUTRAL : machine.getOrDefault(t, NEUTRAL);
    }

    public static double crossSpeed(int tier) {
        if (tier < 1 || tier > 3) return 1.0;
        return SPEC.isLoaded() ? CROSS_SPEED[tier - 1].get() : CrossUpgradeMath.defaultSpeed(tier);
    }

    public static double speed(MachineKind m, UpgradeType t) { return SPEC.isLoaded() ? e(m, t).speed().get() : 1.0; }

    public static double energy(MachineKind m, UpgradeType t) { return SPEC.isLoaded() ? e(m, t).energy().get() : 1.0; }

    public static double capacity(MachineKind m, UpgradeType t) { return SPEC.isLoaded() ? e(m, t).capacity().get() : 1.0; }

    public static double inputRate(MachineKind m, UpgradeType t) { return SPEC.isLoaded() ? e(m, t).inputRate().get() : 1.0; }

    public static double cooldown(MachineKind m, UpgradeType t) { return SPEC.isLoaded() ? e(m, t).cooldown().get() : 1.0; }

    public static int leafIdleSleepTicks() { return SPEC.isLoaded() ? LEAF_IDLE_SLEEP.get() : 0; }

    public static boolean preventsLava(MachineKind m, UpgradeType t) { return SPEC.isLoaded() && e(m, t).preventsLava().get(); }

    public static double outputRate(MachineKind m, UpgradeType t) { return SPEC.isLoaded() ? e(m, t).outputRate().get() : 1.0; }

    public static int processorFePerTick(MachineKind m, int aa) {
        if (!SPEC.isLoaded() || m == null) return aa;
        return switch (m) {
            case CRUSHER -> CRUSHER_FE.get();
            case CRUSHER_DOUBLE -> CRUSHER_DOUBLE_FE.get();
            case POWERED_FURNACE -> FURNACE_FE.get();
            default -> aa;
        };
    }
    public static int baseMaxReceive(MachineKind m, int aa) {
        if (!SPEC.isLoaded() || m == null || aa < 0) return aa;
        return switch (m) {
            case CRUSHER -> RX_CRUSHER.get();
            case CRUSHER_DOUBLE -> RX_CRUSHER_DOUBLE.get();
            case POWERED_FURNACE -> RX_FURNACE.get();
            case ENERGIZER -> RX_ENERGIZER.get();
            default -> aa;
        };
    }
    public static int baseMaxExtract(MachineKind m, int aa) {
        if (!SPEC.isLoaded() || m == null || aa < 0) return aa;
        return switch (m) {
            case COAL_GENERATOR -> TX_COAL.get();
            case HEAT_COLLECTOR -> TX_HEAT.get();
            case OIL_GENERATOR -> TX_OIL.get();
            case LEAF_GENERATOR -> TX_LEAF.get();
            default -> aa;
        };
    }
    public static int alloyCapacity() { return SPEC.isLoaded() ? ALLOY_CAPACITY.get() : UpgradeMath.ALLOY_DEFAULT_CAPACITY; }
    public static int alloyMaxReceive() { return SPEC.isLoaded() ? ALLOY_RX.get() : UpgradeMath.ALLOY_DEFAULT_MAX_RECEIVE; }

    /** Base capacity used before upgrade multipliers: the Alloy Furnace has its own, others keep AA's captured value. */
    public static int baseCapacity(net.minecraft.world.level.block.entity.BlockEntity be, int captured) {
        return be instanceof com.aamoreadditions.block.AlloyFurnaceBlockEntity ? alloyCapacity() : captured;
    }

    /** Base maxReceive before upgrade multipliers (Alloy Furnace: own config; others: existing per-machine config). */
    public static int baseMaxReceive(net.minecraft.world.level.block.entity.BlockEntity be, int captured) {
        return be instanceof com.aamoreadditions.block.AlloyFurnaceBlockEntity ? alloyMaxReceive() : baseMaxReceive(MachineKind.of(be), captured);
    }

    public static int alloyFePerTick() { return SPEC.isLoaded() ? ALLOY_FE.get() : UpgradeMath.ALLOY_DEFAULT_FE; }
    public static int alloyExtraFePerTick() { return SPEC.isLoaded() ? ALLOY_EXTRA_FE.get() : UpgradeMath.ALLOY_DEFAULT_EXTRA_FE; }
    public static int coalFePerTick(int aa) { return SPEC.isLoaded() ? COAL_FE.get() : aa; }
    public static int heatFePerTick(int aa) { return SPEC.isLoaded() ? HEAT_FE.get() : aa; }
    public static int leafFePerLeaf(int aa) { return SPEC.isLoaded() ? LEAF_FE.get() : aa; }

    /** index 0 = burn ticks, 1 = total FE, 2 = mB per use; returns aa if fluid not configured. */
    public static int oil(net.neoforged.neoforge.fluids.FluidStack fuel, int index, int aa) {
        if (!SPEC.isLoaded() || fuel == null || fuel.isEmpty()) return aa;
        var id = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fuel.getFluid());
        var v = OIL.get(id.toString());
        if (v == null) {
            if (fuel.getFluid() instanceof net.minecraft.world.level.material.FlowingFluid ff) v = OIL.get(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(ff.getSource()).toString());
        }
        return v == null ? aa : v[index].get();
    }
}
