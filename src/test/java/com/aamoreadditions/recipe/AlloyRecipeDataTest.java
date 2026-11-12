package com.aamoreadditions.recipe;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Structural checks of every alloying recipe JSON (same fields/limits the codec enforces; MC codecs need a game bootstrap). */
class AlloyRecipeDataTest {
    static final Path DIR = Paths.get("src/main/resources/data/aamoreadditions/recipe/alloying");
    static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    static final Set<String> MODS = Set.of("create", "immersiveengineering", "mekanism", "oritech", "enderio", "thermal");

    @SuppressWarnings("unchecked")
    @Test void allRecipesValid() throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(DIR)) { files = s.filter(p -> p.toString().endsWith(".json")).toList(); }
        assertFalse(files.isEmpty());
        for (Path f : files) {
            Map<String, Object> j = (Map<String, Object>) new Json(Files.readString(f)).value();
            String n = f.toString();
            assertEquals("aamoreadditions:alloying", j.get("type"), n);
            List<Object> cond = (List<Object>) j.get("neoforge:conditions");
            String folder = f.getParent().getFileName().toString();
            String mod;
            if (folder.equals("minecraft")) {
                // vanilla recipe: always active, no load conditions
                assertNull(cond, n + " vanilla recipe must have no conditions");
                mod = "minecraft";
            } else {
                assertNotNull(cond, n + " missing neoforge:conditions");
                Map<String, Object> c0 = (Map<String, Object>) cond.get(0);
                assertEquals("neoforge:mod_loaded", c0.get("type"), n);
                mod = (String) c0.get("modid");
                assertTrue(MODS.contains(mod), n);
                assertEquals(mod, folder, n + " folder != modid");
            }
            List<Object> ings = (List<Object>) j.get("ingredients");
            assertTrue(ings.size() >= 1 && ings.size() <= 3, n);
            for (Object o : ings) {
                Map<String, Object> ing = (Map<String, Object>) o;
                if ("neoforge:compound".equals(ing.get("type"))) {
                    List<Object> ch = (List<Object>) ing.get("children");
                    assertTrue(ch != null && ch.size() >= 2, n);
                    for (Object co : ch) {
                        Map<String, Object> c = (Map<String, Object>) co;
                        String cid = (String) (c.containsKey("tag") ? c.get("tag") : c.get("item"));
                        assertNotNull(cid, n); assertTrue(ID.matcher(cid).matches(), n + " bad id " + cid);
                    }
                } else {
                    assertNull(ing.get("type"), n);
                    String id = (String) (ing.containsKey("tag") ? ing.get("tag") : ing.get("item"));
                    assertNotNull(id, n); assertTrue(ID.matcher(id).matches(), n + " bad id " + id);
                }
                assertTrue(((Number) ing.get("count")).intValue() >= 1, n);
            }
            Map<String, Object> res = (Map<String, Object>) j.get("result");
            assertTrue(res.containsKey("id") ^ res.containsKey("tag"), n + " result needs exactly one of id / tag");
            if (res.containsKey("id")) {
                String out = (String) res.get("id");
                assertTrue(ID.matcher(out).matches(), n);
                assertEquals(mod, out.substring(0, out.indexOf(':')), n + " output must come from its own mod");
            } else {
                String tag = (String) res.get("tag");
                assertTrue(tag.matches("c:[a-z_]+/[a-z0-9_]+"), n + " tag result must be a c:<kind>/<material> tag: " + tag);
            }
            int cnt = ((Number) res.get("count")).intValue();
            assertTrue(cnt >= 1 && cnt <= 64, n);
        }
    }

    /** Exact contents of the Ender IO / Thermal compat recipes (ratios from the 1.21.1 EnderIO / 1.20.1 Thermal sources). */
    @SuppressWarnings("unchecked")
    @Test void compatRecipesMatchSources() throws IOException {
        Map<String, String> expect = new LinkedHashMap<>();
        expect.put("enderio/conductive_alloy", "c:ingots/iron*1,c:ingots/copper*1->enderio:conductive_alloy_ingot*2");
        expect.put("enderio/dark_steel", "c:ingots/iron*1,c:dusts/coal*1,c:obsidians*1->enderio:dark_steel_ingot*2");
        expect.put("enderio/end_steel", "c:end_stones*1,c:ingots/dark_steel*1,c:obsidians*1->enderio:end_steel_ingot*2");
        expect.put("enderio/energetic_alloy", "c:dusts/redstone*1,c:ingots/conductive_alloy*1,c:ingots/gold*1->enderio:energetic_alloy_ingot*2");
        expect.put("enderio/pulsating_alloy", "c:ingots/iron*1,c:ender_pearls*1->enderio:pulsating_alloy_ingot*2");
        expect.put("enderio/redstone_alloy", "c:dusts/redstone*1,c:ingots/copper*1->enderio:redstone_alloy_ingot*1");
        expect.put("enderio/soularium", "[minecraft:soul_sand|minecraft:soul_soil]*1,c:ingots/gold*1->enderio:soularium_ingot*1");
        expect.put("enderio/vibrant_alloy", "c:ingots/energetic_alloy*1,c:ender_pearls*1,c:dusts/glowstone*1->enderio:vibrant_alloy_ingot*2");
        expect.put("thermal/signalum", "[c:ingots/copper|c:dusts/copper]*3,[c:ingots/silver|c:dusts/silver]*1,c:dusts/redstone*4->#c:ingots/signalum*4");
        expect.put("thermal/lumium", "[c:ingots/tin|c:dusts/tin]*3,[c:ingots/silver|c:dusts/silver]*1,c:dusts/glowstone*2->#c:ingots/lumium*4");
        expect.put("thermal/enderium", "[c:ingots/lead|c:dusts/lead]*3,c:dusts/diamond*1,[c:ender_pearls|c:dusts/ender_pearl]*2->#c:ingots/enderium*2");
        for (var e : expect.entrySet()) {
            Path f = DIR.resolve(e.getKey() + ".json");
            Map<String, Object> j = (Map<String, Object>) new Json(Files.readString(f)).value();
            List<String> parts = new ArrayList<>();
            for (Object o : (List<Object>) j.get("ingredients")) {
                Map<String, Object> ing = (Map<String, Object>) o;
                String v;
                if (ing.containsKey("children")) {
                    List<String> ch = new ArrayList<>();
                    for (Object co : (List<Object>) ing.get("children")) { var c = (Map<String, Object>) co; ch.add((String) (c.containsKey("tag") ? c.get("tag") : c.get("item"))); }
                    v = "[" + String.join("|", ch) + "]";
                } else v = (String) (ing.containsKey("tag") ? ing.get("tag") : ing.get("item"));
                parts.add(v + "*" + ((Number) ing.get("count")).intValue());
            }
            Map<String, Object> res = (Map<String, Object>) j.get("result");
            String out = res.containsKey("tag") ? "#" + res.get("tag") : (String) res.get("id");
            assertEquals(e.getValue(), String.join(",", parts) + "->" + out + "*" + ((Number) res.get("count")).intValue(), f.toString());
            String mod = e.getKey().substring(0, e.getKey().indexOf('/'));
            assertEquals(mod, ((Map<String, Object>) ((List<Object>) j.get("neoforge:conditions")).get(0)).get("modid"));
        }
    }

    /** No two recipes may take the exact same input set (they would produce different outputs for the same slots). */
    @SuppressWarnings("unchecked")
    @Test void noDuplicateInputSets() throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.walk(DIR)) { files = s.filter(p -> p.toString().endsWith(".json")).sorted().toList(); }
        Map<String, Path> seen = new HashMap<>();
        for (Path f : files) {
            Map<String, Object> j = (Map<String, Object>) new Json(Files.readString(f)).value();
            List<String> keys = new ArrayList<>();
            for (Object o : (List<Object>) j.get("ingredients")) keys.add(o.toString());
            Collections.sort(keys);
            Path prev = seen.put(String.join(";", keys), f);
            assertNull(prev, f + " has the same inputs as " + prev);
        }
    }

    /** Minimal JSON reader for the test. */
    static final class Json {
        final String s; int i;
        Json(String s) { this.s = s; }
        Object value() {
            ws(); char c = s.charAt(i);
            if (c == '{') { i++; Map<String, Object> m = new LinkedHashMap<>(); ws(); if (s.charAt(i) == '}') { i++; return m; }
                while (true) { ws(); String k = str(); ws(); i++; m.put(k, value()); ws(); if (s.charAt(i++) == '}') return m; } }
            if (c == '[') { i++; List<Object> l = new ArrayList<>(); ws(); if (s.charAt(i) == ']') { i++; return l; }
                while (true) { l.add(value()); ws(); if (s.charAt(i++) == ']') return l; } }
            if (c == '"') return str();
            if (s.startsWith("true", i)) { i += 4; return true; }
            if (s.startsWith("false", i)) { i += 5; return false; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int st = i; while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(st, i));
        }
        String str() {
            i++; StringBuilder b = new StringBuilder();
            while (s.charAt(i) != '"') {
                char c = s.charAt(i++);
                if (c == '\\') {
                    c = s.charAt(i++);
                    switch (c) {
                        case 'n' -> c = '\n'; case 'r' -> c = '\r'; case 't' -> c = '\t';
                        case 'b' -> c = '\b'; case 'f' -> c = '\f';
                        case 'u' -> { c = (char) Integer.parseInt(s.substring(i, i + 4), 16); i += 4; }
                        default -> { }
                    }
                }
                b.append(c);
            }
            i++; return b.toString();
        }
        void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    }
}
