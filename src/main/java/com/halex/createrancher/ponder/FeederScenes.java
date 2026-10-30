package com.halex.createrancher.ponder;

import com.halex.createrancher.content.feeder.MechanicalFeederBlockEntity;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// The Mechanical Feeder's Ponder scene
public class FeederScenes {

    private static final BlockPos FEEDER = new BlockPos(3, 1, 3);
    private static final BlockPos COG = new BlockPos(4, 1, 3);
    private static final BlockPos SHAFT = new BlockPos(4, 0, 3);
    private static final BlockPos DEPOT = new BlockPos(5, 1, 1);
    private static final Vec3 LONE_COW_AT = new Vec3(4.8, 1, 5);
    private static final Vec3 CALF_AT = new Vec3(1.7, 1, 3.2);
    private static final int SPEED = 48;
    private static final int MOVE_TICKS = Mth.ceil(1024f / SPEED) + 1;
    private static final int RETRACT_TICKS = Mth.ceil(512f / SPEED) + 1;

    public static void breeding(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mechanical_feeder", "Breeding Animals with the Mechanical Feeder");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        ItemStack feederItem = BlockRegistry.MECHANICAL_FEEDER.asStack();
        scene.world()
                .showSection(util.select()
                        .fromTo(0, 1, 0, 6, 1, 6)
                        .substract(util.select()
                                .fromTo(1, 1, 1, 5, 1, 5)), Direction.DOWN);
        scene.idle(10);
        PonderMobs cowA = PonderMobs.spawn(scene, EntityType.COW, new Vec3(1.6, 1, 1.8), 200, false);
        PonderMobs cowB = PonderMobs.spawn(scene, EntityType.COW, new Vec3(1.6, 1, 4.6), 250, false);
        PonderMobs loneCow = PonderMobs.spawn(scene, EntityType.COW, LONE_COW_AT, 120, false);
        scene.idle(15);

        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("Mechanical Feeders feed animals the food they breed with")
                .pointAt(new Vec3(1.6, 2.2, 3.2))
                .placeNearTarget();
        scene.idle(150);

        scene.world()
                .showSection(util.select()
                        .position(DEPOT), Direction.DOWN);
        scene.idle(10);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.UP, new ItemStack(Items.WHEAT, 3));
        scene.idle(10);
        scene.overlay()
                .showControls(util.vector()
                        .topOf(DEPOT), Pointing.DOWN, 105)
                .rightClick()
                .withItem(feederItem);
        scene.idle(7);
        AABB depotBounds = AllShapes.CASING_13PX.get(Direction.UP)
                .bounds();
        scene.overlay()
                .chaseBoundingBoxOutline(PonderPalette.INPUT, DEPOT, depotBounds.move(DEPOT), 470);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .colored(PonderPalette.INPUT)
                .text("Right-Click a depot, belt or other container while holding the Mechanical Feeder to make it the item pickup point")
                .pointAt(util.vector()
                        .blockSurface(DEPOT, Direction.WEST))
                .placeNearTarget();
        scene.idle(150);

        scene.world()
                .showSection(util.select()
                        .position(FEEDER), Direction.DOWN);
        scene.idle(15);
        scene.effects()
                .indicateSuccess(FEEDER);
        scene.overlay()
                .showText(123)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .text("Then place the Mechanical Feeder within 5 blocks of it")
                .pointAt(util.vector()
                        .blockSurface(FEEDER, Direction.WEST))
                .placeNearTarget();
        scene.idle(133);

        scene.overlay()
                .chaseBoundingBoxOutline(PonderPalette.GREEN, FEEDER, new AABB(0, 0, 0, 7, 3, 7), 168);
        scene.overlay()
                .showText(158)
                .text("It feeds animals in a 7x3x7 area around it")
                .pointAt(util.vector()
                        .topOf(FEEDER))
                .placeNearTarget();
        scene.idle(168);

        scene.world()
                .showSection(util.select()
                        .position(COG), Direction.DOWN);
        scene.idle(10);
        scene.world()
                .setKineticSpeed(util.select()
                        .position(FEEDER), -SPEED);
        scene.world()
                .setKineticSpeed(util.select()
                        .position(COG)
                        .add(util.select()
                                .position(SHAFT)), SPEED);
        scene.overlay()
                .showText(158)
                .attachKeyFrame()
                .text("Once powered, it feeds adults that are ready to breed, but only when they have a partner nearby")
                .pointAt(util.vector()
                        .topOf(FEEDER))
                .placeNearTarget();
        scene.idle(20);
        ItemStack wheat = new ItemStack(Items.WHEAT);
        feed(scene, cowA, wheat, 2);
        feed(scene, cowB, wheat, 1);

        PonderMobs calf = PonderMobs.spawn(scene, EntityType.COW, CALF_AT, 90, true);
        calf.showHearts(scene);
        scene.overlay()
                .showText(140)
                .text("The pair then breeds as usual")
                .pointAt(new Vec3(1.7, 1.8, 3.2))
                .placeNearTarget();
        scene.idle(150);

        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("If no pair is ready then it will wait for two animals to be available, holding on to any food it has already picked up")
                .pointAt(util.vector()
                        .topOf(FEEDER))
                .placeNearTarget();
        takeFood(scene, wheat, 0);
        scene.idle(115);
        scene.overlay()
                .showControls(util.vector()
                        .topOf(FEEDER), Pointing.DOWN, 88)
                .rightClick();
        scene.overlay()
                .showText(123)
                .text("Right-Click it with an empty hand to take back what it's holding")
                .pointAt(util.vector()
                        .blockSurface(FEEDER, Direction.WEST))
                .placeNearTarget();
        scene.idle(25);
        moveArm(scene, Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);
        scene.idle(108);

        ItemStack pellet = ItemRegistry.ALFALFA_PELLETS.asStack();
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.UP, pellet.copyWithCount(2));
        scene.idle(10);
        scene.overlay()
                .showText(168)
                .attachKeyFrame()
                .text("It also feeds Alfalfa Pellets to any animal that can eat them, whether or not they are ready to breed")
                .pointAt(util.vector()
                        .topOf(FEEDER))
                .placeNearTarget();
        scene.idle(10);
        feed(scene, loneCow, pellet, 1);
        feed(scene, calf, pellet, 0);
        scene.idle(10);

        scene.overlay()
                .showText(123)
                .text("Once an animal has digested its pellet, it drops Dung and is ready for another")
                .pointAt(LONE_COW_AT.add(0, 1.4, 0))
                .placeNearTarget();
        scene.idle(20);
        scene.world()
                .createItemEntity(LONE_COW_AT.add(-.6, .1, .8), Vec3.ZERO, BlockRegistry.DUNG.asStack());
        scene.world()
                .createItemEntity(CALF_AT.add(-.3, .1, .75), Vec3.ZERO, BlockRegistry.DUNG.asStack());
        scene.idle(103);

        scene.effects()
                .indicateRedstone(FEEDER);
        scene.overlay()
                .showText(123)
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .text("A redstone signal pauses the Mechanical Feeder")
                .pointAt(util.vector()
                        .blockSurface(FEEDER, Direction.WEST))
                .placeNearTarget();
        scene.idle(133);
    }

    private static void aimAt(SceneBuilder scene, PonderMobs animal) {
        scene.world()
                .modifyBlockEntity(FEEDER, MechanicalFeederBlockEntity.class, feeder -> feeder.presentTarget(animal.get()));
    }

    // Starts the arm's next move
    private static void moveArm(SceneBuilder scene, Phase phase, ItemStack held, int targetIndex) {
        scene.world()
                .modifyBlockEntity(FEEDER, MechanicalFeederBlockEntity.class,
                        feeder -> feeder.presentPhase(phase, held, targetIndex));
    }

    // Picks up a piece of food from the depot
    private static void takeFood(CreateSceneBuilder scene, ItemStack food, int left) {
        moveArm(scene, Phase.MOVE_TO_INPUT, ItemStack.EMPTY, 0);
        scene.idle(MOVE_TICKS);
        scene.world()
                .removeItemsFromBelt(DEPOT);
        if (left > 0)
            scene.world()
                    .createItemOnBeltLike(DEPOT, Direction.UP, food.copyWithCount(left));
        moveArm(scene, Phase.SEARCH_OUTPUTS, food.copyWithCount(1), -1);
        scene.idle(RETRACT_TICKS);
    }

    // Feeds the animal a piece of food
    private static void feed(CreateSceneBuilder scene, PonderMobs animal, ItemStack food, int foodLeft) {
        takeFood(scene, food, foodLeft);
        aimAt(scene, animal);
        moveArm(scene, Phase.MOVE_TO_OUTPUT, food.copyWithCount(1), 0);
        scene.idle(MOVE_TICKS);
        if (food.is(ItemRegistry.ALFALFA_PELLETS.get()))
            animal.showEating(scene, food);
        else
            animal.showHearts(scene);
        moveArm(scene, Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);
        scene.idle(RETRACT_TICKS + 8);
    }
}
