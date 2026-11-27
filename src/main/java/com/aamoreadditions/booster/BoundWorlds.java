package com.aamoreadditions.booster;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/** Sidecar for a phantom tile's bound dimension (AA stores only a {@link net.minecraft.core.BlockPos}). */
public final class BoundWorlds {
    private static final String TAG = "aamaBoundDim";
    private static final Map<BlockEntity, ResourceKey<Level>> DIM = new WeakHashMap<>();

    private BoundWorlds() {}

    public static void set(BlockEntity tile, ResourceKey<Level> dim) {
        if (tile == null) return;
        if (dim == null) DIM.remove(tile);
        else DIM.put(tile, dim);
    }

    public static ResourceKey<Level> get(BlockEntity tile) {
        if (tile == null) return null;
        ResourceKey<Level> stored = DIM.get(tile);
        if (stored != null) return stored;
        return tile.getLevel() != null ? tile.getLevel().dimension() : null;
    }

    public static void write(BlockEntity tile, CompoundTag tag) {
        ResourceKey<Level> dim = DIM.get(tile);
        if (dim != null) tag.putString(TAG, dim.location().toString());
    }

    public static void read(BlockEntity tile, CompoundTag tag) {
        if (!tag.contains(TAG)) return;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG));
        if (id != null) DIM.put(tile, ResourceKey.create(Registries.DIMENSION, id));
    }
}
