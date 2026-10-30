package com.halex.createrancher.content.joist;

import javax.annotation.Nullable;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

// A joist's item. Burns in a furnace like its planks.
public class JoistItem extends BlockItem {

    public JoistItem(JoistBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return new ItemStack(((JoistBlock) getBlock()).getPlanks()).getBurnTime(recipeType);
    }
}
