package com.halex.createrancher.content.fisher.client;

import com.halex.createrancher.content.fisher.MechanicalFisherBlock;
import com.halex.createrancher.content.fisher.MechanicalFisherBlockEntity.Phase;
import com.halex.createrancher.content.fisher.MechanicalFisherBlockEntity;
import com.halex.createrancher.content.net.client.NetRendering;
import com.halex.createrancher.content.render.OpaqueBatching;
import com.halex.createrancher.index.PartialModelRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.Affine;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Draws the fisher's moving parts and its net
public class MechanicalFisherRenderer extends SafeBlockEntityRenderer<MechanicalFisherBlockEntity> {

    private static final Vec3 HINGE = new Vec3(0, 14 / 16f, 2 / 16f);
    private static final double KNOT_OUT = .5 + 1.5 / 16;
    private static final double KNOT_HEIGHT = 12.5 / 16;
    private static final float LAUNCH_ANGLE = 70;
    private static final int FLICK_TICKS = 3;

    public MechanicalFisherRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(MechanicalFisherBlockEntity fisher, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        buffer = OpaqueBatching.of(buffer);
        BlockState state = fisher.getBlockState();
        Direction facing = state.getValue(MechanicalFisherBlock.HORIZONTAL_FACING);
        Phase phase = fisher.getPhase();
        float time = phaseTime(fisher, partialTicks);
        float angle = plateAngle(fisher, partialTicks);
        boolean onPlate = netOnPlate(fisher, partialTicks);

        if (!VisualizationManager.supportsVisualization(fisher.getLevel())) {
            SuperByteBuffer cog = CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, state);
            KineticBlockEntityRenderer.standardKineticRotationTransform(cog, fisher, light)
                    .renderInto(ms, buffer.getBuffer(RenderType.solid()));

            SuperByteBuffer plate = CachedBuffers.partial(PartialModelRegistry.FISHER_PLATE, state);
            transformPlate(plate, facing, angle);
            plate.light(light)
                    .renderInto(ms, buffer.getBuffer(RenderType.solid()));

            if (onPlate) {
                SuperByteBuffer pile = CachedBuffers.partial(PartialModelRegistry.FISHER_NET_PILE, state);
                transformPlate(pile, facing, angle);
                pile.light(light)
                        .renderInto(ms, buffer.getBuffer(RenderType.cutout()));
            }

            if (fisher.hasNet()) {
                SuperByteBuffer knot = CachedBuffers.partial(PartialModelRegistry.FISHER_KNOT, state);
                transformPlate(knot, facing, 0);
                knot.light(light)
                        .renderInto(ms, buffer.getBuffer(RenderType.solid()));
            }
        }

        if (fisher.getSpot() == null || phase == Phase.IDLE || onPlate)
            return;

        BlockPos pos = fisher.getBlockPos();
        Vec3 throwPoint = Vec3.atLowerCornerOf(pos)
                .add(.5, 15 / 16f, .5);
        Vec3 knot = Vec3.atLowerCornerOf(pos)
                .add(.5, KNOT_HEIGHT, .5)
                .add(Vec3.atLowerCornerOf(facing.getNormal())
                        .scale(KNOT_OUT));
        Vec3 water = fisher.getNetPosition();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        float bobTime = fisher.getAnimationTicks() + partialTicks;

        Vec3 at;
        float spread;
        float floatSize;
        if (phase == Phase.FISHING) {
            at = water;
            spread = 1;
            floatSize = NetRendering.FLOAT_SIZE;
        } else if (phase == Phase.LAUNCHING) {
            float flight = Math.max(1, fisher.getFlightTicks() - FLICK_TICKS);
            float progress = Mth.clamp((time - FLICK_TICKS) / flight, 0, 1);
            double arc = .9 + throwPoint.distanceTo(water) * .15;
            at = throwPoint.lerp(water, progress)
                    .add(0, arc * 4 * progress * (1 - progress), 0);
            spread = Mth.lerp(progress, NetRendering.GATHERED_SPREAD, 1);
            floatSize = Mth.lerp(progress, NetRendering.GATHERED_FLOAT_SIZE, NetRendering.FLOAT_SIZE);
        } else {
            float progress = Mth.clamp(time / fisher.getReelTicks(), 0, 1);
            at = water.lerp(throwPoint, progress);
            spread = Mth.lerp(progress, 1, NetRendering.GATHERED_SPREAD);
            floatSize = Mth.lerp(progress, NetRendering.FLOAT_SIZE, NetRendering.GATHERED_FLOAT_SIZE);
        }

        int netLight = LevelRenderer.getLightColor(fisher.getLevel(), BlockPos.containing(at));
        ms.pushPose();
        ms.translate(at.x - origin.x, at.y - origin.y, at.z - origin.z);
        NetRendering.renderNet(ms, buffer, netLight, bobTime, phase == Phase.FISHING && fisher.isBiting(), spread,
                pos.asLong(), floatSize);
        ms.popPose();
        Vec3 tie = NetRendering.edgeTowards(at, knot, spread)
                .add(0, .04, 0);
        NetRendering.renderRope(ms, buffer, tie.subtract(origin), knot.subtract(origin));
    }

    // How long the fisher has been in its current phase
    static float phaseTime(MechanicalFisherBlockEntity fisher, float partialTicks) {
        Phase phase = fisher.getPhase();
        return fisher.getPhaseTicks() + (fisher.getSpeed() != 0 && phase != Phase.IDLE ? partialTicks : 0);
    }

    // How far the ejector plate is tipped up
    static float plateAngle(MechanicalFisherBlockEntity fisher, float partialTicks) {
        return fisher.getPhase() == Phase.LAUNCHING ? launcherAngle(phaseTime(fisher, partialTicks)) : 0;
    }

    // Whether the net lies folded on the plate
    static boolean netOnPlate(MechanicalFisherBlockEntity fisher, float partialTicks) {
        Phase phase = fisher.getPhase();
        return fisher.hasNet() && (phase == Phase.IDLE || phase == Phase.LAUNCHING
                && phaseTime(fisher, partialTicks) < FLICK_TICKS);
    }

    // Flicks up fast, then settles back
    private static float launcherAngle(float time) {
        if (time < FLICK_TICKS)
            return LAUNCH_ANGLE * time / FLICK_TICKS;
        return Math.max(0, LAUNCH_ANGLE - (time - FLICK_TICKS) * 8);
    }

    // Turns and tips the ejector plate
    static <T extends Affine<T>> void transformPlate(T buffer, Direction facing, float angle) {
        buffer.center()
                .rotateYDegrees(180 - facing.toYRot())
                .uncenter()
                .translate(HINGE)
                .rotateXDegrees(-angle)
                .translateBack(HINGE);
    }

    @Override
    public boolean shouldRenderOffScreen(MechanicalFisherBlockEntity fisher) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
