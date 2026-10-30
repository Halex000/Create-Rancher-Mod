package com.halex.createrancher.content.depot;

import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes.DepotPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// Lets mechanical arms use the smart depot like a depot
public class SmartDepotArmPointType extends ArmInteractionPointType {

    @Override
    public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof SmartDepotBlock;
    }

    @Override
    public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
        return new DepotPoint(this, level, pos, state);
    }
}
