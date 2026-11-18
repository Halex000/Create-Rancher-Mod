package com.halex.createrancher.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

// Lets a processor list's processors be swapped out
@Mixin(StructureProcessorList.class)
public interface StructureProcessorListAccessor {

    @Mutable
    @Accessor("list")
    void createrancher$setList(List<StructureProcessor> list);
}
