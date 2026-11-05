package com.aamoreadditions.upgrade;

/** Upgrade holder that isn't a block entity (Empowerer structure): 3 slots, Speed upgrades only. */
public interface SpeedOnlyHolder {
    int UPGRADE_SLOTS = 3;
    /** Called when the upgrade slots change (server: mark saved data dirty). */
    default void upgradesChanged() {}
}
