package com.halex.createrancher.content.crate;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// A crate with 15 slots that keeps its items when broken
public class CrateBlock extends BaseEntityBlock {

    public static final MapCodec<CrateBlock> CODEC = simpleCodec(CrateBlock::new);

    public CrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<CrateBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrateBlockEntity crate)
            player.openMenu(crate);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // Makes creative mode drop the crate like a shulker box
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            if (!level.isClientSide && player.isCreative() && !crate.isEmpty()) {
                ItemStack stack = new ItemStack(this);
                stack.applyComponents(crate.collectComponents());
                ItemEntity item = new ItemEntity(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack);
                item.setDefaultPickUpDelay();
                level.addFreshEntity(item);
            } else {
                crate.unpackLootTable(player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()))
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    // Lists what's inside in the tooltip
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (stack.has(DataComponents.CONTAINER_LOOT))
            tooltip.add(Component.translatable("container.shulkerBox.unknownContents"));
        int shown = 0;
        int total = 0;
        for (ItemStack item : stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItems()) {
            total++;
            if (shown <= 4) {
                shown++;
                tooltip.add(Component.translatable("container.shulkerBox.itemCount", item.getHoverName(),
                        item.getCount()));
            }
        }
        if (total > shown)
            tooltip.add(Component.translatable("container.shulkerBox.more", total - shown)
                    .withStyle(ChatFormatting.ITALIC));
    }
}
