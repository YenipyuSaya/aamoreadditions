package com.aamoreadditions.upgrade;

import com.aamoreadditions.AAMoreAdditions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.aamoreadditions.network.ModNetwork.UpgradePolicyPayload;

@EventBusSubscriber(modid = AAMoreAdditions.MODID)
public final class UpgradeSession {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            var snapshot = UpgradeStartup.local();
            PacketDistributor.sendToPlayer(player, new UpgradePolicyPayload(snapshot.dependency(), snapshot.slots()));
        }
    }
}
