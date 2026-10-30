package com.halex.createrancher.content.joist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;

// One wood's joist
public final class JoistWood {

    private final JoistWoods.Found found;
    private final JoistBlock block;
    private BlockItem item;

    JoistWood(JoistWoods.Found found, JoistBlock block) {
        this.found = found;
        this.block = block;
    }

    public ResourceLocation id() {
        return found.joist();
    }

    public ResourceLocation planks() {
        return found.planks();
    }

    public ResourceLocation strippedLog() {
        return found.strippedLog();
    }

    // The wood's English name
    public String name() {
        return found.name();
    }

    public JoistBlock block() {
        return block;
    }

    public BlockItem item() {
        return item;
    }

    void setItem(BlockItem item) {
        this.item = item;
    }
}
