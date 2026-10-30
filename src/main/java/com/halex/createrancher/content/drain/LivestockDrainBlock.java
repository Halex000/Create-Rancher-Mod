package com.halex.createrancher.content.drain;

import java.util.Map;

import com.halex.createrancher.index.BlockEntityRegistry;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Livestock Drain block. Animals standing on it fill it.
public class LivestockDrainBlock extends Block implements IBE<LivestockDrainBlockEntity>, IWrenchable {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty NORTH_EAST = BooleanProperty.create("north_east");
    public static final BooleanProperty SOUTH_EAST = BooleanProperty.create("south_east");
    public static final BooleanProperty SOUTH_WEST = BooleanProperty.create("south_west");
    public static final BooleanProperty NORTH_WEST = BooleanProperty.create("north_west");

    private static final Map<Direction, BooleanProperty> SIDES = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST,
            Direction.SOUTH, SOUTH, Direction.WEST, WEST);

    public LivestockDrainBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(NORTH_EAST, false)
                .setValue(SOUTH_EAST, false)
                .setValue(SOUTH_WEST, false)
                .setValue(NORTH_WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(NORTH, EAST, SOUTH, WEST, NORTH_EAST, SOUTH_EAST, SOUTH_WEST,
                NORTH_WEST));
    }

    public static boolean isDrain(BlockState state) {
        return state.getBlock() instanceof LivestockDrainBlock;
    }

    public static BooleanProperty side(Direction direction) {
        return SIDES.get(direction);
    }

    // Whether this drain is joined to the one on that side
    public static boolean joins(BlockState state, Direction direction) {
        return isDrain(state) && direction.getAxis()
                .isHorizontal() && state.getValue(side(direction));
    }

    // Whether the drains at two neighbouring spots are joined to each other
    public static boolean joined(BlockGetter level, BlockPos pos, Direction direction) {
        return joins(level.getBlockState(pos), direction) && joins(level.getBlockState(pos.relative(direction)),
                direction.getOpposite());
    }

    // Whether the corner between two sides is open
    private static boolean cornerOpen(BlockState state, BlockGetter level, BlockPos pos, Direction a, Direction b) {
        return state.getValue(side(a)) && state.getValue(side(b)) && joined(level, pos.relative(a), b) && joined(
                level, pos.relative(b), a);
    }

    // Drops joins to spots that aren't drains any more and works out the corners
    public static BlockState withCorners(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction direction : SIDES.keySet())
            if (!isDrain(level.getBlockState(pos.relative(direction))))
                state = state.setValue(side(direction), false);
        return state.setValue(NORTH_EAST, cornerOpen(state, level, pos, Direction.NORTH, Direction.EAST))
                .setValue(SOUTH_EAST, cornerOpen(state, level, pos, Direction.SOUTH, Direction.EAST))
                .setValue(SOUTH_WEST, cornerOpen(state, level, pos, Direction.SOUTH, Direction.WEST))
                .setValue(NORTH_WEST, cornerOpen(state, level, pos, Direction.NORTH, Direction.WEST));
    }

    // Joins a new drain to the drains beside it, as long as the joined drain still fits in 16x16
    public static BlockState joinNeighbours(BlockState state, BlockGetter level, BlockPos pos) {
        DrainArea area = new DrainArea(pos);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(direction);
            boolean join = false;
            if (isDrain(level.getBlockState(next))) {
                DrainArea there = DrainArea.of(level, next);
                join = there != null && (area.contains(next) || area.fitsWith(there));
                if (join)
                    area.add(there);
            }
            state = state.setValue(side(direction), join);
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = joinNeighbours(defaultBlockState(), context.getLevel(), context.getClickedPos());
        return withCorners(state, context.getLevel(), context.getClickedPos());
    }

    // Joins nearby drains when placed
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!isDrain(oldState))
            joinAround(level, pos);
    }

    // Joins a placed or moved drain to the drains beside it
    static void joinAround(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isDrain(state))
            return;
        BlockState joined = joinNeighbours(state, level, pos);
        if (joined != state)
            level.setBlock(pos, joined, Block.UPDATE_CLIENTS);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(direction);
            BlockState there = level.getBlockState(next);
            if (isDrain(there) && there.getValue(side(direction.getOpposite())) != joined.getValue(side(direction)))
                level.setBlock(next, there.setValue(side(direction.getOpposite()), joined.getValue(side(direction))),
                        Block.UPDATE_CLIENTS);
        }
        refreshCorners(level, pos);
    }

    // Drops joins to drains that aren't there any more
    static void tidyJoins(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isDrain(state))
            return;
        BlockState tidy = withCorners(state, level, pos);
        if (tidy != state)
            level.setBlock(pos, tidy, Block.UPDATE_CLIENTS);
    }

    // Works out the corners again for a drain and the drains round it
    static void refreshCorners(Level level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos at = pos.offset(dx, 0, dz);
                BlockState there = level.getBlockState(at);
                if (!isDrain(there))
                    continue;
                BlockState updated = withCorners(there, level, at);
                if (updated != there)
                    level.setBlock(at, updated, Block.UPDATE_CLIENTS);
            }
    }

    // Drops the join to a drain beside it that's gone
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction.getAxis()
                .isHorizontal() && !isDrain(neighbourState))
            state = state.setValue(side(direction), false);
        return state;
    }

    // Tells diagonal drains when a drain is placed or broken
    @Override
    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags,
                                                 int recursionLeft) {
        for (int dx = -1; dx <= 1; dx += 2)
            for (int dz = -1; dz <= 1; dz += 2) {
                BlockPos corner = pos.offset(dx, 0, dz);
                BlockState there = level.getBlockState(corner);
                if (!isDrain(there))
                    continue;
                BlockState updated = withCorners(there, level, corner);
                if (updated != there)
                    level.setBlock(corner, updated, flags & ~Block.UPDATE_NEIGHBORS, recursionLeft);
            }
    }

    // Full block hitbox
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    // Fills or empties buckets
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return onBlockEntityUseItemOn(level, pos, drain -> FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack,
                drain) || FluidHelper.tryFillItemFromBE(level, player, hand, stack, drain)
                ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
    }

    // How full the whole joined drain is
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof LivestockDrainBlockEntity drain))
            return 0;
        LivestockDrainGroup group = drain.getGroup();
        int total = group.getTotalAmount();
        if (total == 0)
            return 0;
        return 1 + Mth.floor(14f * total / group.getCapacity());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public Class<LivestockDrainBlockEntity> getBlockEntityClass() {
        return LivestockDrainBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LivestockDrainBlockEntity> getBlockEntityType() {
        return BlockEntityRegistry.LIVESTOCK_DRAIN.get();
    }
}
