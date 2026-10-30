package com.halex.createrancher.content.fisher.client;

import java.util.function.Consumer;

import com.halex.createrancher.content.fisher.MechanicalFisherBlock;
import com.halex.createrancher.content.fisher.MechanicalFisherBlockEntity;
import com.halex.createrancher.index.PartialModelRegistry;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.minecraft.core.Direction;

// Draws the fisher's cog, plate, folded net and knot with Flywheel
public class MechanicalFisherVisual extends KineticBlockEntityVisual<MechanicalFisherBlockEntity>
        implements SimpleDynamicVisual {

    private final RotatingInstance cog;
    private final TransformedInstance plate;
    private final TransformedInstance pile;
    private final TransformedInstance knot;
    private float shownAngle = Float.NaN;
    private boolean shownPile;
    private boolean shownKnot;
    private Direction shownFacing;

    public MechanicalFisherVisual(VisualizationContext context, MechanicalFisherBlockEntity blockEntity,
                                  float partialTick) {
        super(context, blockEntity, partialTick);
        cog = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(
                        AllPartialModels.SHAFTLESS_COGWHEEL))
                .createInstance();
        cog.setup(blockEntity)
                .setPosition(getVisualPosition())
                .setChanged();
        plate = part(PartialModelRegistry.FISHER_PLATE);
        pile = part(PartialModelRegistry.FISHER_NET_PILE);
        knot = part(PartialModelRegistry.FISHER_KNOT);
        relight(cog, plate, pile, knot);
        place(partialTick);
    }

    private TransformedInstance part(PartialModel model) {
        return instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(model))
                .createInstance();
    }

    // Moves the plate, net and knot when something changed
    private void place(float partialTick) {
        Direction facing = blockEntity.getBlockState()
                .getValue(MechanicalFisherBlock.HORIZONTAL_FACING);
        float angle = MechanicalFisherRenderer.plateAngle(blockEntity, partialTick);
        boolean showPile = MechanicalFisherRenderer.netOnPlate(blockEntity, partialTick);
        boolean showKnot = blockEntity.hasNet();
        if (angle == shownAngle && showPile == shownPile && showKnot == shownKnot && facing == shownFacing)
            return;
        shownAngle = angle;
        shownPile = showPile;
        shownKnot = showKnot;
        shownFacing = facing;
        tilt(plate, facing, angle, true);
        tilt(pile, facing, angle, showPile);
        tilt(knot, facing, 0, showKnot);
    }

    private void tilt(TransformedInstance part, Direction facing, float angle, boolean shown) {
        if (!shown) {
            part.setZeroTransform()
                    .setChanged();
            return;
        }
        part.setIdentityTransform()
                .translate(getVisualPosition());
        MechanicalFisherRenderer.transformPlate(part, facing, angle);
        part.setChanged();
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        place(context.partialTick());
    }

    @Override
    public void update(float partialTick) {
        cog.setup(blockEntity)
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(cog, plate, pile, knot);
    }

    @Override
    protected void _delete() {
        cog.delete();
        plate.delete();
        pile.delete();
        knot.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(cog);
        consumer.accept(plate);
        consumer.accept(pile);
        consumer.accept(knot);
    }
}
