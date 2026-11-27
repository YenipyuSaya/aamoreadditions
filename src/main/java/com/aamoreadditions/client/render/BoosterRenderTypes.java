package com.aamoreadditions.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Render types for the Dimensional Phantom Booster. Extends {@link RenderType} only to reach the protected render-state
 * shards (the same trick other mods use); it is never instantiated.
 */
public final class BoosterRenderTypes extends RenderType {
    public static final ResourceLocation CREEPER_ARMOR = ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper_armor.png");

    /**
     * Vanilla item glint ({@link RenderType#glint()}: same texture, shader, scroll/rotation matrix and blend) with two changes:
     * LEQUAL instead of EQUAL depth test, because it is drawn from a block entity renderer over chunk-mesh geometry whose depth is
     * not bit-identical, plus a small view-space layering offset against z-fighting.
     */
    private static final RenderType RING_GLINT = create("aamoreadditions_ring_glint",
            DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_GLINT_SHADER)
                    .setTextureState(new TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, true, false))
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setTexturingState(GLINT_TEXTURING)
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .createCompositeState(false));

    /**
     * Vanilla {@code RenderType.energySwirl} (additive, lightmap, creeper_armor.png) without the texture-matrix offset (the scroll is
     * added to the vertex UVs instead, so this type is a constant and nothing is allocated per frame), with back-face culling
     * (the shell is a closed box: without it the far faces would add a second time) and without depth writes.
     */
    private static final RenderType ENERGY_SHELL = create("aamoreadditions_energy_shell",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                    .setTextureState(new TextureStateShard(CREEPER_ARMOR, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(false));

    private BoosterRenderTypes() {
        super("aamoreadditions_unused", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {});
    }

    public static RenderType ringGlint() { return RING_GLINT; }

    public static RenderType energyShell() { return ENERGY_SHELL; }
}
