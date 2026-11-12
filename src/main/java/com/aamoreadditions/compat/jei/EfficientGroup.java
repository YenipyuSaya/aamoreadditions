package com.aamoreadditions.compat.jei;

import com.aamoreadditions.recipe.EfficientGrouping;
import com.aamoreadditions.recipe.EfficientSmeltingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One JEI entry of "Alloy Furnace: Efficient Smelting": every loaded recipe of one shape (all metals). */
public record EfficientGroup(EfficientGrouping.Shape shape, List<RecipeHolder<EfficientSmeltingRecipe>> members) {

    public static EfficientGrouping.Shape shapeOf(EfficientSmeltingRecipe r) {
        return new EfficientGrouping.Shape(r.kind(), r.ingredientCount(), r.result().count(), r.baseTime());
    }

    /** Groups for JEI from the available recipes (members sorted by output tag / id, i.e. by metal). */
    public static List<EfficientGroup> build(List<RecipeHolder<EfficientSmeltingRecipe>> recipes) {
        List<RecipeHolder<EfficientSmeltingRecipe>> sorted = new ArrayList<>(recipes);
        sorted.sort(java.util.Comparator.comparing((RecipeHolder<EfficientSmeltingRecipe> h) -> String.valueOf(h.value().result().group()))
                .thenComparing(h -> h.id().toString()));
        List<EfficientGroup> out = new ArrayList<>();
        for (var e : EfficientGrouping.group(sorted, h -> shapeOf(h.value()))) out.add(new EfficientGroup(e.getKey(), List.copyOf(e.getValue())));
        return out;
    }

    /** Per-member slot lists: inputs 1..n, then the output candidates. */
    public List<List<List<ItemStack>>> memberSlots() {
        List<List<List<ItemStack>>> l = new ArrayList<>();
        for (var h : members) {
            var r = h.value();
            List<List<ItemStack>> slots = new ArrayList<>();
            for (int i = 0; i < r.ingredientCount(); i++) slots.add(Arrays.asList(r.ingredient(i).getItems()));
            slots.add(r.result().candidates());
            l.add(slots);
        }
        return l;
    }

    /** Aligned rows (one metal / item variant per row). */
    public List<List<ItemStack>> rows() { return EfficientGrouping.align(memberSlots()); }
}
