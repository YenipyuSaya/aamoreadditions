package com.aamoreadditions.digger;

import com.aamoreadditions.client.UpgradeStrip;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Source/resource contract checks; NOT runtime Mixin or in-game tests. */
class DiggerWiringTest {
    static String source(String relative) throws Exception { return Files.readString(Path.of("src/main/java/com/aamoreadditions/" + relative)); }
    static String resource(String relative) throws Exception { return Files.readString(Path.of("src/main/resources/" + relative)); }

    @Test void mixinsAreRegistered() throws Exception {
        var json = resource("aamoreadditions.mixins.json");
        for (String m : new String[]{"\"TileEntityBaseAccessor\"", "\"TileEntityVerticalDiggerMixin\"", "\"ContainerMinerMixin\"", "\"WorldUtilFakeHarvestMixin\""}) assertTrue(json.contains(m), m);
        assertTrue(source("mixin/UpgradeQuickMoveMixin.java").contains("ContainerMiner.class"));
        assertFalse(source("mixin/UpgradeMachineTickMixin.java").contains("TileEntityVerticalDigger"), "single HEAD-cancel lives in the digger mixin");
        var tick = source("mixin/TileEntityVerticalDiggerMixin.java");
        assertTrue(tick.contains("if (UpgradeRuntime.tick(be)) return;"));
        assertTrue(source("mixin/BlockContainerBaseMixin.java").contains("DiggerRuntime.dropFillers(level, digger)"));
    }

    @Test void chunkPauseAndCheckOrder() throws Exception {
        var s = source("digger/DiggerRuntime.java");
        int loaded = s.indexOf("if (!level.isLoaded(target)) return false;");
        int air = s.indexOf("if (state.isAir())"), black = s.indexOf("if (blacklisted(state.getBlock())) return true;"), drops = s.indexOf("Block.getDrops(state, level, target, be, null, tool)");
        assertTrue(loaded > 0 && loaded < air && air < black && black < drops);
        assertFalse(s.contains("getChunk("), "never loads chunks");
        assertEquals(1, s.split("new ItemStack\\(ActuallyItems.NETHERITE_AIOT").length - 1, "one cached dummy tool");
        assertTrue(s.contains("Block.getDrops(state, level, target, be, null, tool)"));
        assertFalse(s.contains("silk || fortune > 0"), "drops always use the cached tool, including the plain AIOT");
    }

    @Test void buttonIdsDoNotCollideWithUpgradePanelIds() {
        int[] ids = {DiggerRuntime.BUTTON_MODE, DiggerRuntime.BUTTON_HIGHLIGHT, DiggerRuntime.BUTTON_FILLERS_CLOSE, DiggerRuntime.BUTTON_FILLERS_OPEN};
        for (int id : ids) assertTrue(id != 7700 && id != 7701 && id != 7710);
        assertEquals(4, java.util.Arrays.stream(ids).distinct().count());
    }

    @Test void fillersGeometry() {
        // Filler panel rows stay aligned with AA's output rows (y 21/39/57)
        assertEquals(21, DiggerMenus.SLOT_Y0);
        assertTrue(DiggerMenus.PANEL_X > 148, "right of the gui_breaker head");
        // Toggle: same column as the inner top-right upgrade buttons, vertically centred on the 3x3 output grid
        assertEquals(134, DiggerMenus.TOGGLE_X);
        assertEquals(10, DiggerMenus.TOGGLE_SIZE);
        int outputTop = 21, outputBottom = 21 + 2 * 18 + 16; // last slot y=57, 16px item
        assertEquals((outputTop + outputBottom) / 2 - DiggerMenus.TOGGLE_SIZE / 2, DiggerMenus.TOGGLE_Y);
        assertEquals(42, DiggerMenus.TOGGLE_Y);
    }

    @Test void upgradeButtonsAtInnerTopRight() throws Exception {
        assertTrue(source("client/UpgradeStrip.java").contains("GuiMiner) return 134;"));
        var screen = source("client/digger/DiggerScreen.java");
        assertTrue(screen.contains("BTN_X = -1, MODE_Y = 4, HIGHLIGHT_Y = 23"));
    }

    @Test void langKeysExistInBothLanguages() throws Exception {
        var en = JsonParser.parseString(resource("assets/aamoreadditions/lang/en_us.json")).getAsJsonObject();
        var zh = JsonParser.parseString(resource("assets/aamoreadditions/lang/zh_cn.json")).getAsJsonObject();
        for (String k : new String[]{"message.aamoreadditions.upgrades.conflict", "gui.aamoreadditions.digger.mode.strict_radius",
                "gui.aamoreadditions.digger.mode.strict_chunk", "gui.aamoreadditions.digger.highlight", "gui.aamoreadditions.digger.highlight.on",
                "gui.aamoreadditions.digger.highlight.off", "gui.aamoreadditions.digger.fillers.open", "gui.aamoreadditions.digger.fillers.close",
                "gui.aamoreadditions.digger.fillers.required", "gui.aamoreadditions.digger.fillers.need_ores_mode", "gui.aamoreadditions.info.digger.silk_touch",
                "gui.aamoreadditions.info.digger.vertical_span", "gui.aamoreadditions.info.speed_cap"}) {
            assertTrue(en.has(k), k);
            assertTrue(zh.has(k), k);
        }
        assertEquals("与其它插件冲突", zh.get("message.aamoreadditions.upgrades.conflict").getAsString());
        assertEquals("已安装此类插件", zh.get("message.aamoreadditions.upgrades.already_installed").getAsString());
        assertEquals("This upgrade type is already installed", en.get("message.aamoreadditions.upgrades.already_installed").getAsString());
        assertEquals("需要：钻头方块放置升级", zh.get("gui.aamoreadditions.digger.fillers.required").getAsString());
        assertEquals("Need to be in \"Only Mining Ores\" mode", en.get("gui.aamoreadditions.digger.fillers.need_ores_mode").getAsString());
        assertEquals("需要处于“只挖掘矿石”模式", zh.get("gui.aamoreadditions.digger.fillers.need_ores_mode").getAsString());
        assertEquals("Blocks per action: %s", en.get("gui.aamoreadditions.info.digger.vertical_span").getAsString());
        assertEquals("每次工作判定：%s 块", zh.get("gui.aamoreadditions.info.digger.vertical_span").getAsString());
        assertEquals(" (Max)", en.get("gui.aamoreadditions.info.speed_cap").getAsString());
        assertEquals("（最大）", zh.get("gui.aamoreadditions.info.speed_cap").getAsString());
        assertFalse(resource("assets/aamoreadditions/lang/zh_cn.json").contains("组件"));
    }

    @Test void duplicateIsWhiteAndConflictIsRed() throws Exception {
        var s = source("upgrade/UpgradeRules.java");
        assertTrue(s.contains("upgrades.already_installed\").withStyle(ChatFormatting.WHITE)"));
        assertTrue(s.contains("upgrades.conflict\").withStyle(ChatFormatting.RED)"));
        assertTrue(source("client/UpgradeStrip.java").contains("check == UpgradeRules.Check.CONFLICT"));
    }

    @Test void fillersPanelReusesUpgradeSlideAnimation() throws Exception {
        assertEquals(180, UpgradeStrip.ANIM_MS);
        assertEquals(0f, UpgradeStrip.ease(0f));
        assertEquals(1f, UpgradeStrip.ease(1f));
        assertEquals(0.5f, UpgradeStrip.ease(0.5f), 1e-6f);
        assertEquals(DiggerMenus.SLIDE, UpgradeStrip.slideOffset(0f, DiggerMenus.SLIDE));
        assertEquals(0, UpgradeStrip.slideOffset(1f, DiggerMenus.SLIDE));
        assertEquals(148, DiggerMenus.HEAD_RIGHT);
        assertEquals(DiggerMenus.PANEL_X + DiggerMenus.PANEL_W - DiggerMenus.HEAD_RIGHT, DiggerMenus.SLIDE);
        var screen = source("client/digger/DiggerScreen.java");
        assertTrue(screen.contains("UpgradeStrip.progress("));
        assertTrue(screen.contains("UpgradeStrip.slideOffset("));
        assertTrue(screen.contains("if (!open) setOpen(menu, false)"));
        assertTrue(screen.contains("else sendToServer(menu, true)"));
        assertTrue(screen.contains("a.open && p >= 1f"));
        assertTrue(source("mixin/AbstractContainerScreenMixin.java").contains("DiggerScreen.renderPanel"));
        assertTrue(source("compat/jei/JeiCompat.java").contains("DiggerScreen.open(screen)"));
    }

    @Test void placingAndTooltipAreGatedIndependentlyOfAutomation() throws Exception {
        var runtime = source("digger/DiggerRuntime.java");
        assertTrue(runtime.contains("boolean placer = DiggerEffects.placingEnabled(tile);"));
        assertFalse(runtime.contains("boolean placer = DiggerEffects.fillersUnlocked(tile);"));
        var auto = source("digger/DiggerRuntime.java");
        assertTrue(auto.contains("return DiggerEffects.placingEnabled(tile) && UpgradeSlots.get(tile).legal();"));
        assertTrue(source("digger/DiggerFillerSlot.java").contains("return DiggerEffects.fillersLocked(fillers.tile());"));
        var screen = source("client/digger/DiggerScreen.java");
        assertTrue(screen.contains("fillerRequirementLines(boolean missingAugment, boolean onlyMineOres)"));
        assertTrue(screen.contains("fillers.required\").withStyle(ChatFormatting.RED)"));
        assertTrue(screen.contains("fillers.need_ores_mode\").withStyle(ChatFormatting.RED)"));
        assertTrue(screen.contains("RenderTooltipEvent.Pre"));
        assertTrue(screen.contains("stackedTooltipOrigin("));
        assertTrue(screen.contains("pendingReq"));
    }

    @Test void strictChunkAreaIsWiredThroughScanNbtAndHighlight() throws Exception {
        var mixin = source("mixin/TileEntityVerticalDiggerMixin.java");
        assertTrue(mixin.contains("aamoreadditions_digger_scan_strict_chunk"));
        assertTrue(mixin.contains("aamoreadditions$work.scanStrictChunk"));
        var runtime = source("digger/DiggerRuntime.java");
        assertTrue(runtime.contains("restartForArea"));
        var highlight = source("client/digger/DiggerHighlightRenderer.java");
        assertTrue(highlight.contains("DiggerScan.relative"));
        assertTrue(highlight.contains("aamoreadditions$strictChunk()"));
        assertFalse(highlight.contains("Strict Chunk has no defined area yet"));
        assertTrue(highlight.contains("disableDepthTest()"));
        assertTrue(highlight.contains("depthMask(false)"));
        assertTrue(highlight.contains("enableDepthTest()"));
        assertFalse(highlight.contains("enableDepthTest();\n        RenderSystem.blendFuncSeparate"));
        var mixin2 = source("mixin/TileEntityVerticalDiggerMixin.java");
        assertTrue(mixin2.contains("aamoreadditions_digger_scan_y_span"));
        assertTrue(runtime.contains("mineColumn"));
        assertTrue(runtime.contains("DiggerEffects.verticalSpan"));
        assertTrue(source("client/UpgradeInfo.java").contains("info.digger.vertical_span"));
        assertTrue(source("client/UpgradeInfo.java").contains("scanPeriodValue("));
        assertTrue(source("client/UpgradeInfo.java").contains("info.speed_cap\").withStyle(ChatFormatting.GOLD)"));
    }
}
