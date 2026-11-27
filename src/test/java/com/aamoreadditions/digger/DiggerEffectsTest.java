package com.aamoreadditions.digger;

import com.aamoreadditions.effect.FarmerUpgradeMath;
import com.aamoreadditions.upgrade.UpgradeType;
import com.aamoreadditions.upgrade.UpgradeLogic.Plugin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Digger effects reuse the Farmer policy with AA's 5-tick period. */
class DiggerEffectsTest {
    private static FarmerUpgradeMath.Effects eval(boolean dependency, Plugin... p) {
        return FarmerUpgradeMath.evaluate(List.of(p), dependency);
    }

    @Test void periodFromAaFiveTicks() {
        assertEquals(5, DiggerEffects.period(1.0));
        assertEquals(3, DiggerEffects.period(eval(true, new Plugin("SPEED", 1)).speed()));   // x2: 5/2=2.5 -> 3
        assertEquals(2, DiggerEffects.period(eval(true, new Plugin("SPEED", 1), new Plugin("SPEED", 2)).speed())); // dep. highest = x3: 5/3 -> 2
        assertEquals(2, DiggerEffects.period(eval(true, new Plugin("SPEED", 2)).speed())); // Speed II alone is not the cap
        assertEquals(1, DiggerEffects.period(eval(true, new Plugin("SPEED", 3)).speed())); // dep. Speed III: 5/5=1
        assertEquals(1, DiggerEffects.period(eval(false, new Plugin("SPEED", 3), new Plugin("SPEED", 3)).speed())); // capped at 1 tick
        assertEquals(1, DiggerEffects.period(eval(false, new Plugin("SPEED", 1), new Plugin("SPEED", 2)).speed())); // independent I+II = x5
    }

    @Test void fortuneLevelsAreTheFarmerEfficiencyLevels() {
        assertEquals(1, eval(true, new Plugin("EFFICIENCY", 1)).fortune());
        assertEquals(3, eval(true, new Plugin("EFFICIENCY", 1), new Plugin("EFFICIENCY", 2)).fortune());
        assertEquals(0, eval(false, new Plugin("EFFICIENCY", 1)).fortune()); // 0.7 < 1
        assertEquals(2, eval(false, new Plugin("EFFICIENCY", 2), new Plugin("EFFICIENCY", 2)).fortune()); // 2.4
    }

    @Test void costsAreConfigBasesTimesTheEnergyMultiplier() {
        assertEquals(650, DiggerEffects.miningBase(false, 650, 3));
        assertEquals(1950, DiggerEffects.miningBase(true, 650, 3));
        assertEquals(Integer.MAX_VALUE, DiggerEffects.miningBase(true, Integer.MAX_VALUE, 3)); // saturates, no overflow
        assertEquals(500, DiggerEffects.cost(500, 1.0));
        assertEquals(975, DiggerEffects.cost(650, 1.5));
        assertEquals(0, DiggerEffects.cost(0, 2.0)); // a 0 FE config stays free
        assertEquals(DiggerEffects.DEFAULT_PLACING_FE, 500);
        assertEquals(DiggerEffects.DEFAULT_MINING_FE, 650);
        assertEquals(DiggerEffects.DEFAULT_ORES_ONLY_MULTIPLIER, 3);
    }

    @Test void placingIsGatedToOresOnlyModeWithThePlacerInstalled() {
        assertFalse(DiggerEffects.placingEnabled(false, false));
        assertFalse(DiggerEffects.placingEnabled(false, true));  // no augment, even in ores-only
        assertFalse(DiggerEffects.placingEnabled(true, false)); // Mining Everything: no place, no FE
        assertTrue(DiggerEffects.placingEnabled(true, true));
        // locked = missing augment OR wrong mode (inverse of placingEnabled)
        assertTrue(DiggerEffects.fillersLocked(false, false));
        assertTrue(DiggerEffects.fillersLocked(false, true));
        assertTrue(DiggerEffects.fillersLocked(true, false));
        assertFalse(DiggerEffects.fillersLocked(true, true));
        assertEquals(!DiggerEffects.placingEnabled(true, false), DiggerEffects.fillersLocked(true, false));
    }

    @Test void verticalSpanIs3Or5WithASingleRangePlugin() {
        assertEquals(1, DiggerEffects.verticalSpan(true, false, false));
        assertEquals(3, DiggerEffects.verticalSpan(true, true, false));
        assertEquals(5, DiggerEffects.verticalSpan(true, false, true));
        assertEquals(1, DiggerEffects.verticalSpan(false, true, false)); // illegal: effect off
        assertEquals(1, DiggerEffects.verticalSpan(false, false, true));
    }

    @Test void airRequiresBufferedMiningCostOnlyWithoutARangeUpgrade() {
        // AA: stored >= actualUse (effective mining cost, incl. ores-only and energy multipliers).
        assertTrue(DiggerEffects.hasBufferedMiningCost(650, 650, 1));
        assertTrue(DiggerEffects.hasBufferedMiningCost(1950, 1950, 1));
        assertFalse(DiggerEffects.hasBufferedMiningCost(649, 650, 1));
        assertFalse(DiggerEffects.hasBufferedMiningCost(650, 1950, 1)); // ores-only cost, not the unmultiplied base
        assertFalse(DiggerEffects.hasBufferedMiningCost(0, 650, 1));
        assertTrue(DiggerEffects.hasBufferedMiningCost(0, 0, 1)); // a 0 FE config stays free
        // 3x3 / 5x5: air and skips need no stored energy.
        assertTrue(DiggerEffects.hasBufferedMiningCost(0, 650, 3));
        assertTrue(DiggerEffects.hasBufferedMiningCost(0, 1950, 5));
        assertTrue(DiggerEffects.hasBufferedMiningCost(0, 650, 5));
    }

    @Test void fortuneEnergyAndCapacityMatchFarmerExactly() {
        @SuppressWarnings("unchecked")
        List<UpgradeType>[] sets = new List[]{
                List.of(UpgradeType.EFFICIENCY_1),
                List.of(UpgradeType.EFFICIENCY_2),
                List.of(UpgradeType.EFFICIENCY_1, UpgradeType.EFFICIENCY_2),
                java.util.Collections.nCopies(5, UpgradeType.EFFICIENCY_1),
                List.of(UpgradeType.SPEED_1, UpgradeType.SPEED_2, UpgradeType.SPEED_3,
                        UpgradeType.EFFICIENCY_1, UpgradeType.EFFICIENCY_2),
                java.util.stream.Stream.concat(
                        java.util.Collections.nCopies(4, UpgradeType.SPEED_1).stream(),
                        java.util.Collections.nCopies(4, UpgradeType.EFFICIENCY_1).stream()).toList()
        };
        for (boolean dep : new boolean[]{true, false}) {
            for (List<UpgradeType> set : sets) {
                var plugins = set.stream().map(u -> new Plugin(u.line.name(), u.tier)).toList();
                assertEquals(FarmerUpgradeMath.evaluate(plugins, dep), DiggerEffects.evaluate(set, dep),
                        "dep=" + dep + " " + set);
            }
        }
        // Explicit Farmer numbers: capacity is an unpenalized sum (I = x1.5, II = x2); energy uses the
        // processor mixed policy, including the 0.15 efficiency-line boundary in independent mode.
        var i1 = DiggerEffects.evaluate(List.of(UpgradeType.EFFICIENCY_1), false);
        assertEquals(1.5, i1.capacity());
        assertEquals(0.8, i1.energy());
        var i2 = DiggerEffects.evaluate(List.of(UpgradeType.EFFICIENCY_2), false);
        assertEquals(2.0, i2.capacity());
        assertEquals(0.5, i2.energy());
        var five = DiggerEffects.evaluate(java.util.Collections.nCopies(5, UpgradeType.EFFICIENCY_1), false);
        assertEquals(7.5, five.capacity());
        assertEquals(0.425984, five.energy(), 1e-12);
        assertEquals(FarmerUpgradeMath.operationCost(650, five.energy()), DiggerEffects.cost(650, five.energy()));
        var dep2 = DiggerEffects.evaluate(List.of(UpgradeType.EFFICIENCY_1, UpgradeType.EFFICIENCY_2), true);
        assertEquals(2.0, dep2.capacity()); // highest tier only
        assertEquals(0.5, dep2.energy());
    }

    @Test void scanPeriodCapsAtOneTickForDiggerAndFarmer() {
        // Digger original period 5: gold (Max) only when period is actually 1, i.e. speed >= 5
        // (Speed III, independent I+II, 3x I, 2x II, …). Speed I (x2) and II (x3) do not reach it.
        assertEquals(3, DiggerEffects.period(2.0)); // 5/2=2.5 -> 3
        assertEquals(2, DiggerEffects.period(3.0)); // 5/3 ≈ 1.667 -> 2
        assertEquals(2, DiggerEffects.period(4.0)); // 5/4=1.25 -> 2
        assertEquals(1, DiggerEffects.period(5.0));
        assertEquals(1, DiggerEffects.period(10.0));
        assertFalse(FarmerUpgradeMath.atScanCap(DiggerEffects.period(2.0)));
        assertFalse(FarmerUpgradeMath.atScanCap(DiggerEffects.period(3.0)));
        assertTrue(FarmerUpgradeMath.atScanCap(DiggerEffects.period(5.0)));
        // Farmer original period 6: cap only at speed >= 6. Speed III (x5) is 6/5=1.2 -> 2, not the cap.
        assertEquals(3, FarmerUpgradeMath.scanTicks(6, 2.0));
        assertEquals(2, FarmerUpgradeMath.scanTicks(6, 3.0));
        assertEquals(2, FarmerUpgradeMath.scanTicks(6, 4.0));
        assertEquals(2, FarmerUpgradeMath.scanTicks(6, 5.0));
        assertEquals(1, FarmerUpgradeMath.scanTicks(6, 6.0));
        assertFalse(FarmerUpgradeMath.atScanCap(FarmerUpgradeMath.scanTicks(6, 5.0)));
        assertTrue(FarmerUpgradeMath.atScanCap(FarmerUpgradeMath.scanTicks(6, 6.0)));
        assertEquals(1, FarmerUpgradeMath.MIN_SCAN_TICKS);
        assertFalse(FarmerUpgradeMath.atScanCap(20)); // sleeping tick rate is not the cap
    }
}
