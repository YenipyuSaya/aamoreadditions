package com.aamoreadditions.mixin;

import com.aamoreadditions.fake.MachineFakePlayers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityBreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Placer (and Breaker-as-placer) useItemAtSide is attributed to this tile's fake player. */
@Mixin(value = TileEntityBreaker.class, remap = false)
public abstract class TileEntityBreakerMixin {
    @WrapOperation(method = "doWork", at = @At(value = "INVOKE",
            target = "Lde/ellpeck/actuallyadditions/mod/util/WorldUtil;useItemAtSide(Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack aamoreadditions$bindMachine(Direction side, Level level, BlockPos pos, ItemStack stack,
                                                  Operation<ItemStack> original) {
        MachineFakePlayers.bind((TileEntityBreaker) (Object) this);
        try {
            return original.call(side, level, pos, stack);
        } finally {
            MachineFakePlayers.unbind();
        }
    }
}
