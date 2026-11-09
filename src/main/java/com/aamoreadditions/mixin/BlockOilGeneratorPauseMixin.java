package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.OilBurnPause;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.ellpeck.actuallyadditions.mod.blocks.BlockOilGenerator;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityOilGenerator;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Retained fuel is not active generation while full: suppress only Oil's smoke predicate. */
@Mixin(value=BlockOilGenerator.class,remap=false)
public abstract class BlockOilGeneratorPauseMixin {
    @WrapOperation(method="randomTick",at=@At(value="FIELD",
            target="Lde/ellpeck/actuallyadditions/mod/tile/TileEntityOilGenerator;currentBurnTime:I",opcode=Opcodes.GETFIELD))
    private int aamoreadditions$noSmokeWhileFull(TileEntityOilGenerator tile,Operation<Integer> original){
        return OilBurnPause.mayAdvance(tile.storage.getEnergyStored(),tile.storage.getMaxEnergyStored())?original.call(tile):0;
    }
}
