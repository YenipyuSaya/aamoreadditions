package com.aamoreadditions.upgrade;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Source-contract checks, not runtime Mixin/loot/game integration tests. */
class FarmerRuntimeWiringTest {
    static String source(String path)throws Exception{return UpgradeWiringTest.source(path);}
    @Test void allFiveCostLocalsAreScaledBeforeTheirExistingChecks()throws Exception {
        var s=source("mixin/FarmerBehaviorCostMixin.java");
        for(String name:List.of("Default","Cactus","Reed","NetherWart","MelonPumpkin"))assertTrue(s.contains(name+"FarmerBehavior.class"));
        assertTrue(s.contains("@ModifyVariable(method=\"tryPlantSeed\",at=@At(\"STORE\"),ordinal=0)"));
        assertTrue(s.contains("@ModifyVariable(method=\"tryHarvestPlant\",at=@At(\"STORE\"),ordinal=0)"));
        assertEquals(2,s.split("FarmerEffects.cost",-1).length-1);
        var special=source("mixin/FarmerDefaultHarvestMixin.java");
        assertTrue(special.contains("IFarmer;extractEnergy(I)V"));
        assertTrue(special.contains("original.call(farmer,FarmerEffects.cost(farmer,amount))"));
        assertFalse(s.contains("getEnergy()")); // Never fake the player's stored FE to bypass a gate.
    }
    @Test void originalHarvestDropsAndChargesUseTheSameLivePolicy()throws Exception {
        for(String name:List.of("FarmerHarvestMixin.java","FarmerDefaultHarvestMixin.java")) {
            var s=source("mixin/"+name);
            assertTrue(s.contains("BlockState;getDrops(Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;"));
            assertTrue(s.contains("@Local(argsOnly=true) IFarmer farmer"));
            assertTrue(s.contains("FarmerEffects.harvest(state,context,farmer,builder->original.call(state,builder))"));
        }
        var effects=source("effect/FarmerEffects.java");
        assertTrue(effects.contains("UpgradeSlots.get(tile).activeUpgrades()"));
        assertTrue(effects.contains("UpgradeSlots.get(tile).legal() ? evaluate(tile).fortune() : 0"));
        assertTrue(effects.contains("FarmerUpgradeMath.operationCost(original,evaluate(tile).energy())"));
    }
    @Test void periodHookRunsInsideExistingEnergyAndRedstoneBranchAndIllegalResetUsesSharedInterface()throws Exception {
        var s=source("mixin/TileEntityFarmerMixin.java");
        assertTrue(s.contains("opcode=Opcodes.GETFIELD,ordinal=0"));
        assertTrue(s.contains("FarmerUpgradeMath.armCounter(remaining,period)"));
        assertTrue(s.contains("implements FarmerWorkState"));assertTrue(s.contains("resetUpgradeWork(){aamoreadditions$resetFarmerScan();}"));
        assertTrue(source("upgrade/FarmerWorkState.java").contains("extends UpgradeResetState"));
        assertFalse(s.contains("checkBehaviors(")); // original scan/body, planting and hook flow stay intact.
    }
    @Test void storageCapacityAndInfoShareActualFarmerPolicyAndNoPendingStubRemains()throws Exception {
        var effects=source("effect/UpgradeEffects.java");
        assertTrue(effects.contains("return FarmerEffects.evaluate(active).capacity()"));
        assertTrue(effects.contains("TileEntityFarmer t) return t.storage"));
        assertFalse(source("upgrade/UpgradeSlots.java").contains("effectsPending"));
        assertFalse(source("client/UpgradeInfo.java").contains("effects_pending"));
        var mixins=Files.readString(Path.of("src/main/resources/aamoreadditions.mixins.json"));
        for(String name:List.of("TileEntityFarmerMixin","FarmerBehaviorCostMixin","FarmerHarvestMixin","FarmerDefaultHarvestMixin"))assertTrue(mixins.contains(name));
    }
    @Test void commonFarmerRuntimeHasNoClientOrOptionalJadeReferences()throws Exception {
        for(String name:List.of("effect/FarmerEffects.java","effect/FarmerHarvestLoot.java","mixin/TileEntityFarmerMixin.java","mixin/FarmerBehaviorCostMixin.java","mixin/FarmerHarvestMixin.java","mixin/FarmerDefaultHarvestMixin.java")) {
            var s=source(name);assertFalse(s.contains("net.minecraft.client"));assertFalse(s.contains("snownee.jade"));
        }
    }
    @Test void illegalMachinesCannotBypassStopThroughBehaviorOrDefaultPlantHelper()throws Exception {
        assertTrue(source("effect/FarmerEffects.java").contains("!com.aamoreadditions.upgrade.UpgradeRuntime.blocked(tile)"));
        var behavior=source("mixin/FarmerBehaviorCostMixin.java");
        assertEquals(2,behavior.split(java.util.regex.Pattern.quote("if (!FarmerEffects.canWork(farmer))"),-1).length-1);
        assertTrue(behavior.contains("FarmerResult.STOP_PROCESSING"));
        var helper=source("mixin/FarmerDefaultHarvestMixin.java");
        assertTrue(helper.contains("@Inject(method=\"defaultPlant\",at=@At(\"HEAD\"),cancellable=true)"));
        assertTrue(helper.contains("if (!FarmerEffects.canWork(farmer)) cir.setReturnValue(false)"));
    }

    @Test void firstNbtReadDefersCapacityClampingUntilSavedPluginsAreRestored()throws Exception {
        var s=source("upgrade/UpgradeSlots.java");
        assertTrue(s.contains("new Handler(holder, false)"));
        assertTrue(s.contains("handler.deserializeNBT(provider, tag)"));
        assertTrue(s.contains("public Handler(Object holder) { this(holder, true); }"));
        assertTrue(s.contains("if (initializeEffects) refresh()"));
        var load=s.substring(s.indexOf("public void deserializeNBT"),s.indexOf("public void replaceContents"));
        assertTrue(load.indexOf("super.deserializeNBT")<load.indexOf("refresh()"));
        assertTrue(load.indexOf("stacks.set(i, resized.kept().get(i))")<load.indexOf("refresh()"));
    }

}
