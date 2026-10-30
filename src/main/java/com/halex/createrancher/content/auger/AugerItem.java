package com.halex.createrancher.content.auger;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

// Placing an auger on the side of another starts a diagonal
public class AugerItem extends BlockItem {

    public AugerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Level level = context.getLevel();
        BlockPos placedOnPos = context.getClickedPos()
                .relative(face.getOpposite());
        BlockState placedOn = level.getBlockState(placedOnPos);
        if (!(placedOn.getBlock() instanceof AugerBlock) || AugerBlock.axisOf(placedOn) != Direction.Axis.Y
                || context.isSecondaryUseActive() || face.getAxis()
                .isVertical())
            return super.place(context);

        BlockPos diagonalPos = context.getClickedPos()
                .above();
        BlockState there = level.getBlockState(diagonalPos);
        if (there.canBeReplaced())
            return super.place(BlockPlaceContext.at(context, diagonalPos, face));
        if (!(there.getBlock() instanceof AugerBlock) || AugerBlock.axisOf(there) != Direction.Axis.Y
                || AugerBlock.slopeOf(there) != AugerBlock.Slope.NONE)
            return InteractionResult.FAIL;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        BlockState diagonal = AugerBlock.shaped(there.setValue(AugerBlock.SLOPE, AugerBlock.Slope.of(
                AugerBlock.Slope.Kind.END, face)), level, diagonalPos, true);
        if (!AugerBlock.slopeOf(diagonal)
                .fromDiagonal())
            return InteractionResult.FAIL;
        KineticBlockEntity.switchToBlockState(level, diagonalPos, diagonal);
        return InteractionResult.SUCCESS;
    }
}
