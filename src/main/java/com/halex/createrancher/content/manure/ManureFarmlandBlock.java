package com.halex.createrancher.content.manure;

import com.halex.createrancher.index.BlockRegistry;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;

// Farmland made from manure. Always moist and can't be trampled.
public class ManureFarmlandBlock extends FarmBlock {

    public static final MapCodec<ManureFarmlandBlock> CODEC = simpleCodec(ManureFarmlandBlock::new);

    public ManureFarmlandBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(MOISTURE, MAX_MOISTURE));
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<FarmBlock> codec() {
        return (MapCodec<FarmBlock>) (MapCodec<?>) CODEC;
    }

    // Never dries out
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    // Can't be trampled
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 1, entity.damageSources()
                .fall());
    }

    // Turns back into manure when covered
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos))
            level.setBlockAndUpdate(pos, pushEntitiesUp(state, BlockRegistry.MANURE_BLOCK.getDefaultState(), level,
                    pos));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos()) ? defaultBlockState()
                : BlockRegistry.MANURE_BLOCK.getDefaultState();
    }

    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing,
                                    BlockState plant) {
        return TriState.TRUE;
    }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
