package com.aamoreadditions.config;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Pure label and source-contract checks; no claim of a running Configured screen. */
class EnglishConfigTest {
    private String source(String name) throws Exception {
        return Files.readString(Path.of("src/main/java/com/aamoreadditions/"+name));
    }
    @Test void literalAndGeneratedFieldCategoryNamesHaveReadableLabels() throws Exception {
        Set<String> keys=new HashSet<>();
        for(String name:List.of("effect/UpgradeConfig.java","upgrade/UpgradeStartup.java")) {
            var s=source(name); assertTrue(s.contains("new com.aamoreadditions.config.EnglishConfigBuilder()"));
            var matcher=Pattern.compile("(?:define(?:InRange)?|push)\\(\"([^\"]+)\"").matcher(s);
            while(matcher.find())keys.add(matcher.group(1));
        }
        for(String name:List.of("effect/MachineKind.java","upgrade/UpgradeType.java")) {
            var matcher=Pattern.compile("\\(\"([a-z0-9_]+)\"").matcher(source(name));
            while(matcher.find())keys.add(matcher.group(1));
        }
        keys.addAll(List.of("slots","alloy_furnace","casing_cross","canola_oil","refined_canola_oil","crystallized_oil","empowered_oil"));
        assertTrue(keys.size()>=45);
        for(var key:keys) {
            var label=EnglishConfigNames.label(key);assertFalse(label.isBlank(),key);
            assertFalse(label.contains("_"),key);assertFalse(label.contains("aamoreadditions.configuration"),key);
        }
    }
    @Test void builderLabelsMetadataWithoutChangingStoragePath() throws Exception {
        var s=source("config/EnglishConfigBuilder.java");
        assertTrue(s.contains("translation(EnglishConfigNames.label(path.getLast()))"));
        assertTrue(s.contains("super.define(path,defaults,validator,type)"));assertTrue(s.contains("super.push(path)"));
        assertEquals("Crusher FE Per Tick",EnglishConfigNames.label("crusher_fe_per_tick"));
        assertEquals("Base Values",EnglishConfigNames.label("base_values"));
        assertEquals("Dependency Upgrade Plugins",EnglishConfigNames.label("Dependency Upgrade Plugins"));
    }
    @Test void configuredOwnsItsFactoryWhenInstalled() throws Exception {
        var s=source("client/UpgradeConfigScreen.java");
        int guard=s.indexOf("if (ModList.get().isLoaded(\"configured\")) return;");
        assertTrue(guard>=0);assertTrue(guard<s.indexOf(".registerExtensionPoint("));
        assertFalse(source("upgrade/UpgradeStartup.java").contains(".translation(\"aamoreadditions.configuration"));
    }
}
