package com.halex.createrancher.content.manure;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

// Dung. Stacks in layers like snow.
public class DungBlock extends SnowLayerBlock {

    public static final MapCodec<DungBlock> CODEC = simpleCodec(DungBlock::new);

    public DungBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<SnowLayerBlock> codec() {
        return (MapCodec<SnowLayerBlock>) (MapCodec<?>) CODEC;
    }

    // Doesn't melt
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }
}
