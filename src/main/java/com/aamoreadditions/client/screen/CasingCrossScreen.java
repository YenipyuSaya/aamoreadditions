package com.aamoreadditions.client.screen;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.menu.CasingCrossMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import de.ellpeck.actuallyadditions.mod.inventory.gui.AAScreen;
import de.ellpeck.actuallyadditions.mod.util.AssetUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.text.NumberFormat;

public class CasingCrossScreen extends AAScreen<CasingCrossMenu> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/casing_cross.png");
    private static final int BAR_X = 5, BAR_Y = 5;
    /** Lock button (18x18, framed) right of the energy bar, top aligned. */
    public static final int LOCK_X = BAR_X + 18 + 2, LOCK_Y = BAR_Y, LOCK_SIZE = 18;
    /** Upgrade toggle/info column, top-right inside the border. */
    public static final int UPGRADE_BTN_X = 162;
    /** JEI "show recipes" area (GUI coords): between the down input frame (right edge x89) and the output frame (left x127), rows of those frames. */
    public static final int RECIPE_AREA_X = 90, RECIPE_AREA_Y = 65, RECIPE_AREA_W = 37, RECIPE_AREA_H = 20;
    public static final int ARROW_X = 94, ARROW_Y = 70, ARROW_W = 28;

    public CasingCrossScreen(CasingCrossMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 93 + 86;
    }

    private net.minecraft.client.gui.components.Button buttonLock;

    @Override
    public void init() {
        super.init();
        // same spot as the Alloy Furnace: GUI left edge, y+5 (energy bar starts at x+27, so no overlap)
        buttonLock = new net.minecraft.client.gui.components.Button(leftPos + LOCK_X, topPos + LOCK_Y, LOCK_SIZE, LOCK_SIZE, Component.empty(), b -> {
            var mc = Minecraft.getInstance();
            if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, CasingCrossMenu.BUTTON_LOCK);
        }, s -> Component.empty()) {
            @Override
            protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                // vanilla bevelled button frame (normal / highlighted), pressed = darker + icon nudged 1px
                super.renderWidget(g, mx, my, pt);
                boolean pressed = isHovered() && org.lwjgl.glfw.GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), 0) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                if (pressed) g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x50000000);
                int o = pressed ? 1 : 0;
                RenderSystem.enableBlend();
                g.blit(menu.locked() ? AlloyFurnaceScreen.TEX_LOCK : AlloyFurnaceScreen.TEX_UNLOCK, getX() + 1 + o, getY() + 1 + o, 0, 0, 16, 16, 16, 16);
                RenderSystem.disableBlend();
            }
        };
        addRenderableWidget(buttonLock);
    }

    /** Ghosts like the Alloy Furnace: ingredient (tags cycle every 20 ticks), barrier for forbidden slots, result in the output. */
    private void renderLockGhosts(GuiGraphics g) {
        var h = menu.lockedRecipe();
        if (h == null) return;
        var r = h.value();
        int[] map = menu.lockMap();
        var mc = Minecraft.getInstance();
        long t = mc.level == null ? 0 : mc.level.getGameTime();
        for (int s = 0; s < 5; s++) {
            var slot = menu.slots.get(s);
            if (slot.hasItem()) continue;
            int m = map[s];
            net.minecraft.world.item.ItemStack icon;
            if (m == com.aamoreadditions.structure.CasingCrossData.LOCK_BASE) icon = AlloyFurnaceScreen.cycle(r.getInput().getItems(), t);
            else if (m >= 0 && m <= 3) icon = AlloyFurnaceScreen.cycle(com.aamoreadditions.structure.CasingCrossData.standIngredient(r, m).getItems(), t);
            else icon = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER);
            ghost(g, slot, icon);
        }
        var out = menu.slots.get(CasingCrossMenu.OUT);
        if (!out.hasItem()) ghost(g, out, r.getOutput());
    }

    private void ghost(GuiGraphics g, net.minecraft.world.inventory.Slot slot, net.minecraft.world.item.ItemStack icon) {
        if (icon.isEmpty()) return;
        int x = leftPos + slot.x, y = topPos + slot.y;
        g.renderFakeItem(icon, x, y);
        g.pose().pushPose();
        g.pose().translate(0, 0, 250);
        g.fill(x, y, x + 16, y + 16, AlloyFurnaceScreen.GHOST_OVERLAY);
        g.pose().popPose();
    }

    @Override
    public void renderBg(GuiGraphics g, float f, int x, int y) {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.blit(AlloyFurnaceScreen.INVENTORY, leftPos, topPos + 93, 0, 0, 176, 86);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, 176, 93);
        // progress arrow (fill drawn from texture u=176,v=0), between work area and output slot
        int t = menu.recipeTime(), p = menu.progress();
        if (t > 0 && p > 0) g.blit(TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y, 176, 0, Math.min(ARROW_W, p * ARROW_W / t), 9);
        renderLockGhosts(g);
        // energy bar, same look as AA's EnergyDisplay
        int bx = leftPos + BAR_X, by = topPos + BAR_Y;
        g.blit(AssetUtil.GUI_INVENTORY_LOCATION, bx, by, 18, 171, 18, 85);
        int h = com.aamoreadditions.effect.UpgradeMath.energyBarHeight(menu.energy(), menu.capacity(), 83);
        if (h > 0) {
            var mc = Minecraft.getInstance();
            float[] c = AssetUtil.getWheelColor(mc.level == null ? 0 : mc.level.getGameTime() % 256);
            RenderSystem.setShaderColor(c[0] / 255F, c[1] / 255F, c[2] / 255F, 1F);
            g.blit(AssetUtil.GUI_INVENTORY_LOCATION, bx + 1, by + 84 - h, 36, 172, 16, h);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float f) {
        super.render(g, x, y, f);
        if (buttonLock.isMouseOver(x, y))
            g.renderTooltip(font, Component.translatable(menu.locked() ? "gui.aamoreadditions.alloyfurnace.recipe_locked" : "gui.aamoreadditions.alloyfurnace.recipe_unlocked"), x, y);
        int rx = x - leftPos - BAR_X, ry = y - topPos - BAR_Y;
        if (rx >= 0 && ry >= 0 && rx < 18 && ry < 85) {
            NumberFormat nf = NumberFormat.getInstance();
            g.renderTooltip(font, Component.translatable("misc.actuallyadditions.power_long", nf.format(menu.energy()), nf.format(menu.capacity())), x, y);
        }
    }
}
