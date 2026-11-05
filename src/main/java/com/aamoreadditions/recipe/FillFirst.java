package com.aamoreadditions.recipe;

/**
 * Unlocked automation rule for the 3 alloy inputs (input 1, input 2, extra): "top up an existing same-item stack first".
 * A slot that already holds the item always accepts (normal merge); an EMPTY slot refuses the item while another
 * input slot holds the same item and is not full yet. Pure state check, so simulate == execute.
 */
public final class FillFirst {
    private FillFirst() {}
    /**
     * @param slot   index (0..2) asked to accept
     * @param empty  per-slot: is empty
     * @param same   per-slot: holds the same item+components as the offered stack
     * @param count  per-slot count, @param limit per-slot effective limit (min(maxStackSize, slotLimit))
     */
    public static boolean mayAccept(int slot, boolean[] empty, boolean[] same, int[] count, int[] limit) {
        if (!empty[slot]) return true; // merge/mismatch is decided by the handler itself
        for (int j = 0; j < empty.length; j++)
            if (j != slot && same[j] && count[j] < limit[j]) return false;
        return true;
    }
}
