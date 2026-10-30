package com.halex.createrancher.content.burner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

// Draws the burner's blaze, rods, goggles and hat with Flywheel
public class LiquidBlazeBurnerVisual extends AbstractBlockEntityVisual<LiquidBlazeBurnerBlockEntity>
        implements SimpleDynamicVisual {

    @Nullable
    private TransformedInstance head;
    @Nullable
    private TransformedInstance goggles;
    @Nullable
    private TransformedInstance hat;
    @Nullable
    private TransformedInstance rods;
    @Nullable
    private TransformedInstance rods2;
    private List<PartialModel> shownModels = List.of();

    public LiquidBlazeBurnerVisual(VisualizationContext context, LiquidBlazeBurnerBlockEntity blockEntity,
                                   float partialTick) {
        super(context, blockEntity, partialTick);
        animate(partialTick);
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        if (!isVisible(context.frustum()) || doDistanceLimitThisFrame(context))
            return;
        animate(context.partialTick());
    }

    // The parts to show
    private List<PartialModel> models(HeatLevel heatLevel, boolean blockAbove) {
        List<PartialModel> models = new ArrayList<>(5);
        if (heatLevel == HeatLevel.NONE) {
            for (int i = 0; i < 5; i++)
                models.add(null);
            return models;
        }
        PartialModel blaze = BlazeBurnerRenderer.getBlazeModel(heatLevel, blockAbove);
        models.add(blaze);
        models.add(blockEntity.goggles ? blaze == AllPartialModels.BLAZE_INERT ? AllPartialModels.BLAZE_GOGGLES_SMALL
                : AllPartialModels.BLAZE_GOGGLES : null);
        models.add(blockEntity.hat ? AllPartialModels.TRAIN_HAT : blockEntity.stockKeeper
                ? AllPartialModels.LOGISTICS_HAT : null);
        boolean hasRods = heatLevel.isAtLeast(HeatLevel.FADING);
        models.add(!hasRods ? null : heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS
                : AllPartialModels.BLAZE_BURNER_RODS);
        models.add(!hasRods ? null : heatLevel == HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS_2
                : AllPartialModels.BLAZE_BURNER_RODS_2);
        return models;
    }

    @Nullable
    private TransformedInstance make(@Nullable PartialModel model) {
        if (model == null)
            return null;
        TransformedInstance instance = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(model))
                .createInstance();
        instance.light(LightTexture.FULL_BRIGHT);
        return instance;
    }

    private void animate(float partialTicks) {
        HeatLevel heatLevel = blockEntity.getHeatLevelFromBlock();
        float animation = blockEntity.headAnimation.getValue(partialTicks) * .175f;
        boolean blockAbove = animation > 0.125f;
        List<PartialModel> models = models(heatLevel, blockAbove);
        if (!models.equals(shownModels)) {
            deleteParts();
            shownModels = models;
            head = make(models.get(0));
            goggles = make(models.get(1));
            hat = make(models.get(2));
            rods = make(models.get(3));
            rods2 = make(models.get(4));
        }
        if (head == null)
            return;

        float horizontalAngle = AngleHelper.rad(blockEntity.getHeadAngle(partialTicks));
        float[] bob = LiquidBlazeBurnerRenderer.bob(level, heatLevel, blockEntity.hashCode());
        float headY = bob[0] - (animation * .75f) + LiquidBlazeBurnerRenderer.BLAZE_RAISE;
        float rodsY = LiquidBlazeBurnerRenderer.BLAZE_RAISE + LiquidBlazeBurnerRenderer.ROD_RAISE;

        head.setIdentityTransform()
                .translate(getVisualPosition())
                .translate(0, headY, 0)
                .rotateCentered(horizontalAngle, Direction.UP)
                .setChanged();
        if (goggles != null)
            goggles.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translate(0, headY + 8 / 16f, 0)
                    .rotateCentered(horizontalAngle, Direction.UP)
                    .setChanged();
        if (hat != null) {
            hat.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translate(0, headY, 0);
            if (shownModels.get(0) == AllPartialModels.BLAZE_INERT)
                hat.translateY(0.5f)
                        .center()
                        .scale(0.75f)
                        .uncenter();
            else
                hat.translateY(0.75f);
            hat.rotateCentered(horizontalAngle + Mth.PI, Direction.UP)
                    .translate(0.5f, 0, 0.5f)
                    .setChanged();
        }
        if (rods != null)
            rods.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translate(0, bob[1] + animation + .125f + rodsY, 0)
                    .setChanged();
        if (rods2 != null)
            rods2.setIdentityTransform()
                    .translate(getVisualPosition())
                    .translate(0, bob[2] + animation - 3 / 16f + rodsY, 0)
                    .setChanged();
    }

    private void deleteParts() {
        for (TransformedInstance part : new TransformedInstance[] {head, goggles, hat, rods, rods2})
            if (part != null)
                part.delete();
        head = goggles = hat = rods = rods2 = null;
    }

    @Override
    public void updateLight(float partialTick) {
    }

    @Override
    protected void _delete() {
        deleteParts();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
    }
}
