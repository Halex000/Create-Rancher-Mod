package com.halex.createrancher.content.grate.client;

import javax.annotation.Nullable;

import com.halex.createrancher.content.grate.BeltGrates;
import com.halex.createrancher.content.render.OpaqueBatching;
import com.halex.createrancher.index.PartialModelRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// Draws grated belt segments
public class BeltGrateClient {

    // Picks the belt and grate models for a segment
    private static PartialModel pick(PartialModel flat, PartialModel previousSlope, PartialModel nextSlope,
                                     PartialModel bothSlopes, boolean slopeToPrevious, boolean slopeToNext) {
        if (slopeToPrevious && slopeToNext)
            return bothSlopes;
        if (slopeToPrevious)
            return previousSlope;
        return slopeToNext ? nextSlope : flat;
    }

    // The belt model for a grated segment, or null
    @Nullable
    public static PartialModel getLoweredBelt(BeltBlockEntity belt) {
        if (!BeltGrates.isGrated(belt) || !BeltGrates.canHoldGrate(belt.getBlockState()))
            return null;
        Direction facing = facing(belt.getBlockState());
        Level level = belt.getLevel();
        if (level == null)
            return null;
        BlockPos pos = belt.getBlockPos();
        return pick(PartialModelRegistry.GRATED_BELT_FLAT, PartialModelRegistry.GRATED_BELT_SLOPE_NORTH,
                PartialModelRegistry.GRATED_BELT_SLOPE_SOUTH, PartialModelRegistry.GRATED_BELT_SLOPE_BOTH,
                !BeltGrates.isNeighbourGrated(level, pos, facing.getOpposite()),
                !BeltGrates.isNeighbourGrated(level, pos, facing));
    }

    // The grate model for a segment, or null
    @Nullable
    public static PartialModel getGrate(BeltBlockEntity belt) {
        if (!BeltGrates.isGrated(belt) || !BeltGrates.canHoldGrate(belt.getBlockState()))
            return null;
        Direction facing = facing(belt.getBlockState());
        Level level = belt.getLevel();
        if (level == null)
            return null;
        BlockPos pos = belt.getBlockPos();
        PartialModel[] grates = PartialModelRegistry.BELT_GRATES[BeltGrates.getMetal(belt)
                .ordinal()];
        return pick(grates[0], grates[1], grates[2], grates[3],
                !BeltGrates.isNeighbourGrated(level, pos, facing.getOpposite()),
                !BeltGrates.isNeighbourGrated(level, pos, facing));
    }

    // Turns the models to face along the belt
    public static float modelYRot(BlockState state) {
        return AngleHelper.horizontalAngle(facing(state));
    }

    private static Direction facing(BlockState state) {
        return state.getValue(BeltBlock.HORIZONTAL_FACING);
    }

    // Draws a segment's grate without Flywheel
    public static void renderGrate(BeltBlockEntity belt, PoseStack ms, MultiBufferSource buffer, int light) {
        PartialModel grate = getGrate(belt);
        if (grate == null)
            return;
        CachedBuffers.partial(grate, belt.getBlockState())
                .center()
                .rotateYDegrees(modelYRot(belt.getBlockState()))
                .uncenter()
                .light(light)
                .renderInto(ms, OpaqueBatching.of(buffer)
                        .getBuffer(RenderType.cutout()));
    }

    // How far down an item on a grated belt is drawn
    public static float itemDrop(BeltBlockEntity controller, float offset) {
        Level level = controller.getLevel();
        if (level == null || controller.beltLength <= 0)
            return 0;
        int index = Mth.clamp(Mth.floor(offset), 0, controller.beltLength - 1);
        Direction facing = facing(controller.getBlockState());
        BlockPos pos = controller.getBlockPos()
                .relative(facing, index);
        if (!BeltGrates.isGrated(level, pos))
            return 0;

        float along = offset - index;
        float drop = BeltGrates.BELT_DROP;
        if (!BeltGrates.isNeighbourGrated(level, pos, facing.getOpposite()))
            drop = Math.min(drop, Math.max(along, 0));
        if (!BeltGrates.isNeighbourGrated(level, pos, facing))
            drop = Math.min(drop, Math.max(1 - along, 0));
        return -drop;
    }

    // Redraws a segment when its grate changes
    public static void onGrateChanged(BeltBlockEntity belt) {
        Level level = belt.getLevel();
        VisualizationManager manager = VisualizationManager.get(level);
        Direction facing = facing(belt.getBlockState());
        for (BlockPos pos : new BlockPos[] {belt.getBlockPos(), belt.getBlockPos()
                .relative(facing), belt.getBlockPos()
                .relative(facing.getOpposite())}) {
            if (!(level.getBlockEntity(pos) instanceof BeltBlockEntity be))
                continue;
            refreshModel(be);
            if (manager == null)
                continue;
            manager.blockEntities()
                    .queueRemove(be);
            manager.blockEntities()
                    .queueAdd(be);
        }
    }

    // Redraws grated segments when a casing changes
    public static void onCasingChanged(BeltBlockEntity belt) {
        Level level = belt.getLevel();
        Direction facing = facing(belt.getBlockState());
        for (Direction side : new Direction[] {facing, facing.getOpposite()})
            if (level.getBlockEntity(belt.getBlockPos()
                    .relative(side)) instanceof BeltBlockEntity neighbour && BeltGrates.isGrated(neighbour))
                refreshModel(neighbour);
    }

    private static void refreshModel(BeltBlockEntity belt) {
        if (!belt.isVirtual())
            belt.requestModelDataUpdate();
        belt.getLevel()
                .sendBlockUpdated(belt.getBlockPos(), belt.getBlockState(), belt.getBlockState(), 16);
    }
}
