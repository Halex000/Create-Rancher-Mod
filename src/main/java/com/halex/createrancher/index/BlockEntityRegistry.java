package com.halex.createrancher.index;

import static com.halex.createrancher.CreateRancher.REGISTRATE;

import com.halex.createrancher.content.burner.LiquidBlazeBurnerBlockEntity;
import com.halex.createrancher.content.burner.LiquidBlazeBurnerRenderer;
import com.halex.createrancher.content.burner.LiquidBlazeBurnerVisual;
import com.halex.createrancher.content.feeder.MechanicalFeederBlockEntity;
import com.halex.createrancher.content.crate.CrateBlockEntity;
import com.halex.createrancher.content.depot.SmartDepotBlockEntity;
import com.halex.createrancher.content.depot.SmartDepotRenderer;
import com.halex.createrancher.content.drain.LivestockDrainBlockEntity;
import com.halex.createrancher.content.drain.client.LivestockDrainRenderer;
import com.halex.createrancher.content.fisher.MechanicalFisherBlockEntity;
import com.halex.createrancher.content.shearer.MechanicalShearerBlockEntity;
import com.halex.createrancher.content.shearer.client.MechanicalShearerRenderer;
import com.halex.createrancher.content.shearer.client.MechanicalShearerVisual;
import com.halex.createrancher.content.fisher.client.MechanicalFisherRenderer;
import com.halex.createrancher.content.fisher.client.MechanicalFisherVisual;
import com.halex.createrancher.content.grill.GrillBlockEntity;
import com.halex.createrancher.content.grill.GrillRenderer;
import com.halex.createrancher.content.herder.MechanicalHerderBlockEntity;
import com.halex.createrancher.content.herder.MechanicalHerderRenderer;
import com.halex.createrancher.content.herder.MechanicalHerderVisual;
import com.halex.createrancher.content.auger.AugerBlockEntity;
import com.halex.createrancher.content.auger.AugerRenderer;
import com.halex.createrancher.content.auger.AugerVisual;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class BlockEntityRegistry {

    public static final BlockEntityEntry<MechanicalHerderBlockEntity> MECHANICAL_HERDER = REGISTRATE
            .blockEntity("mechanical_herder", MechanicalHerderBlockEntity::new)
            .visual(() -> MechanicalHerderVisual::new, false)
            .validBlocks(BlockRegistry.MECHANICAL_HERDER)
            .renderer(() -> MechanicalHerderRenderer::new)
            .register();

    public static final BlockEntityEntry<MechanicalFeederBlockEntity> MECHANICAL_FEEDER = REGISTRATE
            .blockEntity("mechanical_feeder", MechanicalFeederBlockEntity::new)
            .visual(() -> ArmVisual::new)
            .validBlocks(BlockRegistry.MECHANICAL_FEEDER)
            .renderer(() -> ArmRenderer::new)
            .register();

    public static final BlockEntityEntry<MechanicalShearerBlockEntity> MECHANICAL_SHEARER = REGISTRATE
            .blockEntity("mechanical_shearer", MechanicalShearerBlockEntity::new)
            .visual(() -> MechanicalShearerVisual::new)
            .validBlocks(BlockRegistry.MECHANICAL_SHEARER)
            .renderer(() -> MechanicalShearerRenderer::new)
            .register();

    public static final BlockEntityEntry<GrillBlockEntity> GRILL = REGISTRATE
            .blockEntity("campfire_grill", GrillBlockEntity::new)
            .validBlocks(BlockRegistry.CAMPFIRE_GRILL, BlockRegistry.SOUL_CAMPFIRE_GRILL, BlockRegistry.CAULDRON_GRILL)
            .renderer(() -> GrillRenderer::new)
            .register();

    public static final BlockEntityEntry<MechanicalFisherBlockEntity> MECHANICAL_FISHER = REGISTRATE
            .blockEntity("mechanical_fisher", MechanicalFisherBlockEntity::new)
            .visual(() -> MechanicalFisherVisual::new)
            .validBlocks(BlockRegistry.MECHANICAL_FISHER)
            .renderer(() -> MechanicalFisherRenderer::new)
            .register();

    public static final BlockEntityEntry<CrateBlockEntity> CRATE = REGISTRATE
            .<CrateBlockEntity>blockEntity("crate", (type, pos, state) -> new CrateBlockEntity(type, pos, state))
            .validBlocks(BlockRegistry.CRATE)
            .register();

    public static final BlockEntityEntry<LiquidBlazeBurnerBlockEntity> LIQUID_BLAZE_BURNER = REGISTRATE
            .blockEntity("liquid_blaze_burner", LiquidBlazeBurnerBlockEntity::new)
            .visual(() -> LiquidBlazeBurnerVisual::new)
            .validBlocks(BlockRegistry.LIQUID_BLAZE_BURNER)
            .renderer(() -> LiquidBlazeBurnerRenderer::new)
            .register();

    public static final BlockEntityEntry<AugerBlockEntity> AUGER = REGISTRATE
            .blockEntity("auger", AugerBlockEntity::new)
            .visual(() -> AugerVisual::new)
            .validBlocks(BlockRegistry.AUGER)
            .renderer(() -> AugerRenderer::new)
            .register();

    public static final BlockEntityEntry<SmartDepotBlockEntity> SMART_DEPOT = REGISTRATE
            .blockEntity("smart_depot", SmartDepotBlockEntity::new)
            .validBlocks(BlockRegistry.SMART_DEPOT)
            .renderer(() -> SmartDepotRenderer::new)
            .register();

    public static final BlockEntityEntry<LivestockDrainBlockEntity> LIVESTOCK_DRAIN = REGISTRATE
            .blockEntity("livestock_drain", LivestockDrainBlockEntity::new)
            .validBlocks(BlockRegistry.LIVESTOCK_DRAIN)
            .renderer(() -> LivestockDrainRenderer::new)
            .register();

    public static void register() {
    }
}
