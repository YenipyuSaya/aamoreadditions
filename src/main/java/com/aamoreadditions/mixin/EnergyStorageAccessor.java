package com.aamoreadditions.mixin;

import net.neoforged.neoforge.energy.EnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EnergyStorage.class, remap = false)
public interface EnergyStorageAccessor {
    @Accessor("capacity")
    void aamoreadditions$setCapacity(int capacity);

    @Accessor("energy")
    int aamoreadditions$getEnergy();

    @Accessor("energy")
    void aamoreadditions$setEnergy(int energy);

    @Accessor("maxReceive")
    int aamoreadditions$getMaxReceive();

    @Accessor("maxReceive")
    void aamoreadditions$setMaxReceive(int maxReceive);

    @Accessor("maxExtract")
    int aamoreadditions$getMaxExtract();

    @Accessor("maxExtract")
    void aamoreadditions$setMaxExtract(int maxExtract);
}
