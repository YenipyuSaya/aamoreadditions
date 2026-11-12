package com.aamoreadditions.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * aamoreadditions:alloying — 1..3 sized ingredients (item or tag, with count), unordered over input 1 / input 2 / extra.
 * JSON: {"type":"aamoreadditions:alloying","ingredients":[{"tag":"c:ingots/copper","count":1},...],"result":{"id":"create:brass_ingot","count":2}}
 * Result may also be a tag: "result":{"tag":"c:ingots/bronze","count":4} (see {@link AlloyResult}).
 */
public record AlloyingRecipe(List<SizedIngredient> ingredients, AlloyResult result) implements AlloyFurnaceRecipe {

    @Override public int ingredientCount() { return ingredients.size(); }
    @Override public Ingredient ingredient(int i) { return ingredients.get(i).ingredient(); }
    @Override public int ingredientAmount(int i) { return ingredients.get(i).count(); }
    @Override public boolean available() { return result.available(); }

    /** Per-slot amounts to consume (in1, in2, extra) or null. */
    public int[] plan(AlloyInput in) {
        int[][] t = planDetailed(in);
        if (t == null) return null;
        int[] c = new int[3];
        for (int[] row : t) for (int s = 0; s < 3; s++) c[s] += row[s];
        return c;
    }

    /** take[ingredient][slot] or null. */
    @Override
    public int[][] planDetailed(AlloyInput in) {
        List<Predicate<ItemStack>> preds = new ArrayList<>();
        int[] need = new int[ingredients.size()];
        for (int i = 0; i < need.length; i++) {
            Ingredient ing = ingredients.get(i).ingredient();
            preds.add(s -> !s.isEmpty() && ing.test(s));
            need[i] = ingredients.get(i).count();
        }
        List<ItemStack> items = List.of(in.in1(), in.in2(), in.extra());
        int[] counts = {in.in1().getCount(), in.in2().getCount(), in.extra().getCount()};
        for (int s = 0; s < 3; s++) if (items.get(s).isEmpty()) counts[s] = 0;
        return AlloyMatcher.matchDetailed(preds, need, items, counts);
    }

    /** True if the stack matches ingredient #i. */
    @Override
    public boolean ingredientMatches(int i, ItemStack stack) {
        return i >= 0 && i < ingredients.size() && !stack.isEmpty() && ingredients.get(i).ingredient().test(stack);
    }

    /** True if the stack could be part of this recipe. */
    @Override
    public boolean uses(ItemStack stack) {
        for (SizedIngredient si : ingredients) if (si.ingredient().test(stack)) return true;
        return false;
    }

    @Override public boolean matches(AlloyInput input, Level level) { return plan(input) != null; }
    /** Without a furnace's choice: the fixed item, else the only / first candidate (furnaces use {@link AlloyResult#produce}). */
    @Override public ItemStack assemble(AlloyInput input, HolderLookup.Provider registries) { return displayResult(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return displayResult(); }

    public ItemStack displayResult() {
        if (!result.isTag()) return result.fixedItem().copy();
        var c = result.candidates();
        return c.isEmpty() ? ItemStack.EMPTY : c.get(0);
    }
    @Override public boolean isSpecial() { return true; } // keep out of the vanilla recipe book

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> l = NonNullList.create();
        for (SizedIngredient si : ingredients) l.add(si.ingredient());
        return l;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ALLOYING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.ALLOYING.get(); }

    public static final class Serializer implements RecipeSerializer<AlloyingRecipe> {
        public static final MapCodec<AlloyingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.list(SizedIngredient.FLAT_CODEC, 1, 3).fieldOf("ingredients").forGetter(AlloyingRecipe::ingredients),
                AlloyResult.CODEC.fieldOf("result").forGetter(AlloyingRecipe::result)
        ).apply(i, AlloyingRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(3)), AlloyingRecipe::ingredients,
                AlloyResult.STREAM_CODEC, AlloyingRecipe::result,
                AlloyingRecipe::new);

        @Override public MapCodec<AlloyingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
