package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.BaseEnergyValues;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records AA's original capacity/maxReceive/maxExtract once, at construction, so upgrades never compound. */
@Mixin(value = EnergyStorage.class, remap = false)
public abstract class EnergyStorageBaseMixin implements BaseEnergyValues {
    @Unique private int aamoreadditions$baseCapacity;
    @Unique private int aamoreadditions$baseMaxReceive;
    @Unique private int aamoreadditions$baseMaxExtract;

    @Inject(method = "<init>(IIII)V", at = @At("TAIL"))
    private void aamoreadditions$captureBase(int capacity, int maxReceive, int maxExtract, int energy, CallbackInfo ci) {
        this.aamoreadditions$baseCapacity = capacity;
        this.aamoreadditions$baseMaxReceive = maxReceive;
        this.aamoreadditions$baseMaxExtract = maxExtract;
    }

    @Override public int aamoreadditions$baseCapacity() { return aamoreadditions$baseCapacity; }
    @Override public int aamoreadditions$baseMaxReceive() { return aamoreadditions$baseMaxReceive; }
    @Override public int aamoreadditions$baseMaxExtract() { return aamoreadditions$baseMaxExtract; }
}
