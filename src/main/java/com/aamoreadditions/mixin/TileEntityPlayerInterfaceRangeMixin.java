package com.aamoreadditions.mixin;

import com.aamoreadditions.booster.BoosterEffects;
import com.aamoreadditions.booster.BoosterScan;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityPlayerInterface;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes AA's Player Interface distance check ({@code distanceToSqr <= range} instead of
 * {@code range²}) and applies Transfinite / Dimensional binding. The compared position stays
 * the block corner, as in AA; only the missing square is added for the ranged case.
 */
@Mixin(value = TileEntityPlayerInterface.class, remap = false)
public abstract class TileEntityPlayerInterfaceRangeMixin {

    @Shadow public java.util.UUID connectedPlayer;
    @Shadow private int range;

    @Inject(method = "getPlayer", at = @At("HEAD"), cancellable = true)
    private void aama$getPlayer(CallbackInfoReturnable<Player> cir) {
        cir.setReturnValue(findPlayer((TileEntityPlayerInterface) (Object) this));
    }

    private Player findPlayer(TileEntityPlayerInterface self) {
        if (this.connectedPlayer == null || self.getLevel() == null) return null;
        BoosterScan.Binding binding = BoosterEffects.bindingAt(self.getLevel(), self.getBlockPos());
        if (binding == BoosterScan.Binding.DISABLED || this.range <= 0) return null;
        Player player;
        if (binding == BoosterScan.Binding.DIMENSIONAL && self.getLevel().getServer() != null) {
            player = self.getLevel().getServer().getPlayerList().getPlayer(this.connectedPlayer);
        } else if (self.getLevel() instanceof ServerLevel server) {
            player = server.getPlayerByUUID(this.connectedPlayer);
        } else {
            player = self.getLevel().getPlayerByUUID(this.connectedPlayer);
        }
        if (player == null) return null;
        if (binding == BoosterScan.Binding.RANGED) {
            var pos = self.getBlockPos();
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > (double) this.range * (double) this.range) {
                return null;
            }
        }
        // Transfinite: same dimension (getPlayerByUUID already restricts) and no distance check.
        // Dimensional: any dimension; a loaded player entity implies their chunk is loaded.
        return player;
    }
}
