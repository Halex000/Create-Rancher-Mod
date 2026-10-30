package com.halex.createrancher.mixin.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.halex.createrancher.content.auger.AugerPonderTicks;

import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.MethodsReturnNonnullByDefault;

// Counts Ponder scene ticks for augers
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PonderScene.class, remap = false)
public abstract class PonderSceneMixin implements AugerPonderTicks.Counted {

    @Unique
    private long createrancher$ticks;

    // Counts the tick
    @Inject(method = "tick", at = @At("HEAD"))
    private void createrancher$countTick(CallbackInfo ci) {
        createrancher$ticks++;
    }

    @Override
    public long createrancher$ticks() {
        return createrancher$ticks;
    }
}
