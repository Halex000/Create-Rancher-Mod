package com.halex.createrancher.content.wood;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.index.BlockRegistry;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreateRancher.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class StainedWoodSetup {

    // Lets the signs use vanilla's sign block entities
    @SubscribeEvent
    public static void addSignBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.SIGN, BlockRegistry.STAINED_SIGN.get(), BlockRegistry.STAINED_WALL_SIGN.get());
        event.modify(BlockEntityType.HANGING_SIGN, BlockRegistry.STAINED_HANGING_SIGN.get(),
                BlockRegistry.STAINED_WALL_HANGING_SIGN.get());
    }

    // Makes it burn like wood
    @SubscribeEvent
    public static void setUp(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            fire.setFlammable(BlockRegistry.STAINED_LOG.get(), 5, 5);
            fire.setFlammable(BlockRegistry.STAINED_WOOD.get(), 5, 5);
            fire.setFlammable(BlockRegistry.STRIPPED_STAINED_LOG.get(), 5, 5);
            fire.setFlammable(BlockRegistry.STRIPPED_STAINED_WOOD.get(), 5, 5);
            fire.setFlammable(BlockRegistry.STAINED_PLANKS.get(), 5, 20);
            fire.setFlammable(BlockRegistry.STAINED_STAIRS.get(), 5, 20);
            fire.setFlammable(BlockRegistry.STAINED_SLAB.get(), 5, 20);
            fire.setFlammable(BlockRegistry.STAINED_FENCE.get(), 5, 20);
            fire.setFlammable(BlockRegistry.STAINED_FENCE_GATE.get(), 5, 20);
        });
    }
}
