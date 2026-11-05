package com.aamoreadditions.client.screen;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.block.AlloyFurnaceBlockEntity;
import com.aamoreadditions.menu.AlloyFurnaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import de.ellpeck.actuallyadditions.mod.inventory.gui.AAScreen;
import de.ellpeck.actuallyadditions.mod.inventory.gui.EnergyDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AlloyFurnaceScreen extends AAScreen<AlloyFurnaceMenu> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/alloyfurnace.png");
    public static final ResourceLocation INVENTORY = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/alloyfurnace_inventory.png");
    public static final ResourceLocation TEX_LOCK = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/recipe_lock.png");
    public static final ResourceLocation TEX_UNLOCK = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/recipe_unlock.png");
    /** Ghost icon overlay: slot grey at ~60% over the item = semi-transparent look. */
    static final int GHOST_OVERLAY = 0x998B8B8B;

    private final AlloyFurnaceBlockEntity tile;
    private EnergyDisplay energy;
    private Button buttonLock;

    public AlloyFurnaceScreen(AlloyFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.tile = menu.furnace;
        this.imageWidth = 176;
        this.imageHeight = 93 + 86;
    }

    @Override
    public void init() {
        super.init();
        this.energy = new EnergyDisplay(this.leftPos + 27, this.topPos + 5, this.tile.storage);
        this.buttonLock = new LockButton(this.getGuiLeft(), this.topPos + 5);
        this.addRenderableWidget(this.buttonLock);
    }

    /** 16x16 image button: user textures, translucent white overlay on hover; click is handled by the server menu. */
    private final class LockButton extends Button {
        LockButton(int x, int y) {
            super(x, y, 16, 16, Component.empty(), b -> {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, AlloyFurnaceMenu.BUTTON_LOCK);
            }, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            RenderSystem.enableBlend();
            g.blit(tile.lock.locked() ? TEX_LOCK : TEX_UNLOCK, getX(), getY(), 0, 0, 16, 16, 16, 16);
            if (isHoveredOrFocused()) g.fill(getX(), getY(), getX() + 16, getY() + 16, 0x40FFFFFF);
            RenderSystem.disableBlend();
        }
    }

    /**
     * Same look as AA's EnergyDisplay.draw, but the fill height uses long math and is clamped
     * (AA computes energy * 83 / max in int: overflows above ~25.8M FE and over-draws if energy > max).
     */
    private void drawEnergyBar(GuiGraphics g) {
        int bx = leftPos + 27, by = topPos + 5;
        g.blit(de.ellpeck.actuallyadditions.mod.util.AssetUtil.GUI_INVENTORY_LOCATION, bx, by, 18, 171, 18, 85);
        int h = com.aamoreadditions.effect.UpgradeMath.energyBarHeight(tile.storage.getEnergyStored(), tile.storage.getMaxEnergyStored(), 83);
        if (h > 0) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            float[] c = de.ellpeck.actuallyadditions.mod.util.AssetUtil.getWheelColor(mc.level == null ? 0 : mc.level.getGameTime() % 256);
            RenderSystem.setShaderColor(c[0] / 255F, c[1] / 255F, c[2] / 255F, 1F);
            g.blit(de.ellpeck.actuallyadditions.mod.util.AssetUtil.GUI_INVENTORY_LOCATION, bx + 1, by + 84 - h, 36, 172, 16, h);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }
    }

    /** Ghost icons for a recorded lock: ingredient (tags cycle every 20 ticks, sorted by id), barrier for unused slots, result in the output. */
    private void renderLockGhosts(GuiGraphics g) {
        var r = tile.lockedRecipe();
        if (r == null) return;
        net.minecraft.world.inventory.Slot[] slots = {menu.slots.get(AlloyFurnaceMenu.SLOT_IN_1), menu.slots.get(AlloyFurnaceMenu.SLOT_IN_2), menu.extraSlot};
        long t = tile.getLevel() == null ? 0 : tile.getLevel().getGameTime();
        for (int s = 0; s < 3; s++) {
            if (slots[s].hasItem()) continue;
            int ing = tile.lock.ingredientFor(s);
            net.minecraft.world.item.ItemStack icon;
            if (ing == com.aamoreadditions.recipe.RecipeLock.FORBIDDEN) icon = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER);
            else icon = cycle(r.ingredients().get(ing).ingredient().getItems(), t);
            ghost(g, slots[s], icon);
        }
        var out = menu.slots.get(AlloyFurnaceMenu.SLOT_OUT);
        if (!out.hasItem()) ghost(g, out, r.result());
    }

    /** Stable order (by item id), one item per 20 game ticks. */
    static net.minecraft.world.item.ItemStack cycle(net.minecraft.world.item.ItemStack[] items, long gameTime) {
        java.util.TreeMap<String, net.minecraft.world.item.ItemStack> byId = new java.util.TreeMap<>();
        for (var st : items) byId.putIfAbsent(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).toString(), st);
        if (byId.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        var list = new java.util.ArrayList<>(byId.values());
        return list.get((int) ((gameTime / 20) % list.size()));
    }

    private void ghost(GuiGraphics g, net.minecraft.world.inventory.Slot slot, net.minecraft.world.item.ItemStack icon) {
        if (icon.isEmpty()) return;
        int x = leftPos + slot.x, y = topPos + slot.y;
        g.renderFakeItem(icon, x, y);
        g.pose().pushPose();
        g.pose().translate(0, 0, 250);
        g.fill(x, y, x + 16, y + 16, GHOST_OVERLAY);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float f) {
        super.render(g, x, y, f);
        this.energy.render(g, x, y);
        if (this.buttonLock.isMouseOver(x, y)) {
            g.renderTooltip(font, Component.translatable(tile.lock.locked() ? "gui.aamoreadditions.alloyfurnace.recipe_locked" : "gui.aamoreadditions.alloyfurnace.recipe_unlocked"), x, y);
        } else if (x - this.leftPos >= 56 && x - this.leftPos <= 63 && y - this.topPos >= 60 && y - this.topPos <= 67) {
            // green "+" incl. its dark outline: texture px x56..63, y60..67 (cells 57..62 x 61..66), top-left of the Extra frame
            g.renderTooltip(font, Component.translatable("slot.aamoreadditions.alloyfurnace.extra"), x, y);
        }
    }

    /** "Show recipes" click area (GUI coords x,y,w,h): between the right input frame (bottom y39) and the output frame (top y64), x106..119 (incl. the arrow, left of the channel at x120); for JEI. */
    public static final int RECIPE_AREA_X = 106, RECIPE_AREA_Y = 40, RECIPE_AREA_W = 14, RECIPE_AREA_H = 24;

    /** Progress arrow: 7x18 at (109,43); fill sprite at u=176,v=0; 10 segments. */
    static final int ARROW_X = 109, ARROW_Y = 43, ARROW_W = 7, ARROW_H = 18, ARROW_SEGMENTS = 10;

    /** Filled rows for a segment count: round(seg*H/10) -> 0,2,4,5,7,9,11,13,14,16,18. */
    static int arrowRows(int seg) {
        seg = Math.max(0, Math.min(seg, ARROW_SEGMENTS));
        return (seg * ARROW_H * 2 + ARROW_SEGMENTS) / (ARROW_SEGMENTS * 2);
    }

    /** Which progress the arrow shows (0..scale, floored): currently the larger of the two lines. */
    public int arrowProgress(int scale) { return Math.max(progress1(scale), progress2(scale)); }

    /** Smelt progress 0..scale for line 1 / 2 (kept for a future progress display). */
    public int progress1(int scale) { return tile.getFirstTimeToScale(scale); }
    public int progress2(int scale) { return tile.getSecondTimeToScale(scale); }

    @Override
    public void renderBg(GuiGraphics g, float f, int x, int y) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(INVENTORY, this.leftPos, this.topPos + 93, 0, 0, 176, 86);
        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 176, 93);
        int seg = arrowProgress(ARROW_SEGMENTS);
        if (seg > 0) g.blit(TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y, 176, 0, ARROW_W, arrowRows(seg));
        renderLockGhosts(g);
        drawEnergyBar(g);
    }
}
