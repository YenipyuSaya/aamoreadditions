package com.aamoreadditions.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpgradeMathTest {
    @Test
    void crusherSpeedOne() {
        // AA crusher: 100 ticks, 40 FE/t. Speed I: +100% speed, +50% energy per item.
        int time = UpgradeMath.time(100, 2.0);
        int tick = UpgradeMath.energyPerTick(40, 1.5, 2.0);
        assertEquals(50, time);
        assertEquals(120, tick);
        assertEquals(6000, time * tick);
    }

    @Test
    void crusherSpeedTwoAndThree() {
        assertEquals(33, UpgradeMath.time(100, 3.0));
        assertEquals(240, UpgradeMath.energyPerTick(40, 2.0, 3.0));
        assertEquals(20, UpgradeMath.time(100, 5.0));
        assertEquals(500, UpgradeMath.energyPerTick(40, 2.5, 5.0));
    }

    @Test
    void capacityAndCombine() {
        assertEquals(90000, UpgradeMath.capacity(60000, 1.5));
        assertEquals(120000, UpgradeMath.capacity(60000, 2.0));
        // Multiplicative (user decision): Speed III (x2.5 energy, x5 speed) + Eff II (x0.5) = 1.25
        double e = EnergyCombine.combine(java.util.List.of(2.5, 0.5));
        assertEquals(1.25, e, 1e-9);
        assertEquals(20, UpgradeMath.time(100, 5.0));
        assertEquals(250, UpgradeMath.energyPerTick(40, e, 5.0));
        // Eff I only: 40 * 0.8 = 32 FE/t; Eff II only: 20 FE/t; both 100 ticks
        assertEquals(32, UpgradeMath.energyPerTick(40, 0.8, 1.0));
        assertEquals(20, UpgradeMath.energyPerTick(40, 0.5, 1.0));
        assertEquals(1, UpgradeMath.energyPerTick(40, 0.0, 1.0)); // never below 1
        // Speed I + Eff I: 40 * 1.2 * 2 = 96 FE/t at 50 ticks
        assertEquals(96, UpgradeMath.energyPerTick(40, EnergyCombine.combine(java.util.List.of(1.5, 0.8)), 2.0));
    }

    @Test
    void generators() {
        // Coal: 20 FE/t, 1600 t (coal item)
        assertEquals(40, UpgradeMath.generatorOutput(20, 2.0));
        assertEquals(720, UpgradeMath.generatorBurnTime(1600, 2.0, 0.9));
        assertEquals(28800, 40 * 720);
        assertEquals(427, UpgradeMath.generatorBurnTime(1600, 3.0, 0.8));
        assertEquals(240, UpgradeMath.generatorBurnTime(1600, 5.0, 0.75));
        // Coal efficiency (multiplicative with speed line)
        assertEquals(3200, UpgradeMath.generatorBurnTime(1600, 1.0, 2.0));
        assertEquals(64000, 20 * 3200);
        assertEquals(100, UpgradeMath.generatorOutput(20, 5.0));
        assertEquals(480, UpgradeMath.generatorBurnTime(1600, 5.0, EnergyCombine.combine(java.util.List.of(0.75, 2.0))));
        assertEquals(120000, UpgradeMath.capacity(60000, 2.0));
        assertEquals(150000, UpgradeMath.capacity(50000, 3.0));
        // Heat collector: 40 FE/t x S
        assertEquals(80, UpgradeMath.generatorOutput(40, 2.0));
        assertEquals(200, UpgradeMath.generatorOutput(40, 5.0));
        assertEquals(90000, UpgradeMath.capacity(30000, 3.0));
        // Leaf generator: cooldown 5 x 0.6/0.4/0.2 -> 3/2/1, FE per leaf 300 x2/x3
        assertEquals(3, UpgradeMath.capacity(5, 0.6));
        assertEquals(2, UpgradeMath.capacity(5, 0.4));
        assertEquals(1, UpgradeMath.capacity(5, 0.2));
        assertEquals(900, UpgradeMath.capacity(300, 3.0));
        // Oil, refined canola: 9600 FE / 120 t = 80 FE/t
        assertEquals(160, UpgradeMath.generatorOutput(80, 2.0));
        assertEquals(54, UpgradeMath.generatorBurnTime(120, 2.0, 0.9));
        assertEquals(400, UpgradeMath.generatorOutput(80, 5.0));
        assertEquals(18, UpgradeMath.generatorBurnTime(120, 5.0, 0.75));
    }

    @Test
    void neutralAndRounding() {
        assertEquals(100, UpgradeMath.time(100, 1.0));
        assertEquals(40, UpgradeMath.energyPerTick(40, 1.0, 1.0));
        assertEquals(1, UpgradeMath.time(1, 8.0));
        assertEquals(13, UpgradeMath.energyPerTick(10, 1.3, 1.0)); // no float drift upward
        assertEquals(14, UpgradeMath.energyPerTick(10, 1.31, 1.0)); // rounds up
    }
}
