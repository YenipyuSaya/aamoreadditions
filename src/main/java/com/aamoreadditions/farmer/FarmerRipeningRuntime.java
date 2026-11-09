package com.aamoreadditions.farmer;

import com.aamoreadditions.effect.FarmerScanRuntime;
import com.aamoreadditions.upgrade.*;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import java.util.ArrayList;

/** Runs AFTER native farming. Ripening then remaining-FE charging, never harvest sleep bookkeeping. */
public final class FarmerRipeningRuntime {
    private FarmerRipeningRuntime() {}
    public static void prepare(TileEntityFarmer tile){
        var ring=FarmerRingInventory.get(tile);ring.prepare();
        if(!ring.enabled() || !ring.hasRing() || UpgradeRuntime.blocked(tile))ring.clock.reset();
    }
    public static void afterNative(TileEntityFarmer tile) {
        if(!(tile.getLevel() instanceof ServerLevel level))return;
        var ring=FarmerRingInventory.get(tile);
        if(UpgradeRuntime.blocked(tile)){ring.clock.reset();return;}
        if(!ring.enabled() || !ring.hasRing()){ring.clock.reset();return;}
        var active=UpgradeSlots.get(tile).activeUpgrades().stream().map(t->new UpgradeLogic.Plugin(t.line.name(),t.tier)).toList();
        var plan=FarmerRipeningMath.evaluate(active,UpgradeStartup.dependency());
        int side=FarmerScanRuntime.side(tile);
        var facing=tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        boolean runnable=!tile.isRedstonePowered && tile.storage.getEnergyStored()>0;
        if(ring.clock.step(plan.period(),side,facing.get3DDataValue(),tile.getBlockPos().asLong(),true,runnable)) {
            var cap=ring.getStackInSlot(0).getCapability(Capabilities.EnergyStorage.ITEM);
            if(cap!=null && cap.getEnergyStored()>=FarmerRipeningMath.COST) {
                var center=tile.getBlockPos().relative(facing,side/2+1);
                var targets=new ArrayList<BlockPos>();
                for(int z=-side/2;z<=side/2;z++) for(int x=-side/2;x<=side/2;x++) {
                    var pos=center.offset(x,0,z);
                    if(loadedNeighborhood(level,pos) && eligible(level,pos,level.getBlockState(pos)))targets.add(pos);
                }
                int calls=FarmerRipeningMath.run(plan.draws(),targets.size(),cap::getEnergyStored,level.random::nextInt,index->{
                    var pos=targets.get(index);
                    if(!loadedNeighborhood(level,pos))return false;
                    var state=level.getBlockState(pos);
                    if(!eligible(level,pos,state))return false;
                    state.randomTick(level,pos,level.random);
                    return true; // invocation, not growth, consumes 300 FE
                },amount->cap.extractEnergy(amount,false));
                if(calls>0)ring.energyChanged();
            }
        }
        // Farmer-only unlimited transfer into AA's existing energy component.
        // Redstone pauses scanning/ripening, not this Energizer-style transfer.
        if(tile.storage.getEnergyStored()>0) {
            int received=FarmerRingCharging.charge(ring.getStackInSlot(0),tile.storage.getEnergyStored());
            if(received>0){tile.storage.extractEnergyInternal(received,false);ring.energyChanged();}
        }
    }
    private static boolean loadedNeighborhood(ServerLevel level,BlockPos pos) {
        // Vanilla crop growth can inspect adjacent farmland/fruit positions. Do not load border chunks.
        for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++)if(!level.hasChunkAt(pos.offset(x,0,z)))return false;
        return true;
    }
    static boolean eligible(ServerLevel level,BlockPos pos,BlockState state) {
        var block=state.getBlock();
        if(block instanceof AttachedStemBlock || block instanceof GrassBlock || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM))return false;
        if(block instanceof CropBlock crop)return !crop.isMaxAge(state);
        if(block instanceof StemBlock)return true; // AGE7 without fruit is still allowed
        if(block instanceof NetherWartBlock)return state.getValue(BlockStateProperties.AGE_3)<3;
        if(block instanceof SugarCaneBlock || block==Blocks.CACTUS) {
            if(!level.getBlockState(pos.above()).isAir())return false;
            return !(level.getBlockState(pos.below()).is(block) && level.getBlockState(pos.below(2)).is(block));
        }
        // Same AGE7 compatibility used by AA's default farming behavior, not all bone-meal targets.
        return state.hasProperty(BlockStateProperties.AGE_7) && state.getValue(BlockStateProperties.AGE_7)<7 && state.isRandomlyTicking();
    }
}
