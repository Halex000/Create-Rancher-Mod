package com.halex.createrancher.mixin.jei;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.FluidRegistry;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.compat.jei.category.BasinCategory;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// Adds the Liquid Blaze Burner and biofuel to JEI's heated basin recipes
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BasinCategory.class, remap = false)
public abstract class BasinCategoryHeatMixin {

    @Unique
    private static final String SET_RECIPE = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;"
            + "Lcom/simibubi/create/content/processing/basin/BasinRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V";
    @Unique
    private static final String DRAW = "draw(Lcom/simibubi/create/content/processing/basin/BasinRecipe;"
            + "Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/client/gui/GuiGraphics;DD)V";
    @Unique
    private static final int TALLER = 8;
    @Unique
    private static final int RAISE = 10;
    @Unique
    private static final int LEFT_X = 134;
    @Unique
    private static final int RIGHT_X = 153;
    @Unique
    private static final int CREATE_SLOT_Y = 81;
    @Unique
    private static final int TOP_Y = CREATE_SLOT_Y - RAISE;
    @Unique
    private static final int ROW = 18;
    @Unique
    private static final int BAR_HEIGHT = 19;
    @Unique
    private static final int BAR_TOP = 10;
    @Unique
    private static final int BAR_BOTTOM = 9;
    @Unique
    private static final int EDGE = 2;
    @Unique
    private static final int GAP_X = 76;
    @Unique
    private static final int GAP_WIDTH = 53;
    @Unique
    private static final int GAP_FILL_FROM = 20;
    @Unique
    private static final int TOP_GAP_FROM = 85;
    @Unique
    private static final int TOP_GAP_TO = 120;
    @Unique
    private static final int ITEMS_Y = 51;
    @Unique
    private static final int ITEMS_RAISE = 3;

    @Shadow
    @Final
    private boolean needsHeating;

    // Recipe height
    public int getHeight() {
        int height = ((CreateRecipeCategory<?>) (Object) this).getBackground()
                .getHeight();
        return needsHeating ? height + TALLER : height;
    }

    // Moves Create's heaters up
    @ModifyExpressionValue(method = SET_RECIPE, at = @At(value = "CONSTANT", args = "intValue=" + CREATE_SLOT_Y))
    private int createrancher$raiseCreatesHeaters(int y) {
        return TOP_Y;
    }

    // Moves the items up
    @ModifyExpressionValue(method = SET_RECIPE, at = @At(value = "CONSTANT", args = "intValue=" + ITEMS_Y))
    private int createrancher$raiseTheItems(int y) {
        return y - ITEMS_RAISE;
    }

    @Inject(method = SET_RECIPE, at = @At("TAIL"))
    private void createrancher$addTheLiquidBurner(IRecipeLayoutBuilder builder, BasinRecipe recipe,
                                                  IFocusGroup focuses, CallbackInfo ci) {
        if (recipe.getRequiredHeat()
                .testBlazeBurner(HeatLevel.NONE))
            return;
        boolean superheated = !recipe.getRequiredHeat()
                .testBlazeBurner(HeatLevel.KINDLED);
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, LEFT_X, TOP_Y + ROW)
                .addItemStack(BlockRegistry.LIQUID_BLAZE_BURNER.asStack());
        if (superheated)
            builder.addSlot(RecipeIngredientRole.CATALYST, RIGHT_X, TOP_Y + ROW)
                    .addItemStack(new ItemStack(FluidRegistry.BIOFUEL.get()
                            .getBucket()));
    }

    @WrapOperation(method = DRAW, at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/gui/"
            + "AllGuiTextures;render(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void createrancher$drawATallerBar(AllGuiTextures texture, GuiGraphics graphics, int x, int y,
                                              Operation<Void> original) {
        if (texture == AllGuiTextures.JEI_HEAT_BAR)
            createrancher$drawBar(texture, graphics, x, y - RAISE, BAR_HEIGHT + ROW);
        else if (texture == AllGuiTextures.JEI_NO_HEAT_BAR)
            createrancher$drawBar(texture, graphics, x, y, BAR_HEIGHT + ROW - RAISE);
        else if (texture == AllGuiTextures.JEI_DOWN_ARROW)
            original.call(texture, graphics, x, y - ITEMS_RAISE);
        else
            original.call(texture, graphics, x, y);
    }

    // Moves the heat text up
    @WrapOperation(method = DRAW, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;"
            + "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"))
    private int createrancher$raiseTheHeatText(GuiGraphics graphics, Font font, Component text, int x, int y,
                                               int colour, boolean shadow, Operation<Integer> original,
                                               @Local(argsOnly = true) BasinRecipe recipe) {
        int raise = recipe.getRequiredHeat() == HeatCondition.NONE ? 0 : RAISE;
        return original.call(graphics, font, text, x, y - raise, colour, shadow);
    }

    // Draws the heat bar
    @Unique
    private static void createrancher$drawBar(AllGuiTextures texture, GuiGraphics graphics, int x, int y, int height) {
        int u = texture.getStartX();
        int v = texture.getStartY();
        int width = texture.getWidth();
        if (texture == AllGuiTextures.JEI_HEAT_BAR) {
            graphics.blit(texture.location, x, y, u, v, TOP_GAP_FROM, EDGE);
            graphics.blit(texture.location, x + TOP_GAP_TO, y, u + TOP_GAP_TO, v, width - TOP_GAP_TO, EDGE);
            graphics.blit(texture.location, x, y + EDGE, u, v + EDGE, width, BAR_TOP - EDGE);
        } else
            graphics.blit(texture.location, x, y, u, v, width, BAR_TOP);
        for (int row = BAR_TOP; row < height - BAR_BOTTOM; row++)
            graphics.blit(texture.location, x, y + row, u, v + BAR_TOP + (row - BAR_TOP) % 2, width, 1);
        graphics.blit(texture.location, x, y + height - BAR_BOTTOM, u, v + BAR_HEIGHT - BAR_BOTTOM, width,
                BAR_BOTTOM);
        if (texture == AllGuiTextures.JEI_HEAT_BAR)
            graphics.blit(texture.location, x + GAP_X, y + height - EDGE, u + GAP_FILL_FROM, v + BAR_HEIGHT - EDGE,
                    GAP_WIDTH, EDGE);
    }
}
