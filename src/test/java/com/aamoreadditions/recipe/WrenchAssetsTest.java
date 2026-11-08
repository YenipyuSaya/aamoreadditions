package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Resource validation needs neither a game launch nor a mod-loader session. */
class WrenchAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources");
    @SuppressWarnings("unchecked")
    private static Map<String, Object> json(String relative) throws Exception {
        return (Map<String, Object>) new AlloyRecipeDataTest.Json(Files.readString(ROOT.resolve(relative))).value();
    }
    @Test void originalDiagonalWrenchRecipeOnlyChangesTopLeftIngredientAndResult() throws Exception {
        var recipe = json("data/aamoreadditions/recipe/upgrade_wrench.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type"));
        assertEquals(List.of("C  ", " S ", "  S"), recipe.get("pattern"));
        assertEquals(Map.of("C", Map.of("item", "actuallyadditions:ender_casing"),
                "S", Map.of("item", "actuallyadditions:enori_crystal")), recipe.get("key"));
        assertEquals(Map.of("id", "aamoreadditions:upgrade_wrench", "count", 1.0), recipe.get("result"));
    }
    @Test void resetConsumesOneWrenchAndCreatesOneFreshUnrecordedWrench() throws Exception {
        var recipe = json("data/aamoreadditions/recipe/upgrade_wrench_nbt.json");
        assertEquals("minecraft:crafting_shapeless", recipe.get("type"));
        assertEquals(List.of(Map.of("item", "aamoreadditions:upgrade_wrench")), recipe.get("ingredients"));
        assertEquals(Map.of("id", "aamoreadditions:upgrade_wrench", "count", 1.0), recipe.get("result"));
    }
    @Test void modelUsesApprovedUnmodified16PixelTexture() throws Exception {
        var model = json("assets/aamoreadditions/models/item/upgrade_wrench.json");
        assertEquals("minecraft:item/generated", model.get("parent"));
        assertEquals(Map.of("layer0", "aamoreadditions:item/upgrade_wrench"), model.get("textures"));
        var path = ROOT.resolve("assets/aamoreadditions/textures/item/upgrade_wrench.png");
        var image = ImageIO.read(path.toFile());
        assertNotNull(image);
        assertEquals(16, image.getWidth()); assertEquals(16, image.getHeight());
        assertEquals("97ea64348ce9160e8f4b5beea2e19d89254ab8f6bc3770a297a9b603d6e58f1e",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
    }
    @Test void exactBilingualNamesTooltipAndActionBarText() throws Exception {
        var zh = json("assets/aamoreadditions/lang/zh_cn.json"); var en = json("assets/aamoreadditions/lang/en_us.json");
        assertEquals("升级插件扳手", zh.get("item.aamoreadditions.upgrade_wrench"));
        assertEquals("Upgrade Plugins Wrench", en.get("item.aamoreadditions.upgrade_wrench"));
        assertEquals("已记录升级槽位状态：", zh.get("tooltip.aamoreadditions.upgrade_wrench.recorded"));
        assertEquals("空", zh.get("tooltip.aamoreadditions.upgrade_wrench.empty"));
        assertEquals("Empty", en.get("tooltip.aamoreadditions.upgrade_wrench.empty"));
        assertEquals("升级槽位状态已记录。", zh.get("message.aamoreadditions.upgrade_wrench.recorded"));
        assertEquals("Upgrade Slots recorded.", en.get("message.aamoreadditions.upgrade_wrench.recorded"));
        assertEquals("升级插件已安装。", zh.get("message.aamoreadditions.upgrade_wrench.installed"));
        assertEquals("Upgrades installed.", en.get("message.aamoreadditions.upgrade_wrench.installed"));
    }
}
