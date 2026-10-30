package com.halex.createrancher.content.herder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.util.Mth;

// Draws the herder with Flywheel
public class MechanicalHerderVisual extends SingleAxisRotatingVisual<MechanicalHerderBlockEntity>
        implements SimpleDynamicVisual {

    private final List<TransformedInstance> parts = new ArrayList<>();
    private final PoseStack poseStack = new PoseStack();

    private float yaw = Float.NaN;
    private float shoulder = Float.NaN;
    private float bend = Float.NaN;
    private boolean carrying;

    public MechanicalHerderVisual(VisualizationContext context, MechanicalHerderBlockEntity blockEntity,
                                   float partialTick) {
        super(context, blockEntity, partialTick, Models.partial(AllPartialModels.ARM_COG));
        TransformStack.of(poseStack)
                .translate(getVisualPosition())
                .center();
        HerderArmParts.layOut(poseStack, 0, 0, 0, false, (index, model, pose) -> parts.add(instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(model))
                .createInstance()));
        animate(partialTick);
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        animate(context.partialTick());
    }

    private void animate(float partialTick) {
        float yawNow = blockEntity.yaw.getValue(partialTick);
        float shoulderNow = blockEntity.shoulderAngle.getValue(partialTick);
        float bendNow = blockEntity.bendAngle.getValue(partialTick);
        boolean carryingNow = blockEntity.isCarrying();

        if (Mth.equal(yaw, yawNow) && Mth.equal(shoulder, shoulderNow) && Mth.equal(bend, bendNow)
                && carrying == carryingNow)
            return;

        yaw = yawNow;
        shoulder = shoulderNow;
        bend = bendNow;
        carrying = carryingNow;
        HerderArmParts.layOut(poseStack, yaw, shoulder, bend, carrying, (index, model, pose) -> parts.get(index)
                .setTransform(pose)
                .setChanged());
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
