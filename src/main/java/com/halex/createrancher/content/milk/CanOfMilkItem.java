package com.halex.createrancher.content.milk;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

// A can of milk or chocolate milk
public class CanOfMilkItem extends Item {

    private final MilkDrink drink;

    public CanOfMilkItem(MilkDrink drink, Properties properties) {
        super(properties);
        this.drink = drink;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return drink.startDrinking(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        return drink.finishDrinking(this, stack, level, drinker, ItemStack.EMPTY);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MilkDrink.DRINK_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
