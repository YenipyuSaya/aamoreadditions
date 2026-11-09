package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.*;
import com.aamoreadditions.upgrade.FarmerWorkState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.ellpeck.actuallyadditions.api.farmer.FarmerResult;
import de.ellpeck.actuallyadditions.api.farmer.IFarmerBehavior;
import de.ellpeck.actuallyadditions.api.internal.IFarmer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Native AA traversal and planting stay intact; only counter/harvest calls are adapted. */
@Mixin(value=TileEntityFarmer.class,remap=false)
public abstract class TileEntityFarmerMixin implements FarmerWorkState, com.aamoreadditions.farmer.FarmerRingState {
    @Unique private com.aamoreadditions.farmer.FarmerRingInventory aamoreadditions$ring;
    @Override public com.aamoreadditions.farmer.FarmerRingInventory aamoreadditions$ringInventory(){
        if(aamoreadditions$ring==null)aamoreadditions$ring=new com.aamoreadditions.farmer.FarmerRingInventory((TileEntityFarmer)(Object)this);
        return aamoreadditions$ring;
    }
    @Shadow private int waitTime;
    @Shadow @Final private static List<IFarmerBehavior> SORTED_FARMER_BEHAVIORS;
    @Unique private final FarmerScanState aamoreadditions$scan=new FarmerScanState();
    @Unique private boolean aamoreadditions$candidate;
    @Unique private boolean aamoreadditions$harvesting, aamoreadditions$storageBlocked, aamoreadditions$harvestFits, aamoreadditions$planted;
    @Unique private static final String AAMORE_SLEEP="aamoreadditions_farmer_sleeping";
    @Override public void aamoreadditions$setFarmerWait(int ticks){waitTime=ticks;}
    @Override public FarmerScanState aamoreadditions$farmerScan(){return aamoreadditions$scan;}
    @Override public void aamoreadditions$resetFarmerScan(){aamoreadditions$scan.reset();waitTime=0;}
    @Override public void aamoreadditions$resetUpgradeWork(){aamoreadditions$resetFarmerScan();}

    @Inject(method="serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",at=@At("HEAD"))
    private static void aamoreadditions$prepare(Level level,BlockPos pos,BlockState state,
                                                net.minecraft.world.level.block.entity.BlockEntity be,CallbackInfo ci) {
        if(be instanceof TileEntityFarmer tile)FarmerScanRuntime.prepare(tile);
    }
    @WrapOperation(method="serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            at=@At(value="FIELD",target="Lde/ellpeck/actuallyadditions/mod/tile/TileEntityFarmer;waitTime:I",opcode=Opcodes.GETFIELD,ordinal=0))
    private static int aamoreadditions$actualScanPeriod(TileEntityFarmer tile,Operation<Integer> original) {
        int remaining=original.call(tile);
        int period=FarmerScanRuntime.cooldown(tile);
        int armed=FarmerUpgradeMath.armCounter(remaining,period);
        ((FarmerWorkState)tile).aamoreadditions$setFarmerWait(armed);
        return armed;
    }
    @Inject(method="serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",at=@At("TAIL"))
    private static void aamoreadditions$ripeningAfterNative(Level level,BlockPos pos,BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity be,CallbackInfo ci) {
        if(be instanceof TileEntityFarmer tile)com.aamoreadditions.farmer.FarmerRipeningRuntime.afterNative(tile);
    }
    @Inject(method="checkBehaviors",at=@At("HEAD"))
    private void aamoreadditions$beginCell(BlockPos query,CallbackInfo ci){
        aamoreadditions$candidate=false;
        aamoreadditions$storageBlocked=false;
        aamoreadditions$harvestFits=false;
        aamoreadditions$planted=false;
    }

    @WrapOperation(method="checkBehaviors",at=@At(value="INVOKE",target="Lde/ellpeck/actuallyadditions/api/farmer/IFarmerBehavior;tryHarvestPlant(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lde/ellpeck/actuallyadditions/api/internal/IFarmer;)Lde/ellpeck/actuallyadditions/api/farmer/FarmerResult;"))
    private FarmerResult aamoreadditions$cheapHarvest(IFarmerBehavior behavior,ServerLevel level,BlockPos pos,
                                                      IFarmer farmer,Operation<FarmerResult> original) {
        boolean possible=FarmerHarvestPrefilter.possible(behavior,level,pos);
        aamoreadditions$candidate|=possible;
        if(!possible && !FarmerHarvestPrefilter.preserveStop(behavior,level,pos))return FarmerResult.FAIL;
        aamoreadditions$harvesting=true;
        try {return original.call(behavior,level,pos,farmer);}
        finally {aamoreadditions$harvesting=false;}
    }
    @WrapOperation(method="checkBehaviors",at=@At(value="INVOKE",target="Lde/ellpeck/actuallyadditions/api/farmer/IFarmerBehavior;tryPlantSeed(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lde/ellpeck/actuallyadditions/api/internal/IFarmer;)Lde/ellpeck/actuallyadditions/api/farmer/FarmerResult;"))
    private FarmerResult aamoreadditions$plantingWork(IFarmerBehavior behavior,ItemStack seed,Level level,BlockPos pos,
                                                     IFarmer farmer,Operation<FarmerResult> original) {
        FarmerResult result=original.call(behavior,seed,level,pos,farmer);
        if(result==FarmerResult.SUCCESS)aamoreadditions$planted=true;
        return result;
    }
    /** Observe the original final output-fit result, after seed fallback and Fortune.
     * Calls outside the current harvest (automation, planting, external probes) are ignored. */
    @Inject(method="canAddToOutput",at=@At("RETURN"))
    private void aamoreadditions$actualHarvestCapacity(List<ItemStack> drops,CallbackInfoReturnable<Boolean> cir) {
        if(!aamoreadditions$harvesting)return;
        if(cir.getReturnValue())aamoreadditions$harvestFits=true;
        else aamoreadditions$storageBlocked=true;
    }
    @Inject(method="checkBehaviors",at=@At("RETURN"))
    private void aamoreadditions$checkedCell(BlockPos query,CallbackInfo ci) {
        FarmerScanRuntime.checked((TileEntityFarmer)(Object)this,query,aamoreadditions$candidate,aamoreadditions$storageBlocked,aamoreadditions$harvestFits,aamoreadditions$planted,SORTED_FARMER_BEHAVIORS);
    }
    @Inject(method="writeSyncableNBT",at=@At("TAIL"))
    private void aamoreadditions$writeSleep(CompoundTag tag,HolderLookup.Provider provider,
                                            TileEntityBase.NBTType type,CallbackInfo ci) {
        if(type==TileEntityBase.NBTType.SYNC)tag.putBoolean(AAMORE_SLEEP,aamoreadditions$scan.sleeping());
        if(type==TileEntityBase.NBTType.SAVE_TILE || type==TileEntityBase.NBTType.SYNC)
            aamoreadditions$ringInventory().write(tag,provider,type==TileEntityBase.NBTType.SYNC);
    }
    @Inject(method="readSyncableNBT",at=@At("TAIL"))
    private void aamoreadditions$readSleep(CompoundTag tag,HolderLookup.Provider provider,
                                           TileEntityBase.NBTType type,CallbackInfo ci) {
        if(type==TileEntityBase.NBTType.SYNC)aamoreadditions$scan.syncedSleeping(tag.getBoolean(AAMORE_SLEEP));
        else {aamoreadditions$scan.loaded();waitTime=0;}
        if(type==TileEntityBase.NBTType.SAVE_TILE || type==TileEntityBase.NBTType.SYNC)
            aamoreadditions$ringInventory().read(tag,provider,type==TileEntityBase.NBTType.SYNC);
    }
}
