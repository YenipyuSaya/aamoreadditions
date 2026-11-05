package com.aamoreadditions.mixin;

import com.aamoreadditions.structure.CasingCrossData;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityDisplayStand;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While a Display Stand belongs to an active Casing Cross structure, its outside energy interface is the structure's shared pool. */
@Mixin(value = TileEntityDisplayStand.class, remap = false)
public abstract class TileEntityDisplayStandEnergyMixin {
    @Inject(method = "getEnergyStorage(Lnet/minecraft/core/Direction;)Lnet/neoforged/neoforge/energy/IEnergyStorage;", at = @At("HEAD"), cancellable = true)
    private void aamoreadditions$structureEnergy(Direction facing, CallbackInfoReturnable<IEnergyStorage> cir) {
        TileEntityDisplayStand self = (TileEntityDisplayStand) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel sl)) return;
        CasingCrossData.Entry e = CasingCrossData.get(sl).entryAt(self.getBlockPos());
        if (e != null) cir.setReturnValue(e.energy);
    }
}
