package com.aamoreadditions.farmer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Always exists in both menu layouts; server-authoritative visibility does not shift indices. */
public final class FarmerRingSlot extends SlotItemHandler {
    private final FarmerRingInventory ring;
    public FarmerRingSlot(FarmerRingInventory ring){super(ring,0,67,75);this.ring=ring;}
    @Override public boolean isActive(){return ring.enabled();}
    @Override public boolean mayPlace(ItemStack stack){return isActive() && FarmerRingInventory.isRing(stack);}
    @Override public boolean mayPickup(Player player){return isActive() && super.mayPickup(player);}
}
