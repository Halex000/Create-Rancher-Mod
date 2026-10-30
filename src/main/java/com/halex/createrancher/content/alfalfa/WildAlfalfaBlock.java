package com.halex.createrancher.content.alfalfa;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Alfalfa that grows wild near water. Drops Alfalfa Seeds.
public class WildAlfalfaBlock extends BushBlock {

    public static final MapCodec<WildAlfalfaBlock> CODEC = simpleCodec(WildAlfalfaBlock::new);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

    public WildAlfalfaBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WildAlfalfaBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(level, pos);
        return SHAPE.move(offset.x, offset.y, offset.z);
    }
}
