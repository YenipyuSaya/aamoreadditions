package com.aamoreadditions.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

/** Common view of the two Alloy Furnace recipe types (alloying / efficient smelting) used by the furnace, lock and GUI. */
public interface AlloyFurnaceRecipe extends Recipe<AlloyInput> {
    AlloyResult result();
    int ingredientCount();
    Ingredient ingredient(int i);
    /** take[ingredient][slot] (slots: input 1, input 2, input 3 = extra) or null. */
    int[][] planDetailed(AlloyInput in);
    boolean ingredientMatches(int i, ItemStack stack);
    boolean uses(ItemStack stack);
    /** Standard processing time in ticks (before upgrades). */
    int STANDARD_TIME = 80;
    /** Processing time in ticks before upgrades (alloying: 80). */
    default int baseTime() { return STANDARD_TIME; }
    /** Items of ingredient #i per operation (alloying: its count; efficient smelting: 1). */
    int ingredientAmount(int i);
    /** Recipe shows / runs only if its items exist in this game. */
    boolean available();
}
