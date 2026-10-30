package com.halex.createrancher.content.auger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.halex.createrancher.content.auger.AugerBlock.Slope;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Quaternionf;
import org.joml.Vector3f;

// Draws an auger's axle and helix with Flywheel. Carried items are drawn by AugerRenderer.
public class AugerVisual extends KineticBlockEntityVisual<AugerBlockEntity> implements SimpleDynamicVisual {

    private BlockState shownState;
    @Nullable
    private volatile RotatingInstance axle;
    @Nullable
    private volatile RotatingInstance helix;
    @Nullable
    private volatile TransformedInstance turningAxle;
    private volatile List<TransformedInstance> panes = List.of();
    private long firstUnit;
    private long lastUnit = -1;

    public AugerVisual(VisualizationContext context, AugerBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        build();
    }

    // Creates the instances for the auger's current shape
    private void build() {
        shownState = blockEntity.getBlockState();
        Slope slope = AugerBlock.slopeOf(shownState);
        if (slope == Slope.NONE)
            buildStraight();
        else {
            PartialModel model = slope.kind == Slope.Kind.DIAGONAL ? PartialModelRegistry.AUGER_DIAGONAL_AXLE
                    : PartialModelRegistry.AUGER_JOINT_AXLE;
            turningAxle = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(model))
                    .createInstance();
            relight(turningAxle);
        }
    }

    private void buildStraight() {
        Direction.Axis axis = AugerBlock.axisOf(shownState);
        axle = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT))
                .createInstance();
        axle.rotateToFace(Direction.UP, axis)
                .setup(blockEntity)
                .setPosition(getVisualPosition())
                .setChanged();
        helix = instancerProvider().instancer(AllInstanceTypes.ROTATING,
                        Models.partial(PartialModelRegistry.AUGER_HELIX[shownState.getValue(AugerBlock.PART)
                                .ordinal()]))
                .createInstance();
        helix.setup(blockEntity)
                .setPosition(getVisualPosition());
        helix.rotation.set(AugerRenderer.toAxis(axis));
        helix.setChanged();
        relight(axle, helix);
    }

    // Matches the helix units to the ones a diagonal auger draws right now
    private void matchPanes() {
        long[] units = AugerRenderer.diagonalUnits(blockEntity);
        if (units[0] == firstUnit && units[1] == lastUnit)
            return;
        deletePanes();
        List<TransformedInstance> made = new ArrayList<>();
        for (long unit = units[0]; unit <= units[1]; unit++) {
            TransformedInstance pane = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(
                            Math.floorMod(unit, 2) == 0 ? PartialModelRegistry.AUGER_HELIX_EVEN
                                    : PartialModelRegistry.AUGER_HELIX_ODD))
                    .createInstance();
            relight(pane);
            made.add(pane);
        }
        firstUnit = units[0];
        lastUnit = units[1];
        panes = List.copyOf(made);
    }

    // Spins a diagonal auger's axle and helix units
    private void placeDiagonal(Direction towards) {
        matchPanes();
        Quaternionf tilt = AugerRenderer.toDiagonal(towards);
        Vector3f line = AugerRenderer.diagonalLine(towards);
        float angle = AugerRenderer.diagonalAngle(blockEntity);
        double middle = AugerRenderer.diagonalMiddle(blockEntity);
        turningAxle.setIdentityTransform()
                .translate(getVisualPosition())
                .translate(line)
                .rotateCentered(tilt)
                .rotateCentered(angle, Direction.UP)
                .setChanged();
        List<TransformedInstance> shown = panes;
        for (int i = 0; i < shown.size(); i++)
            shown.get(i)
                    .setIdentityTransform()
                    .translate(getVisualPosition())
                    .translate(line)
                    .rotateCentered(tilt)
                    .rotateCentered(angle, Direction.UP)
                    .translate(0, (float) (((firstUnit + i) * 4 - middle + 8) / 16), 0)
                    .setChanged();
    }

    // Spins the axle where the column turns
    private void placeJoint(Slope slope) {
        turningAxle.setIdentityTransform()
                .translate(getVisualPosition())
                .rotateCentered(AugerRenderer.jointAngle(blockEntity), Direction.UP);
        if (slope.kind == Slope.Kind.END)
            turningAxle.rotateCentered(Mth.PI, Direction.EAST);
        turningAxle.setChanged();
    }

    private void deletePanes() {
        List<TransformedInstance> old = panes;
        panes = List.of();
        for (TransformedInstance pane : old)
            pane.delete();
        firstUnit = 0;
        lastUnit = -1;
    }

    private void clear() {
        if (axle != null)
            axle.delete();
        if (helix != null)
            helix.delete();
        if (turningAxle != null)
            turningAxle.delete();
        deletePanes();
        axle = null;
        helix = null;
        turningAxle = null;
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        if (blockEntity.getBlockState() != shownState) {
            clear();
            build();
        }
        if (turningAxle == null)
            return;
        Slope slope = AugerBlock.slopeOf(shownState);
        if (slope.kind == Slope.Kind.DIAGONAL)
            placeDiagonal(slope.leaning());
        else
            placeJoint(slope);
    }

    @Override
    public void update(float partialTick) {
        if (axle != null)
            axle.setup(blockEntity)
                    .setChanged();
        if (helix != null)
            helix.setup(blockEntity)
                    .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        RotatingInstance shaft = axle;
        RotatingInstance blades = helix;
        TransformedInstance turning = turningAxle;
        if (shaft != null)
            relight(shaft);
        if (blades != null)
            relight(blades);
        if (turning != null)
            relight(turning);
        for (TransformedInstance pane : panes)
            relight(pane);
    }

    @Override
    protected void _delete() {
        clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (axle != null)
            consumer.accept(axle);
        if (helix != null)
            consumer.accept(helix);
        if (turningAxle != null)
            consumer.accept(turningAxle);
        panes.forEach(consumer);
    }
}
