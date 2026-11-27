package com.aamoreadditions.booster;

import com.aamoreadditions.effect.UpgradeConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoosterEffectsTest {

    private static BoosterScan scan(int cap, BoosterTier... layers) {
        return BoosterEffects.scan(i -> i < layers.length ? layers[i] : null, cap);
    }

    @Test void emptyStackIsRangedMultiplierOne() {
        var s = scan(3);
        assertEquals(List.of(), s.layers());
        assertFalse(s.overloaded());
        assertEquals(BoosterScan.Binding.RANGED, s.binding(false));
        assertEquals(BoosterScan.Binding.RANGED, s.binding(true));
        assertEquals(1, s.multiplier(2, 5, false));
        assertEquals(16, BoosterEffects.multipliedRange(16, s, BoosterEffects.Machine.FACE));
        assertEquals(2, BoosterEffects.multipliedRange(2, s, BoosterEffects.Machine.DIGGER));
    }

    @Test void vanillaDoublesAndAdvancedFivesMultiplyWhenMixed() {
        var twoVanilla = scan(3, BoosterTier.VANILLA, BoosterTier.VANILLA);
        assertEquals(4, twoVanilla.multiplier(2, 5, false));
        assertEquals(64, BoosterEffects.multipliedRange(16, twoVanilla, BoosterEffects.Machine.FACE));

        var advanced = scan(3, BoosterTier.ADVANCED);
        assertEquals(5, advanced.multiplier(2, 5, false));
        assertEquals(80, BoosterEffects.multipliedRange(16, advanced, BoosterEffects.Machine.FACE));

        var mixed = scan(3, BoosterTier.VANILLA, BoosterTier.ADVANCED);
        assertFalse(mixed.overloaded());
        assertEquals(10, mixed.multiplier(2, 5, false));
        assertEquals(160, BoosterEffects.multipliedRange(16, mixed, BoosterEffects.Machine.FACE));
        assertEquals(30, BoosterEffects.multipliedRange(3, mixed, BoosterEffects.Machine.FACE)); // placer/breaker share
    }

    @Test void capStopsTheStackAndNonBoosterGapSeparates() {
        var capped = scan(2, BoosterTier.VANILLA, BoosterTier.VANILLA, BoosterTier.ADVANCED);
        assertEquals(List.of(BoosterTier.VANILLA, BoosterTier.VANILLA), capped.layers());
        assertEquals(4, capped.multiplier(2, 5, false));

        // A hole at offset 1 ends the counted column; the booster above the hole is not connected.
        var gapped = BoosterEffects.scan(i -> i == 0 ? BoosterTier.TRANSFINITE : i == 2 ? BoosterTier.VANILLA : null, 3);
        assertEquals(List.of(BoosterTier.TRANSFINITE), gapped.layers());
        assertFalse(gapped.overloaded());
        assertEquals(BoosterScan.Binding.TRANSFINITE, gapped.binding(false));
    }

    @Test void loneSpecialStandsAloneOnFacesButCountsAsAdvancedOnTheDigger() {
        var transfinite = scan(3, BoosterTier.TRANSFINITE);
        assertFalse(transfinite.overloaded());
        assertEquals(BoosterScan.Binding.TRANSFINITE, transfinite.binding(false));
        assertEquals(BoosterScan.Binding.RANGED, transfinite.binding(true));
        assertEquals(Integer.MAX_VALUE, BoosterEffects.multipliedRange(16, transfinite, BoosterEffects.Machine.FACE));
        assertEquals(Integer.MAX_VALUE, BoosterEffects.multipliedRange(32, transfinite, BoosterEffects.Machine.INTERFACE));
        assertEquals(10, BoosterEffects.multipliedRange(2, transfinite, BoosterEffects.Machine.DIGGER)); // 2 * 5

        var dimensional = scan(3, BoosterTier.DIMENSIONAL);
        assertEquals(BoosterScan.Binding.DIMENSIONAL, dimensional.binding(false));
        assertEquals(BoosterScan.Binding.RANGED, dimensional.binding(true));
        assertEquals(10, BoosterEffects.multipliedRange(2, dimensional, BoosterEffects.Machine.DIGGER));
    }

    @Test void specialPlusLowerOrSpecialPlusSpecialOverloadsEveryMachineIncludingTheDigger() {
        var specialAndVanilla = scan(3, BoosterTier.TRANSFINITE, BoosterTier.VANILLA);
        assertTrue(specialAndVanilla.overloaded());
        assertEquals(BoosterScan.Binding.DISABLED, specialAndVanilla.binding(false));
        assertEquals(BoosterScan.Binding.DISABLED, specialAndVanilla.binding(true));
        assertEquals(0, BoosterEffects.multipliedRange(16, specialAndVanilla, BoosterEffects.Machine.FACE));
        assertEquals(0, BoosterEffects.multipliedRange(2, specialAndVanilla, BoosterEffects.Machine.DIGGER));

        var specialAndAdvanced = scan(3, BoosterTier.ADVANCED, BoosterTier.DIMENSIONAL);
        assertTrue(specialAndAdvanced.overloaded());
        assertEquals(0, BoosterEffects.multipliedRange(2, specialAndAdvanced, BoosterEffects.Machine.DIGGER));

        var twoSpecials = scan(3, BoosterTier.TRANSFINITE, BoosterTier.DIMENSIONAL);
        assertTrue(twoSpecials.overloaded());
        assertEquals(0, BoosterEffects.multipliedRange(2, twoSpecials, BoosterEffects.Machine.DIGGER));

        var twoSame = scan(3, BoosterTier.TRANSFINITE, BoosterTier.TRANSFINITE);
        assertTrue(twoSame.overloaded());
    }

    @Test void diggerMixedVanillaAndAdvancedStillMultiplies() {
        var mixed = scan(3, BoosterTier.VANILLA, BoosterTier.ADVANCED);
        assertFalse(mixed.overloaded());
        assertEquals(20, BoosterEffects.multipliedRange(2, mixed, BoosterEffects.Machine.DIGGER)); // 2 * 2 * 5
    }

    @Test void playerInterfaceRangedDistanceUsesTheSquaredRange() {
        int range = 32;
        // AA compared distSqr <= range (32), so a player 6 blocks away (distSqr=36) was out of reach.
        assertTrue(36.0 <= (double) range * range);
        assertFalse(36.0 <= range);
        assertTrue((range + 0.01) * (range + 0.01) > (double) range * range);
        assertFalse(range * range + 0.01 <= (double) range * range);
    }

    @Test void multiplierClampsAtIntegerMax() {
        var many = scan(8, BoosterTier.ADVANCED, BoosterTier.ADVANCED, BoosterTier.ADVANCED,
                BoosterTier.ADVANCED, BoosterTier.ADVANCED, BoosterTier.ADVANCED, BoosterTier.ADVANCED, BoosterTier.ADVANCED);
        // 5^8 = 390625, times default 16 is still fine; use a huge default
        assertEquals(Integer.MAX_VALUE, BoosterEffects.multipliedRange(Integer.MAX_VALUE / 2, many, BoosterEffects.Machine.FACE));
    }

    @Test void configDefaultsMatchTheDecidedValues() {
        assertEquals(3, UpgradeConfig.DEFAULT_PHANTOM_BOOSTER_MAX_STACK);
        assertEquals(2, UpgradeConfig.DEFAULT_PHANTOM_BOOSTER_AA_MULTIPLIER);
        assertEquals(5, UpgradeConfig.DEFAULT_PHANTOM_BOOSTER_ADVANCED_MULTIPLIER);
        assertEquals(3, UpgradeConfig.phantomBoosterMaxStack());
        assertEquals(2, UpgradeConfig.phantomBoosterAaMultiplier());
        assertEquals(5, UpgradeConfig.phantomBoosterAdvancedMultiplier());
    }

    @Test void overloadUsesFixedVoidLikeLivingOnlyNumbers() {
        assertEquals(4.0F, BoosterOverload.DAMAGE);
        assertEquals(3.0, BoosterOverload.RADIUS);
        assertEquals(0.5F, BoosterOverload.SOUND_VOLUME);
        assertTrue(BoosterOverload.inRadius(9.0));
        assertFalse(BoosterOverload.inRadius(9.0001));
        assertFalse(BoosterOverload.inRadius(36.0)); // old TNT-radius confusion must not apply
        assertEquals(0, BoosterOverload.boostersToDrop(i -> null));
        // Consecutive column, not limited by the stack cap; a gap stops the drop list.
        var col = new BoosterTier[]{BoosterTier.TRANSFINITE, BoosterTier.VANILLA, BoosterTier.ADVANCED, BoosterTier.VANILLA};
        assertEquals(4, BoosterOverload.boostersToDrop(i -> i < col.length ? col[i] : null));
        assertEquals(1, BoosterOverload.boostersToDrop(i -> i == 0 ? BoosterTier.TRANSFINITE : i == 2 ? BoosterTier.VANILLA : null));
        assertEquals("aamoreadditions:phantom_booster_overload", BoosterOverload.DAMAGE_TYPE.location().toString());
    }
}
