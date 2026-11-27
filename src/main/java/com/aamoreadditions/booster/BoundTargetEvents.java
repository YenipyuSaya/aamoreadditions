package com.aamoreadditions.booster;

import com.aamoreadditions.AAMoreAdditions;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Drops cross-dimension capability listeners when a target level unloads, without loading anything. */
@EventBusSubscriber(modid = AAMoreAdditions.MODID)
public final class BoundTargetEvents {
    private BoundTargetEvents() {}

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) BoundTargets.onLevelUnload(level);
    }
}
