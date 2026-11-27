package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoundDisplays;
import com.aamoreadditions.booster.BoundTargets;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityLaserRelayEnergy;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomEnergyface;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolve the bound energy storage in the stored dimension. */
@Mixin(value = TileEntityPhantomEnergyface.class, remap = false)
public abstract class TileEntityPhantomEnergyfaceMixin {

    @Inject(method = "isBoundThingInRange", at = @At("HEAD"), cancellable = true)
    private void aama$isBoundThingInRange(CallbackInfoReturnable<Boolean> cir) {
        TileEntityPhantomEnergyface self = (TileEntityPhantomEnergyface) (Object) this;
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
        if (be == null || be instanceof TileEntityLaserRelayEnergy) {
            cir.setReturnValue(false);
            return;
        }
        for (Direction facing : Direction.values()) {
            if (target.get().capability(Capabilities.EnergyStorage.BLOCK, facing) != null) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "getEnergyStorage", at = @At("HEAD"), cancellable = true)
    private void aama$getEnergyStorage(Direction facing, CallbackInfoReturnable<IEnergyStorage> cir) {
        cir.setReturnValue(BoundTargets.capability((TileEntityPhantomEnergyface) (Object) this, Capabilities.EnergyStorage.BLOCK, facing));
    }
}
