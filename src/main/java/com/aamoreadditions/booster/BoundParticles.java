package com.aamoreadditions.booster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Portal particles on a phantom bind. Same-dimension binds keep AA's client
 * {@code renderParticles}. Cross-dimension binds spawn the same PORTAL burst
 * on the bound block in the already-loaded target {@link ServerLevel}, once per
 * host server tick at AA's 50% rate, without loading chunks.
 *
 * <p>{@link ServerLevel#sendParticles} is called with {@code count = 0} so the
 * packet carries exact position and velocity (one particle) and is still limited
 * to players within 32 blocks.
 */
public final class BoundParticles {
    public record Burst(double x, double y, double z, double vx, double vy, double vz) {}

    private BoundParticles() {}

    /** AA {@code renderParticles} position and velocity around {@code bound}. */
    public static Burst sample(BlockPos bound, RandomSource random) {
        double y = bound.getY() + random.nextFloat();
        int i1 = random.nextInt(2) * 2 - 1;
        int j1 = random.nextInt(2) * 2 - 1;
        double vy = (random.nextFloat() - 0.5D) * 0.125D;
        double z = bound.getZ() + 0.5D + 0.25D * j1;
        double vz = random.nextFloat() * 1.0F * j1;
        double x = bound.getX() + 0.5D + 0.25D * i1;
        double vx = random.nextFloat() * 1.0F * i1;
        return new Burst(x, y, z, vx, vy, vz);
    }

    /**
     * If the host has a cross-dimension bind and both chunks are already loaded,
     * maybe send one PORTAL particle at the bound block in the target level.
     * Same-dimension binds are left to AA's client path. Never loads chunks.
     *
     * @return {@code true} if a particle packet was sent
     */
    public static boolean trySpawn(BlockEntity host) {
        if (host == null || host.getLevel() == null || host.getLevel().isClientSide) return false;
        var dim = BoundTargets.dimension(host);
        if (dim == null || dim == host.getLevel().dimension()) return false;
        var target = BoundTargets.resolveIfLoaded(host);
        if (target.isEmpty() || !(target.get().level() instanceof ServerLevel server)) return false;
        RandomSource random = host.getLevel().random;
        if (random.nextInt(2) != 0) return false;
        Burst burst = sample(target.get().pos(), random);
        // count 0: one particle at (x,y,z) with velocity (vx,vy,vz) * speed; speed 1 keeps AA's velocities.
        server.sendParticles(ParticleTypes.PORTAL, burst.x(), burst.y(), burst.z(), 0, burst.vx(), burst.vy(), burst.vz(), 1.0);
        return true;
    }
}
