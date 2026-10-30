package com.halex.createrancher.content.manure;

import javax.annotation.Nullable;

import com.halex.createrancher.index.BlockRegistry;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MudBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.util.TriState;

// A block of manure. Till it to make Manure Farmland.
public class ManureBlock extends MudBlock {

    public static final MapCodec<ManureBlock> CODEC = simpleCodec(ManureBlock::new);
    private static final VoxelShape COLLISION_SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public ManureBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<MudBlock> codec() {
        return (MapCodec<MudBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    // Plants grow on it, and sugar cane doesn't need water
    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing,
                                    BlockState plant) {
        if (facing == Direction.UP && plant.getBlock() instanceof SugarCaneBlock)
            return TriState.TRUE;
        return super.canSustainPlant(state, level, soilPosition, facing, plant);
    }

    @Nullable
    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility,
                                           boolean simulate) {
        if (itemAbility == ItemAbilities.HOE_TILL && context.getLevel()
                .getBlockState(context.getClickedPos()
                        .above())
                .isAir())
            return BlockRegistry.MANURE_FARMLAND.getDefaultState();
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}
