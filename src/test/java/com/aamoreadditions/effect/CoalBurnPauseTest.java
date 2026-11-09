package com.aamoreadditions.effect;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Models AA's separate decrement/generation/start blocks using the shared production gate. */
class CoalBurnPauseTest {
    static final class Coal {
        int stored, capacity = 60000, burn = 20, maxBurn = 20, rate = 30, fuel = 10;
        boolean redstone;
        void tick() {
            if (burn > 0) {
                if (OilBurnPause.mayAdvance(stored, capacity)) burn--;
                if (OilBurnPause.mayAdvance(stored, capacity)) stored += Math.min(capacity - stored, rate);
            }
            if (!redstone && burn <= 0 && stored < capacity && fuel > 0) {
                burn = maxBurn;
                fuel--;
            }
        }
    }
    @Test void fullBeforeStartKeepsFuelAndDoesNotStart() {
        var c = new Coal(); c.burn = 0; c.stored = c.capacity; c.tick();
        assertEquals(0, c.burn); assertEquals(10, c.fuel);
    }
    @Test void fullDuringBurnRetainsTimeAndProgressDenominator() {
        var c = new Coal(); c.stored = c.capacity;
        for (int i = 0; i < 1000; i++) c.tick();
        assertEquals(20, c.burn); assertEquals(20, c.maxBurn);
        assertEquals(10, c.fuel); assertEquals(c.capacity, c.stored);
    }
    @Test void oneFeGapCostsFullTickAndClipsOverflow() {
        var c = new Coal(); c.stored = c.capacity - 1; c.tick();
        assertEquals(19, c.burn); assertEquals(c.capacity, c.stored);
        c.tick(); assertEquals(19, c.burn);
    }
    @Test void exactlyOneTickRoomIsFilledNormally() {
        var c = new Coal(); c.stored = c.capacity - c.rate; c.tick();
        assertEquals(19, c.burn); assertEquals(c.capacity, c.stored);
    }
    @Test void extractionResumesSameFuel() {
        var c = new Coal(); c.stored = c.capacity; c.tick(); c.stored -= 5; c.tick();
        assertEquals(19, c.burn); assertEquals(10, c.fuel); assertEquals(c.capacity, c.stored);
    }
    @Test void bothModesRetainSharedSpeedAndUtilizationMath() {
        for (boolean dependency : new boolean[]{true, false}) {
            var c = new Coal();
            double speed = dependency ? 5 : UpgradeAggregation.speed(List.of(2., 3., 5., 2.));
            double utilization = dependency ? .75 * 2 : UpgradeAggregation.generatorUtilization(
                    List.of(.9, .8, .75, .9), List.of(1.5, 2.));
            c.rate = UpgradeMath.generatorOutput(30, speed);
            c.burn = c.maxBurn = UpgradeMath.generatorBurnTime(1600, speed, utilization);
            int before = c.burn; c.stored = c.capacity; c.tick(); assertEquals(before, c.burn);
            c.stored--; c.tick(); assertEquals(before - 1, c.burn); assertEquals(c.capacity, c.stored);
            assertEquals(10, c.fuel);
        }
    }
    @Test void finalTickFillingStorageCannotConsumeNextFuel() {
        var c = new Coal(); c.burn = 1; c.stored = c.capacity - 1; c.tick();
        assertEquals(0, c.burn); assertEquals(10, c.fuel);
        c.stored--; c.tick(); assertEquals(c.maxBurn, c.burn); assertEquals(9, c.fuel);
    }
    @Test void repeatedPauseCyclesDoNotSpendExtraBurnTicks() {
        var c = new Coal(); c.stored = c.capacity;
        for (int n = 0; n < 5; n++) {
            for (int idle = 0; idle < 50; idle++) c.tick();
            c.stored--; c.tick();
        }
        assertEquals(15, c.burn); assertEquals(10, c.fuel);
    }
    @Test void nativeRedstoneOnlyBlocksStartingNewFuel() {
        var c = new Coal(); c.redstone = true; c.stored = c.capacity; c.tick();
        assertEquals(20, c.burn);
        c.stored--; c.tick(); assertEquals(19, c.burn);
        c.burn = 0; c.stored = 0; c.tick(); assertEquals(0, c.burn); assertEquals(10, c.fuel);
    }
}
