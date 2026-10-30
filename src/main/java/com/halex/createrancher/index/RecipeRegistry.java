package com.halex.createrancher.index;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.stew.CanOfSuspiciousStewRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RecipeRegistry {

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(
            Registries.RECIPE_SERIALIZER, CreateRancher.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CanOfSuspiciousStewRecipe>>
            CAN_OF_SUSPICIOUS_STEW = SERIALIZERS.register("can_of_suspicious_stew",
            () -> new SimpleCraftingRecipeSerializer<>(CanOfSuspiciousStewRecipe::new));

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
