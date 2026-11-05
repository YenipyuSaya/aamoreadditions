package com.aamoreadditions.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuInvoker {
    @Invoker("addSlot")
    Slot aamoreadditions$addSlot(Slot slot);

    @Invoker("moveItemStackTo")
    boolean aamoreadditions$moveItemStackTo(ItemStack stack, int start, int end, boolean reverse);
}
