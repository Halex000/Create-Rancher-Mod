package com.halex.createrancher.content.fisher;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.DataComponentRegistry;

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

@EventBusSubscriber(modid = CreateRancher.MOD_ID, value = Dist.CLIENT)
public class MechanicalFisherClient {

    public static final int SPOT_COLOR = 0xDDC166;

    private static final Object SPOT_SLOT = new Object();

    // Outlines the fishing spot while holding a fisher
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null)
            return;
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!BlockRegistry.MECHANICAL_FISHER.isIn(stack))
            return;
        BlockPos spot = stack.get(DataComponentRegistry.FISHING_SPOT);
        if (spot == null)
            return;
        Outliner.getInstance()
                .showAABB(SPOT_SLOT, new AABB(spot))
                .colored(SPOT_COLOR)
                .lineWidth(1 / 16f);
    }
}
