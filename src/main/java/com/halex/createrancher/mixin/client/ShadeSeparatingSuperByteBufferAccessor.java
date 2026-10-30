package com.halex.createrancher.mixin.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.createmod.catnip.render.ShadeSeparatingSuperByteBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = ShadeSeparatingSuperByteBuffer.class, remap = false)
public interface ShadeSeparatingSuperByteBufferAccessor {

    @Mutable
    @Accessor("invertFakeDiffuseNormal")
    void createrancher$setInvertFakeDiffuseNormal(boolean invert);
}
