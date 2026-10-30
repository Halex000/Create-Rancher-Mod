package com.halex.createrancher.content.drain.client;

import java.util.ArrayList;
import java.util.List;

import com.halex.createrancher.content.drain.LivestockDrainBlock;
import com.halex.createrancher.content.drain.LivestockDrainBlockEntity;
import com.halex.createrancher.content.drain.LivestockDrainGroup;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.createmod.catnip.render.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

// Draws the fluids in a livestock drain
public class LivestockDrainRenderer extends SmartBlockEntityRenderer<LivestockDrainBlockEntity> {

    private static final float FLOOR = 5 / 16f;
    private static final float DEPTH = 10 / 16f;
    private static final float LOWEST = 1 / 64f;
    private static final float OVERLAP = 1 / 1024f;

    public LivestockDrainRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(LivestockDrainBlockEntity drain, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(drain, partialTicks, ms, buffer, light, overlay);
        List<FluidStack> share = drain.getShare();
        int total = drain.getShareAmount();
        float level = drain.getShownLevel(partialTicks);
        if (total == 0 || level <= 0)
            return;
        LivestockDrainGroup group = drain.getGroup();
        float surface = FLOOR + Math.max(LOWEST, DEPTH * level);
        boolean alongX = group.getAxis() == Direction.Axis.X;
        List<float[]> open = openCells(drain.getBlockState());
        VertexConsumer builder = FluidRenderHelper.getFluidBuilder(buffer);

        int before = 0;
        for (FluidStack fluid : share) {
            float[] strip = group.stripWithin(drain.getBlockPos(), before / (float) total,
                    (before + fluid.getAmount()) / (float) total);
            before += fluid.getAmount();
            if (strip[1] <= strip[0])
                continue;
            IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(client.getStillTexture(fluid));
            int colour = client.getTintColor(fluid);
            int glow = Math.max((light >> 4) & 0xF, fluid.getFluidType()
                    .getLightLevel(fluid));
            int fluidLight = (light & 0xF00000) | glow << 4;
            for (float[] cell : open) {
                float x0 = cell[0], z0 = cell[1], x1 = cell[2], z1 = cell[3];
                if (alongX) {
                    x0 = Math.max(x0, strip[0]);
                    x1 = Math.min(x1, strip[1]);
                } else {
                    z0 = Math.max(z0, strip[0]);
                    z1 = Math.min(z1, strip[1]);
                }
                if (x1 > x0 && z1 > z0)
                    FluidRenderHelper.renderStillTiledFace(Direction.UP, x0 - OVERLAP, z0 - OVERLAP, x1 + OVERLAP,
                            z1 + OVERLAP, surface, builder, ms, fluidLight, colour, sprite);
            }
        }
    }

    // The open parts inside the drain
    static List<float[]> openCells(BlockState state) {
        float w = 2 / 16f, e = 14 / 16f;
        boolean north = state.getValue(LivestockDrainBlock.NORTH);
        boolean south = state.getValue(LivestockDrainBlock.SOUTH);
        boolean west = state.getValue(LivestockDrainBlock.WEST);
        boolean east = state.getValue(LivestockDrainBlock.EAST);
        List<float[]> cells = new ArrayList<>();
        cells.add(new float[] {w, w, e, e});
        if (north)
            cells.add(new float[] {w, 0, e, w});
        if (south)
            cells.add(new float[] {w, e, e, 1});
        if (west)
            cells.add(new float[] {0, w, w, e});
        if (east)
            cells.add(new float[] {e, w, 1, e});
        if (north && west && state.getValue(LivestockDrainBlock.NORTH_WEST))
            cells.add(new float[] {0, 0, w, w});
        if (north && east && state.getValue(LivestockDrainBlock.NORTH_EAST))
            cells.add(new float[] {e, 0, 1, w});
        if (south && west && state.getValue(LivestockDrainBlock.SOUTH_WEST))
            cells.add(new float[] {0, e, w, 1});
        if (south && east && state.getValue(LivestockDrainBlock.SOUTH_EAST))
            cells.add(new float[] {e, e, 1, 1});
        return cells;
    }
}
