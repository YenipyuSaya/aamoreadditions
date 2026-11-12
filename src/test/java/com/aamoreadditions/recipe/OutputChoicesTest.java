package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OutputChoicesTest {
    static final String STEEL = "c:ingots/steel", BRONZE = "c:ingots/bronze";
    static final List<String> STEELS = List.of("mekanism:ingot_steel", "oritech:steel_ingot");

    @Test void noItemsMeansNoneAndNotProducible() {
        var r = new OutputChoices().resolve(STEEL, List.of());
        assertEquals(OutputChoices.Kind.NONE, r.kind()); assertFalse(r.producible());
        assertFalse(OutputChoices.selectable(List.of()));
    }

    @Test void singleItemWorksWithoutPrompt() {
        var r = new OutputChoices().resolve(STEEL, List.of("oritech:steel_ingot"));
        assertEquals(OutputChoices.Kind.SINGLE, r.kind()); assertEquals("oritech:steel_ingot", r.item());
        assertFalse(OutputChoices.selectable(List.of("oritech:steel_ingot")));
        // fixed item without group
        assertEquals("create:andesite_alloy", new OutputChoices().resolve(null, List.of("create:andesite_alloy")).item());
    }

    @Test void severalItemsBlockUntilChosen() {
        var c = new OutputChoices();
        var r = c.resolve(STEEL, STEELS);
        assertEquals(OutputChoices.Kind.NEEDS_CHOICE, r.kind()); assertFalse(r.producible());
        assertTrue(OutputChoices.selectable(STEELS));
        assertTrue(c.choose(STEEL, "mekanism:ingot_steel", STEELS));
        r = c.resolve(STEEL, STEELS);
        assertEquals(OutputChoices.Kind.CHOSEN, r.kind()); assertEquals("mekanism:ingot_steel", r.item());
        assertTrue(OutputChoices.selectable(STEELS), "button stays usable after a choice");
    }

    @Test void sameTagReusedAcrossRecipesNewTagPromptsAgain() {
        var c = new OutputChoices();
        c.choose(STEEL, "mekanism:ingot_steel", STEELS);
        // a different recipe with the same output tag (IE steel / Oritech steel) -> same choice
        assertEquals("mekanism:ingot_steel", c.resolve(STEEL, STEELS).item());
        var bronzes = List.of("mekanism:ingot_bronze", "thermal:bronze_ingot");
        assertEquals(OutputChoices.Kind.NEEDS_CHOICE, c.resolve(BRONZE, bronzes).kind());
    }

    @Test void serverValidationRejectsItemsOutsideTheTag() {
        var c = new OutputChoices();
        assertFalse(c.choose(STEEL, "minecraft:diamond", STEELS));
        assertFalse(c.choose(STEEL, "mekanism:ingot_steel", List.of()));
        assertFalse(c.choose(null, "mekanism:ingot_steel", STEELS));
        assertFalse(c.choose("", "mekanism:ingot_steel", STEELS));
        assertFalse(c.choose(STEEL, null, STEELS));
        assertTrue(c.isEmpty());
    }

    @Test void changeIsReportedOnlyWhenDifferent() {
        var c = new OutputChoices();
        assertTrue(c.choose(STEEL, "mekanism:ingot_steel", STEELS));
        assertFalse(c.choose(STEEL, "mekanism:ingot_steel", STEELS), "same choice: no reset");
        assertTrue(c.choose(STEEL, "oritech:steel_ingot", STEELS), "changed: caller resets progress");
        assertEquals("oritech:steel_ingot", c.get(STEEL));
    }

    @Test void staleChoicePromptsAgain() {
        var c = new OutputChoices();
        c.choose(STEEL, "oritech:steel_ingot", STEELS);
        var now = List.of("mekanism:ingot_steel", "immersiveengineering:ingot_steel"); // oritech removed
        assertEquals(OutputChoices.Kind.NEEDS_CHOICE, c.resolve(STEEL, now).kind());
        assertEquals(OutputChoices.Kind.SINGLE, c.resolve(STEEL, List.of("mekanism:ingot_steel")).kind());
    }

    @Test void encodeDecodeRoundTrip() {
        var c = new OutputChoices();
        c.choose(STEEL, "mekanism:ingot_steel", STEELS);
        c.choose(BRONZE, "thermal:bronze_ingot", List.of("thermal:bronze_ingot", "mekanism:ingot_bronze"));
        Map<String, String> enc = c.encode();
        var d = new OutputChoices();
        d.decode(enc);
        assertEquals(c.view(), d.view());
        d.decode(null); assertTrue(d.isEmpty(), "old saves: no choices");
        d.decode(Map.of("", "x", "c:ingots/a", "")); assertTrue(d.isEmpty(), "bad entries dropped");
        enc.put("c:ingots/x", "y"); assertNull(c.get("c:ingots/x"), "encode returns a copy");
    }

    @Test void waitsWhileOtherItemOccupiesOutput() {
        // empty output: fits
        assertTrue(OutputChoices.fits(true, false, 0, 4, 64));
        // old chosen item still in the output -> wait (no mixing)
        assertFalse(OutputChoices.fits(false, false, 1, 4, 64));
        // same item with room / full
        assertTrue(OutputChoices.fits(false, true, 60, 4, 64));
        assertFalse(OutputChoices.fits(false, true, 61, 4, 64));
        assertFalse(OutputChoices.fits(true, false, 0, 65, 64));
    }

    @Test void buttonLook() {
        // several items, no choice: red "!", clickable (furnace waits)
        assertEquals(OutputChoices.Look.ALERT, OutputChoices.look(OutputChoices.Kind.NEEDS_CHOICE));
        assertTrue(OutputChoices.clickable(OutputChoices.Kind.NEEDS_CHOICE));
        // choice made: green check, clickable, independent of running state (no such input exists any more)
        assertEquals(OutputChoices.Look.CHECK, OutputChoices.look(OutputChoices.Kind.CHOSEN));
        assertTrue(OutputChoices.clickable(OutputChoices.Kind.CHOSEN));
        // single item / no items / no recipe: dark, not clickable
        for (var k : new OutputChoices.Kind[]{OutputChoices.Kind.SINGLE, OutputChoices.Kind.NONE, null}) {
            assertEquals(OutputChoices.Look.DARK, OutputChoices.look(k));
            assertFalse(OutputChoices.clickable(k));
        }
        // end to end with resolve()
        var c = new OutputChoices();
        assertEquals(OutputChoices.Look.ALERT, OutputChoices.look(c.resolve(STEEL, STEELS).kind()));
        c.choose(STEEL, "oritech:steel_ingot", STEELS);
        assertEquals(OutputChoices.Look.CHECK, OutputChoices.look(c.resolve(STEEL, STEELS).kind()));
        assertEquals(OutputChoices.Look.DARK, OutputChoices.look(c.resolve(STEEL, List.of("oritech:steel_ingot")).kind()));
    }

    @Test void materialTagForIdResults() {
        assertEquals("c:ingots/steel", OutputChoices.materialTag(List.of("c:ingots", "c:ingots/steel", "minecraft:beacon_payment_items")));
        assertEquals("c:ingots/dark_steel", OutputChoices.materialTag(List.of("c:dusts/x", "c:ingots/dark_steel")));
        assertEquals("c:gems/x", OutputChoices.materialTag(List.of("c:gems/x", "c:storage_blocks/x")));
        assertEquals("c:aa/b", OutputChoices.materialTag(List.of("c:zz/b", "c:aa/b")));
        assertNull(OutputChoices.materialTag(List.of("c:ingots", "forge:ingots/steel", "c:a/b/c", "c:ingots/")));
        assertNull(OutputChoices.materialTag(null));
    }
}
