package com.aamoreadditions.client;

import com.aamoreadditions.AAMoreAdditions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Use NeoForge's standard config UI, including English comment/tooltips and restart metadata. */
@EventBusSubscriber(modid = AAMoreAdditions.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class UpgradeConfigScreen {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        // Configured generates its own factory only when a mod has no factory.
        // Do not silently replace that UI with NeoForge's default screen.
        if (ModList.get().isLoaded("configured")) return;
        ModList.get().getModContainerById(AAMoreAdditions.MODID).orElseThrow()
                .registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
