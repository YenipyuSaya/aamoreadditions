package com.aamoreadditions.booster;

import com.aamoreadditions.config.EnglishConfigNames;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PhantomBoosterWiringTest {
    private static final Path ROOT = Path.of("src/main");
    private static final String[] IDS = {
            "advanced_phantom_booster", "transfinite_phantom_booster", "dimensional_phantom_booster"};

    private static String read(String rel) throws Exception {
        return Files.readString(ROOT.resolve(rel));
    }

    private static Map<String, Object> json(String rel) throws Exception {
        return new Gson().fromJson(read(rel), new TypeToken<Map<String, Object>>(){}.getType());
    }

    @Test void blocksItemsAndTabAreRegistered() throws Exception {
        var blocks = read("java/com/aamoreadditions/block/ModBlocks.java");
        var tab = read("java/com/aamoreadditions/AAMoreAdditions.java");
        for (String id : IDS) {
            assertTrue(blocks.contains("\"" + id + "\""), id);
            assertTrue(tab.contains(id.toUpperCase() + "_ITEM") || tab.contains(snakeToConst(id) + "_ITEM"), id + " in tab");
        }
        assertTrue(tab.contains("ADVANCED_PHANTOM_BOOSTER_ITEM"));
        assertTrue(tab.contains("TRANSFINITE_PHANTOM_BOOSTER_ITEM"));
        assertTrue(tab.contains("DIMENSIONAL_PHANTOM_BOOSTER_ITEM"));
        var props = read("java/com/aamoreadditions/block/PhantomBoosterBlock.java");
        assertTrue(props.contains("strength(1.5F, 10.0F)") || props.contains("strength(1.5f, 10.0f)") || props.contains("strength(1.5F, 10.0F)"));
        assertTrue(props.contains("SoundType.STONE"));
        assertTrue(props.contains("Block.box(5, 0, 5, 11, 16, 11)"));
    }

    private static String snakeToConst(String id) { return id.toUpperCase(); }

    @Test void modelsBlockstatesLootAndPickaxeTagReferenceAaArtAndDropSelf() throws Exception {
        for (String id : IDS) {
            var state = json("resources/assets/aamoreadditions/blockstates/" + id + ".json");
            var variants = (Map<String, Object>) state.get("variants");
            var model = (Map<String, Object>) variants.get("");
            assertEquals("aamoreadditions:block/" + id, model.get("model"));
            var item = json("resources/assets/aamoreadditions/models/item/" + id + ".json");
            assertEquals("aamoreadditions:block/" + id, item.get("parent"));
            // The art (pillar/base/cap) is still AA's: the models reference its texture instead of copying it.
            var block = json("resources/assets/aamoreadditions/models/block/" + id + ".json");
            assertEquals("actuallyadditions:block/models/model_phantom_booster", ((Map<?, ?>) block.get("textures")).get("base"));
            var loot = read("resources/data/aamoreadditions/loot_table/blocks/" + id + ".json");
            assertTrue(loot.contains("aamoreadditions:" + id));
            assertTrue(loot.contains("minecraft:survives_explosion"));
        }
        var tag = read("resources/data/minecraft/tags/block/mineable/pickaxe.json");
        for (String id : IDS) assertTrue(tag.contains("aamoreadditions:" + id), id);
    }

    @Test void recipesMatchTheConfirmedLayoutsAndVerifiedItemIds() throws Exception {
        var advanced = json("resources/data/aamoreadditions/recipe/advanced_phantom_booster.json");
        assertEquals(List.of("RER", "EBE", "RER"), advanced.get("pattern"));
        var aKey = (Map<String, Map<String, String>>) advanced.get("key");
        assertEquals("actuallyadditions:phantom_booster", aKey.get("B").get("item"));
        assertEquals("actuallyadditions:empowered_enori_crystal", aKey.get("E").get("item")); // 充能铁晶
        assertEquals("actuallyadditions:empowered_restonia_crystal", aKey.get("R").get("item"));
        assertEquals("aamoreadditions:advanced_phantom_booster", ((Map<?, ?>) advanced.get("result")).get("id"));
        assertEquals(1, ((Number) ((Map<?, ?>) advanced.get("result")).get("count")).intValue());

        var trans = json("resources/data/aamoreadditions/recipe/transfinite_phantom_booster.json");
        assertEquals(List.of("YDY", "DAD", "YDY"), trans.get("pattern"));
        var tKey = (Map<String, Map<String, String>>) trans.get("key");
        assertEquals("aamoreadditions:advanced_phantom_booster", tKey.get("A").get("item"));
        assertEquals("actuallyadditions:empowered_diamatine_crystal_block", tKey.get("D").get("item"));
        assertEquals("minecraft:ender_eye", tKey.get("Y").get("item"));
        assertEquals(1, ((Number) ((Map<?, ?>) trans.get("result")).get("count")).intValue());

        var dim = json("resources/data/aamoreadditions/recipe/dimensional_phantom_booster.json");
        assertEquals(List.of("ETE", "TNT", "ETE"), dim.get("pattern"));
        var dKey = (Map<String, Map<String, String>>) dim.get("key");
        assertEquals("actuallyadditions:ender_star", dKey.get("N").get("item")); // 末影之星 (AA item, not minecraft:nether_star)
        assertEquals("aamoreadditions:transfinite_phantom_booster", dKey.get("T").get("item"));
        assertEquals("actuallyadditions:empowered_emeradic_crystal_block", dKey.get("E").get("item"));
        assertEquals("aamoreadditions:dimensional_phantom_booster", ((Map<?, ?>) dim.get("result")).get("id"));
        assertEquals(2, ((Number) ((Map<?, ?>) dim.get("result")).get("count")).intValue());
    }

    @Test void langNamesMatchTheRequestedEnglishAndChineseStyle() throws Exception {
        var en = json("resources/assets/aamoreadditions/lang/en_us.json");
        var zh = json("resources/assets/aamoreadditions/lang/zh_cn.json");
        assertEquals("Advanced Phantom Booster", en.get("block.aamoreadditions.advanced_phantom_booster"));
        assertEquals("Transfinite Phantom Booster", en.get("block.aamoreadditions.transfinite_phantom_booster"));
        assertEquals("Dimensional Phantom Booster", en.get("block.aamoreadditions.dimensional_phantom_booster"));
        assertEquals("高级幻灵增幅器", zh.get("block.aamoreadditions.advanced_phantom_booster"));
        assertEquals("超限幻灵增幅器", zh.get("block.aamoreadditions.transfinite_phantom_booster"));
        assertEquals("次元幻灵增幅器", zh.get("block.aamoreadditions.dimensional_phantom_booster"));
    }

    @Test void mixinsTakeOverRangeBindingAndTheDiggerTreatsSpecialsAsAdvanced() throws Exception {
        var json = read("resources/aamoreadditions.mixins.json");
        for (String m : new String[]{
                "TileEntityPhantomfaceRangeMixin", "TileEntityPhantomPlacerBoundMixin",
                "TileEntityPlayerInterfaceRangeMixin", "ItemPhantomConnectorMixin"}) {
            assertTrue(json.contains("\"" + m + "\""), m);
        }
        var range = read("java/com/aamoreadditions/mixin/TileEntityPhantomfaceRangeMixin.java");
        assertTrue(range.contains("BoosterEffects.upgradeRange"));
        assertTrue(range.contains("isBoundInRange"));
        var player = read("java/com/aamoreadditions/mixin/TileEntityPlayerInterfaceRangeMixin.java");
        assertTrue(player.contains("range * (double) this.range") || player.contains("(double) this.range * (double) this.range"));
        var effects = read("java/com/aamoreadditions/booster/BoosterEffects.java");
        assertTrue(effects.contains("Machine.DIGGER"));
        assertTrue(effects.contains("specialsAsAdvanced"));
        assertTrue(read("java/com/aamoreadditions/booster/BoosterScan.java").contains("specialsAsAdvanced"));
        var chunk = read("java/com/aamoreadditions/mixin/LevelChunkSetBlockStateMixin.java");
        assertTrue(chunk.contains("BoosterEffects.onBlockChanged"));
        var digger = read("java/com/aamoreadditions/digger/DiggerRuntime.java");
        assertTrue(digger.contains("TileEntityPhantomface.upgradeRange"));
        assertFalse(digger.contains("BoosterEffects."), "digger keeps using AA upgradeRange, which the mixin takes over");
    }

    @Test void configLabelsStayEnglishDisplayNames() {
        assertEquals("Max Stack Count", EnglishConfigNames.label("max_stack_count"));
        assertEquals("AA Range Multiplier", EnglishConfigNames.label("aa_range_multiplier"));
        assertEquals("Advanced Range Multiplier", EnglishConfigNames.label("advanced_range_multiplier"));
    }

    @Test void overloadDamageTypeLangAndTagsAreWiredAndRealTntIsGone() throws Exception {
        var type = json("resources/data/aamoreadditions/damage_type/phantom_booster_overload.json");
        assertEquals("phantom_booster_overload", type.get("message_id"));
        assertEquals(0.0, ((Number) type.get("exhaustion")).doubleValue());
        var en = json("resources/assets/aamoreadditions/lang/en_us.json");
        var zh = json("resources/assets/aamoreadditions/lang/zh_cn.json");
        assertEquals("%s tried to overload the Phantom Booster", en.get("death.attack.phantom_booster_overload"));
        assertEquals("%s试图超载幻灵增幅器", zh.get("death.attack.phantom_booster_overload"));
        for (String tag : new String[]{"bypasses_armor", "bypasses_resistance", "bypasses_enchantments", "no_knockback"}) {
            var body = read("resources/data/minecraft/tags/damage_type/" + tag + ".json");
            assertTrue(body.contains("aamoreadditions:phantom_booster_overload"), tag);
        }
        var effects = read("java/com/aamoreadditions/booster/BoosterEffects.java");
        assertFalse(effects.contains("ExplosionInteraction.TNT"));
        assertFalse(effects.contains("level.explode"));
        assertTrue(effects.contains("EXPLOSION_EMITTER"));
        assertTrue(effects.contains("BoosterOverload.SOUND_VOLUME"));
        assertTrue(effects.contains("LivingEntity"));
        assertTrue(effects.contains("dropBoostersAbove"));
        assertTrue(effects.contains("destroyBlock"));
        var config = read("java/com/aamoreadditions/effect/UpgradeConfig.java");
        assertFalse(config.contains("overload_explosion_power"));
        assertTrue(config.contains("Warning: Excessively high value may cause performance issues."));
        // Bound dimension comes from the stored (clicked) block's world, not the player.
        var connector = read("java/com/aamoreadditions/mixin/ItemPhantomConnectorMixin.java");
        assertTrue(connector.contains("ItemPhantomConnector.getStoredWorld"));
        assertTrue(connector.contains("BoundWorlds.set"));
    }
}
