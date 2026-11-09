package com.aamoreadditions.upgrade;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** Source contracts complement pure view tests. These do not run Jade or Minecraft. */
class JadeWiringTest {
    static String source(String relative) throws Exception { return UpgradeWiringTest.source(relative); }
    @Test void payloadUsesAuthoritativeModeAndCentralLegalityAndActivation() throws Exception {
        var s = source("compat/jade/UpgradeJadeProvider.java");
        assertTrue(s.contains("UpgradeStartup.dependency()"));assertTrue(s.contains("handler.legal()"));
        assertTrue(s.contains("handler.activeUpgrades()"));assertTrue(s.contains("UpgradeDisplay.visible(installed, dependency, legal"));
        assertTrue(s.contains("UpgradeDisplay.group(visible, ItemStack::getItem)"));
        assertTrue(s.contains("state.putBoolean(\"Dependency\", dependency)"));
        assertTrue(s.contains("state.putBoolean(\"Legal\", legal)"));
        assertTrue(s.contains("data.put(KEY, state)"));assertFalse(s.contains("if (!list.isEmpty()) data.put"));
        assertTrue(s.contains("if (!state.getBoolean(\"Legal\")) return"));
    }
    @Test void clientDrawsRealIconsTimesCountsAndBoundedRowsNotNames() throws Exception {
        var s=source("compat/jade/UpgradeJadeProvider.java");
        assertTrue(s.contains("helper.smallItem(group.representative())"));
        assertTrue(s.contains("Component.literal(\"×\" + group.count())"));
        assertTrue(s.contains("UpgradeDisplay.rows(groups)"));
        assertFalse(s.contains("getHoverName()"));assertFalse(s.contains("Minecraft.getInstance"));
        assertTrue(s.contains("copyWithCount(1).save"));
    }
    @Test void wrenchAndJadeShareCrossAndNativeTargetResolution() throws Exception {
        var s=source("compat/jade/UpgradeJadeProvider.java");
        assertTrue(s.contains("UpgradeSlots.target(accessor.getLevel(), accessor.getPosition())"));
        assertTrue(source("item/UpgradeWrenchItem.java").contains("UpgradeSlots.target(level, pos)"));
        var slots=source("upgrade/UpgradeSlots.java");
        assertTrue(slots.contains("data.entryAt(pos)"));assertTrue(slots.contains("CasingCrossStructure.matches(level, anchor)"));
        for (String part : new String[]{"DECORATIVE_MACHINE_CASING","EMPOWERER","DISPLAY_STAND"}) assertTrue(s.contains(part));
        assertTrue(s.contains("UpgradeSlots.supports(be)"));assertTrue(slots.contains("TileEntityEnergizer"));
    }
    @Test void pluginRemainsOptionalAndCoversNonBlockEntityCasings() throws Exception {
        var s=source("compat/jade/AAMoreAdditionsJadePlugin.java");
        assertTrue(s.contains("@WailaPlugin"));
        assertTrue(s.contains("registerBlockDataProvider(UpgradeJadeProvider.INSTANCE, Block.class)"));
        assertTrue(s.contains("registerBlockComponent(UpgradeJadeProvider.INSTANCE, Block.class)"));
        assertFalse(source("AAMoreAdditions.java").contains("compat.jade"));
        assertFalse(source("compat/jade/UpgradeJadeProvider.java").contains("net.minecraft.client"));
        assertTrue(Files.readString(Path.of("build.gradle")).contains("compileOnly \"maven.modrinth:jade:"));
    }
    @Test void exactRequestedActiveHeadingsExistInBothLocales() throws Exception {
        var base=Path.of("src/main/resources/assets/aamoreadditions/lang");
        assertTrue(Files.readString(base.resolve("en_us.json")).contains("\"tooltip.aamoreadditions.active_upgrades\": \"upgrade plugins activated:\""));
        assertTrue(Files.readString(base.resolve("zh_cn.json")).contains("\"tooltip.aamoreadditions.active_upgrades\": \"当前激活的升级插件：\""));
    }
}
