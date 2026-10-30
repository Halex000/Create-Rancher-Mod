package com.halex.createrancher.content.alfalfa;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.index.BlockRegistry;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.GrassColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

// Tints wild alfalfa with the grass colour
@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreateRancher.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class WildAlfalfaColors {

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor(),
                BlockRegistry.WILD_ALFALFA.get());
    }
}
