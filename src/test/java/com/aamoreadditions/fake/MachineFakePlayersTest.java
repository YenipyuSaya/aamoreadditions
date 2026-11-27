package com.aamoreadditions.fake;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Profile identity only; no ServerLevel or FakePlayer is constructed. */
class MachineFakePlayersTest {
    @Test void nameFitsTheLegacySixteenCharacterLimit() {
        assertEquals("[AAMA Machine]", MachineFakePlayers.NAME);
        assertTrue(MachineFakePlayers.NAME.length() <= 16);
    }

    @Test void uuidIsDeterministicForTheSameDimensionAndPosition() {
        var overworld = ResourceLocation.parse("minecraft:overworld");
        var pos = new BlockPos(8, 64, -12);
        var first = MachineFakePlayers.uuid(overworld, pos);
        assertEquals(first, MachineFakePlayers.uuid(overworld, pos));
        assertNotEquals(first, MachineFakePlayers.uuid(overworld, pos.offset(1, 0, 0)));
        assertNotEquals(first, MachineFakePlayers.uuid(ResourceLocation.parse("minecraft:the_nether"), pos));
    }
}
