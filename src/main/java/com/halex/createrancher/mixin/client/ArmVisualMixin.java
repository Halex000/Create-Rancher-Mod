package com.halex.createrancher.mixin.client;

import java.util.ArrayList;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.halex.createrancher.content.feeder.FeederArmModels;
import com.halex.createrancher.content.feeder.MechanicalFeederBlockEntity;
import com.halex.createrancher.mixin.ArmBlockEntityAccessor;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.MethodsReturnNonnullByDefault;

// Draws the feeder in its own colours with Flywheel
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ArmVisual.class)
public abstract class ArmVisualMixin extends SingleAxisRotatingVisual<ArmBlockEntity> {

    @Shadow
    @Final
    TransformedInstance base;
    @Shadow
    @Final
    TransformedInstance lowerBody;
    @Shadow
    @Final
    TransformedInstance upperBody;
    @Shadow
    @Final
    TransformedInstance claw;
    @Shadow
    @Final
    private ArrayList<TransformedInstance> clawGrips;

    private ArmVisualMixin(VisualizationContext context, ArmBlockEntity blockEntity, float partialTick, Model model) {
        super(context, blockEntity, partialTick, model);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void createrancher$feederParts(VisualizationContext context, ArmBlockEntity blockEntity,
                                           float partialTick, CallbackInfo ci) {
        if (!(blockEntity instanceof MechanicalFeederBlockEntity))
            return;
        instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(FeederArmModels.forFeeder(
                        AllPartialModels.ARM_COG)))
                .stealInstance(rotatingModel);
        createrancher$move(base, AllPartialModels.ARM_BASE);
        createrancher$move(lowerBody, AllPartialModels.ARM_LOWER_BODY);
        createrancher$move(upperBody, AllPartialModels.ARM_UPPER_BODY);
        createrancher$moveClaw();
        createrancher$move(clawGrips.get(0), AllPartialModels.ARM_CLAW_GRIP_UPPER);
        createrancher$move(clawGrips.get(1), AllPartialModels.ARM_CLAW_GRIP_LOWER);
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void createrancher$feederClaw(float partialTick, CallbackInfo ci) {
        if (blockEntity instanceof MechanicalFeederBlockEntity)
            createrancher$moveClaw();
    }

    @Unique
    private void createrancher$moveClaw() {
        createrancher$move(claw, ((ArmBlockEntityAccessor) blockEntity).createrancher$hasGoggles()
                ? AllPartialModels.ARM_CLAW_BASE_GOGGLES
                : AllPartialModels.ARM_CLAW_BASE);
    }

    @Unique
    private void createrancher$move(TransformedInstance part, PartialModel armPart) {
        instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(FeederArmModels.forFeeder(armPart)))
                .stealInstance(part);
    }
}
