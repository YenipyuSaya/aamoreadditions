package com.aamoreadditions.compat.jei;

import com.aamoreadditions.block.ModBlocks;
import com.aamoreadditions.recipe.AlloyingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Arrays;
import java.util.List;

/** Layout (116x40): up to 3 ingredient slots in a row (unordered), animated arrow, output slot. */
public class AlloyingCategory implements IRecipeCategory<RecipeHolder<AlloyingRecipe>> {
    static final int W = 116, H = 40, SLOT_Y = 16, ARROW_X = 60, OUT_X = 94;

    private final IDrawable icon, slot, outSlot, arrowEmpty;
    private final IDrawableAnimated arrow;

    public AlloyingCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.ALLOY_FURNACE_ITEM.get()));
        this.slot = gui.getSlotDrawable();
        this.outSlot = gui.getOutputSlot();
        this.arrowEmpty = gui.getRecipeArrow();
        this.arrow = gui.createAnimatedRecipeArrow(80);
    }

    @Override public RecipeType<RecipeHolder<AlloyingRecipe>> getRecipeType() { return JeiCompat.ALLOYING; }
    @Override public Component getTitle() { return Component.translatable("jei.aamoreadditions.alloying"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return W; }
    @Override public int getHeight() { return H; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AlloyingRecipe> holder, IFocusGroup focuses) {
        List<SizedIngredient> ings = holder.value().ingredients();
        for (int i = 0; i < ings.size(); i++) {
            // getItems() already carries the ingredient count; tags rotate automatically
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, SLOT_Y + 1)
                    .setBackground(slot, -1, -1)
                    .addItemStacks(Arrays.asList(ings.get(i).getItems()))
                    .addRichTooltipCallback(tagTooltip(ings.get(i)));
        }
        // same-tag output: every item the player can choose in the furnace, rotating (sorted by id)
        var res = holder.value().result();
        String group = res.group();
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 5, SLOT_Y - 3)
                .setBackground(outSlot, -5, -5)
                .addItemStacks(res.candidates())
                .addRichTooltipCallback((view, tip) -> {
                    if (group != null && res.candidates().size() > 1)
                        tip.add(Component.translatable("jei.aamoreadditions.alloying.tag", group).withStyle(net.minecraft.ChatFormatting.GRAY));
                });
    }

    /** Adds "Tag: c:ingots/xxx" (grey) for tag ingredients so the tag id is visible. */
    private static mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback tagTooltip(SizedIngredient si) {
        java.util.List<String> tags = new java.util.ArrayList<>();
        for (var v : si.ingredient().getValues()) if (v instanceof net.minecraft.world.item.crafting.Ingredient.TagValue tv) tags.add(tv.tag().location().toString());
        return (view, tip) -> {
            for (String t : tags) tip.add(Component.translatable("jei.aamoreadditions.alloying.tag", t).withStyle(net.minecraft.ChatFormatting.GRAY));
        };
    }

    @Override
    public void draw(RecipeHolder<AlloyingRecipe> recipe, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        arrowEmpty.draw(g, ARROW_X, SLOT_Y);
        arrow.draw(g, ARROW_X, SLOT_Y);
    }
}
