package com.aamoreadditions.mixin;

import com.aamoreadditions.structure.CasingCrossData;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityBase;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityDisplayStand;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityEmpowerer;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While an Empowerer or Display Stand belongs to an active Casing Cross structure, pipes/hoppers see the structure's inventory instead of the block's own. */
@Mixin(value = TileEntityBase.class, remap = false)
public abstract class TileEntityBaseItemHandlerMixin {
    @Inject(method = "getItemHandler", at = @At("HEAD"), cancellable = true)
    private void aamoreadditions$structureItems(Direction facing, CallbackInfoReturnable<IItemHandler> cir) {
        TileEntityBase self = (TileEntityBase) (Object) this;
        if (!(self instanceof TileEntityEmpowerer) && !(self instanceof TileEntityDisplayStand)) return;
        if (!(self.getLevel() instanceof ServerLevel sl)) return;
        CasingCrossData.Entry e = CasingCrossData.get(sl).entryAt(self.getBlockPos());
        if (e != null) cir.setReturnValue(e.external);
    }
}
