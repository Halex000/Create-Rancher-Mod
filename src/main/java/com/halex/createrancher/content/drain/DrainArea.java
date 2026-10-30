package com.halex.createrancher.content.drain;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

// The spots of a group of joined drains
public class DrainArea {

    public static final int MAX_WIDTH = 16;

    private final LongSet positions = new LongOpenHashSet();
    private int minX, maxX, minZ, maxZ;

    DrainArea(BlockPos pos) {
        positions.add(pos.asLong());
        minX = maxX = pos.getX();
        minZ = maxZ = pos.getZ();
    }

    // Every drain joined to this one, or null if they spread wider than a joined drain can be
    @Nullable
    static DrainArea of(BlockGetter level, BlockPos start) {
        DrainArea area = new DrainArea(start);
        Deque<BlockPos> toVisit = new ArrayDeque<>();
        toVisit.add(start);
        while (!toVisit.isEmpty()) {
            BlockPos pos = toVisit.poll();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(direction);
                if (area.contains(next) || !LivestockDrainBlock.joined(level, pos, direction))
                    continue;
                area.include(next);
                if (!area.fits())
                    return null;
                toVisit.add(next);
            }
        }
        return area;
    }

    boolean contains(BlockPos pos) {
        return positions.contains(pos.asLong());
    }

    // Whether this and another area would still fit together
    boolean fitsWith(DrainArea other) {
        return Math.max(maxX, other.maxX) - Math.min(minX, other.minX) < MAX_WIDTH
                && Math.max(maxZ, other.maxZ) - Math.min(minZ, other.minZ) < MAX_WIDTH;
    }

    void add(DrainArea other) {
        positions.addAll(other.positions);
        minX = Math.min(minX, other.minX);
        maxX = Math.max(maxX, other.maxX);
        minZ = Math.min(minZ, other.minZ);
        maxZ = Math.max(maxZ, other.maxZ);
    }

    private void include(BlockPos pos) {
        positions.add(pos.asLong());
        minX = Math.min(minX, pos.getX());
        maxX = Math.max(maxX, pos.getX());
        minZ = Math.min(minZ, pos.getZ());
        maxZ = Math.max(maxZ, pos.getZ());
    }

    private boolean fits() {
        return maxX - minX < MAX_WIDTH && maxZ - minZ < MAX_WIDTH;
    }
}
