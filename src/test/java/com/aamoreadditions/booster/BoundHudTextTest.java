package com.aamoreadditions.booster;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BoundHudTextTest {
    private static Map<String, String> lang(String file) throws Exception {
        var json = Files.readString(Path.of("src/main/resources/assets/aamoreadditions/lang/" + file));
        return new Gson().fromJson(json, new TypeToken<Map<String, String>>(){}.getType());
    }

    @Test void dimensionLangKeyAndFallbackMatchVanillaShape() {
        assertEquals("dimension.minecraft.overworld", BoundHudText.dimensionLangKey("minecraft", "overworld"));
        assertEquals("dimension.minecraft.the_nether", BoundHudText.dimensionLangKey("minecraft", "the_nether"));
        assertEquals("dimension.minecraft.the_end", BoundHudText.dimensionLangKey("minecraft", "the_end"));
        assertEquals("minecraft:the_nether", BoundHudText.dimensionFallback("minecraft", "the_nether"));
        assertEquals("dimension.modid.custom_dim", BoundHudText.dimensionLangKey("modid", "custom_dim"));
        assertEquals("modid:custom_dim", BoundHudText.dimensionFallback("modid", "custom_dim"));
    }

    @Test void crossDimensionalLineDropsDistanceAndPutsDimOnTheRight() {
        assertEquals(
                "The connected Block is Chest at 10, 64, -3 Overworld. Cross-Dimensional",
                BoundHudText.previewCross("Chest", 10, 64, -3, "Overworld", "Cross-Dimensional"));
        assertEquals(
                "The connected Block is 箱子 at 10, 64, -3 主世界. 跨维度",
                BoundHudText.previewCross("箱子", 10, 64, -3, "主世界", "跨维度"));
    }

    @Test void sameDimensionAppendsDimensionToTheRightOfTheAaSentence() {
        String aa = "The connected Block is Chest at 10, 64, -3 that is 5 Blocks away.";
        assertEquals(aa + " Overworld", BoundHudText.previewSameDimAppend(aa, "Overworld"));
        String zh = "连接的方块箱子在10,64,-3有5个方块远.";
        assertEquals(zh + " 主世界", BoundHudText.previewSameDimAppend(zh, "主世界"));
    }

    @Test void langKeysAreExact() throws Exception {
        var en = lang("en_us.json");
        var zh = lang("zh_cn.json");
        assertEquals("Cross-Dimensional", en.get(BoundHudText.CROSS_DIMENSIONAL_KEY));
        assertEquals("跨维度", zh.get(BoundHudText.CROSS_DIMENSIONAL_KEY));
        assertEquals("The connected Block is %s at %s, %s, %s %s. %s", en.get(BoundHudText.BOUND_INFO_CROSS_KEY));
        assertEquals("连接的方块%s在%s,%s,%s %s. %s", zh.get(BoundHudText.BOUND_INFO_CROSS_KEY));
    }
}
