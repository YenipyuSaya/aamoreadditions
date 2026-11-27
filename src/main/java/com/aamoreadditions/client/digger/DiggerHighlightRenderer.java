package com.aamoreadditions.client.digger;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.digger.DiggerHighlights;
import com.aamoreadditions.digger.DiggerScan;
import com.aamoreadditions.digger.DiggerState;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Work-area highlight: the four walls of the scanned box drawn exactly like the vanilla world border
 * (forcefield.png, additive blend, UV scrolled by (t, t) with t = millis % 3000 / 3000, stationary tint #20A0FF),
 * in the same render stage (after weather, where vanilla draws the border).
 * Depth test and depth writes are off so a buried digger's walls stay visible through terrain; both are restored after the pass.
 */
@EventBusSubscriber(modid = AAMoreAdditions.MODID, value = Dist.CLIENT)
public final class DiggerHighlightRenderer {
    private DiggerHighlightRenderer() {}

    static final float ALPHA = 0.75F;

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        double max = (mc.options.getEffectiveRenderDistance() + 1) * 16.0;
        var diggers = DiggerHighlights.snapshot().stream()
                .filter(t -> t.getLevel() == mc.level && ((DiggerState) t).aamoreadditions$highlight())
                .filter(t -> Math.abs(t.getBlockPos().getX() + 0.5 - cam.x) < max && Math.abs(t.getBlockPos().getZ() + 0.5 - cam.z) < max)
                .toList();
        if (diggers.isEmpty()) return;
        float t = (Util.getMillis() % 3000L) / 3000.0F;
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderTexture(0, DiggerScreen.FORCEFIELD);
        RenderSystem.depthMask(false);
        int c = DiggerScreen.BORDER_BLUE;
        RenderSystem.setShaderColor(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F, ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.polygonOffset(-3.0F, -3.0F);
        RenderSystem.enablePolygonOffset();
        RenderSystem.disableCull();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        int minY = mc.level.getMinBuildHeight();
        for (TileEntityVerticalDigger tile : diggers) {
            var st = (DiggerState) tile;
            var p = tile.getBlockPos();
            var area = DiggerScan.relative(
                    p.getX(), p.getZ(), Math.max(0, st.aamoreadditions$range()), st.aamoreadditions$strictChunk());
            box(b, cam, t,
                    p.getX() + area.minX(), minY + 1, p.getZ() + area.minZ(),
                    p.getX() + area.maxX() + 1, p.getY(), p.getZ() + area.maxZ() + 1);
        }
        var mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        RenderSystem.enableCull();
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    /** Camera-relative quads, one block wide (vanilla border layout): u along the wall, v along y, both * 0.5. */
    static void box(BufferBuilder b, Vec3 cam, float t, int x0, int y0, int z0, int x1, int y1, int z1) {
        if (y1 <= y0) return;
        float fy0 = (float) (y0 - cam.y), fy1 = (float) (y1 - cam.y);
        float v0 = t + (y0 & 1023) * 0.5F, v1 = v0 + (y1 - y0) * 0.5F;
        for (int x = x0; x < x1; x++) {
            float fx0 = (float) (x - cam.x), fx1 = fx0 + 1;
            float u0 = t + (x & 1023) * 0.5F, u1 = u0 + 0.5F;
            quad(b, fx0, fx1, (float) (z0 - cam.z), (float) (z0 - cam.z), fy0, fy1, u0, u1, v0, v1);
            quad(b, fx0, fx1, (float) (z1 - cam.z), (float) (z1 - cam.z), fy0, fy1, u0, u1, v0, v1);
        }
        for (int z = z0; z < z1; z++) {
            float fz0 = (float) (z - cam.z), fz1 = fz0 + 1;
            float u0 = t + (z & 1023) * 0.5F, u1 = u0 + 0.5F;
            quad(b, (float) (x0 - cam.x), (float) (x0 - cam.x), fz0, fz1, fy0, fy1, u0, u1, v0, v1);
            quad(b, (float) (x1 - cam.x), (float) (x1 - cam.x), fz0, fz1, fy0, fy1, u0, u1, v0, v1);
        }
    }

    private static void quad(BufferBuilder b, float xa, float xb, float za, float zb, float y0, float y1,
                             float u0, float u1, float v0, float v1) {
        b.addVertex(xa, y0, za).setUv(u0, v1);
        b.addVertex(xb, y0, zb).setUv(u1, v1);
        b.addVertex(xb, y1, zb).setUv(u1, v0);
        b.addVertex(xa, y1, za).setUv(u0, v0);
    }
}
