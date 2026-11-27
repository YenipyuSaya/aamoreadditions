package com.aamoreadditions.fake;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * One dedicated fake player per machine tile, never the shared {@code FakePlayerFactory.getMinecraft} player.
 * The profile name is shared; the UUID is deterministic from dimension + block position so it is stable across reloads.
 */
public final class MachineFakePlayers {
    private MachineFakePlayers() {}

    /** 14 characters (within the 16-character profile name limit). */
    public static final String NAME = "[AAMA Machine]";

    private static final Map<BlockEntity, FakePlayer> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<BlockEntity> CURRENT = new ThreadLocal<>();
    /** When a machine acts in a different already-loaded level (Dimensional bind), use this work level. */
    private static final ThreadLocal<ServerLevel> WORK_LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<BlockPos> WORK_POS = new ThreadLocal<>();

    public static UUID uuid(ResourceKey<Level> dimension, BlockPos pos) {
        return uuid(dimension.location(), pos);
    }

    public static UUID uuid(ResourceLocation dimension, BlockPos pos) {
        return UUID.nameUUIDFromBytes(("aamoreadditions:machine:" + dimension + ":"
                + pos.getX() + ":" + pos.getY() + ":" + pos.getZ()).getBytes(StandardCharsets.UTF_8));
    }

    public static GameProfile profile(ResourceKey<Level> dimension, BlockPos pos) {
        return new GameProfile(uuid(dimension, pos), NAME);
    }

    /** The per-tile player, moved to the tile's centre. Cached until the tile is removed or its level unloads. */
    public static FakePlayer of(BlockEntity be) {
        ServerLevel work = WORK_LEVEL.get();
        if (work != null && CURRENT.get() == be) {
            // Cross-dimension work: a player in the already-loaded target level, not the cached host player.
            BlockPos at = WORK_POS.get() != null ? WORK_POS.get() : be.getBlockPos();
            FakePlayer fp = new FakePlayer(work, profile(be.getLevel() != null ? be.getLevel().dimension() : work.dimension(), be.getBlockPos()));
            fp.setPos(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
            return fp;
        }
        if (!(be.getLevel() instanceof ServerLevel level))
            throw new IllegalStateException("Machine fake player requested without a server level");
        FakePlayer fp = CACHE.get(be);
        if (fp == null || fp.level() != level) {
            fp = new FakePlayer(level, profile(level.dimension(), be.getBlockPos()));
            CACHE.put(be, fp);
        }
        BlockPos p = be.getBlockPos();
        fp.setPos(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
        return fp;
    }

    /** Bind the machine whose code is about to call an AA helper that has no BlockEntity argument. */
    public static void bind(BlockEntity be) { bind(be, null, null); }

    /**
     * Bind the machine and, when {@code workLevel} is a different already-loaded level, have
     * {@link #of(BlockEntity)} / {@link #current()} use a player in that level at {@code workPos}.
     */
    public static void bind(BlockEntity be, ServerLevel workLevel, BlockPos workPos) {
        CURRENT.set(be);
        if (workLevel != null) WORK_LEVEL.set(workLevel); else WORK_LEVEL.remove();
        if (workPos != null) WORK_POS.set(workPos); else WORK_POS.remove();
    }

    public static void unbind() {
        CURRENT.remove();
        WORK_LEVEL.remove();
        WORK_POS.remove();
    }

    /** The player for the bound machine, already positioned; {@code null} if nothing is bound. */
    public static FakePlayer current() {
        BlockEntity be = CURRENT.get();
        return be == null ? null : of(be);
    }

    public static void release(BlockEntity be) { CACHE.remove(be); }

    public static void releaseLevel(Level level) {
        CACHE.entrySet().removeIf(e -> e.getValue() != null && e.getValue().level() == level);
    }

    /** Set the main-hand stack for an action, then put back whatever was there. */
    public static <T> T withMainHand(FakePlayer fp, ItemStack stack, Supplier<T> action) {
        var inv = fp.getInventory();
        int slot = inv.selected;
        ItemStack previous = inv.items.get(slot);
        inv.items.set(slot, stack);
        try {
            return action.get();
        } finally {
            inv.items.set(slot, previous);
        }
    }
}
