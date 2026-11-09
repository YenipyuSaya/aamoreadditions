package com.aamoreadditions.effect;

import com.aamoreadditions.farmer.FarmerRipeningMath;
import com.aamoreadditions.upgrade.*;
import com.electronwill.nightconfig.core.CommentedConfig;
import net.neoforged.fml.config.InMemoryConfigFixture;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the same runtime adapters used by Farmer work/Info and Cross work/menu, without a game launch. */
class SpecialMachineIsolationTest {
    CommentedConfig config;
    @BeforeEach void load() {
        config=CommentedConfig.inMemory(); UpgradeConfig.SPEC.correct(config);
        UpgradeConfig.SPEC.acceptConfig(InMemoryConfigFixture.of(config)); mode(false);
    }
    @AfterEach void unload() { UpgradeConfig.SPEC.acceptConfig(null); UpgradeStartup.resetClient(); }
    private void mode(boolean dependency) { UpgradeStartup.remote(new UpgradeStartup.Snapshot(dependency, UpgradeStartup.local().slots())); }
    private void customize(String path,Object value) { config.set(path,value); UpgradeConfig.SPEC.afterReload(); }
    @Test void crossRegistersExactlyThreeEnglishSpeedFieldsWithOriginalDefaultsAndRange() {
        var category=(com.electronwill.nightconfig.core.UnmodifiableConfig)UpgradeConfig.SPEC.getSpec().get("casing_cross");
        assertEquals(3,category.valueMap().size());
        for(var t:List.of(UpgradeType.SPEED_1,UpgradeType.SPEED_2,UpgradeType.SPEED_3)) {
            var node=(com.electronwill.nightconfig.core.UnmodifiableConfig)category.get(t.id);
            assertEquals(Set.of("speed_multiplier"),node.valueMap().keySet());
            var field=(net.neoforged.neoforge.common.ModConfigSpec.ValueSpec)node.get("speed_multiplier");
            assertEquals(CrossUpgradeMath.defaultSpeed(t.tier),field.getDefault());
            assertEquals(.01,field.getRange().getMin()); assertEquals(100000.0,field.getRange().getMax());
            assertEquals("Speed Multiplier",field.getTranslationKey());
            assertEquals(net.neoforged.neoforge.common.ModConfigSpec.RestartType.NONE,field.restartType());
        }
    }
    @Test void realAdaptersDoNotReadAnotherMachinesEditableParametersInEitherMode() {
        var all=List.of(UpgradeType.SPEED_1,UpgradeType.SPEED_2,UpgradeType.SPEED_3,UpgradeType.EFFICIENCY_1,UpgradeType.EFFICIENCY_2);
        for(boolean dependency:new boolean[]{false,true}) {
            mode(dependency); var farmer=FarmerEffects.evaluate(all); double cross=CrossEffects.speed(all);
            for(var t:UpgradeType.values()) {
                customize("powered_furnace."+t.id+".speed_multiplier",99.0);
                customize("powered_furnace."+t.id+".energy_multiplier",7.0);
                customize("powered_furnace."+t.id+".capacity_multiplier",900.0);
            }
            assertEquals(farmer,FarmerEffects.evaluate(all)); assertEquals(cross,CrossEffects.speed(all));
            customize("casing_cross.speed_upgrade_machine_3.speed_multiplier",4.2);
            assertEquals(farmer,FarmerEffects.evaluate(all));
            assertEquals(dependency?4.2:7.7,CrossEffects.speed(all),1e-12);
            customize("casing_cross.speed_upgrade_machine_3.speed_multiplier",3.0);
        }
    }
    @Test void crossUsesCustomHighestInTrueAndMixedTierSumWithSingleBoundaryInFalse() {
        customize("casing_cross.speed_upgrade_machine_1.speed_multiplier",1.7);
        customize("casing_cross.speed_upgrade_machine_2.speed_multiplier",2.4);
        customize("casing_cross.speed_upgrade_machine_3.speed_multiplier",4.2);
        var mixed=List.of(UpgradeType.SPEED_1,UpgradeType.SPEED_2,UpgradeType.SPEED_3,UpgradeType.SPEED_1);
        mode(false); assertEquals(9,CrossEffects.speed(mixed),1e-12);
        mode(true); assertEquals(4.2,CrossEffects.speed(mixed.subList(0,3)));
    }
    @Test void farmerExamplesRunThroughRuntimeAdapterAndRingPolicy() {
        var fiveSpeed=Collections.nCopies(5,UpgradeType.SPEED_1);
        var speed=FarmerEffects.evaluate(fiveSpeed);
        assertEquals(10,speed.speed()); assertEquals(1,speed.cooldown()); assertEquals(9,speed.energy());
        assertEquals(1,speed.capacity()); assertEquals(0,speed.fortune());
        assertEquals(2250,FarmerUpgradeMath.operationCost(250,speed.energy()));
        assertEquals(3150,FarmerUpgradeMath.operationCost(350,speed.energy()));
        assertEquals(4500,FarmerUpgradeMath.operationCost(500,speed.energy()));
        assertEquals(6300,FarmerUpgradeMath.plantingReserve(350,speed.energy()));
        var ring=FarmerRipeningMath.evaluate(fiveSpeed.stream().map(t->new UpgradeLogic.Plugin(t.line.name(),t.tier)).toList(),false);
        assertEquals(10,ring.period()); assertEquals(27,ring.draws());
        var efficiency=FarmerEffects.evaluate(Collections.nCopies(5,UpgradeType.EFFICIENCY_1));
        assertEquals(1,efficiency.speed()); assertEquals(6,efficiency.cooldown());
        assertEquals(.425984,efficiency.energy(),1e-12); assertEquals(7.5,efficiency.capacity()); assertEquals(3,efficiency.fortune());
        assertEquals(106,FarmerUpgradeMath.operationCost(250,efficiency.energy()));
        assertEquals(149,FarmerUpgradeMath.operationCost(350,efficiency.energy()));
        assertEquals(213,FarmerUpgradeMath.operationCost(500,efficiency.energy()));
        assertEquals(298,FarmerUpgradeMath.plantingReserve(350,efficiency.energy()));
    }
    @Test void farmerTrueUsesHighestWithoutIndependentPenaltiesOrFortuneQuantity() {
        mode(true);
        var active=FarmerEffects.evaluate(List.of(UpgradeType.SPEED_1,UpgradeType.SPEED_2,UpgradeType.SPEED_3,UpgradeType.EFFICIENCY_1,UpgradeType.EFFICIENCY_2));
        assertEquals(5,active.speed()); assertEquals(1,active.cooldown()); assertEquals(1.25,active.energy());
        assertEquals(2,active.capacity()); assertEquals(3,active.fortune());
        assertEquals(1,FarmerEffects.evaluate(List.of(UpgradeType.EFFICIENCY_1)).fortune());
        assertEquals(new FarmerRipeningMath.Plan(10,34),FarmerRipeningMath.evaluate(List.of(
                new UpgradeLogic.Plugin("SPEED",1),new UpgradeLogic.Plugin("SPEED",2),new UpgradeLogic.Plugin("SPEED",3)),true));
    }
    @Test void zeroToTenCrossPluginsUseSharedLayerCountAndCap() {
        mode(false); assertEquals(1,CrossEffects.speed(List.of()));
        for(int n=1;n<=10;n++) {
            double loss=1-.1*Math.min(3,Math.max(0,n-3));
            assertEquals(1.5*n*loss,CrossEffects.speed(Collections.nCopies(n,UpgradeType.SPEED_1)),1e-12);
        }
        assertEquals(7.2,CrossEffects.speed(List.of(UpgradeType.SPEED_1,UpgradeType.SPEED_2,UpgradeType.SPEED_3,UpgradeType.SPEED_1)),1e-12);
    }
    @Test void crossFallbackBeforeConfigLoadRetainsOriginalCoefficients() {
        UpgradeConfig.SPEC.acceptConfig(null);
        assertEquals(1.5,UpgradeConfig.crossSpeed(1)); assertEquals(2,UpgradeConfig.crossSpeed(2)); assertEquals(3,UpgradeConfig.crossSpeed(3));
        assertEquals(1,UpgradeConfig.crossSpeed(0)); assertEquals(1,UpgradeConfig.crossSpeed(4));
    }
}
