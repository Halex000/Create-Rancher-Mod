package com.halex.createrancher.content.drain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import com.halex.createrancher.compat.sable.SubLevels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

// Joined livestock drains that share their fluids
public class LivestockDrainGroup {

    public static final int CAPACITY = 1500;
    static final int SYNC_TICKS = 10;

    @Nullable
    private final Level level;
    private final List<LivestockDrainBlockEntity> members;
    private final LongSet positions = new LongOpenHashSet();
    private final AABB standingSpace;
    private final Direction.Axis axis;
    private final Slices slices;
    private final List<FluidStack> totals = new ArrayList<>();
    private final IFluidHandler handler = new Handler();
    private boolean valid = true;
    private long lastSync;
    private boolean syncSoon;

    private LivestockDrainGroup(@Nullable Level level, List<LivestockDrainBlockEntity> members) {
        this.level = level;
        this.members = members;
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (LivestockDrainBlockEntity member : members) {
            BlockPos pos = member.getBlockPos();
            positions.add(pos.asLong());
            minX = Math.min(minX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        int y = members.getFirst()
                .getBlockPos()
                .getY();
        standingSpace = new AABB(minX, y + 0.5, minZ, maxX + 1, y + 2, maxZ + 1);
        axis = maxX - minX >= maxZ - minZ ? Direction.Axis.X : Direction.Axis.Z;
        slices = new Slices(members, axis);
    }

    // Builds the group from every joined drain
    static void form(LivestockDrainBlockEntity start) {
        Level level = start.getLevel();
        if (level == null || start.isRemoved()) {
            LivestockDrainGroup alone = new LivestockDrainGroup(level, List.of(start));
            alone.valid = false;
            start.group = alone;
            return;
        }
        List<LivestockDrainBlockEntity> found = joinedTo(level, start);
        while (!level.isClientSide && trim(level, found))
            found = joinedTo(level, start);
        if (!level.isClientSide)
            for (LivestockDrainBlockEntity drain : found)
                LivestockDrainBlock.tidyJoins(level, drain.getBlockPos());
        found.sort(Comparator.comparingInt((LivestockDrainBlockEntity drain) -> drain.getBlockPos()
                        .getX())
                .thenComparingInt(drain -> drain.getBlockPos()
                        .getZ()));
        LivestockDrainGroup group = new LivestockDrainGroup(level, found);
        for (LivestockDrainBlockEntity member : found) {
            LivestockDrainGroup old = member.group;
            if (old != null && old != group)
                old.valid = false;
            member.group = group;
            for (FluidStack stack : member.getShare())
                group.add(stack);
        }
        if (!level.isClientSide)
            group.spread();
    }

    // The drains joined to this one, following the open sides between them
    private static List<LivestockDrainBlockEntity> joinedTo(Level level, LivestockDrainBlockEntity start) {
        List<LivestockDrainBlockEntity> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> toVisit = new ArrayDeque<>();
        toVisit.add(start.getBlockPos());
        seen.add(start.getBlockPos());
        while (!toVisit.isEmpty()) {
            BlockPos pos = toVisit.poll();
            LivestockDrainBlockEntity drain = pos.equals(start.getBlockPos()) ? start : drainAt(level, pos);
            if (drain == null)
                continue;
            found.add(drain);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(direction);
                if (LivestockDrainBlock.joined(level, pos, direction) && seen.add(next))
                    toVisit.add(next);
            }
        }
        return found;
    }

    // Splits off anything past 16 blocks from the group's north-west corner. Returns whether it split anything.
    private static boolean trim(Level level, List<LivestockDrainBlockEntity> found) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (LivestockDrainBlockEntity drain : found) {
            BlockPos pos = drain.getBlockPos();
            minX = Math.min(minX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        if (maxX - minX < DrainArea.MAX_WIDTH && maxZ - minZ < DrainArea.MAX_WIDTH)
            return false;
        boolean split = false;
        for (LivestockDrainBlockEntity drain : found) {
            BlockPos pos = drain.getBlockPos();
            if (!inside(pos, minX, minZ))
                continue;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(direction);
                if (inside(next, minX, minZ) || !LivestockDrainBlock.joined(level, pos, direction))
                    continue;
                level.setBlock(pos, level.getBlockState(pos)
                        .setValue(LivestockDrainBlock.side(direction), false), Block.UPDATE_CLIENTS);
                level.setBlock(next, level.getBlockState(next)
                        .setValue(LivestockDrainBlock.side(direction.getOpposite()), false), Block.UPDATE_CLIENTS);
                LivestockDrainBlock.refreshCorners(level, pos);
                LivestockDrainBlock.refreshCorners(level, next);
                split = true;
            }
        }
        return split;
    }

    private static boolean inside(BlockPos pos, int minX, int minZ) {
        return pos.getX() - minX < DrainArea.MAX_WIDTH && pos.getZ() - minZ < DrainArea.MAX_WIDTH;
    }

    @Nullable
    private static LivestockDrainBlockEntity drainAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos))
            return null;
        if (level.getBlockEntity(pos) instanceof LivestockDrainBlockEntity drain && !drain.isRemoved())
            return drain;
        return null;
    }

    // Whether this is the client's copy
    private boolean clientSide() {
        return level == null || level.isClientSide;
    }

    public boolean isValid() {
        return valid;
    }

    void invalidate() {
        valid = false;
    }

    boolean isLeader(LivestockDrainBlockEntity drain) {
        return members.getFirst() == drain;
    }

    public int size() {
        return members.size();
    }

    public int getCapacity() {
        return CAPACITY * members.size();
    }

    public IFluidHandler getFluidHandler() {
        return handler;
    }

    public List<FluidStack> getContents() {
        if (clientSide()) {
            totals.clear();
            for (LivestockDrainBlockEntity member : members)
                for (FluidStack stack : member.getShare())
                    add(stack);
        }
        return totals;
    }

    public int getTotalAmount() {
        int total = 0;
        for (FluidStack stack : getContents())
            total += stack.getAmount();
        return total;
    }

    private static int indexOf(List<FluidStack> stacks, FluidStack stack) {
        for (int i = 0; i < stacks.size(); i++)
            if (FluidStack.isSameFluidSameComponents(stacks.get(i), stack))
                return i;
        return -1;
    }

    private void add(FluidStack stack) {
        if (stack.isEmpty())
            return;
        int index = indexOf(totals, stack);
        if (index < 0)
            totals.add(stack.copy());
        else
            totals.get(index)
                    .grow(stack.getAmount());
    }

    // Puts in as much as fits and returns how much went in
    public int offer(FluidStack stack) {
        if (stack.isEmpty())
            return 0;
        int index = indexOf(totals, stack);
        int amount = Math.min(stack.getAmount(), getCapacity() - getTotalAmount());
        if (amount <= 0)
            return 0;
        if (index < 0)
            syncSoon = true;
        add(stack.copyWithAmount(amount));
        return amount;
    }

    // Puts fluid in from outside the group
    public int fill(FluidStack stack) {
        int amount = offer(stack);
        if (amount > 0)
            spread();
        return amount;
    }

    // Lets the animals on the group add their fluid
    void tick() {
        Level level = this.level;
        if (level == null)
            return;
        if (getTotalAmount() >= getCapacity()) {
            sync(level);
            return;
        }
        BlockPos anchor = members.getFirst()
                .getBlockPos();
        List<Mob> animals = level.getEntitiesOfClass(Mob.class, standingSpace, mob -> mob.isAlive()
                && (positions.contains(mob.getOnPos()
                .asLong()) || positions.contains(BlockPos.containing(SubLevels.toLocal(level, anchor, mob.position()))
                .below()
                .asLong())));
        if (!animals.isEmpty()) {
            List<FluidStack> made = new ArrayList<>(animals.size());
            for (Mob animal : animals)
                made.add(LivestockMilking.milk(animal));
            boolean changed = false;
            int first = (int) (level.getGameTime() % animals.size());
            for (int n = 0; n < animals.size(); n++) {
                int i = (first + n) % animals.size();
                int added = offer(made.get(i));
                LivestockMilking.given(animals.get(i), added);
                changed |= added > 0;
            }
            if (changed)
                spread();
        }
        sync(level);
    }

    // Syncs drains whose fluids changed
    private void sync(Level level) {
        long now = level.getGameTime();
        if (!syncSoon && now - lastSync < SYNC_TICKS)
            return;
        boolean any = false;
        for (LivestockDrainBlockEntity member : members)
            if (member.shareChanged) {
                member.shareChanged = false;
                member.sendData();
                level.updateNeighbourForOutputSignal(member.getBlockPos(), member.getBlockState()
                        .getBlock());
                any = true;
            }
        syncSoon = false;
        if (any)
            lastSync = now;
    }

    // Splits the fluid evenly between the drains
    void spread() {
        totals.removeIf(FluidStack::isEmpty);
        int count = members.size();
        List<List<FluidStack>> shares = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            shares.add(new ArrayList<>());
        int handedOut = 0;
        for (FluidStack total : totals) {
            int each = total.getAmount() / count;
            int left = total.getAmount() % count;
            for (int i = 0; i < count; i++) {
                int amount = each + (Math.floorMod(i - handedOut, count) < left ? 1 : 0);
                if (amount > 0)
                    shares.get(i)
                            .add(total.copyWithAmount(amount));
            }
            handedOut = (handedOut + left) % count;
        }
        for (int i = 0; i < count; i++)
            syncSoon |= members.get(i)
                    .setShare(shares.get(i));
    }

    private FluidStack drain(int index, int maxAmount, IFluidHandler.FluidAction action) {
        FluidStack total = totals.get(index);
        int amount = Math.min(maxAmount, total.getAmount());
        FluidStack drained = total.copyWithAmount(amount);
        if (action.execute() && amount > 0) {
            total.shrink(amount);
            if (total.isEmpty())
                syncSoon = true;
            spread();
        }
        return drained;
    }

    // Where the fluids are drawn in this drain
    public float[] stripWithin(BlockPos pos, float from, float to) {
        return slices.within(axis == Direction.Axis.X ? pos.getX() : pos.getZ(), from, to);
    }

    public Direction.Axis getAxis() {
        return axis;
    }

    // The group's fluid handler for pipes
    private class Handler implements IFluidHandler {

        // Shows one empty tank when empty
        @Override
        public int getTanks() {
            return Math.max(1, getContents().size());
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            List<FluidStack> contents = getContents();
            return tank < contents.size() ? contents.get(tank) : FluidStack.EMPTY;
        }

        // Each fluid can fill the whole store
        @Override
        public int getTankCapacity(int tank) {
            return getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !valid)
                return 0;
            int room = Math.min(resource.getAmount(), getCapacity() - getTotalAmount());
            if (action.simulate() || room <= 0 || clientSide())
                return Math.max(0, room);
            return LivestockDrainGroup.this.fill(resource.copyWithAmount(room));
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            int index = indexOf(totals, resource);
            if (index < 0 || !valid || clientSide())
                return FluidStack.EMPTY;
            return LivestockDrainGroup.this.drain(index, resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (totals.isEmpty() || !valid || clientSide())
                return FluidStack.EMPTY;
            return LivestockDrainGroup.this.drain(0, maxDrain, action);
        }
    }

    // Splits the group into rows for drawing the fluids
    private static class Slices {

        private static final float WALL = 2 / 16f;

        private final int first;
        private final int[] widths;
        private final float total;

        Slices(List<LivestockDrainBlockEntity> members, Direction.Axis axis) {
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (LivestockDrainBlockEntity member : members) {
                int along = axis.choose(member.getBlockPos()
                        .getX(), 0, member.getBlockPos()
                        .getZ());
                min = Math.min(min, along);
                max = Math.max(max, along);
            }
            first = min;
            widths = new int[max - min + 1];
            for (LivestockDrainBlockEntity member : members)
                widths[axis.choose(member.getBlockPos()
                        .getX(), 0, member.getBlockPos()
                        .getZ()) - min]++;
            float sum = 0;
            for (int row = 0; row < widths.length; row++)
                sum += widths[row] * length(row);
            total = sum;
        }

        // How much of a row is inside the walls
        private float length(int row) {
            float start = row == 0 ? WALL : 0;
            float end = row == widths.length - 1 ? 1 - WALL : 1;
            return end - start;
        }

        // How far along the group a fraction of the store reaches
        private float position(float fraction) {
            float area = Mth.clamp(fraction, 0, 1) * total;
            for (int row = 0; row < widths.length; row++) {
                float rowArea = widths[row] * length(row);
                if (area <= rowArea || row == widths.length - 1)
                    return row + (row == 0 ? WALL : 0) + (rowArea == 0 ? 0 : Math.min(area / rowArea, 1)
                            * length(row));
                area -= rowArea;
            }
            return widths.length;
        }

        float[] within(int along, float from, float to) {
            float offset = along - first;
            return new float[] {Mth.clamp(position(from) - offset, 0, 1), Mth.clamp(position(to) - offset, 0, 1)};
        }
    }
}
