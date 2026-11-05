package com.aamoreadditions.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlloyFurnaceEnergyTest {
    @Test
    void extraSlotEmptyIs80() { assertEquals(80, UpgradeMath.alloyEnergyPerTick(80, 40, false, 1, 1)); }

    @Test
    void extraSlotFilledIs120() { assertEquals(120, UpgradeMath.alloyEnergyPerTick(80, 40, true, 1, 1)); }

    @Test
    void followsConfig() {
        assertEquals(100, UpgradeMath.alloyEnergyPerTick(100, 10, false, 1, 1));
        assertEquals(110, UpgradeMath.alloyEnergyPerTick(100, 10, true, 1, 1));
        assertEquals(0, UpgradeMath.alloyEnergyPerTick(0, 0, true, 2, 3));
        assertEquals(40, UpgradeMath.alloyEnergyPerTick(0, 40, true, 1, 1));
    }

    @Test
    void upgradeMultipliersApply() {
        // Speed I (speed x2, energy x1.5): 80 -> 240, 120 -> 360
        assertEquals(240, UpgradeMath.alloyEnergyPerTick(80, 40, false, 1.5, 2.0));
        assertEquals(360, UpgradeMath.alloyEnergyPerTick(80, 40, true, 1.5, 2.0));
        // Efficiency II (energy x0.5): 80 -> 40, 120 -> 60
        assertEquals(40, UpgradeMath.alloyEnergyPerTick(80, 40, false, 0.5, 1.0));
        assertEquals(60, UpgradeMath.alloyEnergyPerTick(80, 40, true, 0.5, 1.0));
    }

    @Test
    void chargedOncePerTick() {
        assertEquals(80, UpgradeMath.alloyTickCharge(true, 1000, 80));   // working: exactly one charge
        assertEquals(120, UpgradeMath.alloyTickCharge(true, 1000, 120));
        assertEquals(0, UpgradeMath.alloyTickCharge(false, 1000, 80));   // idle: nothing
    }

    @Test
    void notEnoughEnergyNoProgressNoCharge() {
        assertFalse(UpgradeMath.alloyTickProgresses(true, 79, 80));
        assertEquals(0, UpgradeMath.alloyTickCharge(true, 79, 80));
        assertTrue(UpgradeMath.alloyTickProgresses(true, 80, 80));
    }

    @Test
    void defaultStorage50000AndInput1000() {
        assertEquals(50000, UpgradeMath.ALLOY_DEFAULT_CAPACITY);
        assertEquals(1000, UpgradeMath.ALLOY_DEFAULT_MAX_RECEIVE);
        assertEquals(80, UpgradeMath.ALLOY_DEFAULT_FE);
        assertEquals(40, UpgradeMath.ALLOY_DEFAULT_EXTRA_FE);
    }

    @Test
    void capacityAndInputFollowConfigAndUpgrades() {
        // same math the handler uses: base (config) * multiplier
        assertEquals(50000, UpgradeMath.capacity(50000, 1.0));
        assertEquals(100000, UpgradeMath.capacity(50000, 2.0));   // efficiency II capacity x2
        assertEquals(80000, UpgradeMath.capacity(80000, 1.0));    // config changed to 80000
        assertEquals(10000, UpgradeMath.capacity(500, 20.0));     // efficiency II input x20
    }

    @Test
    void loadClampsToCapacity() {
        assertEquals(45000, UpgradeMath.clampEnergy(45000, 50000)); // old save within new cap: kept
        assertEquals(20000, UpgradeMath.clampEnergy(45000, 20000)); // cap lowered in config: clamped
        assertEquals(0, UpgradeMath.clampEnergy(-5, 50000));
    }

    @Test
    void energyBarNoOverflowAndClamped() {
        assertEquals(83, UpgradeMath.energyBarHeight(50000, 50000, 83));
        assertEquals(41, UpgradeMath.energyBarHeight(25000, 50000, 83));
        assertEquals(83, UpgradeMath.energyBarHeight(Integer.MAX_VALUE, Integer.MAX_VALUE, 83)); // int math would overflow
        assertEquals(83, UpgradeMath.energyBarHeight(60000, 50000, 83)); // energy above max (before clamp) stays in the bar
        assertEquals(0, UpgradeMath.energyBarHeight(1, 50000, 83)); // same as AA: tiny amounts show no pixel
        assertEquals(0, UpgradeMath.energyBarHeight(0, 50000, 83));
    }
}
