package com.halex.createrancher.content.fisher.client;

import java.util.List;

import com.halex.createrancher.CommonConfig;
import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.index.BlockRegistry;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

// Adds the fisher's stress to its tooltip
@EventBusSubscriber(modid = CreateRancher.MOD_ID, value = Dist.CLIENT)
public class MechanicalFisherTooltip {

    // Runs last so the stress goes at the bottom
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTooltip(ItemTooltipEvent event) {
        if (!BlockRegistry.MECHANICAL_FISHER.isIn(event.getItemStack()) || !StressImpact.isEnabled())
            return;
        List<Component> tooltip = event.getToolTip();
        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(Component.translatable("create.tooltip.stressImpact")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" ")
                .append(Component.translatable("createrancher.tooltip.fisher_stress",
                        Math.round(CommonConfig.MECHANICAL_FISHER_STRESS.get())))
                .withStyle(StressImpact.LOW.getAbsoluteColor()));
    }
}
