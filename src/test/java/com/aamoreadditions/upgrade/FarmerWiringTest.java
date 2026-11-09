package com.aamoreadditions.upgrade;

import com.aamoreadditions.config.EnglishConfigNames;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Source contracts plus pure planner/migration checks, NOT a running Farmer/menu/game test. */
class FarmerWiringTest {
    static String source(String relative) throws Exception { return UpgradeWiringTest.source(relative); }

    @Test void farmerConfigIsAppendedWithoutReindexingExistingMachines() throws Exception {
        var s=source("upgrade/UpgradeStartup.java");
        assertTrue(s.contains("\"energizer\",\"alloy_furnace\",\"casing_cross\",\"farmer\""));
        assertTrue(s.contains("List.of(5,5,5,5,5,5,5,2,5,3,5)"));
        assertTrue(s.contains("TileEntityFarmer) key = \"farmer\""));
        assertTrue(s.contains("SLOTS.add(b.gameRestart().defineInRange(KEYS.get(i), local.slots().get(i), 0, 10))"));
        assertEquals("Farmer",EnglishConfigNames.label("farmer"));
    }
    @Test void policyPacketBoundTracksAllKeysAndRejectsOldProtocol() throws Exception {
        var s=source("network/ModNetwork.java");
        assertTrue(s.contains("ByteBufCodecs.list(com.aamoreadditions.upgrade.UpgradeStartup.KEYS.size())"));
        assertTrue(s.contains("event.registrar(\"3\").playToClient(UpgradePolicyPayload.TYPE"));
        assertFalse(s.contains("ByteBufCodecs.list(10)"));
        assertTrue(source("upgrade/UpgradeSession.java").contains("snapshot.slots()"));
    }
    @Test void farmerMenuAppendsSharedSlotsAtConstructorTailAndHandlesQuickMove() throws Exception {
        var s=source("mixin/ContainerFarmerMixin.java");
        assertTrue(s.contains("@Mixin(value = ContainerFarmer.class, remap = false)"));
        assertTrue(s.contains("TileEntityFarmer;)V"));assertTrue(s.contains("at = @At(\"TAIL\")"));
        assertTrue(s.contains("UpgradeMenus.addSlots((AbstractContainerMenu) (Object) this, tile)"));
        assertTrue(source("mixin/UpgradeQuickMoveMixin.java").contains("ContainerFarmer.class"));
        assertTrue(Files.readString(Path.of("src/main/resources/aamoreadditions.mixins.json")).contains("\"ContainerFarmerMixin\""));
    }
    @Test void farmerAloneUsesRightTopToggleWithInfoOnItsLeft() throws Exception {
        var s=source("client/UpgradeStrip.java");
        assertTrue(s.contains("GuiFarmer) return 134"));
        assertTrue(s.contains("? buttonX(screen) - UpgradeToggleButton.SIZE - 1 : buttonX(screen)"));
        assertTrue(s.contains("? BTN_Y : BTN_Y + UpgradeToggleButton.SIZE + 1"));
        assertTrue(s.contains("new UpgradeInfoButton(screen.getGuiLeft() + infoX(screen), screen.getGuiTop() + infoY(screen)"));
        assertTrue(source("client/UpgradeToggleButton.java").contains("SIZE = 10"));
        // Verified AA geometry: energy (33,6,18,85), first slot y21, title y-10.
        int toggleX=134, infoX=toggleX-10-1, y=4;
        assertEquals(123,infoX);assertTrue(infoX+10<toggleX);
        assertTrue(toggleX+10<147);assertTrue(infoX>33+18);
        assertTrue(y+10<21);assertTrue(y>-10+9);
    }
    @Test void zeroSlotsReuseTheExistingNoButtonsNoPanelGate() throws Exception {
        var s=source("client/UpgradeStrip.java");
        assertTrue(s.contains("!UpgradeMenus.hasUpgradeSlots(screen.getMenu())) return"));
        assertTrue(source("upgrade/UpgradeMenus.java").contains("for (int i = 0; i < h.getSlots(); i++)"));
        assertTrue(source("mixin/AbstractContainerScreenMixin.java").contains("if (UpgradeMenus.hasUpgradeSlots(menu)) topPos"));
    }
    @Test void farmerAcceptsKnownUpgradeTypesViaTheSharedEntryPoints() throws Exception {
        var slots=source("upgrade/UpgradeSlots.java");
        assertTrue(slots.contains("|| be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer"));
        assertTrue(slots.contains("return holder instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer"));
        assertTrue(slots.contains("if (isFarmer(holder)) return t != null"));
        assertFalse(slots.contains("effectsPending"));
        assertTrue(source("upgrade/UpgradeRules.java").contains("!UpgradeSlots.allows(h.holder(), UpgradeType.of(stack))"));
        assertTrue(source("upgrade/UpgradeSlot.java").contains("UpgradeRules.canInstall(handler, stack, getSlotIndex())"));
        assertTrue(source("upgrade/UpgradeInteraction.java").contains("UpgradeRules.canInstall(h, held, -1)"));
        assertTrue(source("item/UpgradeWrenchItem.java").contains("handler.isItemValid(0, new ItemStack(i))"));
    }
    @Test void infoShowsOnlyThreeLiveRowsWithCorrectColorsAndNoFarmerBoundarySection() throws Exception {
        var s=source("client/UpgradeInfo.java");
        var farmer=s.substring(s.indexOf("if (be instanceof TileEntityFarmer tile)"),s.indexOf("var lines = new java.util.ArrayList"));
        for (String field:List.of("cooldown","energy","fortune"))assertTrue(farmer.contains("info.farmer."+field));
        assertTrue(farmer.contains("aqua(effects.energy())"));assertTrue(farmer.contains("withStyle(ChatFormatting.AQUA)"));
        assertTrue(farmer.contains("withStyle(ChatFormatting.GREEN)"));
        assertFalse(farmer.contains("info.slots"));assertFalse(farmer.contains("effects_pending"));
        assertFalse(farmer.contains("appendBoundary"));
        assertTrue(s.contains("if (com.aamoreadditions.upgrade.UpgradeSlots.isFarmer(h.holder())) return"));
        assertTrue(s.contains("if (machine == null) return"));
    }
    @Test void farmerUsesSharedDefaultsWithoutCreatingAnotherEffectConfigNamespace() throws Exception {
        assertFalse(source("effect/MachineKind.java").contains("TileEntityFarmer"));
        assertTrue(source("effect/UpgradeEffects.java").contains("TileEntityFarmer t) return t.storage"));
        assertFalse(source("effect/UpgradeConfig.java").contains("TileEntityFarmer"));
        assertTrue(source("effect/UpgradeConfig.java").contains("ProcessorUpgradeDefaults.speed(t.tier)"));
        assertTrue(source("effect/FarmerUpgradeMath.java").contains("ProcessorUpgradeDefaults.speed(plugin.tier())"));
        assertTrue(source("mixin/UpgradeMachineTickMixin.java").contains("TileEntityFarmer.class"));
        assertTrue(source("mixin/UpgradeMachineTickMixin.java").contains("UpgradeRuntime.tick(be)"));
    }
    @Test void farmerUsesSharedPersistenceSyncAndChunkSafeOverflowDropLifecycle() throws Exception {
        assertTrue(source("mixin/BlockEntitySetLevelMixin.java").contains("UpgradeSlots.supports(be)"));
        assertTrue(source("mixin/BlockContainerBaseMixin.java").contains("UpgradeSlots.drop(level, be)"));
        var slots=source("upgrade/UpgradeSlots.java");
        assertTrue(slots.contains("holder.hasData(UPGRADE_INV.get()) ? holder.getData(UPGRADE_INV.get()) : new Handler(holder, false)"));
        assertTrue(slots.contains("aa.sendUpdate()"));assertTrue(slots.contains("PendingUpgradeDrops"));
        assertTrue(slots.contains("if (level.addFreshEntity(entity)) { it.remove(); dirty(); }"));
        assertTrue(source("upgrade/UpgradeRuntime.java").contains("upgrades.drainOverflow(level, be.getBlockPos())"));
    }
    @Test void jadeAndWrenchUseTheSameActiveAndAcceptancePolicy() throws Exception {
        var jade=source("compat/jade/UpgradeJadeProvider.java");
        assertTrue(jade.contains("UpgradeSlots.supports(be)"));assertTrue(jade.contains("handler.legal()"));
        assertTrue(jade.contains("handler.activeUpgrades()"));
        assertTrue(source("item/UpgradeWrenchItem.java").contains("UpgradeSlots.target(level, pos)"));
        var slots=source("upgrade/UpgradeSlots.java");
        assertFalse(slots.contains("effectsPending"));
    }
    @Test void unknownTaggedPluginsCannotBeInstalledEvenInCreative() {
        var known=Set.of("speed1","speed2","speed3","efficiency1","efficiency2");
        for (int count=0;count<=10;count++) for (boolean creative:new boolean[]{false,true}) {
            List<String> empty=new ArrayList<>(Collections.nCopies(count,null));
            var result=WrenchPlanner.plan(List.of("unknown"),empty,known::contains,Map.of("unknown",10),creative,
                    items->items.stream().allMatch(Objects::isNull));
            assertEquals(empty,result);assertFalse(WrenchPlanner.changed(empty,result));
        }
    }
    @Test void unexpectedSavedPluginsAreConservedAcrossFiveSlotMigrationToZeroOrTen() {
        var old=List.of("saved1","saved2","saved3","saved4","saved5");
        for (int count=0;count<=10;count++) {
            var resized=UpgradeLogic.resize(old,count,"");
            var conserved=new ArrayList<>(resized.kept());conserved.removeIf(String::isEmpty);conserved.addAll(resized.overflow());
            assertEquals(old,conserved);assertEquals(Math.max(0,5-count),resized.overflow().size());
        }
    }
}
