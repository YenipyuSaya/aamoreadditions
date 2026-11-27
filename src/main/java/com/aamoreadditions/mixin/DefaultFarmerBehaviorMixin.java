package com.aamoreadditions.mixin;

import com.aamoreadditions.fake.MachineFakePlayers;
import de.ellpeck.actuallyadditions.mod.misc.apiimpl.farmer.DefaultFarmerBehavior;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Farmer tilling uses the bound Farmer's fake player. EntityWorm (not a work block) still uses the shared one.
 */
@Mixin(value = DefaultFarmerBehavior.class, remap = false)
public abstract class DefaultFarmerBehaviorMixin {
    @Redirect(method = "useHoeAt", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/util/FakePlayerFactory;getMinecraft(Lnet/minecraft/server/level/ServerLevel;)Lnet/neoforged/neoforge/common/util/FakePlayer;"))
    private static net.neoforged.neoforge.common.util.FakePlayer aamoreadditions$farmerOrShared(ServerLevel level) {
        var bound = MachineFakePlayers.current();
        return bound != null ? bound : FakePlayerFactory.getMinecraft(level);
    }
}
