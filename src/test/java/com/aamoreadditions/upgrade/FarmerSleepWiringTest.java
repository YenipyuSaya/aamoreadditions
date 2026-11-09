package com.aamoreadditions.upgrade;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source contracts supplement pure state tests; not live Minecraft/Mixin tests. */
class FarmerSleepWiringTest {
    static String source(String path)throws Exception{return UpgradeWiringTest.source(path);}
    @Test void configIsReadableEnglishDefaultTrueAndUsesExistingLiveServerSpec()throws Exception {
        var s=source("effect/UpgradeConfig.java");
        assertTrue(s.contains(".define(\"farmer_smart_sleep\", true)"));
        assertTrue(s.contains("new com.aamoreadditions.config.EnglishConfigBuilder()"));
        assertTrue(s.contains("public static boolean farmerSmartSleep(){return !SPEC.isLoaded() || FARMER_SMART_SLEEP.get();}"));
        var field=s.substring(s.indexOf("FARMER_SMART_SLEEP = b.comment"),s.indexOf("for (MachineKind m : MachineKind.values())"));
        assertFalse(field.contains("gameRestart"));
        assertTrue(source("AAMoreAdditions.java").contains("ModConfig.Type.SERVER, com.aamoreadditions.effect.UpgradeConfig.SPEC"));
    }
    @Test void cheapFilteringIsIndependentOfSleepSwitchAndDoesNotSkipPlanting()throws Exception {
        var s=source("mixin/TileEntityFarmerMixin.java");
        assertTrue(s.contains("IFarmerBehavior;tryHarvestPlant"));
        assertTrue(s.contains("if(!possible && !FarmerHarvestPrefilter.preserveStop(behavior,level,pos))return FarmerResult.FAIL"));
        assertTrue(s.contains("return original.call(behavior,level,pos,farmer)"));
        assertTrue(s.contains("IFarmerBehavior;tryPlantSeed"));
        var planting=s.substring(s.indexOf("private FarmerResult aamoreadditions$plantingWork"),s.indexOf("/** Observe the original final output-fit result"));
        assertTrue(planting.contains("original.call(behavior,seed,level,pos,farmer)"));
        assertTrue(planting.contains("if(result==FarmerResult.SUCCESS)aamoreadditions$planted=true"));
        assertFalse(planting.contains("FarmerHarvestPrefilter"));
        var filter=source("effect/FarmerHarvestPrefilter.java");
        assertFalse(filter.contains("farmerSmartSleep"));assertFalse(filter.contains("getDrops"));
        assertTrue(filter.contains("if(!known(behavior))return true"));
        assertTrue(filter.contains("type==CactusFarmerBehavior.class"));assertTrue(filter.contains("type==ReedFarmerBehavior.class"));
    }
    @Test void maturityChecksMatchBuiltInsAndOnlyUseLocalBlockStates()throws Exception {
        var s=source("effect/FarmerHarvestPrefilter.java");
        assertTrue(s.contains("crop.isMaxAge(state)"));assertTrue(s.contains("!(block instanceof StemBlock)"));
        assertTrue(s.contains("BlockStateProperties.AGE_3)>=3"));
        assertTrue(s.contains("block==Blocks.MELON || block==Blocks.PUMPKIN"));
        assertTrue(s.contains("pos.above(2)"));assertTrue(s.contains("instanceof SugarCaneBlock"));
        assertFalse(s.contains("canAddToOutput"));assertFalse(s.contains("getEnergy"));
    }
    @Test void onlyActualCellReturnsAdvanceAccountingAndUnknownBehaviorsPreventSleep()throws Exception {
        var s=source("mixin/TileEntityFarmerMixin.java");
        assertTrue(s.contains("@Inject(method=\"checkBehaviors\",at=@At(\"RETURN\"))"));
        assertTrue(s.contains("SORTED_FARMER_BEHAVIORS"));
        var runtime=source("effect/FarmerScanRuntime.java");
        assertTrue(runtime.contains("behaviors.stream().anyMatch(b->!FarmerHarvestPrefilter.known(b))"));
        assertTrue(runtime.contains("scan.observe(index,candidate,storageBlocked,harvestFits,unknown)"));
        var prepare=runtime.substring(runtime.indexOf("public static void prepare"),runtime.indexOf("public static void checked"));
        assertFalse(prepare.contains("scan.observe"));assertFalse(prepare.contains("getBlockState(pos"));
        assertTrue(runtime.contains("z*side+x"));assertTrue(runtime.contains("radius+1"));
    }
    @Test void existingAaSyncCarriesTheRealServerSleepFlagAndInfoStillHasThreeRows()throws Exception {
        var s=source("mixin/TileEntityFarmerMixin.java");
        assertTrue(s.contains("writeSyncableNBT"));assertTrue(s.contains("TileEntityBase.NBTType.SYNC)tag.putBoolean"));
        assertTrue(s.contains("syncedSleeping(tag.getBoolean(AAMORE_SLEEP))"));
        assertTrue(s.contains("else {aamoreadditions$scan.loaded();waitTime=0;}"));
        var info=source("client/UpgradeInfo.java");
        assertTrue(info.contains("String.valueOf(com.aamoreadditions.effect.FarmerScanRuntime.cooldown(tile))"));
        assertTrue(info.contains("withStyle(ChatFormatting.AQUA)"));assertTrue(info.contains("withStyle(ChatFormatting.GREEN)"));
        var runtime=source("effect/FarmerScanRuntime.java");
        assertTrue(runtime.contains("if(old!=scan.sleeping())tile.sendUpdate()"));
        assertTrue(runtime.contains("?FarmerScanState.SLOW_TICKS:FarmerEffects.evaluate(tile).cooldown()"));
    }
    @Test void storageResultComesFromOriginalOutputCheckOnlyDuringHarvest()throws Exception {
        var s=source("mixin/TileEntityFarmerMixin.java");
        assertTrue(s.contains("@Inject(method=\"canAddToOutput\",at=@At(\"RETURN\"))"));
        assertTrue(s.contains("if(!aamoreadditions$harvesting)return"));
        assertTrue(s.contains("if(cir.getReturnValue())aamoreadditions$harvestFits=true"));
        assertTrue(s.contains("else aamoreadditions$storageBlocked=true"));
        assertTrue(s.contains("finally {aamoreadditions$harvesting=false;}"));
        assertFalse(s.contains("getDrops("));assertFalse(s.contains("getSlots("));
    }
    @Test void upgradesGeometryAndDisableResetWhileNoDropsOrAreaCacheWasAdded()throws Exception {
        var s=source("upgrade/UpgradeSlots.java");
        assertTrue(s.contains("((FarmerWorkState)be).aamoreadditions$resetFarmerScan()"));
        var runtime=source("effect/FarmerScanRuntime.java");
        assertTrue(runtime.contains("side%2==0?side+1:side"));
        assertTrue(runtime.contains("UpgradeConfig.farmerSmartSleep()"));
        assertTrue(runtime.contains("work.aamoreadditions$setFarmerWait(0)"));
        assertFalse(runtime.contains("getDrops"));assertFalse(runtime.contains("for(int x"));
        assertFalse(runtime.contains("net.minecraft.client"));
    }
}
