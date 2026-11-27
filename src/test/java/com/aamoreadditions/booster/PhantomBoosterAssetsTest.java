package com.aamoreadditions.booster;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Validates the three tier models, ring textures and their .mcmeta files (JSON, references, alpha, animation length). */
class PhantomBoosterAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/aamoreadditions");
    private static final Gson GSON = new Gson();

    private static JsonObject json(Path p) throws Exception {
        return GSON.fromJson(Files.readString(p), JsonObject.class);
    }

    private static void check(String id, int tickSum, int frames, boolean dimensional) throws Exception {
        JsonObject model = json(ASSETS.resolve("models/block/" + id + ".json"));
        assertEquals("minecraft:cutout", model.get("render_type").getAsString());
        JsonObject textures = model.getAsJsonObject("textures");
        assertEquals("aamoreadditions:block/" + id + "_rings", textures.get("rings").getAsString());
        assertFalse(textures.has("shell"), "the shell is drawn by the block entity renderer, not the static model");
        JsonArray elements = model.getAsJsonArray("elements");
        assertEquals(7, elements.size(), "AA geometry only: pillar, four ring planes, cap, base");
        for (JsonElement e : elements) assertFalse(e.getAsJsonObject().has("name") && e.getAsJsonObject().get("name").getAsString().equals("energy_shell"));
        // every face texture variable resolves
        for (JsonElement e : elements) {
            for (var face : e.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                String ref = face.getValue().getAsJsonObject().get("texture").getAsString();
                assertTrue(ref.startsWith("#") && textures.has(ref.substring(1)), id + " " + ref);
            }
        }
        Path png = ASSETS.resolve("textures/block/" + id + "_rings.png");
        BufferedImage img = ImageIO.read(png.toFile());
        assertEquals(32, img.getWidth());
        assertEquals(32 * frames, img.getHeight());
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int a = img.getRGB(x, y) >>> 24;
                assertTrue(a == 0 || a == 255, id + " alpha at " + x + "," + y + " = " + a);
            }
        }
        JsonObject anim = json(Path.of(png + ".mcmeta")).getAsJsonObject("animation");
        assertTrue(anim.get("interpolate").getAsBoolean());
        assertEquals(32, anim.get("width").getAsInt());
        assertEquals(32, anim.get("height").getAsInt());
        int sum = 0;
        int frameTime = anim.get("frametime").getAsInt();
        for (JsonElement f : anim.getAsJsonArray("frames")) {
            JsonObject o = f.getAsJsonObject();
            assertTrue(o.get("index").getAsInt() < frames);
            sum += o.has("time") ? o.get("time").getAsInt() : frameTime;
        }
        assertEquals(tickSum, sum, id + " animation length in ticks");
        assertEquals(dimensional, id.startsWith("dimensional"));
    }

    @Test void advancedTransfiniteAndDimensionalAssetsAreValid() throws Exception {
        check("advanced_phantom_booster", 144, 44, false);        // 3 breaths + overlapped red sweeps (7.2 s)
        check("transfinite_phantom_booster", 104, 26, false);     // 2 breaths + whole-ring eye-of-ender climb (5.2 s)
        check("dimensional_phantom_booster", 20, 20, true);       // 1.0 s nether-star rings with ender-star blue climb
    }

    @Test void dimensionalUsesBlockEntityRendererForTheShellAndGlint() throws Exception {
        String type = Files.readString(Path.of("src/main/java/com/aamoreadditions/block/ModBlocks.java"));
        assertTrue(type.contains("DimensionalPhantomBoosterBlock::new"));
        assertTrue(type.contains("DIMENSIONAL_PHANTOM_BOOSTER_BE"));
        String reg = Files.readString(Path.of("src/main/java/com/aamoreadditions/client/render/ModBlockEntityRenderers.java"));
        assertTrue(reg.contains("Dist.CLIENT"));
        String block = Files.readString(Path.of("src/main/java/com/aamoreadditions/block/DimensionalPhantomBoosterBlock.java"));
        assertTrue(block.contains("extends PhantomBoosterBlock"));
        // Advanced and Transfinite stay purely resource based (plain PhantomBoosterBlock, no block entity).
        assertTrue(type.contains("\"advanced_phantom_booster\", PhantomBoosterBlock::new"));
        assertTrue(type.contains("\"transfinite_phantom_booster\", PhantomBoosterBlock::new"));
    }
}
