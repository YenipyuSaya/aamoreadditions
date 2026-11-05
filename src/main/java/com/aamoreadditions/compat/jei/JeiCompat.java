package com.aamoreadditions.compat.jei;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.block.ModBlocks;
import com.aamoreadditions.client.screen.AlloyFurnaceScreen;
import com.aamoreadditions.recipe.AlloyingRecipe;
import com.aamoreadditions.recipe.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Loaded by JEI only (annotation scan); nothing else in the mod references this package. */
@JeiPlugin
public class JeiCompat implements IModPlugin {
    public static final RecipeType<RecipeHolder<AlloyingRecipe>> ALLOYING =
            RecipeType.createRecipeHolderType(ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "alloying"));

    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "jei"); }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new AlloyingCategory(reg.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        reg.addRecipes(ALLOYING, level.getRecipeManager().getAllRecipesFor(ModRecipes.ALLOYING.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        reg.addRecipeCatalyst(ModBlocks.ALLOY_FURNACE_ITEM.get(), ALLOYING);
    }

    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(new AlloyTransferHandler(reg.getTransferHelper()), ALLOYING);
        reg.addRecipeTransferHandler(new EmpowererTransferHandler(reg.getTransferHelper()), de.ellpeck.actuallyadditions.mod.jei.JEIActuallyAdditionsPlugin.EMPOWERER);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        reg.addRecipeClickArea(AlloyFurnaceScreen.class, AlloyFurnaceScreen.RECIPE_AREA_X, AlloyFurnaceScreen.RECIPE_AREA_Y,
                AlloyFurnaceScreen.RECIPE_AREA_W, AlloyFurnaceScreen.RECIPE_AREA_H, ALLOYING);
        // Empowerer structure: jump to AA's own Empowerer category (no category of ours)
        reg.addRecipeClickArea(com.aamoreadditions.client.screen.CasingCrossScreen.class,
                com.aamoreadditions.client.screen.CasingCrossScreen.RECIPE_AREA_X, com.aamoreadditions.client.screen.CasingCrossScreen.RECIPE_AREA_Y,
                com.aamoreadditions.client.screen.CasingCrossScreen.RECIPE_AREA_W, com.aamoreadditions.client.screen.CasingCrossScreen.RECIPE_AREA_H,
                de.ellpeck.actuallyadditions.mod.jei.JEIActuallyAdditionsPlugin.EMPOWERER);
    }
}
