package com.aamoreadditions.compat.jei;

import com.aamoreadditions.menu.CasingCrossMenu;
import com.aamoreadditions.menu.EmpowererTransfer;
import com.aamoreadditions.menu.ModMenus;
import com.aamoreadditions.network.ModNetwork;
import com.aamoreadditions.structure.CasingCrossData;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
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

/** "+" in AA's Empowerer JEI category while the Empowerer structure GUI is open. */
public class EmpowererTransferHandler implements IRecipeTransferHandler<CasingCrossMenu, EmpowererRecipe> {
    private final IRecipeTransferHandlerHelper helper;

    public EmpowererTransferHandler(IRecipeTransferHandlerHelper helper) { this.helper = helper; }

    @Override public Class<? extends CasingCrossMenu> getContainerClass() { return CasingCrossMenu.class; }
    @Override public Optional<MenuType<CasingCrossMenu>> getMenuType() { return Optional.of(ModMenus.CASING_CROSS.get()); }
    @Override public RecipeType<EmpowererRecipe> getRecipeType() { return de.ellpeck.actuallyadditions.mod.jei.JEIActuallyAdditionsPlugin.EMPOWERER; }

    @Override
    public IRecipeTransferError transferRecipe(CasingCrossMenu menu, EmpowererRecipe recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        // AA's JEI category holds bare recipes: find the holder (id) by identity
        RecipeHolder<EmpowererRecipe> holder = null;
        for (var h : CasingCrossData.sortedRecipes(player.level().getRecipeManager())) if (h.value() == recipe) { holder = h; break; }
        if (holder == null) return helper.createInternalError();
        ItemStack[] cur = new ItemStack[5];
        for (int s = 0; s < 5; s++) cur[s] = menu.slots.get(s).getItem();
        var locked = menu.lockedRecipe();
        String recId = menu.lockRecorded() ? (locked == null ? "?" : locked.id().toString()) : null;
        var r = EmpowererTransfer.simulate(player, cur, recId, menu.lockMap(), holder);
        switch (r.outcome()) {
            case IGNORED: return null;
            case MISSING: {
                List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT), miss = new ArrayList<>();
                for (int i : r.missing()) if (i < inputs.size()) miss.add(inputs.get(i));
                return helper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), miss);
            }
            case SLOT_OVERFLOW: return helper.createUserErrorWithTooltip(Component.translatable("jei.aamoreadditions.empowering.transfer.overflow"));
            case INVALID: return helper.createInternalError();
            default: break;
        }
        if (doTransfer) PacketDistributor.sendToServer(new ModNetwork.EmpowererTransferPayload(holder.id().toString()));
        return null;
    }
}
