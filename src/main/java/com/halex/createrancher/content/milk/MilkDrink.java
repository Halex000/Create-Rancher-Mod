package com.halex.createrancher.content.milk;

import java.util.List;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.EffectCures;

// Drinking milk or chocolate milk
public enum MilkDrink {
    MILK,
    CHOCOLATE_MILK;

    public static final int DRINK_TICKS = 32;

    public InteractionResultHolder<ItemStack> startDrinking(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    // Cures the drinker and uses up the drink
    public ItemStack finishDrinking(Item item, ItemStack stack, Level level, LivingEntity drinker, ItemStack empty) {
        if (drinker instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(item));
        }
        if (!level.isClientSide)
            cure(drinker);
        if (drinker instanceof Player player && !empty.isEmpty())
            return ItemUtils.createFilledResult(stack, player, empty, false);
        stack.consume(1, drinker);
        return stack.isEmpty() ? empty : stack;
    }

    public void cure(LivingEntity drinker) {
        if (this == MILK) {
            drinker.removeEffectsCuredBy(EffectCures.MILK);
            return;
        }
        List<Holder<MobEffect>> harmful = drinker.getActiveEffects()
                .stream()
                .filter(effect -> effect.getEffect()
                        .value()
                        .getCategory() == MobEffectCategory.HARMFUL && effect.getCures()
                        .contains(EffectCures.MILK))
                .map(MobEffectInstance::getEffect)
                .toList();
        harmful.forEach(drinker::removeEffect);
    }
}
