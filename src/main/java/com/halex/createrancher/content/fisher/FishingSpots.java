package com.halex.createrancher.content.fisher;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;

// Picks where a Mechanical Fisher fishes
public class FishingSpots {

    // The top water block of the clicked column, or null
    @Nullable
    public static BlockPos surfaceOf(BlockGetter level, BlockPos clicked) {
        BlockPos pos = clicked;
        if (!isWater(level, pos)) {
            pos = pos.above();
            if (!isWater(level, pos))
                return null;
        }
        int limit = level.getMaxBuildHeight() - pos.getY();
        while (limit-- > 0 && isWater(level, pos.above()))
            pos = pos.above();
        return isFishable(level, pos) ? pos : null;
    }

    // Water with air right above it
    public static boolean isFishable(BlockGetter level, BlockPos surface) {
        return isWater(level, surface) && level.getBlockState(surface.above())
                .isAir();
    }

    private static boolean isWater(BlockGetter level, BlockPos pos) {
        return level.getFluidState(pos)
                .is(FluidTags.WATER);
    }
}
