package com.aamoreadditions.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Pure (Minecraft-free) planner for the JEI "move items" button of the Alloy Furnace.
 * Target slots: 0 = input 1, 1 = input 2, 2 = extra.
 * - unlocked / lock pending: ingredient #i -> slot i;
 * - lock recorded, same recipe: each ingredient -> the slot(s) it is locked to (amount split, remainder to the first);
 * - lock recorded, other recipe: IGNORED (do nothing, silently).
 * Exactly one recipe's worth is planned. For each ingredient the first matching pool item kind that has enough
 * (counting all pool slots of that same kind) is used.
 */
public final class TransferPlanner {
    private TransferPlanner() {}

    public enum Status { OK, IGNORED, MISSING, SLOT_OVERFLOW, BAD_LOCK }

    /**
     * @param slotIngredient ingredient index per target slot (-1 = stays empty)
     * @param slotAmount     amount per target slot
     * @param slotSource     pool index whose item kind fills that slot (-1 = none)
     * @param take           amount to remove from each pool index
     * @param missing        ingredient indices that can't be satisfied
     */
    public record Plan(Status status, int[] slotIngredient, int[] slotAmount, int[] slotSource, int[] take, List<Integer> missing) {
        static Plan of(Status s, int pool) { return new Plan(s, new int[]{-1, -1, -1}, new int[3], new int[]{-1, -1, -1}, new int[pool], List.of()); }
    }

    public static <T> Plan plan(String recipeId, List<? extends Predicate<T>> ingredients, int[] counts, RecipeLock lock,
                                List<T> pool, int[] poolCounts, BiPredicate<T, T> sameKind, ToIntFunction<T> maxStack) {
        int n = pool.size();
        if (lock != null && lock.recorded() && !lock.recipeId().equals(recipeId)) return Plan.of(Status.IGNORED, n);
        boolean useLock = lock != null && lock.recorded();

        // target slots per ingredient
        List<List<Integer>> targets = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) {
            List<Integer> t = new ArrayList<>();
            if (useLock) { for (int s = 0; s < RecipeLock.SLOTS; s++) if (lock.ingredientFor(s) == i) t.add(s); }
            else if (i < RecipeLock.SLOTS) t.add(i);
            if (t.isEmpty()) return Plan.of(Status.BAD_LOCK, n);
            targets.add(t);
        }

        int[] left = poolCounts.clone(), take = new int[n], kindOf = new int[ingredients.size()];
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) {
            kindOf[i] = -1;
            for (int p = 0; p < n && kindOf[i] < 0; p++) {
                if (left[p] <= 0 || pool.get(p) == null || !ingredients.get(i).test(pool.get(p))) continue;
                int have = 0;
                for (int q = 0; q < n; q++) if (left[q] > 0 && pool.get(q) != null && sameKind.test(pool.get(p), pool.get(q))) have += left[q];
                if (have >= counts[i]) {
                    kindOf[i] = p;
                    int still = counts[i];
                    for (int q = 0; q < n && still > 0; q++) {
                        if (left[q] > 0 && pool.get(q) != null && sameKind.test(pool.get(p), pool.get(q))) {
                            int a = Math.min(still, left[q]);
                            left[q] -= a; take[q] += a; still -= a;
                        }
                    }
                }
            }
            if (kindOf[i] < 0) missing.add(i);
        }
        if (!missing.isEmpty()) return new Plan(Status.MISSING, new int[]{-1, -1, -1}, new int[3], new int[]{-1, -1, -1}, new int[n], missing);

        int[] slotIng = {-1, -1, -1}, slotAmt = new int[3], slotSrc = {-1, -1, -1};
        for (int i = 0; i < ingredients.size(); i++) {
            List<Integer> t = targets.get(i);
            int per = counts[i] / t.size(), rem = counts[i] % t.size();
            int max = maxStack.applyAsInt(pool.get(kindOf[i]));
            for (int k = 0; k < t.size(); k++) {
                int s = t.get(k), amt = per + (k < rem ? 1 : 0);
                if (amt > max) return Plan.of(Status.SLOT_OVERFLOW, n);
                slotIng[s] = i; slotAmt[s] = amt; slotSrc[s] = amt > 0 ? kindOf[i] : -1;
            }
        }
        return new Plan(Status.OK, slotIng, slotAmt, slotSrc, take, List.of());
    }

    /**
     * Merge `count` of `item` into an inventory model (kinds + counts, null kind = empty slot), first into matching
     * stacks then into empty slots. Mutates the lists. Returns the amount that did not fit (to be dropped).
     */
    public static <T> int returnInto(List<T> kinds, int[] counts, T item, int count, BiPredicate<T, T> sameKind, ToIntFunction<T> maxStack) {
        int max = maxStack.applyAsInt(item);
        for (int i = 0; i < kinds.size() && count > 0; i++) {
            if (kinds.get(i) != null && counts[i] > 0 && sameKind.test(kinds.get(i), item) && counts[i] < max) {
                int a = Math.min(count, max - counts[i]); counts[i] += a; count -= a;
            }
        }
        for (int i = 0; i < kinds.size() && count > 0; i++) {
            if (kinds.get(i) == null || counts[i] <= 0) {
                int a = Math.min(count, max); kinds.set(i, item); counts[i] = a; count -= a;
            }
        }
        return count;
    }
}
