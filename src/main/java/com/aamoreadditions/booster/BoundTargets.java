package com.aamoreadditions.booster;

import de.ellpeck.actuallyadditions.api.tile.IPhantomTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Resolves a phantom-style tile's bound target in the dimension stored for that bind.
 *
 * <p>This is the shared entry point for Phantomfaces, the Phantom Placer/Breaker, and any
 * future AAMA interface that binds a {@link BlockPos} plus a dimension. Callers must not
 * use {@code tile.getLevel().getBlockEntity(boundPos)} (or {@code getCapability} /
 * {@code getBlockState} on the host level at that pos) when the bind can be cross-dimension.
 * The Player Interface does not bind a block; it resolves a player entity and is handled
 * separately.
 *
 * <p>Rules:
 * <ul>
 *   <li>The bound dimension is the stored dimension of the <em>bound block</em>
 *       ({@link BoundWorlds}), not the player's dimension.</li>
 *   <li>The target level is {@code server.getLevel(storedDim)}. Nothing here loads a chunk
 *       or a dimension; an unloaded target is simply missing.</li>
 *   <li>{@link #resolve(BlockEntity)} succeeds only when the bind is in range (including
 *       Dimensional / Transfinite rules), the host chunk is loaded, and the target chunk is
 *       already loaded.</li>
 *   <li>Same-dimension ranged and Transfinite binds go through this helper too, so behaviour
 *       there stays aligned with a single code path.</li>
 * </ul>
 */
public final class BoundTargets {
    public record Target(Level level, BlockPos pos) {
        public boolean sameDimension(Level host) {
            return host != null && level.dimension() == host.dimension();
        }

        public BlockState state() {
            return level.getBlockState(pos);
        }

        public @Nullable BlockEntity blockEntity() {
            return level.getBlockEntity(pos);
        }

        public <T, C> @Nullable T capability(BlockCapability<T, C> cap, C context) {
            return level.getCapability(cap, pos, context);
        }
    }

    private record ListenerKey(ResourceKey<Level> dim, BlockPos pos) {}

    /** Last capability-listener registration per phantom tile, so we can refresh after a cross-dim unload. */
    private static final Map<BlockEntity, ListenerKey> LISTENERS = Collections.synchronizedMap(new WeakHashMap<>());

    private BoundTargets() {}

    public static @Nullable BlockPos position(BlockEntity tile) {
        return tile instanceof IPhantomTile phantom ? phantom.getBoundPosition() : null;
    }

    /** Stored dimension of the bound block; falls back to the host level when none is stored. */
    public static @Nullable ResourceKey<Level> dimension(BlockEntity tile) {
        if (tile == null) return null;
        ResourceKey<Level> stored = BoundWorlds.get(tile);
        if (stored != null) return stored;
        return tile.getLevel() != null ? tile.getLevel().dimension() : null;
    }

    /**
     * In-range target whose host and target chunks are already loaded. Empty if unbound, out of
     * range, the dimension is missing, or either chunk is unloaded. Never loads chunks.
     */
    public static Optional<Target> resolve(BlockEntity tile) {
        BlockPos bound = position(tile);
        if (tile == null || bound == null || !(tile instanceof IPhantomTile phantom)) return Optional.empty();
        if (!BoosterEffects.isBoundInRange(tile, bound, phantom.getRange())) return Optional.empty();
        return targetIfLoaded(tile, bound);
    }

    /**
     * Stored bind whose target chunk is already loaded, skipping the range check.
     * Used for hasBoundPosition validation and cache updates. Does not load chunks.
     */
    public static Optional<Target> resolveIfLoaded(BlockEntity tile) {
        BlockPos bound = position(tile);
        if (tile == null || bound == null || tile.getLevel() == null) return Optional.empty();
        return targetIfLoaded(tile, bound);
    }

    private static Optional<Target> targetIfLoaded(BlockEntity tile, BlockPos bound) {
        Level host = tile.getLevel();
        if (host == null || !host.isLoaded(tile.getBlockPos())) return Optional.empty();
        Level target = BoosterEffects.resolveLevel(host, dimension(tile));
        if (target == null || !target.isLoaded(bound)) return Optional.empty();
        return Optional.of(new Target(target, bound));
    }

    /**
     * {@code true} if the bind is the tile itself (same dimension and position) or a loaded
     * {@link IPhantomTile}. An unloaded target is not illegal: the bind is kept and treated as
     * out of range until the chunk is present again.
     */
    public static boolean isIllegalBind(BlockEntity tile, BlockPos bound) {
        if (tile == null || bound == null) return false;
        Level host = tile.getLevel();
        ResourceKey<Level> dim = dimension(tile);
        if (host != null && dim == host.dimension() && bound.equals(tile.getBlockPos())) return true;
        Optional<Target> target = targetIfLoaded(tile, bound);
        return target.isPresent() && target.get().blockEntity() instanceof IPhantomTile;
    }

    public static <T> @Nullable T capability(BlockEntity tile, BlockCapability<T, Direction> cap, @Nullable Direction side) {
        return resolve(tile).map(t -> t.capability(cap, side)).orElse(null);
    }

    /**
     * {@code true} when the target is loaded but we have no (or a stale) capability listener for it.
     * Used so a chunk that loads later still refreshes the face's exposed caps. Unloaded targets
     * do not count as a change (avoids a tick-loop of invalidations).
     */
    public static boolean listenerOutOfDate(BlockEntity tile) {
        Optional<Target> target = resolveIfLoaded(tile);
        if (target.isEmpty()) return false;
        ListenerKey current = LISTENERS.get(tile);
        Target t = target.get();
        return current == null
                || !current.dim().equals(t.level().dimension())
                || !current.pos().equals(t.pos());
    }

    public static void rememberListener(BlockEntity tile, Target target) {
        if (tile == null || target == null) return;
        LISTENERS.put(tile, new ListenerKey(target.level().dimension(), target.pos().immutable()));
    }

    public static void forgetListener(BlockEntity tile) {
        if (tile != null) LISTENERS.remove(tile);
    }

    /**
     * Level to register a capability invalidation listener on, if the target chunk is loaded.
     * {@code empty} means do not register (and do not fall back to the host level, which would
     * listen at the wrong coordinates in the wrong dimension).
     */
    public static Optional<ServerLevel> listenerLevel(BlockEntity tile) {
        return resolveIfLoaded(tile).map(Target::level).filter(ServerLevel.class::isInstance).map(ServerLevel.class::cast);
    }

    /** Drops listener bookkeeping and invalidates faces bound into a level that is unloading. */
    public static void onLevelUnload(Level level) {
        if (level == null) return;
        ResourceKey<Level> dim = level.dimension();
        LISTENERS.entrySet().removeIf(e -> {
            if (!e.getValue().dim().equals(dim)) return false;
            BlockEntity tile = e.getKey();
            if (tile != null && !tile.isRemoved()) tile.invalidateCapabilities();
            return true;
        });
    }

    public static void resetListenerMemory() {
        LISTENERS.clear();
    }
}
