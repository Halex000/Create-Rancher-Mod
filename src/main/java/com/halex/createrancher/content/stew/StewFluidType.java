package com.halex.createrancher.content.stew;

import java.util.function.Consumer;

import org.joml.Vector3f;

import com.halex.createrancher.content.stew.client.StewFluidRenderer;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;

// A stew as a fluid
public class StewFluidType extends FluidType {

    private final ResourceLocation still;
    private final ResourceLocation flowing;
    private final Vector3f fog;

    public StewFluidType(Properties properties, ResourceLocation still, ResourceLocation flowing, int fogColour) {
        super(properties);
        this.still = still;
        this.flowing = flowing;
        fog = new Vector3f((fogColour >> 16 & 0xFF) / 255f, (fogColour >> 8 & 0xFF) / 255f, (fogColour & 0xFF) / 255f);
    }

    @SuppressWarnings("removal")
    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }

            // Uses StewFluidRenderer for placed stew
            @Override
            public boolean renderFluid(FluidState fluidState, BlockAndTintGetter level, BlockPos pos,
                                       VertexConsumer buffer, BlockState blockState) {
                StewFluidRenderer.render(level, pos, buffer, blockState, fluidState, still, flowing);
                return true;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                           float darkenWorldAmount, Vector3f fluidFogColor) {
                return fog;
            }

            // Fog distance
            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance,
                                        float partialTick, float nearDistance, float farDistance, FogShape shape) {
                RenderSystem.setShaderFogShape(FogShape.CYLINDER);
                RenderSystem.setShaderFogStart(-8);
                RenderSystem.setShaderFogEnd(3);
            }
        });
    }
}
