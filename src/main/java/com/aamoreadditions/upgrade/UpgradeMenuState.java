package com.aamoreadditions.upgrade;

/** Duck interface mixed into AbstractContainerMenu: whether the upgrade panel is open. */
public interface UpgradeMenuState {
    /** Button ids sent via the vanilla ServerboundContainerButtonClickPacket. */
    int BUTTON_CLOSE = 7700, BUTTON_OPEN = 7701;

    boolean aamoreadditions$isUpgradeOpen();

    void aamoreadditions$setUpgradeOpen(boolean open);
}
