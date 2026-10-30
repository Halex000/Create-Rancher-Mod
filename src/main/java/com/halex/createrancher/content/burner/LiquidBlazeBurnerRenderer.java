package com.halex.createrancher.content.burner;

import com.halex.createrancher.content.render.OpaqueBatching;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

// Draws the blaze and the fuel in the liquid blaze burner
public class LiquidBlazeBurnerRenderer extends SafeBlockEntityRenderer<LiquidBlazeBurnerBlockEntity> {

    static final float BLAZE_RAISE = 1 / 16f, ROD_RAISE = 1 / 16f;
    private static final float INSIDE_MIN = 3.05f / 16, INSIDE_MAX = 12.95f / 16;
    private static final float FUEL_BOTTOM = 1 / 16f, CLEAR_BOTTOM = 2 / 16f, CLEAR_HEIGHT = 2 / 16f;

    public LiquidBlazeBurnerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(LiquidBlazeBurnerBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource bufferSource, int light, int overlay) {
        bufferSource = OpaqueBatching.of(bufferSource);
        renderFuel(be, ms, bufferSource, light);
        HeatLevel heatLevel = be.getHeatLevelFromBlock();
        Level level = be.getLevel();
        if (heatLevel == HeatLevel.NONE || level == null)
            return;
        float animation = be.headAnimation.getValue(partialTicks) * .175f;
        float horizontalAngle = AngleHelper.rad(be.getHeadAngle(partialTicks));
        PartialModel hat = be.hat ? AllPartialModels.TRAIN_HAT : be.stockKeeper ? AllPartialModels.LOGISTICS_HAT : null;
        renderBlaze(ms, null, bufferSource, level, be.getBlockState(), heatLevel, animation, horizontalAngle,
                heatLevel.isAtLeast(HeatLevel.FADING), be.goggles, hat, be.hashCode(),
                !VisualizationManager.supportsVisualization(be.getLevel()));
    }

    // Draws the burner on a contraption
    public static void renderInContraption(MovementContext context, ContraptionMatrices matrices,
                                           MultiBufferSource bufferSource, LerpedFloat headAngle, boolean conductor) {
        BlockState state = context.state;
        HeatLevel heatLevel = BlazeBurnerBlock.getHeatLevelOf(state);
        if (heatLevel == HeatLevel.NONE)
            return;
        if (!heatLevel.isAtLeast(HeatLevel.FADING))
            heatLevel = HeatLevel.FADING;
        Level level = context.world;
        float horizontalAngle = AngleHelper.rad(headAngle.getValue(AnimationTickHolder.getPartialTicks(level)));
        boolean drawGoggles = context.blockEntityData.contains("Goggles");
        boolean drawHat = conductor || context.blockEntityData.contains("TrainHat");
        renderBlaze(matrices.getViewProjection(), matrices.getModel(), bufferSource, level, state, heatLevel, 0,
                horizontalAngle, false, drawGoggles, drawHat ? AllPartialModels.TRAIN_HAT : null, context.hashCode(), true);
    }

    // How far the blaze's head and rods bob this frame
    static float[] bob(Level level, HeatLevel heatLevel, int hashCode) {
        float time = AnimationTickHolder.getRenderTime(level);
        float renderTick = time + (hashCode % 13) * 16f;
        float offsetMult = heatLevel.isAtLeast(HeatLevel.FADING) ? 64 : 16;
        return new float[] {Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMult,
                Mth.sin((float) ((renderTick / 16f + Math.PI) % (2 * Math.PI))) / offsetMult,
                Mth.sin((float) ((renderTick / 16f + Math.PI / 2) % (2 * Math.PI))) / offsetMult};
    }

    // Draws the blaze, or just its flame when Flywheel draws the rest
    private static void renderBlaze(PoseStack ms, @Nullable PoseStack modelTransform, MultiBufferSource bufferSource,
                                    Level level, BlockState blockState, HeatLevel heatLevel, float animation,
                                    float horizontalAngle, boolean canDrawFlame, boolean drawGoggles,
                                    @Nullable PartialModel drawHat, int hashCode, boolean body) {
        boolean blockAbove = animation > 0.125f;
        float time = AnimationTickHolder.getRenderTime(level);
        float[] bob = bob(level, heatLevel, hashCode);
        float offset1 = bob[1];
        float offset2 = bob[2];
        float headY = bob[0] - (animation * .75f) + BLAZE_RAISE;

        ms.pushPose();
        if (body)
            renderBody(ms, modelTransform, bufferSource, blockState, heatLevel, animation, horizontalAngle, drawGoggles,
                    drawHat, blockAbove, offset1, offset2, headY);

        if (canDrawFlame && blockAbove) {
            SpriteShiftEntry spriteShift = heatLevel == HeatLevel.SEETHING ? AllSpriteShifts.SUPER_BURNER_FLAME
                    : AllSpriteShifts.BURNER_FLAME;
            float spriteWidth = spriteShift.getTarget()
                    .getU1() - spriteShift.getTarget()
                    .getU0();
            float spriteHeight = spriteShift.getTarget()
                    .getV1() - spriteShift.getTarget()
                    .getV0();
            float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();
            double vScroll = speed * time;
            vScroll = vScroll - Math.floor(vScroll);
            vScroll = vScroll * spriteHeight / 2;
            double uScroll = speed * time / 2;
            uScroll = uScroll - Math.floor(uScroll);
            uScroll = uScroll * spriteWidth / 2;
            SuperByteBuffer flameBuffer = CachedBuffers.partial(AllPartialModels.BLAZE_BURNER_FLAME, blockState);
            if (modelTransform != null)
                flameBuffer.transform(modelTransform);
            flameBuffer.translate(0, BLAZE_RAISE, 0);
            flameBuffer.shiftUVScrolling(spriteShift, (float) uScroll, (float) vScroll);
            draw(flameBuffer, horizontalAngle, ms, bufferSource.getBuffer(RenderType.cutoutMipped()));
        }
        ms.popPose();
    }

    // Draws the blaze's head, rods, goggles and hat
    private static void renderBody(PoseStack ms, @Nullable PoseStack modelTransform, MultiBufferSource bufferSource,
                                   BlockState blockState, HeatLevel heatLevel, float animation, float horizontalAngle,
                                   boolean drawGoggles, @Nullable PartialModel drawHat, boolean blockAbove,
                                   float offset1, float offset2, float headY) {
        PartialModel blazeModel = BlazeBurnerRenderer.getBlazeModel(heatLevel, blockAbove);
        SuperByteBuffer blazeBuffer = CachedBuffers.partial(blazeModel, blockState);
        if (modelTransform != null)
            blazeBuffer.transform(modelTransform);
        blazeBuffer.translate(0, headY, 0);
        draw(blazeBuffer, horizontalAngle, ms, bufferSource.getBuffer(RenderType.solid()));

        if (drawGoggles) {
            PartialModel gogglesModel = blazeModel == AllPartialModels.BLAZE_INERT ? AllPartialModels.BLAZE_GOGGLES_SMALL
                    : AllPartialModels.BLAZE_GOGGLES;
            SuperByteBuffer gogglesBuffer = CachedBuffers.partial(gogglesModel, blockState);
            if (modelTransform != null)
                gogglesBuffer.transform(modelTransform);
            gogglesBuffer.translate(0, headY + 8 / 16f, 0);
            draw(gogglesBuffer, horizontalAngle, ms, bufferSource.getBuffer(RenderType.solid()));
        }

        if (drawHat != null) {
            SuperByteBuffer hatBuffer = CachedBuffers.partial(drawHat, blockState);
            if (modelTransform != null)
                hatBuffer.transform(modelTransform);
            hatBuffer.translate(0, headY, 0);
            if (blazeModel == AllPartialModels.BLAZE_INERT) {
                hatBuffer.translateY(0.5f)
                        .center()
                        .scale(0.75f)
                        .uncenter();
            } else {
                hatBuffer.translateY(0.75f);
            }
            hatBuffer.rotateCentered(horizontalAngle + Mth.PI, Direction.UP)
                    .translate(0.5f, 0, 0.5f)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, bufferSource.getBuffer(RenderType.cutoutMipped()));
        }

        if (heatLevel.isAtLeast(HeatLevel.FADING)) {
            PartialModel rodsModel = heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS
                    : AllPartialModels.BLAZE_BURNER_RODS;
            PartialModel rodsModel2 = heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS_2
                    : AllPartialModels.BLAZE_BURNER_RODS_2;
            float rodsY = BLAZE_RAISE + ROD_RAISE;
            SuperByteBuffer rodsBuffer = CachedBuffers.partial(rodsModel, blockState);
            if (modelTransform != null)
                rodsBuffer.transform(modelTransform);
            rodsBuffer.translate(0, offset1 + animation + .125f + rodsY, 0)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
            SuperByteBuffer rodsBuffer2 = CachedBuffers.partial(rodsModel2, blockState);
            if (modelTransform != null)
                rodsBuffer2.transform(modelTransform);
            rodsBuffer2.translate(0, offset2 + animation - 3 / 16f + rodsY, 0)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
        }
    }

    private static void draw(SuperByteBuffer buffer, float horizontalAngle, PoseStack ms, VertexConsumer vc) {
        buffer.rotateCentered(horizontalAngle, Direction.UP)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(ms, vc);
    }

    // Draws the fuel through the windows
    private static void renderFuel(LiquidBlazeBurnerBlockEntity be, PoseStack ms, MultiBufferSource bufferSource,
                                   int light) {
        FluidStack fuel = be.getFuel();
        if (fuel.isEmpty())
            return;
        float full = Math.min(1, fuel.getAmount() / (float) LiquidBlazeBurnerBlockEntity.CAPACITY);
        float top = CLEAR_BOTTOM + CLEAR_HEIGHT * full;
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fuel, INSIDE_MIN, FUEL_BOTTOM, INSIDE_MIN, INSIDE_MAX, top,
                INSIDE_MAX, bufferSource, ms, light, false, true);
    }
}
