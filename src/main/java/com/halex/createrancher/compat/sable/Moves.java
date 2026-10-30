package com.halex.createrancher.compat.sable;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

// Spots block entities that Sable moved without calling onPlace
public final class Moves {

    private static final String ORIGIN = "Origin";
    private static final String FACING = "SavedFacing";

    private Moves() {
    }

    // Saves where the block is, so loading it somewhere else shows it moved
    public static void saveOrigin(CompoundTag tag, BlockPos pos) {
        tag.putLong(ORIGIN, pos.asLong());
    }

    // Whether the tag was saved at another spot
    public static boolean moved(CompoundTag tag, BlockPos pos) {
        return tag.contains(ORIGIN) && tag.getLong(ORIGIN) != pos.asLong();
    }

    // Saves which way the block faces
    public static void saveFacing(CompoundTag tag, Direction facing) {
        tag.putByte(FACING, (byte) facing.get2DDataValue());
    }

    // How far the block turned since the tag was saved
    public static Rotation turn(CompoundTag tag, Direction facing) {
        if (!tag.contains(FACING))
            return Rotation.NONE;
        Direction saved = Direction.from2DDataValue(tag.getByte(FACING));
        for (Rotation rotation : Rotation.values())
            if (rotation.rotate(saved) == facing)
                return rotation;
        return Rotation.NONE;
    }

    // How far an arm turned while being moved
    public static Rotation armTurn(Level level, BlockPos arm, CompoundTag point) {
        for (Rotation rotation : Rotation.values()) {
            CompoundTag turned = point.copy();
            turnPoint(turned, rotation);
            if (ArmInteractionPoint.deserialize(turned, level, arm) != null)
                return rotation;
        }
        return Rotation.NONE;
    }

    // Turns a saved arm point around the arm
    public static void turnPoint(CompoundTag point, Rotation rotation) {
        NbtUtils.readBlockPos(point, "Pos")
                .ifPresent(pos -> point.put("Pos", NbtUtils.writeBlockPos(pos.rotate(rotation))));
    }
}
