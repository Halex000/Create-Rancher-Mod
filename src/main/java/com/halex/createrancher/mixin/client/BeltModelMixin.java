package com.halex.createrancher.mixin.client;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.halex.createrancher.content.grate.BeltGrates;
import com.halex.createrancher.index.PartialModelRegistry;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltModel;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

// Uses a shorter casing under grated belt segments
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(BeltModel.class)
public abstract class BeltModelMixin {

    @ModifyVariable(method = "getQuads", at = @At(value = "STORE", ordinal = 0))
    private List<BakedQuad> createrancher$lowerCasing(List<BakedQuad> quads, BlockState state, Direction side,
                                                      RandomSource rand, ModelData extraData,
                                                      RenderType renderType) {
        Integer slopes = extraData.get(BeltGrates.GRATED_CASING_PROPERTY);
        if (quads.isEmpty() || slopes == null || !BeltGrates.canHoldGrate(state))
            return quads;
        boolean alongX = state.getValue(BeltBlock.HORIZONTAL_FACING)
                .getAxis() == Direction.Axis.X;
        PartialModel[] casings = alongX ? PartialModelRegistry.GRATED_BELT_CASING_X
                : PartialModelRegistry.GRATED_BELT_CASING_Z;
        return casings[slopes].get()
                .getQuads(state, side, rand, ModelData.EMPTY, renderType);
    }
}
