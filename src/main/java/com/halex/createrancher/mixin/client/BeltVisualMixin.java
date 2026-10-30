package com.halex.createrancher.mixin.client;

import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.halex.createrancher.content.grate.client.BeltGrateClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltVisual;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.MethodsReturnNonnullByDefault;

// Draws grated belt segments with Flywheel
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(BeltVisual.class)
public abstract class BeltVisualMixin extends KineticBlockEntityVisual<BeltBlockEntity> {

    @Unique
    @Nullable
    private TransformedInstance createrancher$grate;

    private BeltVisualMixin(VisualizationContext context, BeltBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/BeltRenderer;getBeltPartial(ZZZZ)Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel createrancher$lowerBeltUnderGrate(boolean diagonal, boolean start, boolean end,
                                                          boolean bottom, Operation<PartialModel> original) {
        PartialModel lowered = bottom ? null : BeltGrateClient.getLoweredBelt(blockEntity);
        return lowered != null ? lowered : original.call(diagonal, start, end, bottom);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void createrancher$addGrate(VisualizationContext context, BeltBlockEntity blockEntity,
                                        float partialTick, CallbackInfo ci) {
        PartialModel grate = BeltGrateClient.getGrate(blockEntity);
        if (grate == null)
            return;
        createrancher$grate = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(grate))
                .createInstance();
        createrancher$grate.setIdentityTransform()
                .translate(getVisualPosition())
                .center()
                .rotateYDegrees(BeltGrateClient.modelYRot(blockState))
                .uncenter()
                .setChanged();
        relight(createrancher$grate);
    }

    @Inject(method = "updateLight", at = @At("TAIL"))
    private void createrancher$relightGrate(float partialTick, CallbackInfo ci) {
        if (createrancher$grate != null)
            relight(createrancher$grate);
    }

    @Inject(method = "_delete", at = @At("TAIL"))
    private void createrancher$deleteGrate(CallbackInfo ci) {
        if (createrancher$grate != null)
            createrancher$grate.delete();
    }

    @Inject(method = "collectCrumblingInstances", at = @At("TAIL"))
    private void createrancher$crumbleGrate(Consumer<Instance> consumer, CallbackInfo ci) {
        if (createrancher$grate != null)
            consumer.accept(createrancher$grate);
    }
}
