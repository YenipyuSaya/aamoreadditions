package com.aamoreadditions.recipe;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class UpgradeRecipeDataTest {
    @SuppressWarnings("unchecked") static Map<String,Object> read(String file) throws Exception {
        return (Map<String,Object>)new AlloyRecipeDataTest.Json(Files.readString(Path.of("src/main/resources/data/aamoreadditions/recipe/"+file+".json"))).value();
    }
    @Test void newRecipesOnlyReplaceCenterAndCarryServerSideCondition() throws Exception {
        for(String[] pair:new String[][]{{"speed_upgrade_machine_2","speed_upgrade_machine_1"},{"speed_upgrade_machine_3","speed_upgrade_machine_2"},{"efficiency_upgrade_machine_2","efficiency_upgrade_machine_1"}}){
            var original=read(pair[0]);var added=read(pair[0]+"_from_previous");
            assertEquals(original.get("pattern"),added.get("pattern"));assertEquals(original.get("result"),added.get("result"));
            assertFalse(original.containsKey("neoforge:conditions"));
            assertEquals(List.of(Map.of("type","aamoreadditions:independent_upgrades")),added.get("neoforge:conditions"));
            @SuppressWarnings("unchecked") var originalKey=(Map<String,Object>)original.get("key");
            @SuppressWarnings("unchecked") var addedKey=(Map<String,Object>)added.get("key");
            assertEquals(originalKey.get("A"),addedKey.get("A"));assertEquals(originalKey.get("B"),addedKey.get("B"));
            assertEquals(Map.of("item","aamoreadditions:"+pair[1]),addedKey.get("C"));
        }
    }
    @Test void conditionIsRegisteredAndUsesFrozenServerLocalSnapshot() throws Exception {
        var root=Path.of("src/main/java/com/aamoreadditions");
        String condition=Files.readString(root.resolve("recipe/IndependentUpgradeCondition.java"));
        assertTrue(condition.contains("!UpgradeStartup.local().dependency()"));
        assertTrue(condition.contains("CONDITIONS.register(\"independent_upgrades\""));
        assertTrue(Files.readString(root.resolve("AAMoreAdditions.java")).contains("IndependentUpgradeCondition.CONDITIONS.register(modBus)"));
    }
}
