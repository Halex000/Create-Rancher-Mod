package com.halex.createrancher.content.stew;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

// Suspicious stew effects and their tooltips
public class SuspiciousStews {

    // Every effect suspicious stew can have
    public static List<SuspiciousStewEffects> allEffects() {
        Set<SuspiciousStewEffects> effects = new LinkedHashSet<>();
        for (SuspiciousEffectHolder holder : SuspiciousEffectHolder.getAllEffectHolders())
            effects.add(holder.getSuspiciousEffects());
        return new ArrayList<>(effects);
    }

    // Adds the stew's effects to a tooltip
    public static void addTooltip(SuspiciousStewEffects effects, Consumer<Component> tooltip, float tickRate) {
        List<MobEffectInstance> instances = new ArrayList<>();
        for (SuspiciousStewEffects.Entry entry : effects.effects())
            instances.add(entry.createEffectInstance());
        PotionContents.addPotionTooltip(instances, tooltip, 1, tickRate);
    }
}
