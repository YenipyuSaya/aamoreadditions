package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoosterEffects;
import com.aamoreadditions.booster.BoundParticles;
import com.aamoreadditions.booster.BoundTargets;
import com.aamoreadditions.booster.BoundDisplays;
import com.aamoreadditions.booster.BoundWorlds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Take over booster range, binding checks, bound-dimension NBT, and cross-dimension target access. */
@Mixin(value = TileEntityPhantomface.class, remap = false)
public abstract class TileEntityPhantomfaceRangeMixin {

    @Shadow public BlockPos boundPosition;
    @Shadow public int range;
    @Shadow private int rangeBefore;
    @Shadow private BlockPos boundPosBefore;
    @Shadow private Block boundBlockBefore;

    @Inject(method = "upgradeRange", at = @At("HEAD"), cancellable = true)
    private static void aama$upgradeRange(int defaultRange, Level world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(BoosterEffects.upgradeRange(defaultRange, world, pos));
    }

    @Inject(method = "hasBoundPosition", at = @At("HEAD"), cancellable = true)
    private void aama$hasBoundPosition(CallbackInfoReturnable<Boolean> cir) {
        if (this.boundPosition == null) {
            cir.setReturnValue(false);
            return;
        }
        TileEntityPhantomface self = (TileEntityPhantomface) (Object) this;
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
        TileEntityPhantomface self = (TileEntityPhantomface) (Object) this;
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

    @Inject(method = "doesNeedUpdateSend", at = @At("HEAD"), cancellable = true)
    private void aama$doesNeedUpdateSend(CallbackInfoReturnable<Boolean> cir) {
        TileEntityPhantomface self = (TileEntityPhantomface) (Object) this;
        if (this.boundPosition != this.boundPosBefore || this.rangeBefore != this.range
                || BoundTargets.listenerOutOfDate(self)) {
            cir.setReturnValue(true);
            return;
        }
        if (this.boundPosition == null) {
            cir.setReturnValue(this.boundBlockBefore != null);
            return;
        }
        var target = BoundTargets.resolveIfLoaded(self);
        if (target.isEmpty()) {
            // Unloaded target: not a change. Re-check when the chunk is present again (listenerOutOfDate).
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(target.get().state().getBlock() != this.boundBlockBefore);
    }

    @Inject(method = "onUpdateSent", at = @At("HEAD"))
    private void aama$refreshBoundDisplay(CallbackInfo ci) {
        BoundDisplays.refresh((BlockEntity) (Object) this);
    }

    @WrapOperation(method = "onUpdateSent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
            remap = true))
    private BlockState aama$onUpdateSentBoundState(Level level, BlockPos pos, Operation<BlockState> original) {
        TileEntityPhantomface self = (TileEntityPhantomface) (Object) this;
        var target = BoundTargets.resolveIfLoaded(self);
        if (target.isPresent()) return target.get().state();
        // Cross-dimension but unloaded: do not read the host world at the bound coordinates.
        var dim = BoundTargets.dimension(self);
        if (self.getLevel() != null && dim != null && dim != self.getLevel().dimension()) {
            return this.boundBlockBefore != null ? this.boundBlockBefore.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return original.call(level, pos);
    }

    @Inject(method = "getComparatorStrength", at = @At("HEAD"), cancellable = true)
    private void aama$getComparatorStrength(CallbackInfoReturnable<Integer> cir) {
        TileEntityPhantomface self = (TileEntityPhantomface) (Object) this;
        var target = BoundTargets.resolve(self);
        if (target.isEmpty()) {
            cir.setReturnValue(0);
            return;
        }
        BlockState state = target.get().state();
        cir.setReturnValue(state.hasAnalogOutputSignal()
                ? state.getAnalogOutputSignal(target.get().level(), target.get().pos())
                : 0);
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;registerCapabilityListener(Lnet/minecraft/core/BlockPos;Lnet/neoforged/neoforge/capabilities/ICapabilityInvalidationListener;)V"))
    private void aama$registerCapListener(ServerLevel instance, BlockPos pos, ICapabilityInvalidationListener listener,
                                          Operation<Void> original) {
        TileEntityPhantomface self = (TileEntityPhantomface) (Object) this;
        BoundTargets.forgetListener(self);
        var target = BoundTargets.resolveIfLoaded(self);
        if (target.isEmpty() || !(target.get().level() instanceof ServerLevel server)) return;
        original.call(server, target.get().pos(), listener);
        BoundTargets.rememberListener(self, target.get());
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void aama$crossDimPortalParticles(CallbackInfo ci) {
        TileEntityPhantomface self = (TileEntityPhantomface) (Object) this;
        BoundParticles.trySpawn(self);
        BoundDisplays.sync(self);
    }

    @Inject(method = "renderParticles", at = @At("HEAD"), cancellable = true)
    private void aama$skipCrossDimParticles(CallbackInfo ci) {
        TileEntityPhantomface self = (TileEntityPhantomface) (Object) this;
        Level host = self.getLevel();
        var dim = BoundTargets.dimension(self);
        if (host != null && dim != null && dim != host.dimension()) ci.cancel();
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
}
