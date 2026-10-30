package com.halex.createrancher;

import com.halex.createrancher.content.herder.ConfigureMechanicalHerderPacket;
import com.halex.createrancher.compat.jei.JeiConversions;
import com.halex.createrancher.content.drain.LivestockMilking;
import com.halex.createrancher.content.milk.CowCans;
import com.halex.createrancher.content.milk.MilkTooltips;
import com.halex.createrancher.content.shearer.Shearing;
import com.halex.createrancher.content.spout.SpoutBuckets;
import com.halex.createrancher.content.joist.JoistPacks;
import com.halex.createrancher.content.joist.JoistWoods;
import com.halex.createrancher.content.villager.VillagerTradeChanges;
import com.halex.createrancher.content.fluid.LavaInteractions;
import com.halex.createrancher.content.manure.ManureItem;
import com.halex.createrancher.index.ArmPointTypeRegistry;
import com.halex.createrancher.index.AttachmentRegistry;
import com.halex.createrancher.index.BlockEntityRegistry;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.DataComponentRegistry;
import com.halex.createrancher.index.EntityRegistry;
import com.halex.createrancher.index.FluidRegistry;
import com.halex.createrancher.index.MenuRegistry;
import com.halex.createrancher.index.ParticleRegistry;
import com.halex.createrancher.index.PlacementRegistry;
import com.halex.createrancher.index.PotionRegistry;
import com.halex.createrancher.index.RecipeRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.halex.createrancher.content.burner.LiquidBlazeBurnerFuels;
import com.halex.createrancher.content.burner.LiquidBlazeBurnerFuelsPacket;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.mojang.logging.LogUtils;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(CreateRancher.MOD_ID)
public class CreateRancher {

    public static final String MOD_ID = "createrancher";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .defaultCreativeTab(CreateRancherTabs.BASE.getKey())
            .setTooltipModifierFactory(item -> new ParagraphedDescription(item, Palette.STANDARD_CREATE)
                    .andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    public CreateRancher(IEventBus eventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        REGISTRATE.registerEventListeners(eventBus);

        BlockRegistry.register();
        BlockEntityRegistry.register();
        ItemRegistry.register(eventBus);
        FluidRegistry.register();
        MenuRegistry.register(eventBus);
        EntityRegistry.register(eventBus);
        PotionRegistry.register(eventBus);
        DataComponentRegistry.register(eventBus);
        ArmPointTypeRegistry.register(eventBus);
        AttachmentRegistry.register(eventBus);
        ParticleRegistry.register(eventBus);
        PlacementRegistry.register(eventBus);
        RecipeRegistry.register(eventBus);
        CreateRancherTabs.register(eventBus);
        JoistWoods.register(eventBus);
        JoistPacks.register(eventBus);
        if (FMLEnvironment.dist == Dist.CLIENT)
            CreateRancherClient.init();

        eventBus.addListener(this::setup);
        LivestockMilking.register();
        Shearing.register();
        SpoutBuckets.register();
        MilkTooltips.register();
        CowCans.register();
        VillagerTradeChanges.register();
        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, event -> event.addListener(
                new LiquidBlazeBurnerFuels()));
        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event -> event.getRelevantPlayers()
                .forEach(player -> PacketDistributor.sendToPlayer(player, new LiquidBlazeBurnerFuelsPacket(
                        LiquidBlazeBurnerFuels.all()))));
        eventBus.addListener(this::setupClient);
        eventBus.addListener(this::registerPayloads);
        eventBus.addListener(this::registerCapabilities);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ManureItem.registerDispenserBehaviour(ItemRegistry.MANURE.get()));
        event.enqueueWork(() -> BoilerHeater.REGISTRY.register(BlockRegistry.LIQUID_BLAZE_BURNER.get(),
                BoilerHeater.BLAZE_BURNER));
        event.enqueueWork(LavaInteractions::register);
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityRegistry.GRILL.get(),
                (grill, side) -> grill.getItemHandler());
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new FluidBucketWrapper(stack),
                FluidRegistry.TANNIN.get()
                        .getBucket(), FluidRegistry.BIOFUEL.get()
                        .getBucket(), FluidRegistry.MUSHROOM_STEW.get()
                        .getBucket(), FluidRegistry.RABBIT_STEW.get()
                        .getBucket(), FluidRegistry.BEETROOT_SOUP.get()
                        .getBucket(), FluidRegistry.BEEF_STEW.get()
                        .getBucket(), FluidRegistry.CHICKEN_STEW.get()
                        .getBucket(), FluidRegistry.PORK_STEW.get()
                        .getBucket(), FluidRegistry.MUTTON_STEW.get()
                        .getBucket(), ItemRegistry.CHOCOLATE_MILK_BUCKET.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlockEntityRegistry.LIQUID_BLAZE_BURNER.get(),
                (burner, side) -> burner.getFuelHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityRegistry.MECHANICAL_FISHER.get(),
                (fisher, side) -> fisher.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityRegistry.CRATE.get(),
                (crate, side) -> new InvWrapper(crate));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityRegistry.AUGER.get(),
                (auger, side) -> auger.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityRegistry.SMART_DEPOT.get(),
                (depot, side) -> side == Direction.DOWN ? null : depot.getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BlockEntityRegistry.MECHANICAL_SHEARER.get(),
                (shearer, side) -> shearer.getItemHandler(side != null));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BlockEntityRegistry.LIVESTOCK_DRAIN.get(),
                (drain, side) -> side == Direction.UP ? null : drain.getFluidHandler());
    }

    private void setupClient(final FMLClientSetupEvent event) {
        if (ModList.get()
                .isLoaded("jei"))
            event.enqueueWork(JeiConversions::register);
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(ConfigureMechanicalHerderPacket.TYPE, ConfigureMechanicalHerderPacket.STREAM_CODEC,
                        ConfigureMechanicalHerderPacket::handle)
                .playToClient(LiquidBlazeBurnerFuelsPacket.TYPE, LiquidBlazeBurnerFuelsPacket.STREAM_CODEC,
                        LiquidBlazeBurnerFuelsPacket::handle);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
