package com.halex.createrancher.content.alfalfa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HayBlock;
import net.minecraft.world.level.block.state.BlockState;

// A bale of alfalfa. Works like a hay bale but horses and llamas won't eat it.
public class AlfalfaBaleBlock extends HayBlock {

    public AlfalfaBaleBlock(Properties properties) {
        super(properties);
    }

    // Makes it burn like a hay bale
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 20;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }
}
