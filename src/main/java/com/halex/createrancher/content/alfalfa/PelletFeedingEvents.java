package com.halex.createrancher.content.alfalfa;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.index.ItemRegistry;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = CreateRancher.MOD_ID)
public class PelletFeedingEvents {

    // Feeds an animal a pellet when it's right-clicked with one
    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        Entity target = event.getTarget();
        if (!stack.is(ItemRegistry.ALFALFA_PELLETS.get()) || !PelletFeeding.eatsPellets(target))
            return;

        event.setCanceled(true);
        if (event.getLevel().isClientSide) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (!PelletFeeding.canFeed(target)) {
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        Player player = event.getEntity();
        PelletFeeding.feed((LivingEntity) target);
        stack.consume(1, player);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        PelletFeeding.tick(event.getEntity());
    }
}
