package com.aamoreadditions.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Pure, bounded transaction planner. Null means an empty slot, not an absent recording.
 * At most 10 slots => at most 1024 final configurations; no world/inventory is mutated here.
 */
public final class WrenchPlanner {
    private WrenchPlanner() {}

    public static <T> List<T> plan(List<T> recorded, List<T> current, Predicate<T> accepts,
                                   Map<T, Integer> inventory, boolean creative, Predicate<List<T>> valid) {
        int size = current.size();
        if (size < 0 || size > 10 || recorded.size() > 10)
            throw new IllegalArgumentException("Upgrade slots must be between 0 and 10");
        List<T> desired = new ArrayList<>(current);
        if (recorded.size() == size) {
            for (int i = 0; i < size; i++) {
                T item = recorded.get(i);
                if (item == null || accepts.test(item)) desired.set(i, item);
            }
        } else {
            int next = 0;
            for (T item : recorded) {
                if (item == null || !accepts.test(item)) continue;
                if (next == size) break;
                desired.set(next++, item);
            }
        }
        Map<T, Integer> available = new HashMap<>(inventory);
        for (T item : current) if (item != null) available.merge(item, 1, Integer::sum);
        // Earlier slots have priority, but evaluate complete configurations so swaps and
        // later-slot prerequisites work regardless of the temporary installation order.
        for (int mask = (1 << size) - 1; mask >= 0; mask--) {
            List<T> result = new ArrayList<>(current);
            for (int i = 0; i < size; i++) if ((mask & (1 << (size - i - 1))) != 0) result.set(i, desired.get(i));
            if (!valid.test(result)) continue;
            if (!creative) {
                Map<T, Integer> needed = new HashMap<>();
                for (T item : result) if (item != null) needed.merge(item, 1, Integer::sum);
                if (needed.entrySet().stream().anyMatch(e -> e.getValue() > available.getOrDefault(e.getKey(), 0))) continue;
            }
            return result;
        }
        // Old/externally edited invalid configurations are never silently destroyed.
        return new ArrayList<>(current);
    }

    public static <T> boolean changed(List<T> before, List<T> after) {
        return !Objects.equals(before, after);
    }
}
