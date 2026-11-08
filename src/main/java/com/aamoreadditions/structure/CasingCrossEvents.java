package com.aamoreadditions.structure;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.block.ModBlocks;
import com.aamoreadditions.menu.CasingCrossMenu;
import de.ellpeck.actuallyadditions.mod.blocks.ActuallyBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Detection (driven by block changes), activation sound, whole-structure right-click, energy capability. */
public final class CasingCrossEvents {
    private CasingCrossEvents() {}

    /** Changed cells waiting for a check, per level; filled from the LevelChunk#setBlockState mixin. */
    private static final Map<ServerLevel, Set<BlockPos>> PENDING = new WeakHashMap<>();

    public static boolean isPart(Block b) {
        return b == ActuallyBlocks.EMPOWERER.get() || b == ActuallyBlocks.DISPLAY_STAND.get() || b == ModBlocks.DECORATIVE_MACHINE_CASING.get();
    }

    /** Called by the mixin after a real block-state change. Cheap: just queues the position. */
    public static void onBlockChanged(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (!(level instanceof ServerLevel sl)) return;
        if (oldState.getBlock() == newState.getBlock() && !isPart(newState.getBlock())) return;
        if (!isPart(oldState.getBlock()) && !isPart(newState.getBlock())) return;
        synchronized (PENDING) { PENDING.computeIfAbsent(sl, k -> new LinkedHashSet<>()).add(pos.immutable()); }
    }

    @EventBusSubscriber(modid = AAMoreAdditions.MODID)
    public static final class GameEvents {
        /** Processes the queued changes at the end of the level tick (only does work if something changed). */
        @SubscribeEvent
        public static void onLevelTick(LevelTickEvent.Post event) {
            if (!(event.getLevel() instanceof ServerLevel level)) return;
            Set<BlockPos> set;
            synchronized (PENDING) { set = PENDING.remove(level); }
            CasingCrossData data = CasingCrossData.get(level);
            if (set != null) for (BlockPos pos : set) check(level, data, pos);
            data.tick(level);
        }

        @SubscribeEvent
        public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
            // Mirror ServerPlayerGameMode: sneak + holding an item skips block interaction so placement can proceed.
            var player = event.getEntity();
            if (player.isSecondaryUseActive()
                    && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())) {
                return;
            }
            Level level = event.getLevel();
            BlockPos pos = event.getPos();
            boolean active;
            BlockPos anchor = null;
            if (level instanceof ServerLevel sl) {
                anchor = CasingCrossData.get(sl).anchorOf(pos);
                active = anchor != null && CasingCrossStructure.matches(level, anchor);
            } else {
                active = clientLooksActive(level, pos);
            }
            if (!active) return;
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getHand() != InteractionHand.MAIN_HAND) return;
            if (player instanceof ServerPlayer sp && anchor != null) {
                final BlockPos a = anchor;
                sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new CasingCrossMenu(id, inv, a, CasingCrossData.get((ServerLevel) level).entry(a)),
                        Component.translatable("container.aamoreadditions.casing_cross")), buf -> buf.writeBlockPos(a));
            }
        }
    }

    @EventBusSubscriber(modid = AAMoreAdditions.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        /**
         * Energy for the structure: on the casings and the Empowerer. Display Stands are handled by TileEntityDisplayStandEnergyMixin: AA gives
         * them their own energy storage (their block-entity provider would answer first / conflict).
         * The Empowerer's AA provider returns null (no energy), so ours is used there.
         */
        @SubscribeEvent
        public static void caps(RegisterCapabilitiesEvent event) {
            event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, be, side) -> {
                if (!(level instanceof ServerLevel sl)) return null;
                CasingCrossData.Entry e = CasingCrossData.get(sl).entryAt(pos);
                return e == null ? null : e.energy;
            }, ModBlocks.DECORATIVE_MACHINE_CASING.get(), ActuallyBlocks.EMPOWERER.get());
            // Item handler: casings only here. The Empowerer and Display Stands already have AA providers, handled by TileEntityBaseItemHandlerMixin.
            event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
                if (!(level instanceof ServerLevel sl)) return null;
                CasingCrossData.Entry e = CasingCrossData.get(sl).entryAt(pos);
                return e == null ? null : e.external;
            }, ModBlocks.DECORATIVE_MACHINE_CASING.get());
        }
    }

    /** Client has no saved data: treat as active when the cell is part of a correctly built cross. Server decides for real. */
    public static boolean clientLooksActive(Level level, BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();
        if (!isPart(b)) return false;
        for (var p : CasingCrossStructure.PARTS) {
            if (p.block().get() == b && CasingCrossStructure.matches(level, pos.subtract(p.offset()))) return true;
        }
        return false;
    }

    private static boolean loaded(ServerLevel level, BlockPos anchor) {
        for (var p : CasingCrossStructure.PARTS) if (!level.isLoaded(anchor.offset(p.offset()))) return false;
        return true;
    }

    static void check(ServerLevel level, CasingCrossData data, BlockPos pos) {
        // 1. an active structure owning this cell: still intact?
        BlockPos owner = data.anchorOf(pos);
        if (owner != null && loaded(level, owner) && !CasingCrossStructure.matches(level, owner)) deactivate(level, data, owner);
        // 2. could this change complete a new structure?
        Block b = level.getBlockState(pos).getBlock();
        if (!isPart(b)) return;
        for (var p : CasingCrossStructure.PARTS) {
            if (p.block().get() != b) continue;
            BlockPos cand = pos.subtract(p.offset());
            if (data.entry(cand) != null || !data.cellsFree(cand) || !loaded(level, cand)) continue;
            if (CasingCrossStructure.matches(level, cand)) {
                data.activate(cand);
                invalidate(level, cand);
                level.playSound(null, cand, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1F, 1F);
            }
        }
    }

    static void deactivate(ServerLevel level, CasingCrossData data, BlockPos anchor) {
        // close any open GUI first so nothing is moved into a dead inventory
        for (ServerPlayer sp : level.getServer().getPlayerList().getPlayers()) {
            if (sp.containerMenu instanceof CasingCrossMenu m && m.anchor.equals(anchor) && m.isServerFor(level)) sp.closeContainer();
        }
        CasingCrossData.Entry e = data.remove(anchor);
        // Clear each surviving cell's client-only display, even if its last update was a chunk-entry tag.
        CasingCrossDisplay.sync(level, anchor, true);
        if (e != null) {
            e.upgrades.drainOverflow(level, anchor);
            List<net.minecraft.world.item.ItemStack> drops = new ArrayList<>();
            for (int i = 0; i < e.inv.getSlots(); i++) {
                var st = e.inv.getStackInSlot(i);
                if (!st.isEmpty()) drops.add(st.copy());
                e.inv.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
            for (int i = 0; i < e.upgrades.getSlots(); i++) {
                var st = e.upgrades.getStackInSlot(i);
                if (!st.isEmpty()) drops.add(st.copy());
                e.upgrades.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
            for (var st : drops) Containers.dropItemStack(level, anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5, st);
            // stored energy is lost when the structure breaks
        }
        invalidate(level, anchor);
        level.playSound(null, anchor, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1F, 1F);
    }

    private static void invalidate(ServerLevel level, BlockPos anchor) {
        for (var p : CasingCrossStructure.PARTS) level.invalidateCapabilities(anchor.offset(p.offset()));
    }
}
