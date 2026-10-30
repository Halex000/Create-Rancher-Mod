package com.halex.createrancher;

import com.halex.createrancher.content.crate.CrateScreen;
import com.halex.createrancher.content.grill.CauldronGrillBlock;
import com.halex.createrancher.content.grill.client.GrillLavaParticle;
import com.halex.createrancher.content.grill.client.GrillSpitParticle;
import com.halex.createrancher.content.net.FishingNetItem;
import com.halex.createrancher.content.net.client.FishingNetRenderer;
import com.halex.createrancher.content.satchel.SatchelScreen;
import com.halex.createrancher.content.wood.StainedWood;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.EntityRegistry;
import com.halex.createrancher.index.FluidRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.halex.createrancher.index.MenuRegistry;
import com.halex.createrancher.index.ParticleRegistry;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

// Client registration
@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreateRancher.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CreateRancherClientSetup {

    @SubscribeEvent
    public static void setUp(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Sheets.addWoodType(StainedWood.TYPE);
            ItemBlockRenderTypes.setRenderLayer(FluidRegistry.TANNIN.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidRegistry.TANNIN.get()
                    .getSource(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidRegistry.BIOFUEL.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FluidRegistry.BIOFUEL.get()
                    .getSource(), RenderType.translucent());
            ItemProperties.register(ItemRegistry.FISHING_NET.get(), CreateRancher.asResource("cast"),
                    (stack, level, entity, seed) -> {
                        if (!(entity instanceof Player player) || player.fishing == null)
                            return 0;
                        boolean main = entity.getMainHandItem() == stack;
                        boolean off = entity.getOffhandItem() == stack && !(entity.getMainHandItem()
                                .getItem() instanceof FishingNetItem);
                        return main || off ? 1 : 0;
                    });
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.FISHING_NET.get(), FishingNetRenderer::new);
    }

    // Tints the tannin and biofuel buckets
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new DynamicFluidContainerModel.Colors(), FluidRegistry.TANNIN.get()
                .getBucket(), FluidRegistry.BIOFUEL.get()
                .getBucket());
    }

    // Tints water in cauldron grills with the biome's water colour
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (state.getValue(CauldronGrillBlock.CONTENTS) != CauldronGrillBlock.Contents.WATER)
                return -1;
            return level != null && pos != null ? BiomeColors.getAverageWaterColor(level, pos) : DEFAULT_WATER;
        }, BlockRegistry.CAULDRON_GRILL.get());
    }

    private static final int DEFAULT_WATER = 0x3F76E4;

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.GRILL_LAVA.get(), GrillLavaParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.GRILL_SPIT.get(), GrillSpitParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.CRATE.get(), CrateScreen::new);
        event.register(MenuRegistry.SATCHEL.get(), SatchelScreen::new);
    }
}
