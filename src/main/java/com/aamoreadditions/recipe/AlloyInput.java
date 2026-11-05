package com.aamoreadditions.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Input 1, input 2, extra slot (in that order). */
public record AlloyInput(ItemStack in1, ItemStack in2, ItemStack extra) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) { case 0 -> in1; case 1 -> in2; case 2 -> extra; default -> ItemStack.EMPTY; };
    }

    @Override
    public int size() { return 3; }
}
