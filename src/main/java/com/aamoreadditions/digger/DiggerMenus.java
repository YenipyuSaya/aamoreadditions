package com.aamoreadditions.digger;

import com.aamoreadditions.mixin.AbstractContainerMenuInvoker;
import com.aamoreadditions.upgrade.UpgradeSlot;
import de.ellpeck.actuallyadditions.mod.inventory.ContainerMiner;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Fillers panel geometry (GUI-relative) and server-side menu logic. */
public final class DiggerMenus {
    private DiggerMenus() {}

    /** Panel right of the gui_breaker head (opaque up to x=148); slot rows aligned with the output rows (y 21/39/57). */
    public static final int PANEL_X = 151, PANEL_Y = 16, PANEL_W = 62, PANEL_H = 62;
    /** Right edge of the opaque gui_breaker head; the panel hides behind this while closed. */
    public static final int HEAD_RIGHT = 148;
    /** Distance the panel travels (same idea as {@link com.aamoreadditions.client.UpgradeStrip#SLIDE}). */
    public static final int SLIDE = PANEL_X + PANEL_W - HEAD_RIGHT;
    public static final int SLOT_X0 = PANEL_X + 5, SLOT_Y0 = PANEL_Y + 5;
    /**
     * Fillers toggle: 10x10, same column as the upgrade toggle/info (UpgradeStrip.buttonX(GuiMiner)=134).
     * Vertically centred on AA's 3x3 output slots at (62+j*18, 21+i*18): item area y=21..73, centre 47, top=42.
     */
    public static final int TOGGLE_SIZE = 10, TOGGLE_X = 134, TOGGLE_Y = 42;

    public static void addSlots(ContainerMiner menu) {
        if (menu.miner == null) return;
        DiggerFillers fillers = DiggerFillers.of(menu.miner);
        for (int i = 0; i < DiggerFillers.SIZE; i++)
            ((AbstractContainerMenuInvoker) menu).aamoreadditions$addSlot(new DiggerFillerSlot(menu, fillers, i));
    }

    public static boolean click(ContainerMiner menu, Player player, int id) {
        if (id == DiggerRuntime.BUTTON_FILLERS_OPEN || id == DiggerRuntime.BUTTON_FILLERS_CLOSE) {
            ((DiggerMenuState) menu).aamoreadditions$setFillersOpen(id == DiggerRuntime.BUTTON_FILLERS_OPEN);
            return true;
        }
        if (id != DiggerRuntime.BUTTON_MODE && id != DiggerRuntime.BUTTON_HIGHLIGHT) return false;
        if (menu.miner == null || !menu.stillValid(player) || player.level().isClientSide) return true;
        return DiggerRuntime.button(menu.miner, id);
    }

    /**
     * Shift-click for slots AA's quickMoveStack must never see (AA treats every index >= 9 as player inventory).
     * Returns null to let the upgrade logic / AA handle it.
     */
    public static ItemStack quickMove(ContainerMiner menu, Player player, int index) {
        if (index < 0 || index >= menu.slots.size()) return ItemStack.EMPTY;
        Slot source = menu.slots.get(index);
        int fStart = -1, fEnd = -1, invStart = -1, invEnd = -1;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s instanceof DiggerFillerSlot) { if (fStart < 0) fStart = i; fEnd = i + 1; }
            else if (s.container instanceof Inventory) { if (invStart < 0) invStart = i; invEnd = i + 1; }
        }
        var inv = (AbstractContainerMenuInvoker) menu;
        if (source instanceof DiggerFillerSlot) {
            if (!source.hasItem() || !source.isActive() || !source.mayPickup(player) || invStart < 0) return ItemStack.EMPTY;
            ItemStack stack = source.getItem(), copy = stack.copy();
            if (!inv.aamoreadditions$moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            if (stack.isEmpty()) source.set(ItemStack.EMPTY); else source.setChanged();
            source.onTake(player, stack);
            return copy;
        }
        if (source instanceof UpgradeSlot) return ItemStack.EMPTY; // closed panel / rejected: never into AA's output slots
        if (source.container instanceof Inventory && source.hasItem() && fStart >= 0
                && ((DiggerMenuState) menu).aamoreadditions$fillersOpen() && !((DiggerFillerSlot) menu.slots.get(fStart)).locked()) {
            ItemStack stack = source.getItem(), copy = stack.copy();
            if (!inv.aamoreadditions$moveItemStackTo(stack, fStart, fEnd, false)) return null; // fillers full: AA's own logic
            if (stack.isEmpty()) source.set(ItemStack.EMPTY); else source.setChanged();
            return copy;
        }
        return null;
    }

    public static boolean isMiner(AbstractContainerMenu menu) {
        return menu instanceof ContainerMiner;
    }
}
