package com.halex.createrancher.content.target;

import javax.annotation.Nullable;

import com.halex.createrancher.CreateRancher;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

// Drops a machine's picked target when the player left-clicks it
@EventBusSubscriber(modid = CreateRancher.MOD_ID, value = Dist.CLIENT)
public class MachineTargetClient {

    private static boolean waitingForRelease;

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!event.isAttack() || player == null)
            return;
        if (!waitingForRelease) {
            ItemStack stack = player.getMainHandItem();
            BlockPos target = MachineTargets.get(player, stack);
            if (target == null || !(stack.getItem() instanceof TargetingItem item)
                    || !isLookingAt(mc, player, item, target))
                return;
            MachineTargets.drop(player);
            waitingForRelease = true;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    // Stops a held click from breaking the block once its target is dropped
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (waitingForRelease && !Minecraft.getInstance().options.keyAttack.isDown())
            waitingForRelease = false;
    }

    private static boolean isLookingAt(Minecraft mc, LocalPlayer player, TargetingItem item, BlockPos target) {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY)
            return false;
        return means(player.level(), item, target, mc.hitResult)
                || means(player.level(), item, target, player.pick(player.blockInteractionRange(), 1, true));
    }

    private static boolean means(Level level, TargetingItem item, BlockPos target, @Nullable HitResult hit) {
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                && target.equals(item.targetAt(level, block.getBlockPos()));
    }
}
