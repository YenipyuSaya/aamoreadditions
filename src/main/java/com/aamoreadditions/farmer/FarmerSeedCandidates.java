package com.aamoreadditions.farmer;

import java.util.*;
import java.util.regex.Pattern;

/** Parsed "farmer_memory_seed_candidates" config list: item ids and #tag ids; malformed entries are reported. */
public record FarmerSeedCandidates(Set<String> items, Set<String> tags, List<String> invalid) {
    public static final List<String> DEFAULTS = List.of("#c:seeds", "#minecraft:villager_plantable_seeds", "#aamoreadditions:farmer_memory_seeds");
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public static FarmerSeedCandidates parse(List<String> entries) {
        Set<String> items = new TreeSet<>(), tags = new TreeSet<>();
        List<String> invalid = new ArrayList<>();
        for (String raw : entries) {
            String e = raw == null ? "" : raw.trim();
            boolean tag = e.startsWith("#");
            String id = tag ? e.substring(1) : e;
            if (!ID.matcher(id).matches()) invalid.add(String.valueOf(raw));
            else (tag ? tags : items).add(id);
        }
        return new FarmerSeedCandidates(Set.copyOf(items), Set.copyOf(tags), List.copyOf(invalid));
    }

    /** True when the item (by id, and the tag ids it is in) matches an entry. */
    public boolean matches(String itemId, java.util.function.Predicate<String> inTag) {
        return items.contains(itemId) || tags.stream().anyMatch(inTag);
    }
}
