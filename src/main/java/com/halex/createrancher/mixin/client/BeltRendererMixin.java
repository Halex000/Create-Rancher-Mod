package com.halex.createrancher.mixin.client;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.halex.createrancher.content.grate.client.BeltGrateClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// Draws grated belt segments without Flywheel
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(BeltRenderer.class)
public abstract class BeltRendererMixin {

    @WrapOperation(method = "renderSafe", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/BeltRenderer;getBeltPartial(ZZZZ)Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel createrancher$lowerBeltUnderGrate(boolean diagonal, boolean start, boolean end,
                                                          boolean bottom, Operation<PartialModel> original,
                                                          @Local(argsOnly = true) BeltBlockEntity be) {
        PartialModel lowered = bottom ? null : BeltGrateClient.getLoweredBelt(be);
        return lowered != null ? lowered : original.call(diagonal, start, end, bottom);
    }

    @Inject(method = "renderSafe", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/BeltRenderer;renderItems(Lcom/simibubi/create/content/kinetics/belt/BeltBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"))
    private void createrancher$renderGrate(BeltBlockEntity be, float partialTicks, PoseStack ms,
                                           MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
        if (!VisualizationManager.supportsVisualization(be.getLevel()))
            BeltGrateClient.renderGrate(be, ms, buffer, light);
    }

    // Lowers items on grated segments
    @Inject(method = "renderItem", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0, shift = At.Shift.AFTER))
    private void createrancher$lowerItemUnderGrate(BeltBlockEntity be, float partialTicks, PoseStack ms,
                                                   MultiBufferSource buffer, int light, int overlay,
                                                   Direction beltFacing, Vec3i directionVec, BeltSlope slope,
                                                   int verticality, boolean slopeAlongX, boolean onContraption,
                                                   TransportedItemStack transported, Vec3 beltStartOffset,
                                                   CallbackInfo ci) {
        if (slope != BeltSlope.HORIZONTAL)
            return;
        float offset = be.getSpeed() == 0 ? transported.beltPosition
                : Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
        float drop = BeltGrateClient.itemDrop(be, offset);
        if (drop != 0)
            ms.translate(0, drop, 0);
    }
}
