package com.aamoreadditions.effect;

import de.ellpeck.actuallyadditions.api.farmer.IFarmerBehavior;
import de.ellpeck.actuallyadditions.mod.misc.apiimpl.farmer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Exact built-in classes only. Subclasses/unknown behaviors always run normally. */
public final class FarmerHarvestPrefilter {
    private FarmerHarvestPrefilter(){}
    public static boolean known(IFarmerBehavior behavior) {
        var type=behavior.getClass();
        return type==DefaultFarmerBehavior.class || type==CactusFarmerBehavior.class
                || type==ReedFarmerBehavior.class || type==NetherWartFarmerBehavior.class
                || type==MelonPumpkinFarmerBehavior.class;
    }
    /** Bare reed/cactus bases can return STOP_PROCESSING even with no harvestable
     * upper segment. Retain that original call so planting/extension priority is unchanged. */
    public static boolean preserveStop(IFarmerBehavior behavior,Level level,BlockPos pos) {
        var type=behavior.getClass();
        if(type==CactusFarmerBehavior.class)return level.getBlockState(pos).is(Blocks.CACTUS);
        return type==ReedFarmerBehavior.class && level.getBlockState(pos).getBlock() instanceof SugarCaneBlock;
    }
    public static boolean possible(IFarmerBehavior behavior,Level level,BlockPos pos) {
        if(!known(behavior))return true;
        var state=level.getBlockState(pos);
        var block=state.getBlock();
        var type=behavior.getClass();
        if(type==DefaultFarmerBehavior.class) {
            if(block instanceof CropBlock crop)return crop.isMaxAge(state);
            return state.hasProperty(BlockStateProperties.AGE_7)
                    && state.getValue(BlockStateProperties.AGE_7)>=7 && !(block instanceof StemBlock);
        }
        if(type==NetherWartFarmerBehavior.class)
            return block instanceof NetherWartBlock && state.getValue(BlockStateProperties.AGE_3)>=3;
        if(type==MelonPumpkinFarmerBehavior.class)return block==Blocks.MELON || block==Blocks.PUMPKIN;
        if(type==CactusFarmerBehavior.class) {
            if(block!=Blocks.CACTUS)return false;
            return level.getBlockState(pos.above()).is(Blocks.CACTUS)
                    || level.getBlockState(pos.above(2)).is(Blocks.CACTUS);
        }
        if(!(block instanceof SugarCaneBlock))return false;
        return level.getBlockState(pos.above()).getBlock() instanceof SugarCaneBlock
                || level.getBlockState(pos.above(2)).getBlock() instanceof SugarCaneBlock;
    }
}
