package com.aamoreadditions.effect;

import com.aamoreadditions.upgrade.FarmerWorkState;
import de.ellpeck.actuallyadditions.api.farmer.IFarmerBehavior;
import de.ellpeck.actuallyadditions.mod.config.CommonConfig;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityFarmer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.List;

/** O(1) geometry checks per tick, one cell per actual scan, no area pre-scan. */
public final class FarmerScanRuntime {
    private FarmerScanRuntime(){}
    public static int side(TileEntityFarmer tile) {
        int side=Math.min(tile.getArea(),CommonConfig.Machines.FARMER_AREA.get());
        return side%2==0?side+1:side;
    }
    public static void prepare(TileEntityFarmer tile) {
        if(!(tile.getLevel() instanceof ServerLevel))return;
        var work=(FarmerWorkState)tile;var scan=work.aamoreadditions$farmerScan();
        boolean old=scan.sleeping();
        int facing=tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue();
        if(scan.configure(side(tile),facing,tile.getBlockPos().asLong(),UpgradeConfig.farmerSmartSleep()))
            work.aamoreadditions$setFarmerWait(0);
        if(old!=scan.sleeping())tile.sendUpdate();
    }
    public static void checked(TileEntityFarmer tile,BlockPos query,boolean candidate,boolean storageBlocked,boolean harvestFits,boolean planted,List<IFarmerBehavior> behaviors) {
        if(!(tile.getLevel() instanceof ServerLevel))return;
        var scan=((FarmerWorkState)tile).aamoreadditions$farmerScan();
        boolean old=scan.sleeping();int side=side(tile),radius=side/2;
        var center=tile.getBlockPos().relative(tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING),radius+1);
        int x=query.getX()-center.getX()+radius,z=query.getZ()-center.getZ()+radius;
        // Unknown active behaviors prevent asserting that a whole round was empty.
        boolean unknown=behaviors.stream().anyMatch(b->!FarmerHarvestPrefilter.known(b));
        int index=x>=0&&x<side&&z>=0&&z<side?z*side+x:-1;
        if(planted)scan.planted(index);
        else scan.observe(index,candidate,storageBlocked,harvestFits,unknown);
        if(old!=scan.sleeping())tile.sendUpdate();
    }
    public static int cooldown(TileEntityFarmer tile) {
        return ((FarmerWorkState)tile).aamoreadditions$farmerScan().sleeping()
                ?FarmerScanState.SLOW_TICKS:FarmerEffects.evaluate(tile).cooldown();
    }
}
