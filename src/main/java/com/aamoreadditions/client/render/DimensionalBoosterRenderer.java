package com.aamoreadditions.client.render;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.block.DimensionalPhantomBoosterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

/**
 * Client effects of the Dimensional Phantom Booster (simulated beforehand in the art preview):
 * <ol>
 *   <li><b>Ring glint</b>: the vanilla enchantment glint, drawn only over the opaque texels of the ring texture. The texel quads are
 *       derived from the baked model's own ring quads (so they follow the model UV layout exactly) and use the atlas UVs of those
 *       texels, like a vanilla foil pass does.</li>
 *   <li><b>Energy shell</b>: the vanilla charged-creeper swirl ({@code creeper_armor.png}, additive, colour 0x808080, UV scroll
 *       0.01 per tick on both axes as in {@code EnergySwirlLayer}/{@code CreeperPowerLayer}) on the four side faces of the box
 *       [4.5,3,4.5]-[11.5,14,11.5] (model units).</li>
 * </ol>
 */
public class DimensionalBoosterRenderer implements BlockEntityRenderer<DimensionalPhantomBoosterBlockEntity> {
    public static final ResourceLocation RING_SPRITE = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "block/dimensional_phantom_booster_rings");

    /** Shell box in model units (matches the approved art: bottom at the base's top, top at the cap's bottom). */
    static final float SX0 = 4.5F, SX1 = 11.5F, SY0 = 3F, SY1 = 14F;
    /** creeper_armor.png region used for the shell: u 16..44, v 16..27 of the 64x32 texture, 1 texel per model unit. */
    private static final float TEX_W = 64F, TEX_H = 32F, REGION_U = 16F, REGION_V = 16F, SIDE_W = 7F;
    private static final int SWIRL_COLOR = 0xFF808080; // vanilla EnergySwirlLayer colour (-8355712)
    private static final float GLINT_LIFT = 0.0015F;   // blocks; keeps the glint quads in front of the ring planes

    private BakedModel cachedModel;
    private List<float[]> glintQuads = List.of();

    public DimensionalBoosterRenderer() {}

    @Override
    public void render(DimensionalPhantomBoosterBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) return;
        renderEffects(be.getBlockState(), level.getGameTime() + partialTick, poseStack, buffers, packedLight);
    }

    /** Shared by the block entity renderer (and usable by an item renderer later). {@code time} is in ticks. */
    public void renderEffects(BlockState state, float time, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        PoseStack.Pose pose = poseStack.last();
        rebuildGlintQuads(state);
        if (!glintQuads.isEmpty()) {
            VertexConsumer glint = buffers.getBuffer(BoosterRenderTypes.ringGlint());
            for (float[] q : glintQuads) {
                for (int i = 0; i < 4; i++) {
                    glint.addVertex(pose, q[i * 5], q[i * 5 + 1], q[i * 5 + 2]).setUv(q[i * 5 + 3], q[i * 5 + 4]);
                }
            }
        }
        VertexConsumer shell = buffers.getBuffer(BoosterRenderTypes.energyShell());
        float scroll = time * 0.01F % 1F; // wrapped like vanilla (keeps float precision on long-running worlds)
        drawShell(shell, pose, scroll, packedLight);
    }

    // ---------------------------------------------------------------- energy shell

    private static void drawShell(VertexConsumer c, PoseStack.Pose pose, float scroll, int light) {
        float x0 = SX0 / 16F, x1 = SX1 / 16F, y0 = SY0 / 16F, y1 = SY1 / 16F;
        float v0 = (REGION_V / TEX_H) + scroll;
        float v1 = ((REGION_V + (SY1 - SY0)) / TEX_H) + scroll;
        // Corners as seen from outside: top-left, bottom-left, bottom-right, top-right (counter-clockwise = front face).
        for (int side = 0; side < 4; side++) {
            float ua = (REGION_U + SIDE_W * side) / TEX_W + scroll, ub = (REGION_U + SIDE_W * (side + 1)) / TEX_W + scroll;
            float lx, lz, rx, rz, nx, nz;
            switch (side) {
                case 0 -> { lx = x1; lz = x0; rx = x0; rz = x0; nx = 0; nz = -1; } // north face (z = low), left is +x
                case 1 -> { lx = x1; lz = x1; rx = x1; rz = x0; nx = 1; nz = 0; }  // east face (x = high), left is +z
                case 2 -> { lx = x0; lz = x1; rx = x1; rz = x1; nx = 0; nz = 1; }  // south face (z = high), left is -x
                default -> { lx = x0; lz = x0; rx = x0; rz = x1; nx = -1; nz = 0; } // west face (x = low), left is -z
            }
            shellVertex(c, pose, lx, y1, lz, ua, v0, nx, nz, light);
            shellVertex(c, pose, lx, y0, lz, ua, v1, nx, nz, light);
            shellVertex(c, pose, rx, y0, rz, ub, v1, nx, nz, light);
            shellVertex(c, pose, rx, y1, rz, ub, v0, nx, nz, light);
        }
    }

    private static void shellVertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, float u, float v, float nx, float nz, int light) {
        c.addVertex(pose, x, y, z).setColor(SWIRL_COLOR).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, 0F, nz);
    }

    // ---------------------------------------------------------------- ring glint geometry

    private void rebuildGlintQuads(BlockState state) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        if (model == cachedModel) return;
        cachedModel = model;
        List<float[]> out = new ArrayList<>();
        for (BakedQuad quad : model.getQuads(state, null, RandomSource.create(0L), ModelData.EMPTY, null)) {
            TextureAtlasSprite sprite = quad.getSprite();
            if (RING_SPRITE.equals(sprite.contents().name())) addOpaqueTexelQuads(quad, sprite, out);
        }
        glintQuads = out;
    }

    /** One quad per opaque texel of the ring sprite inside this model quad. Each float[] = 4 x (x, y, z, atlasU, atlasV). */
    private static void addOpaqueTexelQuads(BakedQuad quad, TextureAtlasSprite sprite, List<float[]> out) {
        int[] v = quad.getVertices();
        int stride = v.length / 4;
        int w = sprite.contents().width(), h = sprite.contents().height();
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float[][] pos = new float[4][3], tex = new float[4][2];
        for (int i = 0; i < 4; i++) {
            for (int axis = 0; axis < 3; axis++) pos[i][axis] = Float.intBitsToFloat(v[i * stride + axis]);
            tex[i][0] = (Float.intBitsToFloat(v[i * stride + 4]) - u0) / (u1 - u0) * w; // texel column coordinate
            tex[i][1] = (Float.intBitsToFloat(v[i * stride + 5]) - v0) / (v1 - v0) * h; // texel row coordinate
        }
        Direction dir = quad.getDirection();
        float[] lift = {dir.getStepX() * GLINT_LIFT, dir.getStepY() * GLINT_LIFT, dir.getStepZ() * GLINT_LIFT};
        out.addAll(RingTexelQuads.split(pos, tex, new float[]{u0, u1, v0, v1}, w, h, (x, y) -> !sprite.contents().isTransparent(0, x, y), lift));
    }
}
