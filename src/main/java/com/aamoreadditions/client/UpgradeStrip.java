package com.aamoreadditions.client;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.upgrade.UpgradeMenuState;
import com.aamoreadditions.upgrade.UpgradeMenus;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.aamoreadditions.upgrade.UpgradeRules;
import com.aamoreadditions.upgrade.UpgradeSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client side of the upgrade panel: toggle button, slide animation, per-machine-type memory.
 * Texture: assets/aamoreadditions/textures/gui/upgrade_strip.png
 */
@EventBusSubscriber(modid = AAMoreAdditions.MODID, value = Dist.CLIENT)
public final class UpgradeStrip {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "textures/gui/upgrade_strip.png");
    /** Panel geometry (fully open) relative to the original GUI's top-left (title sits at y=-10). */
    public static final int X = 38, Y = -38, W = 100, H = 26;
    /** Texture file size. */
    public static final int TEX_W = 128, TEX_H = 32;
    /** Total vertical space added above the GUI (panel + title row). */
    public static final int EXTRA_HEIGHT = 38;
    /** Distance the panel travels: closed = completely hidden behind the original GUI. */
    public static final int SLIDE = 38;
    /** Empowerer structure: panel sits only 5 px above the GUI top (no title row above it). */
    public static final int COMPACT_Y = -31;
    public static boolean compact(AbstractContainerMenu m) { return m instanceof com.aamoreadditions.menu.CasingCrossMenu; }
    public static int stripY(AbstractContainerMenu m) { return compact(m) ? COMPACT_Y : Y; }
    public static int extraHeight(AbstractContainerMenu m) { return compact(m) ? -COMPACT_Y : EXTRA_HEIGHT; }
    public static int slide(AbstractContainerMenu m) { return compact(m) ? -COMPACT_Y : SLIDE; }
    /** Toggle button (10x10), top-right inside the original GUI border; left side is reserved for an FE bar. */
    public static final int BTN_Y = 4;

    /** Per-GUI x, checked against AA textures/widgets (see report). */
    static int buttonX(AbstractContainerScreen<?> screen) {
        if (screen instanceof de.ellpeck.actuallyadditions.mod.inventory.gui.CrusherScreen c
                && c.getMenu().isDouble) return 134;                                   // double crusher: inner edge x=146
        if (screen instanceof de.ellpeck.actuallyadditions.mod.inventory.gui.GuiFurnaceDouble) return 134; // inner edge x=146
        if (screen instanceof com.aamoreadditions.client.screen.AlloyFurnaceScreen) return 134;
        if (screen instanceof com.aamoreadditions.client.screen.CasingCrossScreen) return com.aamoreadditions.client.screen.CasingCrossScreen.UPGRADE_BTN_X;            // same frame as double furnace
        if (screen instanceof de.ellpeck.actuallyadditions.mod.inventory.gui.GuiOilGenerator) return 104;  // fluid bar occupies x116-133
        return 124;                                                                       // crusher, coal generator: inner edge x=135
    }
    public static final long ANIM_MS = 180;

    /** Remembered open state per machine type for this session. */
    private static final Map<MenuType<?>, Boolean> REMEMBERED = new HashMap<>();
    private static final Map<AbstractContainerScreen<?>, Anim> ANIMS = new WeakHashMap<>();

    private static final class Anim {
        boolean open;          // target state
        long changedAt;        // millis when toggled
        float from;            // progress at toggle time
    }

    private UpgradeStrip() {}

    /** Number of upgrade slots in this menu (5, or 2 for the energizer). */
    public static int slotCount(AbstractContainerMenu menu) {
        int n = 0;
        for (Slot s : menu.slots) if (s instanceof UpgradeSlot) n++;
        return n;
    }

    /** Panel x / width for n slots (5 -> 38 / 100, unchanged; 2 -> 65 / 46), centred like the slots. */
    public static int panelX(int n) { return UpgradeSlot.x0(n) - 6; }
    public static int panelW(int n) { return n * 18 + 10; }

    public static boolean contains(AbstractContainerMenu menu, double rx, double ry) {
        int n = slotCount(menu), x = panelX(n);
        return n > 0 && rx >= x && rx < x + panelW(n) && ry >= stripY(menu) && ry < stripY(menu) + H;
    }

    public static boolean isOpen(AbstractContainerMenu menu) {
        return UpgradeMenus.hasUpgradeSlots(menu) && ((UpgradeMenuState) menu).aamoreadditions$isUpgradeOpen();
    }

    private static float progress(Anim a) {
        float t = Math.min(1f, (Util.getMillis() - a.changedAt) / (float) ANIM_MS);
        float target = a.open ? 1f : 0f;
        float lin = a.from + (target - a.from) * t;
        return lin;
    }

    private static float ease(float p) { // ease-in-out cubic
        return p < 0.5f ? 4 * p * p * p : 1 - (float) Math.pow(-2 * p + 2, 3) / 2;
    }

    @SubscribeEvent
    static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !UpgradeMenus.hasUpgradeSlots(screen.getMenu())) return;
        AbstractContainerMenu menu = screen.getMenu();
        Anim anim = ANIMS.get(screen);
        if (anim == null) { // first init of this screen (init also runs on resize)
            anim = new Anim();
            ANIMS.put(screen, anim);
            boolean open = REMEMBERED.getOrDefault(menu.getType(), false);
            anim.open = open;
            anim.from = open ? 1f : 0f;
            if (open) setOpen(menu, true);
        }
        Anim a = anim;
        Button button = new UpgradeToggleButton(screen.getGuiLeft() + buttonX(screen), screen.getGuiTop() + BTN_Y, () -> a.open, b -> {
                    boolean open = !a.open;
                    a.from = progress(a);
                    a.open = open;
                    a.changedAt = Util.getMillis();
                    REMEMBERED.put(menu.getType(), open);
                    if (!open) setOpen(menu, false); // slots disappear immediately; re-enabled when the slide finishes
                    else sendToServer(menu, true);
                    b.setTooltip(tooltip(open));
                });
        button.setTooltip(tooltip(a.open));
        event.addListener(button);
        net.minecraft.world.level.block.entity.BlockEntity be = null;
        for (Slot s : menu.slots) if (s instanceof UpgradeSlot u && u.handler().holder() instanceof net.minecraft.world.level.block.entity.BlockEntity b) { be = b; break; }
        if (screen.getMenu() instanceof com.aamoreadditions.menu.CasingCrossMenu cm) {
            // Empowerer structure: speed only
            event.addListener(new UpgradeInfoButton(screen.getGuiLeft() + buttonX(screen), screen.getGuiTop() + BTN_Y + UpgradeToggleButton.SIZE + 1,
                    () -> {
                        var lines = new java.util.ArrayList<Component>();
                        lines.add(Component.translatable("gui.aamoreadditions.info.speed", Component.literal(UpgradeInfo.fmt(cm.speedMultiplier())).withStyle(net.minecraft.ChatFormatting.AQUA)).withStyle(net.minecraft.ChatFormatting.WHITE));
                        UpgradeInfo.appendBoundary(lines,cm.upgrades);
                        return lines;
                    }));
        } else if (be != null && com.aamoreadditions.effect.MachineKind.of(be) != null && com.aamoreadditions.effect.MachineKind.of(be) != com.aamoreadditions.effect.MachineKind.ENERGIZER) {
            final var fbe = be;
            event.addListener(new UpgradeInfoButton(screen.getGuiLeft() + buttonX(screen), screen.getGuiTop() + BTN_Y + UpgradeToggleButton.SIZE + 1, () -> UpgradeInfo.lines(fbe)));
        }
    }

    private static Tooltip tooltip(boolean open) {
        return Tooltip.create(Component.translatable(open ? "gui.aamoreadditions.upgrades.close" : "gui.aamoreadditions.upgrades.open"));
    }

    /** Sets the client flag and tells the server (so server-side shift-click respects it). */
    private static void setOpen(AbstractContainerMenu menu, boolean open) {
        ((UpgradeMenuState) menu).aamoreadditions$setUpgradeOpen(open);
        sendToServer(menu, open);
    }

    private static void sendToServer(AbstractContainerMenu menu, boolean open) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, open ? UpgradeMenuState.BUTTON_OPEN : UpgradeMenuState.BUTTON_CLOSE);
    }

    /** Called (via mixin) before the machine's own renderBg, so the GUI covers the hidden part of the panel. */
    public static void renderPanel(AbstractContainerScreen<?> screen, GuiGraphics g, float partialTick) {
        Anim a = ANIMS.get(screen);
        if (a == null) return;
        float p = progress(a);
        // Client slots become active only once fully open.
        if (a.open && p >= 1f && !((UpgradeMenuState) screen.getMenu()).aamoreadditions$isUpgradeOpen()) {
            ((UpgradeMenuState) screen.getMenu()).aamoreadditions$setUpgradeOpen(true);
        }
        if (p <= 0f) return;
        int offset = Math.round((1f - ease(p)) * slide(screen.getMenu()));
        int n = slotCount(screen.getMenu()), px = screen.getGuiLeft() + panelX(n), py = screen.getGuiTop() + stripY(screen.getMenu()) + offset;
        // Repeat a real cell; never sample beyond the original five-cell texture.
        g.blit(TEXTURE,px,py,0,0,5,H,TEX_W,TEX_H);
        for(int i=0;i<n;i++) g.blit(TEXTURE,px+5+i*18,py,5,0,18,H,TEX_W,TEX_H);
        g.blit(TEXTURE,px+5+n*18,py,W-5,0,5,H,TEX_W,TEX_H);
    }

    // ---- Rejection message shown above the GUI (client-side, from synced slot contents) ----
    public static final long MSG_MS = 2500, FADE_MS = 800;
    private static Component message;
    private static long messageAt;

    /** Called (via mixin) at the start of mouseClicked, before vanilla handles the click. */
    public static void onClick(AbstractContainerScreen<?> screen, Slot hovered) {
        AbstractContainerMenu menu = screen.getMenu();
        if (hovered == null || !isOpen(menu)) return;
        UpgradeSlot first = null;
        for (Slot s : menu.slots) if (s instanceof UpgradeSlot u) { first = u; break; }
        if (first == null) return;
        UpgradeRules.Check check = UpgradeRules.Check.OK;
        if (hovered instanceof UpgradeSlot u) {
            ItemStack carried = menu.getCarried();
            if (!carried.isEmpty() && carried.is(com.aamoreadditions.upgrade.UpgradeSlots.UPGRADES)) {
                check = UpgradeRules.canInstall(u.handler(), carried, u.getSlotIndex());
            }
        } else if (hovered.container instanceof Inventory && Screen.hasShiftDown() && menu.getCarried().isEmpty()
                && hovered.getItem().is(com.aamoreadditions.upgrade.UpgradeSlots.UPGRADES)) {
            check = UpgradeRules.canInstall(first.handler(), hovered.getItem(), -1);
        }
        if (check == UpgradeRules.Check.DUPLICATE || check == UpgradeRules.Check.MISSING_PREREQ) {
            message = check.message();
            messageAt = Util.getMillis();
        }
    }

    @SubscribeEvent
    static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !UpgradeMenus.hasUpgradeSlots(screen.getMenu())) return;
        boolean illegal = false;
        for (Slot slot : screen.getMenu().slots) if (slot instanceof UpgradeSlot u) { illegal = !u.handler().legal(); break; }
        if (illegal) {
            var g = event.getGuiGraphics();
            g.pose().pushPose(); g.pose().translate(0,0,400);
            g.drawCenteredString(Minecraft.getInstance().font,Component.translatable("message.aamoreadditions.upgrades.wrong_logic"),
                    screen.getGuiLeft()+screen.getXSize()/2,screen.getGuiTop()+stripY(screen.getMenu())-12,0xFFFF5555);
            g.pose().popPose(); return;
        }
        if (message == null) return;
        long age = Util.getMillis() - messageAt;
        if (age > MSG_MS) { message = null; return; }
        float alpha = age < MSG_MS - FADE_MS ? 1f : 1f - (age - (MSG_MS - FADE_MS)) / (float) FADE_MS;
        int a = Mth.clamp((int) (alpha * 255), 5, 255); // font skips alpha < 4
        Anim anim = ANIMS.get(screen);
        boolean panelShown = anim != null && progress(anim) > 0f;
        int y = screen.getGuiTop() + (panelShown ? stripY(screen.getMenu()) - 12 : -22);
        GuiGraphics g = event.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0, 0, 400); // above slots/items
        g.drawCenteredString(Minecraft.getInstance().font, message, screen.getGuiLeft() + screen.getXSize() / 2, y, (a << 24) | 0xFFFFFF);
        g.pose().popPose();
    }
}
