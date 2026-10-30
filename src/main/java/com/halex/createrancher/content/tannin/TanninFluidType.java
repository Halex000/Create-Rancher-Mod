package com.halex.createrancher.content.tannin;

import org.joml.Vector3f;

import com.halex.createrancher.content.fluid.TintedWaterFluidType;

import net.minecraft.resources.ResourceLocation;

// Tannin
public class TanninFluidType extends TintedWaterFluidType {

    private static final int TINT = 0xD8703420;
    private static final Vector3f FOG = new Vector3f(.36f, .16f, .09f);

    public TanninFluidType(Properties properties, ResourceLocation still, ResourceLocation flowing) {
        super(properties, still, flowing, TINT, FOG, 12);
    }
}
