package com.aamoreadditions.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Pure (Minecraft-free) grouping of efficient-smelting recipes for JEI: ONE entry per recipe shape instead of one per metal.
 * Shape = (input kind, number of input slots, output count, base time). E.g. all "raw ore in 2 slots -> 3 ingots, 80 ticks"
 * recipes form one entry. KubeJS recipes with another count/time/kind get their own entry (same shape = same entry).
 * Inside an entry every slot cycles in sync, one metal (and one item variant) per step.
 */
public final class EfficientGrouping {
    private EfficientGrouping() {}

    public record Shape(EfficientKind kind, int slots, int count, int ticks) {}

    /** Display order: raw ore, ore, raw block, dust, other; then slots, count, time. */
    public static final Comparator<Shape> ORDER = Comparator
            .comparingInt((Shape s) -> List.of(EfficientKind.RAW_ORE, EfficientKind.ORE, EfficientKind.RAW_BLOCK, EfficientKind.DUST, EfficientKind.OTHER).indexOf(s.kind()))
            .thenComparingInt(Shape::slots).thenComparingInt(Shape::count).thenComparingInt(Shape::ticks);

    /** Group recipes by shape (members keep the given order), groups sorted by {@link #ORDER}. */
    public static <R> List<Map.Entry<Shape, List<R>>> group(List<R> recipes, Function<R, Shape> shape) {
        Map<Shape, List<R>> m = new LinkedHashMap<>();
        for (R r : recipes) m.computeIfAbsent(shape.apply(r), k -> new ArrayList<>()).add(r);
        List<Map.Entry<Shape, List<R>>> l = new ArrayList<>(m.entrySet());
        l.sort(Map.Entry.comparingByKey(ORDER));
        return l;
    }

    /**
     * Aligned display rows. member = per-slot item lists (inputs..., output); each member contributes
     * max(list sizes) rows, slot j of row k = list_j[k % size_j]. Members with an empty slot list are skipped.
     * Returns rows (each = one item per slot) so that all slots cycle together.
     */
    public static <T> List<List<T>> align(List<List<List<T>>> members) {
        List<List<T>> rows = new ArrayList<>();
        for (List<List<T>> slots : members) {
            int len = 0;
            boolean ok = !slots.isEmpty();
            for (List<T> l : slots) { if (l.isEmpty()) ok = false; len = Math.max(len, l.size()); }
            if (!ok) continue;
            for (int k = 0; k < len; k++) {
                List<T> row = new ArrayList<>();
                for (List<T> l : slots) row.add(l.get(k % l.size()));
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Focus: keep rows whose slot (inputs for an input focus, the last slot = output for an output focus) holds the focused item.
     * No focus or nothing left -> all rows.
     */
    public static <T> List<List<T>> focus(List<List<T>> rows, T focused, boolean output, BiPredicate<T, T> same) {
        if (focused == null) return rows;
        List<List<T>> keep = new ArrayList<>();
        for (List<T> r : rows) {
            boolean hit = false;
            if (output) hit = same.test(r.get(r.size() - 1), focused);
            else for (int j = 0; j < r.size() - 1 && !hit; j++) hit = same.test(r.get(j), focused);
            if (hit) keep.add(r);
        }
        return keep.isEmpty() ? rows : keep;
    }

    /** Column j of the rows (the list one JEI slot cycles through). */
    public static <T> List<T> column(List<List<T>> rows, int j) {
        List<T> c = new ArrayList<>(rows.size());
        for (List<T> r : rows) c.add(r.get(j));
        return c;
    }
}
