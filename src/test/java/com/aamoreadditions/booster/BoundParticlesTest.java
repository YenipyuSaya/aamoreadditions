package com.aamoreadditions.booster;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundParticlesTest {

    @Test void sampleMatchesAaRenderParticles() {
        var bound = new BlockPos(10, 20, 30);
        long seed = 123456789L;
        var expected = aaBurst(bound, RandomSource.create(seed));
        var actual = BoundParticles.sample(bound, RandomSource.create(seed));
        assertEquals(expected.x(), actual.x());
        assertEquals(expected.y(), actual.y());
        assertEquals(expected.z(), actual.z());
        assertEquals(expected.vx(), actual.vx());
        assertEquals(expected.vy(), actual.vy());
        assertEquals(expected.vz(), actual.vz());
    }

    @Test void sampleStaysOnTheBoundBlockColumn() {
        var bound = new BlockPos(-4, 70, 12);
        var random = RandomSource.create(99L);
        for (int i = 0; i < 40; i++) {
            var b = BoundParticles.sample(bound, random);
            assertTrue(b.y() >= bound.getY() && b.y() < bound.getY() + 1.0, "y " + b.y());
            assertEquals(0.25, Math.abs(b.x() - (bound.getX() + 0.5)), 1e-9);
            assertEquals(0.25, Math.abs(b.z() - (bound.getZ() + 0.5)), 1e-9);
            assertTrue(Math.abs(b.vy()) <= 0.0625 + 1e-9);
            assertTrue(Math.abs(b.vx()) <= 1.0 + 1e-9);
            assertTrue(Math.abs(b.vz()) <= 1.0 + 1e-9);
            // Horizontal velocity sign matches the offset side, as in AA.
            assertEquals(Math.signum(b.x() - (bound.getX() + 0.5)), Math.signum(b.vx()));
            assertEquals(Math.signum(b.z() - (bound.getZ() + 0.5)), Math.signum(b.vz()));
        }
    }

    @Test void fiftyPercentGateUsesNextIntTwo() throws Exception {
        var src = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/aamoreadditions/booster/BoundParticles.java"));
        assertTrue(src.contains("random.nextInt(2) != 0"));
        assertTrue(src.contains("ParticleTypes.PORTAL"));
        assertTrue(src.contains("sendParticles"));
        // count 0 = one particle with exact velocity; default sendParticles is 32-block range
        assertTrue(src.contains(", 0, burst.vx()"));
        assertTrue(src.contains("resolveIfLoaded"));
        assertTrue(src.contains("isClientSide"));
        assertTrue(src.contains("dim == host.getLevel().dimension()"));
    }

    /** Same arithmetic and random-call order as AA TileEntityPhantomface.renderParticles. */
    private static BoundParticles.Burst aaBurst(BlockPos bound, RandomSource random) {
        double d1 = bound.getY() + random.nextFloat();
        int i1 = random.nextInt(2) * 2 - 1;
        int j1 = random.nextInt(2) * 2 - 1;
        double d4 = (random.nextFloat() - 0.5D) * 0.125D;
        double d2 = bound.getZ() + 0.5D + 0.25D * j1;
        double d5 = random.nextFloat() * 1.0F * j1;
        double d0 = bound.getX() + 0.5D + 0.25D * i1;
        double d3 = random.nextFloat() * 1.0F * i1;
        return new BoundParticles.Burst(d0, d1, d2, d3, d4, d5);
    }
}
