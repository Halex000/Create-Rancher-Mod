package com.halex.createrancher.index;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.alfalfa.NearBiomeFilter;
import com.halex.createrancher.content.villager.AlfalfaFarmProcessor;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PlacementRegistry {

    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, CreateRancher.MOD_ID);

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<NearBiomeFilter>> NEAR_BIOME =
            PLACEMENT_MODIFIERS.register("near_biome", () -> () -> NearBiomeFilter.CODEC);

    private static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, CreateRancher.MOD_ID);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<AlfalfaFarmProcessor>>
            ALFALFA_FARM = STRUCTURE_PROCESSORS.register("alfalfa_farm", () -> () -> AlfalfaFarmProcessor.CODEC);

    public static void register(IEventBus modBus) {
        PLACEMENT_MODIFIERS.register(modBus);
        STRUCTURE_PROCESSORS.register(modBus);
    }
}
