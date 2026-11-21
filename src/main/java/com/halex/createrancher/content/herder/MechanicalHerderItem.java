package com.halex.createrancher.content.herder;

import java.util.List;

import com.halex.createrancher.content.target.MachineTargets;
import com.halex.createrancher.content.target.TargetingItem;
import com.halex.createrancher.index.DataComponentRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

// The herder item. Sneak right-click a block to pick the drop-off.
public class MechanicalHerderItem extends BlockItem implements TargetingItem {

    public MechanicalHerderItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || context.getHand() != InteractionHand.MAIN_HAND)
            return super.useOn(context);

        Level level = context.getLevel();
        if (level.isClientSide) {
            BlockPos clicked = context.getClickedPos();
            MachineTargets.toggle(player, clicked, Component.translatable("createrancher.message.drop_off_selected",
                    clicked.getX(), clicked.getY(), clicked.getZ(), MechanicalHerderBlockEntity.RANGE));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public DataComponentType<BlockPos> savedTarget() {
        return DataComponentRegistry.DROP_OFF.get();
    }

    @Override
    public Component targetDroppedMessage() {
        return Component.translatable("createrancher.message.drop_off_cleared");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BlockPos dropOff = stack.get(DataComponentRegistry.DROP_OFF);
        if (dropOff != null)
            tooltip.add(Component.translatable("createrancher.tooltip.drop_off", dropOff.getX(), dropOff.getY(),
                            dropOff.getZ())
                    .withStyle(ChatFormatting.GOLD));
    }
}
