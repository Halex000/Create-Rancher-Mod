package com.halex.createrancher.index;

import static com.halex.createrancher.CreateRancher.REGISTRATE;

import com.halex.createrancher.CommonConfig;
import com.halex.createrancher.content.alfalfa.AlfalfaBaleBlock;
import com.halex.createrancher.content.alfalfa.AlfalfaCropBlock;
import com.halex.createrancher.content.burner.LiquidBlazeBurnerBlock;
import com.halex.createrancher.content.burner.LiquidBlazeBurnerMovementBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.halex.createrancher.content.alfalfa.WildAlfalfaBlock;
import com.halex.createrancher.content.crate.CrateBlock;
import com.halex.createrancher.content.crate.CrateItem;
import com.halex.createrancher.content.depot.SmartDepotBlock;
import com.halex.createrancher.content.drain.LivestockDrainBlock;
import com.halex.createrancher.content.drain.client.LivestockDrainCTBehaviour;
import com.halex.createrancher.content.feeder.MechanicalFeederBlock;
import com.halex.createrancher.content.feeder.MechanicalFeederItem;
import com.halex.createrancher.content.fisher.MechanicalFisherBlock;
import com.halex.createrancher.content.fisher.MechanicalFisherItem;
import com.halex.createrancher.content.shearer.MechanicalShearerBlock;
import com.halex.createrancher.content.herder.MechanicalHerderBlock;
import com.halex.createrancher.content.herder.MechanicalHerderItem;
import com.halex.createrancher.content.grill.CampfireGrillBlock;
import com.halex.createrancher.content.grill.CauldronGrillBlock;
import com.halex.createrancher.content.manure.DungBlock;
import com.halex.createrancher.content.manure.ManureBlock;
import com.halex.createrancher.content.manure.ManureFarmlandBlock;
import com.halex.createrancher.content.auger.AugerBlock;
import com.halex.createrancher.content.auger.AugerItem;
import com.halex.createrancher.content.wood.StainedWood;
import com.halex.createrancher.content.wood.StrippableLogBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class BlockRegistry {

    public static final BlockEntry<MechanicalHerderBlock> MECHANICAL_HERDER = REGISTRATE
            .block("mechanical_herder", MechanicalHerderBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
            .blockstate(NonNullBiConsumer.noop())
            .loot(NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .onRegister(block -> BlockStressValues.IMPACTS.register(block,
                    () -> CommonConfig.MECHANICAL_HERDER_STRESS_IMPACT.get()))
            .item(MechanicalHerderItem::new)
            .model(NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .build()
            .register();

    public static final BlockEntry<MechanicalFeederBlock> MECHANICAL_FEEDER = REGISTRATE
            .block("mechanical_feeder", MechanicalFeederBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
            .blockstate(NonNullBiConsumer.noop())
            .loot(NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .onRegister(block -> BlockStressValues.IMPACTS.register(block,
                    () -> CommonConfig.MECHANICAL_FEEDER_STRESS_IMPACT.get()))
            .item(MechanicalFeederItem::new)
            .model(NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .build()
            .register();

    public static final BlockEntry<MechanicalShearerBlock> MECHANICAL_SHEARER = REGISTRATE
            .block("mechanical_shearer", MechanicalShearerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
            .blockstate(NonNullBiConsumer.noop())
            .loot(NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .onRegister(block -> BlockStressValues.IMPACTS.register(block,
                    () -> CommonConfig.MECHANICAL_SHEARER_STRESS_IMPACT.get()))
            .item()
            .model(NonNullBiConsumer.noop())
            .setData(ProviderType.LANG, NonNullBiConsumer.noop())
            .build()
            .register();

    public static final BlockEntry<MechanicalFisherBlock> MECHANICAL_FISHER = REGISTRATE
            .block("mechanical_fisher", MechanicalFisherBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_RED)
                    .noOcclusion())
            .transform(BlockRegistry::handmade)
            .item(MechanicalFisherItem::new)
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<AugerBlock> AUGER = REGISTRATE
            .block("auger", AugerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.COLOR_GRAY)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion())
            .transform(BlockRegistry::handmade)
            .item(AugerItem::new)
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<SmartDepotBlock> SMART_DEPOT = REGISTRATE
            .block("smart_depot", SmartDepotBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW)
                    .noOcclusion())
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<LivestockDrainBlock> LIVESTOCK_DRAIN = REGISTRATE
            .block("livestock_drain", LivestockDrainBlock::new)
            .initialProperties(SharedProperties::copperMetal)
            .properties(p -> p.noOcclusion())
            .transform(BlockRegistry::handmade)
            .onRegister(CreateRegistrate.connectedTextures(() -> new LivestockDrainCTBehaviour()))
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<WildAlfalfaBlock> WILD_ALFALFA = REGISTRATE
            .block("wild_alfalfa", WildAlfalfaBlock::new)
            .properties(p -> p.mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY))
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<AlfalfaCropBlock> ALFALFA_CROP = REGISTRATE
            .block("alfalfa_crop", AlfalfaCropBlock::new)
            .properties(p -> p.mapColor(MapColor.PLANT)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.CROP)
                    .pushReaction(PushReaction.DESTROY))
            .transform(BlockRegistry::handmade)
            .register();

    public static final BlockEntry<AlfalfaBaleBlock> ALFALFA_BALE = REGISTRATE
            .block("alfalfa_bale", AlfalfaBaleBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .instrument(NoteBlockInstrument.BANJO)
                    .strength(.5f)
                    .sound(SoundType.GRASS))
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<DungBlock> DUNG = REGISTRATE
            .block("dung", DungBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN)
                    .forceSolidOn()
                    .strength(.2f)
                    .sound(SoundType.MUD)
                    .isViewBlocking((state, level, pos) -> state.getValue(SnowLayerBlock.LAYERS) >= 8)
                    .pushReaction(PushReaction.DESTROY))
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<ManureBlock> MANURE_BLOCK = REGISTRATE
            .block("manure_block", ManureBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN)
                    .strength(.5f)
                    .sound(SoundType.MUD)
                    .isValidSpawn((state, level, pos, type) -> true)
                    .isRedstoneConductor((state, level, pos) -> true)
                    .isViewBlocking((state, level, pos) -> true)
                    .isSuffocating((state, level, pos) -> true))
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<ManureFarmlandBlock> MANURE_FARMLAND = REGISTRATE
            .block("manure_farmland", ManureFarmlandBlock::new)
            .properties(p -> p.mapColor(MapColor.COLOR_BROWN)
                    .strength(.5f)
                    .sound(SoundType.MUD)
                    .isViewBlocking((state, level, pos) -> true)
                    .isSuffocating((state, level, pos) -> true))
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<RotatedPillarBlock> STRIPPED_STAINED_LOG = woodBlock("stripped_stained_log",
            RotatedPillarBlock::new);
    public static final BlockEntry<RotatedPillarBlock> STRIPPED_STAINED_WOOD = woodBlock("stripped_stained_wood",
            RotatedPillarBlock::new);
    public static final BlockEntry<StrippableLogBlock> STAINED_LOG = woodBlock("stained_log",
            p -> new StrippableLogBlock(STRIPPED_STAINED_LOG, p));
    public static final BlockEntry<StrippableLogBlock> STAINED_WOOD = woodBlock("stained_wood",
            p -> new StrippableLogBlock(STRIPPED_STAINED_WOOD, p));
    public static final BlockEntry<Block> STAINED_PLANKS = woodBlock("stained_planks", Block::new);
    public static final BlockEntry<StairBlock> STAINED_STAIRS = woodBlock("stained_stairs",
            p -> new StairBlock(STAINED_PLANKS.getDefaultState(), p));
    public static final BlockEntry<SlabBlock> STAINED_SLAB = woodBlock("stained_slab", SlabBlock::new);

    public static final BlockEntry<FenceBlock> STAINED_FENCE = REGISTRATE
            .block("stained_fence", FenceBlock::new)
            .properties(p -> woodProperties(p).forceSolidOn())
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<FenceGateBlock> STAINED_FENCE_GATE = REGISTRATE
            .block("stained_fence_gate", p -> new FenceGateBlock(StainedWood.TYPE, p))
            .properties(p -> woodProperties(p).forceSolidOn())
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<DoorBlock> STAINED_DOOR = REGISTRATE
            .block("stained_door", p -> new DoorBlock(StainedWood.SET, p))
            .properties(p -> woodProperties(p).strength(3)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY))
            .transform(BlockRegistry::handmade)
            .item(DoubleHighBlockItem::new)
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<TrapDoorBlock> STAINED_TRAPDOOR = REGISTRATE
            .block("stained_trapdoor", p -> new TrapDoorBlock(StainedWood.SET, p))
            .properties(p -> woodProperties(p).strength(3)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false))
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<PressurePlateBlock> STAINED_PRESSURE_PLATE = REGISTRATE
            .block("stained_pressure_plate", p -> new PressurePlateBlock(StainedWood.SET, p))
            .properties(p -> woodProperties(p).strength(.5f)
                    .forceSolidOn()
                    .noCollission()
                    .pushReaction(PushReaction.DESTROY))
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<ButtonBlock> STAINED_BUTTON = REGISTRATE
            .block("stained_button", p -> new ButtonBlock(StainedWood.SET, 30, p))
            .properties(p -> p.strength(.5f)
                    .noCollission()
                    .pushReaction(PushReaction.DESTROY))
            .transform(BlockRegistry::handmade)
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<StandingSignBlock> STAINED_SIGN = signBlock("stained_sign",
            p -> new StandingSignBlock(StainedWood.TYPE, p));
    public static final BlockEntry<WallSignBlock> STAINED_WALL_SIGN = signBlock("stained_wall_sign",
            p -> new WallSignBlock(StainedWood.TYPE, p));
    public static final BlockEntry<CeilingHangingSignBlock> STAINED_HANGING_SIGN = signBlock("stained_hanging_sign",
            p -> new CeilingHangingSignBlock(StainedWood.TYPE, p));
    public static final BlockEntry<WallHangingSignBlock> STAINED_WALL_HANGING_SIGN = signBlock(
            "stained_wall_hanging_sign", p -> new WallHangingSignBlock(StainedWood.TYPE, p));

    private static <T extends Block> BlockEntry<T> woodBlock(String name,
                                                            NonNullFunction<BlockBehaviour.Properties, T> factory) {
        return REGISTRATE.block(name, factory)
                .properties(BlockRegistry::woodProperties)
                .transform(BlockRegistry::handmade)
                .item()
                .transform(BlockRegistry::handmadeItem)
                .build()
                .register();
    }

    private static <T extends Block> BlockEntry<T> signBlock(String name,
                                                            NonNullFunction<BlockBehaviour.Properties, T> factory) {
        return REGISTRATE.block(name, factory)
                .properties(p -> woodProperties(p).forceSolidOn()
                        .noCollission()
                        .strength(1))
                .transform(BlockRegistry::handmade)
                .register();
    }

    private static BlockBehaviour.Properties woodProperties(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.TERRACOTTA_RED)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2, 3)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    public static final BlockEntry<CrateBlock> CRATE = REGISTRATE
            .block("crate", CrateBlock::new)
            .properties(p -> woodProperties(p).strength(2)
                    .pushReaction(PushReaction.DESTROY))
            .transform(BlockRegistry::handmade)
            .item(CrateItem::new)
            .properties(p -> p.stacksTo(1))
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    public static final BlockEntry<CampfireGrillBlock> CAMPFIRE_GRILL = REGISTRATE
            .block("campfire_grill", p -> new CampfireGrillBlock(false, p))
            .properties(p -> grillProperties(p, 15))
            .transform(BlockRegistry::handmade)
            .register();

    public static final BlockEntry<CampfireGrillBlock> SOUL_CAMPFIRE_GRILL = REGISTRATE
            .block("soul_campfire_grill", p -> new CampfireGrillBlock(true, p))
            .properties(p -> grillProperties(p, 10))
            .transform(BlockRegistry::handmade)
            .register();

    public static final BlockEntry<CauldronGrillBlock> CAULDRON_GRILL = REGISTRATE
            .block("cauldron_grill", CauldronGrillBlock::new)
            .initialProperties(() -> Blocks.CAULDRON)
            .properties(p -> p.lightLevel(state -> state.getValue(CauldronGrillBlock.CONTENTS)
                    == CauldronGrillBlock.Contents.LAVA ? 15 : 0))
            .transform(BlockRegistry::handmade)
            .register();

    public static final BlockEntry<LiquidBlazeBurnerBlock> LIQUID_BLAZE_BURNER = REGISTRATE
            .block("liquid_blaze_burner", LiquidBlazeBurnerBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.mapColor(MapColor.COLOR_ORANGE)
                    .lightLevel(BlazeBurnerBlock::getLight)
                    .noOcclusion())
            .transform(BlockRegistry::handmade)
            .onRegister(MovementBehaviour.movementBehaviour(new LiquidBlazeBurnerMovementBehaviour()))
            .item()
            .transform(BlockRegistry::handmadeItem)
            .build()
            .register();

    private static BlockBehaviour.Properties grillProperties(BlockBehaviour.Properties properties, int light) {
        return properties.mapColor(MapColor.PODZOL)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2)
                .sound(SoundType.WOOD)
                .lightLevel(state -> state.getValue(CampfireGrillBlock.LIT) ? light : 0)
                .noOcclusion()
                .ignitedByLava();
    }

    // For blocks with handmade assets in src/main/resources
    private static <T extends Block> BlockBuilder<T, CreateRegistrate> handmade(
            BlockBuilder<T, CreateRegistrate> builder) {
        return builder.blockstate(NonNullBiConsumer.noop())
                .loot(NonNullBiConsumer.noop())
                .setData(ProviderType.LANG, NonNullBiConsumer.noop());
    }

    private static <T extends Block, I extends BlockItem> ItemBuilder<I, BlockBuilder<T, CreateRegistrate>> handmadeItem(
            ItemBuilder<I, BlockBuilder<T, CreateRegistrate>> builder) {
        return builder.model(NonNullBiConsumer.noop())
                .setData(ProviderType.LANG, NonNullBiConsumer.noop());
    }

    public static void register() {
    }
}
