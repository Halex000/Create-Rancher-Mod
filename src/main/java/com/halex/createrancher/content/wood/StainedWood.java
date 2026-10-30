package com.halex.createrancher.content.wood;

import com.halex.createrancher.CreateRancher;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

// Stained wood, made with Tannic Powder
public class StainedWood {

    public static final BlockSetType SET = BlockSetType.register(new BlockSetType(CreateRancher.MOD_ID + ":stained"));
    public static final WoodType TYPE = WoodType.register(new WoodType(CreateRancher.MOD_ID + ":stained", SET));
}
