package com.aamoreadditions.client;

import com.aamoreadditions.AAMoreAdditions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Supplier;

/** 10x10 non-interactive "i" badge (textures/gui/upgrade_info.png); hover shows live upgrade stats. */
public class UpgradeInfoButton extends AbstractWidget {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/upgrade_info.png");
    private final Supplier<List<Component>> lines;

    public UpgradeInfoButton(int x, int y, Supplier<List<Component>> lines) {
        super(x, y, UpgradeToggleButton.SIZE, UpgradeToggleButton.SIZE, Component.empty());
        this.lines = lines;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.blit(TEXTURE, getX(), getY(), 0, 0, width, height, 10, 10);
        if (isHovered() && Minecraft.getInstance().screen != null) {
            List<FormattedCharSequence> seq = lines.get().stream().map(Component::getVisualOrderText).toList();
            Minecraft.getInstance().screen.setTooltipForNextRenderPass(seq);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) { return false; }
    @Override public boolean mouseReleased(double mx, double my, int button) { return false; }
    @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { return false; }
    @Override public void playDownSound(SoundManager sm) {}
    @Override public ComponentPath nextFocusPath(FocusNavigationEvent e) { return null; }
    @Override public boolean isFocused() { return false; }
    @Override protected void updateWidgetNarration(NarrationElementOutput out) {}
}
