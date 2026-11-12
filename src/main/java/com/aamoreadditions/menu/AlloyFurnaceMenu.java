package com.aamoreadditions.menu;

import com.aamoreadditions.block.AlloyFurnaceBlockEntity;
import com.aamoreadditions.upgrade.UpgradeMenus;
import de.ellpeck.actuallyadditions.mod.inventory.slot.SlotItemHandlerUnconditioned;
import de.ellpeck.actuallyadditions.mod.inventory.slot.SlotOutput;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPoweredFurnace;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Objects;

/** Alloy Furnace menu. Slot order: 0 input1, 1 input2, 2 output, 3 extra, 4..39 player, then upgrade slots. */
public class AlloyFurnaceMenu extends AbstractContainerMenu {
    public static final int SLOT_IN_1 = 0, SLOT_IN_2 = 1, SLOT_OUT = 2, SLOT_EXTRA = 3, INV_START = 4, INV_END = 40;

    public final AlloyFurnaceBlockEntity furnace;
    public final Slot extraSlot;

    public AlloyFurnaceMenu(int windowId, Inventory inventory, AlloyFurnaceBlockEntity tile) {
        super(ModMenus.ALLOY_FURNACE.get(), windowId);
        this.furnace = tile;
        this.addSlot(new LockedInput(tile, TileEntityPoweredFurnace.SLOT_INPUT_1, 71, 21));
        this.addSlot(new LockedInput(tile, TileEntityPoweredFurnace.SLOT_INPUT_2, 109, 21));
        this.addSlot(new SlotOutput(tile.inv, TileEntityPoweredFurnace.SLOT_OUTPUT_2, 109, 69));
        this.extraSlot = this.addSlot(new LockedExtra(tile, 63, 69));
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 97 + i * 18));
        for (int i = 0; i < 9; i++)
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 155));
        UpgradeMenus.addSlots(this, tile);
    }

    public static final int BUTTON_LOCK = 7710, BUTTON_MODE = 7711;

    /** Lock button (sent by the screen through handleInventoryButtonClick; processed on the server). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_LOCK) {
            if (!player.level().isClientSide) furnace.toggleLock();
            return true;
        }
        if (id == BUTTON_MODE) {
            if (!player.level().isClientSide) furnace.toggleMode();
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    /** Input slot that respects the recipe lock for manual placement / shift-click. */
    static final class LockedInput extends SlotItemHandlerUnconditioned {
        final AlloyFurnaceBlockEntity tile; final int lockSlot;
        LockedInput(AlloyFurnaceBlockEntity tile, int invSlot, int x, int y) {
            super(tile.inv, invSlot, x, y);
            this.tile = tile; this.lockSlot = AlloyFurnaceBlockEntity.lockSlotOf(invSlot);
        }
        @Override public boolean mayPlace(ItemStack stack) { return tile.lockAllows(lockSlot, stack) && super.mayPlace(stack); }
    }

    static final class LockedExtra extends SlotItemHandler {
        final AlloyFurnaceBlockEntity tile;
        LockedExtra(AlloyFurnaceBlockEntity tile, int x, int y) { super(tile.extra, 0, x, y); this.tile = tile; }
        @Override public boolean mayPlace(ItemStack stack) { return tile.lockAllows(AlloyFurnaceBlockEntity.LOCK_EXTRA, stack) && super.mayPlace(stack); }
    }

    public static AlloyFurnaceMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
        return new AlloyFurnaceMenu(windowId, inv, (AlloyFurnaceBlockEntity) Objects.requireNonNull(inv.player.level().getBlockEntity(data.readBlockPos())));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack up = UpgradeMenus.quickMove(this, player, index);
        if (up != null) return up;
        if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (index < INV_END) {
            // any item; lock (mayPlace) still applies. Pass 1: top up same stacks, pass 2: empty slots; order in1, in2, extra
            int[] order = {SLOT_IN_1, SLOT_IN_2, SLOT_EXTRA};
            boolean moved = false;
            for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++)
                for (int i : order) {
                    if (stack.isEmpty()) break;
                    ItemStack here = this.slots.get(i).getItem();
                    boolean ok = pass == 0 ? !here.isEmpty() && ItemStack.isSameItemSameComponents(here, stack) : here.isEmpty();
                    if (ok && this.moveItemStackTo(stack, i, i + 1, false)) moved = true;
                }
            if (!moved) {
                if (index < INV_START + 27) { if (!this.moveItemStackTo(stack, INV_START + 27, INV_END, false)) return ItemStack.EMPTY; }
                else if (!this.moveItemStackTo(stack, INV_START, INV_START + 27, false)) return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return furnace.canPlayerUse(player);
    }
}
