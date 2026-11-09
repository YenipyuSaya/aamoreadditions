package com.aamoreadditions.upgrade;

/** Minimal bridge for the AA scan counter; no farming algorithm is duplicated. */
public interface FarmerWorkState extends UpgradeResetState {
    void aamoreadditions$setFarmerWait(int ticks);
    com.aamoreadditions.effect.FarmerScanState aamoreadditions$farmerScan();
    void aamoreadditions$resetFarmerScan();
}
