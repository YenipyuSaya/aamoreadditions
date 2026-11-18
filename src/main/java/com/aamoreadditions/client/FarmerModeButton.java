package com.aamoreadditions.client;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.farmer.FarmerMemoryRuntime;
import com.aamoreadditions.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import de.ellpeck.actuallyadditions.mod.inventory.gui.GuiFarmer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Farmer GUI mode button (approved preview): vanilla 18x18 button at GUI (3,5); a 14x14 frame inside the face
 * (dashed #AA0000 = Out-of-order, solid #55FF55 = Memory) and the vanilla wheat seeds texture, 1 px left of centre.
 * The texture is read from the game's own assets at runtime; nothing is redistributed.
 */
@EventBusSubscriber(modid = AAMoreAdditions.MODID, value = Dist.CLIENT)
public final class FarmerModeButton extends Button {
    public static final int X = 3, Y = 5, SIZE = 18;
    public static final int RED = 0xFFAA0000, GREEN = 0xFF55FF55;
    /** Dash pattern along each 14 px edge: on2 off2 x3 + on2 (corners on). */
    static final int[] DASH = {1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1};
    private static final ResourceLocation SEEDS = ResourceLocation.withDefaultNamespace("textures/item/wheat_seeds.png");

    private final TileEntityFarmer farmer;
    private Boolean shown;

    FarmerModeButton(int x, int y, TileEntityFarmer farmer) {
        super(x, y, SIZE, SIZE, Component.empty(), b -> PacketDistributor.sendToServer(ModNetwork.FarmerModePayload.INSTANCE), DEFAULT_NARRATION);
        this.farmer = farmer;
    }

    @SubscribeEvent
    static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof GuiFarmer screen) || screen.getMenu().farmer == null) return;
        event.addListener(new FarmerModeButton(screen.getGuiLeft() + X, screen.getGuiTop() + Y, screen.getMenu().farmer));
    }

    static String tooltipKey(boolean memory) {
        return memory ? "gui.aamoreadditions.farmer.mode.memory" : "gui.aamoreadditions.farmer.mode.out_of_order";
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        boolean memory = FarmerMemoryRuntime.of(farmer).memory();
        if (shown == null || shown != memory) { shown = memory; setTooltip(Tooltip.create(Component.translatable(tooltipKey(memory)))); }
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        g.blitSprite(SPRITES.get(this.active, isHoveredOrFocused()), getX(), getY(), SIZE, SIZE);
        int fx = getX() + 2, fy = getY() + 2, color = memory ? GREEN : RED;
        for (int i = 0; i < 14; i++) {
            if (!memory && DASH[i] == 0) continue;
            g.fill(fx + i, fy, fx + i + 1, fy + 1, color);
            g.fill(fx + i, fy + 13, fx + i + 1, fy + 14, color);
            g.fill(fx, fy + i, fx + 1, fy + i + 1, color);
            g.fill(fx + 13, fy + i, fx + 14, fy + i + 1, color);
        }
        g.blit(SEEDS, getX(), getY() + 1, 0, 0, 16, 16, 16, 16);
        RenderSystem.disableBlend();
    }
}
