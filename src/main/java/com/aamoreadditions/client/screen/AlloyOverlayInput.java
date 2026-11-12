package com.aamoreadditions.client.screen;

import com.aamoreadditions.AAMoreAdditions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * The Alloy Furnace selection overlay gets mouse clicks before anyone else (highest priority, cancels the event):
 * if a JEI "show recipe/usage" key is bound to the left mouse button, our left-click selection wins.
 */
@EventBusSubscriber(modid = AAMoreAdditions.MODID, value = Dist.CLIENT)
public final class AlloyOverlayInput {
    private AlloyOverlayInput() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof AlloyFurnaceScreen s && s.handleOverlayClick(event.getMouseX(), event.getMouseY(), event.getButton()))
            event.setCanceled(true);
    }
}
