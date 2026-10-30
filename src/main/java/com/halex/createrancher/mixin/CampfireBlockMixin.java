package com.halex.createrancher.mixin;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.halex.createrancher.index.BlockRegistry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

// Alfalfa bales make campfires signal fires
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin {

    @Inject(method = "isSmokeSource", at = @At("HEAD"), cancellable = true)
    private void createrancher$alfalfaBaleSignals(BlockState below, CallbackInfoReturnable<Boolean> cir) {
        if (below.is(BlockRegistry.ALFALFA_BALE.get()))
            cir.setReturnValue(true);
    }
}
