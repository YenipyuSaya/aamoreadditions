package com.aamoreadditions.upgrade;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class CoalBurnPauseWiringTest {
    static String source(String path) throws Exception { return UpgradeWiringTest.source(path); }
    @Test void onlyFirstDecrementWriteIsGatedAndBaseTickNotCancelled() throws Exception {
        var s = source("mixin/TileEntityCoalGeneratorMixin.java");
        assertTrue(s.contains("TileEntityCoalGenerator;currentBurnTime:I"));
        assertTrue(s.contains("opcode = Opcodes.PUTFIELD, ordinal = 0"));
        assertTrue(s.contains("original.call(tile, value)"));
        assertTrue(s.contains("OilBurnPause.mayAdvance(tile.storage.getEnergyStored(), tile.storage.getMaxEnergyStored())"));
        assertFalse(s.contains("cancellable")); assertFalse(s.contains("ci.cancel"));
        assertFalse(s.contains("lastBurnTime")); assertFalse(s.contains("setEnergyStored"));
        assertFalse(s.contains("isRedstonePowered"));
    }
    @Test void productionUsesSameGateAndKeepsUpgradeCalculation() throws Exception {
        var s = source("mixin/TileEntityCoalGeneratorMixin.java");
        assertTrue(s.contains("OilBurnPause.mayAdvance(storage.getEnergyStored(), storage.getMaxEnergyStored())"));
        assertTrue(s.contains("UpgradeEffects.generatorOutput(be, com.aamoreadditions.effect.UpgradeConfig.coalFePerTick(amount))"));
        assertTrue(s.contains("simulate) : 0"));
        assertTrue(s.contains("UpgradeEffects.generatorBurnTime(tile, value)"));
        assertFalse(s.contains("fraction")); assertFalse(s.contains("remainder"));
    }
    @Test void smokePredicateIsVirtualAndMixinRegistered() throws Exception {
        var s = source("mixin/BlockCoalGeneratorPauseMixin.java");
        assertTrue(s.contains("@Mixin(value=BlockCoalGenerator.class"));
        assertTrue(s.contains("method=\"randomTick\""));
        assertTrue(s.contains("OilBurnPause.mayAdvance")); assertTrue(s.contains("?original.call(tile):0"));
        assertFalse(s.contains("PUTFIELD"));
        assertTrue(Files.readString(Path.of("src/main/resources/aamoreadditions.mixins.json"))
                .contains("BlockCoalGeneratorPauseMixin"));
    }
    @Test void illegalConfigurationStillClearsFuelAndRetainsFeSharing() throws Exception {
        var s = source("upgrade/UpgradeRuntime.java");
        assertTrue(s.contains("if (be instanceof TileEntityCoalGenerator t) t.currentBurnTime = t.maxBurnTime = 0"));
        assertTrue(s.contains("provider.doesShareEnergy()"));
    }
}
