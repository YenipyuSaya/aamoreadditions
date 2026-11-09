package com.aamoreadditions.upgrade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Presentation only: callers obtain activation/legality from the shared upgrade handler. */
public final class UpgradeDisplay {
    public static final int GROUPS_PER_ROW = 3;
    private UpgradeDisplay() {}

    public record Group<T>(T representative, int count) {}

    /** Illegal stopped machines must never advertise any plugin as active. */
    public static <T> List<T> visible(List<T> installed, boolean dependency, boolean legal, Predicate<T> active) {
        if (!legal) return List.of();
        return dependency ? installed.stream().filter(active).toList() : List.copyOf(installed);
    }

    /** Group by item identity, not translated names, components, or upgrade line. */
    public static <T, K> List<Group<T>> group(List<T> items, Function<T, K> identity) {
        var groups = new LinkedHashMap<K, Group<T>>();
        for (T item : items) {
            K key = identity.apply(item);
            Group<T> previous = groups.get(key);
            groups.put(key, previous == null ? new Group<>(item, 1)
                    : new Group<>(previous.representative(), previous.count() + 1));
        }
        return List.copyOf(groups.values());
    }

    /** Keep icons compact even with ten different upgrade items from external tags. */
    public static <T> List<List<Group<T>>> rows(List<Group<T>> groups) {
        var rows = new ArrayList<List<Group<T>>>();
        for (int i = 0; i < groups.size(); i += GROUPS_PER_ROW)
            rows.add(List.copyOf(groups.subList(i, Math.min(i + GROUPS_PER_ROW, groups.size()))));
        return List.copyOf(rows);
    }
}
