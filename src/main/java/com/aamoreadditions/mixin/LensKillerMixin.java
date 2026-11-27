package com.aamoreadditions.mixin;

import com.aamoreadditions.fake.MachineFakePlayers;
import de.ellpeck.actuallyadditions.api.internal.IAtomicReconstructor;
import de.ellpeck.actuallyadditions.mod.items.lens.LensKiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Killer lens player-attack damage is attributed to the Atomic Reconstructor tile. */
@Mixin(value = LensKiller.class, remap = false)
public abstract class LensKillerMixin {
    @Inject(method = "onAttacked", at = @At("HEAD"), cancellable = true)
    private void aamoreadditions$reconstructor(LivingEntity entity, IAtomicReconstructor tile, CallbackInfo ci) {
        if (tile.getWorldObject().isClientSide || !(tile instanceof BlockEntity be)) return;
        entity.hurt(entity.damageSources().playerAttack(MachineFakePlayers.of(be)), 20);
        ci.cancel();
    }
}
