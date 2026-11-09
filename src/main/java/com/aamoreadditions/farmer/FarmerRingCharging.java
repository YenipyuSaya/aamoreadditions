package com.aamoreadditions.farmer;

import de.ellpeck.actuallyadditions.mod.components.ActuallyComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.energy.ComponentEnergyStorage;

/** Local Farmer charge adapter, sharing AA's persisted component without changing
 * its globally registered 2000-FE capability or charging behavior anywhere else. */
public final class FarmerRingCharging {
    private FarmerRingCharging() {}
    public static int charge(ItemStack ring,int available) {
        if(available<=0 || !FarmerRingInventory.isRing(ring))return 0;
        var nativeStorage=ring.getCapability(Capabilities.EnergyStorage.ITEM);
        if(nativeStorage==null)return 0;
        return receive(ring,ActuallyComponents.ENERGY_STORAGE.get(),nativeStorage.getMaxEnergyStored(),available);
    }
    static int receive(MutableDataComponentHolder holder,DataComponentType<Integer> energy,int capacity,int available) {
        return new ComponentEnergyStorage(holder,energy,capacity,Integer.MAX_VALUE,0).receiveEnergy(available,false);
    }
}
