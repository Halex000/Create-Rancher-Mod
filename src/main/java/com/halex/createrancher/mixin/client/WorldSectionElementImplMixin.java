package com.halex.createrancher.mixin.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.createmod.catnip.render.ShadeSeparatingSuperByteBuffer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.minecraft.MethodsReturnNonnullByDefault;

// Fixes the lighting on redstone dust in Ponder scenes
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = WorldSectionElementImpl.class, remap = false)
public abstract class WorldSectionElementImplMixin {

    @ModifyReturnValue(method = "buildStructureBuffer", at = @At("RETURN"))
    private SuperByteBuffer createrancher$lightUnshadedFaces(SuperByteBuffer buffer) {
        if (buffer instanceof ShadeSeparatingSuperByteBuffer && buffer instanceof ShadeSeparatingSuperByteBufferAccessor accessor)
            accessor.createrancher$setInvertFakeDiffuseNormal(true);
        return buffer;
    }
}
