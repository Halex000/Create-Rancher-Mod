package com.halex.createrancher.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.halex.createrancher.CreateRancher;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

// Has Delightful Creators' stew fluids give way to the mod's own for the stews both mods make
public final class DelightfulCreatorsCompat {

    private static final String MOD = "delightfulcreators";
    private static final List<String> SHARED = List.of("mushroom_stew", "rabbit_stew", "beetroot_soup");

    private DelightfulCreatorsCompat() {
    }

    // The recipes with Delightful Creators' shared stews made by the mod's recipes alone and used as the mod's fluids
    public static List<RecipeHolder<?>> apply(List<RecipeHolder<?>> recipes, HolderLookup.Provider registries) {
        Map<Fluid, Fluid> swaps = new LinkedHashMap<>();
        for (String name : SHARED) {
            ResourceLocation theirs = ResourceLocation.fromNamespaceAndPath(MOD, name);
            ResourceLocation ours = CreateRancher.asResource(name);
            if (BuiltInRegistries.FLUID.containsKey(theirs) && BuiltInRegistries.FLUID.containsKey(ours))
                swaps.put(BuiltInRegistries.FLUID.get(theirs), BuiltInRegistries.FLUID.get(ours));
        }
        return swap(recipes, registries, swaps);
    }

    // The recipes with each fluid swapped for its replacement
    public static List<RecipeHolder<?>> swap(List<RecipeHolder<?>> recipes, HolderLookup.Provider registries,
                                             Map<Fluid, Fluid> swaps) {
        if (swaps.isEmpty())
            return recipes;
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        Set<Fluid> madeByUs = new HashSet<>();
        Set<String> ours = new HashSet<>();
        for (RecipeHolder<?> holder : recipes) {
            if (!holder.id()
                    .getNamespace()
                    .equals(CreateRancher.MOD_ID) || !(holder.value() instanceof ProcessingRecipe<?, ?> recipe))
                continue;
            for (FluidStack made : recipe.getFluidResults())
                madeByUs.add(made.getFluid());
            String key = matchKey(ops, recipe);
            if (key != null)
                ours.add(key);
        }
        Map<Fluid, Double> scales = new HashMap<>();
        for (Map.Entry<Fluid, Fluid> entry : swaps.entrySet())
            scales.put(entry.getKey(), scale(recipes, registries, entry.getKey(), entry.getValue()));

        List<RecipeHolder<?>> result = new ArrayList<>(recipes.size());
        int removed = 0;
        int changed = 0;
        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof ProcessingRecipe<?, ?> recipe) || !uses(recipe, swaps.keySet())) {
                result.add(holder);
                continue;
            }
            if (recipe.getFluidResults()
                    .stream()
                    .anyMatch(made -> swaps.containsKey(made.getFluid()) && madeByUs.contains(swaps.get(made
                            .getFluid())))) {
                removed++;
                continue;
            }
            JsonElement json = encode(ops, recipe);
            if (json == null) {
                result.add(holder);
                continue;
            }
            for (Map.Entry<Fluid, Fluid> entry : swaps.entrySet())
                json = replace(json, BuiltInRegistries.FLUID.getKey(entry.getKey())
                        .toString(), BuiltInRegistries.FLUID.getKey(entry.getValue())
                        .toString(), scales.get(entry.getKey()));
            Recipe<?> swapped = Recipe.CODEC.parse(ops, json)
                    .resultOrPartial(error -> CreateRancher.LOGGER.warn("Couldn't swap the stew in {}: {}", holder
                            .id(), error))
                    .orElse(null);
            if (swapped == null) {
                result.add(holder);
                continue;
            }
            String key = matchKey(ops, swapped);
            if (key != null && ours.contains(key)) {
                removed++;
                continue;
            }
            result.add(new RecipeHolder<>(holder.id(), swapped));
            changed++;
        }
        if (removed + changed > 0)
            CreateRancher.LOGGER.info("Moved {} recipes over to Create Rancher's stews and removed {} it already has",
                    changed, removed);
        return result;
    }

    private static boolean uses(ProcessingRecipe<?, ?> recipe, Set<Fluid> fluids) {
        for (FluidStack made : recipe.getFluidResults())
            if (fluids.contains(made.getFluid()))
                return true;
        for (List<FluidStack> ingredient : VersionCompat.fluidIngredients(recipe))
            for (FluidStack stack : ingredient)
                if (fluids.contains(stack.getFluid()))
                    return true;
        return false;
    }

    // How much of the replacement makes a bowl compared to the fluid it replaces
    private static double scale(List<RecipeHolder<?>> recipes, HolderLookup.Provider registries, Fluid from,
                                Fluid to) {
        Map<Item, Integer> fromServings = new HashMap<>();
        Map<Item, Integer> toServings = new HashMap<>();
        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof FillingRecipe filling) || filling.getIngredients()
                    .isEmpty() || !filling.getIngredients()
                    .get(0)
                    .test(new ItemStack(Items.BOWL)))
                continue;
            List<FluidStack> fluids = VersionCompat.requiredFluids(filling);
            if (fluids.isEmpty())
                continue;
            Item made = filling.getResultItem(registries)
                    .getItem();
            FluidStack fluid = fluids.get(0);
            if (fluid.getFluid() == from)
                fromServings.put(made, fluid.getAmount());
            else if (fluid.getFluid() == to)
                toServings.put(made, fluid.getAmount());
        }
        for (Map.Entry<Item, Integer> entry : fromServings.entrySet())
            if (toServings.containsKey(entry.getKey()) && entry.getValue() > 0)
                return toServings.get(entry.getKey()) / (double) entry.getValue();
        return 1;
    }

    @Nullable
    private static JsonElement encode(RegistryOps<JsonElement> ops, Recipe<?> recipe) {
        return Recipe.CODEC.encodeStart(ops, recipe)
                .result()
                .orElse(null);
    }

    // The json with every mention of one fluid swapped for another
    private static JsonElement replace(JsonElement json, String from, String to, double scale) {
        if (json instanceof JsonArray array) {
            JsonArray copy = new JsonArray();
            for (JsonElement element : array)
                copy.add(isString(element, from) ? new JsonPrimitive(to) : replace(element, from, to, scale));
            return copy;
        }
        if (!(json instanceof JsonObject object))
            return json;
        JsonObject copy = new JsonObject();
        boolean mentioned = false;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (isString(entry.getValue(), from)) {
                copy.addProperty(entry.getKey(), to);
                mentioned = true;
            } else {
                JsonElement value = replace(entry.getValue(), from, to, scale);
                copy.add(entry.getKey(), value);
                if (entry.getValue() instanceof JsonArray array && array.contains(new JsonPrimitive(from)))
                    mentioned = true;
            }
        }
        if (mentioned && copy.get("amount") instanceof JsonPrimitive amount && amount.isNumber())
            copy.addProperty("amount", Math.max(1, (int) Math.round(amount.getAsInt() * scale)));
        return copy;
    }

    private static boolean isString(JsonElement element, String value) {
        return element instanceof JsonPrimitive primitive && primitive.isString() && primitive.getAsString()
                .equals(value);
    }

    // What a recipe takes and makes, ignoring heat, time and ingredient order
    @Nullable
    private static String matchKey(RegistryOps<JsonElement> ops, Recipe<?> recipe) {
        if (!(recipe instanceof ProcessingRecipe<?, ?> processing) || !(encode(ops, recipe) instanceof JsonObject json))
            return null;
        List<String> items = new ArrayList<>();
        for (Ingredient ingredient : processing.getIngredients())
            items.add(Ingredient.CODEC.encodeStart(ops, ingredient)
                    .result()
                    .map(JsonElement::toString)
                    .orElse("?"));
        items.sort(null);
        List<String> fluids = new ArrayList<>();
        for (List<FluidStack> ingredient : VersionCompat.fluidIngredients(processing)) {
            List<String> options = new ArrayList<>();
            for (FluidStack stack : ingredient)
                options.add(BuiltInRegistries.FLUID.getKey(stack.getFluid()) + "x" + stack.getAmount() + stack
                        .getComponentsPatch());
            options.sort(null);
            fluids.add(options.toString());
        }
        fluids.sort(null);
        return json.get("type") + "|" + items + "|" + fluids + "|" + json.get("results");
    }
}
