package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Source checks for the same-tag output wiring (MC classes need a game bootstrap). */
class AlloyOutputWiringTest {
    static String src(String p) throws Exception { return Files.readString(Path.of("src/main/java/com/aamoreadditions/" + p)); }

    @Test void recipeAcceptsIdOrTagResult() throws Exception {
        var r = src("recipe/AlloyResult.java");
        assertTrue(r.contains("Codec.either(ItemStack.STRICT_CODEC, TAG_FORM)"));
        assertTrue(r.contains("TagKey.codec(Registries.ITEM).fieldOf(\"tag\")"));
        assertTrue(r.contains("optionalFieldOf(\"count\", 1)"));
        var a = src("recipe/AlloyingRecipe.java");
        assertTrue(a.contains("AlloyResult.CODEC.fieldOf(\"result\")"));
        assertTrue(a.contains("AlloyResult.STREAM_CODEC, AlloyingRecipe::result"));
    }

    @Test void unavailableOutputsAreHidden() throws Exception {
        var m = src("recipe/ModRecipes.java");
        assertTrue(m.contains("if (!h.value().result().available()) continue;"));
        assertTrue(src("compat/jei/JeiCompat.java").contains("reg.addRecipes(ALLOYING, ModRecipes.available(level))"));
    }

    @Test void furnaceProducesOnlyTheResolvedItemAndBlocksWithoutChoice() throws Exception {
        var be = src("block/AlloyFurnaceBlockEntity.java");
        assertTrue(be.contains("match.recipe().value().result().produce(outputChoices)"));
        assertTrue(be.contains("boolean working = match != null && !result.isEmpty() && outputFits(result);"));
        assertTrue(be.contains("if (!working) firstSmeltTime = 0;"));
        assertTrue(be.contains("OutputChoices.fits(out.isEmpty(), ItemStack.isSameItemSameComponents(out, result)"));
    }

    @Test void choiceIsValidatedResetsProgressAndSyncs() throws Exception {
        var be = src("block/AlloyFurnaceBlockEntity.java");
        int i = be.indexOf("public boolean chooseOutput(");
        String body = be.substring(i, be.indexOf("}\n", be.indexOf("return true;", i)));
        assertTrue(body.contains("outputChoices.choose(tag, item, com.aamoreadditions.recipe.AlloyResult.itemsInTag(tag))"));
        assertTrue(body.contains("firstSmeltTime = 0;"));
        assertTrue(body.contains("sendUpdate();"));
        assertTrue(be.contains("compound.put(\"AAMAOutputChoice\", oc);"));
        assertTrue(be.contains("outputChoices.decode(m);"));
        var net = src("network/ModNetwork.java");
        assertTrue(net.contains("player.containerMenu instanceof com.aamoreadditions.menu.AlloyFurnaceMenu m && m.stillValid(player)"));
        assertTrue(net.contains("m.furnace.chooseOutput(tag, item)"));
        assertTrue(net.contains("playToServer(AlloyOutputChoicePayload.TYPE"));
    }

    @Test void overlayWinsLeftClickAndJeiSeesItems() throws Exception {
        var in = src("client/screen/AlloyOverlayInput.java");
        assertTrue(in.contains("@SubscribeEvent(priority = EventPriority.HIGHEST)"));
        assertTrue(in.contains("ScreenEvent.MouseButtonPressed.Pre"));
        assertTrue(in.contains("event.setCanceled(true)"));
        var jei = src("compat/jei/JeiCompat.java");
        assertTrue(jei.contains("addGuiContainerHandler(AlloyFurnaceScreen.class"));
        assertTrue(jei.contains("getClickableIngredientUnderMouse"));
        var sc = src("client/screen/AlloyFurnaceScreen.java");
        assertTrue(sc.contains("SEL_X = 132, SEL_Y = 72, SEL_SIZE = 10"));
        assertTrue(sc.contains("PICK_X = 127, PICK_Y = 43, PICK_STEP = 18"));
        assertTrue(sc.contains("key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE"));
        assertTrue(sc.contains("g.blitSprite(SPRITES.get(this.active, isHoveredOrFocused()), getX(), getY(), LOCK_SIZE, LOCK_SIZE);"));
        assertTrue(sc.contains("LOCK_X = -1, LOCK_Y = 4, LOCK_SIZE = 18"));
        assertTrue(sc.contains("new LockButton(this.getGuiLeft() + LOCK_X, this.topPos + LOCK_Y)"));
        assertTrue(sc.contains("int o = (LOCK_SIZE - 16) / 2;"));
        assertTrue(sc.contains("g.blit(TEX_SELECT, getX(), getY(), u, 0, SEL_SIZE, SEL_SIZE, 50, 10);"));
    }

    @Test void noProcessingFlagAndButtonFollowsChoiceState() throws Exception {
        var be = src("block/AlloyFurnaceBlockEntity.java");
        assertFalse(be.contains("AAMAProcessing")); assertFalse(be.contains("public boolean processing"));
        var sc = src("client/screen/AlloyFurnaceScreen.java");
        assertFalse(sc.contains("processing"));
        assertTrue(sc.contains("int u = selectU(cur == null ? null : cur.res().kind(), this.active && isHovered());"));
        assertTrue(sc.contains("selectable = cur != null && com.aamoreadditions.recipe.OutputChoices.clickable(cur.res().kind())"));
        assertTrue(sc.contains("this.buttonSelect.active = selectable;"));
        assertTrue(sc.contains("gui.aamoreadditions.alloyfurnace.same_tag.chosen"));
    }

    @Test void selectTextureHasFiveFramesAndUnlockIconIsClean() throws Exception {
        var sel = javax.imageio.ImageIO.read(Path.of("src/main/resources/assets/aamoreadditions/textures/gui/alloy_select.png").toFile());
        assertEquals(50, sel.getWidth()); assertEquals(10, sel.getHeight());
        // check frame (u=30): green mark pixel at (2,4), none at the "!" bar position (4,2)
        int g = sel.getRGB(30 + 2, 4);
        assertTrue(((g >> 8) & 255) > ((g >> 16) & 255) + 80, "green check pixel");
        // unlocked icon: the two stray dark pixels inside the open shackle (6,3) and (9,3) are transparent now,
        // matching the locked icon (no dark pixels on the inner side of the shackle)
        var un = javax.imageio.ImageIO.read(Path.of("src/main/resources/assets/aamoreadditions/textures/gui/recipe_unlock.png").toFile());
        var lk = javax.imageio.ImageIO.read(Path.of("src/main/resources/assets/aamoreadditions/textures/gui/recipe_lock.png").toFile());
        for (int x : new int[]{6, 9}) assertEquals(0, (un.getRGB(x, 3) >>> 24), "unlock (" + x + ",3) must be transparent");
        for (int x : new int[]{6, 7, 8}) assertEquals(0, (lk.getRGB(x, 3) >>> 24), "lock (" + x + ",3) unchanged transparent");
        // the keyhole-side pixels stay as in the original art
        for (var im : new java.awt.image.BufferedImage[]{un, lk}) {
            assertEquals(0xFFC1B59D, im.getRGB(6, 11)); assertEquals(0xFFCCBF9D, im.getRGB(8, 11));
        }
    }

    @Test void langHasSameTagTooltip() throws Exception {
        var en = Files.readString(Path.of("src/main/resources/assets/aamoreadditions/lang/en_us.json"));
        var zh = Files.readString(Path.of("src/main/resources/assets/aamoreadditions/lang/zh_cn.json"));
        assertTrue(en.contains("\"gui.aamoreadditions.alloyfurnace.same_tag.1\": \"Same-Tag items detected.\""));
        assertTrue(en.contains("\"gui.aamoreadditions.alloyfurnace.same_tag.2\": \"Press the button to select one.\""));
        assertTrue(zh.contains("\"gui.aamoreadditions.alloyfurnace.same_tag.1\": \"检测到相同标签的物品。\""));
        assertTrue(zh.contains("\"gui.aamoreadditions.alloyfurnace.same_tag.2\": \"按下以选择一个。\""));
        assertTrue(en.contains("\"gui.aamoreadditions.alloyfurnace.same_tag.chosen\": \"Selected: %s\""));
        assertTrue(en.contains("\"gui.aamoreadditions.alloyfurnace.same_tag.change\": \"Press the button to change it.\""));
        assertTrue(zh.contains("\"gui.aamoreadditions.alloyfurnace.same_tag.chosen\": \"已选择：%s\""));
        assertTrue(zh.contains("\"gui.aamoreadditions.alloyfurnace.same_tag.change\": \"按下以更改选择。\""));
        assertTrue(Files.exists(Path.of("src/main/resources/assets/aamoreadditions/textures/gui/alloy_select.png")));
    }
}
