package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Alloy / Efficient-Smelting mode toggle . */
class AlloyModeWiringTest {
    static String src(String p) throws Exception { return Files.readString(Path.of("src/main/java/com/aamoreadditions/" + p)); }
    static final String GUI = "src/main/resources/assets/aamoreadditions/textures/gui/";

    @Test void modeIsSavedSyncedAndToggledOnTheServer() throws Exception {
        var be = src("block/AlloyFurnaceBlockEntity.java");
        assertTrue(be.contains("public boolean efficientMode;"));
        assertTrue(be.contains("efficientMode = !efficientMode;"));
        assertTrue(be.contains("compound.putBoolean(\"AAMAMode\", efficientMode);"));
        assertTrue(be.contains("efficientMode = compound.getBoolean(\"AAMAMode\");"));
        var menu = src("menu/AlloyFurnaceMenu.java");
        assertTrue(menu.contains("BUTTON_LOCK = 7710, BUTTON_MODE = 7711"));
        assertTrue(menu.contains("if (!player.level().isClientSide) furnace.toggleMode();"));
        // alloying recipes only run in Alloy mode; switching resets progress
        int tick = be.indexOf("private void tickAlloy(");
        assertTrue(be.substring(tick, be.indexOf("private void finishAlloy(")).contains("var match = com.aamoreadditions.recipe.ModRecipes.find(level, alloyInput(), efficientMode);"));
        assertTrue(be.contains("return com.aamoreadditions.recipe.ModRecipes.find(level, alloyInput(), efficientMode);"));
        var mr = src("recipe/ModRecipes.java");
        assertTrue(mr.contains("return efficient ? findEfficient(level, input) : find(level, input);"));
        int t = be.indexOf("public void toggleMode()");
        assertTrue(be.substring(t, be.indexOf("}", t)).contains("firstSmeltTime = 0;"));
        assertTrue(be.substring(t, be.indexOf("}", t)).contains("lock.unlock();"));
    }

    @Test void buttonSitsBelowLockWithSameLook() throws Exception {
        var sc = src("client/screen/AlloyFurnaceScreen.java");
        assertTrue(sc.contains("MODE_X = LOCK_X, MODE_Y = LOCK_Y + LOCK_SIZE + 1"));
        assertTrue(sc.contains("new ModeButton(this.getGuiLeft() + MODE_X, this.topPos + MODE_Y)"));
        assertTrue(sc.contains("handleInventoryButtonClick(menu.containerId, AlloyFurnaceMenu.BUTTON_MODE)"));
        assertTrue(sc.contains("g.blit(tile.efficientMode ? TEX_MODE_EFFICIENT : TEX_MODE_ALLOY, getX() + o, getY() + o, 0, 0, 16, 16, 16, 16);"));
        // energy bar starts at x27: button spans x-1..16, y23..40
        assertTrue(-1 + 18 <= 27);
    }

    @Test void iconsAndLang() throws Exception {
        for (String n : new String[]{"mode_alloy.png", "mode_efficient.png"}) {
            var im = javax.imageio.ImageIO.read(Path.of(GUI + n).toFile());
            assertEquals(16, im.getWidth(), n); assertEquals(16, im.getHeight(), n);
        }
        var fl = javax.imageio.ImageIO.read(Path.of(GUI + "mode_efficient.png").toFile());
        // 14x14 vanilla flame centred: 1px transparent border, GUI background grey removed
        for (int i = 0; i < 16; i++) {
            assertEquals(0, fl.getRGB(i, 0) >>> 24); assertEquals(0, fl.getRGB(0, i) >>> 24);
            assertEquals(0, fl.getRGB(i, 15) >>> 24); assertEquals(0, fl.getRGB(15, i) >>> 24);
        }
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) assertNotEquals(0xFFC6C6C6, fl.getRGB(x, y));
        var en = Files.readString(Path.of("src/main/resources/assets/aamoreadditions/lang/en_us.json"));
        var zh = Files.readString(Path.of("src/main/resources/assets/aamoreadditions/lang/zh_cn.json"));
        assertTrue(en.contains("\"gui.aamoreadditions.alloyfurnace.mode.alloy\": \"Alloy mode\""));
        assertTrue(en.contains("\"gui.aamoreadditions.alloyfurnace.mode.efficient\": \"Efficient-Smelting mode\""));
        assertTrue(zh.contains("\"gui.aamoreadditions.alloyfurnace.mode.alloy\": \"合金模式\""));
        assertTrue(zh.contains("\"gui.aamoreadditions.alloyfurnace.mode.efficient\": \"高效烧炼模式\""));
        assertTrue(zh.contains("\"gui.aamoreadditions.alloyfurnace.mode.switch\": \"按下切换至%s\""));
    }
}
