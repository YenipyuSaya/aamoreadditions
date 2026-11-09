package com.aamoreadditions.upgrade;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
/** Source-contract checks complement pure policy tests; these are NOT runtime Mixin/game tests. */
class UpgradeWiringTest {
    static String source(String relative) throws Exception {return Files.readString(Path.of("src/main/java/com/aamoreadditions/"+relative));}
    @Test void startupRulesAreCapturedOnceAndSynced() throws Exception {
        var s=source("upgrade/UpgradeStartup.java");
        assertTrue(s.contains("If true, higher-level upgrades cannot be installed independently. Lower-level upgrades must be installed beforehand."));
        assertTrue(s.contains("define(\"Dependency Upgrade Plugins\", false)"));
        assertTrue(s.contains("Warning: This is very likely to cause machines that have already been configured to stop working, you must be clear about what you are doing!"));
        assertTrue(s.contains("beforehand.\",\n                \"\",\n                \"Warning:"));
        assertTrue(s.contains("ModConfigEvent.Loading"));assertFalse(s.contains("ModConfigEvent.Reloading"));
        assertTrue(s.contains("|| captured"));assertTrue(s.contains("0, 10"));assertTrue(s.contains(".gameRestart()"));
        assertTrue(source("upgrade/UpgradeSession.java").contains("PacketDistributor.sendToPlayer"));
        assertTrue(source("client/UpgradeDisconnect.java").contains("UpgradeStartup.resetClient()"));
    }
    @Test void everyNativeUpgradeMachineUsesCentralTickGuard() throws Exception {
        var s=source("mixin/UpgradeMachineTickMixin.java");
        for(String name:new String[]{"Crusher","PoweredFurnace","CoalGenerator","OilGenerator","HeatCollector","LeafGenerator","Energizer","Empowerer","DisplayStand"})assertTrue(s.contains("TileEntity"+name+".class"));
        assertTrue(s.contains("UpgradeRuntime.tick(be)"));assertTrue(source("block/AlloyFurnaceBlockEntity.java").contains("UpgradeRuntime.tick(this)"));
        assertTrue(source("structure/CasingCrossData.java").contains("if (!upgrades.legal()) { reset(); return; }"));
    }
    @Test void wrongLogicClearsWorkButPreservesEnergySharing() throws Exception {
        var s=source("upgrade/UpgradeRuntime.java");
        assertTrue(s.contains("firstCrushTime = t.secondCrushTime = 0"));assertTrue(s.contains("firstSmeltTime = t.secondSmeltTime = 0"));
        assertTrue(s.contains("currentBurnTime = t.maxBurnTime = 0"));assertTrue(s.contains("currentEnergyProduce = 0"));
        assertTrue(s.contains("WorldUtil.doEnergyInteraction"));assertTrue(s.contains("level.isLoaded"));
        var guard=source("upgrade/UpgradeAutomation.java");assertTrue(guard.contains("UpgradeRuntime.blocked(be) ? stack"));
        assertTrue(guard.contains("UpgradeRuntime.blocked(be)?FluidStack.EMPTY"));
    }
    @Test void cachedStructureCapabilitiesRouteOnEachOperation() throws Exception {
        var hook=source("mixin/UpgradeInventoryAutomationMixin.java");
        assertTrue(hook.contains("TileEntityEmpowerer")); assertTrue(hook.contains("TileEntityDisplayStand"));
        assertTrue(hook.contains("UpgradeAutomation.items(be,cir.getReturnValue())"));
        var guard=source("upgrade/UpgradeAutomation.java");
        assertTrue(guard.contains("private IItemHandler target()"));
        assertTrue(guard.contains("if (entry != null) return entry.external"));
        assertTrue(guard.contains("!inRange(target,slot) || UpgradeRuntime.blocked(be)"));
        assertTrue(source("upgrade/UpgradeRuntime.java").contains("empowerer.processTime = 0; be.setChanged()"));
    }
    @Test void overflowIsPersistedAndRemovedOnlyAfterSuccessfulWorldSpawn() throws Exception {
        var s=source("upgrade/UpgradeSlots.java");
        assertTrue(s.contains("tag.put(\"PendingUpgradeDrops\", list)"));
        assertTrue(s.contains("nbt.getList(\"PendingUpgradeDrops\", 10)"));
        assertTrue(s.contains("if (!level.hasChunkAt(pos)) return"));
        assertTrue(s.contains("if (level.addFreshEntity(entity)) { it.remove(); dirty(); }"));
        assertTrue(source("structure/CasingCrossEvents.java").contains("e.upgrades.drainOverflow(level, anchor)"));
    }
    @Test void wrenchValidatesRecordAndSupportsRecordedZeroSlots() throws Exception {
        assertTrue(source("item/WrenchRecord.java").contains("Codec.intRange(0, 10)"));
        var s=source("item/UpgradeWrenchItem.java");assertTrue(s.contains("recording && !handler.legal()"));
        assertTrue(s.contains("UpgradeRules.validContents(held.get(WrenchRecord.TYPE.get()).slots())"));
        assertTrue(s.contains("message.aamoreadditions.upgrades.wrong_logic"));
    }
    @Test void independentMachineAndGuiConsumeAllInstalledLineMembers() throws Exception {
        var slots=source("upgrade/UpgradeSlots.java");
        assertTrue(slots.contains("UpgradeLogic.active(installed,UpgradeStartup.dependency()"));
        assertTrue(source("upgrade/UpgradeLogic.java").contains("if (!dependency) return List.copyOf(installed)"));
        var effects=source("effect/UpgradeEffects.java");
        assertTrue(effects.contains("active.stream().filter(t -> t.line == line).map(t -> value.applyAsDouble(t)).toList()"));
        assertTrue(effects.contains("case CRUSHER, CRUSHER_DOUBLE, POWERED_FURNACE -> UpgradeAggregation.processorEnergy(s,e)"));
        assertFalse(effects.contains(".distinct()"));
        var gui=source("client/UpgradeInfo.java");
        assertTrue(gui.contains("var active = h.activeUpgrades()"));
        assertTrue(gui.contains("UpgradeType.Line.SPEED).count()"));
        assertTrue(gui.contains("UpgradeType.Line.EFFICIENCY).count()"));
        assertTrue(gui.contains("boundaryFactors(kind,speed,efficiency,"));
        assertTrue(gui.contains("energy = UpgradeEffects.energy(be)"));
        for(String machine:new String[]{"Crusher","PoweredFurnace"})
            assertTrue(source("mixin/TileEntity"+machine+"Mixin.java").contains("UpgradeEffects.energyPerTick("));
        assertTrue(source("block/AlloyFurnaceBlockEntity.java").contains("UpgradeEffects.alloyEnergyPerTick("));
    }
    @Test void attachmentSyncKeepsTheHandlerReferencedByOpenMenuSlots() throws Exception {
        var s=source("upgrade/UpgradeSlots.java");
        assertTrue(s.contains("holder.hasData(UPGRADE_INV.get()) ? holder.getData(UPGRADE_INV.get()) : new Handler(holder, false)"));
        assertTrue(s.contains("handler.deserializeNBT(provider, tag)"));
        assertTrue(s.contains("activeCache = null;\n            refresh()"));
        assertTrue(source("client/UpgradeInfoButton.java").contains("lines.get().stream()"));
        assertTrue(source("upgrade/UpgradeMenus.java").contains("new UpgradeSlot(menu, h, i)"));
    }
    @Test void everyManualMutationReachesServerSyncWithoutDependingOnCapacityChanges() throws Exception {
        var s=source("upgrade/UpgradeSlots.java");
        assertTrue(s.contains("protected void onContentsChanged(int slot) {\n            refresh();"));
        assertTrue(s.contains("&& be.getLevel() instanceof net.minecraft.server.level.ServerLevel) aa.sendUpdate()"));
        assertTrue(s.contains("UpgradeRuntime.clearWork(be);\n            dirty()"));
        assertTrue(source("upgrade/UpgradeInteraction.java").contains("h.setStackInSlot(i, ItemStack.EMPTY)"));
        assertTrue(source("item/UpgradeWrenchItem.java").contains("handler.replaceContents(replacement)"));
        assertTrue(source("upgrade/UpgradeMenus.java").contains("slot.set(ItemStack.EMPTY)"));
    }
    @Test void infoUsesCoefficientsAndIndependentAffectedAttributeLines() throws Exception {
        var s=source("client/UpgradeInfo.java");assertTrue(s.contains("boundaryFactors("));
        assertTrue(s.contains("boundaryLine(lines,\"speed\",f.speed())"));assertTrue(s.contains("boundaryLine(lines,\"energy\",f.energy())"));
        assertTrue(s.contains("boundaryLine(lines,\"utilization\",f.utilization())"));assertTrue(s.contains("if (value == 1) return"));
    }
}
