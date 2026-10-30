package com.halex.createrancher.content.alfalfa;

import com.halex.createrancher.index.PlacementRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

// Only lets a feature generate near certain biomes
public class NearBiomeFilter extends PlacementFilter {

    public static final MapCodec<NearBiomeFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    TagKey.hashedCodec(Registries.BIOME)
                            .fieldOf("biomes")
                            .forGetter(filter -> filter.biomes),
                    Codec.intRange(0, 32)
                            .fieldOf("radius")
                            .forGetter(filter -> filter.radius))
            .apply(instance, NearBiomeFilter::new));

    private final TagKey<Biome> biomes;
    private final int radius;

    public NearBiomeFilter(TagKey<Biome> biomes, int radius) {
        this.biomes = biomes;
        this.radius = radius;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        for (int x = -radius; x <= radius; x += 4)
            for (int z = -radius; z <= radius; z += 4)
                if (x * x + z * z <= radius * radius && context.getLevel()
                        .getBiome(pos.offset(x, 0, z))
                        .is(biomes))
                    return true;
        return false;
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementRegistry.NEAR_BIOME.get();
    }
}
