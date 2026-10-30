package com.halex.createrancher.content.feeder;

import javax.annotation.Nullable;

import com.halex.createrancher.index.BlockEntityRegistry;
import com.halex.createrancher.index.DataComponentRegistry;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

// The Mechanical Feeder block
public class MechanicalFeederBlock extends ArmBlock {

    public MechanicalFeederBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide)
            return;
        BlockPos pickup = stack.get(DataComponentRegistry.FEEDER_PICKUP);
        if (pickup == null || !(level.getBlockEntity(pos) instanceof MechanicalFeederBlockEntity feeder))
            return;
        MechanicalFeederBlockEntity.LinkStatus status = feeder.linkPickup(pickup);
        if (placer instanceof Player player)
            player.displayClientMessage(Component.translatable(status.getMessageKey(), ArmBlockEntity.getRange()),
                    true);
    }

    @Override
    public BlockEntityType<? extends ArmBlockEntity> getBlockEntityType() {
        return BlockEntityRegistry.MECHANICAL_FEEDER.get();
    }
}
