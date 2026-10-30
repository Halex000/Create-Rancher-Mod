package com.halex.createrancher.content.stew;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.compat.DelightfulCreatorsCompat;
import com.halex.createrancher.compat.VersionCompat;
import com.halex.createrancher.index.FluidRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.content.fluids.transfer.EmptyingRecipe;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.fluids.FluidStack;

// Adds the stew recipes worked out at load
@EventBusSubscriber(modid = CreateRancher.MOD_ID)
public final class StewRecipes {

    private static final int SERVING = 250;
    private static final String FLUID = "createrancher:suspicious_stew";
    @Nullable
    private static RecipeManager loading;

    private StewRecipes() {
    }

    // The name used in a stew recipe's id for something from any mod
    public static String name(ResourceLocation id) {
        String path = id.getPath()
                .replace('/', '_');
        return id.getNamespace()
                .equals("minecraft") ? path : id.getNamespace() + "_" + path;
    }

    // The mixing recipe that makes suspicious stew from this flower
    public static ResourceLocation mixingId(Item flower) {
        return CreateRancher.asResource("mixing/suspicious_stew_from_" + name(BuiltInRegistries.ITEM.getKey(flower)));
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        loading = event.getServerResources()
                .getRecipeManager();
    }

    @SubscribeEvent
    public static void onTags(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD || loading == null)
            return;
        RecipeManager recipes = loading;
        loading = null;
        try {
            add(recipes, event.getRegistryAccess());
        } catch (RuntimeException e) {
            CreateRancher.LOGGER.error("Couldn't add stew recipes", e);
        }
    }

    // A suspicious stew a crafting recipe makes, and what goes in besides the bowl
    private record Stew(Item flower, SuspiciousStewEffects effects, List<Ingredient> ingredients) {
    }

    // Swaps other mods' copies of the mod's stews over to its own and adds missing stew recipes
    private static void add(RecipeManager recipes, HolderLookup.Provider registries) {
        List<RecipeHolder<?>> loaded = new ArrayList<>(recipes.getRecipes());
        List<RecipeHolder<?>> swapped = DelightfulCreatorsCompat.apply(loaded, registries);
        List<RecipeHolder<?>> added = generate(swapped, registries);
        if (added.isEmpty() && swapped == loaded)
            return;
        List<RecipeHolder<?>> all = new ArrayList<>(swapped);
        all.addAll(added);
        recipes.replaceRecipes(all);
        CreateRancher.LOGGER.info("Added {} stew recipes", added.size());
    }

    // The stew recipes missing from these: suspicious stews from any mod and item drain recipes for bowls
    public static List<RecipeHolder<?>> generate(Collection<RecipeHolder<?>> existing,
                                                 HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        Set<ResourceLocation> ids = new HashSet<>();
        existing.forEach(holder -> ids.add(holder.id()));
        List<RecipeHolder<?>> added = new ArrayList<>();

        Map<Item, Stew> stews = new LinkedHashMap<>();
        for (RecipeHolder<?> holder : existing) {
            if (holder.value()
                    .getType() != RecipeType.CRAFTING)
                continue;
            Stew stew = fromCraftingRecipe(holder.value(), registries);
            if (stew != null)
                stews.putIfAbsent(stew.flower(), stew);
        }
        Ingredient brown = Ingredient.of(Items.BROWN_MUSHROOM);
        Ingredient red = Ingredient.of(Items.RED_MUSHROOM);
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.SMALL_FLOWERS)) {
            SuspiciousEffectHolder effects = SuspiciousEffectHolder.tryGet(holder.value());
            if (effects != null)
                stews.putIfAbsent(holder.value(), new Stew(holder.value(), effects.getSuspiciousEffects(), List.of(brown,
                        red, Ingredient.of(holder.value()))));
        }

        Set<SuspiciousStewEffects> allEffects = new LinkedHashSet<>(SuspiciousStews.allEffects());
        for (Stew stew : stews.values()) {
            allEffects.add(stew.effects());
            ResourceLocation id = mixingId(stew.flower());
            if (ids.contains(id))
                continue;
            JsonObject json = new JsonObject();
            json.addProperty("type", "create:mixing");
            JsonArray ingredients = new JsonArray();
            for (Ingredient ingredient : stew.ingredients())
                ingredients.add(Ingredient.CODEC_NONEMPTY.encodeStart(ops, ingredient)
                        .getOrThrow());
            json.add("ingredients", ingredients);
            JsonObject result = new JsonObject();
            result.addProperty("id", FLUID);
            result.addProperty("amount", SERVING);
            result.add("components", components(ops, stew.effects()));
            JsonArray results = new JsonArray();
            results.add(result);
            json.add("results", results);
            parse(id, json, ops, added, ids);
        }

        List<EmptyingRecipe> emptying = new ArrayList<>();
        for (RecipeHolder<?> holder : existing)
            if (holder.value() instanceof EmptyingRecipe recipe)
                emptying.add(recipe);
        Set<SuspiciousStewEffects> inBowls = new HashSet<>();
        Set<SuspiciousStewEffects> inCans = new HashSet<>();
        for (RecipeHolder<?> holder : existing) {
            if (!(holder.value() instanceof FillingRecipe filling))
                continue;
            ItemStack made = filling.getResultItem(registries);
            SuspiciousStewEffects effects = made.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
            if (effects == null)
                continue;
            if (made.is(Items.SUSPICIOUS_STEW))
                inBowls.add(effects);
            else if (made.is(ItemRegistry.CAN_OF_SUSPICIOUS_STEW.get()))
                inCans.add(effects);
        }
        for (SuspiciousStewEffects effects : allEffects) {
            if (effects.effects()
                    .isEmpty())
                continue;
            if (!inBowls.contains(effects))
                parse(fillingId("suspicious_stew_", effects, ids), filling(ops, "minecraft:bowl",
                        "minecraft:suspicious_stew", effects), ops, added, ids);
            if (!inCans.contains(effects))
                parse(fillingId("can_of_suspicious_stew_", effects, ids), filling(ops, "createrancher:empty_can",
                        "createrancher:can_of_suspicious_stew", effects), ops, added, ids);
            FluidStack stew = new FluidStack(FluidRegistry.SUSPICIOUS_STEW.get()
                    .getSource(), SERVING);
            stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
            if (!poursOut(emptying, withEffects(ItemRegistry.CAN_OF_SUSPICIOUS_STEW.asStack(), effects)))
                parse(effectsId("emptying/can_of_suspicious_stew_", effects, ids), emptying(ops, withComponents(ops,
                        "createrancher:can_of_suspicious_stew", effects), "createrancher:empty_can", stew), ops, added,
                        ids);
            if (!poursOut(emptying, withEffects(new ItemStack(Items.SUSPICIOUS_STEW), effects)))
                parse(effectsId("emptying/suspicious_stew_", effects, ids), emptying(ops, withComponents(ops,
                        "minecraft:suspicious_stew", effects), "minecraft:bowl", stew), ops, added, ids);
        }

        for (RecipeHolder<?> holder : existing) {
            if (!holder.id()
                    .getNamespace()
                    .equals(CreateRancher.MOD_ID) || !(holder.value() instanceof FillingRecipe filling) || filling
                    .getIngredients()
                    .isEmpty() || !filling.getIngredients()
                    .get(0)
                    .test(new ItemStack(Items.BOWL)))
                continue;
            ItemStack made = filling.getResultItem(registries);
            List<FluidStack> fluids = VersionCompat.requiredFluids(filling);
            if (made.isEmpty() || made.has(DataComponents.SUSPICIOUS_STEW_EFFECTS) || fluids.isEmpty() || poursOut(
                    emptying, made))
                continue;
            ResourceLocation item = BuiltInRegistries.ITEM.getKey(made.getItem());
            JsonObject ingredient = new JsonObject();
            ingredient.addProperty("item", item.toString());
            parse(CreateRancher.asResource("emptying/" + name(item)), emptying(ops, ingredient, "minecraft:bowl",
                    fluids.get(0)), ops, added, ids);
        }

        return added;
    }

    // Whether an item drain recipe already pours this out
    private static boolean poursOut(List<EmptyingRecipe> emptying, ItemStack stack) {
        for (EmptyingRecipe recipe : emptying)
            for (Ingredient ingredient : recipe.getIngredients())
                if (ingredient.test(stack))
                    return true;
        return false;
    }

    private static ItemStack withEffects(ItemStack stack, SuspiciousStewEffects effects) {
        stack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
        return stack;
    }

    // An ingredient matching the item with exactly these stew effects
    private static JsonObject withComponents(RegistryOps<JsonElement> ops, String item, SuspiciousStewEffects effects) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("type", "neoforge:components");
        ingredient.addProperty("items", item);
        ingredient.add("components", components(ops, effects));
        return ingredient;
    }

    // The flower, effects and other ingredients of a crafting recipe for suspicious stew with a bowl in it
    @Nullable
    private static Stew fromCraftingRecipe(Recipe<?> recipe, HolderLookup.Provider registries) {
        ItemStack made;
        try {
            made = recipe.getResultItem(registries);
        } catch (RuntimeException e) {
            return null;
        }
        SuspiciousStewEffects effects = made.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (!made.is(Items.SUSPICIOUS_STEW) || effects == null || effects.effects()
                .isEmpty())
            return null;
        List<Ingredient> others = new ArrayList<>();
        boolean bowl = false;
        Item flower = null;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty())
                continue;
            if (!bowl && ingredient.test(new ItemStack(Items.BOWL))) {
                bowl = true;
                continue;
            }
            others.add(ingredient);
            ItemStack[] items = ingredient.getItems();
            if (flower == null && items.length > 0 && !ingredient.test(new ItemStack(Items.BROWN_MUSHROOM))
                    && !ingredient.test(new ItemStack(Items.RED_MUSHROOM)))
                flower = items[0].getItem();
        }
        if (!bowl || flower == null || others.isEmpty())
            return null;
        return new Stew(flower, effects, others);
    }

    private static JsonObject components(RegistryOps<JsonElement> ops, SuspiciousStewEffects effects) {
        JsonObject components = new JsonObject();
        components.add("minecraft:suspicious_stew_effects", SuspiciousStewEffects.CODEC.encodeStart(ops, effects)
                .getOrThrow());
        return components;
    }

    // An item drain recipe pouring the item out into its container and a fluid
    private static JsonObject emptying(RegistryOps<JsonElement> ops, JsonObject ingredient, String container,
                                       FluidStack fluid) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:emptying");
        JsonArray ingredients = new JsonArray();
        ingredients.add(ingredient);
        json.add("ingredients", ingredients);
        JsonObject empty = new JsonObject();
        empty.addProperty("id", container);
        JsonArray results = new JsonArray();
        results.add(empty);
        results.add(FluidStack.CODEC.encodeStart(ops, fluid)
                .getOrThrow());
        json.add("results", results);
        return json;
    }

    private static JsonObject filling(RegistryOps<JsonElement> ops, String container, String made,
                                      SuspiciousStewEffects effects) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "create:filling");
        JsonArray ingredients = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("item", container);
        ingredients.add(item);
        JsonObject fluid = new JsonObject();
        fluid.addProperty("type", "fluid_stack");
        fluid.addProperty("fluid", FLUID);
        fluid.addProperty("amount", SERVING);
        fluid.add("components", components(ops, effects));
        ingredients.add(fluid);
        json.add("ingredients", ingredients);
        JsonObject result = new JsonObject();
        result.addProperty("id", made);
        result.add("components", components(ops, effects));
        JsonArray results = new JsonArray();
        results.add(result);
        json.add("results", results);
        return json;
    }

    // A free id for a spout recipe named after the stew's effects
    private static ResourceLocation fillingId(String prefix, SuspiciousStewEffects effects, Set<ResourceLocation> ids) {
        return effectsId("filling/" + prefix, effects, ids);
    }

    // A free id for a recipe named after the stew's effects
    private static ResourceLocation effectsId(String prefix, SuspiciousStewEffects effects, Set<ResourceLocation> ids) {
        List<String> parts = new ArrayList<>();
        for (SuspiciousStewEffects.Entry entry : effects.effects())
            parts.add(entry.effect()
                    .unwrapKey()
                    .map(key -> name(key.location()))
                    .orElse("effect"));
        String base = prefix + String.join("_and_", parts);
        ResourceLocation id = CreateRancher.asResource(base);
        for (int i = 2; ids.contains(id); i++)
            id = CreateRancher.asResource(base + "_" + i);
        return id;
    }

    private static void parse(ResourceLocation id, JsonObject json, RegistryOps<JsonElement> ops,
                              List<RecipeHolder<?>> added, Set<ResourceLocation> ids) {
        Recipe.CODEC.parse(ops, json)
                .resultOrPartial(error -> CreateRancher.LOGGER.warn("Couldn't make stew recipe {}: {}", id, error))
                .ifPresent(recipe -> {
                    added.add(new RecipeHolder<>(id, recipe));
                    ids.add(id);
                });
    }
}
