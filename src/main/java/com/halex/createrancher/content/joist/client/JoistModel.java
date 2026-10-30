package com.halex.createrancher.content.joist.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.joist.JoistWood;
import com.simibubi.create.content.decoration.girder.GirderBlock;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

// A joist's model with its brackets
public class JoistModel extends BakedModelWrapper<BakedModel> {

    private static final ModelProperty<Integer> BRACKETS = new ModelProperty<>();

    private final ModelResourceLocation[] brackets = new ModelResourceLocation[4];

    public JoistModel(BakedModel original, JoistWood wood) {
        super(original);
        for (Direction side : Iterate.horizontalDirections)
            brackets[side.get2DDataValue()] = bracket(wood, side);
    }

    public static ModelResourceLocation bracket(JoistWood wood, Direction side) {
        return ModelResourceLocation.standalone(CreateRancher.asResource("block/" + wood.id()
                .getPath() + "/bracket_" + side.getSerializedName()));
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                           @NotNull BlockState state, @NotNull ModelData modelData) {
        int connected = 0;
        for (Direction side : Iterate.horizontalDirections)
            if (GirderBlock.isConnected(level, pos, state, side))
                connected |= 1 << side.get2DDataValue();
        return modelData.derive()
                .with(BRACKETS, connected)
                .build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource rand, @NotNull ModelData data,
                                             @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        Integer connected = data.get(BRACKETS);
        if (side != null || connected == null || connected == 0)
            return quads;
        List<BakedQuad> withBrackets = new ArrayList<>(quads);
        for (int i = 0; i < 4; i++)
            if ((connected & 1 << i) != 0)
                withBrackets.addAll(Minecraft.getInstance()
                        .getModelManager()
                        .getModel(brackets[i])
                        .getQuads(state, null, rand, ModelData.EMPTY, renderType));
        return withBrackets;
    }
}
