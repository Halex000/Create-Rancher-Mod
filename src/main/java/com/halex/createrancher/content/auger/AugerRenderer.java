package com.halex.createrancher.content.auger;

import java.util.function.Predicate;

import com.halex.createrancher.content.auger.AugerBlock.Part;
import com.halex.createrancher.content.auger.AugerBlock.Slope;
import com.halex.createrancher.content.auger.AugerBlockEntity.Load;
import com.halex.createrancher.content.render.OpaqueBatching;
import com.halex.createrancher.index.PartialModelRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Quaternionf;
import org.joml.Vector3f;

// Draws the axle, blades and items inside an auger
public class AugerRenderer extends SafeBlockEntityRenderer<AugerBlockEntity> {

    private static final double ITEM_REACH = 2.5;

    private static final ItemFit FLAT = new ItemFit(5f, 4.4f, 4.3f / 16, .1f, .3f, .05f);
    private static final ItemFit BLOCK = new ItemFit(5f, 4.6f, 3.1f / 8, .1f, -.15f, .1f);

    // Where and how big an item sits on the blades
    private record ItemFit(float radius, float lift, float scale, float along, float out, float up) {
    }

    static final double HALF = 8 * Math.sqrt(2);

    private final ItemRenderer itemRenderer;

    public AugerRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    // Rotates the helix to the auger's axis
    static Quaternionf toAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Quaternionf().rotationZ(-Mth.HALF_PI);
            case Y -> new Quaternionf();
            case Z -> new Quaternionf().rotationX(Mth.HALF_PI);
        };
    }

    // Turns the helix to go up a diagonal
    static Quaternionf toDiagonal(Direction towards) {
        return new Quaternionf().rotationTo(0, 1, 0, towards.getStepX(), 1, towards.getStepZ());
    }

    @Override
    protected void renderSafe(AugerBlockEntity auger, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        buffer = OpaqueBatching.of(buffer);
        Slope slope = AugerBlock.slopeOf(auger.getBlockState());
        if (slope.kind == Slope.Kind.DIAGONAL)
            renderDiagonal(auger, slope.leaning(), partialTicks, ms, buffer, light, overlay);
        else if (slope == Slope.NONE)
            renderStraight(auger, partialTicks, ms, buffer, light, overlay);
        else {
            if (!VisualizationManager.supportsVisualization(auger.getLevel()))
                renderJointAxle(auger, slope, ms, buffer, light);
            boolean start = slope.kind == Slope.Kind.START;
            renderDiagonalLoads(auger, slope.leaning(), partialTicks, middle -> start ? middle >= HALF - ITEM_REACH
                    : middle <= ITEM_REACH - HALF, ms, buffer, light, overlay);
        }
    }

    // How far the axle where the column turns has spun
    static float jointAngle(AugerBlockEntity auger) {
        return KineticBlockEntityRenderer.getAngleForBe(auger, auger.getBlockPos(), Direction.Axis.Y);
    }

    // The first and last helix units a diagonal auger draws, in quarter blocks along the diagonal
    static long[] diagonalUnits(AugerBlockEntity auger) {
        double middle = diagonalMiddle(auger);
        double from = middle - HALF - (turns(auger, false) ? 6 : 0);
        double to = middle + HALF + (turns(auger, true) ? 4 : 0);
        return new long[] {(long) Math.ceil(from / 4), (long) Math.ceil(to / 4) - 1};
    }

    // Draws the axle where the column turns
    private void renderJointAxle(AugerBlockEntity auger, Slope slope, PoseStack ms, MultiBufferSource buffer,
                                 int light) {
        float angle = jointAngle(auger);
        SuperByteBuffer axle = CachedBuffers.partial(PartialModelRegistry.AUGER_JOINT_AXLE, auger.getBlockState());
        axle.rotateCentered(angle, Direction.UP);
        if (slope.kind == Slope.Kind.END)
            axle.rotateCentered(Mth.PI, Direction.EAST);
        axle.light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    private void renderStraight(AugerBlockEntity auger, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                                int light, int overlay) {
        BlockState state = auger.getBlockState();
        Part part = state.getValue(AugerBlock.PART);
        Direction.Axis axis = AugerBlock.axisOf(state);
        Quaternionf turn = toAxis(axis);
        float angle = KineticBlockEntityRenderer.getAngleForBe(auger, auger.getBlockPos(), axis);

        if (!VisualizationManager.supportsVisualization(auger.getLevel())) {
            VertexConsumer solid = buffer.getBuffer(RenderType.solid());
            SuperByteBuffer axle = CachedBuffers.block(KineticBlockEntityRenderer.shaft(axis));
            KineticBlockEntityRenderer.kineticRotationTransform(axle, auger, axis, angle, light)
                    .renderInto(ms, solid);
            SuperByteBuffer helix = CachedBuffers.partial(PartialModelRegistry.AUGER_HELIX[part.ordinal()], state);
            helix.rotateCentered(turn);
            KineticBlockEntityRenderer.kineticRotationTransform(helix, auger, Direction.Axis.Y, angle, light)
                    .renderInto(ms, solid);
        }

        for (Load load : auger.getLoads()) {
            float height = Mth.lerp(partialTicks, load.prevHeight, load.height);
            float around = angle - Mth.TWO_PI * height;
            renderLoad(auger, load.stack, around, height, turn, ms, buffer, light, overlay);
        }
    }

    // Rotation angle for diagonal augers
    static float diagonalAngle(AugerBlockEntity auger) {
        float time = AnimationTickHolder.getRenderTime(auger.getLevel());
        return ((time * auger.getSpeed() * 3f / 10) % 360) / 180 * Mth.PI;
    }

    // How far along the diagonal an auger's middle is
    static double diagonalMiddle(AugerBlockEntity auger) {
        return 2 * HALF * auger.getBlockPos()
                .getY();
    }

    // Moves the auger's middle onto the diagonal
    static Vector3f diagonalLine(Direction towards) {
        float shift = AugerBlock.DIAGONAL_SHIFT / 16;
        return new Vector3f(towards.getStepX() * shift, -shift, towards.getStepZ() * shift);
    }

    // Draws a diagonal auger
    private void renderDiagonal(AugerBlockEntity auger, Direction towards, float partialTicks, PoseStack ms,
                                MultiBufferSource buffer, int light, int overlay) {
        BlockState state = auger.getBlockState();
        Quaternionf tilt = toDiagonal(towards);
        Vector3f line = diagonalLine(towards);
        float angle = diagonalAngle(auger);
        double middle = diagonalMiddle(auger);

        if (VisualizationManager.supportsVisualization(auger.getLevel())) {
            renderDiagonalLoads(auger, towards, partialTicks, item -> true, ms, buffer, light, overlay);
            return;
        }
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());
        SuperByteBuffer axle = CachedBuffers.partial(PartialModelRegistry.AUGER_DIAGONAL_AXLE, state);
        axle.translate(line);
        axle.rotateCentered(tilt);
        axle.rotateCentered(angle, Direction.UP);
        axle.light(light)
                .renderInto(ms, solid);

        long[] units = diagonalUnits(auger);
        for (long unit = units[0]; unit <= units[1]; unit++) {
            SuperByteBuffer panes = CachedBuffers.partial(Math.floorMod(unit, 2) == 0
                    ? PartialModelRegistry.AUGER_HELIX_EVEN : PartialModelRegistry.AUGER_HELIX_ODD, state);
            panes.translate(line);
            panes.rotateCentered(tilt);
            panes.rotateCentered(angle, Direction.UP);
            panes.translate(0, (unit * 4 - middle + 8) / 16, 0);
            panes.light(light)
                    .renderInto(ms, solid);
        }

        renderDiagonalLoads(auger, towards, partialTicks, item -> true, ms, buffer, light, overlay);
    }

    // Draws the items on a diagonal auger
    private void renderDiagonalLoads(AugerBlockEntity auger, Direction towards, float partialTicks,
                                     Predicate<Double> drawn, PoseStack ms, MultiBufferSource buffer, int light,
                                     int overlay) {
        Quaternionf tilt = toDiagonal(towards);
        Vector3f line = diagonalLine(towards);
        float angle = diagonalAngle(auger);
        double middle = diagonalMiddle(auger);
        ms.pushPose();
        ms.translate(line.x(), line.y(), line.z());
        for (Load load : auger.getLoads()) {
            float height = Mth.lerp(partialTicks, load.prevHeight, load.height);
            double distance = (height - .5) * 2 * HALF;
            ItemFit fit = itemRenderer.getModel(load.stack, auger.getLevel(), null, 0)
                    .isGui3d() ? BLOCK : FLAT;
            if (!drawn.test(distance + fit.lift()))
                continue;
            double turnsAlong = (middle + distance) / 16;
            float around = (float) (angle - Mth.TWO_PI * (turnsAlong - Math.floor(turnsAlong)));
            renderLoad(auger, load.stack, around, (float) (.5 + distance / 16), tilt, ms, buffer, light, overlay);
        }
        ms.popPose();
    }

    // Whether the auger before or after this one is where the column turns
    static boolean turns(AugerBlockEntity auger, boolean onwards) {
        BlockState state = auger.getBlockState();
        BlockPos at = onwards ? AugerBlock.nextPos(state, auger.getBlockPos()) : AugerBlock.previousPos(state,
                auger.getBlockPos());
        return AugerBlock.slopeOf(auger.getLevel()
                        .getBlockState(at))
                .kind != Slope.Kind.DIAGONAL;
    }

    // Draws one stack
    private void renderLoad(AugerBlockEntity auger, ItemStack stack, float around, float height, Quaternionf turn,
                            PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ItemFit fit = itemRenderer.getModel(stack, auger.getLevel(), null, 0)
                .isGui3d() ? BLOCK : FLAT;
        int copies = stack.getCount() > 32 ? 3 : stack.getCount() > 1 ? 2 : 1;
        ms.pushPose();
        ms.translate(.5, .5, .5);
        ms.mulPose(turn);
        ms.translate(0, height - .5 + fit.lift() / 16, 0);
        ms.mulPose(Axis.YP.rotation(around));
        ms.translate(fit.radius() / 16, 0, 0);
        for (int i = 0; i < copies; i++) {
            ms.pushPose();
            float rise = i == 0 ? 0 : i == 1 ? fit.up() : -fit.up();
            ms.translate(i * fit.out() / 16, rise / 16, -i * fit.along() / 16);
            ms.mulPose(Axis.YP.rotationDegrees(90));
            ms.scale(fit.scale(), fit.scale(), fit.scale());
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ms, buffer, auger.getLevel(),
                    0);
            ms.popPose();
        }
        ms.popPose();
    }
}
