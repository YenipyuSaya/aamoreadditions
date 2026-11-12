#!/usr/bin/env python3
"""Datagen-style generator for aamoreadditions:efficient_smelting recipes (Alloy Furnace, Efficient-Smelting mode).

Metal list = Immersive Engineering's RecipeMetals (exact list & hasOre flags) + titanium, iridium.
Fixed yields:  raw in 2 input slots -> 3 ingots | raw in all 3 input slots -> 5 | ore -> 2 | raw block -> 16 | dust -> 1.
Alloys / ore-less metals (IE hasOre=false): dust -> ingot only.
Conditions: neoforge:not tag_empty on the input tag AND the output ingot tag; vanilla metals (iron/gold/copper)
get none, except their dust recipes (vanilla has no dusts).
Optional "time" (ticks) is NOT written: default 80, raw blocks 8x (detected from the tag); KubeJS may set it.
Run from the repo root:  python3 tools/gen_efficient_smelting.py
"""
import json, os, shutil

# (name, hasOre) in IE RecipeMetals order, then ours
METALS = [
    ("iron", True), ("gold", True),                                              # vanilla
    ("copper", True), ("aluminum", True), ("lead", True), ("silver", True),
    ("nickel", True), ("uranium", True), ("constantan", False), ("electrum", False),
    ("steel", False),                                                            # IE
    ("tin", True), ("zinc", True), ("platinum", True), ("tungsten", True),
    ("osmium", True), ("cobalt", True), ("ardite", True), ("bronze", False),
    ("brass", False), ("invar", False), ("rose_gold", False), ("manyullyn", False),  # IE compat
    ("titanium", True), ("iridium", True),                                       # AAMA additions
]
VANILLA = {"iron", "gold", "copper"}
OUT = "src/main/resources/data/aamoreadditions/recipe/efficient_smelting"

def not_empty(tag):
    return {"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "tag": tag}}

def recipe(metal, in_tag, slots, count):
    """slots = number of input slots (1..3; input 3 = extra) that each hold one in_tag item."""
    r = {}
    if metal not in VANILLA or in_tag.startswith("c:dusts/"):
        r["neoforge:conditions"] = [not_empty(in_tag), not_empty(f"c:ingots/{metal}")]
    r["type"] = "aamoreadditions:efficient_smelting"
    r["ingredients"] = [{"tag": in_tag} for _ in range(slots)]
    r["result"] = {"tag": f"c:ingots/{metal}", "count": count}
    return r

def recipes_for(metal, has_ore):
    out = {}
    if has_ore:
        out["raw_x2"] = recipe(metal, f"c:raw_materials/{metal}", 2, 3)
        out["raw_x3"] = recipe(metal, f"c:raw_materials/{metal}", 3, 5)
        out["ore"] = recipe(metal, f"c:ores/{metal}", 1, 2)
        out["raw_block"] = recipe(metal, f"c:storage_blocks/raw_{metal}", 1, 16)
    out["dust"] = recipe(metal, f"c:dusts/{metal}", 1, 1)
    return out

def main():
    if os.path.isdir(OUT): shutil.rmtree(OUT)
    n = 0
    for metal, has_ore in METALS:
        d = os.path.join(OUT, metal); os.makedirs(d)
        for name, r in recipes_for(metal, has_ore).items():
            with open(os.path.join(d, name + ".json"), "w", encoding="utf-8") as f:
                json.dump(r, f, indent=2); f.write("\n")
            n += 1
    print(f"{n} recipes for {len(METALS)} metals -> {OUT}")

if __name__ == "__main__":
    main()
