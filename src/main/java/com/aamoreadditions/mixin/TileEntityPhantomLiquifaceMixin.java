package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoundDisplays;
import com.aamoreadditions.booster.BoundTargets;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityLaserRelayFluids;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomLiquiface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolve the bound fluid handler in the stored dimension. */
@Mixin(value = TileEntityPhantomLiquiface.class, remap = false)
public abstract class TileEntityPhantomLiquifaceMixin {

    @Inject(method = "isBoundThingInRange", at = @At("HEAD"), cancellable = true)
    private void aama$isBoundThingInRange(CallbackInfoReturnable<Boolean> cir) {
        TileEntityPhantomLiquiface self = (TileEntityPhantomLiquiface) (Object) this;
        if (BoundDisplays.useSyncedInRange(self)) {
            cir.setReturnValue(BoundDisplays.boundOk(self));
            return;
        }
        var target = BoundTargets.resolve(self);
        if (target.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        BlockEntity be = target.get().blockEntity();
        if (be == null || be instanceof TileEntityLaserRelayFluids) {
            cir.setReturnValue(false);
            return;
        }
        for (Direction facing : Direction.values()) {
            if (target.get().capability(Capabilities.FluidHandler.BLOCK, facing) != null) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "getFluidHandler", at = @At("HEAD"), cancellable = true)
    private void aama$getFluidHandler(Direction facing, CallbackInfoReturnable<IFluidHandler> cir) {
        cir.setReturnValue(BoundTargets.capability((TileEntityPhantomLiquiface) (Object) this, Capabilities.FluidHandler.BLOCK, facing));
    }
}
