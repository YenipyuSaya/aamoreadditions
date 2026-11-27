package com.aamoreadditions.mixin;
import com.aamoreadditions.upgrade.*;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityInventoryBase;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=TileEntityInventoryBase.class,remap=false)
public abstract class UpgradeInventoryAutomationMixin {
    @Inject(method="getItemHandler",at=@At("RETURN"),cancellable=true)
    private void aamoreadditions$guard(Direction side,CallbackInfoReturnable<IItemHandler> cir){
        BlockEntity be=(BlockEntity)(Object)this;
        // Inventory machines override TileEntityBase#getItemHandler. Route the actual
        // override as well, so every stand/Empowerer cell uses the shared guarded view.
        if (be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger digger) {
            // output slots 0-8 (AA rules) + insert-only filler slots 9-17, all behind the shared guard
            cir.setReturnValue(UpgradeAutomation.items(be,com.aamoreadditions.digger.DiggerAutomation.items(digger,cir.getReturnValue())));
            return;
        }
        if (UpgradeSlots.supports(be) || be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityEmpowerer
                || be instanceof de.ellpeck.actuallyadditions.mod.tile.TileEntityDisplayStand)
            cir.setReturnValue(UpgradeAutomation.items(be,cir.getReturnValue()));
    }
}
