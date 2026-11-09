package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.FarmerEffects;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import de.ellpeck.actuallyadditions.api.internal.IFarmer;
import de.ellpeck.actuallyadditions.mod.misc.apiimpl.farmer.DefaultFarmerBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.List;

@Mixin(value=DefaultFarmerBehavior.class,remap=false)
public abstract class FarmerDefaultHarvestMixin {
    @Inject(method="defaultPlant",at=@At("HEAD"),cancellable=true)
    private static void aamoreadditions$guardDefaultPlant(Level level,BlockPos pos,BlockState plant,IFarmer farmer,
                                                         int use,CallbackInfoReturnable<Boolean> cir) {
        if (!FarmerEffects.canWork(farmer)) cir.setReturnValue(false);
    }
    @WrapOperation(method="doFarmerStuff",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/level/block/state/BlockState;getDrops(Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;"))
    private List<ItemStack> aamoreadditions$fortune(BlockState state,LootParams.Builder context,
            Operation<List<ItemStack>> original,@Local(argsOnly=true) IFarmer farmer) {
        return FarmerEffects.harvest(state,context,farmer,builder->original.call(state,builder));
    }
    // This private helper uses literal 250, not tryHarvestPlant's scaled local cost.
    @WrapOperation(method="doFarmerStuff",at=@At(value="INVOKE",
            target="Lde/ellpeck/actuallyadditions/api/internal/IFarmer;extractEnergy(I)V"))
    private void aamoreadditions$matchingHarvestCharge(IFarmer farmer,int amount,Operation<Void> original) {
        original.call(farmer,FarmerEffects.cost(farmer,amount));
    }
}
