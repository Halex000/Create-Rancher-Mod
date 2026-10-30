package com.halex.createrancher.content.grate;

import javax.annotation.Nullable;

import com.simibubi.create.AllItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// What a belt grate is plated with
public enum GrateMetal {
    PLAIN("plain"),
    COPPER("copper"),
    IRON("iron"),
    BRASS("brass");

    private final String name;

    GrateMetal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // The nugget that plates a grate with this metal, or null for a plain grate
    @Nullable
    public Item getNugget() {
        return switch (this) {
            case PLAIN -> null;
            case COPPER -> AllItems.COPPER_NUGGET.get();
            case IRON -> Items.IRON_NUGGET;
            case BRASS -> AllItems.BRASS_NUGGET.get();
        };
    }

    // The metal a nugget plates a grate with, or null if it isn't one
    @Nullable
    public static GrateMetal byNugget(ItemStack stack) {
        for (GrateMetal metal : values())
            if (metal.getNugget() != null && stack.is(metal.getNugget()))
                return metal;
        return null;
    }

    public static GrateMetal byName(String name) {
        for (GrateMetal metal : values())
            if (metal.name.equals(name))
                return metal;
        return PLAIN;
    }
}
