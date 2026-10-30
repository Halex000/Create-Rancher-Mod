package com.halex.createrancher.content.auger;

import java.util.Objects;

import javax.annotation.Nullable;

import com.halex.createrancher.index.BlockEntityRegistry;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteShapes;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Auger block. Carries items up, down or along a line of augers.
public class AugerBlock extends KineticBlock implements IBE<AugerBlockEntity>, TransformableBlock {

    // Where an auger sits in its column
    public enum Part implements StringRepresentable {
        SINGLE, BOTTOM, MIDDLE, TOP;

        public static Part of(boolean augerBelow, boolean augerAbove) {
            if (augerBelow)
                return augerAbove ? MIDDLE : TOP;
            return augerAbove ? BOTTOM : SINGLE;
        }

        // Whether this auger is the top of the column and has the rim
        public boolean isTop() {
            return this == SINGLE || this == TOP;
        }

        public boolean isBottom() {
            return this == SINGLE || this == BOTTOM;
        }

        // The same position seen from the other end
        public Part flipped() {
            return this == TOP ? BOTTOM : this == BOTTOM ? TOP : this;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    // Where the column turns diagonal
    public enum Slope implements StringRepresentable {
        NONE(null, null), START_NORTH(Kind.START, Direction.NORTH), START_SOUTH(Kind.START, Direction.SOUTH),
        START_WEST(Kind.START, Direction.WEST), START_EAST(Kind.START, Direction.EAST),
        DIAGONAL_NORTH(Kind.DIAGONAL, Direction.NORTH), DIAGONAL_SOUTH(Kind.DIAGONAL, Direction.SOUTH),
        DIAGONAL_WEST(Kind.DIAGONAL, Direction.WEST), DIAGONAL_EAST(Kind.DIAGONAL, Direction.EAST),
        END_NORTH(Kind.END, Direction.NORTH), END_SOUTH(Kind.END, Direction.SOUTH), END_WEST(Kind.END, Direction.WEST),
        END_EAST(Kind.END, Direction.EAST);

        public enum Kind {
            START, DIAGONAL, END
        }

        @Nullable
        public final Kind kind;
        @Nullable
        public final Direction towards;

        Slope(@Nullable Kind kind, @Nullable Direction towards) {
            this.kind = kind;
            this.towards = towards;
        }

        public static Slope of(@Nullable Kind kind, @Nullable Direction towards) {
            for (Slope slope : values())
                if (slope.kind == kind && slope.towards == towards)
                    return slope;
            return NONE;
        }

        // Whether it comes up a diagonal from below
        public boolean fromDiagonal() {
            return kind == Kind.DIAGONAL || kind == Kind.END;
        }

        // Whether a diagonal carries on from its top
        public boolean toDiagonal() {
            return kind == Kind.START || kind == Kind.DIAGONAL;
        }

        // The way a sloped auger leans. Only for slopes other than NONE.
        public Direction leaning() {
            return Objects.requireNonNull(towards, "a straight auger doesn't lean");
        }

        public Slope rotated(Rotation rotation) {
            return towards == null ? this : of(kind, rotation.rotate(towards));
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    // Which ends of the window stop short for a turn
    public enum Trim implements StringRepresentable {
        NONE, BOTTOM, TOP, BOTH;

        public static Trim of(boolean bottom, boolean top) {
            return bottom ? top ? BOTH : BOTTOM : top ? TOP : NONE;
        }

        public boolean bottom() {
            return this == BOTTOM || this == BOTH;
        }

        public boolean top() {
            return this == TOP || this == BOTH;
        }

        public Trim flipped() {
            return of(top(), bottom());
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final EnumProperty<Slope> SLOPE = EnumProperty.create("slope", Slope.class);
    public static final EnumProperty<Trim> TRIM = EnumProperty.create("trim", Trim.class);

    public static final float DIAGONAL_LENGTH = (float) Math.sqrt(2);
    public static final float DIAGONAL_SHIFT = (16 - 7 * DIAGONAL_LENGTH) / 2;

    private static final VoxelShape[][] SHAPES = new VoxelShape[3][Part.values().length];
    private static final VoxelShape[][] SLOPED = new VoxelShape[3][4];
    private static final VoxelShape[][] SLOPED_COLLISION = new VoxelShape[3][4];

    static {
        for (Direction.Axis axis : Direction.Axis.values())
            for (Part part : Part.values()) {
                VoxelShape shape = box(axis, 1, 0, 15, 16);
                if (part.isTop())
                    shape = Shapes.or(shape, box(axis, 0, 14, 16, 16));
                if (part.isBottom())
                    shape = Shapes.or(shape, box(axis, 0, 0, 16, 2));
                SHAPES[axis.ordinal()][part.ordinal()] = shape;
            }
        for (Slope.Kind kind : Slope.Kind.values())
            for (Direction towards : Direction.Plane.HORIZONTAL) {
                VoxelShape shape = slopedShape(kind, towards);
                SLOPED[kind.ordinal()][towards.get2DDataValue()] = shape;
                SLOPED_COLLISION[kind.ordinal()][towards.get2DDataValue()] = Shapes.joinUnoptimized(
                        ChuteShapes.COLLISION_MASK, shape, BooleanOp.AND);
            }
    }

    // A box along the auger's axis, in pixels
    private static VoxelShape box(Direction.Axis axis, double lo, double start, double hi, double end) {
        return switch (axis) {
            case X -> Block.box(start, lo, lo, end, hi, hi);
            case Y -> Block.box(lo, start, lo, hi, end, hi);
            case Z -> Block.box(lo, lo, start, hi, hi, end);
        };
    }

    // Hitbox for diagonal augers
    private static VoxelShape slopedShape(Slope.Kind kind, Direction towards) {
        return switch (kind) {
            case START -> Shapes.block();
            case DIAGONAL -> AllShapes.CHUTE_SLOPE.get(towards);
            case END -> Shapes.joinUnoptimized(ChuteShapes.INTERSECTION_MASK, Shapes.or(Shapes.block(),
                    AllShapes.CHUTE_SLOPE.get(towards)), BooleanOp.AND);
        };
    }

    public AugerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, Part.SINGLE)
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(SLOPE, Slope.NONE)
                .setValue(TRIM, Trim.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(PART, AXIS, SLOPE, TRIM));
    }

    // Picks which way the auger faces when placed
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        if (face.getAxis()
                .isHorizontal() && !context.isSecondaryUseActive() && canRiseFrom(level, pos.below()
                .relative(face.getOpposite()), face, true))
            return shaped(defaultBlockState().setValue(SLOPE, Slope.of(Slope.Kind.END, face)), level, pos, true);
        return shaped(defaultBlockState().setValue(AXIS, face.getAxis()), level, pos, false);
    }

    // Whether a diagonal can go up from this auger
    private static boolean canRiseFrom(BlockGetter level, BlockPos fromPos, Direction towards, boolean placing) {
        BlockState from = level.getBlockState(fromPos);
        if (!(from.getBlock() instanceof AugerBlock) || axisOf(from) != Direction.Axis.Y)
            return false;
        Slope slope = slopeOf(from);
        if (slope.fromDiagonal())
            return slope.towards == towards;
        Direction rising = startsTowards(level, fromPos);
        return rising == towards || placing && rising == null;
    }

    // Stops an auger going right under a diagonal
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return !(above.getBlock() instanceof AugerBlock && slopeOf(above).fromDiagonal());
    }

    // Joins or leaves the column when neighbours change
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return withPart(state, level, pos);
    }

    // Checks diagonal connections a tick later
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (oldState.getBlock() != state.getBlock() || slopeOf(oldState) != slopeOf(state))
            recheckAround(level, pos, false);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (!state.is(newState.getBlock()))
            recheckAround(level, pos, isMoving);
    }

    // Has the augers round a spot check their shape a tick later
    static void recheckAround(Level level, BlockPos pos, boolean beside) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            recheck(level, pos.above()
                    .relative(side));
            recheck(level, pos.below()
                    .relative(side));
        }
        if (beside)
            for (Direction side : Direction.values())
                recheck(level, pos.relative(side));
    }

    // Has an auger check its shape a tick later
    static void recheck(Level level, BlockPos pos) {
        if (level.getBlockState(pos)
                .getBlock() instanceof AugerBlock auger && !level.getBlockTicks()
                .hasScheduledTick(pos, auger))
            level.scheduleTick(pos, auger, 1);
    }

    // Updates the auger's slope
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState updated = shaped(state, level, pos);
        if (updated != state)
            KineticBlockEntity.switchToBlockState(level, pos, updated);
    }

    // Works out its slope and place in the column
    public static BlockState shaped(BlockState state, BlockGetter level, BlockPos pos) {
        return shaped(state, level, pos, false);
    }

    // Shape for an auger that's being placed
    public static BlockState shaped(BlockState state, BlockGetter level, BlockPos pos, boolean placing) {
        return withPart(state.setValue(SLOPE, slopeFor(state, level, pos, placing)), level, pos);
    }

    // Works out the auger's place in the column
    private static BlockState withPart(BlockState state, BlockGetter level, BlockPos pos) {
        if (slopeOf(state).kind == Slope.Kind.DIAGONAL)
            return state.setValue(TRIM, Trim.NONE)
                    .setValue(PART, Part.of(diagonalAt(level, previousPos(state, pos)), diagonalAt(level,
                            nextPos(state, pos))));
        boolean back = linked(level, pos, state, false), onwards = linked(level, pos, state, true);
        boolean upright = slopeOf(state) == Slope.NONE && axisOf(state) == Direction.Axis.Y;
        Trim trim = Trim.of(upright && back && slopeOf(level.getBlockState(previousPos(state, pos))).kind
                == Slope.Kind.END, upright && onwards && slopeOf(level.getBlockState(nextPos(state, pos))).kind
                == Slope.Kind.START);
        return state.setValue(TRIM, trim)
                .setValue(PART, Part.of(back, onwards));
    }

    private static boolean diagonalAt(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof AugerBlock && slopeOf(state).kind == Slope.Kind.DIAGONAL;
    }

    private static Slope slopeFor(BlockState state, BlockGetter level, BlockPos pos, boolean placing) {
        if (axisOf(state) != Direction.Axis.Y)
            return Slope.NONE;
        Slope slope = slopeOf(state);
        if (slope.fromDiagonal() && canRiseFrom(level, pos.below()
                .relative(slope.towards.getOpposite()), slope.towards, placing)) {
            BlockState next = level.getBlockState(pos.above()
                    .relative(slope.towards));
            boolean onwards = next.getBlock() instanceof AugerBlock && slopeOf(next).fromDiagonal() && slopeOf(next)
                    .towards == slope.towards;
            return Slope.of(onwards ? Slope.Kind.DIAGONAL : Slope.Kind.END, slope.towards);
        }
        Direction towards = startsTowards(level, pos);
        return towards == null ? Slope.NONE : Slope.of(Slope.Kind.START, towards);
    }

    // Which way a diagonal goes up from this auger, if any
    @Nullable
    private static Direction startsTowards(BlockGetter level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockState above = level.getBlockState(pos.above()
                    .relative(side));
            if (above.getBlock() instanceof AugerBlock && slopeOf(above).fromDiagonal() && slopeOf(above).towards == side)
                return side;
        }
        return null;
    }

    // Where the next auger in the column is
    public static BlockPos nextPos(BlockState state, BlockPos pos) {
        Slope slope = slopeOf(state);
        if (slope.toDiagonal())
            return pos.above()
                    .relative(slope.towards);
        return pos.relative(Direction.fromAxisAndDirection(axisOf(state), Direction.AxisDirection.POSITIVE));
    }

    // Where the previous auger in the column is
    public static BlockPos previousPos(BlockState state, BlockPos pos) {
        Slope slope = slopeOf(state);
        if (slope.fromDiagonal())
            return pos.below()
                    .relative(slope.towards.getOpposite());
        return pos.relative(Direction.fromAxisAndDirection(axisOf(state), Direction.AxisDirection.NEGATIVE));
    }

    // Whether this auger joins the next one
    public static boolean linked(BlockGetter level, BlockPos pos, BlockState state, boolean forwards) {
        BlockPos otherPos = forwards ? nextPos(state, pos) : previousPos(state, pos);
        BlockState other = level.getBlockState(otherPos);
        if (!(other.getBlock() instanceof AugerBlock))
            return false;
        return (forwards ? previousPos(other, otherPos) : nextPos(other, otherPos)).equals(pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Slope slope = slopeOf(state);
        if (slope != Slope.NONE)
            return SLOPED[slope.kind.ordinal()][slope.towards.get2DDataValue()];
        return SHAPES[axisOf(state).ordinal()][state.getValue(PART)
                .ordinal()];
    }

    // Same collision as Create's chute
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        Slope slope = slopeOf(state);
        if (slope != Slope.NONE)
            return SLOPED_COLLISION[slope.kind.ordinal()][slope.towards.get2DDataValue()];
        return getShape(state, level, pos, context);
    }

    // Keeps the auger on its line when rotated
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction positive = Direction.fromAxisAndDirection(axisOf(state), Direction.AxisDirection.POSITIVE);
        return reoriented(state, rotation.rotate(positive)).setValue(SLOPE, slopeOf(state).rotated(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction positive = Direction.fromAxisAndDirection(axisOf(state), Direction.AxisDirection.POSITIVE);
        Slope slope = slopeOf(state);
        if (slope != Slope.NONE)
            slope = Slope.of(slope.kind, mirror.mirror(slope.towards));
        return reoriented(state, mirror.mirror(positive)).setValue(SLOPE, slope);
    }

    // Turns the auger with a contraption
    @Override
    public BlockState transform(BlockState state, StructureTransform transform) {
        Slope slope = slopeOf(state);
        if (slope != Slope.NONE) {
            boolean tipped = transform.rotationAxis != null && transform.rotationAxis != Direction.Axis.Y
                    && transform.rotation != Rotation.NONE;
            state = state.setValue(SLOPE, tipped ? Slope.NONE : Slope.of(slope.kind, transform.rotateFacing(
                    transform.mirrorFacing(slope.towards))));
        }
        return reoriented(state, positiveEndAfter(axisOf(state), transform));
    }

    // Which way the positive end points after a contraption turns it
    static Direction positiveEndAfter(Direction.Axis axis, StructureTransform transform) {
        return transform.rotateFacing(transform.mirrorFacing(Direction.fromAxisAndDirection(axis,
                Direction.AxisDirection.POSITIVE)));
    }

    private static BlockState reoriented(BlockState state, Direction positiveEnd) {
        state = state.setValue(AXIS, positiveEnd.getAxis());
        if (positiveEnd.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
            state = state.setValue(PART, state.getValue(PART)
                            .flipped())
                    .setValue(TRIM, state.getValue(TRIM)
                            .flipped());
        return state;
    }

    // The axle runs along the auger
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return axisOf(state);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        if (face.getAxis() != axisOf(state))
            return false;
        Slope slope = slopeOf(state);
        return !(face == Direction.UP && slope.toDiagonal() || face == Direction.DOWN && slope.fromDiagonal());
    }

    // Updates rotation when the slope changes
    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return super.areStatesKineticallyEquivalent(oldState, newState) && slopeOf(oldState) == slopeOf(newState);
    }

    // The axis an auger lies along
    public static Direction.Axis axisOf(BlockState state) {
        return state.hasProperty(AXIS) ? state.getValue(AXIS) : Direction.Axis.Y;
    }

    public static Slope slopeOf(BlockState state) {
        return state.hasProperty(SLOPE) ? state.getValue(SLOPE) : Slope.NONE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public Class<AugerBlockEntity> getBlockEntityClass() {
        return AugerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AugerBlockEntity> getBlockEntityType() {
        return BlockEntityRegistry.AUGER.get();
    }
}
