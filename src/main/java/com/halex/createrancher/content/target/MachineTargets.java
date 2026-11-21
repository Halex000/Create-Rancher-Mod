package com.halex.createrancher.content.target;

import java.util.Optional;

import javax.annotation.Nullable;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.index.AttachmentRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// Remembers the target each player has picked for the machine in their hand
@EventBusSubscriber(modid = CreateRancher.MOD_ID)
public final class MachineTargets {

    public record Picked(Item machine, int slot, ResourceKey<Level> dimension, BlockPos target) {
    }

    private MachineTargets() {
    }

    // The target a held machine would use
    @Nullable
    public static BlockPos get(@Nullable Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof TargetingItem item))
            return null;
        Picked picked = player == null ? null : picked(player);
        return picked != null && stack.is(picked.machine()) ? picked.target() : stack.get(item.savedTarget());
    }

    // Gets the target for a machine being placed and ends the selection
    @Nullable
    public static BlockPos take(@Nullable LivingEntity placer, ItemStack stack) {
        Player player = placer instanceof Player placing ? placing : null;
        BlockPos target = get(player, stack);
        Picked picked = player == null ? null : picked(player);
        if (picked != null && stack.is(picked.machine()))
            player.removeData(AttachmentRegistry.PICKED_TARGET);
        return target;
    }

    // Picks a target or drops it if it's already picked
    public static void toggle(Player player, BlockPos target, Component pickedMessage) {
        if (target.equals(get(player, player.getMainHandItem()))) {
            drop(player);
            return;
        }
        send(player, target.immutable());
        player.displayClientMessage(pickedMessage, true);
    }

    // Drops the held machine's target
    public static void drop(Player player) {
        if (!(player.getMainHandItem()
                .getItem() instanceof TargetingItem item))
            return;
        send(player, null);
        player.displayClientMessage(item.targetDroppedMessage(), true);
    }

    private static void send(Player player, @Nullable BlockPos target) {
        apply(player, target);
        PacketDistributor.sendToServer(new SelectMachineTargetPacket(Optional.ofNullable(target)));
    }

    // Sets the held machine's target and clears any target saved on the item
    public static void apply(Player player, @Nullable BlockPos target) {
        player.removeData(AttachmentRegistry.PICKED_TARGET);
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof TargetingItem item))
            return;
        stack.remove(item.savedTarget());
        if (target != null)
            player.setData(AttachmentRegistry.PICKED_TARGET, Optional.of(new Picked(stack.getItem(),
                    player.getInventory().selected, player.level()
                    .dimension(), target)));
    }

    // Forgets the target once the player stops holding that machine
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.hasData(AttachmentRegistry.PICKED_TARGET) && picked(player) == null)
            player.removeData(AttachmentRegistry.PICKED_TARGET);
    }

    @Nullable
    private static Picked picked(Player player) {
        if (!player.hasData(AttachmentRegistry.PICKED_TARGET))
            return null;
        Picked picked = player.getData(AttachmentRegistry.PICKED_TARGET)
                .orElse(null);
        boolean held = picked != null && picked.slot() == player.getInventory().selected && player.getMainHandItem()
                .is(picked.machine()) && picked.dimension() == player.level()
                .dimension();
        return held ? picked : null;
    }
}
