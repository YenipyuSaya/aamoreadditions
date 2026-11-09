package com.aamoreadditions.effect;

/** Shared Oil/Coal pause: any positive space admits a full burn tick; native FE storage clips overflow. */
public final class OilBurnPause {
    private OilBurnPause() {}
    public static boolean mayAdvance(int stored,int capacity){return stored<capacity;}
}
