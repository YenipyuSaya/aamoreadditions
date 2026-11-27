package com.aamoreadditions.digger;

import com.aamoreadditions.config.EnglishConfigNames;
import com.aamoreadditions.effect.UpgradeConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiggerConfigTest {
    @Test void verticalDiggerGroupHasThreeCostValues() {
        var spec = UpgradeConfig.SPEC.getSpec();
        String[] keys = {"vertical_digger_mining_fe", "vertical_digger_ores_only_multiplier", "vertical_digger_block_placing_fe"};
        int[] defaults = {650, 3, 500};
        for (int i = 0; i < keys.length; i++) {
            var v = (net.neoforged.neoforge.common.ModConfigSpec.ValueSpec) spec.get("vertical_digger." + keys[i]);
            assertNotNull(v, keys[i]);
            assertEquals(defaults[i], v.getDefault());
            assertEquals(EnglishConfigNames.label(keys[i]), v.getTranslationKey());
        }
        // unloaded spec falls back to AA's values
        assertEquals(650, UpgradeConfig.diggerMiningFe());
        assertEquals(3, UpgradeConfig.diggerOresOnlyMultiplier());
        assertEquals(500, UpgradeConfig.diggerPlacingFe());
    }
}
