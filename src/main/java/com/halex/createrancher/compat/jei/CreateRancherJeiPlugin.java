package com.halex.createrancher.compat.jei;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.burner.LiquidBlazeBurnerFuels;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

// Adds the mod's recipes, tabs and items to JEI
@JeiPlugin
public class CreateRancherJeiPlugin implements IModPlugin {

    private final List<CreateRecipeCategory<?>> grillCategories = new ArrayList<>();
    @Nullable
    private static IJeiRuntime runtime;
    private static final Set<LiquidBlazeBurnerFuels.Fuel> shownFuels = new HashSet<>();

    @Override
    public ResourceLocation getPluginUid() {
        return CreateRancher.asResource("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ItemRegistry.CAN_OF_SUSPICIOUS_STEW.get(), new ByEffects());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        grillCategories.clear();
        grillCategories.addAll(GrillCategories.create());
        registration.addRecipeCategories(grillCategories.toArray(IRecipeCategory[]::new));
        registration.addRecipeCategories(new LiquidBlazeBurnerFuelCategory(registration.getJeiHelpers()
                .getGuiHelper()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        fuelsReceived();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    // Adds fuels JEI doesn't have yet
    public static void fuelsReceived() {
        if (runtime == null)
            return;
        List<LiquidBlazeBurnerFuels.Fuel> missing = LiquidBlazeBurnerFuels.available()
                .stream()
                .filter(fuel -> !shownFuels.contains(fuel))
                .toList();
        if (missing.isEmpty())
            return;
        shownFuels.addAll(missing);
        runtime.getRecipeManager()
                .addRecipes(LiquidBlazeBurnerFuelCategory.TYPE, missing);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        grillCategories.forEach(category -> category.registerCatalysts(registration));
        registration.addRecipeCatalyst(BlockRegistry.LIQUID_BLAZE_BURNER.asStack(), LiquidBlazeBurnerFuelCategory.TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        grillCategories.forEach(category -> category.registerRecipes(registration));
        shownFuels.clear();
        shownFuels.addAll(LiquidBlazeBurnerFuels.available());
        registration.addRecipes(LiquidBlazeBurnerFuelCategory.TYPE, LiquidBlazeBurnerFuels.available());
        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
        Ingredient can = Ingredient.of(ItemRegistry.EMPTY_CAN.get());
        Ingredient brown = Ingredient.of(Items.BROWN_MUSHROOM);
        Ingredient red = Ingredient.of(Items.RED_MUSHROOM);
        for (Holder<Item> flower : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.SMALL_FLOWERS)) {
            SuspiciousEffectHolder holder = SuspiciousEffectHolder.tryGet(flower.value());
            if (holder == null)
                continue;
            ItemStack stew = ItemRegistry.CAN_OF_SUSPICIOUS_STEW.asStack();
            stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, holder.getSuspiciousEffects());
            NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY, can, brown, red,
                    Ingredient.of(flower.value()));
            ResourceLocation id = CreateRancher.asResource("jei/can_of_suspicious_stew/" + BuiltInRegistries.ITEM
                    .getKey(flower.value())
                    .getPath());
            recipes.add(new RecipeHolder<>(id, new ShapelessRecipe("can_of_suspicious_stew", CraftingBookCategory.MISC,
                    stew, ingredients)));
        }
        registration.addRecipes(RecipeTypes.CRAFTING, recipes);
    }

    // Tells items apart by their suspicious stew effects
    private static class ByEffects implements ISubtypeInterpreter<ItemStack> {
        @Override
        public Object getSubtypeData(ItemStack stack, UidContext context) {
            return stack.getOrDefault(DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.EMPTY);
        }

        @Override
        public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
            return String.valueOf(getSubtypeData(stack, context));
        }
    }
}
