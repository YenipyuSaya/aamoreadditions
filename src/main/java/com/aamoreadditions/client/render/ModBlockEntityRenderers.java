package com.aamoreadditions.client.render;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.block.ModBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only registration of block entity renderers (the Dist.CLIENT subscriber is never loaded on a dedicated server). */
@EventBusSubscriber(modid = AAMoreAdditions.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModBlockEntityRenderers {
    private ModBlockEntityRenderers() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.DIMENSIONAL_PHANTOM_BOOSTER_BE.get(), ctx -> new DimensionalBoosterRenderer());
    }
}
