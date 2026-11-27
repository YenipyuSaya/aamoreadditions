package com.aamoreadditions.booster;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * HUD / tooltip wording for a phantom bind: block name, coordinates, dimension
 * on the right, and either a same-dimension distance or the Cross-Dimensional label.
 * String helpers mirror that layout so unit tests do not need a running client.
 */
public final class BoundHudText {
    public static final String CROSS_DIMENSIONAL_KEY = "aamoreadditions.phantom.cross_dimensional";
    public static final String BOUND_INFO_CROSS_KEY = "aamoreadditions.phantom.bound_info_cross";

    private BoundHudText() {}

    public static String dimensionLangKey(String namespace, String path) {
        return "dimension." + namespace + "." + path.replace('/', '.');
    }

    public static String dimensionFallback(String namespace, String path) {
        return namespace + ":" + path;
    }

    public static String dimensionLangKey(ResourceLocation id) {
        return dimensionLangKey(id.getNamespace(), id.getPath());
    }

    public static String dimensionFallback(ResourceLocation id) {
        return dimensionFallback(id.getNamespace(), id.getPath());
    }

    /** Localized dimension title (vanilla {@code dimension.*} keys) with the id as fallback. */
    public static Component dimensionName(ResourceKey<Level> dim) {
        if (dim == null) return Component.empty();
        ResourceLocation id = dim.location();
        return Component.translatableWithFallback(dimensionLangKey(id), dimensionFallback(id));
    }

    /** Same-dimension AA sentence, with the dimension name appended on the right. */
    public static MutableComponent appendDimension(Component line, ResourceKey<Level> dim) {
        return line.copy().append(" ").append(dimensionName(dim));
    }

    /**
     * Cross-dimension HUD line: name + XYZ + dimension, then the Cross-Dimensional label
     * instead of a numeric distance.
     */
    public static Component crossDimensionalLine(String blockName, BlockPos pos, ResourceKey<Level> dim) {
        return Component.translatable(BOUND_INFO_CROSS_KEY,
                blockName, pos.getX(), pos.getY(), pos.getZ(),
                dimensionName(dim),
                Component.translatable(CROSS_DIMENSIONAL_KEY));
    }

    /** English layout of {@link #crossDimensionalLine} for unit tests (lang is not loaded). */
    public static String previewCross(String blockName, int x, int y, int z, String dimensionName, String label) {
        return "The connected Block is " + blockName + " at " + x + ", " + y + ", " + z + " " + dimensionName + ". " + label;
    }

    /** Same-dim AA sentence with the dimension on the right, for unit tests. */
    public static String previewSameDimAppend(String aaSentence, String dimensionName) {
        return aaSentence + " " + dimensionName;
    }
}
