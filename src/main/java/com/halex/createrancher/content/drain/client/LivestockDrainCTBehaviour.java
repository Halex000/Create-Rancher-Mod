package com.halex.createrancher.content.drain.client;

import static com.halex.createrancher.CreateRancher.asResource;

import javax.annotation.Nullable;

import com.halex.createrancher.content.drain.LivestockDrainBlock;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

// Connected textures for livestock drains
public class LivestockDrainCTBehaviour extends ConnectedTextureBehaviour.Base {

    public static final CTSpriteShiftEntry TOP = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
            asResource("block/livestock_drain/top"), asResource("block/livestock_drain/top_connected"));
    public static final CTSpriteShiftEntry BOTTOM = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
            asResource("block/livestock_drain/bottom"), asResource("block/livestock_drain/bottom_connected"));
    public static final CTSpriteShiftEntry SIDE = CTSpriteShifter.getCT(AllCTTypes.HORIZONTAL,
            asResource("block/livestock_drain/side"), asResource("block/livestock_drain/side_connected"));

    @Nullable
    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        if (direction == Direction.UP)
            return TOP;
        if (direction == Direction.DOWN)
            return BOTTOM;
        if (direction.getAxis()
                .isHorizontal())
            return SIDE;
        return null;
    }

    // Whether two drains connect
    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
                              BlockPos otherPos, Direction face) {
        if (!LivestockDrainBlock.isDrain(state) || !LivestockDrainBlock.isDrain(other) || pos.getY() != otherPos.getY())
            return false;
        int dx = otherPos.getX() - pos.getX();
        int dz = otherPos.getZ() - pos.getZ();
        if (Math.abs(dx) + Math.abs(dz) == 2)
            return face.getAxis()
                    .isVertical() && Math.abs(dx) == 1 && state.getValue(corner(dx, dz));
        Direction toward = Direction.fromDelta(dx, 0, dz);
        if (toward == null || !LivestockDrainBlock.joins(state, toward) || !LivestockDrainBlock.joins(other,
                toward.getOpposite()))
            return false;
        return face.getAxis()
                .isVertical() || !other.getValue(LivestockDrainBlock.side(face));
    }

    private static BooleanProperty corner(int dx, int dz) {
        if (dz < 0)
            return dx > 0 ? LivestockDrainBlock.NORTH_EAST : LivestockDrainBlock.NORTH_WEST;
        return dx > 0 ? LivestockDrainBlock.SOUTH_EAST : LivestockDrainBlock.SOUTH_WEST;
    }
}
