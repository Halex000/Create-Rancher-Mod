package com.halex.createrancher.content.feeder;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.DataComponentRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Picks the feeder's pickup point when a block is right-clicked
@EventBusSubscriber(modid = CreateRancher.MOD_ID)
public class MechanicalFeederEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getHand() != InteractionHand.MAIN_HAND || !BlockRegistry.MECHANICAL_FEEDER.isIn(stack))
            return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        if (player.isSpectator() || !MechanicalFeederBlockEntity.isPickupCandidate(level, pos))
            return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide)
            return;

        if (pos.equals(stack.get(DataComponentRegistry.FEEDER_PICKUP))) {
            stack.remove(DataComponentRegistry.FEEDER_PICKUP);
            player.displayClientMessage(Component.translatable("createrancher.message.feeder.pickup_cleared"), true);
        } else {
            stack.set(DataComponentRegistry.FEEDER_PICKUP, pos.immutable());
            player.displayClientMessage(Component.translatable("createrancher.message.feeder.pickup_selected",
                    level.getBlockState(pos)
                            .getBlock()
                            .getName()), true);
        }
    }
}
