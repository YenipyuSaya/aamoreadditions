package com.aamoreadditions.recipe;

import com.aamoreadditions.AAMoreAdditions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, AAMoreAdditions.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AAMoreAdditions.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AlloyingRecipe>> ALLOYING =
            TYPES.register("alloying", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "alloying")));
    public static final DeferredHolder<RecipeSerializer<?>, AlloyingRecipe.Serializer> ALLOYING_SERIALIZER =
            SERIALIZERS.register("alloying", AlloyingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<EfficientSmeltingRecipe>> EFFICIENT =
            TYPES.register("efficient_smelting", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "efficient_smelting")));
    public static final DeferredHolder<RecipeSerializer<?>, EfficientSmeltingRecipe.Serializer> EFFICIENT_SERIALIZER =
            SERIALIZERS.register("efficient_smelting", EfficientSmeltingRecipe.Serializer::new);

    private ModRecipes() {}

    /** Efficient-smelting recipes whose input and output exist in this game (for JEI). */
    public static java.util.List<RecipeHolder<EfficientSmeltingRecipe>> availableEfficient(Level level) {
        if (level == null) return java.util.List.of();
        return level.getRecipeManager().getAllRecipesFor(EFFICIENT.get()).stream().filter(h -> h.value().available()).toList();
    }

    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }

    /** First alloying recipe that matches (with its per-slot plan), or null. */
    public static Match find(Level level, AlloyInput input) {
        if (level == null) return null;
        for (RecipeHolder<AlloyingRecipe> h : level.getRecipeManager().getAllRecipesFor(ALLOYING.get())) {
            if (!h.value().result().available()) continue; // output tag has no item in this game
            int[][] take = h.value().planDetailed(input);
            if (take != null) {
                int[] plan = new int[3];
                for (int[] row : take) for (int s = 0; s < 3; s++) plan[s] += row[s];
                return new Match(h, plan, take);
            }
        }
        return null;
    }

    /** Efficient-smelting recipe to run for these slots (see {@link EfficientMatcher} for the choice rules), or null. */
    public static Match findEfficient(Level level, AlloyInput input) {
        if (level == null) return null;
        java.util.List<EfficientMatcher.Candidate<RecipeHolder<EfficientSmeltingRecipe>>> cands = new java.util.ArrayList<>();
        for (RecipeHolder<EfficientSmeltingRecipe> h : level.getRecipeManager().getAllRecipesFor(EFFICIENT.get())) {
            if (!h.value().result().available()) continue;
            int[] a = h.value().assign(input);
            if (a != null) cands.add(new EfficientMatcher.Candidate<>(h, h.id().toString(), h.value().kind().priority, a));
        }
        var c = EfficientMatcher.pick(cands);
        return c == null ? null : new Match(c.recipe(), c.plan(), c.take());
    }

    /** Match for the furnace's current mode. */
    public static Match find(Level level, AlloyInput input, boolean efficient) {
        return efficient ? findEfficient(level, input) : find(level, input);
    }

    /** Any alloy-furnace input (alloying or efficient smelting): both modes accept all of them; the recipe lock filters. */
    public static boolean isIngredient(Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        for (RecipeHolder<AlloyingRecipe> h : level.getRecipeManager().getAllRecipesFor(ALLOYING.get())) if (h.value().uses(stack) && h.value().result().available()) return true;
        for (RecipeHolder<EfficientSmeltingRecipe> h : level.getRecipeManager().getAllRecipesFor(EFFICIENT.get())) if (h.value().uses(stack) && h.value().result().available()) return true;
        return false;
    }

    /** Alloy-furnace recipe (either type) by id, or null. */
    public static AlloyFurnaceRecipe furnaceById(Level level, String id) {
        if (level == null || id == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        var h = level.getRecipeManager().byKey(rl).orElse(null);
        return h != null && h.value() instanceof AlloyFurnaceRecipe r ? r : null;
    }

    /** Alloying recipe by id, or null (missing / other type). */
    public static AlloyingRecipe byId(Level level, String id) {
        if (level == null || id == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        var h = level.getRecipeManager().byKey(rl).orElse(null);
        return h != null && h.value() instanceof AlloyingRecipe r ? r : null;
    }

    /** Recipes whose output exists in this game (for JEI). */
    public static java.util.List<RecipeHolder<AlloyingRecipe>> available(Level level) {
        if (level == null) return java.util.List.of();
        return level.getRecipeManager().getAllRecipesFor(ALLOYING.get()).stream().filter(h -> h.value().result().available()).toList();
    }

    public record Match(RecipeHolder<? extends AlloyFurnaceRecipe> recipe, int[] plan, int[][] take) {}
}
