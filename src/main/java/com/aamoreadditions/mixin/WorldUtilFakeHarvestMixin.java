package com.aamoreadditions.mixin;

import com.aamoreadditions.digger.DiggerState;
import com.aamoreadditions.fake.MachineFakePlayers;
import de.ellpeck.actuallyadditions.mod.items.ActuallyItems;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;
import de.ellpeck.actuallyadditions.mod.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every AA harvest-check goes through the caller's dedicated machine fake player,
 * not the shared Minecraft one. The Vertical Digger also reuses its cached dummy AIOT.
 */
@Mixin(value = WorldUtil.class, remap = false)
public abstract class WorldUtilFakeHarvestMixin {
    @Inject(method = "fireFakeHarvestEventsForDropChance", at = @At("HEAD"), cancellable = true)
    private static void aamoreadditions$machinePlayer(BlockEntity caller, Level level, BlockPos pos,
                                                      CallbackInfoReturnable<Float> cir) {
        if (caller == null || !(level instanceof ServerLevel)) return;
        FakePlayer fake = MachineFakePlayers.of(caller);
        ItemStack tool;
        if (caller instanceof TileEntityVerticalDigger digger) {
            var cached = ((DiggerState) digger).aamoreadditions$work().tool;
            tool = (cached == null || cached.isEmpty())
                    ? new ItemStack(ActuallyItems.NETHERITE_AIOT.get()) : cached;
        } else {
            tool = new ItemStack(ActuallyItems.NETHERITE_AIOT.get());
        }
        cir.setReturnValue(MachineFakePlayers.withMainHand(fake, tool, () -> {
            BlockState state = level.getBlockState(pos);
            BreakEvent event = new BreakEvent(level, pos, state, fake);
            NeoForge.EVENT_BUS.post(event);
            return !event.isCanceled() && EventHooks.doPlayerHarvestCheck(fake, state, level, pos) ? 1F : 0F;
        }));
    }
}
