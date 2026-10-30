package com.halex.createrancher.content.joist.client;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.joist.JoistWood;
import com.halex.createrancher.content.joist.JoistWoods;
import com.halex.createrancher.content.joist.JoistWrenchBehavior;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

// Client setup for joists
@EventBusSubscriber(modid = CreateRancher.MOD_ID, value = Dist.CLIENT)
public class JoistClient {

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        JoistWrenchBehavior.tick();
    }

    // Sets up joist models
    @SuppressWarnings("removal")
    @EventBusSubscriber(modid = CreateRancher.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class Models {

        @SubscribeEvent
        public static void registerBrackets(ModelEvent.RegisterAdditional event) {
            for (JoistWood wood : JoistWoods.all())
                for (Direction side : Iterate.horizontalDirections)
                    event.register(JoistModel.bracket(wood, side));
        }

        @SubscribeEvent
        public static void wrapModels(ModelEvent.ModifyBakingResult event) {
            for (JoistWood wood : JoistWoods.all())
                for (BlockState state : wood.block()
                        .getStateDefinition()
                        .getPossibleStates())
                    event.getModels()
                            .computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                                    (location, model) -> new JoistModel(model, wood));
        }
    }
}
