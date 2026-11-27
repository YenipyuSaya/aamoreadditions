package com.aamoreadditions.booster;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoundTargetsWiringTest {
    private static String src(String rel) throws Exception {
        return Files.readString(Path.of("src/main/java/com/aamoreadditions/" + rel));
    }

    private static String json() throws Exception {
        return Files.readString(Path.of("src/main/resources/aamoreadditions.mixins.json"));
    }

    @Test void resolverIsTheDocumentedReusableEntryPoint() throws Exception {
        var helper = src("booster/BoundTargets.java");
        assertTrue(helper.contains("server.getLevel"));
        assertTrue(helper.contains("Never loads chunks") || helper.contains("never loads a chunk") || helper.contains("Never loads a"));
        assertTrue(helper.contains("future AAMA interface") || helper.contains("future"));
        assertTrue(helper.contains("BoundWorlds"));
        assertTrue(helper.contains("resolve(BlockEntity"));
        assertTrue(helper.contains("capability("));
        assertTrue(helper.contains("onLevelUnload"));
        assertTrue(helper.contains("listenerOutOfDate"));
        assertFalse(helper.contains("getChunk("));
        assertFalse(helper.contains("getChunkAt"));
    }

    @Test void everyPhantomProxyUsesBoundTargetsNotTheHostLevel() throws Exception {
        var json = json();
        for (String m : List.of(
                "TileEntityPhantomfaceRangeMixin", "TileEntityPhantomItemfaceMixin",
                "TileEntityPhantomEnergyfaceMixin", "TileEntityPhantomLiquifaceMixin",
                "TileEntityPhantomRedstonefaceMixin", "TileEntityPhantomPlacerMixin",
                "TileEntityPhantomPlacerBoundMixin")) {
            assertTrue(json.contains("\"" + m + "\""), m);
        }
        for (String rel : List.of(
                "mixin/TileEntityPhantomItemfaceMixin.java",
                "mixin/TileEntityPhantomEnergyfaceMixin.java",
                "mixin/TileEntityPhantomLiquifaceMixin.java",
                "mixin/TileEntityPhantomRedstonefaceMixin.java",
                "mixin/TileEntityPhantomPlacerMixin.java")) {
            var s = src(rel);
            assertTrue(s.contains("BoundTargets"), rel);
        }
        var breaker = src("mixin/TileEntityPhantomPlacerMixin.java");
        assertTrue(breaker.contains("BoundTargets.resolve"));
        assertTrue(breaker.contains("resolved.level()"));
        var face = src("mixin/TileEntityPhantomfaceRangeMixin.java");
        assertTrue(face.contains("registerCapabilityListener"));
        assertTrue(face.contains("listenerLevel") || face.contains("resolveIfLoaded"));
        assertTrue(face.contains("isIllegalBind"));
        assertTrue(src("mixin/TileEntityPhantomPlacerBoundMixin.java").contains("isIllegalBind"));
        var events = src("booster/BoundTargetEvents.java");
        assertTrue(events.contains("BoundTargets.onLevelUnload"));
        var fake = src("fake/MachineFakePlayers.java");
        assertTrue(fake.contains("WORK_LEVEL"));
        assertTrue(fake.contains("bind(BlockEntity be, ServerLevel workLevel, BlockPos workPos)"));
    }

    @Test void radiusThreeSphereIsUnchanged() {
        assertEquals(3.0, BoosterOverload.RADIUS);
        assertTrue(BoosterOverload.inRadius(9.0));
        assertFalse(BoosterOverload.inRadius(9.0001));
    }

    @Test void crossDimensionPortalParticlesAreSpawnedFromTheHostServerTick() throws Exception {
        var face = src("mixin/TileEntityPhantomfaceRangeMixin.java");
        assertTrue(face.contains("BoundParticles.trySpawn"));
        assertTrue(face.contains("aama$skipCrossDimParticles"));
        var placer = src("mixin/TileEntityPhantomPlacerBoundMixin.java");
        assertTrue(placer.contains("BoundParticles.trySpawn"));
        assertTrue(placer.contains("aama$skipCrossDimParticles"));
    }

    @Test void connectorAlwaysBindsAndHudUsesSyncedDisplay() throws Exception {
        var json = json();
        assertTrue(json.contains("\"PhantomHudMixin\""));
        assertTrue(json.contains("\"TileEntityPhantomfaceCapListenerMixin\""));
        var connector = src("mixin/ItemPhantomConnectorMixin.java");
        assertTrue(connector.contains("BoundWorlds.set"));
        assertTrue(connector.contains("BoundDisplays.refresh"));
        assertFalse(connector.contains("allowsCrossDimension"));
        assertFalse(connector.contains("InteractionResult.FAIL"));
        assertTrue(connector.contains("appendHoverText"));
        assertTrue(connector.contains("BoundHudText.dimensionName"));
        var hud = src("mixin/PhantomHudMixin.java");
        assertTrue(hud.contains("BoundHudText.crossDimensionalLine"));
        // AA drawWordWrap is (gui, font, text, x, y, width, color, shadow) -> IIIIZ, not IIIZ.
        assertTrue(hud.contains("FormattedText;IIIIZ)V"));
        assertFalse(hud.contains("FormattedText;IIIZ)V"));
        // AA invokes ClientLevel.getBlockState, not Level.getBlockState — wrapping the latter crashes load.
        assertFalse(hud.contains("Level;getBlockState"));
        assertFalse(hud.contains("import com.llamalad7.mixinextras.injector.wrapoperation"));
        assertTrue(hud.contains("appendDimension"));
        assertTrue(hud.contains("BoundDisplays.defaultStateBlock"));
        var displays = src("booster/BoundDisplays.java");
        assertTrue(displays.contains("aamaBoundBlock"));
        assertTrue(displays.contains("aamaBoundOk"));
        assertTrue(displays.contains("useSyncedInRange"));
        for (String rel : List.of(
                "mixin/TileEntityPhantomfaceRangeMixin.java",
                "mixin/TileEntityPhantomItemfaceMixin.java",
                "mixin/TileEntityPhantomEnergyfaceMixin.java",
                "mixin/TileEntityPhantomLiquifaceMixin.java",
                "mixin/TileEntityPhantomPlacerBoundMixin.java")) {
            var s = src(rel);
            assertTrue(s.contains("BoundDisplays.useSyncedInRange"), rel);
            assertTrue(s.contains("BoundDisplays.boundOk"), rel);
        }
    }
}
