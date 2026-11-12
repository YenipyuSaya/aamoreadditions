package com.aamoreadditions.compat.jei;

import com.aamoreadditions.menu.AlloyFurnaceMenu;
import com.aamoreadditions.menu.AlloyTransfer;
import com.aamoreadditions.menu.ModMenus;
import com.aamoreadditions.network.ModNetwork;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JEI "+" for a grouped efficient-smelting entry. The concrete recipe is the metal currently shown in input 1;
 * if the player lacks it, the first metal of the entry they CAN fill is used. One item per input slot 1/2/3.
 */
public class EfficientTransferHandler implements IRecipeTransferHandler<AlloyFurnaceMenu, EfficientGroup> {
    private final IRecipeTransferHandlerHelper helper;

    public EfficientTransferHandler(IRecipeTransferHandlerHelper helper) { this.helper = helper; }

    @Override public Class<? extends AlloyFurnaceMenu> getContainerClass() { return AlloyFurnaceMenu.class; }
    @Override public Optional<MenuType<AlloyFurnaceMenu>> getMenuType() { return Optional.of(ModMenus.ALLOY_FURNACE.get()); }
    @Override public RecipeType<EfficientGroup> getRecipeType() { return JeiCompat.EFFICIENT; }

    @Override
    public IRecipeTransferError transferRecipe(AlloyFurnaceMenu menu, EfficientGroup group, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
        ItemStack shown = inputs.isEmpty() ? ItemStack.EMPTY : inputs.get(0).getDisplayedItemStack().orElse(ItemStack.EMPTY);
        // shown metal first, then the rest in entry order
        List<RecipeHolder<com.aamoreadditions.recipe.EfficientSmeltingRecipe>> order = new ArrayList<>();
        for (var h : group.members()) if (!shown.isEmpty() && h.value().ingredient(0).test(shown)) order.add(h);
        for (var h : group.members()) if (!order.contains(h)) order.add(h);
        AlloyTransfer.Result first = null;
        for (var h : order) {
            String id = h.id().toString();
            var r = AlloyTransfer.simulate(player, menu.furnace, id, h.value());
            if (r.outcome() == AlloyTransfer.Outcome.OK) {
                if (doTransfer) PacketDistributor.sendToServer(new ModNetwork.AlloyTransferPayload(id));
                return null;
            }
            if (first == null) first = r;
        }
        if (first == null) return helper.createInternalError();
        switch (first.outcome()) {
            case IGNORED: return null; // locked to another recipe: click does nothing (same as the alloy page)
            case MISSING: {
                List<IRecipeSlotView> miss = new ArrayList<>();
                for (int i : first.missing()) if (i < inputs.size()) miss.add(inputs.get(i));
                return helper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), miss);
            }
            case SLOT_OVERFLOW: return helper.createUserErrorWithTooltip(Component.translatable("jei.aamoreadditions.alloying.transfer.overflow"));
            default: return helper.createInternalError();
        }
    }
}
