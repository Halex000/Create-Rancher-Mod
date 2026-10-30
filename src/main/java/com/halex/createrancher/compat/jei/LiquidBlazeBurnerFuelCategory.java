package com.halex.createrancher.compat.jei;

import java.util.List;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.burner.LiquidBlazeBurnerFuels;
import com.halex.createrancher.index.BlockRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

// JEI page for each Liquid Blaze Burner fuel
public class LiquidBlazeBurnerFuelCategory implements IRecipeCategory<LiquidBlazeBurnerFuels.Fuel> {

    public static final RecipeType<LiquidBlazeBurnerFuels.Fuel> TYPE = RecipeType.create(CreateRancher.MOD_ID,
            "liquid_blaze_burner_fuel", LiquidBlazeBurnerFuels.Fuel.class);
    private static final int WIDTH = 177, HEIGHT = 56;
    private static final int MIDDLE = WIDTH / 2;
    private static final int SLOTS_Y = 1;
    private static final int ARROW_Y = 6;
    private static final int BAR_Y = 33;

    private final IDrawable icon;

    public LiquidBlazeBurnerFuelCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(BlockRegistry.LIQUID_BLAZE_BURNER.asStack());
    }

    @Override
    public RecipeType<LiquidBlazeBurnerFuels.Fuel> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("createrancher.jei.liquid_blaze_burner_fuel");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LiquidBlazeBurnerFuels.Fuel fuel, IFocusGroup focuses) {
        List<Fluid> fluids = fuel.fluids();
        builder.addSlot(RecipeIngredientRole.INPUT, MIDDLE - 36, SLOTS_Y)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addItemStacks(fluids.stream()
                        .map(fluid -> new ItemStack(fluid.getBucket()))
                        .filter(bucket -> !bucket.isEmpty())
                        .toList());
        builder.addSlot(RecipeIngredientRole.INPUT, MIDDLE - 16, SLOTS_Y)
                .setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
                .addIngredients(NeoForgeTypes.FLUID_STACK, fluids.stream()
                        .map(fluid -> new FluidStack(fluid, 1000))
                        .toList())
                .setFluidRenderer(1000, false, 16, 16);
    }

    @Override
    public void draw(LiquidBlazeBurnerFuels.Fuel fuel, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
                     double mouseY) {
        var font = Minecraft.getInstance().font;
        HeatCondition heat = fuel.superheated() ? HeatCondition.SUPERHEATED : HeatCondition.HEATED;
        AllGuiTextures.JEI_LIGHT.render(graphics, 81, BAR_Y + 8);
        AllGuiTextures.JEI_HEAT_BAR.render(graphics, 4, BAR_Y);
        graphics.drawString(font, CreateLang.translateDirect(heat.getTranslationKey()), 9, BAR_Y + 6, heat.getColor(), false);
        drawBurner(graphics, MIDDLE + 3, BAR_Y - 25, heat.visualizeAsBlazeBurner());
        AllGuiTextures.JEI_DOWN_ARROW.render(graphics, MIDDLE + 3, ARROW_Y);
        Component time = Component.literal(time(fuel.burnTime()));
        graphics.drawString(font, time, WIDTH - 9 - font.width(time), BAR_Y + 6, 0xFFFFFF);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, LiquidBlazeBurnerFuels.Fuel fuel, IRecipeSlotsView slots,
                           double mouseX, double mouseY) {
        int width = Minecraft.getInstance().font.width(time(fuel.burnTime()));
        if (mouseX >= WIDTH - 9 - width && mouseX < WIDTH - 9 && mouseY >= BAR_Y + 5 && mouseY < BAR_Y + 15)
            tooltip.add(Component.translatable("createrancher.jei.liquid_blaze_burner_fuel.burns",
                    time(fuel.burnTime())));
    }

    // A bucket's burn time in minutes and seconds
    private static String time(int ticks) {
        int seconds = ticks / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    // Draws the burner with its ring of fire
    private static void drawBurner(GuiGraphics graphics, int x, int y, HeatLevel heat) {
        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(x, y, 200);
        ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
        ms.mulPose(Axis.YP.rotationDegrees(22.5f));
        int scale = 23;
        float bob = (Mth.sin(AnimationTickHolder.getRenderTime() / 16f) + 0.5f) / 16f;
        AnimatedKinetics.defaultBlockElement(BlockRegistry.LIQUID_BLAZE_BURNER.getDefaultState()
                        .setValue(BlazeBurnerBlock.HEAT_LEVEL, heat))
                .atLocal(0, 1.65, 0)
                .scale(scale)
                .render(graphics);
        PartialModel blaze = BlazeBurnerRenderer.getBlazeModel(heat, false);
        PartialModel rods = heat == HeatLevel.SEETHING ? AllPartialModels.BLAZE_BURNER_SUPER_RODS_2
                : AllPartialModels.BLAZE_BURNER_RODS_2;
        AnimatedKinetics.defaultBlockElement(blaze)
                .atLocal(0, 1.8 - 2 / 16f, 0)
                .rotateBlock(0, 180, 0)
                .scale(scale)
                .render(graphics);
        AnimatedKinetics.defaultBlockElement(rods)
                .atLocal(1, 1.7 + bob - 1 / 16f, 1)
                .rotate(0, 180, 0)
                .scale(scale)
                .render(graphics);

        ms.scale(scale, -scale, scale);
        ms.translate(0, -1.8 + 2 / 16f, 0);
        SpriteShiftEntry flame = heat == HeatLevel.SEETHING ? AllSpriteShifts.SUPER_BURNER_FLAME
                : AllSpriteShifts.BURNER_FLAME;
        float spriteWidth = flame.getTarget()
                .getU1() - flame.getTarget()
                .getU0();
        float spriteHeight = flame.getTarget()
                .getV1() - flame.getTarget()
                .getV0();
        float time = AnimationTickHolder.getRenderTime(Minecraft.getInstance().level);
        float speed = 1 / 32f + 1 / 64f * heat.ordinal();
        double vScroll = speed * time;
        vScroll = (vScroll - Math.floor(vScroll)) * spriteHeight / 2;
        double uScroll = speed * time / 2;
        uScroll = (uScroll - Math.floor(uScroll)) * spriteWidth / 2;
        CachedBuffers.partial(AllPartialModels.BLAZE_BURNER_FLAME, Blocks.AIR.defaultBlockState())
                .shiftUVScrolling(flame, (float) uScroll, (float) vScroll)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(ms, graphics.bufferSource()
                        .getBuffer(RenderType.cutoutMipped()));
        ms.popPose();
    }
}
