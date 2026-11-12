package com.aamoreadditions.recipe;

import java.util.List;
import java.util.function.Predicate;

/**
 * Pure (Minecraft-free) matcher for Efficient-Smelting mode.
 * - Slots: input 1, input 2, input 3 (= extra). Each recipe ingredient takes exactly ONE item from its OWN slot
 *   (stack size does not matter; "2 raw" = two slots holding raw ore).
 * - Items that belong to no matched recipe are ignored (mixed slots: only the matching group is processed).
 * - Choice among matching recipes: higher kind priority (dust > raw block > ore > raw ore), then MORE slots
 *   (3 raw -> the 3-slot recipe only, never a fallback to 2), then the earliest slot, then recipe id.
 */
public final class EfficientMatcher {
    private EfficientMatcher() {}

    private static final int[][] ORDERS3 = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};

    /** Slot for each ingredient (distinct, non-empty, lowest slots first), or null. occupied[s] = slot s holds an item. */
    public static <T> int[] assign(List<? extends Predicate<T>> ings, List<T> items, boolean[] occupied) {
        int n = ings.size();
        if (n < 1 || n > 3 || items.size() != 3) return null;
        // try slot combinations in lexicographic order of the sorted slot set, then ingredient permutations
        int[] best = null;
        for (int[] order : ORDERS3) {
            int[] a = new int[n];
            boolean ok = true;
            for (int i = 0; i < n && ok; i++) {
                int s = order[i];
                ok = occupied[s] && ings.get(i).test(items.get(s));
                a[i] = s;
            }
            if (ok && (best == null || key(a) < key(best))) best = a;
        }
        return best;
    }

    /** Sort key of a slot assignment: sorted slot set, lexicographic (earlier slots first). */
    static int key(int[] a) {
        int[] s = a.clone();
        java.util.Arrays.sort(s);
        int k = 0;
        for (int v : s) k = k * 4 + v;
        for (int i = s.length; i < 3; i++) k = k * 4 + 3;
        return k;
    }

    public record Candidate<R>(R recipe, String id, int priority, int[] slots) {
        public int[][] take() {
            int[][] t = new int[slots.length][3];
            for (int i = 0; i < slots.length; i++) t[i][slots[i]] = 1;
            return t;
        }
        public int[] plan() {
            int[] p = new int[3];
            for (int s : slots) p[s] = 1;
            return p;
        }
    }


    /** Efficient-Smelting mode only: restart progress when the running recipe differs from the last one (unknown last = adopt). */
    public static boolean restartOnChange(boolean efficientMode, String lastId, String currentId) {
        return efficientMode && lastId != null && currentId != null && !lastId.equals(currentId);
    }

    /** Best candidate per the class rules, or null. */
    public static <R> Candidate<R> pick(List<Candidate<R>> cands) {
        Candidate<R> best = null;
        for (Candidate<R> c : cands) if (best == null || better(c, best)) best = c;
        return best;
    }

    static <R> boolean better(Candidate<R> a, Candidate<R> b) {
        if (a.priority != b.priority) return a.priority > b.priority;
        if (a.slots.length != b.slots.length) return a.slots.length > b.slots.length;
        int ka = key(a.slots), kb = key(b.slots);
        if (ka != kb) return ka < kb;
        return a.id.compareTo(b.id) < 0;
    }
}
