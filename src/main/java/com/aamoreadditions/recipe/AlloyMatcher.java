package com.aamoreadditions.recipe;

import java.util.List;
import java.util.function.Predicate;

/**
 * Pure (Minecraft-free) unordered matcher for the Alloy Furnace.
 * Slots (input 1, input 2, extra) are pooled: an ingredient may take its count from several slots
 * (e.g. 3 copper = 2 in one slot + 1 in another) and one slot may feed several ingredients.
 * Every non-empty slot must hold an item that matches at least one ingredient (foreign items block the recipe).
 */
public final class AlloyMatcher {
    private AlloyMatcher() {}

    private static final int[][] ORDERS3 = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};

    /**
     * @return amount to consume from each slot, or null if the recipe does not match.
     */
    public static <T> int[] match(List<? extends Predicate<T>> ingredients, int[] need, List<T> items, int[] counts) {
        int[][] take = matchDetailed(ingredients, need, items, counts);
        if (take == null) return null;
        int[] consume = new int[items.size()];
        for (int[] row : take) for (int s = 0; s < row.length; s++) consume[s] += row[s];
        return consume;
    }

    /** @return take[ingredient][slot] = amount that ingredient takes from that slot, or null. */
    public static <T> int[][] matchDetailed(List<? extends Predicate<T>> ingredients, int[] need, List<T> items, int[] counts) {
        int n = items.size();
        if (ingredients.isEmpty() || n > 3) return null;
        for (int s = 0; s < n; s++) {
            if (counts[s] <= 0) continue;
            boolean any = false;
            for (Predicate<T> p : ingredients) if (p.test(items.get(s))) { any = true; break; }
            if (!any) return null;
        }
        int[][] take = new int[ingredients.size()][n];
        return solve(0, ingredients, need, items, counts.clone(), take) ? take : null;
    }

    private static <T> boolean solve(int i, List<? extends Predicate<T>> ings, int[] need, List<T> items, int[] left, int[][] take) {
        if (i == ings.size()) return true;
        Predicate<T> p = ings.get(i);
        for (int[] order : ORDERS3) {
            int[] l = left.clone(), c = new int[items.size()];
            int still = need[i];
            for (int s : order) {
                if (s >= items.size() || still == 0) continue;
                if (l[s] > 0 && p.test(items.get(s))) {
                    int amt = Math.min(still, l[s]);
                    l[s] -= amt; c[s] += amt; still -= amt;
                }
            }
            if (still == 0) {
                int[] prev = take[i];
                take[i] = c;
                if (solve(i + 1, ings, need, items, l, take)) return true;
                take[i] = prev;
            }
        }
        return false;
    }
}
