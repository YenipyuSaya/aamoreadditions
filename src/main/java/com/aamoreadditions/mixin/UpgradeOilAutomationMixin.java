package com.aamoreadditions.mixin;
import com.aamoreadditions.upgrade.UpgradeAutomation;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityOilGenerator;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=TileEntityOilGenerator.class,remap=false)
public abstract class UpgradeOilAutomationMixin {
    @Inject(method="getFluidHandler",at=@At("RETURN"),cancellable=true)
    private void aamoreadditions$guard(Direction side,CallbackInfoReturnable<IFluidHandler> cir){
        cir.setReturnValue(UpgradeAutomation.fluids((BlockEntity)(Object)this,cir.getReturnValue()));
    }
}
