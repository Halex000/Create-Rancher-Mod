package com.halex.createrancher.content.alfalfa;

import com.halex.createrancher.index.ItemRegistry;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

// The alfalfa crop, which grows like wheat
public class AlfalfaCropBlock extends CropBlock {

    public AlfalfaCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ItemRegistry.ALFALFA_SEEDS.get();
    }
}
