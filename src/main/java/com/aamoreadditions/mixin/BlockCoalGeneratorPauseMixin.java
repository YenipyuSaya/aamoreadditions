package com.aamoreadditions.mixin;

import com.aamoreadditions.effect.OilBurnPause;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.ellpeck.actuallyadditions.mod.blocks.BlockCoalGenerator;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityCoalGenerator;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Retained fuel is not active generation while full: suppress only Coal's smoke predicate. */
@Mixin(value=BlockCoalGenerator.class,remap=false)
public abstract class BlockCoalGeneratorPauseMixin {
    @WrapOperation(method="randomTick",at=@At(value="FIELD",
            target="Lde/ellpeck/actuallyadditions/mod/tile/TileEntityCoalGenerator;currentBurnTime:I",opcode=Opcodes.GETFIELD))
    private int aamoreadditions$noSmokeWhileFull(TileEntityCoalGenerator tile,Operation<Integer> original){
        return OilBurnPause.mayAdvance(tile.storage.getEnergyStored(),tile.storage.getMaxEnergyStored())?original.call(tile):0;
    }
}
