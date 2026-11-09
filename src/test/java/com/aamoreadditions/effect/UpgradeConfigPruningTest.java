package com.aamoreadditions.effect;

import com.aamoreadditions.config.EnglishConfigNames;
import com.aamoreadditions.upgrade.UpgradeStartup;
import com.aamoreadditions.upgrade.UpgradeType;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec.CorrectionAction;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import net.neoforged.fml.config.InMemoryConfigFixture;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real constructed NeoForge specs and in-memory correction/loading, not a running Configured/game test. */
class UpgradeConfigPruningTest {
    private static final List<String> FIELDS = List.of("speed_multiplier", "energy_multiplier", "capacity_multiplier",
            "input_rate_multiplier", "output_rate_multiplier", "prevents_lava_consumption", "cooldown_multiplier");
    private CommentedConfig config;
    @BeforeEach void loadDefaults() {
        config = CommentedConfig.inMemory();
        UpgradeConfig.SPEC.correct(config);
        assertTrue(UpgradeConfig.SPEC.isCorrect(config));
        UpgradeConfig.SPEC.acceptConfig(InMemoryConfigFixture.of(config));
    }
    @AfterEach void unload() { UpgradeConfig.SPEC.acceptConfig(null); }
    private static Map<String, ModConfigSpec.ValueSpec> leaves(UnmodifiableConfig spec) {
        Map<String, ModConfigSpec.ValueSpec> result = new TreeMap<>();
        collect(spec, "", result); return result;
    }
    private static void collect(UnmodifiableConfig node, String prefix, Map<String, ModConfigSpec.ValueSpec> result) {
        assertFalse(node.valueMap().isEmpty(), "Empty config category: " + prefix);
        node.valueMap().forEach((key, value) -> {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof UnmodifiableConfig child) collect(child, path, result);
            else result.put(path, assertInstanceOf(ModConfigSpec.ValueSpec.class, value));
        });
    }
    private static String path(MachineKind m, UpgradeType t, int field) { return m.key + "." + t.id + "." + FIELDS.get(field); }
    /** Independent expected matrix: seven letters correspond to original S,E,C,I,O,L,D fields. */
    private static String expectedFields(MachineKind m, UpgradeType t) {
        boolean speed = t.line == UpgradeType.Line.SPEED;
        return switch (m) {
            case CRUSHER, CRUSHER_DOUBLE, POWERED_FURNACE -> speed ? "SEC" : "SECI";
            case COAL_GENERATOR, OIL_GENERATOR -> speed ? "SECO" : "SEC";
            case HEAT_COLLECTOR -> speed ? "SCOL" : "SCL";
            case LEAF_GENERATOR -> speed ? "ECOD" : "EC";
            case ENERGIZER -> speed ? "" : "CI";
        };
    }
    private static double[] legacyDefaults(MachineKind m, UpgradeType t) {
        int i = t.tier - 1;
        boolean speed = t.line == UpgradeType.Line.SPEED;
        double[] result = {1, 1, 1, 1, 1, 0, 1};
        switch (m) {
            case CRUSHER, CRUSHER_DOUBLE, POWERED_FURNACE -> {
                if (speed) { result[0] = new double[]{2,3,5}[i]; result[1] = new double[]{1.5,2,2.5}[i]; }
                else { result[1] = new double[]{.8,.5}[i]; result[2] = new double[]{1.5,2}[i]; result[3] = new double[]{10,20}[i]; }
            }
            case COAL_GENERATOR, OIL_GENERATOR -> {
                if (speed) { result[0] = new double[]{2,3,5}[i]; result[1] = new double[]{.9,.8,.75}[i]; result[4] = new double[]{2,5,10}[i]; }
                else { result[1] = new double[]{1.5,2}[i]; result[2] = new double[]{2,3}[i]; }
            }
            case HEAT_COLLECTOR -> {
                if (speed) { result[0] = new double[]{2,3,5}[i]; result[4] = new double[]{2,5,10}[i]; }
                else { result[2] = new double[]{2,3}[i]; result[5] = 1; }
            }
            case LEAF_GENERATOR -> {
                if (speed) { result[4] = new double[]{2,5,10}[i]; result[6] = new double[]{.6,.4,.2}[i]; }
                else { result[1] = new double[]{2,3}[i]; result[2] = new double[]{2,3}[i]; }
            }
            case ENERGIZER -> { if (!speed) result[2] = new double[]{5,20}[i]; }
        }
        return result;
    }
    private static double[] actual(MachineKind m, UpgradeType t) {
        return new double[]{UpgradeConfig.speed(m,t), UpgradeConfig.energy(m,t), UpgradeConfig.capacity(m,t),
                UpgradeConfig.inputRate(m,t), UpgradeConfig.outputRate(m,t), UpgradeConfig.preventsLava(m,t) ? 1 : 0,
                UpgradeConfig.cooldown(m,t)};
    }
    @Test void completeConstructedSpecIs173LeavesWith155RemovedAndThreeCrossFieldsAdded() {
        var server = leaves(UpgradeConfig.SPEC.getSpec());
        var startup = leaves(UpgradeStartup.SPEC.getSpec());
        assertEquals(161, server.size()); assertEquals(12, startup.size());
        assertEquals(173, server.size() + startup.size());
        assertEquals(155, 325 - (server.size() + startup.size() - 3));
        assertEquals(33, server.keySet().stream().filter(k -> k.startsWith("base_values.") || !k.contains(".")).count());
        assertFalse(UpgradeConfig.SPEC.getSpec().contains("energizer.speed_upgrade_machine_1"));
        assertFalse(UpgradeConfig.SPEC.getSpec().contains("energizer.speed_upgrade_machine_2"));
        assertFalse(UpgradeConfig.SPEC.getSpec().contains("energizer.speed_upgrade_machine_3"));
    }
    @Test void everyMachineTierAndFieldMatchesRealConsumerMatrix() {
        var spec = leaves(UpgradeConfig.SPEC.getSpec());
        Map<MachineKind,Integer> expectedCounts = Map.of(MachineKind.CRUSHER,17, MachineKind.CRUSHER_DOUBLE,17,
                MachineKind.POWERED_FURNACE,17, MachineKind.COAL_GENERATOR,18, MachineKind.OIL_GENERATOR,18,
                MachineKind.HEAT_COLLECTOR,18, MachineKind.LEAF_GENERATOR,16, MachineKind.ENERGIZER,4);
        for (var m : MachineKind.values()) {
            int count = 0;
            for (var t : UpgradeType.values()) for (int f = 0; f < FIELDS.size(); f++) {
                boolean kept = expectedFields(m,t).indexOf("SECIOLD".charAt(f)) >= 0;
                assertEquals(kept, spec.containsKey(path(m,t,f)), path(m,t,f));
                if (kept) count++;
            }
            assertEquals(expectedCounts.get(m), count, m.key);
        }
    }
    @Test void all280DefaultGetterResultsRemainIdenticalIncludingRemovedNeutralFields() {
        for (var m : MachineKind.values()) for (var t : UpgradeType.values())
            assertArrayEquals(legacyDefaults(m,t), actual(m,t), 0, m.key + "." + t.id);
    }
    @Test void retainedPathsDefaultsRangesAndReadableLabelsStayUnchanged() {
        var spec = leaves(UpgradeConfig.SPEC.getSpec());
        for (var m : MachineKind.values()) for (var t : UpgradeType.values()) for (int f = 0; f < FIELDS.size(); f++) {
            var v = spec.get(path(m,t,f)); if (v == null) continue;
            assertEquals(EnglishConfigNames.label(FIELDS.get(f)), v.getTranslationKey());
            if (f == 5) { assertEquals(legacyDefaults(m,t)[f] == 1, v.getDefault()); assertNull(v.getRange()); }
            else {
                assertEquals(legacyDefaults(m,t)[f], ((Number)v.getDefault()).doubleValue(), 0);
                assertEquals(f == 1 || f == 6 ? 0.0 : .01, ((Number)v.getRange().getMin()).doubleValue(), 0);
                assertEquals(f == 6 ? 1000.0 : 100000.0, ((Number)v.getRange().getMax()).doubleValue(), 0);
            }
        }
    }
    @Test void obsoleteLegacyKeysAreNormallyCorrectedWithoutTouchingValidCustomValues() {
        for (var m : MachineKind.values()) for (var t : UpgradeType.values()) for (int f = 0; f < FIELDS.size(); f++)
            config.set(path(m,t,f), f == 5 ? true : 7.0);
        config.set("base_values.coal_generator_fe_per_tick", 73);
        config.set("farmer_accelerates_ripening", false);
        List<String> removed = new ArrayList<>();
        UpgradeConfig.SPEC.correct(config, (action,path,oldValue,newValue) -> {
            if (action == CorrectionAction.REMOVE) removed.add(String.join(".",path));
        });
        assertFalse(removed.isEmpty());
        assertTrue(UpgradeConfig.SPEC.isCorrect(config));
        assertFalse(config.contains("energizer.speed_upgrade_machine_1"));
        for (var m : MachineKind.values()) for (var t : UpgradeType.values()) for (int f = 0; f < FIELDS.size(); f++) {
            String key = path(m,t,f);
            if (expectedFields(m,t).indexOf("SECIOLD".charAt(f)) >= 0)
                assertEquals(f == 5 ? Boolean.TRUE : 7.0, config.get(key), key);
            else assertFalse(config.contains(key), key);
        }
        assertEquals(73, (int)config.get("base_values.coal_generator_fe_per_tick"));
        assertEquals(false, config.get("farmer_accelerates_ripening"));
        UpgradeConfig.SPEC.afterReload();
        assertEquals(7, UpgradeConfig.capacity(MachineKind.CRUSHER, UpgradeType.SPEED_3));
        assertEquals(1, UpgradeConfig.inputRate(MachineKind.CRUSHER, UpgradeType.SPEED_3));
        assertEquals(1, UpgradeConfig.energy(MachineKind.HEAT_COLLECTOR, UpgradeType.EFFICIENCY_2));
        assertEquals(1, UpgradeConfig.capacity(MachineKind.ENERGIZER, UpgradeType.SPEED_3));
        assertFalse(UpgradeConfig.preventsLava(MachineKind.CRUSHER, UpgradeType.SPEED_3));
    }
    @Test void trueOnlyEditableEffectsAndHeatSpeedLavaProtectionStillReadCustomValues() {
        config.set(path(MachineKind.CRUSHER, UpgradeType.SPEED_3, 2), 4.0);
        config.set(path(MachineKind.CRUSHER, UpgradeType.EFFICIENCY_2, 0), 1.2);
        config.set(path(MachineKind.CRUSHER, UpgradeType.EFFICIENCY_2, 3), 7.0);
        config.set(path(MachineKind.COAL_GENERATOR, UpgradeType.SPEED_3, 4), 9.0);
        config.set(path(MachineKind.LEAF_GENERATOR, UpgradeType.SPEED_3, 1), 1.25);
        config.set(path(MachineKind.LEAF_GENERATOR, UpgradeType.SPEED_3, 6), .3);
        config.set(path(MachineKind.HEAT_COLLECTOR, UpgradeType.SPEED_3, 5), true);
        UpgradeConfig.SPEC.afterReload();
        assertEquals(8, EnergyCombine.combine(List.of(UpgradeConfig.capacity(MachineKind.CRUSHER,UpgradeType.SPEED_3),
                UpgradeConfig.capacity(MachineKind.CRUSHER,UpgradeType.EFFICIENCY_2))));
        assertEquals(6, EnergyCombine.combine(List.of(UpgradeConfig.speed(MachineKind.CRUSHER,UpgradeType.SPEED_3),
                UpgradeConfig.speed(MachineKind.CRUSHER,UpgradeType.EFFICIENCY_2))), 1e-12);
        assertEquals(7, UpgradeConfig.inputRate(MachineKind.CRUSHER, UpgradeType.EFFICIENCY_2));
        assertEquals(9, UpgradeConfig.outputRate(MachineKind.COAL_GENERATOR, UpgradeType.SPEED_3));
        assertEquals(3.75, EnergyCombine.combine(List.of(UpgradeConfig.energy(MachineKind.LEAF_GENERATOR, UpgradeType.SPEED_3),
                UpgradeConfig.energy(MachineKind.LEAF_GENERATOR, UpgradeType.EFFICIENCY_2))));
        assertEquals(.3, UpgradeConfig.cooldown(MachineKind.LEAF_GENERATOR, UpgradeType.SPEED_3));
        assertTrue(UpgradeConfig.preventsLava(MachineKind.HEAT_COLLECTOR, UpgradeType.SPEED_3));
        assertFalse(UpgradeConfig.preventsLava(MachineKind.HEAT_COLLECTOR, UpgradeType.SPEED_2));
    }
    @Test void defaultCoefficientsStillFeedBothModeFormulasIdentically() {
        var m = MachineKind.CRUSHER;
        assertEquals(5, EnergyCombine.combine(List.of(UpgradeConfig.speed(m,UpgradeType.SPEED_3), UpgradeConfig.speed(m,UpgradeType.EFFICIENCY_2))));
        assertEquals(1.25, EnergyCombine.combine(List.of(UpgradeConfig.energy(m,UpgradeType.SPEED_3), UpgradeConfig.energy(m,UpgradeType.EFFICIENCY_2))));
        assertEquals(7.2, UpgradeAggregation.speed(Collections.nCopies(4,UpgradeConfig.speed(m,UpgradeType.SPEED_1))), 1e-12);
        assertEquals(3.108864, UpgradeAggregation.processorEnergy(Collections.nCopies(4,UpgradeConfig.energy(m,UpgradeType.SPEED_1)),
                Collections.nCopies(4,UpgradeConfig.energy(m,UpgradeType.EFFICIENCY_1))), 1e-12);
    }
    @Test void actualRegisteredValueTreeHasNoPhantomUiFields() {
        Set<String> paths = new TreeSet<>();
        collectValues(UpgradeConfig.SPEC.getValues(), "", paths);
        assertEquals(leaves(UpgradeConfig.SPEC.getSpec()).keySet(), paths);
        assertEquals(161, paths.size());
    }
    private static void collectValues(UnmodifiableConfig node, String prefix, Set<String> paths) {
        assertFalse(node.valueMap().isEmpty());
        node.valueMap().forEach((key,value) -> {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof UnmodifiableConfig child) collectValues(child, path, paths);
            else {
                assertNotNull(assertInstanceOf(ModConfigSpec.ConfigValue.class,value).get());
                paths.add(path);
            }
        });
    }
    @Test void startupAllTwelveDefaultsRangesAndRestartFlagsStayUnchanged() {
        var spec = leaves(UpgradeStartup.SPEC.getSpec());
        assertEquals(false, spec.get("Dependency Upgrade Plugins").getDefault());
        Map<String,Integer> counts = Map.ofEntries(
                Map.entry("crusher",5), Map.entry("crusher_double",5), Map.entry("powered_furnace",5),
                Map.entry("coal_generator",5), Map.entry("oil_generator",5), Map.entry("heat_collector",5),
                Map.entry("leaf_generator",5), Map.entry("energizer",2), Map.entry("alloy_furnace",5),
                Map.entry("casing_cross",3), Map.entry("farmer",5));
        assertEquals(12,spec.size());
        counts.forEach((machine,count) -> {
            var v = spec.get("slots." + machine); assertNotNull(v); assertEquals(count, v.getDefault());
            assertEquals(0,v.getRange().getMin()); assertEquals(10,v.getRange().getMax());
        });
        spec.values().forEach(v -> assertEquals(ModConfigSpec.RestartType.GAME,v.restartType()));
    }
    @Test void realLegacyTomlRoundTripKeepsCustomValuesAndDropsOnlyObsoletePaths() {
        var legacy = new TomlParser().parse("""
                farmer_smart_sleep = false
                [base_values]
                crusher_fe_per_tick = 91
                [crusher.speed_upgrade_machine_3]
                capacity_multiplier = 4.0
                input_rate_multiplier = 19.0
                [heat_collector.speed_upgrade_machine_3]
                prevents_lava_consumption = true
                energy_multiplier = 19.0
                [leaf_generator.speed_upgrade_machine_3]
                energy_multiplier = 1.25
                cooldown_multiplier = 0.3
                [energizer.speed_upgrade_machine_3]
                capacity_multiplier = 900.0
                """);
        UpgradeConfig.SPEC.correct(legacy);
        var roundTrip = new TomlParser().parse(new TomlWriter().writeToString(legacy));
        assertTrue(UpgradeConfig.SPEC.isCorrect(roundTrip));
        assertEquals(false, roundTrip.get("farmer_smart_sleep"));
        assertEquals(91, ((Number)roundTrip.get("base_values.crusher_fe_per_tick")).intValue());
        assertEquals(4.0, roundTrip.get("crusher.speed_upgrade_machine_3.capacity_multiplier"));
        assertEquals(true, roundTrip.get("heat_collector.speed_upgrade_machine_3.prevents_lava_consumption"));
        assertEquals(1.25, roundTrip.get("leaf_generator.speed_upgrade_machine_3.energy_multiplier"));
        assertEquals(.3, roundTrip.get("leaf_generator.speed_upgrade_machine_3.cooldown_multiplier"));
        assertFalse(roundTrip.contains("crusher.speed_upgrade_machine_3.input_rate_multiplier"));
        assertFalse(roundTrip.contains("heat_collector.speed_upgrade_machine_3.energy_multiplier"));
        assertFalse(roundTrip.contains("energizer.speed_upgrade_machine_3"));
    }
}
