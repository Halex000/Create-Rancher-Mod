package com.halex.createrancher.ponder;

import com.halex.createrancher.content.burner.LiquidBlazeBurnerBlockEntity;
import com.halex.createrancher.index.FluidRegistry;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

// The Liquid Blaze Burner's Ponder scene
public class LiquidBlazeBurnerScenes {

    private static final int FADE_TICKS = 10;
    private static final int CLICK_TICKS = 40;
    private static final BlockPos BURNER = new BlockPos(1, 1, 2);
    private static final BlockPos PUMP = new BlockPos(2, 1, 2);
    private static final BlockPos PIPE = new BlockPos(3, 1, 2);
    private static final BlockPos TANK = new BlockPos(4, 1, 2);
    private static final BlockPos COG = new BlockPos(2, 1, 3);

    public static void burning(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("liquid_blaze_burner", "Burning Liquid Fuel");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        scene.world()
                .showSection(util.select()
                        .position(BURNER), Direction.DOWN);
        scene.idle(10);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("Like a Blaze Burner, the Liquid Blaze Burner heats whatever is above it, but it burns liquid fuels instead of items")
                .placeNearTarget()
                .pointAt(util.vector()
                        .topOf(BURNER));
        scene.idle(140 + FADE_TICKS);

        scene.overlay()
                .showControls(util.vector()
                        .topOf(BURNER), Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .withItem(new ItemStack(Items.LAVA_BUCKET));
        scene.idle(10);
        fuel(scene, Fluids.LAVA, HeatLevel.KINDLED);
        scene.idle(CLICK_TICKS - 10 + FADE_TICKS);
        scene.overlay()
                .showText(60)
                .attachKeyFrame()
                .text("Fuel can be poured in from a bucket...")
                .placeNearTarget()
                .pointAt(util.vector()
                        .topOf(BURNER));
        scene.idle(60 + FADE_TICKS);

        Selection pipeline = util.select()
                .position(PUMP)
                .add(util.select()
                        .position(PIPE))
                .add(util.select()
                        .position(TANK))
                .add(util.select()
                        .position(COG));
        scene.world()
                .modifyBlockEntity(TANK, FluidTankBlockEntity.class, tank -> tank.getTankInventory()
                        .fill(new FluidStack(Fluids.LAVA, 4000), IFluidHandler.FluidAction.EXECUTE));
        scene.world()
                .showSection(pipeline, Direction.DOWN);
        scene.idle(10);
        scene.world()
                .setKineticSpeed(util.select()
                        .position(PUMP)
                        .add(util.select()
                                .position(COG)), 32);
        scene.overlay()
                .showText(60)
                .text("...or piped in")
                .placeNearTarget()
                .pointAt(util.vector()
                        .blockSurface(PUMP, Direction.NORTH));
        scene.idle(60 + FADE_TICKS);

        scene.overlay()
                .showControls(util.vector()
                        .topOf(BURNER), Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .withItem(new ItemStack(FluidRegistry.BIOFUEL.get()
                        .getBucket()));
        scene.idle(10);
        fuel(scene, FluidRegistry.BIOFUEL.get()
                .getSource(), HeatLevel.SEETHING);
        scene.idle(CLICK_TICKS - 10 + FADE_TICKS);
        scene.overlay()
                .showText(100)
                .attachKeyFrame()
                .text("Lava keeps the blaze heated; some fuels, like Biofuel, superheat it")
                .placeNearTarget()
                .pointAt(util.vector()
                        .topOf(BURNER));
        scene.idle(100 + FADE_TICKS);
        scene.markAsFinished();
    }

    // Fills the burner with a fuel
    private static void fuel(CreateSceneBuilder scene, Fluid fluid, HeatLevel heat) {
        scene.world()
                .modifyBlockEntity(BURNER, LiquidBlazeBurnerBlockEntity.class, burner -> burner.setFuel(
                        new FluidStack(fluid, 1000)));
        scene.world()
                .modifyBlock(BURNER, state -> state.setValue(BlazeBurnerBlock.HEAT_LEVEL, heat), false);
    }
}
