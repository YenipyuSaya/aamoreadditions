package com.aamoreadditions.booster;

import de.ellpeck.actuallyadditions.api.tile.IPhantomTile;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Sidecar synced to the client with the bound block id (for the HUD name) and whether
 * the bind is currently in range / the right type / loaded ({@code isBoundThingInRange}
 * on the server). Written on the same {@code writeSyncableNBT} SYNC/SAVE path as
 * {@link BoundWorlds}.
 */
public final class BoundDisplays {
    private static final String TAG_BLOCK = "aamaBoundBlock";
    private static final String TAG_OK = "aamaBoundOk";
    private static final Map<BlockEntity, Snapshot> DATA = new WeakHashMap<>();

    public record Snapshot(ResourceLocation block, boolean ok) {}

    private BoundDisplays() {}

    public static ResourceLocation blockId(BlockEntity tile) {
        Snapshot s = tile == null ? null : DATA.get(tile);
        return s == null ? null : s.block();
    }

    public static boolean boundOk(BlockEntity tile) {
        Snapshot s = tile == null ? null : DATA.get(tile);
        return s != null && s.ok();
    }

    /** True when the client must use {@link #boundOk(BlockEntity)} instead of {@code BoundTargets.resolve}. */
    public static boolean useSyncedInRange(BlockEntity tile) {
        Level level = tile == null ? null : tile.getLevel();
        return level != null && level.isClientSide;
    }

    /**
     * Recompute from the live server bind. Keeps the last known block id when the target
     * is unloaded so the HUD can still show a name. Returns true if the snapshot changed.
     */
    public static boolean refresh(BlockEntity tile) {
        if (tile == null || tile.getLevel() == null || tile.getLevel().isClientSide) return false;
        ResourceLocation block = null;
        boolean ok = false;
        if (tile instanceof IPhantomTile phantom && phantom.getBoundPosition() != null) {
            ok = phantom.isBoundThingInRange();
            var target = BoundTargets.resolveIfLoaded(tile);
            if (target.isPresent()) {
                block = BuiltInRegistries.BLOCK.getKey(target.get().state().getBlock());
            } else {
                Snapshot prev = DATA.get(tile);
                if (prev != null) block = prev.block();
            }
        }
        Snapshot next = new Snapshot(block, ok);
        if (Objects.equals(DATA.get(tile), next)) return false;
        if (block == null && !ok) DATA.remove(tile);
        else DATA.put(tile, next);
        return true;
    }

    /** {@link #refresh} and, if the snapshot changed, send a block-entity update to nearby players. */
    public static void sync(BlockEntity tile) {
        if (!refresh(tile)) return;
        if (tile instanceof TileEntityBase base) base.sendUpdate();
    }

    public static void write(BlockEntity tile, CompoundTag tag) {
        if (tile == null || tag == null) return;
        Snapshot s = DATA.get(tile);
        if (s != null && s.block() != null) tag.putString(TAG_BLOCK, s.block().toString());
        tag.putBoolean(TAG_OK, s != null && s.ok());
    }

    public static void read(BlockEntity tile, CompoundTag tag) {
        if (tile == null || tag == null) return;
        ResourceLocation block = tag.contains(TAG_BLOCK) ? ResourceLocation.tryParse(tag.getString(TAG_BLOCK)) : null;
        boolean ok = tag.contains(TAG_OK) && tag.getBoolean(TAG_OK);
        if (block == null && !ok) DATA.remove(tile);
        else DATA.put(tile, new Snapshot(block, ok));
    }

    public static Block defaultStateBlock(BlockEntity tile) {
        ResourceLocation id = blockId(tile);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return null;
        return BuiltInRegistries.BLOCK.get(id);
    }

    public static void resetMemory() {
        DATA.clear();
    }
}
