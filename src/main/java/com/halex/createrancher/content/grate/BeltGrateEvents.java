package com.halex.createrancher.content.grate;

import java.util.List;

import com.halex.createrancher.CreateRancher;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = CreateRancher.MOD_ID)
public class BeltGrateEvents {

    // Sneak right-click a grated belt with an empty hand to take the grate off
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        if (!BeltGrates.isGrated(level, event.getPos()))
            return;
        if (event.getHand() == InteractionHand.MAIN_HAND && player.isShiftKeyDown() && player.getMainHandItem()
                .isEmpty()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (level.isClientSide)
                return;
            List<ItemStack> removed = BeltGrates.removeGrate(level, event.getPos());
            if (!player.isCreative())
                removed.forEach(player.getInventory()::placeItemBackInInventory);
            return;
        }
        plate(event, player, level);
    }

    // Plates a belt grate with the nugget it's clicked with
    private static void plate(PlayerInteractEvent.RightClickBlock event, Player player, Level level) {
        ItemStack held = player.getItemInHand(event.getHand());
        GrateMetal metal = GrateMetal.byNugget(held);
        if (metal == null)
            return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide)
            return;
        GrateMetal before = BeltGrates.plateGrate(level, event.getPos(), metal);
        if (before == null || player.isCreative())
            return;
        held.shrink(1);
        Item returned = before.getNugget();
        if (returned != null)
            player.getInventory()
                    .placeItemBackInInventory(new ItemStack(returned));
    }
}
