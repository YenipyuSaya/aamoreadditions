package com.aamoreadditions.sound;

import com.aamoreadditions.AAMoreAdditions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, AAMoreAdditions.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> ALLOY_FURNACE_WORKING = SOUNDS.register("block.alloyfurnace.working",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "block.alloyfurnace.working")));
    public static final DeferredHolder<SoundEvent, SoundEvent> EMPOWERER_STRUCTURE_WORKING = SOUNDS.register("block.empowerer_structure.working",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "block.empowerer_structure.working")));
    private ModSounds() {}
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
