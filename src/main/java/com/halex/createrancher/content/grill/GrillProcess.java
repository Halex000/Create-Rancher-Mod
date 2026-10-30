package com.halex.createrancher.content.grill;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.halex.createrancher.CommonConfig;
import com.halex.createrancher.compat.VersionCompat;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.Level;

// What a grill does to its items
public enum GrillProcess {

    SMOKING {
        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            return smokingRecipe(stack, level).isPresent();
        }

        @Nullable
        @Override
        public List<ItemStack> process(ItemStack stack, Level level) {
            return smokingRecipe(stack, level).map(recipe -> VersionCompat.applyRecipe(level, stack, recipe.value(),
                    false))
                    .orElse(null);
        }

        @Override
        public int getTime(ItemStack stack, Level level) {
            int time = smokingRecipe(stack, level).map(recipe -> recipe.value()
                            .getCookingTime())
                    .orElse(100);
            return Math.max(1, (int) Math.round(time * CommonConfig.GRILL_SMOKING_TIME_MULTIPLIER.get()));
        }
    },

    HAUNTING {
        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            return AllRecipeTypes.HAUNTING.find(new SingleRecipeInput(stack), level)
                    .isPresent();
        }

        @Nullable
        @Override
        public List<ItemStack> process(ItemStack stack, Level level) {
            return AllRecipeTypes.HAUNTING.find(new SingleRecipeInput(stack), level)
                    .map(recipe -> VersionCompat.applyRecipe(level, stack, recipe.value(), true))
                    .orElse(null);
        }

        @Override
        public int getTime(ItemStack stack, Level level) {
            return CommonConfig.GRILL_HAUNTING_TICKS.get();
        }
    },

    BLASTING {
        // Whether a grill can process this item
        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            SingleRecipeInput input = new SingleRecipeInput(stack);
            Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> smelting = level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, input, level)
                    .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
            if (smelting.isEmpty())
                smelting = level.getRecipeManager()
                        .getRecipeFor(RecipeType.BLASTING, input, level);
            if (smelting.isEmpty())
                return false;
            Optional<RecipeHolder<SmokingRecipe>> smoking = level.getRecipeManager()
                    .getRecipeFor(RecipeType.SMOKING, input, level)
                    .filter(AllRecipeTypes.CAN_BE_AUTOMATED);
            return smoking.isEmpty() || !ItemStack.isSameItem(smoking.get()
                    .value()
                    .getResultItem(level.registryAccess()), smelting.get()
                    .value()
                    .getResultItem(level.registryAccess()));
        }

        @Nullable
        @Override
        public List<ItemStack> process(ItemStack stack, Level level) {
            return AllFanProcessingTypes.BLASTING.process(stack, level);
        }

        @Override
        public int getTime(ItemStack stack, Level level) {
            return CommonConfig.GRILL_BLASTING_TICKS.get();
        }
    },

    WASHING {
        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            return AllFanProcessingTypes.SPLASHING.canProcess(stack, level);
        }

        @Nullable
        @Override
        public List<ItemStack> process(ItemStack stack, Level level) {
            return AllFanProcessingTypes.SPLASHING.process(stack, level);
        }

        @Override
        public int getTime(ItemStack stack, Level level) {
            return CommonConfig.GRILL_WASHING_TICKS.get();
        }
    };

    private static Optional<RecipeHolder<SmokingRecipe>> smokingRecipe(ItemStack stack, Level level) {
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(stack), level);
    }

    public abstract boolean canProcess(ItemStack stack, Level level);

    // What the item turns into, or null
    @Nullable
    public abstract List<ItemStack> process(ItemStack stack, Level level);

    // How long it takes, in ticks
    public abstract int getTime(ItemStack stack, Level level);
}
