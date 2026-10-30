package com.halex.createrancher.content.shearer.client;

import com.halex.createrancher.content.render.OpaqueBatching;
import com.halex.createrancher.content.shearer.MechanicalShearerBlockEntity;
import com.halex.createrancher.index.PartialModelRegistry;
import com.halex.createrancher.mixin.ArmBlockEntityAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

// Draws the shearer when Flywheel is off
public class MechanicalShearerRenderer extends KineticBlockEntityRenderer<MechanicalShearerBlockEntity> {

    private static final float PIVOT_Z = -3f / 16;
    private static final float OPEN_DEGREES = 35;

    public MechanicalShearerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    // Swings a blade open
    public static void transformBlade(TransformStack msr, int flip, float open) {
        msr.translate(0, 0, PIVOT_Z);
        msr.rotateYDegrees(flip * OPEN_DEGREES * open);
        msr.translate(0, 0, -PIVOT_Z);
    }

    @Override
    protected void renderSafe(MechanicalShearerBlockEntity shearer, float pt, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        buffer = OpaqueBatching.of(buffer);
        super.renderSafe(shearer, pt, ms, buffer, light, overlay);
        if (VisualizationManager.supportsVisualization(shearer.getLevel()))
            return;
        BlockState state = shearer.getBlockState();
        VertexConsumer builder = buffer.getBuffer(RenderType.solid());
        ArmBlockEntityAccessor arm = (ArmBlockEntityAccessor) shearer;

        PoseStack local = new PoseStack();
        var msr = TransformStack.of(local);
        msr.center();
        if (state.getValue(ArmBlock.CEILING))
            msr.rotateXDegrees(180);

        ArmRenderer.transformBase(msr, arm.createrancher$getBaseAngle()
                .getValue(pt));
        draw(PartialModelRegistry.FEEDER_BASE, state, local, ms, builder, light);
        ArmRenderer.transformLowerArm(msr, arm.createrancher$getLowerArmAngle()
                .getValue(pt) - 135);
        draw(PartialModelRegistry.FEEDER_LOWER_BODY, state, local, ms, builder, light);
        ArmRenderer.transformUpperArm(msr, arm.createrancher$getUpperArmAngle()
                .getValue(pt) - 90);
        draw(PartialModelRegistry.FEEDER_UPPER_BODY, state, local, ms, builder, light);
        ArmRenderer.transformHead(msr, arm.createrancher$getHeadAngle()
                .getValue(pt));
        draw(PartialModelRegistry.SHEARER_HEAD, state, local, ms, builder, light);
        float open = shearer.getShearsOpen(pt);
        for (int flip = 1; flip >= -1; flip -= 2) {
            local.pushPose();
            transformBlade(msr, flip, open);
            draw(flip > 0 ? PartialModelRegistry.SHEARER_BLADE_LEFT : PartialModelRegistry.SHEARER_BLADE_RIGHT,
                    state, local, ms, builder, light);
            local.popPose();
        }
    }

    private static void draw(PartialModel part, BlockState state, PoseStack local, PoseStack ms,
                             VertexConsumer builder, int light) {
        SuperByteBuffer model = CachedBuffers.partial(part, state)
                .light(light);
        model.transform(local)
                .renderInto(ms, builder);
    }

    @Override
    public boolean shouldRenderOffScreen(MechanicalShearerBlockEntity shearer) {
        return true;
    }

    @Override
    protected SuperByteBuffer getRotatedModel(MechanicalShearerBlockEntity shearer, BlockState state) {
        return CachedBuffers.partial(PartialModelRegistry.FEEDER_COG, state);
    }
}
