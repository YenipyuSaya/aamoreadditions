package com.aamoreadditions.fake;

import com.aamoreadditions.AAMoreAdditions;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Drops cached fake players when a server level unloads, so they cannot pin the world in memory. */
@EventBusSubscriber(modid = AAMoreAdditions.MODID)
public final class MachineFakePlayerEvents {
    private MachineFakePlayerEvents() {}

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) MachineFakePlayers.releaseLevel(level);
    }
}
