package com.halex.createrancher.mixin.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.halex.createrancher.content.render.OpaqueBatching;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.OutlineBufferSource;

// Draws the held back block entity layers along with the game's own block entity buffers
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(OutlineBufferSource.class)
public abstract class OutlineBufferSourceMixin {

    @Inject(method = "endOutlineBatch", at = @At("HEAD"))
    private void createrancher$drawHeld(CallbackInfo ci) {
        OpaqueBatching.endOfBlockEntities();
    }
}
