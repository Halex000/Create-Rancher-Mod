package com.halex.createrancher.content.shearer;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmAngleTarget;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Where the shearer's arm reaches
public class ShearingPoint extends ArmInteractionPoint {

    private static final ThreadLocal<Boolean> SHEARER_TICKING = ThreadLocal.withInitial(() -> false);

    public ShearingPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    // Runs a shearer's arm logic
    public static void whileShearerTicks(Runnable armLogic) {
        boolean wasTicking = SHEARER_TICKING.get();
        SHEARER_TICKING.set(true);
        try {
            armLogic.run();
        } finally {
            SHEARER_TICKING.set(wasTicking);
        }
    }

    @Override
    protected Vec3 getInteractionPositionVector() {
        return level.getBlockEntity(pos) instanceof MechanicalShearerBlockEntity shearer
                ? shearer.getShearTargetTop() : super.getInteractionPositionVector();
    }

    // Aims again every time since the animal moves
    @Override
    public ArmAngleTarget getTargetAngles(BlockPos armPos, boolean ceiling) {
        return new ArmAngleTarget(armPos, getInteractionPositionVector(), getInteractionDirection(), ceiling);
    }

    // One slot so the arm checks for animals to shear
    @Override
    public int getSlotCount(ArmBlockEntity armBlockEntity) {
        return 1;
    }

    @Override
    public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
        return stack;
    }

    // It's always an input
    @Override
    public void cycleMode() {
    }

    public static class Type extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return SHEARER_TICKING.get() && state.getBlock() instanceof MechanicalShearerBlock;
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new ShearingPoint(this, level, pos, state);
        }
    }
}
