package com.halex.createrancher.ponder;

import com.halex.createrancher.content.depot.SmartDepotBlock;
import com.halex.createrancher.content.depot.SmartDepotBlockEntity;
import com.halex.createrancher.index.BlockRegistry;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingBehaviour.Mode;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

// The Smart Depot's Ponder scenes
public class SmartDepotScenes {

    private static final int FADE_TICKS = 10;
    private static final int CLICK_TICKS = 40;

    private static final BlockPos CHEST = new BlockPos(2, 1, 2);
    private static final BlockPos DEPOT = new BlockPos(2, 2, 2);
    private static final BlockPos PRESS = new BlockPos(2, 4, 2);
    private static final BlockPos FAN = new BlockPos(2, 3, 4);
    private static final BlockPos CAMPFIRE = new BlockPos(2, 3, 3);

    public static void usingTheSmartDepot(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("smart_depot", "Using the Smart Depot");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        Selection chest = util.select()
                .position(CHEST);
        Selection depot = util.select()
                .position(DEPOT);
        Vec3 top = util.vector()
                .topOf(DEPOT)
                .add(0, -3 / 16f, 0);
        Vec3 side = util.vector()
                .blockSurface(DEPOT, Direction.WEST);
        Vec3 filter = util.vector()
                .of(2.5, 2 + 3 / 16f, 2 + .45 / 16f);

        scene.world()
                .showSection(chest, Direction.DOWN);
        scene.idle(10);
        scene.world()
                .showSection(depot, Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(120)
                .text("The Smart Depot works like a Brass Funnel and a Depot in one block")
                .pointAt(side)
                .placeNearTarget();
        scene.idle(120 + FADE_TICKS);

        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("Placed above an inventory, it pulls items out of it, like Brass Funnel facing up would...")
                .pointAt(util.vector()
                        .blockSurface(CHEST, Direction.WEST))
                .placeNearTarget();
        scene.idle(30);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.DOWN, new ItemStack(Items.IRON_INGOT, 64));
        scene.idle(110 + FADE_TICKS);
        scene.overlay()
                .showText(140)
                .text("...but instead of throwing the item out as an entity, it puts them on top of itself")
                .pointAt(top)
                .placeNearTarget();
        scene.idle(140 + FADE_TICKS);

        scene.overlay()
                .showControls(top, Pointing.DOWN, CLICK_TICKS)
                .rightClick();
        scene.idle(7);
        scene.world()
                .removeItemsFromBelt(DEPOT);
        scene.idle(CLICK_TICKS - 7 + FADE_TICKS);
        scene.overlay()
                .showText(140)
                .text("Items wait on top until they're taken, by hand or by machines, and then it pulls up the next items")
                .pointAt(top)
                .placeNearTarget();
        scene.idle(20);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.DOWN, new ItemStack(Items.IRON_INGOT, 64));
        scene.idle(120 + FADE_TICKS);

        scene.world()
                .removeItemsFromBelt(DEPOT);
        scene.overlay()
                .showFilterSlotInput(filter, Direction.NORTH, 100);
        scene.world()
                .modifyBlockEntityNBT(depot, SmartDepotBlockEntity.class, nbt -> {
                    nbt.putInt("FilterAmount", 16);
                    nbt.putBoolean("UpTo", false);
                });
        scene.idle(CLICK_TICKS - 10 + FADE_TICKS);
        scene.overlay()
                .showText(160)
                .attachKeyFrame()
                .text("Like a Brass Funnel, the value panel on its sides sets how many items it takes at a time")
                .pointAt(filter)
                .placeNearTarget();
        scene.idle(40);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.DOWN, new ItemStack(Items.IRON_INGOT, 16));
        scene.idle(120 + FADE_TICKS);

        scene.world()
                .removeItemsFromBelt(DEPOT);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        scene.overlay()
                .showFilterSlotInput(filter, Direction.NORTH, 80);
        scene.overlay()
                .showControls(filter, Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .withItem(gold);
        scene.idle(10);
        scene.world()
                .setFilterData(depot, SmartDepotBlockEntity.class, gold);
        scene.idle(CLICK_TICKS - 10 + FADE_TICKS);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("Using an item on the filter slot makes it take only matching items")
                .pointAt(filter)
                .placeNearTarget();
        scene.idle(40);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.DOWN, new ItemStack(Items.GOLD_INGOT, 16));
        scene.idle(100 + FADE_TICKS);

        scene.world()
                .hideSection(depot, Direction.UP);
        scene.idle(15);
        scene.world()
                .removeItemsFromBelt(DEPOT);
        scene.world()
                .modifyBlock(DEPOT, state -> state.setValue(SmartDepotBlock.EXTRACTING, false), false);
        scene.world()
                .modifyBlockEntityNBT(depot, SmartDepotBlockEntity.class, nbt -> nbt.remove("Filter"));
        scene.overlay()
                .showControls(util.vector()
                        .topOf(CHEST), Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .whileSneaking()
                .withItem(BlockRegistry.SMART_DEPOT.asStack());
        scene.idle(7);
        scene.world()
                .showSection(depot, Direction.DOWN);
        scene.idle(CLICK_TICKS - 7 + FADE_TICKS);
        scene.overlay()
                .showText(160)
                .attachKeyFrame()
                .text("Placed while sneaking, it works the other way: whatever is set on its top goes into the inventory below")
                .pointAt(side)
                .placeNearTarget();
        scene.idle(160 + FADE_TICKS);
        ItemStack copper = new ItemStack(Items.COPPER_INGOT, 16);
        scene.overlay()
                .showControls(top, Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .withItem(copper);
        scene.idle(7);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.NORTH, copper);
        scene.idle(12);
        scene.world()
                .removeItemsFromBelt(DEPOT);
        scene.effects()
                .indicateSuccess(CHEST);
        scene.idle(CLICK_TICKS - 19 + FADE_TICKS + 10);

        scene.overlay()
                .showFilterSlotInput(filter, Direction.NORTH, 80);
        scene.overlay()
                .showControls(filter, Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .withItem(copper);
        scene.idle(10);
        scene.world()
                .setFilterData(depot, SmartDepotBlockEntity.class, new ItemStack(Items.COPPER_INGOT));
        scene.idle(CLICK_TICKS - 10 + FADE_TICKS);
        scene.overlay()
                .showText(160)
                .attachKeyFrame()
                .text("Its filter then decides which items go into the inventory; anything else stays on top")
                .pointAt(filter)
                .placeNearTarget();
        scene.idle(40);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.NORTH, new ItemStack(Items.COBBLESTONE, 16));
        scene.idle(120 + FADE_TICKS);

        scene.world()
                .removeItemsFromBelt(DEPOT);
        scene.overlay()
                .showControls(side, Pointing.LEFT, CLICK_TICKS)
                .rightClick()
                .withItem(AllItems.WRENCH.asStack());
        scene.idle(7);
        scene.world()
                .modifyBlock(DEPOT, state -> state.cycle(SmartDepotBlock.EXTRACTING), false);
        scene.idle(CLICK_TICKS - 7 + FADE_TICKS);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("A Wrench switches it between pulling items up and putting them in, the arrow on its side shows which mode it is in")
                .pointAt(side)
                .placeNearTarget();
        scene.idle(140 + FADE_TICKS);
        scene.markAsFinished();
    }

    public static void processing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("smart_depot_processing", "Processing on the Smart Depot");
        scene.configureBasePlate(0, 0, 5);
        scene.setSceneOffsetY(-1);
        scene.scaleSceneView(.9f);
        scene.showBasePlate();
        scene.idle(10);

        Selection chest = util.select()
                .position(CHEST);
        Selection depot = util.select()
                .position(DEPOT);
        Selection press = util.select()
                .fromTo(PRESS, PRESS.south(2));
        Selection fan = util.select()
                .fromTo(FAN, CAMPFIRE);
        Vec3 top = util.vector()
                .topOf(DEPOT)
                .add(0, -3 / 16f, 0);
        Vec3 side = util.vector()
                .blockSurface(DEPOT, Direction.WEST);

        scene.world()
                .showSection(chest, Direction.DOWN);
        scene.idle(10);
        scene.world()
                .showSection(depot, Direction.DOWN);
        scene.idle(10);
        scene.world()
                .setKineticSpeed(press, -64);
        scene.world()
                .showSection(press, Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("Like a Regular Depot, machines can process the items on its top")
                .pointAt(util.vector()
                        .blockSurface(PRESS, Direction.WEST))
                .placeNearTarget();
        scene.idle(140 + FADE_TICKS);

        scene.overlay()
                .showText(160)
                .attachKeyFrame()
                .text("The Smart Depot waits while a machine processes the item on its top...")
                .pointAt(side)
                .placeNearTarget();
        ItemStack ingot = new ItemStack(Items.IRON_INGOT);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.NORTH, ingot);
        scene.idle(20);
        scene.world()
                .modifyBlockEntity(PRESS, MechanicalPressBlockEntity.class, entity -> entity.getPressingBehaviour()
                        .start(Mode.BELT));
        scene.idle(15);
        scene.world()
                .modifyBlockEntity(PRESS, MechanicalPressBlockEntity.class, entity -> entity.getPressingBehaviour()
                        .makePressingParticleEffect(util.vector()
                                .centerOf(DEPOT)
                                .add(0, 8 / 16f, 0), ingot));
        scene.world()
                .removeItemsFromBelt(DEPOT);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.UP, AllItems.IRON_SHEET.asStack());
        scene.idle(SmartDepotBlockEntity.SETTLE_TICKS);
        scene.world()
                .removeItemsFromBelt(DEPOT);
        scene.effects()
                .indicateSuccess(CHEST);
        scene.idle(160 + FADE_TICKS - 35 - SmartDepotBlockEntity.SETTLE_TICKS);
        scene.overlay()
                .showText(150)
                .text("...and deposits the resulting crafted item into the inventory below as soon as it's made, unless the machine will work on it again")
                .pointAt(util.vector()
                        .blockSurface(CHEST, Direction.WEST))
                .placeNearTarget();
        scene.idle(150 + FADE_TICKS);

        scene.overlay()
                .showText(120)
                .attachKeyFrame()
                .text("Anything no machine can work on goes straight in")
                .pointAt(side)
                .placeNearTarget();
        scene.idle(20);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.NORTH, new ItemStack(Items.COBBLESTONE));
        scene.idle(SmartDepotBlockEntity.SETTLE_TICKS + 5);
        scene.world()
                .removeItemsFromBelt(DEPOT);
        scene.effects()
                .indicateSuccess(CHEST);
        scene.idle(120 + FADE_TICKS - 20 - SmartDepotBlockEntity.SETTLE_TICKS - 5);

        scene.world()
                .hideSection(press, Direction.UP);
        scene.idle(15);
        scene.world()
                .setKineticSpeed(fan, -64);
        scene.world()
                .showSection(fan, Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(160)
                .attachKeyFrame()
                .text("The same goes for any other machines that can process items on the top of a Depot")
                .pointAt(util.vector()
                        .blockSurface(CAMPFIRE, Direction.WEST))
                .placeNearTarget();
        ItemStack beef = new ItemStack(Items.BEEF);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.NORTH, beef);
        scene.idle(80);
        scene.world()
                .removeItemsFromBelt(DEPOT);
        scene.world()
                .createItemOnBeltLike(DEPOT, Direction.UP, new ItemStack(Items.COOKED_BEEF));
        scene.idle(SmartDepotBlockEntity.SETTLE_TICKS);
        scene.world()
                .removeItemsFromBelt(DEPOT);
        scene.effects()
                .indicateSuccess(CHEST);
        scene.idle(160 + FADE_TICKS - 80 - SmartDepotBlockEntity.SETTLE_TICKS);
        scene.markAsFinished();
    }
}
