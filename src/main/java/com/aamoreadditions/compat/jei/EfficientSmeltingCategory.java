package com.aamoreadditions.compat.jei;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.block.ModBlocks;
import com.aamoreadditions.recipe.EfficientGrouping;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

/**
 * "Alloy Furnace: Efficient Smelting": one entry per recipe SHAPE (see {@link EfficientGrouping}), not per metal.
 * All slots cycle in sync through the metals of that shape. With a focus (R/U on raw tin, a tin ingot, ...) only the
 * matching metal's rows are shown. Layout 116x44: inputs 1/2/3, vanilla flame filling upwards + base time, output.
 */
public class EfficientSmeltingCategory implements IRecipeCategory<EfficientGroup> {
    static final int W = AlloyingCategory.W, H = AlloyingCategory.H + 4, SLOT_Y = AlloyingCategory.SLOT_Y,
            FLAME_X = 63, FLAME_Y = SLOT_Y + 2, OUT_X = AlloyingCategory.OUT_X;

    private final IDrawable icon, slot, outSlot, flameEmpty;
    private final IGuiHelper gui;
    private final java.util.Map<Integer, IDrawableAnimated> flames = new java.util.HashMap<>();

    public EfficientSmeltingCategory(IGuiHelper gui) {
        this.gui = gui;
        this.icon = gui.createDrawableItemStack(new ItemStack(ModBlocks.ALLOY_FURNACE_ITEM.get()));
        this.slot = gui.getSlotDrawable();
        this.outSlot = gui.getOutputSlot();
        this.flameEmpty = gui.getRecipeFlameEmpty();
    }

    /** Vanilla lit flame filling bottom-to-top (like the furnace GUI in this mode), one cycle = the shape's base time. */
    private IDrawableAnimated flame(int ticks) {
        return flames.computeIfAbsent(ticks, t -> gui.createAnimatedDrawable(gui.getRecipeFlameFilled(), t, IDrawableAnimated.StartDirection.BOTTOM, false));
    }

    @Override public RecipeType<EfficientGroup> getRecipeType() { return JeiCompat.EFFICIENT; }
    @Override public Component getTitle() { return Component.translatable("jei.aamoreadditions.efficient_smelting"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return W; }
    @Override public int getHeight() { return H; }

    @Override
    public ResourceLocation getRegistryName(EfficientGroup g) {
        var s = g.shape();
        return ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID,
                "efficient_smelting/" + s.kind().name().toLowerCase(java.util.Locale.ROOT) + "_" + s.slots() + "_" + s.count() + "_" + s.ticks());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EfficientGroup group, IFocusGroup focuses) {
        List<List<ItemStack>> rows = group.rows();
        // focus: keep only the focused metal's rows so inputs and output stay in sync
        var in = focuses.getItemStackFocuses(RecipeIngredientRole.INPUT).findFirst();
        var out = focuses.getItemStackFocuses(RecipeIngredientRole.OUTPUT).findFirst();
        if (in.isPresent()) rows = EfficientGrouping.focus(rows, in.get().getTypedValue().getIngredient(), false, ItemStack::isSameItem);
        else if (out.isPresent()) rows = EfficientGrouping.focus(rows, out.get().getTypedValue().getIngredient(), true, ItemStack::isSameItem);
        int n = group.shape().slots();
        for (int i = 0; i < n; i++) {
            final int slotIndex = i;
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, SLOT_Y + 1)
                    .setBackground(slot, -1, -1)
                    .addItemStacks(EfficientGrouping.column(rows, i))
                    .addRichTooltipCallback((view, tip) -> view.getDisplayedItemStack().ifPresent(st -> {
                        for (String t : inputTags(group, slotIndex, st)) tip.add(tagLine(t));
                    }));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 5, SLOT_Y - 3)
                .setBackground(outSlot, -5, -5)
                .addItemStacks(EfficientGrouping.column(rows, n))
                .addRichTooltipCallback((view, tip) -> view.getDisplayedItemStack().ifPresent(st -> {
                    String t = outputTag(group, st);
                    if (t != null) tip.add(tagLine(t));
                }));
    }

    private static Component tagLine(String tag) {
        return Component.translatable("jei.aamoreadditions.alloying.tag", tag).withStyle(net.minecraft.ChatFormatting.GRAY);
    }

    /** Tags of input slot i of the member recipe that accepts this stack. */
    static List<String> inputTags(EfficientGroup g, int i, ItemStack st) {
        for (var h : g.members()) {
            var r = h.value();
            if (i < r.ingredientCount() && r.ingredient(i).test(st)) {
                List<String> l = new java.util.ArrayList<>();
                for (var v : r.ingredient(i).getValues()) if (v instanceof Ingredient.TagValue tv) l.add(tv.tag().location().toString());
                return l;
            }
        }
        return List.of();
    }

    /** Output tag of the member recipe that produces this item. */
    static String outputTag(EfficientGroup g, ItemStack st) {
        for (var h : g.members()) {
            var res = h.value().result();
            if (res.group() != null && res.candidates().stream().anyMatch(c -> ItemStack.isSameItem(c, st))) return res.group();
        }
        return null;
    }

    /** "4s", "32s", "2.5s" (base time, upgrades not included). */
    static String seconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.valueOf(Math.round(ticks / 2.0) / 10.0);
    }

    @Override
    public void draw(EfficientGroup group, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        int ticks = group.shape().ticks();
        flameEmpty.draw(g, FLAME_X, FLAME_Y);
        flame(ticks).draw(g, FLAME_X, FLAME_Y);
        Component t = Component.translatable("jei.aamoreadditions.efficient_smelting.time", seconds(ticks));
        var font = net.minecraft.client.Minecraft.getInstance().font;
        g.drawString(font, t, FLAME_X + 7 - font.width(t) / 2, FLAME_Y + 16, 0xFF808080, false);
    }
}
