package com.halex.createrancher.content.satchel;

import com.halex.createrancher.compat.VersionCompat;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

// A leather bag with 9 slots
public class SatchelItem extends Item {

    public SatchelItem(Properties properties) {
        super(properties);
    }

    public static boolean fits(ItemStack stack) {
        return VersionCompat.fitsInContainerItems(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new SatchelMenu(id, inventory, hand),
                    stack.getHoverName()), buf -> buf.writeEnum(hand));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_REMOVE_ONE,
                    SoundSource.PLAYERS, 1, .8f + level.getRandom()
                            .nextFloat() * .4f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        ItemContainerContents contents = itemEntity.getItem()
                .set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (contents != null)
            ItemUtils.onContainerDestroyed(itemEntity, contents.nonEmptyItemsCopy());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int count = (int) stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyStream()
                .count();
        if (count > 0)
            tooltip.add(Component.translatable("createrancher.tooltip.satchel_contents", count, SatchelMenu.SLOTS)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
