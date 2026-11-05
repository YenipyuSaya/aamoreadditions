package com.aamoreadditions.upgrade;

import com.aamoreadditions.mixin.AbstractContainerMenuInvoker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class UpgradeMenus {
    private UpgradeMenus() {}

    /** Appends the upgrade slots AFTER all existing slots so AA's index math keeps working. */
    public static void addSlots(AbstractContainerMenu menu, BlockEntity be) {
        if (be == null || !UpgradeSlots.supports(be)) return;
        UpgradeSlots.Handler h = UpgradeSlots.get(be);
        for (int i = 0; i < h.getSlots(); i++) {
            ((AbstractContainerMenuInvoker) menu).aamoreadditions$addSlot(new UpgradeSlot(menu, h, i));
        }
    }

    public static boolean hasUpgradeSlots(AbstractContainerMenu menu) {
        for (Slot s : menu.slots) if (s instanceof UpgradeSlot) return true;
        return false;
    }

    /**
     * Shift-click handling for upgrade-related moves. Returns null to let the original
     * quickMoveStack run.
     */
    public static ItemStack quickMove(AbstractContainerMenu menu, net.minecraft.world.entity.player.Player player, int index) {
        if (index < 0 || index >= menu.slots.size()) return null;
        Slot slot = menu.slots.get(index);
        if (!slot.hasItem() || !((UpgradeMenuState) menu).aamoreadditions$isUpgradeOpen()) return null;
        int upStart = -1, upEnd = -1, invStart = -1, invEnd = -1;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s instanceof UpgradeSlot) { if (upStart < 0) upStart = i; upEnd = i + 1; }
            else if (s.container instanceof Inventory) { if (invStart < 0) invStart = i; invEnd = i + 1; }
        }
        if (upStart < 0 || invStart < 0) return null;
        AbstractContainerMenuInvoker inv = (AbstractContainerMenuInvoker) menu;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (slot instanceof UpgradeSlot) {
            if (!slot.mayPickup(player)) return ItemStack.EMPTY;
            if (!inv.aamoreadditions$moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else if (slot.container instanceof Inventory && stack.is(UpgradeSlots.UPGRADES)) {
            UpgradeSlots.Handler h = ((UpgradeSlot) menu.slots.get(upStart)).handler();
            if (UpgradeRules.canInstall(h, stack, -1) != UpgradeRules.Check.OK) return ItemStack.EMPTY; // rejected: do nothing
            if (!inv.aamoreadditions$moveItemStackTo(stack, upStart, upEnd, false)) return null; // slots full: fall back to AA logic
        } else {
            return null;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
