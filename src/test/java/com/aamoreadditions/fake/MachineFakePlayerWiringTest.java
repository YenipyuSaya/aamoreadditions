package com.aamoreadditions.fake;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MachineFakePlayerWiringTest {
    private static String source(String rel) throws Exception {
        return Files.readString(Path.of("src/main/java/com/aamoreadditions/" + rel));
    }
    private static String json() throws Exception {
        return Files.readString(Path.of("src/main/resources/aamoreadditions.mixins.json"));
    }

    @Test void mixinsAreRegistered() throws Exception {
        var json = json();
        for (String m : new String[]{
                "WorldUtilFakeHarvestMixin", "WorldUtilUseItemMixin", "TileEntityBreakerMixin",
                "TileEntityPhantomPlacerMixin", "DefaultFarmerBehaviorMixin", "LensKillerMixin",
                "MachineFakePlayerRemovalMixin"}) {
            assertTrue(json.contains("\"" + m + "\""), m);
        }
    }

    @Test void workBlocksDoNotCallTheSharedMinecraftPlayer() throws Exception {
        assertFalse(source("digger/DiggerRuntime.java").contains("FakePlayerFactory"));
        assertTrue(source("digger/DiggerRuntime.java").contains("MachineFakePlayers.of(tile)"));
        assertFalse(source("farmer/FarmerMemoryRuntime.java").contains("FakePlayerFactory"));
        assertTrue(source("farmer/FarmerMemoryRuntime.java").contains("MachineFakePlayers.of(tile)"));
        var harvest = source("mixin/WorldUtilFakeHarvestMixin.java");
        assertFalse(harvest.contains("FakePlayerFactory"));
        assertTrue(harvest.contains("MachineFakePlayers.of(caller)"));
        assertTrue(harvest.contains("withMainHand"));
        var use = source("mixin/WorldUtilUseItemMixin.java");
        assertTrue(use.contains("MachineFakePlayers.current()"));
        assertTrue(use.contains("useItemAtSide without a bound AAMA machine"));
        assertTrue(source("mixin/TileEntityBreakerMixin.java").contains("MachineFakePlayers.bind"));
        var phantom = source("mixin/TileEntityPhantomPlacerMixin.java");
        assertTrue(phantom.contains("MachineFakePlayers.bind"));
        assertTrue(phantom.contains("fireFakeHarvestEventsForDropChance(self, server, target)"));
        assertTrue(phantom.contains("Block.getDrops(state, server, target, resolved.blockEntity())")
                || phantom.contains("Block.getDrops(state, server, target, server.getBlockEntity(target))"));
        assertTrue(phantom.contains("BoundTargets.resolve"));
        assertTrue(phantom.contains("if (!self.isBreaker) return;"));
        assertTrue(source("mixin/DefaultFarmerBehaviorMixin.java").contains("MachineFakePlayers.current()"));
        assertTrue(source("mixin/LensKillerMixin.java").contains("MachineFakePlayers.of(be)"));
        assertTrue(source("mixin/TileEntityVerticalDiggerMixin.java").contains("MachineFakePlayers.of"));
        assertTrue(source("mixin/TileEntityFarmerMixin.java").contains("MachineFakePlayers.bind(tile)"));
        assertTrue(source("mixin/TileEntityFarmerMixin.java").contains("MachineFakePlayers.unbind()"));
        assertTrue(source("mixin/MachineFakePlayerRemovalMixin.java").contains("MachineFakePlayers.release"));
        assertTrue(source("fake/MachineFakePlayerEvents.java").contains("releaseLevel"));
    }
}
