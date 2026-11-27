package com.aamoreadditions.booster;

import com.aamoreadditions.effect.UpgradeConfig;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomPlacer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomface;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPlayerInterface;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

/**
 * Phantom-booster stack evaluation, range take-over, binding rules, and overload explosions.
 *
 * <p>Vertical Digger: Transfinite and Dimensional have no special binding; each counts as an
 * Advanced (×5) multiplier. Overload still explodes if those specials are stacked with each
 * other or with a lower tier above the digger.
 */
public final class BoosterEffects {
    public enum Machine {
        /** Phantomfaces and Phantom Placer / Breaker. Special binding applies. */
        FACE,
        /** Specials count as Advanced multipliers; no special binding. Overload still applies. */
        DIGGER,
        /** All tiers allowed, including special binding. Distance check is squared. */
        INTERFACE
    }

    private record ExplosionKey(ResourceKey<Level> dim, BlockPos pos) {}

    /** Last exploded stack signature per machine, so a ticking {@code upgradeRange} cannot retrigger. */
    private static final Map<ExplosionKey, Integer> EXPLODED = new ConcurrentHashMap<>();
    /** Re-entry guard so dropping boosters cannot retrigger overload via setBlockState. */
    private static final ThreadLocal<BlockPos> DROPPING = new ThreadLocal<>();

    private BoosterEffects() {}

    public static Machine kindAt(Level level, BlockPos pos) {
        if (level == null || pos == null) return Machine.FACE;
        return kindOf(level.getBlockEntity(pos));
    }

    public static Machine kindOf(BlockEntity be) {
        if (be instanceof TileEntityVerticalDigger) return Machine.DIGGER;
        if (be instanceof TileEntityPlayerInterface) return Machine.INTERFACE;
        return Machine.FACE;
    }

    public static boolean isBoosterMachine(BlockEntity be) {
        return be instanceof TileEntityPhantomface
                || be instanceof TileEntityPhantomPlacer
                || be instanceof TileEntityVerticalDigger
                || be instanceof TileEntityPlayerInterface;
    }

    public static boolean specialsAsAdvanced(Machine kind) {
        return kind == Machine.DIGGER;
    }

    /** Walk upward from immediately above the machine; a non-booster or the cap stops the stack. */
    public static BoosterScan scan(IntFunction<BoosterTier> atOffset, int maxStack) {
        int cap = Math.max(1, maxStack);
        List<BoosterTier> layers = new ArrayList<>(Math.min(cap, 8));
        for (int i = 0; i < cap; i++) {
            BoosterTier t = atOffset.apply(i);
            if (t == null) break;
            layers.add(t);
        }
        return BoosterScan.of(layers);
    }

    public static BoosterScan scan(Level level, BlockPos machinePos) {
        if (level == null || machinePos == null) return BoosterScan.of(List.of());
        int cap = UpgradeConfig.phantomBoosterMaxStack();
        return scan(i -> BoosterTier.of(level.getBlockState(machinePos.above(1 + i)).getBlock()), cap);
    }

    public static int multipliedRange(int defaultRange, BoosterScan scan, Machine kind) {
        if (defaultRange <= 0) return 0;
        if (scan.overloaded()) return 0;
        BoosterScan.Binding binding = scan.binding(specialsAsAdvanced(kind));
        if (binding == BoosterScan.Binding.TRANSFINITE || binding == BoosterScan.Binding.DIMENSIONAL) {
            return Integer.MAX_VALUE;
        }
        long value = (long) defaultRange * scan.multiplier(
                UpgradeConfig.phantomBoosterAaMultiplier(),
                UpgradeConfig.phantomBoosterAdvancedMultiplier(),
                specialsAsAdvanced(kind));
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) value;
    }

    /**
     * Replacement for {@link TileEntityPhantomface#upgradeRange(int, Level, BlockPos)}.
     * Explodes on the rising edge of an overloaded stack (placement / piston / first recalc).
     */
    public static int upgradeRange(int defaultRange, Level level, BlockPos pos) {
        return upgradeRange(defaultRange, level, pos, kindAt(level, pos));
    }

    public static int upgradeRange(int defaultRange, Level level, BlockPos pos, Machine kind) {
        BoosterScan scan = scan(level, pos);
        maybeExplode(level, pos, scan);
        return multipliedRange(defaultRange, scan, kind);
    }

    public static BoosterScan.Binding bindingAt(Level level, BlockPos pos) {
        return scan(level, pos).binding(specialsAsAdvanced(kindAt(level, pos)));
    }

    public static boolean allowsCrossDimension(BlockEntity tile) {
        if (tile == null || tile.getLevel() == null) return false;
        return bindingAt(tile.getLevel(), tile.getBlockPos()) == BoosterScan.Binding.DIMENSIONAL;
    }

    /**
     * Distance / dimension / chunk-loaded check that replaces AA {@code isBoundThingInRange}
     * once a bound position exists.
     */
    public static boolean isBoundInRange(BlockEntity tile, BlockPos bound, int range) {
        if (tile == null || bound == null) return false;
        Level level = tile.getLevel();
        if (level == null) return false;
        BoosterScan.Binding binding = bindingAt(level, tile.getBlockPos());
        if (binding == BoosterScan.Binding.DISABLED) return false;
        ResourceKey<Level> boundDim = BoundWorlds.get(tile);
        if (boundDim == null) boundDim = level.dimension();
        Level targetLevel = resolveLevel(level, boundDim);
        if (targetLevel == null) return false;
        if (!level.isLoaded(tile.getBlockPos()) || !targetLevel.isLoaded(bound)) return false;
        return switch (binding) {
            case DISABLED -> false;
            case DIMENSIONAL -> true;
            case TRANSFINITE -> boundDim == level.dimension();
            case RANGED -> boundDim == level.dimension() && bound.distSqr(tile.getBlockPos()) <= (double) range * (double) range;
        };
    }

    public static Level resolveLevel(Level host, ResourceKey<Level> dim) {
        if (host == null || dim == null) return null;
        if (dim == host.dimension()) return host;
        MinecraftServer server = host.getServer();
        if (server == null) return null;
        return server.getLevel(dim);
    }

    /**
     * Called from block-change hooks so overload explodes on placement or piston push, not only
     * on the next machine tick.
     */
    public static void onBlockChanged(Level level, BlockPos changed, BlockState oldState, BlockState newState) {
        if (level == null || level.isClientSide || changed == null) return;
        int cap = UpgradeConfig.phantomBoosterMaxStack();
        for (int i = 0; i <= cap; i++) {
            BlockPos machine = changed.below(i);
            if (isBoosterMachine(level.getBlockEntity(machine))) {
                maybeExplode(level, machine, scan(level, machine));
            }
        }
    }

    static void maybeExplode(Level level, BlockPos machine, BoosterScan scan) {
        if (level == null || level.isClientSide || machine == null) return;
        if (machine.equals(DROPPING.get())) return;
        ExplosionKey key = new ExplosionKey(level.dimension(), machine.immutable());
        if (!scan.overloaded()) {
            EXPLODED.remove(key);
            return;
        }
        int sig = scan.signature();
        Integer previous = EXPLODED.get(key);
        if (previous != null && previous == sig) return;
        EXPLODED.put(key, sig);
        DROPPING.set(machine.immutable());
        try {
            triggerOverload(level, machine);
            EXPLODED.remove(key);
        } finally {
            DROPPING.remove();
        }
    }

    /**
     * Visual TNT burst (no block or item destruction) at the machine, 4 void-like damage in a
     * radius-3 sphere to living entities only, then every consecutive booster above the machine
     * is broken and dropped.
     */
    static void triggerOverload(Level level, BlockPos machine) {
        double x = machine.getX() + 0.5;
        double y = machine.getY() + 0.5;
        double z = machine.getZ() + 0.5;
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        float pitch = (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F;
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, BoosterOverload.SOUND_VOLUME, pitch);
        var source = BoosterOverload.damageSource(level);
        AABB box = new AABB(x - BoosterOverload.RADIUS, y - BoosterOverload.RADIUS, z - BoosterOverload.RADIUS,
                x + BoosterOverload.RADIUS, y + BoosterOverload.RADIUS, z + BoosterOverload.RADIUS);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, BoosterOverload::isTarget)) {
            if (BoosterOverload.inRadius(living.distanceToSqr(x, y, z))) {
                living.hurt(source, BoosterOverload.DAMAGE);
            }
        }
        dropBoostersAbove(level, machine);
    }

    /** Consecutive boosters above {@code machine}, including those past the stack cap, until a non-booster gap. */
    static int dropBoostersAbove(Level level, BlockPos machine) {
        int dropped = 0;
        BlockPos pos = machine.above();
        while (level.isInWorldBounds(pos) && BoosterTier.of(level.getBlockState(pos).getBlock()) != null) {
            level.destroyBlock(pos, true);
            dropped++;
            pos = pos.above();
        }
        return dropped;
    }

    /** Visible for tests: reset explosion de-duplication. */
    public static void resetExplosionMemory() {
        EXPLODED.clear();
    }
}
