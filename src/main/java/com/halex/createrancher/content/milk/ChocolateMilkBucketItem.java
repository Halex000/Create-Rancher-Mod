package com.halex.createrancher.content.milk;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

// A bucket of chocolate milk
public class ChocolateMilkBucketItem extends BucketItem {

    public ChocolateMilkBucketItem(Fluid chocolateMilk, Properties properties) {
        super(chocolateMilk, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return MilkDrink.CHOCOLATE_MILK.startDrinking(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        return MilkDrink.CHOCOLATE_MILK.finishDrinking(this, stack, level, drinker, new ItemStack(Items.BUCKET));
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
