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

    public static final ResourceLocation TEX_SELECT = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/alloy_select.png");

    /** Same-tag selection button (10x10): right of the output frame (x104..129, y64..89) with a 2px gap, centred on it (y72..81). */
    public static final int SEL_X = 132, SEL_Y = 72, SEL_SIZE = 10;
    /** Selection overlay slot frames (18x18): first one right of the vertical channel (x120..125, y39..64), centred on it; more extend right. */
    public static final int PICK_X = 127, PICK_Y = 43, PICK_STEP = 18;
    /** Background darkening behind the overlay (vanilla screen-background colours). */
    static final int DIM_TOP = 0xC0101010, DIM_BOTTOM = 0xD0101010;

    private final AlloyFurnaceBlockEntity tile;
    private EnergyDisplay energy;
    private Button buttonLock;
    private Button buttonMode;
    private SelectButton buttonSelect;

    /** Current output state (recomputed every frame from the synced slots), null if no recipe matches. */
    private OutState cur;
    /** Selection overlay open (for this output group). */
    private boolean overlay;
    private String overlayGroup;
    private boolean swallowRelease;

    record OutState(String group, java.util.List<net.minecraft.world.item.ItemStack> candidates, java.util.List<String> ids,
                    com.aamoreadditions.recipe.OutputChoices.Resolution res) {
        boolean selectable() { return com.aamoreadditions.recipe.OutputChoices.selectable(ids); }
    }

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
        this.buttonLock = new LockButton(this.getGuiLeft() + LOCK_X, this.topPos + LOCK_Y);
        this.addRenderableWidget(this.buttonLock);
        this.buttonMode = new ModeButton(this.getGuiLeft() + MODE_X, this.topPos + MODE_Y);
        this.addRenderableWidget(this.buttonMode);
        this.buttonSelect = new SelectButton(this.leftPos + SEL_X, this.topPos + SEL_Y);
        this.addRenderableWidget(this.buttonSelect);
        this.overlay = false;
    }

    private OutState computeState() {
        var m = tile.currentMatch();
        if (m == null) return null;
        var r = m.recipe().value().result();
        var c = r.candidates();
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (var st : c) ids.add(com.aamoreadditions.recipe.AlloyResult.idOf(st.getItem()));
        String g = r.group();
        return new OutState(g, c, ids, tile.outputChoices.resolve(g, ids));
    }

    /** Selection button look: u offset into alloy_select.png (50x10). */
    static final int SEL_U_DARK = 0, SEL_U_ALERT = 10, SEL_U_ALERT_HOVER = 20, SEL_U_CHECK = 30, SEL_U_CHECK_HOVER = 40;

    static int selectU(com.aamoreadditions.recipe.OutputChoices.Kind kind, boolean hovered) {
        return switch (com.aamoreadditions.recipe.OutputChoices.look(kind)) {
            case CHECK -> hovered ? SEL_U_CHECK_HOVER : SEL_U_CHECK;
            case ALERT -> hovered ? SEL_U_ALERT_HOVER : SEL_U_ALERT;
            case DARK -> SEL_U_DARK;
        };
    }

    /**
     * 10x10 button right of the output. Red "!" (clickable) while the current output has several same-tag items and no
     * choice (the furnace waits); green check (clickable, to change it) once a choice exists, whether or not the furnace is
     * running; dark and inert when there is nothing to choose (single item / no recipe). Opens the selection overlay.
     */
    private final class SelectButton extends Button {
        SelectButton(int x, int y) {
            super(x, y, SEL_SIZE, SEL_SIZE, Component.empty(), b -> {
                if (cur != null && cur.selectable()) { overlay = true; overlayGroup = cur.group(); }
            }, DEFAULT_NARRATION);
            this.active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int u = selectU(cur == null ? null : cur.res().kind(), this.active && isHovered());
            RenderSystem.enableBlend();
            g.blit(TEX_SELECT, getX(), getY(), u, 0, SEL_SIZE, SEL_SIZE, 50, 10);
            RenderSystem.disableBlend();
        }
    }

    // --- selection overlay ---

    public boolean overlayOpen() { return overlay; }

    /** Current furnace mode (synced); JEI click area opens the matching category. */
    public boolean efficientMode() { return tile.efficientMode; }

    private java.util.List<net.minecraft.world.item.ItemStack> overlayItems() {
        return overlay && cur != null ? cur.candidates() : java.util.List.of();
    }

    /** Screen x/y of overlay slot i's 18x18 frame. */
    public int pickFrameX(int i) { return leftPos + PICK_X + i * PICK_STEP; }
    public int pickFrameY() { return topPos + PICK_Y; }

    /** Overlay slot index under the mouse (the 18x18 frame), or -1. */
    public int overlayIndexAt(double mx, double my) {
        var items = overlayItems();
        for (int i = 0; i < items.size(); i++) {
            int fx = pickFrameX(i), fy = pickFrameY();
            if (mx >= fx && mx < fx + 18 && my >= fy && my < fy + 18) return i;
        }
        return -1;
    }

    /** Item shown in the overlay slot under the mouse (count 1), or EMPTY. For JEI's recipe/usage keys. */
    public net.minecraft.world.item.ItemStack overlayStackAt(double mx, double my) {
        int i = overlayIndexAt(mx, my);
        return i < 0 ? net.minecraft.world.item.ItemStack.EMPTY : overlayItems().get(i).copyWithCount(1);
    }

    /**
     * Overlay mouse handling (called first from a highest-priority ScreenEvent.MouseButtonPressed.Pre listener so a
     * JEI key bound to the left mouse button can't steal the selection click). Left click on a slot selects it;
     * a click outside the slots closes the overlay only. Every click is consumed while the overlay is open.
     */
    public boolean handleOverlayClick(double mx, double my, int button) {
        if (!overlay) return false;
        int i = overlayIndexAt(mx, my);
        if (i >= 0) {
            if (button == 0) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new com.aamoreadditions.network.ModNetwork.AlloyOutputChoicePayload(overlayGroup, cur.ids().get(i)));
                overlay = false;
            }
        } else {
            overlay = false;
        }
        swallowRelease = true;
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleOverlayClick(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (swallowRelease || overlay) { swallowRelease = false; return true; }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (overlay || swallowRelease) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (overlay) return true;
        return super.mouseScrolled(mx, my, sx, sy);
    }

    /** Esc (and the inventory key) close only the overlay, never the furnace screen. */
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (overlay) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || this.minecraft.options.keyInventory.isActiveAndMatches(com.mojang.blaze3d.platform.InputConstants.getKey(key, scan)))
                overlay = false;
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    /** Vanilla-style inventory slot frame (18x18). */
    static void slotFrame(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    private void renderOverlay(GuiGraphics g, int mx, int my) {
        var items = overlayItems();
        g.pose().pushPose();
        g.pose().translate(0, 0, 500); // above slots, items, ghosts and the furnace tooltips
        g.fillGradient(0, 0, this.width, this.height, DIM_TOP, DIM_BOTTOM);
        String chosen = tile.outputChoices.get(overlayGroup);
        int hover = overlayIndexAt(mx, my);
        for (int i = 0; i < items.size(); i++) {
            int fx = pickFrameX(i), fy = pickFrameY();
            slotFrame(g, fx, fy);
            var st = items.get(i).copyWithCount(1);
            g.renderItem(st, fx + 1, fy + 1);
            if (i == hover || cur.ids().get(i).equals(chosen)) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                if (i == hover) g.fill(fx + 1, fy + 1, fx + 17, fy + 17, 0x80FFFFFF);
                if (cur.ids().get(i).equals(chosen)) g.renderOutline(fx, fy, 18, 18, 0xFF55FF55);
                g.pose().popPose();
            }
        }
        if (hover >= 0) g.renderTooltip(font, items.get(hover).copyWithCount(1), mx, my);
        g.pose().popPose();
    }


    /** Lock button: 18x18 body at GUI (-1,4) so the 16x16 icon stays where it was (GUI (0,5)), centred with a 1px border. */
    public static final int LOCK_X = -1, LOCK_Y = 4, LOCK_SIZE = 18;
    /** Mode button: same 18x18 body directly below the lock button with a 1px gap (GUI (-1,23)..(16,40)). */
    public static final int MODE_X = LOCK_X, MODE_Y = LOCK_Y + LOCK_SIZE + 1;
    public static final ResourceLocation TEX_MODE_ALLOY = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/mode_alloy.png");
    public static final ResourceLocation TEX_MODE_EFFICIENT = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/mode_efficient.png");

    /** Alloy / Efficient-Smelting mode toggle: lock-button look, icon = current mode; click is handled by the server menu. */
    private final class ModeButton extends Button {
        ModeButton(int x, int y) {
            super(x, y, LOCK_SIZE, LOCK_SIZE, Component.empty(), b -> {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, AlloyFurnaceMenu.BUTTON_MODE);
            }, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            g.blitSprite(SPRITES.get(this.active, isHoveredOrFocused()), getX(), getY(), LOCK_SIZE, LOCK_SIZE);
            int o = (LOCK_SIZE - 16) / 2;
            g.blit(tile.efficientMode ? TEX_MODE_EFFICIENT : TEX_MODE_ALLOY, getX() + o, getY() + o, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
        }
    }

    /** 18x18 button: vanilla/AA button body (highlighted on hover) with the lock icon centred on top; click is handled by the server menu. */
    private final class LockButton extends Button {
        LockButton(int x, int y) {
            super(x, y, LOCK_SIZE, LOCK_SIZE, Component.empty(), b -> {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, AlloyFurnaceMenu.BUTTON_LOCK);
            }, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            g.blitSprite(SPRITES.get(this.active, isHoveredOrFocused()), getX(), getY(), LOCK_SIZE, LOCK_SIZE);
            int o = (LOCK_SIZE - 16) / 2;
            g.blit(tile.lock.locked() ? TEX_LOCK : TEX_UNLOCK, getX() + o, getY() + o, 0, 0, 16, 16, 16, 16);
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
            else icon = cycle(r.ingredient(ing).getItems(), t);
            ghost(g, slots[s], icon);
        }
    }

    /** Output ghost: the chosen same-tag item for the current recipe; otherwise the locked recipe's result. */
    private void renderOutputGhost(GuiGraphics g) {
        var out = menu.slots.get(AlloyFurnaceMenu.SLOT_OUT);
        if (out.hasItem()) return;
        if (cur != null && cur.res().kind() == com.aamoreadditions.recipe.OutputChoices.Kind.CHOSEN) {
            int i = cur.ids().indexOf(cur.res().item());
            if (i >= 0) { ghost(g, out, cur.candidates().get(i)); return; }
        }
        var r = tile.lockedRecipe();
        if (r == null) return;
        var res = r.result();
        var st = res.produce(tile.outputChoices);
        if (st.isEmpty()) st = cycle(res.candidates().toArray(new net.minecraft.world.item.ItemStack[0]), tile.getLevel() == null ? 0 : tile.getLevel().getGameTime());
        ghost(g, out, st);
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
        this.cur = computeState();
        boolean selectable = cur != null && com.aamoreadditions.recipe.OutputChoices.clickable(cur.res().kind());
        this.buttonSelect.active = selectable;
        if (overlay && (!selectable || !java.util.Objects.equals(cur.group(), overlayGroup))) overlay = false; // output changed
        if (overlay) {
            // furnace behind: no hover anywhere (mouse parked off-screen), then dim + overlay on top
            super.render(g, -10000, -10000, f);
            renderOverlay(g, x, y);
            return;
        }
        super.render(g, x, y, f);
        this.energy.render(g, x, y);
        if (selectable && this.buttonSelect.isMouseOver(x, y)) {
            java.util.List<Component> tip;
            if (cur.res().kind() == com.aamoreadditions.recipe.OutputChoices.Kind.CHOSEN) {
                // check state: which item is selected, and that it can be changed
                var chosen = cur.candidates().get(cur.ids().indexOf(cur.res().item()));
                tip = java.util.List.of(
                        Component.translatable("gui.aamoreadditions.alloyfurnace.same_tag.chosen", chosen.getHoverName()).withStyle(net.minecraft.ChatFormatting.WHITE),
                        Component.translatable("gui.aamoreadditions.alloyfurnace.same_tag.change").withStyle(net.minecraft.ChatFormatting.WHITE));
            } else {
                tip = java.util.List.of(
                        Component.translatable("gui.aamoreadditions.alloyfurnace.same_tag.1").withStyle(net.minecraft.ChatFormatting.WHITE),
                        Component.translatable("gui.aamoreadditions.alloyfurnace.same_tag.2").withStyle(net.minecraft.ChatFormatting.WHITE));
            }
            g.renderComponentTooltip(font, tip, x, y);
        } else if (this.buttonMode.isMouseOver(x, y)) {
            Component now = Component.translatable(tile.efficientMode ? "gui.aamoreadditions.alloyfurnace.mode.efficient" : "gui.aamoreadditions.alloyfurnace.mode.alloy");
            Component next = Component.translatable(tile.efficientMode ? "gui.aamoreadditions.alloyfurnace.mode.alloy" : "gui.aamoreadditions.alloyfurnace.mode.efficient");
            g.renderComponentTooltip(font, java.util.List.of(now, Component.translatable("gui.aamoreadditions.alloyfurnace.mode.switch", next)
                    .withStyle(net.minecraft.ChatFormatting.GRAY)), x, y);
        } else if (this.buttonLock.isMouseOver(x, y)) {
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

    /** Efficient-Smelting mode: the arrow becomes vanilla's furnace flame (14x14, fills bottom-to-top), centred on the arrow. */
    static final int FLAME_X = 106, FLAME_Y = 45, FLAME_SIZE = 14;
    static final ResourceLocation VANILLA_FURNACE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    static final ResourceLocation FLAME_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");

    /** Lit rows (0..14) for the flame. */
    static int flameRows(int progress14) { return Math.max(0, Math.min(progress14, FLAME_SIZE)); }

    private void renderFlameProgress(GuiGraphics g) {
        // hide the baked-in empty arrow (its 7x18 box is plain panel grey otherwise), then vanilla's empty flame
        g.fill(leftPos + ARROW_X, topPos + ARROW_Y, leftPos + ARROW_X + ARROW_W, topPos + ARROW_Y + ARROW_H, 0xFFC6C6C6);
        g.blit(VANILLA_FURNACE, leftPos + FLAME_X, topPos + FLAME_Y, 56, 36, FLAME_SIZE, FLAME_SIZE);
        int rows = flameRows(arrowProgress(FLAME_SIZE));
        if (rows > 0) g.blitSprite(FLAME_SPRITE, FLAME_SIZE, FLAME_SIZE, 0, FLAME_SIZE - rows,
                leftPos + FLAME_X, topPos + FLAME_Y + FLAME_SIZE - rows, FLAME_SIZE, rows);
    }

    /** Smelt progress 0..scale for line 1 / 2 (kept for a future progress display). */
    public int progress1(int scale) { return tile.getFirstTimeToScale(scale); }
    public int progress2(int scale) { return tile.getSecondTimeToScale(scale); }

    @Override
    public void renderBg(GuiGraphics g, float f, int x, int y) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(INVENTORY, this.leftPos, this.topPos + 93, 0, 0, 176, 86);
        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 176, 93);
        if (tile.efficientMode) renderFlameProgress(g);
        else {
            int seg = arrowProgress(ARROW_SEGMENTS);
            if (seg > 0) g.blit(TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y, 176, 0, ARROW_W, arrowRows(seg));
        }
        renderLockGhosts(g);
        renderOutputGhost(g);
        drawEnergyBar(g);
    }
}
