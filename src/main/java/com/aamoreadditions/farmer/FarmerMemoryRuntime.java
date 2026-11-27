package com.aamoreadditions.farmer;

import com.aamoreadditions.effect.FarmerEffects;
import com.aamoreadditions.upgrade.FarmerWorkState;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.*;
import com.aamoreadditions.mixin.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import com.aamoreadditions.fake.MachineFakePlayers;

/** Minecraft side of the Farmer memory mode; the rules live in {@link FarmerMemory}. Server side only. */
public final class FarmerMemoryRuntime {
    private FarmerMemoryRuntime() {}

    private static final String KEY_MODE = "aamoreadditions_farmer_memory_mode", KEY_RECORDS = "aamoreadditions_farmer_memory";
    /** AA DefaultFarmerBehavior's tilling charge, scaled like every other Farmer operation. */
    public static final int TILL_COST = 350;
    private static final FarmerSeedLookup<Block> SEEDS = new FarmerSeedLookup<>();

    public static FarmerMemory of(TileEntityFarmer tile) { return ((FarmerMemoryState) tile).aamoreadditions$farmerMemory(); }

    // ---------------- per scanned cell (checkBehaviors HEAD, before AA's harvest/plant loop) ----------------

    /**
     * Memory mode only: record what stands here, then at most one hoe interaction on the ground.
     * Returns true when the hoe interaction happened (work for the smart-sleep scan, like AA's tilling).
     */
    public static boolean beginCell(TileEntityFarmer tile, BlockPos query) {
        FarmerMemory memory = of(tile);
        if (!memory.memory() || !(tile.getLevel() instanceof ServerLevel level)) return false;
        BlockState state = level.getBlockState(query);
        boolean plant = isPlant(state.getBlock());
        memory.observe(query.asLong(), plant ? seedOf(level, state) : null, !plant && bareFarmland(level, query, state));
        if (plant || !state.canBeReplaced() || !state.getFluidState().isEmpty()
                || !memory.shouldTill(query.asLong(), seed -> needsTilling(level, query, seed)) || !hoe(tile, level, query, state))
            return false;
        memory.observe(query.asLong(), null, bareFarmland(level, query, Blocks.AIR.defaultBlockState()));
        return true;
    }

    /** Keeps only records inside the square AA scans: side x side around center (radius + 1 in front), at its y. */
    public static void retainArea(TileEntityFarmer tile, int side) {
        int radius = side / 2;
        BlockPos center = tile.getBlockPos().relative(tile.getBlockState().getValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING), radius + 1);
        of(tile).retain(p -> FarmerMemory.inSquare(center.getX(), center.getY(), center.getZ(), radius,
                BlockPos.getX(p), BlockPos.getY(p), BlockPos.getZ(p)));
    }

    /** Planting gate around every AA tryPlantSeed call (out-of-order mode: always true). */
    public static boolean allowsPlant(TileEntityFarmer tile, BlockPos query, ItemStack seed) {
        return of(tile).allowsPlant(query.asLong(), itemId(seed), id -> cropFits(tile.getLevel(), query, id));
    }

    public static void planted(TileEntityFarmer tile, BlockPos query, ItemStack seed) {
        of(tile).observe(query.asLong(), itemId(seed), false);
    }

    /**
     * One hoe right-click on the ground below query, like AA (fake player, diamond hoe useOn) but accepting any
     * HOE_TILL reaction (grass/dirt/dirt path -> farmland, coarse dirt -> dirt, rooted dirt -> dirt + hanging roots,
     * modded tillables via NeoForge). Charges AA's tilling energy only if the ground actually changed.
     */
    private static boolean hoe(TileEntityFarmer tile, ServerLevel level, BlockPos query, BlockState state) {
        BlockPos ground = query.below();
        BlockState groundState = level.getBlockState(ground);
        int cost = FarmerEffects.cost(tile, TILL_COST);
        if (groundState.getBlock() instanceof FarmBlock || !FarmerEffects.canWork(tile) || tile.getEnergy() < cost) return false;
        var player = MachineFakePlayers.of(tile);
        ItemStack hoe = new ItemStack(Items.DIAMOND_HOE);
        if (!player.mayUseItemAt(query, Direction.UP, hoe)) return false;
        if (!state.isAir()) { // like AA's defaultPlant: clear replaceable vegetation on grass/dirt first (tilling needs air above)
            if (!(groundState.is(BlockTags.DIRT) || groundState.is(Blocks.GRASS_BLOCK))) return false;
            level.setBlockAndUpdate(query, Blocks.AIR.defaultBlockState());
        }
        var context = new UseOnContext(level, player, InteractionHand.MAIN_HAND, hoe,
                new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false));
        BlockState result = groundState.getToolModifiedState(context, ItemAbilities.HOE_TILL, true);
        if (result == null || result == groundState) return false;
        hoe.useOn(context);
        if (level.getBlockState(ground) == groundState) return false;
        tile.extractEnergy(cost);
        return true;
    }

    private static boolean bareFarmland(ServerLevel level, BlockPos pos, BlockState state) {
        return state.canBeReplaced() && level.getBlockState(pos.below()).getBlock() instanceof FarmBlock;
    }

    /**
     * A recorded crop is tilled for only if it is a farmland crop (CropBlock/StemBlock) that cannot stand here now.
     * Nether wart, sugar cane, cactus (and any non-farmland plant) never get their ground tilled.
     */
    static boolean needsTilling(net.minecraft.world.level.Level level, BlockPos pos, String seedId) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(seedId)) instanceof BlockItem bi
                && (bi.getBlock() instanceof CropBlock || bi.getBlock() instanceof StemBlock)
                && !bi.getBlock().defaultBlockState().canSurvive(level, pos);
    }

    /** Whether the recorded crop can be placed here now (BlockItem: canSurvive; anything else: unknown = fits). */
    static boolean cropFits(net.minecraft.world.level.Level level, BlockPos pos, String seedId) {
        return !(BuiltInRegistries.ITEM.get(ResourceLocation.parse(seedId)) instanceof BlockItem bi)
                || bi.getBlock().defaultBlockState().canSurvive(level, pos);
    }

    // ---------------- crop -> seed ----------------

    /** Farmable plants recorded by memory mode; grass, dirt, melon/pumpkin fruit, flowers, ... are not. */
    static boolean isPlant(Block block) {
        return block instanceof CropBlock || block instanceof StemBlock || block instanceof AttachedStemBlock
                || block instanceof NetherWartBlock || block instanceof SugarCaneBlock || block instanceof CactusBlock;
    }

    private static List<String> candidatesRaw;
    private static FarmerSeedCandidates candidates;

    /** Config list parsed once per distinct value; invalid/unknown entries are logged once and ignored. */
    static FarmerSeedCandidates candidates() {
        List<String> raw = com.aamoreadditions.effect.UpgradeConfig.farmerMemorySeedCandidates();
        if (candidates == null || !raw.equals(candidatesRaw)) {
            candidatesRaw = List.copyOf(raw);
            candidates = FarmerSeedCandidates.parse(candidatesRaw);
            var ignored = new java.util.ArrayList<>(candidates.invalid());
            candidates.items().stream().filter(id -> !BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id))).forEach(ignored::add);
            if (!ignored.isEmpty())
                com.mojang.logging.LogUtils.getLogger().warn("farmer_memory_seed_candidates: ignoring invalid or unknown entries {}", ignored);
        }
        return candidates;
    }

    /** Seed from a static walk of the crop's loot table; cached per block type until datapacks reload or the candidate list changes. */
    static String seedOf(ServerLevel level, BlockState state) {
        Block crop = state.getBlock();
        var tables = level.getServer().reloadableRegistries();
        var seeds = candidates();
        return SEEDS.get(crop, List.of(tables, seeds), () -> FarmerSeedLookup.pick(
                FarmerSeedLookup.items(new FarmerSeedLookup.Ref(crop.getLootTable().location().toString()),
                        id -> node(tables.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(id))))),
                id -> kind(crop, BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)), seeds)));
    }

    private static FarmerSeedLookup.Node node(LootTable table) {
        return new FarmerSeedLookup.Group(((LootTableAccessor) table).aamoreadditions$pools().stream()
                .<FarmerSeedLookup.Node>map(p -> group(((LootPoolAccessor) p).aamoreadditions$entries())).toList());
    }

    private static FarmerSeedLookup.Node group(java.util.List<LootPoolEntryContainer> entries) {
        return new FarmerSeedLookup.Group(entries.stream().map(FarmerMemoryRuntime::node).toList());
    }

    private static FarmerSeedLookup.Node node(LootPoolEntryContainer entry) {
        if (entry instanceof LootItem) return new FarmerSeedLookup.Item(((LootItemAccessor) entry).aamoreadditions$item().getRegisteredName());
        if (entry instanceof TagEntry) return new FarmerSeedLookup.Items(BuiltInRegistries.ITEM.getTag(((TagEntryAccessor) entry).aamoreadditions$tag())
                .map(set -> set.stream().map(h -> h.getRegisteredName()).toList()).orElse(java.util.List.of()));
        if (entry instanceof CompositeEntryBase) return group(((CompositeEntryBaseAccessor) entry).aamoreadditions$children());
        if (entry instanceof NestedLootTable) return ((NestedLootTableAccessor) entry).aamoreadditions$contents()
                .<FarmerSeedLookup.Node>map(key -> new FarmerSeedLookup.Ref(key.location().toString()), FarmerMemoryRuntime::node);
        return new FarmerSeedLookup.Other();
    }

    /** Seed = matches the configured candidates (item ids / #tags); placesCrop = places this block (attached stems: the stem). */
    static FarmerSeedLookup.Kind kind(Block crop, Item item, FarmerSeedCandidates seeds) {
        var holder = item.builtInRegistryHolder();
        boolean seed = seeds.matches(holder.getRegisteredName(),
                tag -> holder.is(net.minecraft.tags.TagKey.create(Registries.ITEM, ResourceLocation.parse(tag))));
        boolean places = item instanceof BlockItem bi && (bi.getBlock() == crop || (crop instanceof AttachedStemBlock && bi.getBlock() instanceof StemBlock));
        return new FarmerSeedLookup.Kind(seed, places);
    }

    private static String itemId(ItemStack stack) {
        return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    // ---------------- mode button / NBT ----------------

    /** Server side of the GUI button. Entering memory mode wakes a sleeping Farmer once; leaving it deletes the records. */
    public static void toggle(TileEntityFarmer tile) {
        if (of(tile).toggle()) ((FarmerWorkState) tile).aamoreadditions$resetFarmerScan();
        tile.setChanged();
        tile.sendUpdate();
    }

    /** Mode goes to world saves and client sync; records only to world saves (never to the dropped item). */
    public static void write(FarmerMemory memory, CompoundTag tag, boolean sync) {
        tag.putBoolean(KEY_MODE, memory.memory());
        var snap = memory.snapshot();
        if (sync || snap.isEmpty()) return;
        long[] positions = new long[snap.size()];
        ListTag seeds = new ListTag();
        int i = 0;
        for (var e : snap.entrySet()) { positions[i++] = e.getKey(); seeds.add(StringTag.valueOf(e.getValue())); }
        CompoundTag records = new CompoundTag();
        records.put("Pos", new LongArrayTag(positions));
        records.put("Seed", seeds);
        tag.put(KEY_RECORDS, records);
    }

    public static void read(FarmerMemory memory, CompoundTag tag, boolean sync) {
        boolean on = tag.getBoolean(KEY_MODE);
        if (sync) { memory.setMemory(on); return; }
        CompoundTag records = tag.getCompound(KEY_RECORDS);
        ListTag list = records.getList("Seed", Tag.TAG_STRING);
        var seeds = new java.util.ArrayList<String>(list.size());
        for (int i = 0; i < list.size(); i++) seeds.add(list.getString(i));
        memory.load(on, records.getLongArray("Pos"), seeds);
    }
}
