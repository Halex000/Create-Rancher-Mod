package com.halex.createrancher.content.burner;

import java.util.List;

import com.halex.createrancher.CreateRancher;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Sends the fuel list to players so JEI can show it
public record LiquidBlazeBurnerFuelsPacket(List<LiquidBlazeBurnerFuels.Fuel> fuels) implements CustomPacketPayload {

    public static final Type<LiquidBlazeBurnerFuelsPacket> TYPE =
            new Type<>(CreateRancher.asResource("liquid_blaze_burner_fuels"));

    private static final StreamCodec<RegistryFriendlyByteBuf, TagKey<Fluid>> TAG = ResourceLocation.STREAM_CODEC.map(
            id -> TagKey.create(Registries.FLUID, id), TagKey::location)
            .cast();

    private static final StreamCodec<RegistryFriendlyByteBuf, LiquidBlazeBurnerFuels.Fuel> FUEL = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), LiquidBlazeBurnerFuels.Fuel::fluid,
            ByteBufCodecs.optional(TAG), LiquidBlazeBurnerFuels.Fuel::tag,
            ByteBufCodecs.VAR_INT, LiquidBlazeBurnerFuels.Fuel::burnTime,
            ByteBufCodecs.BOOL, LiquidBlazeBurnerFuels.Fuel::superheated,
            LiquidBlazeBurnerFuels.Fuel::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, LiquidBlazeBurnerFuelsPacket> STREAM_CODEC =
            FUEL.apply(ByteBufCodecs.list())
                    .map(LiquidBlazeBurnerFuelsPacket::new, LiquidBlazeBurnerFuelsPacket::fuels);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LiquidBlazeBurnerFuelsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            LiquidBlazeBurnerFuels.receive(packet.fuels);
            if (ModList.get()
                    .isLoaded("jei"))
                com.halex.createrancher.compat.jei.CreateRancherJeiPlugin.fuelsReceived();
        });
    }
}
