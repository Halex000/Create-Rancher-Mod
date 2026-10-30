package com.halex.createrancher.ponder;

import com.halex.createrancher.content.shearer.MechanicalShearerBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// The Mechanical Shearer's Ponder scene
public class ShearerScenes {

    private static final int FADE_TICKS = 10;
    private static final BlockPos SHEARER = new BlockPos(3, 1, 3);
    private static final BlockPos COG = new BlockPos(4, 1, 3);
    private static final BlockPos SHAFT = new BlockPos(4, 0, 3);
    private static final BlockPos FUNNEL = new BlockPos(2, 1, 3);
    private static final int SPEED = 48;
    private static final int MOVE_TICKS = Mth.ceil(1024f / SPEED) + 1;
    private static final int RETRACT_TICKS = Mth.ceil(512f / SPEED) + 1;

    private static final Vec3 SHEEP_A = new Vec3(1.6, 1, 1.8);
    private static final Vec3 SHEEP_B = new Vec3(5.2, 1, 1.9);
    private static final Vec3 MOOSHROOM = new Vec3(1.8, 1, 5.0);

    public static void shearing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mechanical_shearer", "Shearing Animals with the Mechanical Shearer");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        scene.world()
                .showSection(util.select()
                        .fromTo(0, 1, 0, 6, 1, 6)
                        .substract(util.select()
                                .fromTo(1, 1, 1, 5, 1, 5)), Direction.DOWN);
        scene.idle(10);
        PonderMobs sheepA = PonderMobs.spawn(scene, EntityType.SHEEP, SHEEP_A, 200, false);
        PonderMobs sheepB = PonderMobs.spawn(scene, EntityType.SHEEP, SHEEP_B, 150, false);
        PonderMobs mooshroom = PonderMobs.spawn(scene, EntityType.MOOSHROOM, MOOSHROOM, 250, false);
        scene.idle(15);

        scene.world()
                .showSection(util.select()
                        .position(SHEARER), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("The Mechanical Shearer is able to shear nearby animals")
                .pointAt(util.vector()
                        .blockSurface(SHEARER, Direction.WEST))
                .placeNearTarget();
        scene.idle(140 + FADE_TICKS);

        scene.overlay()
                .chaseBoundingBoxOutline(PonderPalette.GREEN, SHEARER, new AABB(0, 0, 0, 7, 3, 7), 130);
        scene.overlay()
                .showText(120)
                .text("It reaches any animal in a 7x3x7 area around it")
                .pointAt(util.vector()
                        .topOf(SHEARER))
                .placeNearTarget();
        scene.idle(120 + FADE_TICKS);

        scene.world()
                .showSection(util.select()
                        .position(COG), Direction.DOWN);
        scene.idle(10);
        scene.world()
                .setKineticSpeed(util.select()
                        .position(SHEARER), -SPEED);
        scene.world()
                .setKineticSpeed(util.select()
                        .position(COG)
                        .add(util.select()
                                .position(SHAFT)), SPEED);
        scene.overlay()
                .showText(150)
                .attachKeyFrame()
                .text("Once powered, it shears them and the items that come off fall onto the ground")
                .pointAt(util.vector()
                        .topOf(SHEARER))
                .placeNearTarget();
        scene.idle(20);
        snip(scene, sheepA, () -> {
            scene.world()
                    .modifyEntity(sheepA.link(), entity -> ((Sheep) entity).setSheared(true));
            drop(scene, SHEEP_A, new ItemStack(Items.WHITE_WOOL), 2);
        });
        scene.idle(150 + FADE_TICKS - 20 - MOVE_TICKS - RETRACT_TICKS - 8);

        snip(scene, mooshroom, () -> {
            scene.world()
                    .modifyEntity(mooshroom.link(), Entity::discard);
            PonderMobs.spawn(scene, EntityType.COW, MOOSHROOM, 250, false);
            drop(scene, MOOSHROOM, new ItemStack(Items.RED_MUSHROOM), 5);
        });
        scene.overlay()
                .showText(200)
                .attachKeyFrame()
                .text("Any animal that is able to be sheared can interact with the Mechanical Shearer: sheep, mooshrooms, snow golems, and modded animals too")
                .pointAt(MOOSHROOM.add(0, 1.2, 0))
                .placeNearTarget();
        scene.idle(200 + FADE_TICKS);

        scene.overlay()
                .showText(120)
                .text("Animals with nothing to shear are left alone")
                .pointAt(SHEEP_A.add(0, 1.2, 0))
                .placeNearTarget();
        scene.idle(120 + FADE_TICKS);

        scene.world()
                .setBlock(FUNNEL, AllBlocks.ANDESITE_FUNNEL.getDefaultState()
                        .setValue(FunnelBlock.FACING, Direction.WEST)
                        .setValue(FunnelBlock.EXTRACTING, true), false);
        scene.world()
                .showSection(util.select()
                        .position(FUNNEL), Direction.EAST);
        scene.idle(15);
        scene.overlay()
                .showText(210)
                .attachKeyFrame()
                .text("If a funnel, hopper, chute, or other block that pulls from inventories is connected to the Mechanical Shearer, what it shears goes into that instead of falling onto the ground")
                .pointAt(util.vector()
                        .blockSurface(FUNNEL, Direction.WEST))
                .placeNearTarget();
        scene.idle(30);
        snip(scene, sheepB, () -> {
            scene.world()
                    .modifyEntity(sheepB.link(), entity -> ((Sheep) entity).setSheared(true));
            Vec3 out = util.vector()
                    .centerOf(FUNNEL.above())
                    .add(.15, -.45, 0);
            for (int i = 0; i < 2; i++) {
                scene.idle(6);
                scene.world()
                        .flapFunnel(FUNNEL, false);
                scene.world()
                        .createItemEntity(out, util.vector()
                                .of(-.05, 0, 0), new ItemStack(Items.WHITE_WOOL));
            }
        });
        scene.idle(210 + FADE_TICKS - 30 - MOVE_TICKS - RETRACT_TICKS - 8 - 12);
    }

    // Shears an animal
    private static void snip(CreateSceneBuilder scene, PonderMobs animal, Runnable sheared) {
        scene.world()
                .modifyBlockEntity(SHEARER, MechanicalShearerBlockEntity.class, shearer -> shearer.presentTarget(
                        animal.get()));
        moveArm(scene, Phase.MOVE_TO_INPUT, 0);
        scene.idle(MOVE_TICKS);
        scene.world()
                .modifyBlockEntity(SHEARER, MechanicalShearerBlockEntity.class,
                        MechanicalShearerBlockEntity::presentSnip);
        sheared.run();
        moveArm(scene, Phase.SEARCH_INPUTS, -1);
        scene.idle(RETRACT_TICKS + 8);
    }

    private static void moveArm(CreateSceneBuilder scene, Phase phase, int targetIndex) {
        scene.world()
                .modifyBlockEntity(SHEARER, MechanicalShearerBlockEntity.class,
                        shearer -> shearer.presentPhase(phase, targetIndex));
    }

    // Items popping off a sheared animal
    private static void drop(SceneBuilder scene, Vec3 animal, ItemStack stack, int count) {
        for (int i = 0; i < count; i++) {
            double angle = i * 2.4;
            Vec3 motion = new Vec3(Math.cos(angle) * .06, .2, Math.sin(angle) * .06);
            scene.world()
                    .createItemEntity(animal.add(0, 1, 0), motion, stack.copy());
        }
    }
}
