package com.aamoreadditions.recipe;

import java.util.Collection;

/**
 * Category of an efficient-smelting input, derived from its common tags (Minecraft-free).
 * Priority when several groups are in the furnace: dust > raw block > ore > raw ore > other.
 * Raw-block recipes take 8x the base time.
 */
public enum EfficientKind {
    DUST(4, 1), RAW_BLOCK(3, 8), ORE(2, 1), RAW_ORE(1, 1), OTHER(0, 1);

    public final int priority, timeFactor;
    EfficientKind(int priority, int timeFactor) { this.priority = priority; this.timeFactor = timeFactor; }

    /** From tag ids like "c:dusts/iron" (any namespace); the highest-priority match wins. */
    public static EfficientKind ofTags(Collection<String> tagIds) {
        EfficientKind best = OTHER;
        for (String t : tagIds) {
            String p = t.contains(":") ? t.substring(t.indexOf(':') + 1) : t;
            EfficientKind k = p.startsWith("dusts/") ? DUST
                    : p.startsWith("storage_blocks/raw_") ? RAW_BLOCK
                    : p.startsWith("ores/") ? ORE
                    : p.startsWith("raw_materials/") ? RAW_ORE : OTHER;
            if (k.priority > best.priority) best = k;
        }
        return best;
    }
}
