package com.aamoreadditions.digger;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * A filler slot (panel right of the GUI). Any item may be inserted only while the placing effect is
 * enabled (Block Placing plugin installed and AA ores-only mode). Taking items out is always allowed.
 */
public class DiggerFillerSlot extends SlotItemHandler {
    private final AbstractContainerMenu menu;
    private final DiggerFillers fillers;

    public DiggerFillerSlot(AbstractContainerMenu menu, DiggerFillers fillers, int index) {
        super(fillers, index, DiggerMenus.SLOT_X0 + (index % 3) * 18, DiggerMenus.SLOT_Y0 + (index / 3) * 18);
        this.menu = menu;
        this.fillers = fillers;
    }

    public boolean locked() {
        return DiggerEffects.fillersLocked(fillers.tile());
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !locked() && super.mayPlace(stack);
    }

    @Override
    public boolean isActive() {
        return ((DiggerMenuState) menu).aamoreadditions$fillersOpen();
    }
}
