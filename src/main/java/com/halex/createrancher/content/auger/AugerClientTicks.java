package com.halex.createrancher.content.auger;

import com.halex.createrancher.CreateRancher;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Counts client ticks for augers
@EventBusSubscriber(modid = CreateRancher.MOD_ID, value = Dist.CLIENT)
public class AugerClientTicks {

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        AugerBlockEntity.clientTick++;
    }
}
