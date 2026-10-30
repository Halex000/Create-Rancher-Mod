package com.halex.createrancher.index;

import static com.halex.createrancher.CreateRancher.REGISTRATE;

import com.halex.createrancher.content.biofuel.BiofuelFluidType;
import com.halex.createrancher.content.milk.ChocolateMilkFluid;
import com.halex.createrancher.content.stew.StewFluidType;
import com.simibubi.create.content.fluids.VirtualFluid;
import com.halex.createrancher.content.tannin.TanninFluidType;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public class FluidRegistry {

    public static final FluidEntry<BaseFlowingFluid.Flowing> TANNIN = REGISTRATE
            .fluid("tannin", ResourceLocation.withDefaultNamespace("block/water_still"),
                    ResourceLocation.withDefaultNamespace("block/water_flow"), TanninFluidType::new)
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .properties(p -> p.viscosity(1200)
                    .density(1050))
            .fluidProperties(p -> p.levelDecreasePerBlock(2)
                    .tickRate(10)
                    .slopeFindDistance(3)
                    .explosionResistance(100))
            .source(BaseFlowingFluid.Source::new)
            .block()
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
            .blockstate(NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .build()
            .bucket()
            .model(NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .build()
            .register();

    public static final FluidEntry<BaseFlowingFluid.Flowing> BIOFUEL = REGISTRATE
            .fluid("biofuel", ResourceLocation.withDefaultNamespace("block/water_still"),
                    ResourceLocation.withDefaultNamespace("block/water_flow"), BiofuelFluidType::new)
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .properties(p -> p.viscosity(1200)
                    .density(1050))
            .fluidProperties(p -> p.levelDecreasePerBlock(2)
                    .tickRate(10)
                    .slopeFindDistance(3)
                    .explosionResistance(100))
            .source(BaseFlowingFluid.Source::new)
            .block()
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN))
            .blockstate(NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .build()
            .bucket()
            .model(NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .build()
            .register();

    public static final FluidEntry<BaseFlowingFluid.Flowing> MUSHROOM_STEW = stew("mushroom_stew", 0x8A6A48,
            MapColor.TERRACOTTA_BROWN);
    public static final FluidEntry<BaseFlowingFluid.Flowing> RABBIT_STEW = stew("rabbit_stew", 0x8A5A2E,
            MapColor.TERRACOTTA_ORANGE);
    public static final FluidEntry<BaseFlowingFluid.Flowing> BEETROOT_SOUP = stew("beetroot_soup", 0x7A1C1C,
            MapColor.TERRACOTTA_RED);
    public static final FluidEntry<BaseFlowingFluid.Flowing> BEEF_STEW = stew("beef_stew", 0x6E3C24,
            MapColor.TERRACOTTA_BROWN);
    public static final FluidEntry<BaseFlowingFluid.Flowing> CHICKEN_STEW = stew("chicken_stew", 0xA8823F,
            MapColor.TERRACOTTA_YELLOW);
    public static final FluidEntry<BaseFlowingFluid.Flowing> PORK_STEW = stew("pork_stew", 0x9C6450,
            MapColor.TERRACOTTA_PINK);
    public static final FluidEntry<BaseFlowingFluid.Flowing> MUTTON_STEW = stew("mutton_stew", 0x7D5544,
            MapColor.TERRACOTTA_GRAY);

    public static final FluidEntry<VirtualFluid> SUSPICIOUS_STEW = REGISTRATE
            .virtualFluid("suspicious_stew", (properties, still, flowing) -> new StewFluidType(properties, still,
                    flowing, 0x8A6A48), VirtualFluid::createSource, VirtualFluid::createFlowing)
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .properties(p -> p.viscosity(1500)
                    .density(1400))
            .register();

    public static final FluidEntry<ChocolateMilkFluid> CHOCOLATE_MILK = REGISTRATE
            .virtualFluid("chocolate_milk", (properties, still, flowing) -> new StewFluidType(properties, still,
                    flowing, 0xA45F4A), ChocolateMilkFluid::createSource, ChocolateMilkFluid::createFlowing)
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .properties(p -> p.viscosity(1024)
                    .density(1024))
            .register();

    // Makes a stew fluid
    private static FluidEntry<BaseFlowingFluid.Flowing> stew(String name, int fogColour, MapColor mapColour) {
        return REGISTRATE.standardFluid(name, (properties, still, flowing) -> new StewFluidType(properties, still,
                        flowing, fogColour))
                .setData(ProviderType.LANG, NonNullBiConsumer.noop())
                .properties(p -> p.viscosity(1500)
                        .density(1400))
                .fluidProperties(p -> p.levelDecreasePerBlock(2)
                        .tickRate(30)
                        .slopeFindDistance(2)
                        .explosionResistance(100))
                .source(BaseFlowingFluid.Source::new)
                .block()
                .properties(p -> p.mapColor(mapColour))
                .blockstate(NonNullBiConsumer.noop())
                .setData(ProviderType.LANG, NonNullBiConsumer.noop())
                .build()
                .bucket()
                .model(NonNullBiConsumer.noop())
                .setData(ProviderType.LANG, NonNullBiConsumer.noop())
                .build()
                .register();
    }

    public static void register() {
    }
}
