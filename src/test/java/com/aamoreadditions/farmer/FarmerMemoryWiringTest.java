package com.aamoreadditions.farmer;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Source contracts for the Minecraft side (mixin, runtime, packet, button); not live Minecraft/Mixin tests. */
class FarmerMemoryWiringTest {
    static String source(String p) throws Exception { return Files.readString(Path.of("src/main/java/com/aamoreadditions/" + p)); }
    static String between(String s, String a, String b) { int i = s.indexOf(a); return s.substring(i, s.indexOf(b, i)); }

    @Test void mixinCellFlowAndNbt() throws Exception {
        var s = source("mixin/TileEntityFarmerMixin.java");
        assertTrue(s.contains("com.aamoreadditions.farmer.FarmerMemoryState"));
        assertTrue(between(s, "private void aamoreadditions$beginCell", "@WrapOperation")
                .contains("aamoreadditions$planted=com.aamoreadditions.farmer.FarmerMemoryRuntime.beginCell((TileEntityFarmer)(Object)this,query);"));
        var planting = between(s, "private FarmerResult aamoreadditions$plantingWork", "/** Observe the original final output-fit result");
        assertTrue(planting.indexOf("allowsPlant(tile,pos,seed))return FarmerResult.FAIL") < planting.indexOf("original.call(behavior,seed,level,pos,farmer)"));
        assertTrue(planting.contains("if(result==FarmerResult.SUCCESS)com.aamoreadditions.farmer.FarmerMemoryRuntime.planted(tile,pos,seed);"));
        // a skipped planting returns before the work flag, so it is "nothing to do" for the smart sleep
        assertTrue(planting.indexOf("return FarmerResult.FAIL") < planting.indexOf("aamoreadditions$planted=true"));
        // mode + records only for SAVE_TILE/SYNC (never SAVE_BLOCK = item drop data)
        for (String k : new String[]{"write", "read"}) {
            var block = between(s, "if(type==TileEntityBase.NBTType.SAVE_TILE || type==TileEntityBase.NBTType.SYNC){\n            aamoreadditions$ringInventory()." + k, "}");
            assertTrue(block.contains("FarmerMemoryRuntime." + k + "(aamoreadditions$memory,tag,type==TileEntityBase.NBTType.SYNC)"));
        }
    }

    @Test void smartSleepIsModeAgnostic() throws Exception {
        var scanRt = source("effect/FarmerScanRuntime.java");
        assertFalse(scanRt.substring(scanRt.indexOf("public static void checked(")).contains("Memory"), "sleep accounting has no memory branch");
        assertFalse(source("effect/FarmerScanState.java").contains("Memory"));
        assertTrue(source("mixin/TileEntityFarmerMixin.java").contains("FarmerScanRuntime.checked((TileEntityFarmer)(Object)this,query,aamoreadditions$candidate,"));
        var r = source("farmer/FarmerMemoryRuntime.java");
        assertTrue(r.contains("if (of(tile).toggle()) ((FarmerWorkState) tile).aamoreadditions$resetFarmerScan();"));
        assertTrue(r.contains("tile.sendUpdate();"));
        assertTrue(r.contains("if (sync || snap.isEmpty()) return;"), "records never synced to clients");
    }

    @Test void beginCellObservesThenOneUnifiedHoeStep() throws Exception {
        var r = source("farmer/FarmerMemoryRuntime.java");
        var begin = between(r, "public static boolean beginCell(", "/** Planting gate");
        assertTrue(begin.contains("if (!memory.memory() || !(tile.getLevel() instanceof ServerLevel level)) return false;"));
        assertTrue(begin.indexOf("memory.observe(query.asLong(), plant ? seedOf(") < begin.indexOf("hoe(tile, level, query, state)"));
        assertTrue(begin.contains("!memory.shouldTill(query.asLong(), seed -> needsTilling(level, query, seed))"));
        var need = between(r, "static boolean needsTilling(", "/** Whether the recorded crop can be placed here now");
        assertTrue(need.contains("(bi.getBlock() instanceof CropBlock || bi.getBlock() instanceof StemBlock)"), "only farmland crops get their ground tilled");
        assertTrue(need.contains("!bi.getBlock().defaultBlockState().canSurvive(level, pos)"));
        assertTrue(begin.contains("memory.observe(query.asLong(), null, bareFarmland(level, query, Blocks.AIR.defaultBlockState()));"));
        assertTrue(r.contains("return of(tile).allowsPlant(query.asLong(), itemId(seed), id -> cropFits(tile.getLevel(), query, id));"));
        var hoe = between(r, "private static boolean hoe(", "private static boolean bareFarmland(");
        assertTrue(hoe.contains("MachineFakePlayers.of(tile)"));
        assertTrue(hoe.contains("new ItemStack(Items.DIAMOND_HOE)"));
        assertTrue(hoe.contains("FarmerEffects.cost(tile, TILL_COST)"));
        assertTrue(hoe.contains("!FarmerEffects.canWork(tile) || tile.getEnergy() < cost"));
        assertTrue(hoe.contains("getToolModifiedState(context, ItemAbilities.HOE_TILL, true)"));
        assertTrue(hoe.contains("if (result == null || result == groundState) return false;"), "any hoe reaction, incl. coarse/rooted dirt");
        assertFalse(hoe.contains("result.getBlock() instanceof FarmBlock"));
        assertTrue(hoe.indexOf("tile.extractEnergy(cost)") > hoe.indexOf("hoe.useOn(context)"));
        assertTrue(hoe.contains("if (level.getBlockState(ground) == groundState) return false;"));
        var fits = between(r, "static boolean cropFits(", "// ----------------");
        assertTrue(fits.contains("bi.getBlock().defaultBlockState().canSurvive(level, pos)"));
        assertTrue(fits.contains("!(BuiltInRegistries.ITEM.get(ResourceLocation.parse(seedId)) instanceof BlockItem bi)"), "non-block seeds: unknown = fits");
    }

    @Test void seedComesFromAStaticLootTableWalkWithCache() throws Exception {
        var r = source("farmer/FarmerMemoryRuntime.java");
        var seed = between(r, "static String seedOf(", "/** Seed = matches the configured candidates");
        assertTrue(seed.contains("SEEDS.get(crop, List.of(tables, seeds), () -> FarmerSeedLookup.pick("));
        assertTrue(seed.contains("level.getServer().reloadableRegistries()"));
        assertTrue(seed.contains("new FarmerSeedLookup.Ref(crop.getLootTable().location().toString())"));
        assertTrue(seed.contains("tables.getLootTable(ResourceKey.create(Registries.LOOT_TABLE"));
        for (String a : new String[]{"LootTableAccessor) table).aamoreadditions$pools()", "LootPoolAccessor) p).aamoreadditions$entries()",
                "LootItemAccessor) entry).aamoreadditions$item()", "TagEntryAccessor) entry).aamoreadditions$tag()",
                "CompositeEntryBaseAccessor) entry).aamoreadditions$children()", "NestedLootTableAccessor) entry).aamoreadditions$contents()"})
            assertTrue(seed.contains(a), a);
        assertTrue(seed.contains("return new FarmerSeedLookup.Other();"));
        for (String bad : new String[]{"getDrops", "LootParams", "getCloneItemStack", "SpecialPlantable", "ROLLS"}) assertFalse(r.contains(bad), bad);
        assertFalse(r.contains("Tags.Items.SEEDS")); assertFalse(r.contains("VILLAGER_PLANTABLE_SEEDS"));
        assertTrue(r.contains("boolean seed = seeds.matches(holder.getRegisteredName(),"));
        assertTrue(r.contains("SEEDS.get(crop, List.of(tables, seeds),"), "cache cleared on datapack reload or candidate change");
        assertTrue(r.contains("warn(\"farmer_memory_seed_candidates: ignoring invalid or unknown entries {}\", ignored)"));
        var cfg = Files.readString(Path.of("src/main/java/com/aamoreadditions/effect/UpgradeConfig.java"));
        assertTrue(cfg.contains(".defineListAllowEmpty(\"farmer_memory_seed_candidates\", com.aamoreadditions.farmer.FarmerSeedCandidates.DEFAULTS, () -> \"\", o -> o instanceof String)"));
        assertTrue(cfg.indexOf("\"farmer_memory_seed_candidates\"") > cfg.indexOf("\"farmer_accelerates_ripening\""));
        var tag = Files.readString(Path.of("src/main/resources/data/aamoreadditions/tags/item/farmer_memory_seeds.json"));
        for (String id : new String[]{"minecraft:nether_wart", "minecraft:sugar_cane", "minecraft:cactus"}) assertTrue(tag.contains("\"" + id + "\""), id);
        assertFalse(tag.contains("kelp"));
        assertTrue(tag.contains("\"replace\": false"));
        assertTrue(r.contains("bi.getBlock() == crop || (crop instanceof AttachedStemBlock && bi.getBlock() instanceof StemBlock)"));
        var mixins = Files.readString(Path.of("src/main/resources/aamoreadditions.mixins.json"));
        for (String m : new String[]{"LootTableAccessor", "LootPoolAccessor", "CompositeEntryBaseAccessor", "LootItemAccessor", "TagEntryAccessor", "NestedLootTableAccessor"})
            assertTrue(mixins.contains("\"" + m + "\""), m);
    }

    @Test void buttonPacketAndLang() throws Exception {
        var n = source("network/ModNetwork.java");
        assertTrue(n.contains("playToServer(FarmerModePayload.TYPE"));
        assertTrue(n.contains("instanceof de.ellpeck.actuallyadditions.mod.inventory.ContainerFarmer m && m.stillValid(player)"));
        var b = source("client/FarmerModeButton.java");
        assertTrue(b.contains("X = 3, Y = 5, SIZE = 18"));
        assertTrue(b.contains("RED = 0xFFAA0000, GREEN = 0xFF55FF55"));
        assertTrue(b.contains("{1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1}"));
        assertTrue(b.contains("g.blit(SEEDS, getX(), getY() + 1, 0, 0, 16, 16, 16, 16)"));
        assertTrue(b.contains("withDefaultNamespace(\"textures/item/wheat_seeds.png\")"));
        assertFalse(Files.exists(Path.of("src/main/resources/assets/minecraft/textures/item/wheat_seeds.png")));
        var en = Files.readString(Path.of("src/main/resources/assets/aamoreadditions/lang/en_us.json"));
        var zh = Files.readString(Path.of("src/main/resources/assets/aamoreadditions/lang/zh_cn.json"));
        assertTrue(en.contains("\"gui.aamoreadditions.farmer.mode.out_of_order\": \"Mode: Out-of-order\""));
        assertTrue(en.contains("\"gui.aamoreadditions.farmer.mode.memory\": \"Mode: Memory\""));
        assertTrue(zh.contains("\"gui.aamoreadditions.farmer.mode.out_of_order\": \"模式：无序\""));
        assertTrue(zh.contains("\"gui.aamoreadditions.farmer.mode.memory\": \"模式：记忆\""));
    }

    @Test void recordsArePrunedOnlyWhenTheScanAreaChanges() throws Exception {
        var scan = source("effect/FarmerScanRuntime.java");
        var prepare = between(scan, "public static void prepare(", "public static void checked(");
        assertTrue(prepare.indexOf("if(scan.configure(side(tile),facing,tile.getBlockPos().asLong(),UpgradeConfig.farmerSmartSleep())) {")
                < prepare.indexOf("FarmerMemoryRuntime.retainArea(tile,side(tile));"), "only when configure reports a change (area, facing, position, load)");
        assertFalse(between(scan, "public static void checked(", "public static int cooldown(").contains("retainArea"), "never per scan");
        var r = source("farmer/FarmerMemoryRuntime.java");
        var retain = between(r, "public static void retainArea(", "/** Planting gate");
        assertTrue(retain.contains("relative(tile.getBlockState().getValue("));
        assertTrue(retain.contains("HORIZONTAL_FACING), radius + 1)"), "same center as AA's serverTick");
        // no skip-when-unchanged optimisation: observe always writes a seen crop
        assertTrue(source("farmer/FarmerMemory.java").contains("if (seed != null && !seed.isEmpty()) records.put(pos, seed);"));
    }
}
