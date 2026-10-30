package com.halex.createrancher.content.biofuel;

import org.joml.Vector3f;

import com.halex.createrancher.content.fluid.TintedWaterFluidType;

import net.minecraft.resources.ResourceLocation;

// Biofuel
public class BiofuelFluidType extends TintedWaterFluidType {

    private static final int TINT = 0xFFB53216;
    private static final Vector3f FOG = new Vector3f(.23f, .16f, .09f);

    public BiofuelFluidType(Properties properties, ResourceLocation still, ResourceLocation flowing) {
        super(properties, still, flowing, TINT, FOG, 6);
    }
}
