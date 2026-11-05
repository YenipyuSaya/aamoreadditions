package com.aamoreadditions.client;

import com.aamoreadditions.AAMoreAdditions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;

/**
 * 10x10 sprite button. Texture assets/aamoreadditions/textures/gui/upgrade_toggle.png (20x20):
 * row 0 = closed state "∧" (opens), row 1 = open state "∨" (closes); column 0 = normal, column 1 = hover.
 */
public class UpgradeToggleButton extends Button {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/upgrade_toggle.png");
    public static final int SIZE = 10, TEX_W = 20, TEX_H = 20;
    private final BooleanSupplier open;

    public UpgradeToggleButton(int x, int y, BooleanSupplier open, OnPress onPress) {
        super(x, y, SIZE, SIZE, Component.empty(), onPress, DEFAULT_NARRATION);
        this.open = open;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int u = isHoveredOrFocused() ? SIZE : 0;
        int v = open.getAsBoolean() ? SIZE : 0;
        g.blit(TEXTURE, getX(), getY(), u, v, SIZE, SIZE, TEX_W, TEX_H);
    }
}
