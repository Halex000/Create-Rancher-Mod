package com.halex.createrancher.content.depot;

import com.halex.createrancher.index.BlockEntityRegistry;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.depot.SharedDepotBlockMethods;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Smart Depot block
public class SmartDepotBlock extends Block implements IBE<SmartDepotBlockEntity>, IWrenchable,
        ProperWaterloggedBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty EXTRACTING = BooleanProperty.create("extracting");

    public static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 11, 16),
            box(1, 11, 1, 15, 13, 15));

    public SmartDepotBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false)
                .setValue(EXTRACTING, true)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(POWERED, EXTRACTING, WATERLOGGED));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean sneaking = context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown();
        return withWater(defaultBlockState().setValue(POWERED, context.getLevel()
                        .hasNeighborSignal(context.getClickedPos()))
                .setValue(EXTRACTING, !sneaking), context);
    }

    // A wrench switches between pulling and putting in
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide)
            level.setBlockAndUpdate(context.getClickedPos(), state.cycle(EXTRACTING));
        return InteractionResult.SUCCESS;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return fluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        updateWater(level, state, pos);
        return state;
    }

    // Updates when the block below or redstone changes
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                   boolean isMoving) {
        if (level.isClientSide)
            return;
        InvManipulationBehaviour below = BlockEntityBehaviour.get(level, pos, InvManipulationBehaviour.TYPE);
        if (below != null)
            below.onNeighborChanged(fromPos);
        if (state.getValue(POWERED) != level.hasNeighborSignal(pos))
            level.setBlock(pos, state.cycle(POWERED), 2);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // Lets players put items on and take them off
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return SharedDepotBlockMethods.onUse(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        super.updateEntityAfterFallOn(level, entity);
        SharedDepotBlockMethods.onLanded(level, entity);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return SharedDepotBlockMethods.getComparatorInputOverride(state, level, pos);
    }

    // Drops what's on top when broken
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public Class<SmartDepotBlockEntity> getBlockEntityClass() {
        return SmartDepotBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmartDepotBlockEntity> getBlockEntityType() {
        return BlockEntityRegistry.SMART_DEPOT.get();
    }
}
