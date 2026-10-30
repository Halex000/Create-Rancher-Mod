package com.halex.createrancher.content.grill;

import javax.annotation.Nullable;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

// Lets mechanical arms use grills
public class GrillArmPoint extends ArmInteractionPoint {

    public GrillArmPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    // Makes the grill wait for the arm
    @Nullable
    @Override
    protected IItemHandler getHandler(ArmBlockEntity arm) {
        return level.getBlockEntity(pos) instanceof GrillBlockEntity grill ? grill.getArmItemHandler(
                arm.getBlockPos()) : super.getHandler(arm);
    }

    // Aims at the top of the grate
    @Override
    protected Vec3 getInteractionPositionVector() {
        float top = cachedState.getBlock() instanceof GrillBlock grill ? grill.getGrateTop() : 1;
        return Vec3.atBottomCenterOf(pos)
                .add(0, top, 0);
    }

    public static class Type extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return state.getBlock() instanceof GrillBlock;
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new GrillArmPoint(this, level, pos, state);
        }

        @Override
        public int getPriority() {
            return 1;
        }
    }
}
