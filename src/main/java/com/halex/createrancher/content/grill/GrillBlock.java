package com.halex.createrancher.content.grill;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// A block with a grate that processes items on it
public interface GrillBlock {

    // What the grill does, or null
    @Nullable
    GrillProcess getProcess(BlockState state);

    // Whether it's working right now
    boolean isProcessing(Level level, BlockPos pos, BlockState state);

    // Height of the grate's top
    float getGrateTop();

    // Spacing between items on the grate
    float getSlotSpacing();

    // Which way the grid of items is turned
    Direction getFacing(BlockState state);

    // Puts the held item on the grill
    static ItemInteractionResult placeByHand(ItemStack stack, Level level, BlockPos pos, Player player,
                                             Runnable onPlaced) {
        if (!(level.getBlockEntity(pos) instanceof GrillBlockEntity grill) || !grill.canCook(stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!grill.hasFreeSlot())
            return ItemInteractionResult.CONSUME;
        if (!level.isClientSide && grill.placeFood(stack)) {
            stack.consume(1, player);
            onPlaced.run();
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    // Drops everything when removed
    static void dropContents(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GrillBlockEntity grill)
            for (ItemStack stack : grill.getContents())
                Containers.dropItemStack(level, pos.getX(), pos.getY() + 1, pos.getZ(), stack);
    }

    // Picks up dropped items that land on the grill
    static void onLanded(Entity entity) {
        if (!(entity instanceof ItemEntity item) || entity.level().isClientSide)
            return;
        BlockEntity grill = entity.level()
                .getBlockEntity(entity.getOnPos());
        if (!(grill instanceof GrillBlockEntity))
            grill = entity.level()
                    .getBlockEntity(entity.blockPosition());
        if (grill instanceof GrillBlockEntity grillBlockEntity)
            grillBlockEntity.insertDropped(item);
    }

    // Called when a neighbour changes
    static void onNeighbourChanged(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GrillBlockEntity grill)
            grill.onNeighbourChanged();
    }
}
