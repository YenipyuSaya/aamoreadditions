package com.aamoreadditions.upgrade;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class OilBurnPauseWiringTest {
    static String source(String path)throws Exception{return UpgradeWiringTest.source(path);}
    @Test void onlyBurnDecrementAndGenerationAreGatedNotWholeUpdate()throws Exception {
        var s=source("mixin/TileEntityOilGeneratorMixin.java");
        assertTrue(s.contains("currentBurnTime:I\",opcode=Opcodes.PUTFIELD,ordinal=0"));
        assertTrue(s.contains("if(com.aamoreadditions.effect.OilBurnPause.mayAdvance(tile.storage.getEnergyStored(),tile.storage.getMaxEnergyStored()))"));
        assertTrue(s.contains("original.call(tile,value)"));assertTrue(s.contains("?original.call(storage,amount,simulate):0"));
        assertFalse(s.contains("cancellable"));assertFalse(s.contains("ci.cancel"));
        assertFalse(s.contains("isRedstonePowered"));assertFalse(s.contains("setEnergyStored"));
    }
    @Test void fuelAndUpgradeFormulasRemainOnNativeStartingWrites()throws Exception {
        var s=source("mixin/TileEntityOilGeneratorMixin.java");
        assertTrue(s.contains("UpgradeEffects.generatorOutput(tile, value)"));
        assertTrue(s.contains("UpgradeEffects.generatorBurnTime(tile, value)"));
        assertFalse(s.contains("fraction"));assertFalse(s.contains("remainder"));
    }
    @Test void fullOilDoesNotSmokeWhileFuelAndGuiProgressRemainStored()throws Exception {
        var s=source("mixin/BlockOilGeneratorPauseMixin.java");
        assertTrue(s.contains("@Mixin(value=BlockOilGenerator.class"));assertTrue(s.contains("method=\"randomTick\""));
        assertTrue(s.contains("OilBurnPause.mayAdvance"));assertTrue(s.contains("?original.call(tile):0"));
        var json=Files.readString(Path.of("src/main/resources/aamoreadditions.mixins.json"));
        assertTrue(json.contains("BlockOilGeneratorPauseMixin"));
    }
    @Test void illegalConfigStillClearsFuelInsteadOfUsingFullEnergyPause()throws Exception {
        var s=source("upgrade/UpgradeRuntime.java");
        assertTrue(s.contains("t.currentBurnTime = t.maxBurnTime = 0; t.currentEnergyProduce = 0"));
        assertTrue(s.contains("provider.doesShareEnergy()"));
    }
}
