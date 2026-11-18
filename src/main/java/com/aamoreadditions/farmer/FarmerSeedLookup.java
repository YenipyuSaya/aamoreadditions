package com.aamoreadditions.farmer;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Pure part of "which item is this crop's seed": every item its loot table can ever drop (a static walk of the
 * table, ignoring chances and conditions), then a deterministic choice. Cached per block type until the server's
 * loot data is reloaded.
 */
public final class FarmerSeedLookup<K> {
    /** A loot table as plain data: the minimum needed to list every possible item. */
    public sealed interface Node permits Item, Items, Group, Ref, Other {}
    /** A single item entry (loot_item). */
    public record Item(String id) implements Node {}
    /** Every item of a tag entry (expanded or not). */
    public record Items(List<String> ids) implements Node {}
    /** Pools, inline tables and alternatives/sequence/group entries: all children count. */
    public record Group(List<Node> children) implements Node {}
    /** Reference to another loot table (followed once; cycles are cut). */
    public record Ref(String table) implements Node {}
    /** Anything that is not an item (empty, dynamic, unknown modded entries). */
    public record Other() implements Node {}

    /** Every item the table can produce. {@code tables} resolves a Ref (null = unknown table). */
    public static Set<String> items(Node root, Function<String, Node> tables) {
        Set<String> out = new HashSet<>();
        collect(root, tables, new HashSet<>(), out);
        return out;
    }

    private static void collect(Node node, Function<String, Node> tables, Set<String> seenTables, Set<String> out) {
        switch (node) {
            case Item i -> out.add(i.id());
            case Items t -> out.addAll(t.ids());
            case Group g -> g.children().forEach(c -> collect(c, tables, seenTables, out));
            case Ref r -> {
                if (seenTables.add(r.table())) { Node t = tables.apply(r.table()); if (t != null) collect(t, tables, seenTables, out); }
            }
            case Other o -> {}
        }
    }

    /** How a dropped item relates to the crop: a seed (c:seeds or minecraft:villager_plantable_seeds), placing it or not. */
    public record Kind(boolean seed, boolean placesCrop) {}

    /** Among the seeds, the one placing this crop first; lowest item id among equals; null when there is no seed. */
    public static String pick(Collection<String> drops, Function<String, Kind> kind) {
        TreeSet<String> placing = new TreeSet<>(), other = new TreeSet<>();
        for (String id : drops) {
            Kind k = kind.apply(id);
            if (k.seed()) (k.placesCrop() ? placing : other).add(id);
        }
        return !placing.isEmpty() ? placing.first() : other.isEmpty() ? null : other.first();
    }

    private final Map<K, Optional<String>> cache = new HashMap<>();
    private Object generation;

    /** Cached value for key; recomputed only for a new key or after the generation (loot data, candidate list) changed. */
    public String get(K key, Object generation, Supplier<String> compute) {
        if (!Objects.equals(this.generation, generation)) { cache.clear(); this.generation = generation; }
        Optional<String> v = cache.get(key);
        if (v == null) { v = Optional.ofNullable(compute.get()); cache.put(key, v); }
        return v.orElse(null);
    }

    public int size() { return cache.size(); }
}
