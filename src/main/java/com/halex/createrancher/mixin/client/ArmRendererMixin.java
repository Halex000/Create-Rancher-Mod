package com.halex.createrancher.mixin.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.halex.createrancher.content.feeder.FeederArmModels;
import com.halex.createrancher.index.BlockRegistry;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.state.BlockState;

// Draws the feeder in its own colours without Flywheel
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ArmRenderer.class)
public abstract class ArmRendererMixin {

    @ModifyArg(method = {"renderArm", "getRotatedModel"}, at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/render/CachedBuffers;partial(Ldev/engine_room/flywheel/lib/model/baked/PartialModel;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/createmod/catnip/render/SuperByteBuffer;"),
            index = 0)
    private PartialModel createrancher$feederPart(PartialModel armPart, BlockState state) {
        return BlockRegistry.MECHANICAL_FEEDER.has(state) ? FeederArmModels.forFeeder(armPart) : armPart;
    }
}
