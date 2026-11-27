package com.aamoreadditions.digger;

/** Duck interface mixed into AA's ContainerMiner: whether the fillers panel is open (client and server copy). */
public interface DiggerMenuState {
    boolean aamoreadditions$fillersOpen();
    void aamoreadditions$setFillersOpen(boolean open);
}
