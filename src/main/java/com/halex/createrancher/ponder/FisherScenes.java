package com.halex.createrancher.ponder;

import java.util.Random;

import com.halex.createrancher.content.fisher.MechanicalFisherBlockEntity;
import com.halex.createrancher.content.fisher.MechanicalFisherBlockEntity.Phase;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// The Mechanical Fisher's Ponder scene
public class FisherScenes {

    private static final BlockPos FISHER = new BlockPos(6, 2, 2);
    private static final BlockPos COG = new BlockPos(6, 2, 3);
    private static final BlockPos FUNNEL = new BlockPos(6, 2, 1);
    private static final BlockPos SPOT = new BlockPos(2, 1, 2);
    private static final Vec3 NET_AT = new Vec3(2.5, 1 + 8 / 9f, 2.5);
    private static final double SURFACE = 2;
    private static final int SPEED = 32;

    public static void fishing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mechanical_fisher", "Fishing with the Mechanical Fisher");
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();
        scene.world()
                .showSection(util.select()
                        .layer(1), Direction.UP);
        scene.idle(15);

        ItemStack fisherItem = BlockRegistry.MECHANICAL_FISHER.asStack();
        scene.overlay()
                .showControls(util.vector()
                        .topOf(SPOT), Pointing.DOWN, 105)
                .rightClick()
                .whileSneaking()
                .withItem(fisherItem);
        scene.idle(7);
        scene.overlay()
                .chaseBoundingBoxOutline(PonderPalette.INPUT, SPOT, new AABB(SPOT.getX(), SPOT.getY(), SPOT.getZ(),
                        SPOT.getX() + 1, NET_AT.y, SPOT.getZ() + 1), 430);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .colored(PonderPalette.INPUT)
                .text("Sneak and Right-Click water while holding the Mechanical Fisher to set its fishing spot")
                .pointAt(util.vector()
                        .topOf(SPOT))
                .placeNearTarget();
        scene.idle(150);
        scene.overlay()
                .showText(123)
                .text("The fishing spot is the top of the water, which needs open air above it")
                .pointAt(util.vector()
                        .topOf(SPOT))
                .placeNearTarget();
        scene.idle(133);

        scene.world()
                .showSection(util.select()
                        .position(FISHER), Direction.DOWN);
        scene.idle(15);
        scene.effects()
                .indicateSuccess(FISHER);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .text("Then place the Mechanical Fisher within 16 blocks of it")
                .pointAt(util.vector()
                        .topOf(FISHER))
                .placeNearTarget();
        scene.idle(150);

        scene.world()
                .showSection(util.select()
                        .position(COG), Direction.DOWN);
        scene.idle(10);
        scene.world()
                .setKineticSpeed(util.select()
                        .position(FISHER), -SPEED);
        scene.world()
                .setKineticSpeed(util.select()
                        .fromTo(COG.getX(), 0, COG.getZ(), COG.getX(), COG.getY(), COG.getZ()), SPEED);
        scene.overlay()
                .showText(158)
                .attachKeyFrame()
                .text("Power it with a cogwheel beside it, how fast it turns makes no difference")
                .pointAt(util.vector()
                        .topOf(COG))
                .placeNearTarget();
        scene.idle(168);

        ItemStack net = ItemRegistry.FISHING_NET.asStack();
        scene.overlay()
                .showControls(util.vector()
                        .topOf(FISHER), Pointing.DOWN, 88)
                .rightClick()
                .withItem(net);
        scene.idle(7);
        presentNet(scene, net);
        scene.overlay()
                .showText(123)
                .attachKeyFrame()
                .text("Right-Click it with a Fishing Net to load it")
                .pointAt(util.vector()
                        .topOf(FISHER))
                .placeNearTarget();
        scene.idle(133);

        scene.overlay()
                .showText(123)
                .attachKeyFrame()
                .text("It flings the net out onto the water at its fishing spot...")
                .pointAt(NET_AT)
                .placeNearTarget();
        cast(scene);
        scene.idle(101);
        scene.overlay()
                .showText(158)
                .text("...and waits for a bite just like a fishing rod")
                .pointAt(NET_AT)
                .placeNearTarget();
        waitForBite(scene, 90, 35, 10, new Random(7));
        haulIn(scene);
        scene.idle(13);

        scene.overlay()
                .showText(123)
                .attachKeyFrame()
                .text("Then it hauls the net back in with 1 to 4 catches")
                .pointAt(util.vector()
                        .topOf(FISHER))
                .placeNearTarget();
        scene.idle(133);
        scene.overlay()
                .showText(160)
                .text("Like a player thrown Fishing Net, it only catches treasure loot when the net has the Treasure Hunter enchantment")
                .pointAt(util.vector()
                        .topOf(FISHER))
                .placeNearTarget();
        scene.idle(170);

        scene.overlay()
                .showControls(util.vector()
                        .topOf(FISHER), Pointing.DOWN, 88)
                .rightClick();
        scene.overlay()
                .showText(123)
                .attachKeyFrame()
                .text("Right-Click it with an empty hand to collect the catch")
                .pointAt(util.vector()
                        .topOf(FISHER))
                .placeNearTarget();
        scene.idle(133);

        scene.world()
                .showSection(util.select()
                        .position(FUNNEL), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(140)
                .text("Funnels and hoppers can take the catch out too, and put new nets in")
                .pointAt(util.vector()
                        .blockSurface(FUNNEL, Direction.NORTH))
                .placeNearTarget();
        for (ItemStack fish : new ItemStack[] {new ItemStack(Items.COD), new ItemStack(Items.SALMON),
                new ItemStack(Items.COD)}) {
            scene.idle(15);
            scene.world()
                    .flapFunnel(FUNNEL, true);
            scene.world()
                    .createItemEntity(Vec3.atLowerCornerOf(FUNNEL)
                            .add(.5, .3, .1), new Vec3(0, .1, -.08), fish);
        }
        scene.idle(105);

        scene.world()
                .modifyEntities(ItemEntity.class, Entity::discard);
        scene.overlay()
                .showText(170)
                .attachKeyFrame()
                .text("It only casts the net while it has room for 4 catches, and each haul uses the net's durability until it breaks")
                .pointAt(util.vector()
                        .topOf(FISHER))
                .placeNearTarget();
        scene.idle(10);
        cast(scene);
        scene.idle(180);

        scene.overlay()
                .showControls(util.vector()
                        .topOf(FISHER), Pointing.DOWN, 105)
                .rightClick()
                .whileSneaking();
        scene.overlay()
                .showText(158)
                .attachKeyFrame()
                .text("Sneak and Right-Click the Mechanical Fisher with an empty hand to take the net back off, even while it's out on the water")
                .pointAt(util.vector()
                        .topOf(FISHER))
                .placeNearTarget();
        scene.idle(20);
        present(scene, Phase.IDLE, false);
        presentNet(scene, ItemStack.EMPTY);
        scene.idle(168);

        scene.effects()
                .indicateRedstone(FISHER);
        scene.overlay()
                .showText(123)
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .text("A redstone signal pauses the Mechanical Fisher")
                .pointAt(util.vector()
                        .topOf(FISHER))
                .placeNearTarget();
        scene.idle(133);
    }

    private static void present(SceneBuilder scene, Phase phase, boolean biting) {
        scene.world()
                .modifyBlockEntity(FISHER, MechanicalFisherBlockEntity.class, fisher -> fisher.presentPhase(phase,
                        biting));
    }

    private static void presentNet(SceneBuilder scene, ItemStack net) {
        scene.world()
                .modifyBlockEntity(FISHER, MechanicalFisherBlockEntity.class, fisher -> fisher.presentNet(net));
    }

    // Throws the net out
    private static void cast(SceneBuilder scene) {
        present(scene, Phase.LAUNCHING, false);
        scene.idle(MechanicalFisherBlockEntity.FLIGHT_TICKS);
        present(scene, Phase.FISHING, false);
        scene.effects()
                .emitParticles(NET_AT, (level, x, y, z) -> {
                    for (int i = 0; i < 12; i++)
                        level.addParticle(ParticleTypes.SPLASH, x + level.random.nextGaussian() * .6, SURFACE,
                                z + level.random.nextGaussian() * .6, 0, 0, 0);
                }, 1, 1);
    }

    // Waits for a bite
    private static void waitForBite(SceneBuilder scene, int waitTicks, int approachTicks, float angle,
                                    Random random) {
        scene.effects()
                .emitParticles(NET_AT, splashNearby(), .2f, waitTicks);
        scene.idle(waitTicks);

        for (int left = approachTicks; left > 0; left--) {
            angle += (float) ((random.nextDouble() - random.nextDouble()) * 9.188);
            float sin = Mth.sin(angle * Mth.DEG_TO_RAD);
            float cos = Mth.cos(angle * Mth.DEG_TO_RAD);
            Vec3 at = new Vec3(NET_AT.x + sin * left * .1f, SURFACE, NET_AT.z + cos * left * .1f);
            boolean bubble = random.nextFloat() < .15f;
            scene.effects()
                    .emitParticles(at, wake(sin, cos, bubble), 1, 1);
            scene.idle(1);
        }

        present(scene, Phase.FISHING, true);
        scene.effects()
                .emitParticles(NET_AT, (level, x, y, z) -> {
                    for (int i = 0; i < 20; i++)
                        level.addParticle(ParticleTypes.SPLASH, x + level.random.nextGaussian(), SURFACE,
                                z + level.random.nextGaussian(), level.random.nextGaussian() * .2, .2,
                                level.random.nextGaussian() * .2);
                }, 1, 1);
        scene.idle(MechanicalFisherBlockEntity.REACTION_TICKS);
    }

    private static void haulIn(SceneBuilder scene) {
        present(scene, Phase.REELING, false);
        scene.idle(MechanicalFisherBlockEntity.REEL_TICKS);
        present(scene, Phase.IDLE, false);
    }

    // Splashes near the net
    private static ParticleEmitter splashNearby() {
        return (level, x, y, z) -> {
            float angle = level.random.nextFloat() * Mth.TWO_PI;
            float distance = 1.5f + level.random.nextFloat() * 1.5f;
            double sx = x + Mth.sin(angle) * distance;
            double sz = z + Mth.cos(angle) * distance;
            if (!level.getBlockState(BlockPos.containing(sx, SURFACE - 1, sz))
                    .is(Blocks.WATER))
                return;
            int drops = 2 + level.random.nextInt(2);
            for (int i = 0; i < drops; i++)
                level.addParticle(ParticleTypes.SPLASH, sx + level.random.nextGaussian() * .1, SURFACE,
                        sz + level.random.nextGaussian() * .1, 0, 0, 0);
        };
    }

    // A fish's wake
    private static ParticleEmitter wake(float sin, float cos, boolean bubble) {
        return (level, x, y, z) -> {
            if (!level.getBlockState(BlockPos.containing(x, y - 1, z))
                    .is(Blocks.WATER))
                return;
            if (bubble)
                level.addParticle(ParticleTypes.BUBBLE, x + level.random.nextGaussian() * sin, y - .1,
                        z + level.random.nextGaussian() * cos, 0, 0, 0);
            float sideways = sin * .04f;
            float along = cos * .04f;
            level.addParticle(ParticleTypes.FISHING, x, y, z, along, .01, -sideways);
            level.addParticle(ParticleTypes.FISHING, x, y, z, -along, .01, sideways);
        };
    }
}
