package com.halex.createrancher.content.fisher;

import java.util.List;

import com.halex.createrancher.index.DataComponentRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

// The fisher item. Sneak right-click water to pick its fishing spot.
public class MechanicalFisherItem extends BlockItem {

    public MechanicalFisherItem(Block block, Properties properties) {
        super(block, properties);
    }

    // Picks a fishing spot or places the fisher
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()
                || FishingSpots.surfaceOf(context.getLevel(), context.getClickedPos()) == null)
            return super.useOn(context);
        select(context.getLevel(), player, context.getItemInHand(), context.getClickedPos());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    // Picks open water as the fishing spot
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown())
            return super.use(level, player, hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK)
            return InteractionResultHolder.pass(stack);
        select(level, player, stack, hit.getBlockPos());
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void select(Level level, Player player, ItemStack stack, BlockPos clicked) {
        if (level.isClientSide)
            return;
        BlockPos surface = FishingSpots.surfaceOf(level, clicked);
        if (surface == null) {
            player.displayClientMessage(Component.translatable("createrancher.message.fisher.not_fishable"), true);
            return;
        }
        if (surface.equals(stack.get(DataComponentRegistry.FISHING_SPOT))) {
            stack.remove(DataComponentRegistry.FISHING_SPOT);
            player.displayClientMessage(Component.translatable("createrancher.message.fisher.spot_cleared"), true);
            return;
        }
        stack.set(DataComponentRegistry.FISHING_SPOT, surface);
        player.displayClientMessage(Component.translatable("createrancher.message.fisher.spot_selected",
                surface.getX(), surface.getY(), surface.getZ(), MechanicalFisherBlockEntity.RANGE), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BlockPos spot = stack.get(DataComponentRegistry.FISHING_SPOT);
        if (spot != null)
            tooltip.add(Component.translatable("createrancher.tooltip.fishing_spot", spot.getX(), spot.getY(),
                            spot.getZ())
                    .withStyle(ChatFormatting.GOLD));
    }
}
