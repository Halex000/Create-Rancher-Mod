package com.halex.createrancher.content.herder;

import java.util.Optional;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.compat.sable.SubLevels;
import com.halex.createrancher.content.herder.MechanicalHerderBlockEntity.AgeFilter;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ConfigureMechanicalHerderPacket(BlockPos pos, Optional<ResourceLocation> mobFilter, int ageFilter,
                                               int areaSize)
        implements CustomPacketPayload {

    public static final Type<ConfigureMechanicalHerderPacket> TYPE =
            new Type<>(CreateRancher.asResource("configure_mechanical_herder"));

    public static final StreamCodec<ByteBuf, ConfigureMechanicalHerderPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ConfigureMechanicalHerderPacket::pos,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), ConfigureMechanicalHerderPacket::mobFilter,
            ByteBufCodecs.VAR_INT, ConfigureMechanicalHerderPacket::ageFilter,
            ByteBufCodecs.VAR_INT, ConfigureMechanicalHerderPacket::areaSize,
            ConfigureMechanicalHerderPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureMechanicalHerderPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator())
            return;
        if (!player.mayBuild() || !player.level()
                .isLoaded(packet.pos))
            return;
        if (!player.position()
                .closerThan(SubLevels.toWorld(player.level(), packet.pos, Vec3.atCenterOf(packet.pos)), 20))
            return;
        if (!(player.level()
                .getBlockEntity(packet.pos) instanceof MechanicalHerderBlockEntity be))
            return;

        AgeFilter[] ageFilters = AgeFilter.values();
        be.applySettings(packet.mobFilter.orElse(null), ageFilters[Mth.clamp(packet.ageFilter, 0, ageFilters.length - 1)],
                packet.areaSize);
    }
}
