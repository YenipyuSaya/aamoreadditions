package com.aamoreadditions.recipe;

/** Pure rules for automated (pipe/hopper/IItemHandler) access to the Alloy Furnace Extra slot. */
public final class ExtraSlotRules {
    private ExtraSlotRules() {}
    /** Automation may insert any item unless the recipe lock (recorded) forbids it in that slot. */
    public static boolean canAutoInsert(boolean lockAllows) { return lockAllows; }
    /** Automation can never pull from the Extra slot. */
    public static boolean canAutoExtract() { return false; }
}
