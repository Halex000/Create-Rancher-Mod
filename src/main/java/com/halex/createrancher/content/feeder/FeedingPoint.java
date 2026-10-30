package com.halex.createrancher.content.feeder;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmAngleTarget;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Where the feeder feeds animals
public class FeedingPoint extends ArmInteractionPoint {

    private static final ThreadLocal<Boolean> FEEDER_TICKING = ThreadLocal.withInitial(() -> false);

    public FeedingPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    // Runs a feeder's arm logic
    public static void whileFeederTicks(Runnable armLogic) {
        boolean wasTicking = FEEDER_TICKING.get();
        FEEDER_TICKING.set(true);
        try {
            armLogic.run();
        } finally {
            FEEDER_TICKING.set(wasTicking);
        }
    }

    @Override
    protected Vec3 getInteractionPositionVector() {
        return level.getBlockEntity(pos) instanceof MechanicalFeederBlockEntity feeder ? feeder.getFeedTargetTop()
                : super.getInteractionPositionVector();
    }

    // Aims again every time since the animal moves
    @Override
    public ArmAngleTarget getTargetAngles(BlockPos armPos, boolean ceiling) {
        return new ArmAngleTarget(armPos, getInteractionPositionVector(), getInteractionDirection(), ceiling);
    }

    @Override
    public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
        if (!(armBlockEntity instanceof MechanicalFeederBlockEntity feeder) || !feeder.getBlockPos()
                .equals(pos))
            return stack;
        return feeder.feed(stack, simulate);
    }

    @Override
    public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotCount(ArmBlockEntity armBlockEntity) {
        return 0;
    }

    // It's always an output
    @Override
    public void cycleMode() {
    }

    public static class Type extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return FEEDER_TICKING.get() && state.getBlock() instanceof MechanicalFeederBlock;
        }

        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new FeedingPoint(this, level, pos, state);
        }
    }
}
