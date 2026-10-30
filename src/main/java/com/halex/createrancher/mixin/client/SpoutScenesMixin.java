package com.halex.createrancher.mixin.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.halex.createrancher.CreateRancher;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.infrastructure.ponder.scenes.fluid.SpoutScenes;

import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;

// Changes Create's spout Ponder scene to show buckets working
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = SpoutScenes.class, remap = false)
public abstract class SpoutScenesMixin {

    @Unique
    private static final String SPOUT_BUCKETS = "spout_buckets";
    @Unique
    private static final String SPOUT_PIPES = "spout_pipes";
    @Unique
    private static final String CANT_ACCESS = "The content of a Spout cannot be accessed manually";
    @Unique
    private static final String INSTEAD_PIPES = "Instead, Pipes can be used to supply it with fluids";

    @WrapOperation(method = "filling", at = @At(value = "INVOKE", target = "Lnet/createmod/ponder/api/element/"
            + "InputElementBuilder;showing(Lnet/createmod/catnip/gui/element/ScreenElement;)"
            + "Lnet/createmod/ponder/api/element/InputElementBuilder;"))
    private static InputElementBuilder createrancher$rightClickTheBucket(InputElementBuilder input, ScreenElement icon,
                                                                       Operation<InputElementBuilder> original) {
        return input.rightClick();
    }

    // Swaps in the mod's text
    @WrapOperation(method = "filling", at = @At(value = "INVOKE", target = "Lnet/createmod/ponder/api/element/"
            + "TextElementBuilder;text(Ljava/lang/String;)Lnet/createmod/ponder/api/element/TextElementBuilder;"))
    private static TextElementBuilder createrancher$bucketsWork(TextElementBuilder text, String english,
                                                              Operation<TextElementBuilder> original) {
        TextElementBuilder built = original.call(text, english);
        if (CANT_ACCESS.equals(english))
            return built.sharedText(CreateRancher.asResource(SPOUT_BUCKETS));
        if (INSTEAD_PIPES.equals(english))
            return built.sharedText(CreateRancher.asResource(SPOUT_PIPES));
        return built;
    }

    @ModifyArg(method = "filling", at = @At(value = "INVOKE", target = "Lnet/createmod/ponder/api/element/"
            + "TextElementBuilder;colored(Lnet/createmod/ponder/api/PonderPalette;)"
            + "Lnet/createmod/ponder/api/element/TextElementBuilder;", ordinal = 0))
    private static PonderPalette createrancher$greenNotRed(PonderPalette colour) {
        return colour == PonderPalette.RED ? PonderPalette.GREEN : colour;
    }
}
