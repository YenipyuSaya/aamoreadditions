package com.aamoreadditions.client.screen;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AAMoreAdditions.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModScreens {
    private ModScreens() {}

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ALLOY_FURNACE.get(), AlloyFurnaceScreen::new);
        event.register(ModMenus.CASING_CROSS.get(), CasingCrossScreen::new);
    }
}
