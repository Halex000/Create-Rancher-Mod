package com.halex.createrancher.content.villager;

import javax.annotation.Nullable;

import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.PlacementRegistry;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

// Turns some of the wheat a village farm plot places into alfalfa
public class AlfalfaFarmProcessor extends StructureProcessor {

    public static final AlfalfaFarmProcessor INSTANCE = new AlfalfaFarmProcessor();
    public static final MapCodec<AlfalfaFarmProcessor> CODEC = MapCodec.unit(INSTANCE);
    private static final float CHANCE = 0.2f;

    private AlfalfaFarmProcessor() {
    }

    // Gives each wheat block its own chance of becoming alfalfa
    @Override
    public StructureBlockInfo process(LevelReader level, BlockPos offset, BlockPos pos, StructureBlockInfo blockInfo,
                                      StructureBlockInfo relativeBlockInfo, StructurePlaceSettings settings,
                                      @Nullable StructureTemplate template) {
        if (!relativeBlockInfo.state()
                .is(Blocks.WHEAT) || new XoroshiroRandomSource(relativeBlockInfo.pos()
                .asLong()).nextFloat() >= CHANCE)
            return relativeBlockInfo;
        return new StructureBlockInfo(relativeBlockInfo.pos(), BlockRegistry.ALFALFA_CROP.getDefaultState(),
                relativeBlockInfo.nbt());
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return PlacementRegistry.ALFALFA_FARM.get();
    }
}
