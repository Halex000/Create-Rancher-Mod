package com.halex.createrancher.compat.jei;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.grill.CauldronGrillBlock;
import com.halex.createrancher.content.grill.GrillProcess;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.ProcessingViaFanCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.kinetics.fan.processing.HauntingRecipe;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.block.state.BlockState;

// JEI tabs for the four kinds of grill
public class GrillCategories {

    private static final int SCALE = 24;
    private static final int GRILL_X = 46;
    private static final int GRILL_Y = 49;
    private static final double BEHIND_FAN = 2;
    private static final int SHADOW_X = 55;
    private static final int SHADOW_Y = 55;

    public static List<CreateRecipeCategory<?>> create() {
        return List.of(
                new CreateRecipeCategory.Builder<>(SmokingRecipe.class)
                        .addTypedRecipes(() -> RecipeType.SMOKING)
                        .removeNonAutomation()
                        .catalystStack(ItemRegistry.ITEM_GRATE::asStack)
                        .doubleItemIcon(ItemRegistry.ITEM_GRATE.get(), Items.CAMPFIRE)
                        .emptyBackground(177, 70)
                        .build(CreateRancher.asResource("grill_smoking"), info -> new Single<>(info,
                                BlockRegistry.CAMPFIRE_GRILL::getDefaultState, null)),
                new CreateRecipeCategory.Builder<>(HauntingRecipe.class)
                        .addTypedRecipes(AllRecipeTypes.HAUNTING)
                        .catalystStack(ItemRegistry.ITEM_GRATE::asStack)
                        .doubleItemIcon(ItemRegistry.ITEM_GRATE.get(), Items.SOUL_CAMPFIRE)
                        .emptyBackground(177, 70)
                        .build(CreateRancher.asResource("grill_haunting"), info -> new Multi<>(info,
                                BlockRegistry.SOUL_CAMPFIRE_GRILL::getDefaultState, null)),
                new CreateRecipeCategory.Builder<>(SplashingRecipe.class)
                        .addTypedRecipes(AllRecipeTypes.SPLASHING)
                        .catalystStack(ItemRegistry.ITEM_GRATE::asStack)
                        .doubleItemIcon(ItemRegistry.ITEM_GRATE.get(), Items.WATER_BUCKET)
                        .emptyBackground(177, 70)
                        .build(CreateRancher.asResource("grill_washing"), info -> new Multi<>(info,
                                GrillCategories::waterGrill, GrillProcess.WASHING)),
                new CreateRecipeCategory.Builder<>(AbstractCookingRecipe.class)
                        .addTypedRecipesExcluding(() -> RecipeType.SMELTING, () -> RecipeType.BLASTING)
                        .addTypedRecipes(() -> RecipeType.BLASTING)
                        .removeRecipes(() -> RecipeType.SMOKING)
                        .removeNonAutomation()
                        .catalystStack(ItemRegistry.ITEM_GRATE::asStack)
                        .doubleItemIcon(ItemRegistry.ITEM_GRATE.get(), Items.LAVA_BUCKET)
                        .emptyBackground(177, 70)
                        .build(CreateRancher.asResource("grill_blasting"), info -> new Single<>(info,
                                GrillCategories::lavaGrill, GrillProcess.BLASTING)));
    }

    private static BlockState waterGrill() {
        return BlockRegistry.CAULDRON_GRILL.getDefaultState()
                .setValue(CauldronGrillBlock.CONTENTS, CauldronGrillBlock.Contents.WATER)
                .setValue(CauldronGrillBlock.LEVEL, 3);
    }

    private static BlockState lavaGrill() {
        return BlockRegistry.CAULDRON_GRILL.getDefaultState()
                .setValue(CauldronGrillBlock.CONTENTS, CauldronGrillBlock.Contents.LAVA);
    }

    // Draws a grill and its particles
    private static class Grill {

        private final Supplier<BlockState> state;
        @Nullable
        private final GrillProcess particleProcess;
        @Nullable
        private GrillParticles particles;

        Grill(Supplier<BlockState> state, @Nullable GrillProcess particleProcess) {
            this.state = state;
            this.particleProcess = particleProcess;
        }

        void draw(GuiGraphics graphics) {
            AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 70, 6);
            AllGuiTextures.JEI_SHADOW.render(graphics, SHADOW_X, SHADOW_Y);
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(GRILL_X, GRILL_Y, 0);
            pose.mulPose(Axis.XP.rotationDegrees(-12.5f));
            pose.mulPose(Axis.YP.rotationDegrees(22.5f));
            AnimatedKinetics.defaultBlockElement(state.get())
                    .scale(SCALE)
                    .atLocal(0, 0, BEHIND_FAN)
                    .render(graphics);
            if (particleProcess != null) {
                if (particles == null)
                    particles = new GrillParticles(particleProcess, state.get());
                particles.render(graphics, toScreen(pose), SCALE);
            }
            pose.popPose();
        }

        // Turns block coordinates into screen coordinates
        private static Matrix4f toScreen(PoseStack pose) {
            pose.pushPose();
            pose.scale(SCALE, SCALE, SCALE);
            pose.translate(0, 0, BEHIND_FAN);
            pose.scale(1, -1, 1);
            Matrix4f matrix = new Matrix4f(pose.last()
                    .pose());
            pose.popPose();
            return matrix;
        }
    }

    // The input slot above the arrow
    private static void addInput(IRecipeLayoutBuilder builder, Recipe<?> recipe) {
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 5)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addIngredients(recipe.getIngredients()
                        .get(0));
    }

    // Recipes with one result
    private static class Single<T extends Recipe<?>> extends ProcessingViaFanCategory<T> {

        private final Grill grill;

        Single(Info<T> info, Supplier<BlockState> state, @Nullable GrillProcess particles) {
            super(info);
            grill = new Grill(state, particles);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
            addInput(builder, recipe);
            builder.addSlot(RecipeIngredientRole.OUTPUT, 118, 48)
                    .setBackground(getRenderedSlot(), -1, -1)
                    .addItemStack(getResultItem(recipe));
        }

        @Override
        public void draw(T recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            grill.draw(graphics);
        }

        @Override
        protected void renderAttachedBlock(GuiGraphics graphics) {
        }
    }

    // Recipes with several results
    private static class Multi<T extends StandardProcessingRecipe<?>> extends ProcessingViaFanCategory.MultiOutput<T> {

        private final Grill grill;

        Multi(Info<T> info, Supplier<BlockState> state, @Nullable GrillProcess particles) {
            super(info);
            grill = new Grill(state, particles);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
            addInput(builder, recipe);
            List<ProcessingOutput> results = recipe.getRollableResults();
            for (int i = 0; i < results.size(); i++) {
                ProcessingOutput output = results.get(i);
                builder.addSlot(RecipeIngredientRole.OUTPUT, 118 + (i % 2 == 0 ? 0 : 19), 48 - (i / 2) * 19)
                        .setBackground(getRenderedSlot(output), -1, -1)
                        .addItemStack(output.getStack())
                        .addRichTooltipCallback(addStochasticTooltip(output));
            }
        }

        @Override
        public void draw(T recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            grill.draw(graphics);
        }

        @Override
        protected void renderAttachedBlock(GuiGraphics graphics) {
        }
    }
}
