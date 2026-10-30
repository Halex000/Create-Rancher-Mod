package com.halex.createrancher.content.feeder;

import java.util.List;

import com.halex.createrancher.index.DataComponentRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

// The feeder item. Links to its pickup point when placed.
public class MechanicalFeederItem extends BlockItem {

    public MechanicalFeederItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BlockPos pickup = stack.get(DataComponentRegistry.FEEDER_PICKUP);
        if (pickup != null)
            tooltip.add(Component.translatable("createrancher.tooltip.pickup", pickup.getX(), pickup.getY(),
                            pickup.getZ())
                    .withStyle(ChatFormatting.AQUA));
    }
}
