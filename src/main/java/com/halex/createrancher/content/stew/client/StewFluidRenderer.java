package com.halex.createrancher.content.stew.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.blaze3d.vertex.VertexConsumer;

// Draws placed stews with textures that line up between blocks
public class StewFluidRenderer {

    private static final float INSET = .001f;

    public static void render(BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer, BlockState blockState,
                              FluidState fluidState, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        TextureAtlasSprite still = sprite(stillTexture);
        TextureAtlasSprite flowing = sprite(flowingTexture);
        Fluid fluid = fluidState.getType();

        BlockState above = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        boolean drawUp = LiquidBlockRenderer.shouldRenderFace(level, pos, fluidState, blockState, Direction.UP,
                above.getFluidState());
        boolean drawDown = LiquidBlockRenderer.shouldRenderFace(level, pos, fluidState, blockState, Direction.DOWN,
                below.getFluidState()) && !occludes(level, pos.below(), below, Direction.DOWN, .8888889f);

        float nw, ne, sw, se;
        float own = height(level, fluid, pos, blockState, fluidState);
        if (own >= 1) {
            nw = ne = sw = se = 1;
        } else {
            float north = height(level, fluid, pos.north());
            float south = height(level, fluid, pos.south());
            float east = height(level, fluid, pos.east());
            float west = height(level, fluid, pos.west());
            ne = corner(level, fluid, own, north, east, pos.north()
                    .east());
            nw = corner(level, fluid, own, north, west, pos.north()
                    .west());
            se = corner(level, fluid, own, south, east, pos.south()
                    .east());
            sw = corner(level, fluid, own, south, west, pos.south()
                    .west());
        }

        float x = pos.getX() & 15;
        float y = pos.getY() & 15;
        float z = pos.getZ() & 15;
        float bottom = drawDown ? INSET : 0;
        int light = light(level, pos);

        if (drawUp && !occludes(level, pos.above(), above, Direction.UP, Math.min(Math.min(nw, ne), Math.min(sw, se)))) {
            float shade = level.getShade(Direction.UP, true);
            float hnw = nw - INSET, hne = ne - INSET, hsw = sw - INSET, hse = se - INSET;
            Vec3 flow = fluidState.getFlow(level, pos);
            boolean moving = flow.x != 0 || flow.z != 0;
            TextureAtlasSprite top = moving ? flowing : still;
            float[][] uv = moving ? flowingTop(flow) : new float[][] {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
            quad(buffer, top, shade, light,
                    x, y + hnw, z, uv[0][0], uv[0][1],
                    x, y + hsw, z + 1, uv[1][0], uv[1][1],
                    x + 1, y + hse, z + 1, uv[2][0], uv[2][1],
                    x + 1, y + hne, z, uv[3][0], uv[3][1]);
            if (fluidState.shouldRenderBackwardUpFace(level, pos.above()))
                quad(buffer, top, shade, light,
                        x + 1, y + hne, z, uv[3][0], uv[3][1],
                        x + 1, y + hse, z + 1, uv[2][0], uv[2][1],
                        x, y + hsw, z + 1, uv[1][0], uv[1][1],
                        x, y + hnw, z, uv[0][0], uv[0][1]);
        }

        if (drawDown) {
            float shade = level.getShade(Direction.DOWN, true);
            quad(buffer, still, shade, light(level, pos.below()),
                    x, y + bottom, z + 1, 0, 1,
                    x, y + bottom, z, 0, 0,
                    x + 1, y + bottom, z, 1, 0,
                    x + 1, y + bottom, z + 1, 1, 1);
        }

        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(side);
            BlockState nextState = level.getBlockState(next);
            if (!LiquidBlockRenderer.shouldRenderFace(level, pos, fluidState, blockState, side, nextState
                    .getFluidState()))
                continue;
            float x0, z0, x1, z1, h0, h1;
            switch (side) {
                case NORTH -> {
                    x0 = x;
                    z0 = z + INSET;
                    x1 = x + 1;
                    z1 = z + INSET;
                    h0 = nw;
                    h1 = ne;
                }
                case SOUTH -> {
                    x0 = x + 1;
                    z0 = z + 1 - INSET;
                    x1 = x;
                    z1 = z + 1 - INSET;
                    h0 = se;
                    h1 = sw;
                }
                case WEST -> {
                    x0 = x + INSET;
                    z0 = z + 1;
                    x1 = x + INSET;
                    z1 = z;
                    h0 = sw;
                    h1 = nw;
                }
                default -> {
                    x0 = x + 1 - INSET;
                    z0 = z;
                    x1 = x + 1 - INSET;
                    z1 = z + 1;
                    h0 = ne;
                    h1 = se;
                }
            }
            if (occludes(level, next, nextState, side, Math.max(h0, h1)))
                continue;
            boolean alongX = side.getAxis() == Direction.Axis.Z;
            float u0 = (alongX ? x0 - x : z0 - z) * .5f;
            float u1 = (alongX ? x1 - x : z1 - z) * .5f;
            float shade = level.getShade(side.getAxis() == Direction.Axis.Z ? Direction.NORTH : Direction.WEST, true);
            int sideLight = light(level, next);
            quad(buffer, flowing, shade, sideLight,
                    x0, y + h0, z0, u0, (1 - h0) * .5f,
                    x1, y + h1, z1, u1, (1 - h1) * .5f,
                    x1, y + bottom, z1, u1, (1 - bottom) * .5f,
                    x0, y + bottom, z0, u0, (1 - bottom) * .5f);
            quad(buffer, flowing, shade, sideLight,
                    x0, y + bottom, z0, u0, (1 - bottom) * .5f,
                    x1, y + bottom, z1, u1, (1 - bottom) * .5f,
                    x1, y + h1, z1, u1, (1 - h1) * .5f,
                    x0, y + h0, z0, u0, (1 - h0) * .5f);
        }
    }

    // Texture corners for a flowing top
    private static float[][] flowingTop(Vec3 flow) {
        float[][] corners = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        float[][] uv = new float[4][2];
        boolean alongX = Math.abs(flow.x) > Math.abs(flow.z);
        for (int i = 0; i < 4; i++) {
            float cx = corners[i][0];
            float cz = corners[i][1];
            float u, v;
            if (alongX && flow.x > 0) {
                u = 1 - cz;
                v = cx;
            } else if (alongX) {
                u = cz;
                v = 1 - cx;
            } else if (flow.z > 0) {
                u = cx;
                v = cz;
            } else {
                u = 1 - cx;
                v = 1 - cz;
            }
            uv[i][0] = u * .5f;
            uv[i][1] = v * .5f;
        }
        return uv;
    }

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(texture);
    }

    // How full a block is
    private static float height(BlockAndTintGetter level, Fluid fluid, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return height(level, fluid, pos, state, state.getFluidState());
    }

    private static float height(BlockAndTintGetter level, Fluid fluid, BlockPos pos, BlockState state,
                                FluidState fluidState) {
        if (fluid.isSame(fluidState.getType()))
            return fluid.isSame(level.getBlockState(pos.above())
                    .getFluidState()
                    .getType()) ? 1 : fluidState.getOwnHeight();
        return state.isSolid() ? -1 : 0;
    }

    // A corner's height
    private static float corner(BlockAndTintGetter level, Fluid fluid, float own, float a, float b, BlockPos diagonal) {
        if (a >= 1 || b >= 1)
            return 1;
        float[] sum = new float[2];
        if (a > 0 || b > 0) {
            float d = height(level, fluid, diagonal);
            if (d >= 1)
                return 1;
            weigh(sum, d);
        }
        weigh(sum, own);
        weigh(sum, a);
        weigh(sum, b);
        return sum[0] / sum[1];
    }

    private static void weigh(float[] sum, float height) {
        if (height >= .8f) {
            sum[0] += height * 10;
            sum[1] += 10;
        } else if (height >= 0) {
            sum[0] += height;
            sum[1] += 1;
        }
    }

    // Whether the next block covers this face
    private static boolean occludes(BlockAndTintGetter level, BlockPos next, BlockState nextState, Direction side,
                                    float height) {
        if (!nextState.canOcclude())
            return false;
        VoxelShape face = Shapes.box(0, 0, 0, 1, height, 1);
        return Shapes.blockOccudes(face, nextState.getOcclusionShape(level, next), side);
    }

    // Light level for a block
    private static int light(BlockAndTintGetter level, BlockPos pos) {
        int here = LevelRenderer.getLightColor(level, pos);
        int up = LevelRenderer.getLightColor(level, pos.above());
        int block = Math.max(here & 255, up & 255);
        int sky = Math.max(here >> 16 & 255, up >> 16 & 255);
        return block | sky << 16;
    }

    private static void quad(VertexConsumer buffer, TextureAtlasSprite sprite, float shade, int light,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3) {
        vertex(buffer, sprite, shade, light, x0, y0, z0, u0, v0);
        vertex(buffer, sprite, shade, light, x1, y1, z1, u1, v1);
        vertex(buffer, sprite, shade, light, x2, y2, z2, u2, v2);
        vertex(buffer, sprite, shade, light, x3, y3, z3, u3, v3);
    }

    private static void vertex(VertexConsumer buffer, TextureAtlasSprite sprite, float shade, int light, float x,
                               float y, float z, float u, float v) {
        buffer.addVertex(x, y, z)
                .setColor(shade, shade, shade, 1)
                .setUv(sprite.getU(u), sprite.getV(v))
                .setLight(light)
                .setNormal(0, 1, 0);
    }
}
