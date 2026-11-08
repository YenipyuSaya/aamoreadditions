package com.aamoreadditions.upgrade;

import java.util.*;

/** Pure policy shared by menus, interaction, machines and wrench validation. */
public final class UpgradeLogic {
    private UpgradeLogic() {}
    public record Plugin(String line, int tier) {}
    public static boolean valid(List<Plugin> plugins, boolean dependency) {
        if (!dependency) return true;
        Set<Plugin> present = new HashSet<>();
        for (Plugin p : plugins) if (p != null && !present.add(p)) return false;
        for (Plugin p : present)
            for (int tier = 1; tier < p.tier(); tier++) if (!present.contains(new Plugin(p.line(), tier))) return false;
        return true;
    }
    public static <T> List<T> active(List<T> installed, boolean dependency,
                                     java.util.function.Function<T,String> line, java.util.function.ToIntFunction<T> tier) {
        if (!dependency) return List.copyOf(installed);
        Map<String,T> best = new LinkedHashMap<>();
        for (T plugin : installed) best.merge(line.apply(plugin),plugin,(a,b)->tier.applyAsInt(a)>=tier.applyAsInt(b)?a:b);
        return List.copyOf(best.values());
    }
    /** Do not discard truncated slots: return a separate persistent overflow queue. */
    public record Resized<T>(List<T> kept, List<T> overflow) {}
    public static <T> Resized<T> resize(List<T> old, int size, T empty) {
        if (size < 0 || size > 10) throw new IllegalArgumentException("Invalid slot count");
        List<T> kept = new ArrayList<>();
        for (int i = 0; i < size; i++) kept.add(i < old.size() ? old.get(i) : empty);
        return new Resized<>(kept, new ArrayList<>(old.subList(Math.min(size, old.size()), old.size())));
    }
}
