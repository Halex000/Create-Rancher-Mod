package com.halex.createrancher.content.shearer.client;

import java.util.List;
import java.util.function.Consumer;

import com.halex.createrancher.content.shearer.MechanicalShearerBlockEntity;
import com.halex.createrancher.index.PartialModelRegistry;
import com.halex.createrancher.mixin.ArmBlockEntityAccessor;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.util.RecyclingPoseStack;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

// Draws the shearer with Flywheel
public class MechanicalShearerVisual extends SingleAxisRotatingVisual<MechanicalShearerBlockEntity>
        implements SimpleDynamicVisual {

    private final TransformedInstance base;
    private final TransformedInstance lowerBody;
    private final TransformedInstance upperBody;
    private final TransformedInstance head;
    private final TransformedInstance leftBlade;
    private final TransformedInstance rightBlade;
    private final List<TransformedInstance> parts;
    private final RecyclingPoseStack poseStack = new RecyclingPoseStack();

    public MechanicalShearerVisual(VisualizationContext context, MechanicalShearerBlockEntity blockEntity,
                                   float partialTick) {
        super(context, blockEntity, partialTick, Models.partial(PartialModelRegistry.FEEDER_COG));
        base = part(PartialModelRegistry.FEEDER_BASE);
        lowerBody = part(PartialModelRegistry.FEEDER_LOWER_BODY);
        upperBody = part(PartialModelRegistry.FEEDER_UPPER_BODY);
        head = part(PartialModelRegistry.SHEARER_HEAD);
        leftBlade = part(PartialModelRegistry.SHEARER_BLADE_LEFT);
        rightBlade = part(PartialModelRegistry.SHEARER_BLADE_RIGHT);
        parts = List.of(base, lowerBody, upperBody, head, leftBlade, rightBlade);

        var msr = TransformStack.of(poseStack);
        msr.translate(getVisualPosition());
        msr.center();
        if (blockState.getValue(ArmBlock.CEILING))
            msr.rotateXDegrees(180);
        animate(partialTick);
    }

    private TransformedInstance part(dev.engine_room.flywheel.lib.model.baked.PartialModel model) {
        return instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(model))
                .createInstance();
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        animate(ctx.partialTick());
    }

    // Moves the arm and shears each frame
    private void animate(float pt) {
        ArmBlockEntityAccessor arm = (ArmBlockEntityAccessor) blockEntity;
        float baseAngle = arm.createrancher$getBaseAngle()
                .getValue(pt);
        float lowerArmAngle = arm.createrancher$getLowerArmAngle()
                .getValue(pt) - 135;
        float upperArmAngle = arm.createrancher$getUpperArmAngle()
                .getValue(pt) - 90;
        float headAngle = arm.createrancher$getHeadAngle()
                .getValue(pt);

        poseStack.pushPose();
        var msr = TransformStack.of(poseStack);
        ArmRenderer.transformBase(msr, baseAngle);
        base.setTransform(poseStack)
                .setChanged();
        ArmRenderer.transformLowerArm(msr, lowerArmAngle);
        lowerBody.setTransform(poseStack)
                .setChanged();
        ArmRenderer.transformUpperArm(msr, upperArmAngle);
        upperBody.setTransform(poseStack)
                .setChanged();
        ArmRenderer.transformHead(msr, headAngle);
        head.setTransform(poseStack)
                .setChanged();
        float open = blockEntity.getShearsOpen(pt);
        for (int flip = 1; flip >= -1; flip -= 2) {
            poseStack.pushPose();
            MechanicalShearerRenderer.transformBlade(msr, flip, open);
            (flip > 0 ? leftBlade : rightBlade).setTransform(poseStack)
                    .setChanged();
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(parts.toArray(FlatLit[]::new));
    }

    @Override
    protected void _delete() {
        super._delete();
        parts.forEach(AbstractInstance::delete);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        parts.forEach(consumer);
    }
}
