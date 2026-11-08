package com.aamoreadditions.upgrade;

/** Upgrade holder that isn't a block entity (Empowerer structure): configurable slots, Speed upgrades only. */
public interface SpeedOnlyHolder {
    /** Called when the upgrade slots change (server: mark saved data dirty). */
    default void upgradesChanged() {}
}
