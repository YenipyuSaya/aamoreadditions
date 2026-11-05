package com.aamoreadditions.mixin;

import com.aamoreadditions.upgrade.UpgradeSlots;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forces the upgrade handler to exist for every supported machine (placed or loaded), so configured base caps apply without upgrades. */
@Mixin(BlockEntity.class)
public abstract class BlockEntitySetLevelMixin {
    @Inject(method = "setLevel", at = @At("TAIL"))
    private void aamoreadditions$initHandler(Level level, CallbackInfo ci) {
        BlockEntity be = (BlockEntity) (Object) this;
        if (UpgradeSlots.supports(be)) UpgradeSlots.get(be);
    }
}
