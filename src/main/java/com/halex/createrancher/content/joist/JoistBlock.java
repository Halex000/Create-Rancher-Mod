package com.halex.createrancher.content.joist;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.decoration.girder.GirderBlock;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// A wooden version of Create's girder
public class JoistBlock extends GirderBlock {

    private static final int PLACEMENT_HELPER = PlacementHelpers.register(new JoistPlacementHelper());

    private final Block planks;

    public JoistBlock(Properties properties, Block planks) {
        super(properties);
        this.planks = planks;
    }

    public Block getPlanks() {
        return planks;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (AllItems.WRENCH.isIn(stack) && !player.isShiftKeyDown()) {
            if (JoistWrenchBehavior.handleClick(level, pos, state, hitResult))
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            return ItemInteractionResult.FAIL;
        }

        IPlacementHelper helper = PlacementHelpers.get(PLACEMENT_HELPER);
        if (helper.matchesItem(stack))
            return helper.getOffset(player, level, state, pos, hitResult)
                    .placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // Burns like its planks
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return planks.defaultBlockState()
                .getFlammability(level, pos, direction);
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return planks.defaultBlockState()
                .getFireSpreadSpeed(level, pos, direction);
    }
}
