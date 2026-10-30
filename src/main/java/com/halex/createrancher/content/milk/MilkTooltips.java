package com.halex.createrancher.content.milk;

import java.util.List;

import javax.annotation.Nullable;

import com.halex.createrancher.index.ItemRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// Tooltip line saying what milk and chocolate milk remove
public class MilkTooltips {

    public static final String REMOVES_ALL = "createrancher.tooltip.removes_all_effects";
    public static final String REMOVES_NEGATIVE = "createrancher.tooltip.removes_negative_effects";

    public static void register() {
        NeoForge.EVENT_BUS.addListener(MilkTooltips::onTooltip);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        String key = key(event.getItemStack());
        if (key == null)
            return;
        List<Component> lines = event.getToolTip();
        lines.add(Math.min(1, lines.size()), Component.translatable(key)
                .withStyle(MobEffectCategory.BENEFICIAL.getTooltipFormatting()));
    }

    @Nullable
    public static String key(ItemStack stack) {
        if (stack.is(Items.MILK_BUCKET) || stack.is(ItemRegistry.CAN_OF_MILK.get()))
            return REMOVES_ALL;
        if (stack.is(ItemRegistry.CHOCOLATE_MILK_BUCKET.get()) || stack.is(ItemRegistry.CAN_OF_CHOCOLATE_MILK.get()))
            return REMOVES_NEGATIVE;
        return null;
    }
}
