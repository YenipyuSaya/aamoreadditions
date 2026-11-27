package com.aamoreadditions.digger;

import com.aamoreadditions.client.digger.DiggerScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure geometry for the stacked item + requirement tooltip boxes. NOT an in-game render test. */
class DiggerTooltipLayoutTest {
    @Test void defaultOriginMatchesVanillaOffset() {
        int[] p = DiggerScreen.stackedTooltipOrigin(50, 40, 400, 300, 80, 20, 60, 10);
        assertEquals(62, p[0]); // 50+12
        assertEquals(28, p[1]); // 40-12
        assertEquals(p[0], p[2]); // left-aligned
        assertEquals(28 + 20 + DiggerScreen.TOOLTIP_PAD + DiggerScreen.REQ_TOOLTIP_GAP + DiggerScreen.TOOLTIP_PAD, p[3]);
    }

    @Test void flipsLeftWhenTheWiderBoxWouldCrossTheRightEdge() {
        // mouse 380, item 40 wide, req 80 wide, screen 400: 380+12+80=472 > 400
        int[] p = DiggerScreen.stackedTooltipOrigin(380, 40, 400, 300, 40, 20, 80, 10);
        int expectedX = Math.max(380 + 12 - 24 - 80, 4);
        assertEquals(expectedX, p[0]);
        assertTrue(p[0] + 80 <= 400);
        assertEquals(p[0], p[2]);
    }

    @Test void shiftsThePairUpSoTheLowerBoxStaysOnScreen() {
        int itemH = 40, reqH = 20;
        // mouseY 280 -> y=268; bottom = 268+40+20+2+9=339 > 300
        int[] p = DiggerScreen.stackedTooltipOrigin(50, 280, 400, 300, 80, itemH, 60, reqH);
        int expectedY = 300 - (itemH + reqH + DiggerScreen.REQ_TOOLTIP_GAP + 9);
        assertEquals(expectedY, p[1]);
        int redBottom = p[3] + reqH + DiggerScreen.TOOLTIP_PAD;
        assertEquals(300, redBottom);
        assertTrue(p[1] >= DiggerScreen.TOOLTIP_PAD);
        assertTrue(p[3] > p[1] + itemH); // requirement box strictly below the item content
    }

    @Test void clampsTheTopPaddingOntoTheScreen() {
        int itemH = 200, reqH = 100;
        int[] p = DiggerScreen.stackedTooltipOrigin(50, 10, 400, 200, 80, itemH, 60, reqH);
        assertEquals(DiggerScreen.TOOLTIP_PAD, p[1]);
    }
}
