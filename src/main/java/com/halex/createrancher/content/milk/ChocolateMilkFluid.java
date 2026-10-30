package com.halex.createrancher.content.milk;

import com.halex.createrancher.index.ItemRegistry;
import com.simibubi.create.content.fluids.VirtualFluid;

import net.minecraft.world.item.Item;

// Chocolate milk
public class ChocolateMilkFluid extends VirtualFluid {

    public static ChocolateMilkFluid createSource(Properties properties) {
        return new ChocolateMilkFluid(properties, true);
    }

    public static ChocolateMilkFluid createFlowing(Properties properties) {
        return new ChocolateMilkFluid(properties, false);
    }

    public ChocolateMilkFluid(Properties properties, boolean source) {
        super(properties, source);
    }

    @Override
    public Item getBucket() {
        return ItemRegistry.CHOCOLATE_MILK_BUCKET.get();
    }
}
