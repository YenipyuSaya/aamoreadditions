package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.FarmerEffects;
import de.ellpeck.actuallyadditions.api.internal.IFarmer;
import de.ellpeck.actuallyadditions.api.farmer.FarmerResult;
import de.ellpeck.actuallyadditions.mod.misc.apiimpl.farmer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scale each behavior's local unit cost once, before its original affordability check.
 * Planting use*2 therefore reserves two individually rounded charges; defaultPlant
 * still charges only the tilling/planting operations actually performed. */
@Mixin(value={DefaultFarmerBehavior.class,CactusFarmerBehavior.class,ReedFarmerBehavior.class,
        NetherWartFarmerBehavior.class,MelonPumpkinFarmerBehavior.class},remap=false)
public abstract class FarmerBehaviorCostMixin {
    @Inject(method="tryPlantSeed",at=@At("HEAD"),cancellable=true)
    private void aamoreadditions$guardPlant(ItemStack seed,Level level,BlockPos pos,IFarmer farmer,
                                            CallbackInfoReturnable<FarmerResult> cir) {
        if (!FarmerEffects.canWork(farmer)) cir.setReturnValue(FarmerResult.STOP_PROCESSING);
    }
    @Inject(method="tryHarvestPlant",at=@At("HEAD"),cancellable=true)
    private void aamoreadditions$guardHarvest(ServerLevel level,BlockPos pos,IFarmer farmer,
                                              CallbackInfoReturnable<FarmerResult> cir) {
        if (!FarmerEffects.canWork(farmer)) cir.setReturnValue(FarmerResult.STOP_PROCESSING);
    }
    @ModifyVariable(method="tryPlantSeed",at=@At("STORE"),ordinal=0)
    private int aamoreadditions$plantCost(int cost,ItemStack seed,Level level,BlockPos pos,IFarmer farmer) {
        return FarmerEffects.cost(farmer,cost);
    }
    @ModifyVariable(method="tryHarvestPlant",at=@At("STORE"),ordinal=0)
    private int aamoreadditions$harvestCost(int cost,ServerLevel level,BlockPos pos,IFarmer farmer) {
        return FarmerEffects.cost(farmer,cost);
    }
}
