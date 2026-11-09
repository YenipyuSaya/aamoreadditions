package com.aamoreadditions.config;

import com.aamoreadditions.effect.UpgradeConfig;
import com.aamoreadditions.upgrade.UpgradeStartup;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real registered specs: categories have titles only, leaves keep their comments. */
class CategoryTooltipTest {
    private static int categories(ModConfigSpec spec,UnmodifiableConfig node,List<String> path) {
        int count=0;
        for(var entry:node.valueMap().entrySet()) if(entry.getValue() instanceof UnmodifiableConfig child) {
            var next=new ArrayList<>(path);next.add(entry.getKey());count++;
            assertNull(spec.getLevelComment(next),next.toString());
            var title=spec.getLevelTranslationKey(next);
            assertNotNull(title,next.toString());assertFalse(title.isBlank());
            assertFalse(title.contains("aamoreadditions.configuration"));
            count+=categories(spec,child,next);
        }
        return count;
    }
    @Test void everyServerDirectoryHasNoTooltipAndKeepsEnglishTitle() {
        assertTrue(categories(UpgradeConfig.SPEC,UpgradeConfig.SPEC.getSpec(),List.of())>40);
    }
    @Test void everyCommonDirectoryHasNoTooltipAndKeepsEnglishTitle() {
        assertEquals(1,categories(UpgradeStartup.SPEC,UpgradeStartup.SPEC.getSpec(),List.of()));
    }
    @Test void leafDescriptionsAndExactDependencyWarningArePreserved() {
        ModConfigSpec.ValueSpec cost=UpgradeConfig.SPEC.getSpec().get("base_values.crusher_fe_per_tick");
        assertTrue(cost.getComment().contains("Crusher FE/t"));
        ModConfigSpec.ValueSpec dependency=UpgradeStartup.SPEC.getSpec().get("Dependency Upgrade Plugins");
        assertEquals("If true, higher-level upgrades cannot be installed independently. Lower-level upgrades must be installed beforehand.\n\nWarning: This is very likely to cause machines that have already been configured to stop working, you must be clear about what you are doing!",dependency.getComment());
    }
}
