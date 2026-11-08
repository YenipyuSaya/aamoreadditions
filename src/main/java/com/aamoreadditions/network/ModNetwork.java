package com.aamoreadditions.network;

import com.aamoreadditions.AAMoreAdditions;
import com.aamoreadditions.menu.AlloyTransfer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = AAMoreAdditions.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {
    private ModNetwork() {}

    /** Client -> server: "move one batch of this alloying recipe into my open Alloy Furnace". */
    public record AlloyTransferPayload(String recipeId) implements CustomPacketPayload {
        public static final Type<AlloyTransferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "alloy_transfer"));
        public static final StreamCodec<ByteBuf, AlloyTransferPayload> CODEC = ByteBufCodecs.stringUtf8(256).map(AlloyTransferPayload::new, AlloyTransferPayload::recipeId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: move one craft of this empowering recipe into the open Empowerer structure. */
    public record EmpowererTransferPayload(String recipeId) implements CustomPacketPayload {
        public static final Type<EmpowererTransferPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "empowerer_transfer"));
        public static final StreamCodec<ByteBuf, EmpowererTransferPayload> CODEC = ByteBufCodecs.stringUtf8(256).map(EmpowererTransferPayload::new, EmpowererTransferPayload::recipeId);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: Empowerer structure at pos is (not) working (sound). */
    public record EmpowererSoundPayload(net.minecraft.core.BlockPos pos, boolean on) implements CustomPacketPayload {
        public static final Type<EmpowererSoundPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "empowerer_sound"));
        public static final StreamCodec<ByteBuf, EmpowererSoundPayload> CODEC = StreamCodec.composite(
                net.minecraft.core.BlockPos.STREAM_CODEC, EmpowererSoundPayload::pos, ByteBufCodecs.BOOL, EmpowererSoundPayload::on, EmpowererSoundPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record UpgradePolicyPayload(boolean dependency, java.util.List<Integer> slots) implements CustomPacketPayload {
        public static final Type<UpgradePolicyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AAMoreAdditions.MODID, "upgrade_policy"));
        public static final StreamCodec<ByteBuf, UpgradePolicyPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, UpgradePolicyPayload::dependency,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(10)), UpgradePolicyPayload::slots, UpgradePolicyPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(UpgradePolicyPayload.TYPE, UpgradePolicyPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.aamoreadditions.upgrade.UpgradeStartup.remote(
                        new com.aamoreadditions.upgrade.UpgradeStartup.Snapshot(p.dependency(), p.slots()))));
        event.registrar("1").optional().playToServer(AlloyTransferPayload.TYPE, AlloyTransferPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> AlloyTransfer.apply(ctx.player(), payload.recipeId())));
        event.registrar("1").optional().playToServer(EmpowererTransferPayload.TYPE, EmpowererTransferPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.aamoreadditions.menu.EmpowererTransfer.apply(ctx.player(), payload.recipeId())));
        event.registrar("1").optional().playToClient(EmpowererSoundPayload.TYPE, EmpowererSoundPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient())
                        com.aamoreadditions.client.sound.EmpowererSound.onPacket(payload.pos(), payload.on());
                }));
    }
}
