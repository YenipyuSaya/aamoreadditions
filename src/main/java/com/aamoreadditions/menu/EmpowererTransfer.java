package com.aamoreadditions.menu;

import com.aamoreadditions.recipe.TransferPlanner;
import com.aamoreadditions.structure.CasingCrossData;
import de.ellpeck.actuallyadditions.mod.crafting.EmpowererRecipe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI "+" for the Empowerer structure, shared by the client check and the server apply (no JEI classes).
 * Same rule as the Alloy Furnace: everything in the 5 input slots is returned to the player first (on copies),
 * then exactly one craft is placed; leftovers that don't fit are dropped at the player. Offhand/armour are not used.
 * Ingredient order = AA's JEI input order: 0 base, 1..4 standOne..standFour.
 */
public final class EmpowererTransfer {
    private EmpowererTransfer() {}

    public enum Outcome { OK, IGNORED, MISSING, SLOT_OVERFLOW, INVALID }

    public record Result(Outcome outcome, List<Integer> missing, List<ItemStack> newInventory, ItemStack[] newSlots, List<ItemStack> drops) {
        static Result fail(Outcome o) { return new Result(o, List.of(), null, null, List.of()); }
    }

    /** Unlocked/pending targets: standOne..Four -> up, right, down, left (the priority order after the centre). */
    private static final int[] STAND_TARGET = {CasingCrossData.SLOT_UP, CasingCrossData.SLOT_RIGHT, CasingCrossData.SLOT_DOWN, CasingCrossData.SLOT_LEFT};

    /**
     * @param current the 5 input slots (0 up, 1 down, 2 left, 3 right, 4 base)
     * @param lockMap recorded slot map (null if not recorded)
     */
    public static Result simulate(Player player, ItemStack[] current, String recordedId, int[] lockMap, RecipeHolder<EmpowererRecipe> recipe) {
        String id = recipe.id().toString();
        boolean recorded = recordedId != null;
        if (recorded && !recordedId.equals(id)) return Result.fail(Outcome.IGNORED);
        EmpowererRecipe r = recipe.value();
        Ingredient[] ings = {r.getInput(), r.getStandOne(), r.getStandTwo(), r.getStandThree(), r.getStandFour()};
        int[] target = new int[5];
        for (int i = 0; i < 5; i++) {
            if (!recorded) target[i] = i == 0 ? CasingCrossData.SLOT_BASE : STAND_TARGET[i - 1];
            else {
                int want = i == 0 ? CasingCrossData.LOCK_BASE : i - 1, found = -1;
                for (int s = 0; s < 5; s++) if (lockMap[s] == want) { found = s; break; }
                if (found < 0) return Result.fail(Outcome.INVALID);
                target[i] = found;
            }
        }
        // pool: 36 main inventory slots, then the 5 structure input slots (returned)
        List<ItemStack> pool = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) pool.add(s.copy());
        int invSize = pool.size();
        for (ItemStack s : current) pool.add(s.copy());
        int[] left = new int[pool.size()], take = new int[pool.size()], src = new int[5];
        for (int i = 0; i < pool.size(); i++) left[i] = pool.get(i).getCount();
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            src[i] = -1;
            int own = invSize + target[i]; // prefer what already sits in the target slot (top up / keep)
            if (left[own] > 0 && ings[i].test(pool.get(own))) src[i] = own;
            for (int p = 0; p < pool.size() && src[i] < 0; p++)
                if (left[p] > 0 && !pool.get(p).isEmpty() && ings[i].test(pool.get(p))) src[i] = p;
            if (src[i] < 0) { missing.add(i); continue; }
            left[src[i]]--; take[src[i]]++;
        }
        if (!missing.isEmpty()) return new Result(Outcome.MISSING, missing, null, null, List.of());
        ItemStack[] slots = new ItemStack[5];
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
        for (int i = 0; i < 5; i++) {
            ItemStack st = pool.get(src[i]).copyWithCount(1);
            if (!slots[target[i]].isEmpty()) return Result.fail(Outcome.SLOT_OVERFLOW); // two ingredients for one slot
            if (recorded && !CasingCrossData.lockAllows(recipe, lockMap, target[i], st)) return Result.fail(Outcome.INVALID);
            slots[target[i]] = st;
        }
        List<ItemStack> kinds = new ArrayList<>();
        int[] invCounts = new int[invSize];
        for (int i = 0; i < invSize; i++) { invCounts[i] = left[i]; kinds.add(left[i] > 0 ? pool.get(i) : null); }
        List<ItemStack> drops = new ArrayList<>();
        for (int p = invSize; p < pool.size(); p++) {
            if (left[p] <= 0) continue;
            int over = TransferPlanner.returnInto(kinds, invCounts, pool.get(p), left[p], ItemStack::isSameItemSameComponents, ItemStack::getMaxStackSize);
            if (over > 0) drops.add(pool.get(p).copyWithCount(over));
        }
        List<ItemStack> newInv = new ArrayList<>();
        for (int i = 0; i < invSize; i++) newInv.add(kinds.get(i) == null || invCounts[i] <= 0 ? ItemStack.EMPTY : kinds.get(i).copyWithCount(invCounts[i]));
        return new Result(Outcome.OK, List.of(), newInv, slots, drops);
    }

    /** Server: validate everything again and apply. Silently does nothing on failure. */
    public static void apply(Player player, String recipeId) {
        if (!(player.containerMenu instanceof CasingCrossMenu menu) || menu.entry() == null || !menu.stillValid(player)) return;
        var rl = net.minecraft.resources.ResourceLocation.tryParse(recipeId);
        RecipeHolder<EmpowererRecipe> recipe = CasingCrossData.byId(player.level().getRecipeManager(), rl);
        if (recipe == null) return;
        CasingCrossData.Entry e = menu.entry();
        ItemStack[] cur = new ItemStack[5];
        for (int s = 0; s < 5; s++) cur[s] = e.inv.getStackInSlot(s);
        Result r = simulate(player, cur, e.lockRecorded() ? e.lockId.toString() : null, e.lockMap.clone(), recipe);
        if (r.outcome() != Outcome.OK) return;
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) items.set(i, r.newInventory().get(i));
        for (int s = 0; s < 5; s++) e.inv.setStackInSlot(s, r.newSlots()[s]);
        player.getInventory().setChanged();
        for (ItemStack d : r.drops()) player.drop(d, false);
        menu.broadcastChanges();
    }
}
