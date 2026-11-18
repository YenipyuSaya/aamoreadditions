package com.aamoreadditions.farmer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Pure Farmer "memory mode" rules: one record per scanned position (packed BlockPos long), either a seed item id
 * or {@link #EMPTY} (bare farmland, never planted). No Minecraft types; the Farmer mixin feeds it what it sees.
 * In out-of-order mode nothing is recorded, nothing is tilled by memory mode and every seed may be planted.
 */
public final class FarmerMemory {
    public static final String EMPTY = "";

    private boolean memory;
    private final Map<Long, String> records = new HashMap<>();

    public boolean memory() { return memory; }

    /** Leaving memory mode deletes every record. */
    public void setMemory(boolean on) {
        if (memory != on && !on) records.clear();
        memory = on;
    }

    /** Returns the new mode. */
    public boolean toggle() { setMemory(!memory); return memory; }

    /**
     * The single record path: a crop seen (before harvest), a planting or a crop appearing later stores its seed
     * (overwriting); bare farmland (seen, or just tilled) becomes EMPTY unless something is recorded already.
     * Anything else (grass, dirt, fruit blocks, ...) records nothing.
     */
    public void observe(long pos, String seed, boolean bareFarmland) {
        if (!memory) return;
        if (seed != null && !seed.isEmpty()) records.put(pos, seed);
        else if (bareFarmland) records.putIfAbsent(pos, EMPTY);
    }

    /**
     * Hoe the ground (it never plants by itself) at unrecorded and EMPTY positions, and at a seed position whose
     * recorded crop needs tilling (a farmland crop whose farmland turned back into dirt).
     */
    public boolean shouldTill(long pos, Predicate<String> recordedNeedsTilling) {
        if (!memory) return false;
        String rec = records.get(pos);
        return rec == null || rec.isEmpty() || recordedNeedsTilling.test(rec);
    }

    /** Memory mode: only the recorded seed, and only once its crop fits the ground (tilled first). */
    public boolean allowsPlant(long pos, String seed, Predicate<String> cropFits) {
        if (!memory) return true;
        String rec = records.get(pos);
        return rec != null && !rec.isEmpty() && rec.equals(seed) && cropFits.test(rec);
    }

    /** Deletes the records whose positions are outside the current working area (called only when the area changes). */
    public void retain(java.util.function.LongPredicate inArea) {
        records.keySet().removeIf(pos -> !inArea.test(pos));
    }

    /** The scanned layer: |dx|, |dz| <= radius around the center, same y. */
    public static boolean inSquare(int cx, int cy, int cz, int radius, int x, int y, int z) {
        return y == cy && Math.abs(x - cx) <= radius && Math.abs(z - cz) <= radius;
    }

    /** Recorded value: null (none), {@link #EMPTY}, or a seed id. */
    public String record(long pos) { return records.get(pos); }

    public Map<Long, String> snapshot() { return Map.copyOf(records); }

    /** Restores saved state (records are dropped unless memory mode is on). */
    public void load(boolean on, long[] positions, List<String> seeds) {
        memory = on;
        records.clear();
        if (!on || positions == null || seeds == null) return;
        for (int i = 0; i < Math.min(positions.length, seeds.size()); i++)
            if (seeds.get(i) != null) records.put(positions[i], seeds.get(i));
    }
}
