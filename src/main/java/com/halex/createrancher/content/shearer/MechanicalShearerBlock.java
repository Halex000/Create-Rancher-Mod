package com.halex.createrancher.content.shearer;

import com.halex.createrancher.index.BlockEntityRegistry;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// The Mechanical Shearer block
public class MechanicalShearerBlock extends ArmBlock {

    public MechanicalShearerBlock(Properties properties) {
        super(properties);
    }

    // Called when a neighbour changes
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.getBlockEntity(pos) instanceof MechanicalShearerBlockEntity shearer)
            shearer.onNeighbourChanged();
    }

    @Override
    public BlockEntityType<? extends ArmBlockEntity> getBlockEntityType() {
        return BlockEntityRegistry.MECHANICAL_SHEARER.get();
    }
}
