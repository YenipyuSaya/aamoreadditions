package com.aamoreadditions.mixin;

import com.aamoreadditions.fake.MachineFakePlayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drop a tile's cached fake player as soon as the block entity is removed. */
@Mixin(BlockEntity.class)
public abstract class MachineFakePlayerRemovalMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void aamoreadditions$releaseFakePlayer(CallbackInfo ci) {
        MachineFakePlayers.release((BlockEntity) (Object) this);
    }
}
