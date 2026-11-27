package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoosterEffects;
import com.aamoreadditions.booster.BoundParticles;
import com.aamoreadditions.booster.BoundTargets;
import com.aamoreadditions.booster.BoundDisplays;
import com.aamoreadditions.booster.BoundWorlds;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Binding and bound-dimension NBT for the Phantom Placer / Breaker (not a Phantomface subclass). */
@Mixin(value = TileEntityPhantomPlacer.class, remap = false)
public abstract class TileEntityPhantomPlacerBoundMixin {

    @Shadow public BlockPos boundPosition;
    @Shadow public int range;


    @Inject(method = "hasBoundPosition", at = @At("HEAD"), cancellable = true)
    private void aama$hasBoundPosition(CallbackInfoReturnable<Boolean> cir) {
        if (this.boundPosition == null) {
            cir.setReturnValue(false);
            return;
        }
        TileEntityPhantomPlacer self = (TileEntityPhantomPlacer) (Object) this;
        if (BoundTargets.isIllegalBind(self, this.boundPosition)) {
            self.setBoundPosition(null);
            BoundWorlds.set(self, null);
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "isBoundThingInRange", at = @At("HEAD"), cancellable = true)
    private void aama$isBoundThingInRange(CallbackInfoReturnable<Boolean> cir) {
        TileEntityPhantomPlacer self = (TileEntityPhantomPlacer) (Object) this;
        if (BoundDisplays.useSyncedInRange(self)) {
            cir.setReturnValue(BoundDisplays.boundOk(self));
            return;
        }
        if (this.boundPosition == null) {
            cir.setReturnValue(false);
            return;
        }
        if (!self.hasBoundPosition()) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(BoosterEffects.isBoundInRange(self, this.boundPosition, this.range));
    }

    @Inject(method = "writeSyncableNBT", at = @At("RETURN"))
    private void aama$writeBoundDim(CompoundTag compound, HolderLookup.Provider lookupProvider, de.ellpeck.actuallyadditions.mod.tile.TileEntityBase.NBTType type, CallbackInfo ci) {
        BoundWorlds.write((BlockEntity) (Object) this, compound);
        BoundDisplays.write((BlockEntity) (Object) this, compound);
    }

    @Inject(method = "readSyncableNBT", at = @At("RETURN"))
    private void aama$readBoundDim(CompoundTag compound, HolderLookup.Provider lookupProvider, de.ellpeck.actuallyadditions.mod.tile.TileEntityBase.NBTType type, CallbackInfo ci) {
        BoundWorlds.read((BlockEntity) (Object) this, compound);
        BoundDisplays.read((BlockEntity) (Object) this, compound);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static <T extends BlockEntity> void aama$crossDimPortalParticles(Level level, BlockPos pos, BlockState state, T t, CallbackInfo ci) {
        if (t instanceof TileEntityPhantomPlacer tile) {
            BoundParticles.trySpawn(tile);
            BoundDisplays.sync(tile);
        }
    }

    @Inject(method = "renderParticles", at = @At("HEAD"), cancellable = true)
    private void aama$skipCrossDimParticles(CallbackInfo ci) {
        TileEntityPhantomPlacer self = (TileEntityPhantomPlacer) (Object) this;
        Level host = self.getLevel();
        var dim = BoundTargets.dimension(self);
        if (host != null && dim != null && dim != host.dimension()) ci.cancel();
    }
}
