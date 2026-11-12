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
    /** Grouped entries: one per recipe shape (raw x2, raw x3, ore, raw block, dust, ...), cycling through all metals. */
    public static final RecipeType<EfficientGroup> EFFICIENT = RecipeType.create(AAMoreAdditions.MODID, "efficient_smelting", EfficientGroup.class);

    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "jei"); }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        reg.addRecipeCategories(new AlloyingCategory(reg.getJeiHelpers().getGuiHelper()),
                new EfficientSmeltingCategory(reg.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        reg.addRecipes(ALLOYING, ModRecipes.available(level)); // recipes whose output tag has no item are hidden
        reg.addRecipes(EFFICIENT, EfficientGroup.build(ModRecipes.availableEfficient(level))); // per shape, not per metal; empty tags hidden
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        // both pages show up for "uses" on the furnace item
        reg.addRecipeCatalyst(ModBlocks.ALLOY_FURNACE_ITEM.get(), ALLOYING, EFFICIENT);
    }

    @Override
    public void registerRecipeTransferHandlers(mezz.jei.api.registration.IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(new AlloyTransferHandler(reg.getTransferHelper()), ALLOYING);
        reg.addRecipeTransferHandler(new EfficientTransferHandler(reg.getTransferHelper()), EFFICIENT);
        reg.addRecipeTransferHandler(new EmpowererTransferHandler(reg.getTransferHelper()), de.ellpeck.actuallyadditions.mod.jei.JEIActuallyAdditionsPlugin.EMPOWERER);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        // same-tag selection overlay: JEI recipe/usage keys work on the listed items
        reg.addGuiContainerHandler(AlloyFurnaceScreen.class, new mezz.jei.api.gui.handlers.IGuiContainerHandler<AlloyFurnaceScreen>() {
            @Override
            public java.util.Optional<? extends mezz.jei.api.runtime.IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    mezz.jei.api.gui.builder.IClickableIngredientFactory factory, AlloyFurnaceScreen screen, double mx, double my) {
                int i = screen.overlayIndexAt(mx, my);
                if (i < 0) return java.util.Optional.empty();
                return factory.createBuilder(screen.overlayStackAt(mx, my)).buildWithArea(screen.pickFrameX(i) + 1, screen.pickFrameY() + 1, 16, 16);
            }

            /** Same click area in both modes; opens the category of the current mode. */
            @Override
            public java.util.Collection<mezz.jei.api.gui.handlers.IGuiClickableArea> getGuiClickableAreas(AlloyFurnaceScreen screen, double mx, double my) {
                if (screen.overlayOpen()) return java.util.List.of();
                return java.util.List.of(mezz.jei.api.gui.handlers.IGuiClickableArea.createBasic(
                        AlloyFurnaceScreen.RECIPE_AREA_X, AlloyFurnaceScreen.RECIPE_AREA_Y,
                        AlloyFurnaceScreen.RECIPE_AREA_W, AlloyFurnaceScreen.RECIPE_AREA_H,
                        screen.efficientMode() ? EFFICIENT : ALLOYING));
            }
        });
        // Empowerer structure: jump to AA's own Empowerer category (no category of ours)
        reg.addRecipeClickArea(com.aamoreadditions.client.screen.CasingCrossScreen.class,
                com.aamoreadditions.client.screen.CasingCrossScreen.RECIPE_AREA_X, com.aamoreadditions.client.screen.CasingCrossScreen.RECIPE_AREA_Y,
                com.aamoreadditions.client.screen.CasingCrossScreen.RECIPE_AREA_W, com.aamoreadditions.client.screen.CasingCrossScreen.RECIPE_AREA_H,
                de.ellpeck.actuallyadditions.mod.jei.JEIActuallyAdditionsPlugin.EMPOWERER);
    }
}
