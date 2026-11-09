package com.aamoreadditions.item;

import com.aamoreadditions.AAMoreAdditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/** Immutable native item component. Explicit size preserves trailing/all-empty recorded slots. */
public record WrenchRecord(int size, ItemContainerContents contents) {
    public static final Codec<WrenchRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 10).fieldOf("size").forGetter(WrenchRecord::size),
            ItemContainerContents.CODEC.fieldOf("contents").forGetter(WrenchRecord::contents)
    ).apply(instance, WrenchRecord::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WrenchRecord> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AAMoreAdditions.MODID);
    public static final Supplier<DataComponentType<WrenchRecord>> TYPE = COMPONENTS.register("upgrade_wrench_record",
            () -> DataComponentType.<WrenchRecord>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).build());

    public static WrenchRecord capture(List<ItemStack> slots) {
        return new WrenchRecord(slots.size(), ItemContainerContents.fromItems(slots));
    }

    public List<ItemStack> slots() {
        NonNullList<ItemStack> slots = NonNullList.withSize(size, ItemStack.EMPTY);
        contents.copyInto(slots);
        return slots;
    }
}
