package com.halex.createrancher.content.feeder;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.DataComponentRegistry;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;

import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Outlines the pickup point while holding a feeder
@EventBusSubscriber(modid = CreateRancher.MOD_ID, value = Dist.CLIENT)
public class MechanicalFeederClient {

    private static final int PICKUP_COLOR = ArmInteractionPoint.Mode.TAKE.getColor();
    private static final Object PICKUP_SLOT = new Object();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!BlockRegistry.MECHANICAL_FEEDER.isIn(stack))
            return;
        BlockPos pickup = stack.get(DataComponentRegistry.FEEDER_PICKUP);
        if (pickup != null)
            Outliner.getInstance()
                    .showAABB(PICKUP_SLOT, new AABB(pickup))
                    .colored(PICKUP_COLOR)
                    .lineWidth(1 / 16f);
    }
}
