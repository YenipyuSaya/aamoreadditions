package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoundTargets;
import com.aamoreadditions.fake.MachineFakePlayers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPhantomPlacer;
import de.ellpeck.actuallyadditions.mod.util.StackUtil;
import de.ellpeck.actuallyadditions.mod.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Phantom Placer: useItemAtSide is attributed to this tile.
 * Phantom Breaker: break uses the same fake-harvest permission path and getDrops arguments as AA's Breaker,
 * with the dedicated fake player bound to this phantom tile (not the target position).
 */
@Mixin(value = TileEntityPhantomPlacer.class, remap = false)
public abstract class TileEntityPhantomPlacerMixin {
    @Inject(method = "doWork", at = @At("HEAD"), cancellable = true)
    private void aamoreadditions$phantomBreakerHarvest(CallbackInfo ci) {
        var self = (TileEntityPhantomPlacer) (Object) this;
        if (!self.isBreaker) return;
        ci.cancel();
        var resolved = BoundTargets.resolve(self).orElse(null);
        if (resolved == null || !(resolved.level() instanceof ServerLevel server)) return;
        BlockPos target = resolved.pos();
        BlockState state = resolved.state();
        if (state.isAir() || state.getDestroySpeed(server, target) <= -1.0F) return;
        // Same 4-arg getDrops as AA's Breaker (no tool). Loot pos/BE are the target, not the phantom tile.
        List<ItemStack> drops = Block.getDrops(state, server, target, resolved.blockEntity());
        MachineFakePlayers.bind(self, server, target);
        try {
            float chance = WorldUtil.fireFakeHarvestEventsForDropChance(self, server, target);
            if (chance > 0 && server.random.nextFloat() <= chance) {
                if (StackUtil.canAddAll(self.inv, drops, false)) {
                    server.levelEvent(2001, target, Block.getId(state));
                    server.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                    StackUtil.addAll(self.inv, drops, false);
                    self.setChanged();
                }
            }
        } finally {
            MachineFakePlayers.unbind();
        }
    }

    @WrapOperation(method = "doWork", at = @At(value = "INVOKE",
            target = "Lde/ellpeck/actuallyadditions/mod/util/WorldUtil;useItemAtSide(Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack aamoreadditions$bindMachine(Direction side, Level level, BlockPos pos, ItemStack stack,
                                                  Operation<ItemStack> original) {
        var self = (TileEntityPhantomPlacer) (Object) this;
        var resolved = BoundTargets.resolve(self);
        Level useLevel = resolved.map(BoundTargets.Target::level).orElse(level);
        BlockPos usePos = resolved.map(BoundTargets.Target::pos).orElse(pos);
        if (useLevel instanceof ServerLevel server) MachineFakePlayers.bind(self, server, usePos);
        else MachineFakePlayers.bind(self);
        try {
            return original.call(side, useLevel, usePos, stack);
        } finally {
            MachineFakePlayers.unbind();
        }
    }
}
