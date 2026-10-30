package com.halex.createrancher.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import com.halex.createrancher.CreateRancher;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

// Calls the Create and NeoForge methods that changed between the versions the mod runs on
public final class VersionCompat {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    @Nullable
    private static final MethodHandle APPLY_WITH_REMAINDER = find(() -> LOOKUP.findStatic(RecipeApplier.class,
            "applyRecipeOn", MethodType.methodType(List.class, Level.class, ItemStack.class, Recipe.class,
                    boolean.class)));
    @Nullable
    private static final MethodHandle APPLY = find(() -> LOOKUP.findStatic(RecipeApplier.class, "applyRecipeOn",
            MethodType.methodType(List.class, Level.class, ItemStack.class, Recipe.class)));
    @Nullable
    private static final MethodHandle STACK_FITS = find(() -> LOOKUP.findVirtual(ItemStack.class,
            "canFitInsideContainerItems", MethodType.methodType(boolean.class)));

    private VersionCompat() {
    }

    private interface Finder {
        MethodHandle find() throws ReflectiveOperationException;
    }

    @Nullable
    private static MethodHandle find(Finder finder) {
        try {
            return finder.find();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    // Runs a recipe on a stack the way Create's fans do
    @SuppressWarnings("unchecked")
    public static List<ItemStack> applyRecipe(Level level, ItemStack stack, Recipe<?> recipe,
                                              boolean keepRemainder) {
        try {
            if (APPLY_WITH_REMAINDER != null)
                return (List<ItemStack>) APPLY_WITH_REMAINDER.invoke(level, stack, recipe, keepRemainder);
            if (APPLY != null)
                return (List<ItemStack>) APPLY.invoke(level, stack, recipe);
        } catch (Throwable e) {
            CreateRancher.LOGGER.error("Couldn't apply {} with Create", recipe, e);
        }
        return List.of();
    }

    // The fluids a spout recipe can use
    public static List<FluidStack> requiredFluids(FillingRecipe recipe) {
        try {
            return fluidsOf(FillingRecipe.class.getMethod("getRequiredFluid")
                    .invoke(recipe));
        } catch (ReflectiveOperationException | RuntimeException e) {
            CreateRancher.LOGGER.debug("Couldn't read the fluid of {}", recipe, e);
        }
        return List.of();
    }

    // The fluids each of a processing recipe's fluid ingredients can be, with their amounts
    public static List<List<FluidStack>> fluidIngredients(ProcessingRecipe<?, ?> recipe) {
        List<List<FluidStack>> all = new ArrayList<>();
        for (Object ingredient : (List<?>) recipe.getFluidIngredients())
            all.add(fluidsOf(ingredient));
        return all;
    }

    @SuppressWarnings("unchecked")
    private static List<FluidStack> fluidsOf(@Nullable Object ingredient) {
        if (ingredient instanceof SizedFluidIngredient sized)
            return Arrays.asList(sized.getFluids());
        if (ingredient == null)
            return List.of();
        try {
            return (List<FluidStack>) ingredient.getClass()
                    .getMethod("getMatchingFluidStacks")
                    .invoke(ingredient);
        } catch (ReflectiveOperationException | RuntimeException e) {
            CreateRancher.LOGGER.debug("Couldn't read the fluids of {}", ingredient, e);
            return List.of();
        }
    }

    // Whether an item can go inside items that hold other items
    public static boolean fitsInContainerItems(ItemStack stack) {
        if (STACK_FITS != null)
            try {
                return (boolean) STACK_FITS.invoke(stack);
            } catch (Throwable ignored) {
            }
        return stack.getItem()
                .canFitInsideContainerItems();
    }
}
