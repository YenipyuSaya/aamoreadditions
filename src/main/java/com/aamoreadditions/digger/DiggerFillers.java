package com.aamoreadditions.digger;

import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The 3x3 filler inventory. Accepts any item; only block items are ever placed. */
public final class DiggerFillers extends ItemStackHandler {
    public static final int SIZE = 9;
    public static final String NBT = "aamoreadditions_digger_fillers";
    private final TileEntityVerticalDigger tile;

    public DiggerFillers(TileEntityVerticalDigger tile) {
        super(SIZE);
        this.tile = tile;
    }

    public TileEntityVerticalDigger tile() { return tile; }

    public static boolean placeable(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    public java.util.List<ItemStack> contents() { return stacks; }

    public boolean hasPlaceable() {
        return DiggerScan.firstPlaceable(stacks, ItemStack::isEmpty, DiggerFillers::placeable, 0) >= 0;
    }

    @Override
    protected void onContentsChanged(int slot) {
        tile.setChanged();
    }

    public static DiggerFillers of(TileEntityVerticalDigger tile) {
        return ((DiggerState) tile).aamoreadditions$fillers();
    }
}
