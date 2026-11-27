package com.aamoreadditions.client;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Lives in the client package's test tree but reaches the package-private helper by reflection (it is pure geometry). */
class RingTexelQuadsTest {
    @SuppressWarnings("unchecked")
    private static List<float[]> split(float[][] pos, float[][] tex, float[] uv, int w, int h, java.util.function.BiPredicate<Integer, Integer> opaque, float[] lift) throws Exception {
        Class<?> c = Class.forName("com.aamoreadditions.client.render.RingTexelQuads");
        var m = c.getDeclaredMethod("split", float[][].class, float[][].class, float[].class, int.class, int.class, java.util.function.BiPredicate.class, float[].class);
        m.setAccessible(true);
        return (List<float[]>) m.invoke(null, pos, tex, uv, w, h, opaque, lift);
    }

    private static BufferedImage ring() throws Exception {
        return ImageIO.read(Path.of("src/main/resources/assets/aamoreadditions/textures/block/dimensional_phantom_booster_rings.png").toFile());
    }

    /** Plane x=11/16, z 5..11 over side columns a 6..12, y 13..4 over rows b 0..9 (top to bottom). */
    private static float[][] pos(boolean mirrored) {
        float z0 = mirrored ? 11 / 16F : 5 / 16F, z1 = mirrored ? 5 / 16F : 11 / 16F;
        return new float[][]{{11 / 16F, 13 / 16F, z0}, {11 / 16F, 4 / 16F, z0}, {11 / 16F, 4 / 16F, z1}, {11 / 16F, 13 / 16F, z1}};
    }
    private static final float[][] TEX = {{6, 0}, {6, 9}, {12, 9}, {12, 0}};

    @Test void oneQuadPerOpaqueRingTexelOnEveryRingFace() throws Exception {
        BufferedImage img = ring();
        for (boolean mirrored : new boolean[]{false, true}) {
            List<float[]> quads = split(pos(mirrored), TEX, new float[]{0.25F, 0.5F, 0.25F, 0.5F}, 32, 32,
                    (x, y) -> (img.getRGB(x, y) >>> 24) != 0, new float[]{0, 0, 0});
            assertEquals(30, quads.size(), "5 rings x 6 columns of one side");
        }
    }

    @Test void texelCornersFollowTheQuadMappingIncludingMirroring() throws Exception {
        // texel column 6 (first of the side), row 0 (top row) must sit at the z corner that the quad maps column 6 to, y from 13 to 12
        for (boolean mirrored : new boolean[]{false, true}) {
            List<float[]> quads = split(pos(mirrored), TEX, new float[]{0, 1, 0, 1}, 32, 32, (x, y) -> x == 6 && y == 0, new float[]{0, 0, 0});
            assertEquals(1, quads.size());
            float[] q = quads.get(0);
            float zStart = mirrored ? 11 / 16F : 5 / 16F, zStep = (mirrored ? -1 : 1) / 16F;
            // corners (0,0),(0,1),(1,1),(1,0) = (col, row), (col, row+1), (col+1, row+1), (col+1, row)
            assertEquals(zStart, q[2], 1e-6);
            assertEquals(13 / 16F, q[1], 1e-6);
            assertEquals(12 / 16F, q[6], 1e-6); // 9 texel rows span the 9-unit plane: one texel = one unit
            assertEquals(zStart + zStep, q[12], 1e-6);
            assertEquals(6 / 32F, q[3], 1e-6);
            assertEquals(7 / 32F, q[18], 1e-6);
            assertEquals(1 / 32F, q[9], 1e-6);
        }
    }

    @Test void liftMovesEveryVertex() throws Exception {
        List<float[]> quads = split(pos(false), TEX, new float[]{0, 1, 0, 1}, 32, 32, (x, y) -> x == 6 && y == 0, new float[]{0.001F, 0, 0});
        assertEquals(11 / 16F + 0.001F, quads.get(0)[0], 1e-6);
    }
}
