package com.halex.createrancher.content.stew;

import com.halex.createrancher.index.ItemRegistry;
import com.halex.createrancher.index.RecipeRegistry;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

// Crafting recipe for a Can of Suspicious Stew
public class CanOfSuspiciousStewRecipe extends CustomRecipe {

    public CanOfSuspiciousStewRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean brown = false, red = false, flower = false, can = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            if (stack.is(Items.BROWN_MUSHROOM) && !brown)
                brown = true;
            else if (stack.is(Items.RED_MUSHROOM) && !red)
                red = true;
            else if (stack.is(ItemTags.SMALL_FLOWERS) && !flower)
                flower = true;
            else if (stack.is(ItemRegistry.EMPTY_CAN.get()) && !can)
                can = true;
            else
                return false;
        }
        return brown && red && flower && can;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack stew = ItemRegistry.CAN_OF_SUSPICIOUS_STEW.asStack();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            SuspiciousEffectHolder holder = SuspiciousEffectHolder.tryGet(stack.getItem());
            if (holder != null) {
                stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, holder.getSuspiciousEffects());
                break;
            }
        }
        return stew;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.CAN_OF_SUSPICIOUS_STEW.get();
    }
}
