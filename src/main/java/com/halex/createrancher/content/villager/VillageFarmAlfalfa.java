package com.halex.createrancher.content.villager;

import java.util.ArrayList;
import java.util.List;

import com.halex.createrancher.mixin.StructureProcessorListAccessor;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

// Lets village farm plots grow alfalfa
public class VillageFarmAlfalfa {

    private static final List<String> FARMS = List.of("farm_plains", "farm_desert", "farm_savanna", "farm_snowy",
            "farm_taiga");

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VillageFarmAlfalfa::onServerAboutToStart);
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Registry<StructureProcessorList> lists = event.getServer()
                .registryAccess()
                .registryOrThrow(Registries.PROCESSOR_LIST);
        for (String farm : FARMS)
            lists.getOptional(ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.withDefaultNamespace(farm)))
                    .ifPresent(VillageFarmAlfalfa::addAlfalfa);
    }

    // Adds the alfalfa processor after the farm's own crop rules
    private static void addAlfalfa(StructureProcessorList list) {
        List<StructureProcessor> processors = new ArrayList<>(list.list());
        processors.add(AlfalfaFarmProcessor.INSTANCE);
        ((StructureProcessorListAccessor) list).createrancher$setList(List.copyOf(processors));
    }
}
