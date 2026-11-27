package com.aamoreadditions.mixin;

import com.aamoreadditions.fake.MachineFakePlayers;
import de.ellpeck.actuallyadditions.mod.util.WorldUtil;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** {@code useItemAtSide} runs as the bound machine's fake player. Call sites must {@code bind} first. */
@Mixin(value = WorldUtil.class, remap = false)
public abstract class WorldUtilUseItemMixin {
    @Redirect(method = "useItemAtSide",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/util/FakePlayerFactory;getMinecraft(Lnet/minecraft/server/level/ServerLevel;)Lnet/neoforged/neoforge/common/util/FakePlayer;"))
    private static FakePlayer aamoreadditions$boundMachine(ServerLevel level) {
        FakePlayer bound = MachineFakePlayers.current();
        if (bound != null) return bound;
        // Should not run: Breaker/Placer and Phantom Placer bind before calling. Never fall back to the shared player.
        throw new IllegalStateException("WorldUtil.useItemAtSide without a bound AAMA machine fake player");
    }
}
