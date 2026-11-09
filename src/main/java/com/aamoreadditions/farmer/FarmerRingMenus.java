package com.aamoreadditions.farmer;

import com.aamoreadditions.mixin.AbstractContainerMenuInvoker;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class FarmerRingMenus {
    private FarmerRingMenus() {}
    public static void add(AbstractContainerMenu menu,TileEntityFarmer tile){
        ((AbstractContainerMenuInvoker)menu).aamoreadditions$addSlot(new FarmerRingSlot(FarmerRingInventory.get(tile)));
        if(tile.getLevel()!=null && !tile.getLevel().isClientSide)tile.sendUpdate();
    }
    public static ItemStack quickMove(AbstractContainerMenu menu,Player player,int index) {
        if(index<0 || index>=menu.slots.size())return ItemStack.EMPTY;
        var source=menu.slots.get(index);
        int ringIndex=-1,first=-1,last=-1;
        for(int i=0;i<menu.slots.size();i++){
            var slot=menu.slots.get(i);
            if(slot instanceof FarmerRingSlot)ringIndex=i;
            else if(slot.container instanceof Inventory){if(first<0)first=i;last=i+1;}
        }
        if(ringIndex<0 || first<0)return null;
        boolean fromRing=source instanceof FarmerRingSlot;
        boolean toRing=source.container instanceof Inventory && FarmerRingInventory.isRing(source.getItem()) && menu.slots.get(ringIndex).isActive();
        if(!fromRing && !toRing)return null;
        if(!source.hasItem() || !source.mayPickup(player) || !menu.slots.get(ringIndex).isActive())return ItemStack.EMPTY;
        var stack=source.getItem();var original=stack.copy();
        var inv=(AbstractContainerMenuInvoker)menu;
        if(!inv.aamoreadditions$moveItemStackTo(stack,fromRing?first:ringIndex,fromRing?last:ringIndex+1,fromRing))return ItemStack.EMPTY;
        if(stack.isEmpty())source.set(ItemStack.EMPTY);else source.setChanged();
        source.onTake(player,stack);
        return original;
    }
    public static void drop(Level level,TileEntityFarmer tile) {
        var ring=FarmerRingInventory.get(tile);
        var stack=ring.getStackInSlot(0).copy();
        if(stack.isEmpty())return;
        // Detach first, so repeated/reentrant removal cannot duplicate the component-bearing ring.
        ring.setStackInSlot(0,ItemStack.EMPTY);
        net.minecraft.world.Containers.dropItemStack(level,tile.getBlockPos().getX()+.5,tile.getBlockPos().getY()+.5,tile.getBlockPos().getZ()+.5,stack);
    }
}
