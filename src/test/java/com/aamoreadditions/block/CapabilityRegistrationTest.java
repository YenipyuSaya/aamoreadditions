package com.aamoreadditions.block;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** Guards the regression: AA only registers caps for its own BE types, so ours must register energy + item caps itself. */
class CapabilityRegistrationTest {
    @Test
    void alloyFurnaceRegistersEnergyAndItemCapabilities() throws Exception {
        String s = Files.readString(Path.of("src/main/java/com/aamoreadditions/block/ModBlocks.java"));
        assertTrue(s.contains("bus.addListener(ModBlocks::registerCapabilities)"), "listener not wired");
        assertTrue(s.matches("(?s).*Capabilities\\.EnergyStorage\\.BLOCK,\\s*ALLOY_FURNACE_BE\\.get\\(\\).*"), "energy cap missing");
        assertTrue(s.matches("(?s).*Capabilities\\.ItemHandler\\.BLOCK,\\s*ALLOY_FURNACE_BE\\.get\\(\\).*"), "item cap missing");
    }

    @Test
    void itemHandlerIncludesExtraSlotWithoutExtraction() throws Exception {
        String s = Files.readString(Path.of("src/main/java/com/aamoreadditions/block/AlloyFurnaceBlockEntity.java"));
        assertTrue(s.contains("new net.neoforged.neoforge.items.wrapper.CombinedInvWrapper(inv, extraAuto)"));
        assertTrue(s.contains("canAutoInsert(autoAccepts(LOCK_EXTRA, stack))"));
        assertFalse(s.contains("if (!validInput(stack)) return false;"), "automation must not filter by ingredient");
        assertTrue(s.contains("ExtraSlotRules.canAutoExtract()"));
    }
}
