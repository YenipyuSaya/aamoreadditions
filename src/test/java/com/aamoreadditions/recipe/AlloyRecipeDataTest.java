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
    static final Set<String> MODS = Set.of("create", "immersiveengineering", "mekanism", "oritech");

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
                String id = (String) (ing.containsKey("tag") ? ing.get("tag") : ing.get("item"));
                assertNotNull(id, n); assertTrue(ID.matcher(id).matches(), n + " bad id " + id);
                assertTrue(((Number) ing.get("count")).intValue() >= 1, n);
            }
            Map<String, Object> res = (Map<String, Object>) j.get("result");
            String out = (String) res.get("id");
            assertTrue(ID.matcher(out).matches(), n);
            assertEquals(mod, out.substring(0, out.indexOf(':')), n + " output must come from its own mod");
            int cnt = ((Number) res.get("count")).intValue();
            assertTrue(cnt >= 1 && cnt <= 64, n);
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
        String str() { i++; StringBuilder b = new StringBuilder(); while (s.charAt(i) != '"') { if (s.charAt(i) == '\\') i++; b.append(s.charAt(i++)); } i++; return b.toString(); }
        void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    }
}
