package com.aamoreadditions.digger;

import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Pipe/hopper view of the digger: slots 0-8 are AA's output inventory unchanged (extract only),
 * slots 9-17 are the fillers: insert only, and only while placing is enabled (plugin installed, legal slots, ores-only mode).
 * Automation can never extract from the fillers.
 */
public final class DiggerAutomation {
    private DiggerAutomation() {}

    public static IItemHandler items(TileEntityVerticalDigger tile, IItemHandler output) {
        if (output == null) return null;
        return new IItemHandler() {
            private DiggerFillers fillers() { return DiggerFillers.of(tile); }
            private boolean filler(int slot) { return slot >= output.getSlots(); }
            private int f(int slot) { return slot - output.getSlots(); }
            private boolean valid(int slot) { return slot >= 0 && slot < getSlots(); }
            @Override public int getSlots() { return output.getSlots() + DiggerFillers.SIZE; }
            @Override public ItemStack getStackInSlot(int slot) {
                if (!valid(slot)) return ItemStack.EMPTY;
                return filler(slot) ? fillers().getStackInSlot(f(slot)) : output.getStackInSlot(slot);
            }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!valid(slot)) return stack;
                if (!filler(slot)) return output.insertItem(slot, stack, simulate);
                return DiggerRuntime.fillersAutomated(tile) ? fillers().insertItem(f(slot), stack, simulate) : stack;
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!valid(slot) || filler(slot)) return ItemStack.EMPTY;
                return output.extractItem(slot, amount, simulate);
            }
            @Override public int getSlotLimit(int slot) {
                if (!valid(slot)) return 0;
                return filler(slot) ? fillers().getSlotLimit(f(slot)) : output.getSlotLimit(slot);
            }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                if (!valid(slot)) return false;
                return filler(slot) ? DiggerRuntime.fillersAutomated(tile) : output.isItemValid(slot, stack);
            }
        };
    }
}
