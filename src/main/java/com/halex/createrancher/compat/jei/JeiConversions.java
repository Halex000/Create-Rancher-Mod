package com.halex.createrancher.compat.jei;

import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory;

// Things shown in JEI that turn into other things without a recipe
public class JeiConversions {

    public static void register() {
        MysteriousItemConversionCategory.RECIPES.add(ConversionRecipe.create(ItemRegistry.ALFALFA_PELLETS.asStack(),
                BlockRegistry.DUNG.asStack()));
    }
}
