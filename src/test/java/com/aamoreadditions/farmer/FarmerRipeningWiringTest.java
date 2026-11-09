package com.aamoreadditions.farmer;

import com.aamoreadditions.config.EnglishConfigNames;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** Static integration contracts, not a running Minecraft/NeoForge serialization test. */
class FarmerRipeningWiringTest {
    static String source(String name)throws Exception{return Files.readString(Path.of("src/main/java/com/aamoreadditions").resolve(name));}
    @Test void configIsLiveReadableExactEnglishAndDefaultsTrue()throws Exception {
        var s=source("effect/UpgradeConfig.java");
        assertTrue(s.contains(".define(\"farmer_accelerates_ripening\", true)"));
        assertTrue(s.contains("return !SPEC.isLoaded() || FARMER_ACCELERATES_RIPENING.get()"));
        assertEquals("Farmer accelerates ripening",EnglishConfigNames.label("farmer_accelerates_ripening"));
    }
    @Test void dedicatedInventoryChecksItemIdentityNotEnergyOrComponentEquality()throws Exception {
        var s=source("farmer/FarmerRingInventory.java");
        assertTrue(s.contains("super(1)"));assertTrue(s.contains("stack.is(ActuallyItems.RING_OF_GROWTH.get())"));
        assertTrue(s.contains("getSlotLimit(int slot){return 1;}"));
        assertFalse(s.contains("isSameItemSameComponents"));assertFalse(s.contains("getEnergyStored()"));
        assertFalse(s.contains("registerBlock"));assertFalse(s.contains("Capabilities.ItemHandler"));
    }
    @Test void savedAndSyncedRingUsesFullComponentAwareStackSerializationAndStableHandler()throws Exception {
        var s=source("farmer/FarmerRingInventory.java");
        assertTrue(s.contains("tag.put(KEY,serializeNBT(provider))"));
        assertTrue(s.contains("deserializeNBT(provider,tag.getCompound(KEY))"));
        assertTrue(s.contains("loading=true"));assertTrue(s.contains("finally {loading=false;}"));
        var mix=source("mixin/TileEntityFarmerMixin.java");
        assertTrue(mix.contains("if(aamoreadditions$ring==null)"));
        assertTrue(mix.contains("type==TileEntityBase.NBTType.SAVE_TILE || type==TileEntityBase.NBTType.SYNC"));
        assertTrue(mix.contains("aamoreadditions$ringInventory().write(tag,provider"));
        assertTrue(mix.contains("aamoreadditions$ringInventory().read(tag,provider"));
    }
    @Test void disabledConfigHidesLocksAndPreservesRingInsteadOfDeletingIt()throws Exception {
        var slot=source("farmer/FarmerRingSlot.java");
        assertTrue(slot.contains("isActive(){return ring.enabled();}"));
        assertTrue(slot.contains("isActive() && FarmerRingInventory.isRing(stack)"));
        assertTrue(slot.contains("isActive() && super.mayPickup(player)"));
        var runtime=source("farmer/FarmerRipeningRuntime.java");
        assertTrue(runtime.contains("if(!ring.enabled() || !ring.hasRing()){ring.clock.reset();return;}"));
        assertFalse(runtime.contains("setStackInSlot"));
    }
    @Test void menuAppendsWithoutExpandingOriginalTwelveAutomationSlotsAndShiftWorksWithClosedStrip()throws Exception {
        var menu=source("mixin/ContainerFarmerMixin.java");
        assertTrue(menu.indexOf("UpgradeMenus.addSlots")<menu.indexOf("FarmerRingMenus.add"));
        var m=source("farmer/FarmerRingMenus.java");
        assertTrue(m.contains("source.container instanceof Inventory"));assertTrue(m.contains("slot instanceof FarmerRingSlot"));
        assertFalse(m.contains("isUpgradeOpen"));assertFalse(m.contains("tile.inv"));
        var quick=source("mixin/UpgradeQuickMoveMixin.java");
        assertTrue(quick.indexOf("FarmerRingMenus.quickMove")<quick.indexOf("UpgradeMenus.quickMove"));
    }
    @Test void breakDropsStoredRingEvenWhileConfigIsOffAndDetachesBeforeSpawning()throws Exception {
        var s=source("farmer/FarmerRingMenus.java");
        int drop=s.indexOf("public static void drop");s=s.substring(drop);
        assertFalse(s.contains("enabled()"));
        assertTrue(s.indexOf("ring.setStackInSlot(0,ItemStack.EMPTY)")<s.indexOf("Containers.dropItemStack"));
        assertTrue(s.contains("getStackInSlot(0).copy()"));
        assertTrue(source("mixin/BlockContainerBaseMixin.java").contains("FarmerRingMenus.drop(level,farmer)"));
    }
    @Test void nativeScanThenRingLoopThenChargingAndIllegalGuardPreparesEvenWhenCancelling()throws Exception {
        var mix=source("mixin/TileEntityFarmerMixin.java");
        assertTrue(mix.contains("at=@At(\"TAIL\"))\n    private static void aamoreadditions$ripeningAfterNative"));
        var runtime=source("farmer/FarmerRipeningRuntime.java");
        assertTrue(runtime.indexOf("FarmerRipeningMath.run")<runtime.indexOf("FarmerRingCharging.charge"));
        assertTrue(runtime.indexOf("UpgradeRuntime.blocked(tile)")<runtime.indexOf("FarmerRingCharging.charge"));
        assertTrue(runtime.contains("!tile.isRedstonePowered && tile.storage.getEnergyStored()>0"));
        assertTrue(runtime.contains("FarmerRingCharging.charge(ring.getStackInSlot(0),tile.storage.getEnergyStored())"));
        assertTrue(runtime.contains("tile.storage.extractEnergyInternal(received,false)"));
        var guard=source("mixin/UpgradeMachineTickMixin.java");
        assertTrue(guard.indexOf("FarmerRipeningRuntime.prepare")<guard.indexOf("UpgradeRuntime.tick"));
    }
    @Test void ripeningHasNoDependencyOnHarvestSleepOrBookkeeping()throws Exception {
        for(var file:new String[]{"farmer/FarmerRipeningRuntime.java","farmer/FarmerRingInventory.java"}) {
            var s=source(file);assertFalse(s.contains("sleeping()"));assertFalse(s.contains("farmerSmartSleep"));
            assertFalse(s.contains("resetFarmerScan"));assertFalse(s.contains("checkBehaviors"));
        }
    }
    @Test void candidateCollectionUsesFarmerPlaneOncePerCycleWithReplacementAndNoChunkLoads()throws Exception {
        var s=source("farmer/FarmerRipeningRuntime.java");
        assertTrue(s.contains("FarmerScanRuntime.side(tile)"));assertTrue(s.contains("side/2+1"));
        assertTrue(s.contains("center.offset(x,0,z)"));assertTrue(s.contains("level.hasChunkAt(pos.offset(x,0,z))"));
        assertTrue(s.indexOf("ring.clock.step")<s.indexOf("new ArrayList<BlockPos>"));
        assertTrue(s.contains("level.random::nextInt"));assertTrue(s.contains("var state=level.getBlockState(pos)"));
        assertTrue(s.contains("state.randomTick(level,pos,level.random)"));assertFalse(s.contains("getChunk("));
        assertFalse(s.contains("getDrops"));assertFalse(s.contains("targets.remove"));
    }
    @Test void cropFilterExcludesSoilAttachedAndOrdinaryMatureButAllowsUnattachedAgeSevenStem()throws Exception {
        var s=source("farmer/FarmerRipeningRuntime.java");
        assertTrue(s.contains("block instanceof AttachedStemBlock"));assertTrue(s.contains("block instanceof GrassBlock"));
        assertTrue(s.contains("state.is(Blocks.PODZOL)"));assertTrue(s.contains("state.is(Blocks.MYCELIUM)"));
        assertTrue(s.contains("!crop.isMaxAge(state)"));assertTrue(s.contains("if(block instanceof StemBlock)return true"));
        assertFalse(s.contains("instanceof BonemealableBlock"));
    }
    @Test void guiMatchesFinalArrowSevenLayoutAndGhostIsUnderRealItems()throws Exception {
        var s=source("client/FarmerRipeningGui.java");
        assertTrue(s.contains("left+67,top+75,0,0,16,16,16,16"));
        assertTrue(s.contains("RenderSystem.setShaderColor(1,1,1,.35f)"));
        assertTrue(s.contains("textures/item/ring_of_growth.png"));assertTrue(s.contains("left+52,top+81,left+62,top+84"));
        assertTrue(s.contains("top+79+i,left+62+i,top+86-i"));
        assertTrue(s.contains("AssetUtil.getWheelColor"));assertTrue(s.contains("arrowActive(ring.hasRing(),tile.storage.getEnergyStored())"));
        assertFalse(s.contains("isRedstonePowered"));assertFalse(s.contains("cap.getEnergyStored"));
        var mixins=Files.readString(Path.of("src/main/resources/aamoreadditions.mixins.json"));
        assertTrue(mixins.indexOf("GuiFarmerRipeningMixin")>mixins.indexOf("\"client\""));
        var image=javax.imageio.ImageIO.read(Path.of("src/main/resources/assets/aamoreadditions/textures/gui/farmer_ring_slot.png").toFile());
        assertEquals(16,image.getWidth());assertEquals(16,image.getHeight());
    }
}
