package com.halex.createrancher.content.joist;

import java.util.List;
import java.util.function.Predicate;

import com.simibubi.create.content.decoration.girder.GirderBlock;
import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Right-click a joist with another to extend it
public class JoistPlacementHelper implements IPlacementHelper {

    @Override
    public Predicate<ItemStack> getItemPredicate() {
        return stack -> stack.getItem() instanceof BlockItem item && item.getBlock() instanceof JoistBlock;
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return state -> state.getBlock() instanceof JoistBlock;
    }

    private boolean canExtendToward(BlockState state, Direction side) {
        if (!(state.getBlock() instanceof JoistBlock))
            return false;
        Axis axis = side.getAxis();
        boolean x = state.getValue(GirderBlock.X);
        boolean z = state.getValue(GirderBlock.Z);
        if (!x && !z)
            return axis == Axis.Y;
        if (x && z)
            return true;
        return axis == (x ? Axis.X : Axis.Z);
    }

    private int attachedPoles(Level level, BlockPos pos, Direction direction) {
        BlockPos checkPos = pos.relative(direction);
        BlockState state = level.getBlockState(checkPos);
        int count = 0;
        while (canExtendToward(state, direction)) {
            count++;
            checkPos = checkPos.relative(direction);
            state = level.getBlockState(checkPos);
        }
        return count;
    }

    private BlockState withAxis(BlockState state, Axis axis) {
        if (state.getBlock() instanceof GirderBlock)
            return state.setValue(GirderBlock.X, axis == Axis.X)
                    .setValue(GirderBlock.Z, axis == Axis.Z)
                    .setValue(GirderBlock.AXIS, axis);
        return state;
    }

    @Override
    public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult ray) {
        List<Direction> directions = IPlacementHelper.orderedByDistance(pos, ray.getLocation(),
                dir -> canExtendToward(state, dir));
        for (Direction dir : directions) {
            int range = AllConfigs.server().equipment.placementAssistRange.get();
            if (player != null) {
                AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
                if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier.id()))
                    range += 4;
            }
            int poles = attachedPoles(level, pos, dir);
            if (poles >= range)
                continue;

            BlockPos newPos = pos.relative(dir, poles + 1);
            BlockState newState = level.getBlockState(newPos);
            if (!newState.canBeReplaced())
                continue;

            return PlacementOffset.success(newPos, placed -> Block.updateFromNeighbourShapes(withAxis(placed,
                    dir.getAxis()), level, newPos));
        }
        return PlacementOffset.fail();
    }
}
