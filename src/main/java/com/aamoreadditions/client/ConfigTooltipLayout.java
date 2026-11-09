package com.aamoreadditions.client;
/** Pure width policy for this mod's native fallback tooltip, never a global setting. */
public final class ConfigTooltipLayout {
    private ConfigTooltipLayout() {}
    public static int width(int screenWidth,int titleWidth) {
        return Math.max(1,Math.min(Math.max(1,screenWidth-16),Math.max(170,titleWidth)));
    }
}
