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

    private ModRecipes() {}

    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }

    /** First recipe that matches (with its per-slot plan), or null. */
    public static Match find(Level level, AlloyInput input) {
        if (level == null) return null;
        for (RecipeHolder<AlloyingRecipe> h : level.getRecipeManager().getAllRecipesFor(ALLOYING.get())) {
            int[][] take = h.value().planDetailed(input);
            if (take != null) {
                int[] plan = new int[3];
                for (int[] row : take) for (int s = 0; s < 3; s++) plan[s] += row[s];
                return new Match(h, plan, take);
            }
        }
        return null;
    }

    public static boolean isIngredient(Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) return false;
        for (RecipeHolder<AlloyingRecipe> h : level.getRecipeManager().getAllRecipesFor(ALLOYING.get())) if (h.value().uses(stack)) return true;
        return false;
    }

    /** Alloying recipe by id, or null (missing / other type). */
    public static AlloyingRecipe byId(Level level, String id) {
        if (level == null || id == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        var h = level.getRecipeManager().byKey(rl).orElse(null);
        return h != null && h.value() instanceof AlloyingRecipe r ? r : null;
    }

    public record Match(RecipeHolder<AlloyingRecipe> recipe, int[] plan, int[][] take) {}
}
