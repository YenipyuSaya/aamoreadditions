package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoundDisplays;
import com.aamoreadditions.booster.BoundTargets;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomItemface;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolve the bound inventory in the stored dimension; never via the host level's coordinates. */
@Mixin(value = TileEntityPhantomItemface.class, remap = false)
public abstract class TileEntityPhantomItemfaceMixin {

    @Inject(method = "isBoundThingInRange", at = @At("HEAD"), cancellable = true)
    private void aama$isBoundThingInRange(CallbackInfoReturnable<Boolean> cir) {
        TileEntityPhantomItemface self = (TileEntityPhantomItemface) (Object) this;
        if (BoundDisplays.useSyncedInRange(self)) {
            cir.setReturnValue(BoundDisplays.boundOk(self));
            return;
        }
        var target = BoundTargets.resolve(self);
        if (target.isEmpty() || target.get().blockEntity() == null) {
            cir.setReturnValue(false);
            return;
        }
        for (Direction facing : Direction.values()) {
            if (target.get().capability(Capabilities.ItemHandler.BLOCK, facing) != null) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "getItemHandler", at = @At("HEAD"), cancellable = true)
    private void aama$getItemHandler(Direction facing, CallbackInfoReturnable<IItemHandler> cir) {
        cir.setReturnValue(BoundTargets.capability((TileEntityPhantomItemface) (Object) this, Capabilities.ItemHandler.BLOCK, facing));
    }
}
