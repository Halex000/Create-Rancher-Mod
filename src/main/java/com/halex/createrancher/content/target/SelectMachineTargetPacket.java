package com.halex.createrancher.content.target;

import java.util.Optional;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.compat.sable.SubLevels;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Tells the server which target a player picked for the machine in their hand
public record SelectMachineTargetPacket(Optional<BlockPos> target) implements CustomPacketPayload {

    private static final double MAX_DISTANCE = 20;

    public static final Type<SelectMachineTargetPacket> TYPE =
            new Type<>(CreateRancher.asResource("select_machine_target"));

    public static final StreamCodec<ByteBuf, SelectMachineTargetPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), SelectMachineTargetPacket::target,
            SelectMachineTargetPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectMachineTargetPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator())
            return;
        if (packet.target.isEmpty() || isNear(player, packet.target.get()))
            MachineTargets.apply(player, packet.target.orElse(null));
    }

    private static boolean isNear(ServerPlayer player, BlockPos target) {
        return player.level()
                .isLoaded(target) && player.position()
                .closerThan(SubLevels.toWorld(player.level(), target, Vec3.atCenterOf(target)), MAX_DISTANCE);
    }
}
