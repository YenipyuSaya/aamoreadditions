package com.aamoreadditions.upgrade;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
/** Dynamic guards remain effective even when a pipe caches the capability. */
public final class UpgradeAutomation {
    private UpgradeAutomation() {}
    public static IItemHandler items(BlockEntity be,IItemHandler h) {
        if (h==null) return null;
        return new IItemHandler() {
            private IItemHandler target() {
                // Resolve on every operation: a pipe may retain this capability across
                // structure activation/deactivation, not only across plugin edits.
                if ((be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityEmpowerer
                        || be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityDisplayStand)
                        && be.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
                    var entry = com.aamoreadditions.structure.CasingCrossData.get(level).entryAt(be.getBlockPos());
                    if (entry != null) return entry.external;
                }
                return h;
            }
            private boolean inRange(IItemHandler target, int slot) {
                return slot >= 0 && slot < target.getSlots();
            }
            public int getSlots() { return target().getSlots(); }
            public ItemStack getStackInSlot(int slot) {
                var target = target();
                return inRange(target,slot) ? target.getStackInSlot(slot) : ItemStack.EMPTY;
            }
            public int getSlotLimit(int slot) {
                var target = target();
                return inRange(target,slot) ? target.getSlotLimit(slot) : 0;
            }
            public boolean isItemValid(int slot,ItemStack stack) {
                var target = target();
                return inRange(target,slot) && !UpgradeRuntime.blocked(be) && target.isItemValid(slot,stack);
            }
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
                var target = target();
                return !inRange(target,slot) || UpgradeRuntime.blocked(be) ? stack : target.insertItem(slot,stack,simulate);
            }
            public ItemStack extractItem(int slot,int amount,boolean simulate) {
                var target = target();
                return !inRange(target,slot) || UpgradeRuntime.blocked(be) ? ItemStack.EMPTY : target.extractItem(slot,amount,simulate);
            }
        };
    }

    public static IFluidHandler fluids(BlockEntity be,IFluidHandler h) {
        if(h==null)return null;
        return new IFluidHandler(){
            public int getTanks(){return h.getTanks();}
            public FluidStack getFluidInTank(int t){return h.getFluidInTank(t);}
            public int getTankCapacity(int t){return h.getTankCapacity(t);}
            public boolean isFluidValid(int t,FluidStack f){return !UpgradeRuntime.blocked(be)&&h.isFluidValid(t,f);}
            public int fill(FluidStack f,FluidAction a){return UpgradeRuntime.blocked(be)?0:h.fill(f,a);}
            public FluidStack drain(FluidStack f,FluidAction a){return UpgradeRuntime.blocked(be)?FluidStack.EMPTY:h.drain(f,a);}
            public FluidStack drain(int n,FluidAction a){return UpgradeRuntime.blocked(be)?FluidStack.EMPTY:h.drain(n,a);}
        };
    }
}
