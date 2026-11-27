package com.aamoreadditions.client.digger;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.client.UpgradeStrip;
import com.aamoreadditions.digger.DiggerEffects;
import com.aamoreadditions.digger.DiggerFillerSlot;
import com.aamoreadditions.digger.DiggerMenuState;
import com.aamoreadditions.digger.DiggerMenus;
import com.aamoreadditions.digger.DiggerRuntime;
import com.aamoreadditions.digger.DiggerState;
import de.ellpeck.actuallyadditions.mod.inventory.ContainerMiner;
import de.ellpeck.actuallyadditions.mod.inventory.gui.GuiMiner;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Vertical Digger GUI additions: Mode (-1,4) and Highlight (-1,23) 18x18 buttons hugging the left edge (Alloy Furnace
 * positions), the fillers toggle at (134,42) in the upgrade-button column, centred on the 9 output slots,
 * and the 3x3 fillers panel that slides out to the right of the gui_breaker head
 * (same {@link UpgradeStrip#ANIM_MS} / {@link UpgradeStrip#ease} timing as the upgrade panel).
 * Vanilla textures (forcefield, button sprites, barrier item) are read from the game's own assets at runtime.
 */
@EventBusSubscriber(modid = AAMoreAdditions.MODID, value = Dist.CLIENT)
public final class DiggerScreen {
    private DiggerScreen() {}

    public static final int BTN_X = -1, MODE_Y = 4, HIGHLIGHT_Y = 23, SIZE = 18;
    /** Strict Radius frame colours (ChatFormatting RED / BLUE) and Strict Chunk green (ChatFormatting GREEN). */
    public static final int RED = 0xFFFF5555, BLUE = 0xFF5555FF, GREEN = 0xFF55FF55;
    /** BorderStatus colours: stationary #20A0FF, shrinking #FF3030. */
    public static final int BORDER_BLUE = 0x20A0FF, BORDER_RED = 0xFF3030;
    static final int[] DASH = {1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1};
    public static final ResourceLocation FORCEFIELD = ResourceLocation.withDefaultNamespace("textures/misc/forcefield.png");
    static final ResourceLocation BREAKER = ResourceLocation.fromNamespaceAndPath("actuallyadditions", "textures/gui/gui_breaker.png");
    static final ResourceLocation TOGGLE = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/fillers_toggle.png");

    /** Fillers panel open state remembered for this session (like the upgrade strip). */
    private static boolean remembered;
    private static final Map<AbstractContainerScreen<?>, Anim> ANIMS = new WeakHashMap<>();

    private static final class Anim {
        boolean open;
        long changedAt;
        float from;
    }

    public static boolean open(AbstractContainerScreen<?> screen) {
        return screen.getMenu() instanceof DiggerMenuState s && s.aamoreadditions$fillersOpen();
    }

    /** Sets the client flag and tells the server (so server-side shift-click respects it). */
    private static void setOpen(ContainerMiner menu, boolean open) {
        ((DiggerMenuState) menu).aamoreadditions$setFillersOpen(open);
        sendToServer(menu, open);
    }

    private static void sendToServer(ContainerMiner menu, boolean open) {
        var mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId,
                open ? DiggerRuntime.BUTTON_FILLERS_OPEN : DiggerRuntime.BUTTON_FILLERS_CLOSE);
    }

    private static void press(ContainerMiner menu, int id) {
        var mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @SubscribeEvent
    static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof GuiMiner screen) || screen.getMenu().miner == null) return;
        ContainerMiner menu = screen.getMenu();
        TileEntityVerticalDigger tile = menu.miner;
        int l = screen.getGuiLeft(), t = screen.getGuiTop();
        Anim anim = ANIMS.get(screen);
        if (anim == null) { // first init of this screen (init also runs on resize)
            anim = new Anim();
            ANIMS.put(screen, anim);
            anim.open = remembered;
            anim.from = remembered ? 1f : 0f;
            if (remembered) setOpen(menu, true);
        }
        Anim a = anim;
        event.addListener(new ModeButton(l + BTN_X, t + MODE_Y, tile, () -> press(menu, DiggerRuntime.BUTTON_MODE)));
        event.addListener(new HighlightButton(l + BTN_X, t + HIGHLIGHT_Y, tile, () -> press(menu, DiggerRuntime.BUTTON_HIGHLIGHT)));
        Button toggle = new FillersToggleButton(l + DiggerMenus.TOGGLE_X, t + DiggerMenus.TOGGLE_Y, () -> a.open, b -> {
            boolean open = !a.open;
            a.from = UpgradeStrip.progress(a.from, a.open, a.changedAt);
            a.open = open;
            a.changedAt = Util.getMillis();
            remembered = open;
            if (!open) setOpen(menu, false); // slots disappear immediately; re-enabled when the slide finishes
            else sendToServer(menu, true);
            b.setTooltip(toggleTooltip(open));
        });
        toggle.setTooltip(toggleTooltip(a.open));
        event.addListener(toggle);
    }

    static Tooltip toggleTooltip(boolean open) {
        return Tooltip.create(Component.translatable(open ? "gui.aamoreadditions.digger.fillers.close" : "gui.aamoreadditions.digger.fillers.open"));
    }

    /**
     * Called (via mixin) before the machine's own renderBg, so the GUI covers the hidden part of the panel.
     * Client slots become active only once fully open, matching {@link UpgradeStrip#renderPanel}.
     */
    public static void renderPanel(AbstractContainerScreen<?> screen, GuiGraphics g, float partialTick) {
        if (!(screen instanceof GuiMiner)) return;
        Anim a = ANIMS.get(screen);
        if (a == null) return;
        float p = UpgradeStrip.progress(a.from, a.open, a.changedAt);
        if (a.open && p >= 1f && !((DiggerMenuState) screen.getMenu()).aamoreadditions$fillersOpen()) {
            ((DiggerMenuState) screen.getMenu()).aamoreadditions$setFillersOpen(true);
        }
        if (p <= 0f) return;
        int offset = UpgradeStrip.slideOffset(p, DiggerMenus.SLIDE);
        int x = screen.getGuiLeft() + DiggerMenus.PANEL_X - offset, y = screen.getGuiTop() + DiggerMenus.PANEL_Y;
        panel(g, x, y, DiggerMenus.PANEL_W, DiggerMenus.PANEL_H);
        // the machine's own 3x3 output slot frames (gui_breaker 61,20 54x54)
        g.blit(BREAKER, x + 4, y + 4, 61, 20, 54, 54, 256, 256);
    }

    /** Vanilla-style container panel: black outline (rounded corners), white/dark bevel, #C6C6C6 body. */
    static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, 0xFF000000);
        g.fill(x, y + 1, x + w, y + h - 1, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
        g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xFF555555);
    }

    /**
     * Requirement lines for the fillers inventory, evaluated independently: missing Block Placing
     * plugin, and/or not in AA's ores-only mode. Both are red; the list is empty when neither applies.
     * {@code missingAugment} is only the plugin check, not the combined locked state.
     */
    public static List<Component> fillerRequirementLines(boolean missingAugment, boolean onlyMineOres) {
        List<Component> lines = new ArrayList<>();
        if (missingAugment) lines.add(Component.translatable("gui.aamoreadditions.digger.fillers.required").withStyle(ChatFormatting.RED));
        if (!onlyMineOres) lines.add(Component.translatable("gui.aamoreadditions.digger.fillers.need_ores_mode").withStyle(ChatFormatting.RED));
        return lines;
    }

    /** Gap between the outer edges of the item tooltip box and the requirement box below it. */
    public static final int REQ_TOOLTIP_GAP = 2;
    /** Vanilla {@code TooltipRenderUtil} padding around tooltip content. */
    public static final int TOOLTIP_PAD = 3;

    /**
     * Positions a vanilla-style item tooltip and a requirement box stacked under it.
     * Starts at the DefaultTooltipPositioner origin (mouse +12, -12), flips left if the
     * wider box would cross the right edge, then shifts the pair up so the lower box's
     * bottom padding stays on screen, and clamps the top padding to the screen as well.
     * Returns {@code {itemX, itemY, reqX, reqY}} (content origins, not outer edges).
     */
    public static int[] stackedTooltipOrigin(int mouseX, int mouseY, int screenW, int screenH,
                                            int itemW, int itemH, int reqW, int reqH) {
        int x = mouseX + 12, y = mouseY - 12;
        int maxW = Math.max(itemW, reqW);
        if (x + maxW > screenW) x = Math.max(x - 24 - maxW, 4);
        int bottom = y + itemH + reqH + REQ_TOOLTIP_GAP + 9; // item bottom pad + gap + req top+bottom pads
        if (bottom > screenH) y = screenH - (itemH + reqH + REQ_TOOLTIP_GAP + 9);
        if (y < TOOLTIP_PAD) y = TOOLTIP_PAD;
        return new int[]{x, y, x, y + itemH + TOOLTIP_PAD + REQ_TOOLTIP_GAP + TOOLTIP_PAD};
    }

    /** Vanilla tooltip content height (single-line components are 2 px shorter in the measured box). */
    public static int tooltipHeight(List<ClientTooltipComponent> components) {
        int h = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent c : components) h += c.getHeight();
        return h;
    }

    public static int tooltipWidth(Font font, List<ClientTooltipComponent> components) {
        int w = 0;
        for (ClientTooltipComponent c : components) w = Math.max(w, c.getWidth(font));
        return w;
    }

    private static List<FormattedCharSequence> pendingReq;
    private static int pendingReqX, pendingReqY;

    /**
     * Barrier ghosts when locked (missing augment or not ores-only). Empty slots: just the red
     * requirement box. Filled slots: vanilla item tooltip first, then a separate red box below it.
     */
    @SubscribeEvent
    static void onForeground(ContainerScreenEvent.Render.Foreground event) {
        pendingReq = null;
        if (!(event.getContainerScreen() instanceof GuiMiner screen) || !open(screen)) return;
        GuiGraphics g = event.getGuiGraphics();
        var miner = screen.getMenu().miner;
        boolean onlyMineOres = miner != null && miner.onlyMineOres;
        boolean missingAugment = miner == null || !DiggerEffects.fillersUnlocked(miner);
        ItemStack barrier = new ItemStack(Items.BARRIER);
        Slot hovered = null;
        int mx = event.getMouseX() - screen.getGuiLeft(), my = event.getMouseY() - screen.getGuiTop();
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot instanceof DiggerFillerSlot f)) continue;
            if (f.locked()) {
                g.renderFakeItem(barrier, slot.x, slot.y);
                g.pose().pushPose();
                g.pose().translate(0, 0, 250);
                g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xA08B8B8B);
                g.pose().popPose();
            }
            if (mx >= slot.x - 1 && mx < slot.x + 17 && my >= slot.y - 1 && my < slot.y + 17) hovered = slot;
        }
        if (hovered == null) return;
        List<Component> lines = fillerRequirementLines(missingAugment, onlyMineOres);
        if (lines.isEmpty()) return;
        // Empty slot, or carrying an item (vanilla hides the slot's item tooltip): red box only.
        if (!hovered.hasItem() || !screen.getMenu().getCarried().isEmpty())
            screen.setTooltipForNextRenderPass(lines.stream().map(Component::getVisualOrderText).toList());
    }

    /** Shift the vanilla item tooltip up when a requirement box will sit under it. */
    @SubscribeEvent
    static void onItemTooltip(RenderTooltipEvent.Pre event) {
        if (!(Minecraft.getInstance().screen instanceof GuiMiner screen) || !open(screen)) return;
        if (!screen.getMenu().getCarried().isEmpty()) return;
        Slot hovered = screen.getSlotUnderMouse();
        if (!(hovered instanceof DiggerFillerSlot) || !hovered.hasItem()) return;
        var miner = screen.getMenu().miner;
        List<Component> lines = fillerRequirementLines(
                miner == null || !DiggerEffects.fillersUnlocked(miner),
                miner != null && miner.onlyMineOres);
        if (lines.isEmpty()) return;
        Font font = event.getFont();
        int itemW = tooltipWidth(font, event.getComponents()), itemH = tooltipHeight(event.getComponents());
        List<ClientTooltipComponent> req = new ArrayList<>();
        for (Component c : lines) req.add(ClientTooltipComponent.create(c.getVisualOrderText()));
        int[] pos = stackedTooltipOrigin(event.getX(), event.getY(), event.getScreenWidth(), event.getScreenHeight(),
                itemW, itemH, tooltipWidth(font, req), tooltipHeight(req));
        event.setX(pos[0] - 12);
        event.setY(pos[1] + 12);
        pendingReq = lines.stream().map(Component::getVisualOrderText).toList();
        pendingReqX = pos[2];
        pendingReqY = pos[3];
    }

    /** Draw the requirement box after the vanilla item tooltip, at the stacked position. */
    @SubscribeEvent
    static void onRenderPost(ScreenEvent.Render.Post event) {
        if (pendingReq == null || pendingReq.isEmpty()) return;
        List<FormattedCharSequence> lines = pendingReq;
        int x = pendingReqX, y = pendingReqY;
        pendingReq = null;
        if (!(event.getScreen() instanceof GuiMiner) || !open((AbstractContainerScreen<?>) event.getScreen())) return;
        ClientTooltipPositioner fixed = (sw, sh, mx, my, tw, th) -> new org.joml.Vector2i(x, y);
        event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, lines, fixed, x, y);
    }

    /** Clicks on the open panel (between slots) stay inside the GUI, so carried items are not thrown. */
    public static boolean insidePanel(AbstractContainerScreen<?> screen, double rx, double ry) {
        return screen instanceof GuiMiner && open(screen)
                && rx >= DiggerMenus.PANEL_X && rx < DiggerMenus.PANEL_X + DiggerMenus.PANEL_W
                && ry >= DiggerMenus.PANEL_Y && ry < DiggerMenus.PANEL_Y + DiggerMenus.PANEL_H;
    }

    // ---------------- buttons ----------------

    /** Strict Radius: 14x14 dashed frame (left/top-left red, right/top-right blue); Strict Chunk: green 田. */
    static final class ModeButton extends Button {
        private final TileEntityVerticalDigger tile;
        private Boolean shown;

        ModeButton(int x, int y, TileEntityVerticalDigger tile, Runnable press) {
            super(x, y, SIZE, SIZE, Component.empty(), b -> press.run(), DEFAULT_NARRATION);
            this.tile = tile;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean chunk = ((DiggerState) tile).aamoreadditions$strictChunk();
            if (shown == null || shown != chunk) {
                shown = chunk;
                setTooltip(Tooltip.create(Component.translatable(chunk ? "gui.aamoreadditions.digger.mode.strict_chunk" : "gui.aamoreadditions.digger.mode.strict_radius")));
            }
            RenderSystem.enableBlend();
            g.blitSprite(SPRITES.get(this.active, isHoveredOrFocused()), getX(), getY(), SIZE, SIZE);
            int ix = getX() + 1, iy = getY() + 1; // 16x16 icon area
            if (chunk) {
                for (int i = 1; i <= 14; i++) for (int b : new int[]{1, 2, 7, 8, 13, 14}) {
                    g.fill(ix + i, iy + b, ix + i + 1, iy + b + 1, GREEN);
                    g.fill(ix + b, iy + i, ix + b + 1, iy + i + 1, GREEN);
                }
            } else {
                for (int i = 0; i < 14; i++) {
                    if (DASH[i] == 0) continue;
                    int topBottom = i < 7 ? RED : BLUE;
                    g.fill(ix + 1 + i, iy + 1, ix + 2 + i, iy + 2, topBottom);
                    g.fill(ix + 1 + i, iy + 14, ix + 2 + i, iy + 15, topBottom);
                    g.fill(ix + 1, iy + 1 + i, ix + 2, iy + 2 + i, RED);
                    g.fill(ix + 14, iy + 1 + i, ix + 15, iy + 2 + i, BLUE);
                }
            }
            RenderSystem.disableBlend();
        }
    }

    /** The vanilla forcefield texture, tinted like the world border (On = stationary blue, Off = shrinking red), scrolling. */
    static final class HighlightButton extends Button {
        private final TileEntityVerticalDigger tile;
        private Boolean shown;

        HighlightButton(int x, int y, TileEntityVerticalDigger tile, Runnable press) {
            super(x, y, SIZE, SIZE, Component.empty(), b -> press.run(), DEFAULT_NARRATION);
            this.tile = tile;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean on = ((DiggerState) tile).aamoreadditions$highlight();
            if (shown == null || shown != on) {
                shown = on;
                setTooltip(Tooltip.create(Component.translatable("gui.aamoreadditions.digger.highlight",
                        Component.translatable(on ? "gui.aamoreadditions.digger.highlight.on" : "gui.aamoreadditions.digger.highlight.off")
                                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED))));
            }
            RenderSystem.enableBlend();
            g.blitSprite(SPRITES.get(this.active, isHoveredOrFocused()), getX(), getY(), SIZE, SIZE);
            int tint = on ? BORDER_BLUE : BORDER_RED;
            float s = (Util.getMillis() % 3000L) / 3000.0F * 16F; // vanilla UV + (t, t)
            g.setColor(((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F, (tint & 255) / 255F, 1F);
            g.blit(FORCEFIELD, getX() + 1, getY() + 1, s, s, 16, 16, 16, 16);
            g.setColor(1F, 1F, 1F, 1F);
            RenderSystem.disableBlend();
        }
    }

    /** 10x10 toggle, same look as the upgrade toggle, arrow ＞ (closed) / ＜ (open). */
    static final class FillersToggleButton extends Button {
        private final java.util.function.BooleanSupplier open;

        FillersToggleButton(int x, int y, java.util.function.BooleanSupplier open, OnPress press) {
            super(x, y, 10, 10, Component.empty(), press, DEFAULT_NARRATION);
            this.open = open;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            g.blit(TOGGLE, getX(), getY(), isHoveredOrFocused() ? 10 : 0, open.getAsBoolean() ? 10 : 0, 10, 10, 20, 20);
        }
    }
}
