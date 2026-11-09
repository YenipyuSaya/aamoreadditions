package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.FarmerEffects;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import de.ellpeck.actuallyadditions.api.internal.IFarmer;
import de.ellpeck.actuallyadditions.mod.misc.apiimpl.farmer.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;

@Mixin(value={CactusFarmerBehavior.class,ReedFarmerBehavior.class,NetherWartFarmerBehavior.class,
        MelonPumpkinFarmerBehavior.class},remap=false)
public abstract class FarmerHarvestMixin {
    @WrapOperation(method="tryHarvestPlant",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/level/block/state/BlockState;getDrops(Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;"))
    private List<ItemStack> aamoreadditions$fortune(BlockState state,LootParams.Builder context,
            Operation<List<ItemStack>> original,@Local(argsOnly=true) IFarmer farmer) {
        return FarmerEffects.harvest(state,context,farmer,builder->original.call(state,builder));
    }
}
