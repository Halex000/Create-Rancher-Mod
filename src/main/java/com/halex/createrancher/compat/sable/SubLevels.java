package com.halex.createrancher.compat.sable;

import javax.annotation.Nullable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

// Helpers for blocks on Sable sub-levels. Does nothing without Sable.
public final class SubLevels {

    private static final boolean LOADED = ModList.get()
            .isLoaded("sablecompanion");

    private SubLevels() {
    }

    // Whether two blocks are on the same sub-level, or both on none
    public static boolean sameSubLevel(@Nullable Level level, BlockPos a, BlockPos b) {
        return !LOADED || level == null || Sable.same(level, a, b);
    }

    // Whether a block is on a sub-level
    public static boolean isOnSubLevel(@Nullable Level level, BlockPos pos) {
        return LOADED && level != null && Sable.contains(level, pos);
    }

    // Moves a position from the anchor's sub-level into the world
    public static Vec3 toWorld(@Nullable Level level, BlockPos anchor, Vec3 pos) {
        return LOADED && level != null ? Sable.toWorld(level, anchor, pos) : pos;
    }

    // Moves a world position into the anchor's sub-level
    public static Vec3 toLocal(@Nullable Level level, BlockPos anchor, Vec3 pos) {
        return LOADED && level != null ? Sable.toLocal(level, anchor, pos) : pos;
    }

    // Only loaded when Sable is installed
    private static final class Sable {

        static boolean same(Level level, BlockPos a, BlockPos b) {
            SubLevelAccess first = SableCompanion.INSTANCE.getContaining(level, a);
            SubLevelAccess second = SableCompanion.INSTANCE.getContaining(level, b);
            if (first == null || second == null)
                return first == second;
            return first.getUniqueId()
                    .equals(second.getUniqueId());
        }

        static boolean contains(Level level, BlockPos pos) {
            return SableCompanion.INSTANCE.getContaining(level, pos) != null;
        }

        static Vec3 toWorld(Level level, BlockPos anchor, Vec3 pos) {
            SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, anchor);
            return subLevel == null ? pos : subLevel.logicalPose()
                    .transformPosition(pos);
        }

        static Vec3 toLocal(Level level, BlockPos anchor, Vec3 pos) {
            SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, anchor);
            return subLevel == null ? pos : subLevel.logicalPose()
                    .transformPositionInverse(pos);
        }
    }
}
