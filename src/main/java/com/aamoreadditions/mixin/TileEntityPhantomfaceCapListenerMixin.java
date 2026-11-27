package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoundDisplays;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** When the bound target's capabilities change, refresh the synced HUD block / in-range flag. */
@Mixin(targets = "de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomface$CapListener", remap = false)
public abstract class TileEntityPhantomfaceCapListenerMixin {

    @Shadow @Final private TileEntityPhantomface tile;

    @Inject(method = "onInvalidate", at = @At("RETURN"))
    private void aama$refreshBoundDisplay(CallbackInfoReturnable<Boolean> cir) {
        BoundDisplays.sync(this.tile);
    }
}
