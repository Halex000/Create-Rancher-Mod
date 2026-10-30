package com.halex.createrancher.mixin.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.halex.createrancher.content.render.OpaqueBatching;
import com.mojang.blaze3d.vertex.BufferBuilder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

// Draws the held back block entity layers before the game draws anything see-through
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(MultiBufferSource.BufferSource.class)
public abstract class BufferSourceMixin {

    @Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V",
            at = @At("HEAD"))
    private void createrancher$drawHeldFirst(RenderType type, BufferBuilder builder, CallbackInfo ci) {
        if (type.sortOnUpload())
            OpaqueBatching.beforeSeeThrough((MultiBufferSource) this);
    }
}
