package com.aamoreadditions.digger;

import com.aamoreadditions.mixin.TileEntityBaseAccessor;
import com.aamoreadditions.upgrade.UpgradeSlots;
import de.ellpeck.actuallyadditions.mod.config.CommonConfig;
import de.ellpeck.actuallyadditions.mod.items.ActuallyItems;
import de.ellpeck.actuallyadditions.mod.network.PacketHelperServer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomface;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;
import de.ellpeck.actuallyadditions.mod.util.StackUtil;
import de.ellpeck.actuallyadditions.mod.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import com.aamoreadditions.fake.MachineFakePlayers;

import java.util.List;

/**
 * AAMA replacement of TileEntityVerticalDigger#serverTick (ported from Actually Additions, MIT; AA's source is not modified).
 * Differences to AA: upgrade period/energy/capacity, Silk Touch / Fortune drops, chunk-unload pause,
 * range-change restart, a cached dummy AIOT, configurable costs and the Block Placing fillers.
 */
public final class DiggerRuntime {
    private DiggerRuntime() {}

    public static final int BUTTON_MODE = 7720, BUTTON_HIGHLIGHT = 7721, BUTTON_FILLERS_CLOSE = 7722, BUTTON_FILLERS_OPEN = 7723;

    /** Called from the tick mixin after the upgrade guard passed (legal plugins). */
    public static void serverTick(TileEntityVerticalDigger tile, ServerLevel level, BlockPos pos) {
        var base = (TileEntityBaseAccessor) tile;
        base.aamoreadditions$baseServerTick(); // ticksElapsed++ and AA base housekeeping
        DiggerWork w = ((DiggerState) tile).aamoreadditions$work();
        boolean scanTick = false;
        if (!tile.isRedstonePowered) {
            var effects = DiggerEffects.evaluate(tile);
            if (++w.counter >= DiggerEffects.period(effects.speed())) {
                w.counter = 0;
                scanTick = true;
                work(tile, level, pos, w, effects);
            }
        }
        if (!scanTick && base.aamoreadditions$ticksElapsed() % 10 == 0) w.range = TileEntityPhantomface.upgradeRange(TileEntityVerticalDigger.DEFAULT_RANGE, level, pos);
        int energy = tile.storage.getEnergyStored();
        if ((w.oldEnergy != energy || w.oldX != tile.checkX || w.oldY != tile.checkY || w.oldZ != tile.checkZ || w.oldRange != w.range)
                && base.aamoreadditions$sendUpdateWithInterval()) {
            w.oldEnergy = energy; w.oldX = tile.checkX; w.oldY = tile.checkY; w.oldZ = tile.checkZ; w.oldRange = w.range;
        }
    }

    private static void work(TileEntityVerticalDigger tile, ServerLevel level, BlockPos pos, DiggerWork w,
                             com.aamoreadditions.effect.FarmerUpgradeMath.Effects effects) {
        int minY = level.getMinBuildHeight();
        int range = TileEntityPhantomface.upgradeRange(TileEntityVerticalDigger.DEFAULT_RANGE, level, pos);
        w.range = range;
        boolean chunk = ((DiggerState) tile).aamoreadditions$strictChunk();
        var area = DiggerScan.relative(pos.getX(), pos.getZ(), range, chunk);
        var phase = DiggerScan.phase(tile.checkY, minY);
        if (phase == DiggerScan.Phase.DONE) return;
        if (phase == DiggerScan.Phase.WORKING && w.scanRange == 0) {
            // Unknown start (scan from before AAMA, always a radius box): keep it only as a radius scan.
            if (Math.abs(tile.checkX) <= range && Math.abs(tile.checkZ) <= range) {
                w.scanRange = range;
                w.scanStrictChunk = false;
            }
        }
        int span = DiggerEffects.verticalSpan(tile);
        if (phase == DiggerScan.Phase.WORKING && w.scanVerticalSpan == 0) w.scanVerticalSpan = span;
        if (phase == DiggerScan.Phase.START || DiggerScan.restartForArea(
                w.scanRange, w.scanStrictChunk, w.scanVerticalSpan, range, chunk, span, phase)) {
            set(tile, DiggerScan.start(pos.getY(), area));
            w.scanRange = range;
            w.scanStrictChunk = chunk;
            w.scanVerticalSpan = span;
            tile.setChanged();
        }
        if (tile.checkY > minY && mineColumn(tile, level, pos, w, effects, minY, span)) {
            var scanArea = DiggerScan.relative(pos.getX(), pos.getZ(), w.scanRange, w.scanStrictChunk);
            set(tile, DiggerScan.next(new DiggerScan.Cursor(tile.checkX, tile.checkY, tile.checkZ), scanArea, span, minY));
            tile.setChanged();
        }
    }

    private static void set(TileEntityVerticalDigger tile, DiggerScan.Cursor c) {
        tile.checkX = c.x(); tile.checkY = c.y(); tile.checkZ = c.z();
    }

    /**
     * Process the current column: the cursor Y and the next {@code span}-1 blocks downward,
     * never at or below {@code minY}. The cursor stays at the band's top Y until the whole
     * batch succeeds. false = pause mid-batch (missing FE, full output, unloaded chunk).
     */
    private static boolean mineColumn(TileEntityVerticalDigger tile, ServerLevel level, BlockPos origin, DiggerWork w,
                                      com.aamoreadditions.effect.FarmerUpgradeMath.Effects effects, int minY, int span) {
        int y0 = tile.checkY;
        for (int i = 0; i < Math.max(1, span); i++) {
            int y = y0 - i;
            if (y <= minY) break;
            if (!mine(tile, level, origin, w, effects, y, span)) return false;
        }
        return true;
    }

    /** true = this Y is done (mined, skipped, or filled), false = stay (missing FE, full output, unloaded chunk). */
    private static boolean mine(TileEntityVerticalDigger tile, ServerLevel level, BlockPos origin, DiggerWork w,
                                com.aamoreadditions.effect.FarmerUpgradeMath.Effects effects, int y, int span) {
        BlockPos target = new BlockPos(origin.getX() + tile.checkX, y, origin.getZ() + tile.checkZ);
        // Pause while the scanned block's chunk is not loaded; never load or generate it.
        if (!level.isLoaded(target)) return false;
        int mineCost = DiggerEffects.miningCost(tile, effects.energy());
        var storage = tile.storage;
        // No range upgrade: AA's stored >= miningCost gate (air included). Range upgrades skip air freely.
        if (!DiggerEffects.hasBufferedMiningCost(storage.getEnergyStored(), mineCost, span)) return false;
        boolean placer = DiggerEffects.placingEnabled(tile);
        DiggerFillers fillers = DiggerFillers.of(tile);
        BlockState state = level.getBlockState(target);
        // 1) air: nothing to mine; fill it only in ores-only mode with the Block Placing plugin
        if (state.isAir()) {
            if (!placer || !fillers.hasPlaceable()) return true;
            int placeCost = DiggerEffects.placingCost(effects.energy());
            if (storage.getEnergyStored() < placeCost) return false; // wait so no hole is left behind
            place(tile, level, target, fillers, placeCost);
            return true;
        }
        // 2) blacklist
        if (blacklisted(state.getBlock())) return true;
        // 3) tool / hardness / ores-only, then drops
        boolean silk = DiggerEffects.silkTouch(tile);
        int fortune = silk ? 0 : DiggerEffects.fortune(tile);
        ItemStack tool = tool(level, w, silk, fortune);
        if (!tool.isCorrectToolForDrops(state) || state.getDestroySpeed(level, target) < 0F) return true;
        if (tile.onlyMineOres && !ore(tile, level, target, state)) return true;
        boolean plan = placer && fillers.hasPlaceable();
        int placeCost = plan ? DiggerEffects.placingCost(effects.energy()) : 0;
        if ((long) storage.getEnergyStored() < (long) mineCost + placeCost) return false;
        var be = level.getBlockEntity(target);
        // Always the cached dummy AIOT: plain when no Silk/Fortune, enchanted when installed.
        List<ItemStack> drops = Block.getDrops(state, level, target, be, null, tool);
        float chance = WorldUtil.fireFakeHarvestEventsForDropChance(tile, level, target);
        if (chance > 0 && level.random.nextFloat() <= chance) {
            if (!StackUtil.canAddAll(tile.inv, drops, false)) return false;
            level.levelEvent(2001, target, Block.getId(state));
            level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            StackUtil.addAll(tile.inv, drops, false);
            tile.setChanged();
            storage.extractEnergyInternal(mineCost, false);
            PacketHelperServer.spawnLaserWithTimeServer(level, origin.getX(), origin.getY(), origin.getZ(),
                    target.getX(), target.getY(), target.getZ(), 0x429602, 10, 120, 0.1F, 0.8F);
            if (plan) place(tile, level, target, fillers, placeCost);
        }
        return true;
    }

    /**
     * Places one block from the first filler slot holding a block item, skipping non-placeable slots.
     * A block item that cannot be placed here is skipped as well; if nothing can be placed, nothing is consumed.
     */
    static boolean place(TileEntityVerticalDigger tile, ServerLevel level, BlockPos target, DiggerFillers fillers, int cost) {
        for (int i = DiggerScan.firstPlaceable(fillers.contents(), ItemStack::isEmpty, DiggerFillers::placeable, 0); i >= 0;
             i = DiggerScan.firstPlaceable(fillers.contents(), ItemStack::isEmpty, DiggerFillers::placeable, i + 1)) {
            ItemStack stack = fillers.getStackInSlot(i);
            ItemStack probe = stack.copyWithCount(1); // the real slot is only touched after success
            var context = new DirectionalPlaceContext(level, target, Direction.DOWN, probe, Direction.UP);
            if (((BlockItem) stack.getItem()).place(context).consumesAction()) {
                fillers.extractItem(i, 1, false);
                if (cost > 0) tile.storage.extractEnergyInternal(cost, false);
                tile.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean blacklisted(Block block) {
        String reg = BuiltInRegistries.BLOCK.getKey(block).toString();
        for (String s : CommonConfig.Machines.MINER_BLACKLIST.get()) if (reg.equals(s)) return true;
        return false;
    }

    /** AA's "Only Mining Ores" test; the clone stack is only built in this mode. */
    private static boolean ore(TileEntityVerticalDigger tile, ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack stack = state.getBlock().getCloneItemStack(state, new BlockHitResult(Vec3.ZERO, Direction.DOWN, pos, false),
                level, pos, MachineFakePlayers.of(tile));
        if (stack.isEmpty()) return false;
        if (stack.is(Tags.Items.ORES) || state.is(Tags.Blocks.ORES)) return true;
        String reg = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        for (String s : CommonConfig.Machines.MINER_EXTRA_WHITELIST.get()) if (reg.equals(s)) return true;
        return false;
    }

    /** The single dummy AIOT of this digger (plain, or Silk/Fortune when those are installed). Rebuilt only when that key changes. */
    private static ItemStack tool(ServerLevel level, DiggerWork w, boolean silk, int fortune) {
        int key = silk ? -1 : fortune;
        if (w.tool == null || w.tool.isEmpty() || w.toolKey != key) {
            ItemStack t = new ItemStack(ActuallyItems.NETHERITE_AIOT.get());
            var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            if (silk) t.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
            else if (fortune > 0) t.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), fortune);
            w.tool = t;
            w.toolKey = key;
        }
        return w.tool;
    }

    /** Server side of the new GUI buttons (vanilla container-button packet, validated by the menu). */
    public static boolean button(TileEntityVerticalDigger tile, int id) {
        DiggerState st = (DiggerState) tile;
        if (id == BUTTON_MODE) st.aamoreadditions$setStrictChunk(!st.aamoreadditions$strictChunk());
        else if (id == BUTTON_HIGHLIGHT) st.aamoreadditions$setHighlight(!st.aamoreadditions$highlight());
        else return false;
        tile.setChanged();
        tile.sendUpdate();
        return true;
    }

    /** Called after AA's Reset button (id 1): forget the start range so the next scan adopts the current one. */
    public static void reset(TileEntityVerticalDigger tile) {
        DiggerWork w = ((DiggerState) tile).aamoreadditions$work();
        w.scanRange = 0;
        w.scanStrictChunk = false;
        w.scanVerticalSpan = 0;
        w.counter = 0;
    }

    public static void dropFillers(net.minecraft.world.level.Level level, TileEntityVerticalDigger tile) {
        if (level.isClientSide) return;
        DiggerFillers f = DiggerFillers.of(tile);
        BlockPos p = tile.getBlockPos();
        for (int i = 0; i < f.getSlots(); i++) {
            ItemStack s = f.getStackInSlot(i);
            if (!s.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, s.copy());
                f.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    /** Requirement for any filler automation: plugin installed and the slots legal. */
    public static boolean fillersAutomated(TileEntityVerticalDigger tile) {
        return DiggerEffects.placingEnabled(tile) && UpgradeSlots.get(tile).legal();
    }
}
