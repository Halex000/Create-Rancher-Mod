package com.halex.createrancher.content.herder;

import com.halex.createrancher.content.render.OpaqueBatching;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

// Draws the herder when Flywheel is off
public class MechanicalHerderRenderer extends SafeBlockEntityRenderer<MechanicalHerderBlockEntity> {

    public MechanicalHerderRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(MechanicalHerderBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;
        buffer = OpaqueBatching.of(buffer);

        BlockState state = be.getBlockState();
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());
        KineticBlockEntityRenderer.renderRotatingBuffer(be, CachedBuffers.partial(AllPartialModels.ARM_COG, state), ms,
                vc, light);

        PoseStack local = new PoseStack();
        TransformStack.of(local)
                .center();
        HerderArmParts.layOut(local, be.yaw.getValue(pt), be.shoulderAngle.getValue(pt), be.bendAngle.getValue(pt),
                be.isCarrying(), (index, model, pose) -> CachedBuffers.partial(model, state)
                        .transform(pose)
                        .light(light)
                        .renderInto(ms, vc));
    }

    @Override
    public boolean shouldRenderOffScreen(MechanicalHerderBlockEntity be) {
        return true;
    }
}
