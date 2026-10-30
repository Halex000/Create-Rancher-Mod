package com.halex.createrancher.ponder;

import com.halex.createrancher.content.grate.GrateMetal;
import com.halex.createrancher.content.grate.GratedBelt;
import com.halex.createrancher.content.grill.CauldronGrillBlock;
import com.halex.createrancher.content.grill.GrillBlockEntity;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

// The Item Grate's Ponder scenes
public class GrateScenes {

    private static final int BELT_SPEED = 32;
    private static final float TEXT_TIME = 1.6f;
    private static final int TEXT_FADE = 5;

    // Keeps track of time in a scene so text doesn't overlap
    private static class Timeline {

        private final SceneBuilder scene;
        private int now;
        private int textGone;

        Timeline(SceneBuilder scene) {
            this.scene = scene;
        }

        void idle(int ticks) {
            scene.idle(ticks);
            now += ticks;
        }

        TextElementBuilder text(int ticks) {
            if (textGone > now)
                idle(textGone - now);
            int shown = Math.round(ticks * TEXT_TIME);
            textGone = now + shown + 2 * TEXT_FADE;
            return scene.overlay()
                    .showText(shown);
        }

        // Waits until the text showing now has gone
        void finishText() {
            if (textGone > now)
                idle(textGone - now);
        }
    }

    private static final int BELT_X = 3;

    public static void belts(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        Timeline time = new Timeline(scene);
        scene.title("item_grate_belts", "Laying Item Grates over Belts");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        time.idle(10);

        Selection belt = util.select()
                .fromTo(BELT_X, 1, 0, BELT_X, 1, 6);
        Selection shaft = util.select()
                .position(BELT_X - 1, 1, 0);
        scene.world()
                .showSection(belt.add(shaft), Direction.DOWN);
        scene.world()
                .setKineticSpeed(belt.add(shaft), BELT_SPEED);
        time.idle(20);

        ItemStack grate = ItemRegistry.ITEM_GRATE.asStack();
        BlockPos first = new BlockPos(BELT_X, 1, 2);
        scene.overlay()
                .showControls(util.vector()
                        .topOf(first), Pointing.DOWN, 50)
                .rightClick()
                .withItem(grate);
        time.idle(10);
        time.text(100)
                .attachKeyFrame()
                .text("Item Grates can be placed over flat belt segments that have no shaft through them")
                .pointAt(beltSide(3))
                .placeNearTarget();
        for (int z = 2; z <= 4; z++) {
            setGrated(scene, new BlockPos(BELT_X, 1, z), true);
            time.idle(8);
        }
        time.finishText();

        PonderMobs pig = PonderMobs.spawn(scene, EntityType.PIG, new Vec3(BELT_X + .5, 1 + 14 / 16f, 3.5), 180, false);
        time.idle(10);
        time.text(100)
                .attachKeyFrame()
                .text("Players and mobs can stand and walk on grates without being moved by the belt below")
                .pointAt(beltSide(2))
                .placeNearTarget();
        time.idle(120);
        time.finishText();
        scene.world()
                .modifyEntity(pig.link(), Entity::discard);
        time.idle(10);

        BlockPos middle = new BlockPos(BELT_X, 1, 3);
        scene.world()
                .createItemEntity(util.vector()
                        .topOf(middle)
                        .add(0, 1, 0), Vec3.ZERO, new ItemStack(Items.COPPER_INGOT));
        time.text(100)
                .attachKeyFrame()
                .text("Items dropped onto a grate will fall through onto the belt beneath to be transported")
                .pointAt(beltSide(3))
                .placeNearTarget();
        time.idle(14);
        scene.world()
                .modifyEntities(ItemEntity.class, Entity::discard);
        scene.world()
                .createItemOnBelt(middle, Direction.UP, new ItemStack(Items.COPPER_INGOT));
        time.idle(50);
        scene.world()
                .createItemOnBelt(new BlockPos(BELT_X, 1, 0), Direction.UP, new ItemStack(Items.IRON_INGOT));
        time.finishText();
        scene.world()
                .modifyEntities(ItemEntity.class, Entity::discard);

        BlockPos last = new BlockPos(BELT_X, 1, 4);
        scene.overlay()
                .showControls(util.vector()
                        .topOf(last), Pointing.DOWN, 60)
                .rightClick()
                .whileSneaking();
        time.idle(15);
        setGrated(scene, last, false);
        scene.world()
                .createItemEntity(util.vector()
                        .topOf(last), new Vec3(-.1, .25, 0), grate);
        time.text(90)
                .attachKeyFrame()
                .text("Sneak and Right-Click a grate with an empty hand to take it back off the belt")
                .pointAt(beltSide(4))
                .placeNearTarget();
        time.finishText();
        scene.world()
                .modifyEntities(ItemEntity.class, Entity::discard);

        scene.overlay()
                .showControls(util.vector()
                        .topOf(last), Pointing.DOWN, 30)
                .rightClick()
                .withItem(grate);
        time.idle(10);
        setGrated(scene, last, true);
        time.idle(20);
        time.text(110)
                .attachKeyFrame()
                .text("Right-Click a grate on a belt with a copper, iron or brass nugget to plate it with that metal")
                .pointAt(beltSide(3))
                .placeNearTarget();
        GrateMetal[] metals = {GrateMetal.COPPER, GrateMetal.IRON, GrateMetal.BRASS};
        for (int i = 0; i < metals.length; i++) {
            BlockPos pos = new BlockPos(BELT_X, 1, 2 + i);
            scene.overlay()
                    .showControls(util.vector()
                            .topOf(pos), Pointing.DOWN, 30)
                    .rightClick()
                    .withItem(new ItemStack(metals[i].getNugget()));
            time.idle(10);
            setGrateMetal(scene, pos, metals[i]);
            time.idle(30);
        }
        time.finishText();
    }

    private static void setGrateMetal(SceneBuilder scene, BlockPos pos, GrateMetal metal) {
        scene.world()
                .modifyBlockEntity(pos, BeltBlockEntity.class,
                        belt -> ((GratedBelt) belt).createrancher$setGrateMetal(metal));
    }

    private static void setGrated(SceneBuilder scene, BlockPos pos, boolean grated) {
        scene.world()
                .modifyBlockEntity(pos, BeltBlockEntity.class, belt -> ((GratedBelt) belt).createrancher$setGrated(grated));
    }

    // The side of the belt at a segment
    private static Vec3 beltSide(int z) {
        return new Vec3(BELT_X + 1 / 16d, 1.5, z + .5);
    }

    private static final BlockPos CAMPFIRE = new BlockPos(2, 1, 0);
    private static final BlockPos SOUL_GRILL = new BlockPos(4, 1, 1);
    private static final BlockPos LAVA = new BlockPos(5, 1, 4);
    private static final BlockPos MAGMA = new BlockPos(4, 1, 6);
    private static final BlockPos WATER_GRILL = new BlockPos(4, 2, 6);

    private static final BlockPos HOPPER = new BlockPos(3, 1, 4);
    private static final BlockPos GRILL = new BlockPos(3, 2, 4);
    private static final BlockPos FUNNEL = new BlockPos(3, 2, 5);
    private static final BlockPos DUST = new BlockPos(2, 1, 4);
    private static final BlockPos LEVER = new BlockPos(1, 1, 4);
    private static final BlockPos ARM = new BlockPos(4, 1, 2);
    private static final BlockPos COG = new BlockPos(5, 1, 2);
    private static final BlockPos DEPOT = new BlockPos(3, 1, 1);
    private static final int ARM_SPEED = 24;
    private static final int ARM_MOVE = 45;
    private static final int ARM_FINISH_APART = 120;

    private static final BlockPos BELT_GRILL = new BlockPos(3, 1, 3);
    private static final BlockPos BELT_IN = new BlockPos(3, 1, 0);

    public static void grills(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        Timeline time = new Timeline(scene);
        scene.title("item_grate_grills", "Grilling with Item Grates");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(.85f);
        scene.showBasePlate();
        time.idle(10);

        scene.world()
                .showSection(util.select()
                        .position(CAMPFIRE), Direction.DOWN);
        time.idle(15);
        addGrate(scene, util, time, CAMPFIRE, BlockRegistry.CAMPFIRE_GRILL.getDefaultState()
                .setValue(CampfireBlock.FACING, Direction.SOUTH));
        time.text(90)
                .text("Right-Click a campfire with an Item Grate to turn it into a grill")
                .pointAt(beside(CAMPFIRE))
                .placeNearTarget();
        time.finishText();

        scene.overlay()
                .showControls(util.vector()
                        .topOf(CAMPFIRE), Pointing.DOWN, 50)
                .rightClick()
                .withItem(new ItemStack(Items.BEEF));
        time.idle(10);
        time.text(110)
                .attachKeyFrame()
                .text("Up to 9 items can cook on a grill at once, Right-Click it with them or drop them onto it")
                .pointAt(beside(CAMPFIRE))
                .placeNearTarget();
        place(time, scene, CAMPFIRE, new ItemStack(Items.BEEF), 4);
        time.idle(20);
        scene.world()
                .createItemEntity(util.vector()
                        .topOf(CAMPFIRE)
                        .add(0, 1.2, 0), Vec3.ZERO, new ItemStack(Items.BEEF, 5));
        time.idle(12);
        clearDrops(scene);
        place(time, scene, CAMPFIRE, new ItemStack(Items.BEEF), 5);
        time.finishText();

        finish(scene, CAMPFIRE);
        time.idle(15);
        time.text(80)
                .text("Once they're done, they pop off the top")
                .pointAt(beside(CAMPFIRE))
                .placeNearTarget();
        time.finishText();
        clearDrops(scene);

        scene.world()
                .showSection(util.select()
                        .position(SOUL_GRILL)
                        .add(util.select()
                                .position(LAVA))
                        .add(util.select()
                                .fromTo(MAGMA, WATER_GRILL)), Direction.DOWN);
        time.idle(20);
        addGrate(scene, util, time, SOUL_GRILL, BlockRegistry.SOUL_CAMPFIRE_GRILL.getDefaultState()
                .setValue(CampfireBlock.FACING, Direction.SOUTH));
        addGrate(scene, util, time, LAVA, BlockRegistry.CAULDRON_GRILL.getDefaultState()
                .setValue(CauldronGrillBlock.CONTENTS, CauldronGrillBlock.Contents.LAVA));
        addGrate(scene, util, time, WATER_GRILL, BlockRegistry.CAULDRON_GRILL.getDefaultState()
                .setValue(CauldronGrillBlock.CONTENTS, CauldronGrillBlock.Contents.WATER));
        time.text(90)
                .attachKeyFrame()
                .text("Soul campfires and cauldrons of water or lava can have a grate placed on them too")
                .pointAt(beside(SOUL_GRILL))
                .placeNearTarget();
        time.finishText();

        place(time, scene, CAMPFIRE, new ItemStack(Items.BEEF), 3);
        time.text(90)
                .attachKeyFrame()
                .text("Over a campfire, items are smoked")
                .pointAt(beside(CAMPFIRE))
                .placeNearTarget();
        time.idle(20);
        finish(scene, CAMPFIRE);
        time.finishText();

        place(time, scene, SOUL_GRILL, new ItemStack(Items.POTATO), 3);
        time.text(90)
                .text("Over a soul campfire, items are haunted")
                .pointAt(beside(SOUL_GRILL))
                .placeNearTarget();
        time.idle(20);
        finish(scene, SOUL_GRILL);
        time.finishText();

        place(time, scene, LAVA, new ItemStack(Items.RAW_IRON), 3);
        time.text(100)
                .text("Over a full cauldron of lava, items are smelted")
                .pointAt(beside(LAVA))
                .placeNearTarget();
        time.idle(20);
        finish(scene, LAVA);
        time.finishText();

        place(time, scene, WATER_GRILL, AllItems.CRUSHED_IRON.asStack(), 3);
        time.text(100)
                .text("Over a full cauldron of water, items are washed")
                .pointAt(beside(WATER_GRILL))
                .placeNearTarget();
        time.idle(20);
        finish(scene, WATER_GRILL);
        time.finishText();
        time.text(110)
                .colored(PonderPalette.BLUE)
                .text("...but only while something heats it from beneath: lava, magma blocks, fire, campfires, or blaze burners will do the trick")
                .pointAt(beside(MAGMA))
                .placeNearTarget();
        time.finishText();


        scene.world()
                .hideSection(util.select()
                        .position(CAMPFIRE)
                        .add(util.select()
                                .position(SOUL_GRILL))
                        .add(util.select()
                                .position(LAVA))
                        .add(util.select()
                                .fromTo(MAGMA, WATER_GRILL)), Direction.UP);
        time.idle(15);
        clearDrops(scene);
        time.idle(10);

        Selection armAndCog = util.select()
                .position(ARM)
                .add(util.select()
                        .position(COG));
        Selection grillParts = util.select()
                .fromTo(HOPPER, GRILL)
                .add(util.select()
                        .position(FUNNEL))
                .add(util.select()
                        .fromTo(LEVER, DUST));
        Selection single = grillParts.copy()
                .add(armAndCog)
                .add(util.select()
                        .position(DEPOT));
        buildSingleGrill(scene);
        sparkleRedstone(scene);
        scene.world()
                .showSection(grillParts, Direction.DOWN);
        hold(scene, GRILL);
        time.idle(10);
        scene.world()
                .showSection(armAndCog.add(util.select()
                        .position(DEPOT)), Direction.DOWN);
        scene.world()
                .setKineticSpeed(armAndCog, -ARM_SPEED);
        time.idle(20);

        time.text(100)
                .attachKeyFrame()
                .text("Funnels can put items onto a grill, or take finished ones off it")
                .pointAt(new Vec3(FUNNEL.getX(), FUNNEL.getY() + .5, FUNNEL.getZ() + .2))
                .placeNearTarget();
        for (int i = 0; i < 3; i++) {
            scene.world()
                    .createItemEntity(new Vec3(FUNNEL.getX() + .5, FUNNEL.getY() + 1.3, FUNNEL.getZ() + .6), Vec3.ZERO,
                            new ItemStack(Items.BEEF));
            time.idle(10);
            clearDrops(scene);
            scene.world()
                    .flapFunnel(FUNNEL, false);
            place(time, scene, GRILL, new ItemStack(Items.BEEF), 1);
            time.idle(14);
        }
        time.finishText();

        setLever(scene, util, time, true);
        scene.world()
                .modifyBlockEntity(GRILL, GrillBlockEntity.class, grill -> grill.getArmItemHandler(ARM)
                        .extractItem(0, 1, true));
        scene.world()
                .modifyBlockEntity(GRILL, GrillBlockEntity.class, be -> be.presentTimeLeft(3, ARM_FINISH_APART));
        time.idle(10);
        ItemStack cooked = new ItemStack(Items.COOKED_BEEF);
        time.text(80)
                .text("So can Mechanical Arms")
                .pointAt(Vec3.atLowerCornerOf(ARM)
                        .add(0, .15, .5))
                .placeNearTarget();
        armTakes(scene, time, cooked);
        time.finishText();
        armTakes(scene, time, cooked);
        time.text(110)
                .text("While something is taking items out of a grill, finished items wait for it instead of popping off the top of the grill")
                .pointAt(beside(GRILL))
                .placeNearTarget();
        armTakes(scene, time, cooked);
        armRests(scene, time);
        time.finishText();

        setLever(scene, util, time, false);
        place(time, scene, GRILL, new ItemStack(Items.BEEF), 3);
        scene.world()
                .modifyBlockEntity(GRILL, GrillBlockEntity.class, grill -> grill.getItemHandler()
                        .extractItem(0, 1, true));
        time.idle(20);
        finish(scene, GRILL);
        time.idle(10);
        time.text(90)
                .text("Hoppers and chutes beneath a grill take finished items out of it too")
                .pointAt(beside(HOPPER))
                .placeNearTarget();
        for (int i = 0; i < 3; i++) {
            time.idle(25);
            takeOne(scene, GRILL, false);
        }
        time.finishText();

        scene.world()
                .hideSection(single, Direction.UP);
        time.idle(15);

        Selection belts = util.select()
                .fromTo(3, 1, 0, 3, 1, 6)
                .substract(util.select()
                        .position(BELT_GRILL));
        Selection shafts = util.select()
                .position(4, 1, 0)
                .add(util.select()
                        .position(4, 1, 4));
        scene.world()
                .setBlocks(single, Blocks.AIR.defaultBlockState(), false);
        scene.world()
                .restoreBlocks(single.copy()
                        .add(util.select()
                                .fromTo(3, 1, 0, 3, 1, 6))
                        .add(shafts));
        time.idle(10);
        scene.world()
                .showSection(belts.add(shafts)
                        .add(util.select()
                                .position(BELT_GRILL)), Direction.DOWN);
        hold(scene, BELT_GRILL);
        scene.world()
                .setKineticSpeed(belts.add(shafts), BELT_SPEED);
        time.idle(15);
        scene.world()
                .createItemOnBelt(BELT_IN, Direction.UP, new ItemStack(Items.BEEF));
        time.text(100)
                .attachKeyFrame()
                .text("A belt running into the side of a grill slides items onto its grate")
                .pointAt(beside(BELT_GRILL))
                .placeNearTarget();
        time.finishText();

        finish(scene, BELT_GRILL);
        time.text(100)
                .text("Once they're done, they slide off the far side onto the next belt")
                .pointAt(beside(BELT_GRILL))
                .placeNearTarget();
        time.finishText();

        scene.world()
                .createItemOnBelt(BELT_IN.south(), Direction.UP, new ItemStack(Items.COBBLESTONE));
        time.text(90)
                .text("Anything the grill can't process slides straight across")
                .pointAt(beside(BELT_GRILL))
                .placeNearTarget();
        time.finishText();
    }

    // The arm moves a finished item to the depot
    private static void armTakes(CreateSceneBuilder scene, Timeline time, ItemStack item) {
        moveArm(scene, Phase.MOVE_TO_INPUT, ItemStack.EMPTY, 0);
        time.idle(ARM_MOVE);
        takeOne(scene, GRILL, true);
        moveArm(scene, Phase.MOVE_TO_OUTPUT, item, 0);
        time.idle(ARM_MOVE);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.UP, item);
    }

    // Sends the arm back to rest
    private static void armRests(CreateSceneBuilder scene, Timeline time) {
        moveArm(scene, Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);
        time.idle(ARM_MOVE);
    }

    // Moves the arm
    private static void moveArm(SceneBuilder scene, Phase phase, ItemStack held, int target) {
        scene.world()
                .modifyBlockEntity(ARM, ArmBlockEntity.class, arm -> {
                    HolderLookup.Provider registries = arm.getLevel()
                            .registryAccess();
                    CompoundTag tag = arm.saveWithFullMetadata(registries);
                    tag.putString("Phase", phase.name());
                    tag.put("HeldItem", held.saveOptional(registries));
                    tag.putInt("TargetPointIndex", target);
                    tag.putFloat("MovementProgress", 0);
                    arm.readClient(tag, registries);
                });
    }

    // Flicks the lever
    private static void setLever(SceneBuilder scene, SceneBuildingUtil util, Timeline time, boolean on) {
        scene.overlay()
                .showControls(util.vector()
                        .topOf(LEVER), Pointing.DOWN, 30)
                .rightClick();
        time.idle(10);
        scene.world()
                .modifyBlock(LEVER, state -> state.setValue(LeverBlock.POWERED, on), false);
        scene.world()
                .modifyBlock(DUST, state -> state.setValue(RedStoneWireBlock.POWER, on ? 15 : 0), false);
        scene.world()
                .modifyBlock(HOPPER, state -> state.setValue(HopperBlock.ENABLED, !on), false);
        scene.effects()
                .indicateRedstone(LEVER);
        time.idle(20);
    }

    // Sets up the single grill
    private static void buildSingleGrill(SceneBuilder scene) {
        scene.world()
                .setBlock(HOPPER, Blocks.HOPPER.defaultBlockState(), false);
        scene.world()
                .setBlock(GRILL, BlockRegistry.CAMPFIRE_GRILL.getDefaultState()
                        .setValue(CampfireBlock.FACING, Direction.SOUTH), false);
        scene.world()
                .setBlock(FUNNEL, AllBlocks.ANDESITE_FUNNEL.getDefaultState()
                        .setValue(FunnelBlock.FACING, Direction.SOUTH), false);
        scene.world()
                .setBlock(DUST, Blocks.REDSTONE_WIRE.defaultBlockState()
                        .setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE)
                        .setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE)
                        .setValue(RedStoneWireBlock.NORTH, RedstoneSide.NONE)
                        .setValue(RedStoneWireBlock.SOUTH, RedstoneSide.NONE), false);
        scene.world()
                .setBlock(LEVER, Blocks.LEVER.defaultBlockState()
                        .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                        .setValue(LeverBlock.FACING, Direction.EAST), false);
        scene.world()
                .setBlock(COG, AllBlocks.COGWHEEL.getDefaultState(), false);
        scene.world()
                .setBlock(DEPOT, AllBlocks.DEPOT.getDefaultState(), false);
        scene.world()
                .setBlock(ARM, AllBlocks.MECHANICAL_ARM.getDefaultState(), false);
        scene.world()
                .modifyBlockEntity(ARM, ArmBlockEntity.class, arm -> {
                    HolderLookup.Provider registries = arm.getLevel()
                            .registryAccess();
                    ListTag points = new ListTag();
                    points.add(armPoint(GRILL, "createrancher:campfire_grill", "TAKE"));
                    points.add(armPoint(DEPOT, "create:depot", "DEPOSIT"));
                    CompoundTag tag = arm.saveWithFullMetadata(registries);
                    tag.put("InteractionPoints", points);
                    tag.putString("Phase", Phase.SEARCH_INPUTS.name());
                    tag.putInt("TargetPointIndex", -1);
                    arm.readClient(tag, registries);
                });
    }

    // Makes powered redstone sparkle
    private static void sparkleRedstone(SceneBuilder scene) {
        scene.addInstruction(new TickingInstruction(false, 5000) {
            private boolean gone;

            @Override
            public void tick(PonderScene ponder) {
                super.tick(ponder);
                PonderLevel level = ponder.getWorld();
                BlockState dust = level.getBlockState(DUST);
                if (!dust.is(Blocks.REDSTONE_WIRE)) {
                    gone = true;
                    return;
                }
                if (remainingTicks % 3 == 0)
                    dust.getBlock()
                            .animateTick(dust, level, DUST, level.random);
            }

            @Override
            public boolean isComplete() {
                return gone || super.isComplete();
            }
        });
    }

    // One of the arm's targets
    private static CompoundTag armPoint(BlockPos target, String type, String mode) {
        CompoundTag point = new CompoundTag();
        point.putString("Type", type);
        point.put("Pos", NbtUtils.writeBlockPos(target.subtract(ARM)));
        point.putString("Mode", mode);
        return point;
    }

    private static void place(Timeline time, SceneBuilder scene, BlockPos grill, ItemStack stack, int count) {
        for (int i = 0; i < count; i++) {
            scene.world()
                    .modifyBlockEntity(grill, GrillBlockEntity.class, be -> be.placeFood(stack));
            time.idle(4);
        }
    }

    // Puts a grate on a block to make a grill
    private static void addGrate(SceneBuilder scene, SceneBuildingUtil util, Timeline time, BlockPos pos,
                                 BlockState grill) {
        scene.overlay()
                .showControls(util.vector()
                        .topOf(pos), Pointing.DOWN, 40)
                .rightClick()
                .withItem(ItemRegistry.ITEM_GRATE.asStack());
        time.idle(15);
        scene.world()
                .setBlock(pos, grill, false);
        hold(scene, pos);
        time.idle(10);
    }

    // A block's west face
    private static Vec3 beside(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos)
                .add(0, .5, .5);
    }

    // Stops anything on the grill finishing
    private static void hold(SceneBuilder scene, BlockPos grill) {
        scene.world()
                .modifyBlockEntity(grill, GrillBlockEntity.class, GrillBlockEntity::presentHold);
    }

    // Finishes everything on the grill
    private static void finish(SceneBuilder scene, BlockPos grill) {
        scene.world()
                .modifyBlockEntity(grill, GrillBlockEntity.class, be -> be.presentTimeLeft(3));
    }

    // Takes a finished item off the grill
    private static void takeOne(SceneBuilder scene, BlockPos grill, boolean byArm) {
        scene.world()
                .modifyBlockEntity(grill, GrillBlockEntity.class, be -> {
                    IItemHandler handler = byArm ? be.getArmItemHandler(ARM) : be.getItemHandler();
                    for (int slot = 0; slot < handler.getSlots(); slot++)
                        if (!handler.extractItem(slot, 1, false)
                                .isEmpty())
                            return;
                });
    }

    private static void clearDrops(SceneBuilder scene) {
        scene.world()
                .modifyEntities(ItemEntity.class, Entity::discard);
    }
}
