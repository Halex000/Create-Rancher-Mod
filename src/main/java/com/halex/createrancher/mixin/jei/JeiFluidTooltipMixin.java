package com.halex.createrancher.mixin.jei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.stew.SuspiciousStews;
import com.simibubi.create.foundation.item.ItemDescription;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

// Adds stew effects and shift summaries to fluid tooltips in JEI
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Pseudo
@Mixin(targets = "mezz.jei.neoforge.platform.FluidHelper", remap = false)
public class JeiFluidTooltipMixin {

    // For JEI before 19.64
    @Inject(method = "getTooltip(Lmezz/jei/api/gui/builder/ITooltipBuilder;Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At(value = "INVOKE", target = "Lmezz/jei/api/gui/builder/ITooltipBuilder;add(Lnet/minecraft/network/chat/FormattedText;)V",
                    ordinal = 0, shift = At.Shift.AFTER), remap = false, require = 0)
    private void createrancher$addToBuilder(ITooltipBuilder tooltip, FluidStack stack, TooltipFlag flag,
                                            CallbackInfo ci) {
        createrancher$addLines(tooltip::add, stack);
    }

    // For JEI 19.64 and up
    @Inject(method = "getTooltip(Ljava/util/List;Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0,
                    shift = At.Shift.AFTER), remap = false, require = 0)
    private void createrancher$addToList(List<Component> tooltip, FluidStack stack, TooltipFlag flag,
                                         CallbackInfo ci) {
        createrancher$addLines(tooltip::add, stack);
    }

    // Adds the shift summary and stew effects
    @Unique
    private static void createrancher$addLines(Consumer<Component> tooltip, FluidStack stack) {
        addBucketSummary(tooltip, stack);
        SuspiciousStewEffects effects = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (effects == null || effects.effects()
                .isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        float tickRate = mc.level == null ? 20 : mc.level.tickRateManager()
                .tickrate();
        SuspiciousStews.addTooltip(effects, tooltip, tickRate);
    }

    // Adds a bucket's shift summary to its fluid
    @Unique
    private static void addBucketSummary(Consumer<Component> tooltip, FluidStack stack) {
        Item bucket = stack.getFluid()
                .getBucket();
        if (bucket == Items.AIR || !BuiltInRegistries.ITEM.getKey(bucket)
                .getNamespace()
                .equals(CreateRancher.MOD_ID))
            return;
        ItemDescription description = ItemDescription.create(bucket, Palette.STANDARD_CREATE);
        if (description != null)
            description.getCurrentLines()
                    .forEach(tooltip);
    }
}
