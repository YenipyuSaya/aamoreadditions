package com.aamoreadditions.compat.jei;

import com.aamoreadditions.menu.AlloyFurnaceMenu;
import com.aamoreadditions.menu.AlloyTransfer;
import com.aamoreadditions.menu.ModMenus;
import com.aamoreadditions.network.ModNetwork;
import com.aamoreadditions.recipe.AlloyingRecipe;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Client side: checks with the same simulation as the server, then asks the server to move the items. */
public class AlloyTransferHandler implements IRecipeTransferHandler<AlloyFurnaceMenu, RecipeHolder<AlloyingRecipe>> {
    private final IRecipeTransferHandlerHelper helper;

    public AlloyTransferHandler(IRecipeTransferHandlerHelper helper) { this.helper = helper; }

    @Override public Class<? extends AlloyFurnaceMenu> getContainerClass() { return AlloyFurnaceMenu.class; }
    @Override public Optional<MenuType<AlloyFurnaceMenu>> getMenuType() { return Optional.of(ModMenus.ALLOY_FURNACE.get()); }
    @Override public RecipeType<RecipeHolder<AlloyingRecipe>> getRecipeType() { return JeiCompat.ALLOYING; }

    @Override
    public IRecipeTransferError transferRecipe(AlloyFurnaceMenu menu, RecipeHolder<AlloyingRecipe> recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        String id = recipe.id().toString();
        var r = AlloyTransfer.simulate(player, menu.furnace, id, recipe.value());
        switch (r.outcome()) {
            case IGNORED: return null; // locked to another recipe: button looks normal, click does nothing
            case MISSING: {
                List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT), miss = new ArrayList<>();
                for (int i : r.missing()) if (i < inputs.size()) miss.add(inputs.get(i));
                return helper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), miss);
            }
            case SLOT_OVERFLOW: return helper.createUserErrorWithTooltip(Component.translatable("jei.aamoreadditions.alloying.transfer.overflow"));
            case INVALID: return helper.createInternalError();
            default: break;
        }
        if (doTransfer) PacketDistributor.sendToServer(new ModNetwork.AlloyTransferPayload(id));
        return null;
    }
}
