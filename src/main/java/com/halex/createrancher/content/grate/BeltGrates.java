package com.halex.createrancher.content.grate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.halex.createrancher.index.ItemRegistry;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.data.ModelProperty;

// Item Grates on Create belts
public class BeltGrates {

    public static final float BELT_DROP = 4 / 16f;
    private static final Map<ShapePair, VoxelShape> COMBINED = new ConcurrentHashMap<>();

    private static final VoxelShape[][][] GRATE_SHAPES = new VoxelShape[2][2][4];

    static {
        for (int hitbox = 0; hitbox < 2; hitbox++)
            for (int alongX = 0; alongX < 2; alongX++)
                for (int ends = 0; ends < 4; ends++) {
                    double from = (ends & 1) != 0 ? 3 : 0, to = (ends & 2) != 0 ? 13 : 16;
                    double side = hitbox == 1 ? 0 : 1;
                    GRATE_SHAPES[hitbox][alongX][ends] = alongX == 1 ? Block.box(from, 13, side, to, 14, 16 - side)
                            : Block.box(side, 13, from, 16 - side, 14, to);
                }
    }

    public static final ModelProperty<Integer> GRATED_CASING_PROPERTY = new ModelProperty<>();

    // A flat belt segment without a shaft
    public static boolean canHoldGrate(BlockState state) {
        return AllBlocks.BELT.has(state) && state.getValue(BeltBlock.SLOPE) == BeltSlope.HORIZONTAL
                && state.getValue(BeltBlock.PART) == BeltPart.MIDDLE;
    }

    public static boolean isGrated(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof BeltBlockEntity && ((GratedBelt) be).createrancher$isGrated();
    }

    public static boolean isGrated(BeltBlockEntity belt) {
        return ((GratedBelt) belt).createrancher$isGrated();
    }

    public static GrateMetal getMetal(BeltBlockEntity belt) {
        return ((GratedBelt) belt).createrancher$getGrateMetal();
    }

    // Lays a grate over a segment
    public static boolean placeGrate(Level level, BlockPos pos) {
        if (!canHoldGrate(level.getBlockState(pos)) || !(level.getBlockEntity(pos) instanceof BeltBlockEntity belt)
                || isGrated(belt))
            return false;
        setGrated(belt, true);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1, 1.1f);
        return true;
    }

    // Takes the grate off a belt segment and returns what drops
    public static List<ItemStack> removeGrate(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BeltBlockEntity belt) || !isGrated(belt))
            return List.of();
        List<ItemStack> removed = new ArrayList<>();
        removed.add(ItemRegistry.ITEM_GRATE.asStack());
        Item nugget = getMetal(belt).getNugget();
        if (nugget != null)
            removed.add(new ItemStack(nugget));
        ((GratedBelt) belt).createrancher$setGrateMetal(GrateMetal.PLAIN);
        setGrated(belt, false);
        level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1, 1.1f);
        return removed;
    }

    // Plates a segment's grate. Returns the old metal, or null if nothing changed.
    @Nullable
    public static GrateMetal plateGrate(Level level, BlockPos pos, GrateMetal metal) {
        if (!(level.getBlockEntity(pos) instanceof BeltBlockEntity belt) || !isGrated(belt) || getMetal(belt) == metal)
            return null;
        GrateMetal before = getMetal(belt);
        ((GratedBelt) belt).createrancher$setGrateMetal(metal);
        belt.setChanged();
        belt.sendData();
        level.playSound(null, pos, metal == GrateMetal.IRON ? SoundEvents.CHAIN_PLACE : SoundEvents.COPPER_PLACE,
                SoundSource.BLOCKS, 1, 1.3f);
        return before;
    }

    private static void setGrated(BeltBlockEntity belt, boolean grated) {
        ((GratedBelt) belt).createrancher$setGrated(grated);
        belt.setChanged();
        belt.sendData();
    }

    // Drops the grate when the segment can't hold it
    public static void dropGrate(Level level, BlockPos pos) {
        for (ItemStack stack : removeGrate(level, pos))
            Block.popResource(level, pos, stack);
    }

    // Drops the grates of a whole belt
    public static void dropChainGrates(Level level, BlockPos brokenPos, BlockState brokenState) {
        dropGrate(level, brokenPos);
        for (boolean forward : new boolean[] {true, false}) {
            BlockPos pos = BeltBlock.nextSegmentPosition(brokenState, brokenPos, forward);
            int limit = 1000;
            while (pos != null && limit-- > 0) {
                BlockState state = level.getBlockState(pos);
                if (!AllBlocks.BELT.has(state))
                    break;
                dropGrate(level, pos);
                pos = BeltBlock.nextSegmentPosition(state, pos, forward);
            }
        }
    }

    // Whether the next segment is grated too
    public static boolean isNeighbourGrated(BlockGetter level, BlockPos pos, Direction side) {
        return isGrated(level, pos.relative(side));
    }

    // A belt shape joined with a grate shape
    public static VoxelShape withGrate(VoxelShape belt, VoxelShape grate) {
        if (COMBINED.size() > 4096)
            COMBINED.clear();
        return COMBINED.computeIfAbsent(new ShapePair(belt, grate), pair -> Shapes.or(pair.belt(), pair.grate()));
    }

    // A belt shape and a grate shape
    private record ShapePair(VoxelShape belt, VoxelShape grate) {

        @Override
        public boolean equals(Object other) {
            return other instanceof ShapePair pair && pair.belt == belt && pair.grate == grate;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(belt) + System.identityHashCode(grate);
        }
    }

    // The grate's shape over a segment
    public static VoxelShape grateShape(BlockGetter level, BlockPos pos, BlockState state, boolean hitbox) {
        if (!canHoldGrate(state) || !isGrated(level, pos))
            return Shapes.empty();
        Direction.Axis axis = state.getValue(BeltBlock.HORIZONTAL_FACING)
                .getAxis();
        Direction start = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
        int ends = (isNeighbourGrated(level, pos, start) ? 0 : 1)
                | (isNeighbourGrated(level, pos, start.getOpposite()) ? 0 : 2);
        return GRATE_SHAPES[hitbox ? 1 : 0][axis == Direction.Axis.X ? 1 : 0][ends];
    }

    // Which ends of the casing slope up
    public static int casingSlopes(BeltBlockEntity belt) {
        Level level = belt.getLevel();
        if (level == null)
            return 0;
        Direction start = Direction.fromAxisAndDirection(belt.getBlockState()
                .getValue(BeltBlock.HORIZONTAL_FACING)
                .getAxis(), Direction.AxisDirection.NEGATIVE);
        int slopes = 0;
        if (casingSlopesTowards(level, belt.getBlockPos(), start))
            slopes |= 1;
        if (casingSlopesTowards(level, belt.getBlockPos(), start.getOpposite()))
            slopes |= 2;
        return slopes;
    }

    private static boolean casingSlopesTowards(Level level, BlockPos pos, Direction side) {
        return !isNeighbourGrated(level, pos, side)
                && level.getBlockEntity(pos.relative(side)) instanceof BeltBlockEntity neighbour
                && neighbour.casing != CasingType.NONE;
    }
}
