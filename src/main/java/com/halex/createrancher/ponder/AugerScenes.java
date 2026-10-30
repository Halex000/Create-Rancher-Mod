package com.halex.createrancher.ponder;

import com.halex.createrancher.content.auger.AugerBlock;
import com.halex.createrancher.content.auger.AugerBlock.Part;
import com.halex.createrancher.content.auger.AugerBlock.Slope;
import com.halex.createrancher.content.auger.AugerBlock.Trim;
import com.halex.createrancher.content.auger.AugerBlockEntity;
import com.halex.createrancher.index.BlockRegistry;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

// The Auger's Ponder scene
public class AugerScenes {

    private static final int SPEED = 64;
    private static final int STACK = 16;
    private static final int FEED_TICKS = 15;
    private static final int CLICK_TICKS = 40;
    private static final int FADE_TICKS = 10;

    private static final BlockPos SHAFT = new BlockPos(2, 0, 1);
    private static final BlockPos BOTTOM = new BlockPos(2, 1, 1);
    private static final BlockPos TOP = new BlockPos(2, 3, 1);
    private static final BlockPos FUNNEL_IN = new BlockPos(1, 1, 1);
    private static final BlockPos FUNNEL_OUT = new BlockPos(2, 3, 0);

    private static final BlockPos CHEST = new BlockPos(0, 1, 1);
    private static final BlockPos CHEST_COLUMN_BOTTOM = new BlockPos(0, 2, 1);
    private static final BlockPos CHEST_COLUMN_SHAFT = new BlockPos(0, 4, 1);

    private static final BlockPos DIAGONAL_SHAFT = new BlockPos(3, 0, 1);
    private static final BlockPos START = new BlockPos(3, 1, 1);
    private static final BlockPos DIAGONAL = new BlockPos(2, 2, 1);
    private static final BlockPos END = new BlockPos(1, 3, 1);
    private static final BlockPos UPRIGHT = new BlockPos(1, 4, 1);
    private static final BlockPos DIAGONAL_IN = new BlockPos(3, 2, 1);
    private static final BlockPos DIAGONAL_OUT = new BlockPos(0, 4, 1);

    private static final BlockPos LYING_SHAFT = new BlockPos(0, 1, 1);
    private static final BlockPos LYING_FIRST = new BlockPos(1, 1, 1);
    private static final BlockPos LYING_LAST = new BlockPos(3, 1, 1);
    private static final BlockPos LYING_IN = new BlockPos(1, 2, 1);
    private static final BlockPos LYING_OUT = new BlockPos(3, 2, 1);

    public static void auger(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("auger", "Carrying Items with the Auger");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(.9f);
        scene.showBasePlate();
        scene.idle(10);

        Selection column = util.select()
                .fromTo(BOTTOM, TOP);
        for (int i = 0; i < 3; i++) {
            BlockPos pos = BOTTOM.above(i);
            if (i > 0)
                reshape(scene, pos.below(), Slope.NONE, i == 1 ? Part.BOTTOM : Part.MIDDLE);
            reshape(scene, pos, Slope.NONE, i == 0 ? Part.SINGLE : Part.TOP);
            scene.world()
                    .showSection(util.select()
                            .position(pos), Direction.DOWN);
            scene.idle(8);
        }
        scene.idle(7);
        scene.overlay()
                .showText(130)
                .text("Augers placed end to end join into one column")
                .pointAt(util.vector()
                        .blockSurface(TOP, Direction.WEST))
                .placeNearTarget();
        scene.idle(140);

        scene.world()
                .setKineticSpeed(column.copy()
                        .add(util.select()
                                .position(SHAFT)), SPEED);
        scene.effects()
                .rotationDirectionIndicator(BOTTOM);
        scene.overlay()
                .showText(160)
                .attachKeyFrame()
                .text("The column can be spun from either end. Like belts, Augers do not consume stress")
                .pointAt(util.vector()
                        .blockSurface(BOTTOM, Direction.WEST))
                .placeNearTarget();
        scene.idle(170);

        Selection chestColumn = util.select()
                .fromTo(CHEST, CHEST_COLUMN_SHAFT);
        Selection chestColumnTurning = util.select()
                .fromTo(CHEST_COLUMN_BOTTOM, CHEST_COLUMN_SHAFT);
        scene.world()
                .setBlock(CHEST_COLUMN_SHAFT, AllBlocks.SHAFT.getDefaultState(), false);
        scene.world()
                .modifyBlockEntity(CHEST, ChestBlockEntity.class, chest -> {
                    chest.setItem(0, new ItemStack(Items.IRON_INGOT, STACK));
                    chest.setItem(1, new ItemStack(Items.COAL, STACK));
                    chest.setItem(2, new ItemStack(Items.IRON_INGOT, STACK));
                });
        scene.world()
                .showSection(chestColumn, Direction.DOWN);
        scene.idle(15);
        scene.world()
                .setKineticSpeed(chestColumnTurning, SPEED);
        scene.effects()
                .rotationDirectionIndicator(CHEST_COLUMN_BOTTOM);
        scene.overlay()
                .showText(160)
                .attachKeyFrame()
                .text("When the end of an Auger column faces an inventory, it can pull items directly out of it...")
                .pointAt(util.vector()
                        .topOf(CHEST)
                        .add(0, -.125, -.5))
                .placeNearTarget();
        scene.idle(170);
        scene.world()
                .setKineticSpeed(chestColumnTurning, -SPEED);
        scene.effects()
                .rotationDirectionIndicator(CHEST_COLUMN_BOTTOM);
        scene.overlay()
                .showText(160)
                .text("...and push items directly into it")
                .pointAt(util.vector()
                        .topOf(CHEST)
                        .add(0, -.125, -.5))
                .placeNearTarget();
        scene.idle(170);
        scene.world()
                .hideSection(chestColumn, Direction.UP);
        scene.idle(15);
        scene.world()
                .setBlocks(chestColumn, Blocks.AIR.defaultBlockState(), false);
        scene.world()
                .setBlock(DIAGONAL_OUT, AllBlocks.ANDESITE_FUNNEL.getDefaultState()
                        .setValue(FunnelBlock.FACING, Direction.WEST)
                        .setValue(FunnelBlock.EXTRACTING, true), false);

        scene.world()
                .showSection(util.select()
                        .position(FUNNEL_IN), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(165)
                .attachKeyFrame()
                .text("Items can be input at any point along the side of the Auger")
                .pointAt(util.vector()
                        .topOf(FUNNEL_IN))
                .placeNearTarget();
        ItemStack[] stacks = {new ItemStack(Items.COPPER_BLOCK, STACK), new ItemStack(Items.WHEAT, STACK),
                new ItemStack(Items.COPPER_BLOCK, STACK), new ItemStack(Items.WHEAT, STACK)};
        for (ItemStack stack : stacks)
            feed(scene, util.vector()
                    .topOf(FUNNEL_IN), FUNNEL_IN, BOTTOM, stack);
        scene.idle(175 - stacks.length * FEED_TICKS);

        scene.overlay()
                .showText(135)
                .text("Items inside the Auger will move upwards as the screw inside spins")
                .pointAt(util.vector()
                        .blockSurface(BOTTOM.above(), Direction.WEST))
                .placeNearTarget();
        scene.idle(145);

        scene.overlay()
                .showText(135)
                .attachKeyFrame()
                .text("At the top of the column, items will collect to be taken out")
                .pointAt(util.vector()
                        .blockSurface(TOP, Direction.WEST))
                .placeNearTarget();
        scene.idle(145);

        scene.world()
                .showSection(util.select()
                        .position(FUNNEL_OUT), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("Add an output anywhere on the side of the Auger to remove items")
                .pointAt(util.vector()
                        .blockSurface(FUNNEL_OUT, Direction.NORTH))
                .placeNearTarget();
        for (int i = 0; i < 2; i++) {
            scene.idle(25);
            takeOut(scene, TOP, FUNNEL_OUT, Vec3.atLowerCornerOf(FUNNEL_OUT)
                    .add(.5, .3, .1), new Vec3(0, .1, -.08), stacks[i]);
        }
        scene.idle(150);

        scene.world()
                .modifyEntities(ItemEntity.class, Entity::discard);
        scene.world()
                .setKineticSpeed(column.copy()
                        .add(util.select()
                                .position(SHAFT)), -SPEED);
        scene.effects()
                .rotationDirectionIndicator(BOTTOM);
        scene.overlay()
                .showText(150)
                .attachKeyFrame()
                .text("Spin the Auger the other direction to move items downwards")
                .pointAt(util.vector()
                        .blockSurface(BOTTOM.above(), Direction.WEST))
                .placeNearTarget();
        scene.idle(160);

        scene.world()
                .hideSection(column.copy()
                        .add(util.select()
                                .position(FUNNEL_IN))
                        .add(util.select()
                                .position(FUNNEL_OUT)), Direction.UP);
        scene.world()
                .modifyEntities(ItemEntity.class, Entity::discard);
        scene.idle(15);
        scene.world()
                .setBlocks(column, Blocks.AIR.defaultBlockState(), false);
        scene.world()
                .setBlock(SHAFT, Blocks.SNOW_BLOCK.defaultBlockState(), false);
        scene.world()
                .setBlock(DIAGONAL_SHAFT, AllBlocks.SHAFT.getDefaultState(), false);
        scene.idle(10);

        reshape(scene, START, Slope.NONE, Part.SINGLE);
        scene.world()
                .showSection(util.select()
                        .position(START), Direction.DOWN);
        scene.idle(20);

        ItemStack augerItem = BlockRegistry.AUGER.asStack();
        scene.overlay()
                .showControls(util.vector()
                        .blockSurface(START, Direction.WEST), Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .withItem(augerItem);
        scene.idle(7);
        reshape(scene, START, Slope.START_WEST, Part.BOTTOM);
        scene.world()
                .setBlock(DIAGONAL, BlockRegistry.AUGER.getDefaultState()
                        .setValue(AugerBlock.AXIS, Direction.Axis.Y)
                        .setValue(AugerBlock.SLOPE, Slope.END_WEST)
                        .setValue(AugerBlock.PART, Part.TOP)
                        .setValue(AugerBlock.TRIM, Trim.NONE), false);
        scene.world()
                .showSection(util.select()
                        .position(DIAGONAL), Direction.DOWN);
        scene.idle(CLICK_TICKS + FADE_TICKS - 7);
        scene.overlay()
                .showText(170)
                .attachKeyFrame()
                .text("Placing an Auger against the side of another Auger extends the column diagonally")
                .pointAt(util.vector()
                        .blockSurface(DIAGONAL, Direction.WEST))
                .placeNearTarget();
        scene.idle(180);

        scene.overlay()
                .showControls(util.vector()
                        .blockSurface(DIAGONAL, Direction.WEST), Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .withItem(augerItem);
        scene.idle(7);
        reshape(scene, DIAGONAL, Slope.DIAGONAL_WEST, Part.SINGLE);
        reshape(scene, END, Slope.END_WEST, Part.TOP);
        scene.world()
                .showSection(util.select()
                        .position(END), Direction.DOWN);
        scene.idle(CLICK_TICKS + FADE_TICKS - 7);
        scene.overlay()
                .showControls(util.vector()
                        .topOf(END), Pointing.LEFT, CLICK_TICKS)
                .rightClick()
                .withItem(augerItem);
        scene.idle(7);
        reshape(scene, END, Slope.END_WEST, Part.MIDDLE);
        scene.world()
                .showSection(util.select()
                        .position(UPRIGHT), Direction.DOWN);
        scene.idle(CLICK_TICKS + FADE_TICKS - 7);
        scene.overlay()
                .showText(160)
                .text("The Auger column can be extended diagonally or turned upright again")
                .pointAt(util.vector()
                        .blockSurface(UPRIGHT, Direction.WEST))
                .placeNearTarget();
        scene.idle(170);

        Selection diagonalColumn = util.select()
                .position(START)
                .add(util.select()
                        .position(DIAGONAL))
                .add(util.select()
                        .fromTo(END, UPRIGHT));
        Selection diagonalFunnels = util.select()
                .position(DIAGONAL_IN)
                .add(util.select()
                        .position(DIAGONAL_OUT));
        scene.world()
                .showSection(diagonalFunnels, Direction.DOWN);
        scene.idle(15);
        scene.world()
                .setKineticSpeed(diagonalColumn.copy()
                        .add(util.select()
                                .position(DIAGONAL_SHAFT)), SPEED);
        scene.effects()
                .rotationDirectionIndicator(START);
        scene.overlay()
                .showText(180)
                .attachKeyFrame()
                .text("Items and rotation will move smoothly between diagonal and vertical Augers")
                .pointAt(util.vector()
                        .blockSurface(START, Direction.WEST))
                .placeNearTarget();
        ItemStack[] diagonalStacks = {new ItemStack(Items.IRON_INGOT, STACK), new ItemStack(Items.COBBLESTONE, STACK),
                new ItemStack(Items.IRON_INGOT, STACK)};
        for (ItemStack stack : diagonalStacks)
            feed(scene, util.vector()
                    .topOf(DIAGONAL_IN), DIAGONAL_IN, START, stack);
        scene.idle(190 - diagonalStacks.length * FEED_TICKS);
        for (ItemStack stack : diagonalStacks) {
            takeOut(scene, UPRIGHT, DIAGONAL_OUT, Vec3.atLowerCornerOf(DIAGONAL_OUT)
                    .add(.1, .3, .5), new Vec3(-.08, .1, 0), stack);
            scene.idle(25);
        }
        scene.idle(15);

        scene.world()
                .hideSection(diagonalColumn.copy()
                        .add(diagonalFunnels), Direction.UP);
        scene.world()
                .setBlock(DIAGONAL_SHAFT, Blocks.WHITE_CONCRETE.defaultBlockState(), false);
        scene.world()
                .modifyEntities(ItemEntity.class, Entity::discard);
        scene.idle(20);
        scene.world()
                .setBlocks(diagonalColumn.copy()
                        .add(diagonalFunnels)
                        .add(util.select()
                                .position(FUNNEL_IN)), Blocks.AIR.defaultBlockState(), false);
        scene.world()
                .setBlock(LYING_SHAFT, AllBlocks.SHAFT.getDefaultState()
                        .setValue(BlockStateProperties.AXIS, Direction.Axis.X), false);
        for (int i = 0; i < 3; i++)
            scene.world()
                    .setBlock(LYING_FIRST.east(i), BlockRegistry.AUGER.getDefaultState()
                            .setValue(AugerBlock.AXIS, Direction.Axis.X)
                            .setValue(AugerBlock.SLOPE, Slope.NONE)
                            .setValue(AugerBlock.PART, i == 0 ? Part.BOTTOM : i == 1 ? Part.MIDDLE : Part.TOP)
                            .setValue(AugerBlock.TRIM, Trim.NONE), false);
        scene.world()
                .setBlock(LYING_IN, funnel(false), false);
        scene.world()
                .setBlock(LYING_OUT, funnel(true), false);
        scene.world()
                .showSection(util.select()
                        .position(LYING_SHAFT), Direction.DOWN);
        scene.idle(10);
        for (int i = 0; i < 3; i++) {
            BlockPos pos = LYING_FIRST.east(i);
            if (i > 0)
                reshape(scene, pos.west(), Slope.NONE, i == 1 ? Part.BOTTOM : Part.MIDDLE);
            reshape(scene, pos, Slope.NONE, i == 0 ? Part.SINGLE : Part.TOP);
            scene.world()
                    .showSection(util.select()
                            .position(pos), Direction.DOWN);
            scene.idle(8);
        }
        scene.idle(7);
        scene.overlay()
                .showText(130)
                .attachKeyFrame()
                .text("Augers can also be placed horizontally")
                .pointAt(util.vector()
                        .blockSurface(LYING_FIRST, Direction.WEST))
                .placeNearTarget();
        scene.idle(140);

        Selection line = util.select()
                .fromTo(LYING_FIRST, LYING_LAST);
        scene.world()
                .showSection(util.select()
                        .position(LYING_IN)
                        .add(util.select()
                                .position(LYING_OUT)), Direction.DOWN);
        scene.idle(15);
        scene.world()
                .setKineticSpeed(line.copy()
                        .add(util.select()
                                .position(LYING_SHAFT)), SPEED);
        scene.effects()
                .rotationDirectionIndicator(LYING_FIRST);
        scene.overlay()
                .showText(170)
                .text("Like the vertical Augers, the direction items move in is controlled by the rotation direction")
                .pointAt(util.vector()
                        .topOf(LYING_IN))
                .placeNearTarget();
        ItemStack[] lyingStacks = {new ItemStack(Items.WHEAT, STACK), new ItemStack(Items.COPPER_BLOCK, STACK),
                new ItemStack(Items.WHEAT, STACK)};
        for (ItemStack stack : lyingStacks)
            feed(scene, util.vector()
                    .topOf(LYING_IN), LYING_IN, LYING_FIRST, stack);
        scene.idle(180 - lyingStacks.length * FEED_TICKS);
        for (ItemStack stack : lyingStacks) {
            takeOut(scene, LYING_LAST, LYING_OUT, util.vector()
                    .topOf(LYING_OUT), new Vec3(0, .25, -.05), stack);
            scene.idle(25);
        }
        scene.idle(20);
    }

    // An andesite funnel
    private static BlockState funnel(boolean extracting) {
        return AllBlocks.ANDESITE_FUNNEL.getDefaultState()
                .setValue(FunnelBlock.FACING, Direction.UP)
                .setValue(FunnelBlock.EXTRACTING, extracting);
    }

    // Sets an auger's shape
    private static void reshape(CreateSceneBuilder scene, BlockPos pos, Slope slope, Part part) {
        scene.world()
                .modifyBlock(pos, state -> state.setValue(AugerBlock.SLOPE, slope)
                        .setValue(AugerBlock.PART, part), false);
    }

    // Drops a stack into an auger through a funnel
    private static void feed(CreateSceneBuilder scene, Vec3 above, BlockPos funnel, BlockPos auger, ItemStack stack) {
        ElementLink<EntityElement> dropped = scene.world()
                .createItemEntity(above.add(0, .6, 0), new Vec3(0, -.1, 0), stack.copy());
        scene.idle(8);
        scene.world()
                .modifyEntity(dropped, Entity::discard);
        scene.world()
                .flapFunnel(funnel, false);
        scene.world()
                .modifyBlockEntity(auger, AugerBlockEntity.class, be -> be.getItemHandler()
                        .insertItem(0, stack.copy(), false));
        scene.idle(FEED_TICKS - 8);
    }

    // Takes a stack out of an auger through a funnel
    private static void takeOut(CreateSceneBuilder scene, BlockPos auger, BlockPos funnel, Vec3 from, Vec3 motion,
                                ItemStack stack) {
        scene.world()
                .modifyBlockEntity(auger, AugerBlockEntity.class, be -> {
                    IItemHandler handler = be.getItemHandler();
                    if (handler.extractItem(1, STACK, false)
                            .isEmpty())
                        handler.extractItem(0, STACK, false);
                });
        scene.world()
                .flapFunnel(funnel, true);
        scene.world()
                .createItemEntity(from, motion, stack.copy());
    }
}
