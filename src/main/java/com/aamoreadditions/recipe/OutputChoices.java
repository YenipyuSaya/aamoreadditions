package com.aamoreadditions.recipe;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Alloy Furnace "same-tag output" choices (Minecraft-free, unit-testable).
 * One furnace keeps a map output-tag -> chosen item id. Every recipe whose output group is that tag reuses the choice;
 * a new tag with more than one item has no choice yet and blocks the furnace until the player picks one.
 */
public final class OutputChoices {
    public enum Kind {
        /** The output group has no item in this game (recipe hidden / never runs). */
        NONE,
        /** Exactly one candidate: no prompt, works normally. */
        SINGLE,
        /** More than one candidate and a valid saved choice. */
        CHOSEN,
        /** More than one candidate and no (valid) saved choice: furnace waits, button lit. */
        NEEDS_CHOICE
    }

    /** item == null for NONE / NEEDS_CHOICE. */
    public record Resolution(Kind kind, String item) {
        public boolean producible() { return item != null; }
    }

    private final Map<String, String> map = new TreeMap<>();

    public String get(String tag) { return tag == null ? null : map.get(tag); }
    public Map<String, String> view() { return java.util.Collections.unmodifiableMap(map); }
    public boolean isEmpty() { return map.isEmpty(); }

    /**
     * Server-validated choice: the item must be one of the tag's current items.
     * @return true if the stored choice changed (caller resets progress / syncs).
     */
    public boolean choose(String tag, String item, Collection<String> itemsInTag) {
        if (tag == null || tag.isEmpty() || item == null || itemsInTag == null || !itemsInTag.contains(item)) return false;
        return !item.equals(map.put(tag, item));
    }

    /**
     * @param group      output tag id, or null if the result has no same-tag group (fixed item)
     * @param candidates items of that group in this game, sorted (for a fixed item: just that item)
     */
    public Resolution resolve(String group, List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) return new Resolution(Kind.NONE, null);
        if (candidates.size() == 1) return new Resolution(Kind.SINGLE, candidates.get(0));
        String c = get(group);
        if (c != null && candidates.contains(c)) return new Resolution(Kind.CHOSEN, c);
        return new Resolution(Kind.NEEDS_CHOICE, null);
    }

    /** Selection button is lit/interactive when the current output has more than one same-tag item. */
    public static boolean selectable(List<String> candidates) { return candidates != null && candidates.size() > 1; }

    /** Selection button look. */
    public enum Look { DARK, ALERT, CHECK }

    /**
     * Button look from the current output's resolution (null = no recipe): red "!" while a choice is needed (furnace waits),
     * green check once a choice exists (whatever the furnace is doing), dark when no choice applies (single item / no recipe).
     */
    public static Look look(Kind kind) {
        if (kind == Kind.NEEDS_CHOICE) return Look.ALERT;
        if (kind == Kind.CHOSEN) return Look.CHECK;
        return Look.DARK;
    }

    /** Clickable exactly when the look is not dark (a choice is needed or can be changed). */
    public static boolean clickable(Kind kind) { return look(kind) != Look.DARK; }

    /**
     * Can a result of {@code resultCount} be put into the output slot? Different item already there -> wait until removed
     * (never mix the old and the newly chosen item).
     */
    public static boolean fits(boolean outEmpty, boolean sameItem, int outCount, int resultCount, int maxStack) {
        if (outEmpty) return resultCount <= maxStack;
        return sameItem && outCount + resultCount <= maxStack;
    }

    // --- persistence: flat map tag -> item ---
    public Map<String, String> encode() { return new TreeMap<>(map); }

    public void decode(Map<String, String> m) {
        map.clear();
        if (m == null) return;
        m.forEach((k, v) -> { if (k != null && !k.isEmpty() && v != null && !v.isEmpty()) map.put(k, v); });
    }

    public void clear() { map.clear(); }

    /**
     * Same-tag group for an id-form result: the item's c:&lt;kind&gt;/&lt;material&gt; tag.
     * Preferred kinds in order; otherwise the alphabetically first two-segment c: tag; null if none.
     */
    public static final List<String> KIND_ORDER = List.of("ingots", "gems", "dusts", "nuggets", "storage_blocks", "plates", "rods", "gears", "wires");

    public static String materialTag(Collection<String> itemTags) {
        if (itemTags == null) return null;
        String best = null; int bestRank = Integer.MAX_VALUE;
        for (String t : itemTags) {
            if (t == null || !t.startsWith("c:")) continue;
            String path = t.substring(2);
            int slash = path.indexOf('/');
            if (slash <= 0 || slash == path.length() - 1 || path.indexOf('/', slash + 1) >= 0) continue;
            int k = KIND_ORDER.indexOf(path.substring(0, slash));
            int rank = k < 0 ? KIND_ORDER.size() : k;
            if (rank < bestRank || (rank == bestRank && t.compareTo(best) < 0)) { best = t; bestRank = rank; }
        }
        return best;
    }
}
