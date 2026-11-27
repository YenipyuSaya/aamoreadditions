package com.aamoreadditions.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Pure geometry helper (no Minecraft types, unit-tested): splits one rectangular model quad into one quad per opaque texel.
 * The quad is given as 4 corner positions plus the texel-space coordinates (a = column, b = row, in texels of the sprite frame) of
 * each corner; positions are an affine function of (a, b), which is solved from corners 0, 1 and 2.
 */
final class RingTexelQuads {
    private RingTexelQuads() {}

    /**
     * @param pos       4 corners x 3 (x, y, z)
     * @param tex       4 corners x 2 (a, b) in texels
     * @param uv        {u0, u1, v0, v1} atlas bounds of the sprite frame; texel (a, b) maps to u0 + a/w*(u1-u0), v0 + b/h*(v1-v0)
     * @param opaque    (column, row) -> true when the texel is not fully transparent
     * @param lift      offset (x, y, z) added to every output position
     * @return list of float[20] = 4 x (x, y, z, atlasU, atlasV)
     */
    static List<float[]> split(float[][] pos, float[][] tex, float[] uv, int w, int h, BiPredicate<Integer, Integer> opaque, float[] lift) {
        List<float[]> out = new ArrayList<>();
        double d1a = tex[1][0] - tex[0][0], d1b = tex[1][1] - tex[0][1], d2a = tex[2][0] - tex[0][0], d2b = tex[2][1] - tex[0][1];
        double det = d1a * d2b - d1b * d2a;
        if (Math.abs(det) < 1.0E-9) return out;
        double[][] m = new double[3][2];
        for (int axis = 0; axis < 3; axis++) {
            double e1 = pos[1][axis] - pos[0][axis], e2 = pos[2][axis] - pos[0][axis];
            m[axis][0] = (e1 * d2b - e2 * d1b) / det;
            m[axis][1] = (e2 * d1a - e1 * d2a) / det;
        }
        float aLo = Float.MAX_VALUE, aHi = -Float.MAX_VALUE, bLo = Float.MAX_VALUE, bHi = -Float.MAX_VALUE;
        for (float[] t : tex) {
            aLo = Math.min(aLo, t[0]); aHi = Math.max(aHi, t[0]);
            bLo = Math.min(bLo, t[1]); bHi = Math.max(bHi, t[1]);
        }
        int aMin = Math.max((int) Math.floor(aLo + 1.0E-4), 0), aMax = Math.min((int) Math.ceil(aHi - 1.0E-4), w);
        int bMin = Math.max((int) Math.floor(bLo + 1.0E-4), 0), bMax = Math.min((int) Math.ceil(bHi - 1.0E-4), h);
        int[][] corner = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        for (int tx = aMin; tx < aMax; tx++) {
            for (int ty = bMin; ty < bMax; ty++) {
                if (!opaque.test(tx, ty)) continue;
                float[] q = new float[20];
                for (int k = 0; k < 4; k++) {
                    double a = tx + corner[k][0], b = ty + corner[k][1];
                    for (int axis = 0; axis < 3; axis++) {
                        q[k * 5 + axis] = (float) (pos[0][axis] + m[axis][0] * (a - tex[0][0]) + m[axis][1] * (b - tex[0][1])) + lift[axis];
                    }
                    q[k * 5 + 3] = (float) (uv[0] + a / w * (uv[1] - uv[0]));
                    q[k * 5 + 4] = (float) (uv[2] + b / h * (uv[3] - uv[2]));
                }
                out.add(q);
            }
        }
        return out;
    }
}
