package com.aamoreadditions.mixin;
import com.aamoreadditions.upgrade.UpgradeRuntime;
import de.ellpeck.actuallyadditions.mod.tile.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value={TileEntityCrusher.class,TileEntityPoweredFurnace.class,TileEntityCoalGenerator.class,TileEntityOilGenerator.class,TileEntityHeatCollector.class,TileEntityLeafGenerator.class,TileEntityEnergizer.class,TileEntityEmpowerer.class,TileEntityDisplayStand.class},remap=false)
public abstract class UpgradeMachineTickMixin {
    @Inject(method="serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",at=@At("HEAD"),cancellable=true)
    private static void aamoreadditions$guard(Level level, BlockPos pos, BlockState state, BlockEntity be, CallbackInfo ci) {
        if (UpgradeRuntime.tick(be)) ci.cancel();
    }
}
