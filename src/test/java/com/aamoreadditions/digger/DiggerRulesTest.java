package com.aamoreadditions.digger;

import com.aamoreadditions.upgrade.UpgradeType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.aamoreadditions.digger.DiggerUpgrade.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pure plugin rules of the Vertical Digger (no registry, no game). */
class DiggerRulesTest {
    private static List<DiggerUpgrade> l(DiggerUpgrade... u) { return Arrays.asList(u); }

    @Test void drillSpeedAndFortuneAreAliasesOfTheAamaLines() {
        assertEquals(UpgradeType.SPEED_1, SPEED_1.alias);
        assertEquals(UpgradeType.SPEED_2, SPEED_2.alias);
        assertEquals(UpgradeType.SPEED_3, SPEED_3.alias);
        assertEquals(UpgradeType.EFFICIENCY_1, FORTUNE_1.alias);
        assertEquals(UpgradeType.EFFICIENCY_2, FORTUNE_2.alias);
        assertNull(SILK_TOUCH.alias);
        assertNull(BLOCK_PLACING.alias);
        assertNull(RANGE_3.alias);
        assertNull(RANGE_5.alias);
        assertTrue(FORTUNE_1.fortune() && FORTUNE_2.fortune() && !SILK_TOUCH.fortune());
        assertTrue(RANGE_3.range() && RANGE_5.range() && !BLOCK_PLACING.range());
    }

    @Test void silkTouchAndFortuneConflictInBothDirections() {
        assertEquals(DiggerRules.Result.CONFLICT, DiggerRules.canAdd(l(SILK_TOUCH), FORTUNE_1));
        assertEquals(DiggerRules.Result.CONFLICT, DiggerRules.canAdd(l(SILK_TOUCH), FORTUNE_2));
        assertEquals(DiggerRules.Result.CONFLICT, DiggerRules.canAdd(l(null, FORTUNE_2), SILK_TOUCH));
        assertEquals(DiggerRules.Result.OK, DiggerRules.canAdd(l(SPEED_1, BLOCK_PLACING), SILK_TOUCH));
        assertEquals(DiggerRules.Result.OK, DiggerRules.canAdd(l(FORTUNE_1), FORTUNE_2));
    }

    @Test void onlyOneBlockPlacingPlugin() {
        assertEquals(DiggerRules.Result.DUPLICATE, DiggerRules.canAdd(l(SPEED_1, BLOCK_PLACING), BLOCK_PLACING));
        assertEquals(DiggerRules.Result.OK, DiggerRules.canAdd(l(SPEED_1, null), BLOCK_PLACING));
        // the ignored slot (null) is the one being replaced
        assertEquals(DiggerRules.Result.OK, DiggerRules.canAdd(l((DiggerUpgrade) null), BLOCK_PLACING));
    }

    @Test void onlyOneSilkTouchPlugin() {
        assertEquals(DiggerRules.Result.DUPLICATE, DiggerRules.canAdd(l(SPEED_1, SILK_TOUCH), SILK_TOUCH));
        assertEquals(DiggerRules.Result.OK, DiggerRules.canAdd(l(SPEED_1, null), SILK_TOUCH));
        assertEquals(DiggerRules.Result.OK, DiggerRules.canAdd(l((DiggerUpgrade) null), SILK_TOUCH));
    }

    @Test void completeListsAreIllegalWithAPairOrTwoPlacers() {
        assertTrue(DiggerRules.valid(l(SPEED_1, SPEED_2, FORTUNE_1, BLOCK_PLACING, null)));
        assertFalse(DiggerRules.valid(l(SILK_TOUCH, FORTUNE_1)));
        assertFalse(DiggerRules.valid(l(BLOCK_PLACING, null, BLOCK_PLACING)));
        assertFalse(DiggerRules.valid(l(SILK_TOUCH, SILK_TOUCH)));
    }

    @Test void onlyOneRangePluginTotal() {
        assertEquals(DiggerRules.Result.OK, DiggerRules.canAdd(l(SPEED_1, BLOCK_PLACING), RANGE_3));
        assertEquals(DiggerRules.Result.OK, DiggerRules.canAdd(l(SILK_TOUCH), RANGE_5));
        assertEquals(DiggerRules.Result.DUPLICATE, DiggerRules.canAdd(l(RANGE_3), RANGE_3));
        assertEquals(DiggerRules.Result.DUPLICATE, DiggerRules.canAdd(l(RANGE_3), RANGE_5));
        assertEquals(DiggerRules.Result.DUPLICATE, DiggerRules.canAdd(l(RANGE_5), RANGE_3));
        assertEquals(DiggerRules.Result.OK, DiggerRules.canAdd(l(null, SPEED_1), RANGE_5)); // replacing the range slot
        assertTrue(DiggerRules.valid(l(RANGE_3, SILK_TOUCH, BLOCK_PLACING)));
        assertFalse(DiggerRules.valid(l(RANGE_3, RANGE_5)));
        assertFalse(DiggerRules.valid(l(RANGE_3, null, RANGE_3)));
    }
}
