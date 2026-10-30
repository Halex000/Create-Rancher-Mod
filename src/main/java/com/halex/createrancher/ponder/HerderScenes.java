package com.halex.createrancher.ponder;

import com.halex.createrancher.content.herder.MechanicalHerderBlockEntity;
import com.halex.createrancher.content.herder.MechanicalHerderBlockEntity.Phase;
import com.halex.createrancher.index.BlockRegistry;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// The Mechanical Herder's Ponder scenes
public class HerderScenes {

    private static final BlockPos HERDER = new BlockPos(4, 1, 7);
    private static final BlockPos COG = new BlockPos(4, 1, 8);
    private static final BlockPos SHAFT = new BlockPos(4, 0, 8);
    private static final BlockPos DROP_OFF = new BlockPos(8, 1, 4);
    private static final int SPEED = 48;
    private static final int CARRY_TICKS = 90;

    public static void movingAnimals(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mechanical_herder", "Moving Animals with the Mechanical Herder");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(.55f);
        scene.setSceneOffsetY(-1.5f);
        scene.showBasePlate();
        scene.idle(10);

        ItemStack herderItem = BlockRegistry.MECHANICAL_HERDER.asStack();
        scene.world()
                .showSection(pen(util), Direction.DOWN);
        scene.idle(10);
        PonderMobs cowA = PonderMobs.spawn(scene, EntityType.COW, new Vec3(3.5, 1, 4.5), 200, false);
        PonderMobs cowB = PonderMobs.spawn(scene, EntityType.COW, new Vec3(5.5, 1, 3), 160, false);
        scene.idle(15);

        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("Mechanical Herders pick up animals and carry them to a drop-off position")
                .pointAt(util.vector()
                        .topOf(4, 1, 4))
                .placeNearTarget();
        scene.idle(150);

        scene.world()
                .showSection(util.select()
                        .position(DROP_OFF), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showControls(util.vector()
                        .topOf(DROP_OFF), Pointing.DOWN, 105)
                .rightClick()
                .whileSneaking()
                .withItem(herderItem);
        scene.idle(7);
        scene.overlay()
                .chaseBoundingBoxOutline(PonderPalette.OUTPUT, DROP_OFF, new AABB(DROP_OFF), 650);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .colored(PonderPalette.OUTPUT)
                .text("Sneak and Right-Click a block while holding the Mechanical Herder to select the drop-off position")
                .pointAt(util.vector()
                        .blockSurface(DROP_OFF, Direction.NORTH))
                .placeNearTarget();
        scene.idle(150);

        scene.world()
                .showSection(util.select()
                        .position(HERDER), Direction.DOWN);
        scene.idle(15);
        scene.effects()
                .indicateSuccess(HERDER);
        scene.overlay()
                .showText(123)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .text("Then place the Mechanical Herder within 7 blocks of it")
                .pointAt(util.vector()
                        .blockSurface(HERDER, Direction.WEST))
                .placeNearTarget();
        scene.idle(133);

        scene.overlay()
                .chaseBoundingBoxOutline(PonderPalette.INPUT, HERDER, searchArea(5), 285);
        scene.overlay()
                .showText(140)
                .colored(PonderPalette.INPUT)
                .text("Its search area is on the side facing away from you when you place it")
                .pointAt(util.vector()
                        .topOf(2, 1, 3))
                .placeNearTarget();
        scene.idle(150);
        scene.overlay()
                .showText(123)
                .colored(PonderPalette.OUTPUT)
                .text("The drop-off position has to be outside of the search area")
                .pointAt(util.vector()
                        .topOf(DROP_OFF))
                .placeNearTarget();
        scene.idle(133);

        scene.world()
                .showSection(util.select()
                        .position(COG), Direction.DOWN);
        scene.idle(10);
        powerUp(scene, util);
        scene.overlay()
                .showText(105)
                .attachKeyFrame()
                .text("Give it rotational power to get it working")
                .pointAt(util.vector()
                        .blockSurface(COG, Direction.EAST))
                .placeNearTarget();
        scene.idle(115);

        scene.overlay()
                .showText(105)
                .text("It reaches down and grabs an animal with its suction cup...")
                .pointAt(cowAtStart(3.5, 1, 4.5))
                .placeNearTarget();
        scene.idle(55);
        chase(scene, cowA);
        scene.idle(55);
        scene.overlay()
                .showText(123)
                .attachKeyFrame()
                .text("...and carries them over to the drop-off position")
                .pointAt(util.vector()
                        .topOf(DROP_OFF))
                .placeNearTarget();
        carryToDropOff(scene, cowA);
        scene.idle(CARRY_TICKS);
        rest(scene);
        scene.idle(15);
        cowA.walk(scene, new Vec3(0, 0, 1), 10);
        cowA.walk(scene, new Vec3(0, -1, 0), 4);
        scene.idle(10);

        chase(scene, cowB);
        scene.idle(30);
        carryToDropOff(scene, cowB);
        scene.idle(CARRY_TICKS);
        rest(scene);
        scene.idle(20);

        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("It's as tall as a fence, so it can be used in the perimeter of an animal pen")
                .pointAt(util.vector()
                        .blockSurface(HERDER, Direction.NORTH))
                .placeNearTarget();
        scene.idle(150);

        scene.effects()
                .indicateRedstone(HERDER);
        scene.overlay()
                .showText(123)
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .text("A redstone signal pauses it")
                .pointAt(util.vector()
                        .blockSurface(HERDER, Direction.WEST))
                .placeNearTarget();
        scene.idle(133);
    }

    public static void filtering(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mechanical_herder_filtering", "Choosing What the Mechanical Herder Picks Up");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(.55f);
        scene.setSceneOffsetY(-1.5f);
        scene.showBasePlate();
        scene.idle(5);
        scene.world()
                .showSection(util.select()
                        .fromTo(1, 1, 1, 8, 1, 8), Direction.DOWN);
        powerUp(scene, util);
        PonderMobs cowA = PonderMobs.spawn(scene, EntityType.COW, new Vec3(2.8, 1, 3.3), 210, false);
        PonderMobs cowB = PonderMobs.spawn(scene, EntityType.COW, new Vec3(5.6, 1, 5), 140, false);
        PonderMobs calf = PonderMobs.spawn(scene, EntityType.COW, new Vec3(3.4, 1, 5.4), 170, true);
        PonderMobs pig = PonderMobs.spawn(scene, EntityType.PIG, new Vec3(5.4, 1, 2.6), 30, false);
        scene.idle(20);

        scene.overlay()
                .showControls(util.vector()
                        .topOf(HERDER), Pointing.DOWN, 123)
                .rightClick();
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("Right-Click the Herder with an empty hand to choose what it picks up")
                .pointAt(util.vector()
                        .blockSurface(HERDER, Direction.WEST))
                .placeNearTarget();
        scene.idle(150);

        scene.overlay()
                .showControls(util.vector()
                        .topOf(HERDER), Pointing.DOWN, 123)
                .withItem(new ItemStack(Items.NAME_TAG));
        scene.overlay()
                .showText(140)
                .text("Filter for a mob by name, or leave it empty for any mob")
                .pointAt(util.vector()
                        .blockSurface(HERDER, Direction.WEST))
                .placeNearTarget();
        scene.idle(150);

        scene.overlay()
                .showControls(util.vector()
                        .topOf(HERDER), Pointing.DOWN, 123)
                .withItem(new ItemStack(Items.EGG));
        scene.overlay()
                .showText(123)
                .text("Then choose whether it picks up babies, adults or both")
                .pointAt(util.vector()
                        .blockSurface(HERDER, Direction.WEST))
                .placeNearTarget();
        scene.idle(133);

        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .text("If set to baby cows, it takes the calf and leaves the grown cows and the pig alone")
                .pointAt(cowAtStart(3.4, 1.7, 5.4))
                .placeNearTarget();
        scene.idle(60);
        chase(scene, calf);
        scene.idle(30);
        carryToDropOff(scene, calf);
        scene.idle(CARRY_TICKS);
        rest(scene);
        scene.idle(25);

        Object areaSlot = new Object();
        scene.overlay()
                .chaseBoundingBoxOutline(PonderPalette.INPUT, areaSlot, searchArea(3), 70);
        scene.overlay()
                .showText(210)
                .attachKeyFrame()
                .colored(PonderPalette.INPUT)
                .text("The search area can be 3x3x3, 5x3x5 or 7x3x7")
                .pointAt(util.vector()
                        .topOf(4, 1, 5))
                .placeNearTarget();
        scene.idle(70);
        scene.overlay()
                .chaseBoundingBoxOutline(PonderPalette.INPUT, areaSlot, searchArea(5), 70);
        scene.idle(70);
        scene.overlay()
                .chaseBoundingBoxOutline(PonderPalette.INPUT, areaSlot, searchArea(7), 80);
        scene.idle(80);

        scene.overlay()
                .chaseBoundingBoxOutline(PonderPalette.OUTPUT, DROP_OFF, new AABB(DROP_OFF), 123);
        scene.overlay()
                .showText(123)
                .text("Sizes that would cover the drop-off position can't be selected")
                .pointAt(util.vector()
                        .topOf(DROP_OFF))
                .placeNearTarget();
        scene.idle(133);
    }

    private static Selection pen(SceneBuildingUtil util) {
        return util.select()
                .fromTo(1, 1, 1, 7, 1, 7)
                .substract(util.select()
                        .position(HERDER));
    }

    private static void powerUp(CreateSceneBuilder scene, SceneBuildingUtil util) {
        scene.world()
                .setKineticSpeed(util.select()
                        .position(HERDER), SPEED);
        scene.world()
                .setKineticSpeed(util.select()
                        .position(COG)
                        .add(util.select()
                                .position(SHAFT)), -SPEED);
    }

    // The herder's search area
    private static AABB searchArea(int size) {
        return new AABB(HERDER.getX() - size / 2, 0, HERDER.getZ() - size, HERDER.getX() + size / 2 + 1, 3,
                HERDER.getZ());
    }

    private static Vec3 cowAtStart(double x, double y, double z) {
        return new Vec3(x, y + 1.2, z);
    }

    private static void chase(SceneBuilder scene, PonderMobs mob) {
        scene.world()
                .modifyBlockEntity(HERDER, MechanicalHerderBlockEntity.class,
                        herder -> herder.present(Phase.MOVE_TO_MOB, mob.top(), Double.NEGATIVE_INFINITY, mob.get()));
    }

    // Carries a mob to the drop-off
    private static void carryToDropOff(SceneBuilder scene, PonderMobs mob) {
        scene.world()
                .modifyBlockEntity(HERDER, MechanicalHerderBlockEntity.class, herder -> {
                    double height = mob.get()
                            .getBbHeight();
                    Vec3 dropPoint = Vec3.atBottomCenterOf(DROP_OFF.above());
                    double lift = Math.max(mob.get()
                            .getY(), dropPoint.y) + MechanicalHerderBlockEntity.CARRY_LIFT + height;
                    herder.present(Phase.MOVE_TO_DROP_OFF, dropPoint.add(0, height, 0), lift, mob.get());
                });
    }

    private static void rest(SceneBuilder scene) {
        scene.world()
                .modifyBlockEntity(HERDER, MechanicalHerderBlockEntity.class, MechanicalHerderBlockEntity::presentRest);
    }
}
