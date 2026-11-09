package com.aamoreadditions.farmer;

import com.aamoreadditions.effect.UpgradeConfig;
import de.ellpeck.actuallyadditions.mod.items.ActuallyItems;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Dedicated single slot: original AA inv stays 12 slots. Full stack components use its codec. */
public final class FarmerRingInventory extends ItemStackHandler {
    public static final String KEY="aamoreadditions_farmer_growth_ring";
    private final TileEntityFarmer tile;
    private boolean loading,enabledFromServer,pendingSync,lastEnabled;
    public final FarmerRipeningClock clock=new FarmerRipeningClock();
    public FarmerRingInventory(TileEntityFarmer tile){super(1);this.tile=tile;}
    public static FarmerRingInventory get(TileEntityFarmer tile){return ((FarmerRingState)tile).aamoreadditions$ringInventory();}
    public static boolean isRing(ItemStack stack){return stack.is(ActuallyItems.RING_OF_GROWTH.get());}
    public boolean hasRing(){return isRing(getStackInSlot(0));}
    public boolean enabled(){return tile.getLevel() instanceof ServerLevel ? UpgradeConfig.farmerAcceleratesRipening() : enabledFromServer;}
    @Override public int getSlotLimit(int slot){return 1;}
    @Override public boolean isItemValid(int slot,ItemStack stack){return isRing(stack);}
    @Override protected void onContentsChanged(int slot){
        if(loading)return;
        clock.reset();tile.setChanged();
        if(tile.getLevel() instanceof ServerLevel){pendingSync=false;tile.sendUpdate();}
    }
    public void energyChanged(){tile.setChanged();pendingSync=true;}
    public void prepare() {
        if(!(tile.getLevel() instanceof ServerLevel level))return;
        boolean enabled=enabled();
        if(enabled!=lastEnabled){lastEnabled=enabled;clock.reset();pendingSync=false;tile.sendUpdate();}
        if(pendingSync && level.getGameTime()%5==0){pendingSync=false;tile.sendUpdate();}
    }
    public void write(CompoundTag tag,HolderLookup.Provider provider,boolean sync) {
        tag.put(KEY,serializeNBT(provider));
        if(sync)tag.putBoolean(KEY+"_enabled",enabled());
    }
    public void read(CompoundTag tag,HolderLookup.Provider provider,boolean sync) {
        loading=true;
        try {if(tag.contains(KEY))deserializeNBT(provider,tag.getCompound(KEY));}
        finally {loading=false;}
        if(sync)enabledFromServer=tag.getBoolean(KEY+"_enabled");
        else clock.reset();
    }
}
