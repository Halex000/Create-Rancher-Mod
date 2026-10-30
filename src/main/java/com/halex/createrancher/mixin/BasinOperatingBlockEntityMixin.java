package com.halex.createrancher.mixin;

import java.util.Comparator;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;

// When two basin recipes tie, picks the one with more fluids
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public abstract class BasinOperatingBlockEntityMixin {

    @ModifyReturnValue(method = "getMatchingRecipes", at = @At("RETURN"))
    private List<Recipe<?>> createrancher$moreFluidsFirst(List<Recipe<?>> recipes) {
        recipes.sort(Comparator.comparingInt((Recipe<?> recipe) -> -recipe.getIngredients()
                        .size())
                .thenComparingInt(recipe -> -createrancher$fluids(recipe)));
        return recipes;
    }

    @Unique
    private static int createrancher$fluids(Recipe<?> recipe) {
        return recipe instanceof ProcessingRecipe<?, ?> processing ? processing.getFluidIngredients()
                .size() : 0;
    }
}
