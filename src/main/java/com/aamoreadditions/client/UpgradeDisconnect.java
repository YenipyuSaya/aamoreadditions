package com.aamoreadditions.client;
import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.upgrade.UpgradeStartup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
@EventBusSubscriber(modid=AAMoreAdditions.MODID, value=Dist.CLIENT)
public final class UpgradeDisconnect {
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { UpgradeStartup.resetClient(); }
}
