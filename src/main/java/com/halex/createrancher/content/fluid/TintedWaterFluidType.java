package com.halex.createrancher.content.fluid;

import java.util.function.Consumer;

import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

// A fluid that looks like water in its own colour
public class TintedWaterFluidType extends FluidType {

    private final ResourceLocation still;
    private final ResourceLocation flowing;
    private final int tint;
    private final Vector3f fog;
    private final float fogEnd;

    public TintedWaterFluidType(Properties properties, ResourceLocation still, ResourceLocation flowing, int tint,
                                Vector3f fog, float fogEnd) {
        super(properties);
        this.still = still;
        this.flowing = flowing;
        this.tint = tint;
        this.fog = fog;
        this.fogEnd = fogEnd;
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

            @Override
            public int getTintColor() {
                return tint;
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return tint;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return tint;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                           float darkenWorldAmount, Vector3f fluidFogColor) {
                return fog;
            }

            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance,
                                        float partialTick, float nearDistance, float farDistance,
                                        com.mojang.blaze3d.shaders.FogShape shape) {
                RenderSystem.setShaderFogStart(-8);
                RenderSystem.setShaderFogEnd(fogEnd);
            }
        });
    }
}
