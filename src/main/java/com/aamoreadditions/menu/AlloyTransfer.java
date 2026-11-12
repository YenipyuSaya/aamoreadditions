package com.aamoreadditions.menu;

import com.aamoreadditions.block.AlloyFurnaceBlockEntity;
import com.aamoreadditions.recipe.AlloyFurnaceRecipe;
import com.aamoreadditions.recipe.TransferPlanner;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPoweredFurnace;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Shared (client check + server apply) logic for moving one recipe's worth of items from the player into the furnace.
 * Rule for items already in the three furnace slots: they are ALL returned to the player inventory first
 * (simulated on copies; if they don't fit nothing happens). Because returned items join the pool, ingredients
 * already sitting in the furnace are simply taken back, so nothing is lost and nothing extra is needed.
 * No JEI classes here.
 */
public final class AlloyTransfer {
    private AlloyTransfer() {}

    public enum Outcome { OK, IGNORED, MISSING, SLOT_OVERFLOW, INVALID }

    /** newInventory = 36 main slots after the move; drops = returned furnace items that no longer fit (dropped at the player). */
    public record Result(Outcome outcome, List<Integer> missing, List<ItemStack> newInventory, ItemStack[] newSlots, List<ItemStack> drops) {
        static Result fail(Outcome o) { return new Result(o, List.of(), null, null, List.of()); }
    }

    static ItemStack[] currentSlots(AlloyFurnaceBlockEntity f) {
        return new ItemStack[]{f.inv.getStackInSlot(TileEntityPoweredFurnace.SLOT_INPUT_1), f.inv.getStackInSlot(TileEntityPoweredFurnace.SLOT_INPUT_2), f.extra.getStackInSlot(0)};
    }

    /**
     * Pool = the 36 inventory slots + the current furnace slots (they are all returned). The plan takes from the pool;
     * whatever is left of the furnace items is merged back into the inventory, the rest becomes drops.
     * Everything is computed on copies; nothing is created or destroyed.
     */
    public static Result simulate(Player player, AlloyFurnaceBlockEntity f, String recipeId, AlloyFurnaceRecipe recipe) {
        if (f.lock.recorded() && !f.lock.recipeId().equals(recipeId)) return Result.fail(Outcome.IGNORED);
        List<ItemStack> pool = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) pool.add(s.copy());
        int invSize = pool.size();
        for (ItemStack s : currentSlots(f)) pool.add(s.copy());

        List<Predicate<ItemStack>> preds = new ArrayList<>();
        // alloying: ingredient #i (with its count) -> slot i; efficient smelting: one item per ingredient -> input 1/2/3
        int[] counts = new int[recipe.ingredientCount()];
        for (int i = 0; i < counts.length; i++) {
            var ing = recipe.ingredient(i);
            preds.add(s -> !s.isEmpty() && ing.test(s));
            counts[i] = recipe.ingredientAmount(i);
        }
        int[] poolCounts = new int[pool.size()];
        for (int i = 0; i < pool.size(); i++) poolCounts[i] = pool.get(i).getCount();
        var plan = TransferPlanner.plan(recipeId, preds, counts, f.lock, pool, poolCounts, ItemStack::isSameItemSameComponents, ItemStack::getMaxStackSize);
        switch (plan.status()) {
            case IGNORED: return Result.fail(Outcome.IGNORED);
            case MISSING: return new Result(Outcome.MISSING, plan.missing(), null, null, List.of());
            case SLOT_OVERFLOW: return Result.fail(Outcome.SLOT_OVERFLOW);
            case BAD_LOCK: return Result.fail(Outcome.INVALID);
            default: break;
        }
        ItemStack[] slots = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        for (int s = 0; s < 3; s++) {
            if (plan.slotSource()[s] < 0) continue;
            slots[s] = pool.get(plan.slotSource()[s]).copyWithCount(plan.slotAmount()[s]);
            if (!f.lockAllows(s, slots[s])) return Result.fail(Outcome.INVALID);
            if (s == AlloyFurnaceBlockEntity.LOCK_EXTRA && !f.extra.isItemValid(0, slots[s])) return Result.fail(Outcome.INVALID);
        }
        // inventory model after taking
        List<ItemStack> kinds = new ArrayList<>();
        int[] invCounts = new int[invSize];
        for (int i = 0; i < invSize; i++) {
            invCounts[i] = poolCounts[i] - plan.take()[i];
            kinds.add(invCounts[i] > 0 ? pool.get(i) : null);
        }
        // leftovers of the returned furnace items -> inventory, overflow -> drops
        List<ItemStack> drops = new ArrayList<>();
        for (int p = invSize; p < pool.size(); p++) {
            int left = poolCounts[p] - plan.take()[p];
            if (left <= 0) continue;
            int over = TransferPlanner.returnInto(kinds, invCounts, pool.get(p), left, ItemStack::isSameItemSameComponents, ItemStack::getMaxStackSize);
            if (over > 0) drops.add(pool.get(p).copyWithCount(over));
        }
        List<ItemStack> newInv = new ArrayList<>();
        for (int i = 0; i < invSize; i++) newInv.add(kinds.get(i) == null || invCounts[i] <= 0 ? ItemStack.EMPTY : kinds.get(i).copyWithCount(invCounts[i]));
        return new Result(Outcome.OK, List.of(), newInv, slots, drops);
    }

    /** Server: validate and apply. Silently does nothing on any failure. */
    public static void apply(Player player, String recipeId) {
        if (!(player.containerMenu instanceof AlloyFurnaceMenu menu) || !menu.stillValid(player)) return;
        AlloyFurnaceBlockEntity f = menu.furnace;
        if (com.aamoreadditions.upgrade.UpgradeRuntime.blocked(f)) return;
        AlloyFurnaceRecipe recipe = com.aamoreadditions.recipe.ModRecipes.furnaceById(player.level(), recipeId); // either type
        if (recipe == null) return;
        Result r = simulate(player, f, recipeId, recipe);
        if (r.outcome() != Outcome.OK) return;
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) items.set(i, r.newInventory().get(i));
        f.inv.setStackInSlot(TileEntityPoweredFurnace.SLOT_INPUT_1, r.newSlots()[0]);
        f.inv.setStackInSlot(TileEntityPoweredFurnace.SLOT_INPUT_2, r.newSlots()[1]);
        f.extra.setStackInSlot(0, r.newSlots()[2]);
        f.setChanged();
        player.getInventory().setChanged();
        // overflow of the returned furnace items: dropped at the player's feet (vanilla drop, normal pickup delay)
        for (ItemStack d : r.drops()) player.drop(d, false);
        menu.broadcastChanges();
    }
}
