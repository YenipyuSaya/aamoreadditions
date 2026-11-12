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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * aamoreadditions:efficient_smelting — Alloy Furnace in Efficient-Smelting mode.
 * <pre>
 * {"type":"aamoreadditions:efficient_smelting",
 *  "ingredients":[{"tag":"c:raw_materials/iron"},{"tag":"c:raw_materials/iron"}],  // 1..3, one per input slot (input 3 = extra)
 *  "result":{"tag":"c:ingots/iron","count":3},                                     // or {"id":...}; same-tag choice applies
 *  "time":200}                                                                      // optional, ticks before upgrades
 * </pre>
 * Each ingredient consumes exactly one item from its own slot (stack size irrelevant). See {@link EfficientMatcher}.
 * Kind (priority, default time) comes from the ingredient tags: dust > raw block (8x time) > ore > raw ore ({@link EfficientKind}).
 * Default time when "time" is absent: 80 ticks x the kind's factor (raw blocks 640).
 */
public record EfficientSmeltingRecipe(List<Ingredient> ingredients, AlloyResult result, java.util.Optional<Integer> time) implements AlloyFurnaceRecipe {

    public EfficientSmeltingRecipe(List<Ingredient> ingredients, AlloyResult result) { this(ingredients, result, java.util.Optional.empty()); }

    @Override public int ingredientAmount(int i) { return 1; }

    @Override public int ingredientCount() { return ingredients.size(); }
    @Override public Ingredient ingredient(int i) { return ingredients.get(i); }

    @Override
    public boolean available() {
        for (Ingredient i : ingredients) if (i.getItems().length == 0) return false;
        return result.available();
    }

    /** Input category from the first ingredient's tags (tag ingredients) or its items' tags (item ingredients). */
    public EfficientKind kind() {
        if (ingredients.isEmpty()) return EfficientKind.OTHER;
        List<String> tags = new ArrayList<>();
        for (var v : ingredients.get(0).getValues()) {
            if (v instanceof Ingredient.TagValue tv) tags.add(tv.tag().location().toString());
            else for (ItemStack st : v.getItems()) st.getTags().forEach(t -> tags.add(t.location().toString()));
        }
        return EfficientKind.ofTags(tags);
    }

    @Override public int baseTime() { return time.orElseGet(() -> STANDARD_TIME * kind().timeFactor); }

    /** Slot of each ingredient, or null. */
    public int[] assign(AlloyInput in) {
        List<Predicate<ItemStack>> preds = new ArrayList<>();
        for (Ingredient ing : ingredients) preds.add(s -> !s.isEmpty() && ing.test(s));
        List<ItemStack> items = List.of(in.in1(), in.in2(), in.extra());
        boolean[] occ = {!in.in1().isEmpty(), !in.in2().isEmpty(), !in.extra().isEmpty()};
        return EfficientMatcher.assign(preds, items, occ);
    }

    @Override
    public int[][] planDetailed(AlloyInput in) {
        int[] a = assign(in);
        if (a == null) return null;
        int[][] t = new int[a.length][3];
        for (int i = 0; i < a.length; i++) t[i][a[i]] = 1;
        return t;
    }

    @Override public boolean ingredientMatches(int i, ItemStack stack) {
        return i >= 0 && i < ingredients.size() && !stack.isEmpty() && ingredients.get(i).test(stack);
    }

    @Override public boolean uses(ItemStack stack) {
        for (Ingredient i : ingredients) if (i.test(stack)) return true;
        return false;
    }

    @Override public boolean matches(AlloyInput input, Level level) { return assign(input) != null; }
    @Override public ItemStack assemble(AlloyInput input, HolderLookup.Provider registries) { return displayResult(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return displayResult(); }
    @Override public boolean isSpecial() { return true; }

    public ItemStack displayResult() {
        if (!result.isTag()) return result.fixedItem().copy();
        var c = result.candidates();
        return c.isEmpty() ? ItemStack.EMPTY : c.get(0);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> l = NonNullList.create();
        l.addAll(ingredients);
        return l;
    }

    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.EFFICIENT_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.EFFICIENT.get(); }

    public static final class Serializer implements RecipeSerializer<EfficientSmeltingRecipe> {
        public static final MapCodec<EfficientSmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.list(Ingredient.CODEC_NONEMPTY, 1, 3).fieldOf("ingredients").forGetter(EfficientSmeltingRecipe::ingredients),
                AlloyResult.CODEC.fieldOf("result").forGetter(EfficientSmeltingRecipe::result),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("time").forGetter(EfficientSmeltingRecipe::time)
        ).apply(i, EfficientSmeltingRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EfficientSmeltingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list(3)), EfficientSmeltingRecipe::ingredients,
                AlloyResult.STREAM_CODEC, EfficientSmeltingRecipe::result,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), EfficientSmeltingRecipe::time,
                EfficientSmeltingRecipe::new);

        @Override public MapCodec<EfficientSmeltingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, EfficientSmeltingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
