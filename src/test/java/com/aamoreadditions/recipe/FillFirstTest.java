package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FillFirstTest {
    // slots: 0 input1, 1 input2, 2 extra; offered item A
    static boolean acc(int slot, int c0, int c1, int c2, boolean a0, boolean a1, boolean a2) {
        int[] c = {c0, c1, c2};
        boolean[] e = {c0 == 0, c1 == 0, c2 == 0};
        boolean[] s = {a0 && c0 > 0, a1 && c1 > 0, a2 && c2 > 0};
        return FillFirst.mayAccept(slot, e, s, c, new int[]{64, 64, 64});
    }
    @Test void userExample() {
        assertFalse(acc(0, 0, 10, 0, false, true, false)); // input1 empty, input2 has 10 A -> input1 refuses
        assertTrue(acc(1, 0, 10, 0, false, true, false));  // input2 tops up
        assertFalse(acc(2, 0, 10, 0, false, true, false)); // extra refuses too
        assertTrue(acc(0, 0, 64, 0, false, true, false));  // input2 full -> input1 accepts (first in order)
    }
    @Test void extraHoldingSameFillsFirst() {
        assertFalse(acc(0, 0, 0, 5, false, false, true));
        assertTrue(acc(2, 0, 0, 5, false, false, true));
        assertTrue(acc(0, 0, 0, 64, false, false, true));
    }
    @Test void otherItemsDoNotBlock() {
        assertTrue(acc(0, 0, 10, 0, false, false, false)); // input2 holds B
        assertTrue(acc(0, 0, 0, 0, false, false, false));
    }
}
