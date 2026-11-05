package com.aamoreadditions.recipe;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * Alloy Furnace recipe lock state (Minecraft-free, unit-testable).
 * Slots: 0 = input 1, 1 = input 2, 2 = extra.
 * States: unlocked; locked + pending (recipeId == null, no restriction); locked + recorded (slot -> ingredient index, -1 = forbidden).
 * Readable for a future JEI transfer: {@link #recipeId()}, {@link #ingredientFor(int)}.
 */
public final class RecipeLock {
    public static final int SLOTS = 3, FORBIDDEN = -1;

    private boolean locked;
    private String recipeId;
    private final int[] map = {FORBIDDEN, FORBIDDEN, FORBIDDEN};

    public boolean locked() { return locked; }
    public boolean recorded() { return locked && recipeId != null; }
    public String recipeId() { return recordedOrNull(); }
    private String recordedOrNull() { return recorded() ? recipeId : null; }
    /** Ingredient index locked to this slot, or FORBIDDEN (only meaningful when recorded()). */
    public int ingredientFor(int slot) { return map[slot]; }

    public void toggle() { if (locked) unlock(); else { locked = true; recipeId = null; java.util.Arrays.fill(map, FORBIDDEN); } }

    public void unlock() { locked = false; recipeId = null; java.util.Arrays.fill(map, FORBIDDEN); }

    /** Record on the first successful run while pending. take[ingredient][slot] from AlloyMatcher.matchDetailed (before consuming). */
    public boolean recordIfPending(String id, int[][] take) {
        if (!locked || recipeId != null || id == null || take == null) return false;
        recipeId = id;
        for (int s = 0; s < SLOTS; s++) {
            map[s] = FORBIDDEN;
            for (int i = 0; i < take.length; i++) if (s < take[i].length && take[i][s] > 0) { map[s] = i; break; }
        }
        return true;
    }

    /** May an item be inserted into this slot? ingredientMatches tests the item against ingredient #i of the locked recipe. */
    public boolean allows(int slot, IntPredicate ingredientMatches) {
        if (!recorded()) return true;
        int i = map[slot];
        return i != FORBIDDEN && ingredientMatches.test(i);
    }

    /** Unlock if the recorded recipe no longer exists. Returns true if it changed. */
    public boolean validate(Predicate<String> recipeExists) {
        if (recorded() && !recipeExists.test(recipeId)) { unlock(); return true; }
        return false;
    }

    // --- persistence (stored as: bool locked, string id ("" = pending), int[3] map) ---
    public String encodedId() { return recipeId == null ? "" : recipeId; }
    public int[] encodedMap() { return map.clone(); }

    public void decode(boolean locked, String id, int[] m) {
        unlock();
        if (!locked) return;
        this.locked = true;
        this.recipeId = id == null || id.isEmpty() ? null : id;
        if (recipeId != null) {
            if (m == null || m.length != SLOTS) { unlock(); return; }
            for (int s = 0; s < SLOTS; s++) map[s] = m[s] < 0 ? FORBIDDEN : m[s];
        }
    }
}
