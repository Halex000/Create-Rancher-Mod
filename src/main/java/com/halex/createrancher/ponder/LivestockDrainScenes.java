package com.halex.createrancher.ponder;

import java.util.ArrayList;
import java.util.List;

import com.halex.createrancher.content.drain.LivestockDrainBlock;
import com.halex.createrancher.content.drain.LivestockDrainBlockEntity;
import com.halex.createrancher.content.drain.LivestockDrainGroup;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.FluidRegistry;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

// The Livestock Drain's Ponder scenes
public class LivestockDrainScenes {

    private static final int FADE_TICKS = 10;
    private static final int CLICK_TICKS = 40;
    private static final int STEP_TICKS = 10;

    private static final BlockPos FLOOR_DRAIN = new BlockPos(2, 0, 2);
    private static final BlockPos DRAIN = new BlockPos(2, 1, 2);
    private static final BlockPos WEST_DRAIN = new BlockPos(1, 1, 2);
    private static final BlockPos EAST_DRAIN = new BlockPos(3, 1, 2);
    private static final BlockPos PUMP = new BlockPos(2, 1, 1);
    private static final BlockPos TANK = new BlockPos(2, 1, 0);
    private static final BlockPos COG = new BlockPos(3, 1, 1);

    public static void milking(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("livestock_drain", "Collecting Fluids from Livestock");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(1.4f);
        scene.showBasePlate();
        scene.idle(10);

        Vec3 grate = util.vector()
                .topOf(FLOOR_DRAIN);
        Store store = new Store(List.of(FLOOR_DRAIN));
        FluidStack milk = new FluidStack(NeoForgeMod.MILK.get(), 2);
        FluidStack stew = new FluidStack(FluidRegistry.MUSHROOM_STEW.get()
                .getSource(), 2);
        FluidStack suspicious = new FluidStack(FluidRegistry.SUSPICIOUS_STEW.get()
                .getSource(), 2);
        suspicious.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousEffectHolder.tryGet(Items.OXEYE_DAISY)
                .getSuspiciousEffects());

        scene.overlay()
                .showText(160)
                .text("Livestock Drains collect fluid from the animals standing on them at a rate of 2mb/t per animal")
                .pointAt(grate)
                .placeNearTarget();
        scene.idle(160 + FADE_TICKS);

        PonderMobs cow = PonderMobs.spawn(scene, EntityType.COW, new Vec3(2.5, 1, 5.3), 180, false);
        cow.walk(scene, new Vec3(0, 0, -2.8), 40);
        scene.overlay()
                .showText(100)
                .attachKeyFrame()
                .text("Cows and goats give milk...")
                .pointAt(grate)
                .placeNearTarget();
        store.fill(scene, 100 + FADE_TICKS, milk);
        cow.walk(scene, new Vec3(0, 0, 2.8), 40);
        scene.world()
                .modifyEntity(cow.link(), Entity::discard);

        PonderMobs mooshroom = PonderMobs.spawn(scene, EntityType.MOOSHROOM, new Vec3(2.5, 1, 5.3), 180, false);
        mooshroom.walk(scene, new Vec3(0, 0, -2.8), 40);
        scene.overlay()
                .showText(100)
                .text("...while Mooshrooms give mushroom stew instead")
                .pointAt(grate)
                .placeNearTarget();
        store.fill(scene, 100 + FADE_TICKS, stew);
        mooshroom.walk(scene, new Vec3(0, 0, 2.8), 40);
        scene.world()
                .modifyEntity(mooshroom.link(), Entity::discard);
        scene.overlay()
                .showText(140)
                .attachKeyFrame()
                .text("Different fluids can be in a drain at once, each sharing whatever space is available")
                .pointAt(grate)
                .placeNearTarget();
        scene.idle(140 + FADE_TICKS);

        PonderMobs brown = PonderMobs.spawn(scene, EntityType.MOOSHROOM, new Vec3(2.5, 1, 5.3), 180, false);
        scene.world()
                .modifyEntity(brown.link(), entity -> ((MushroomCow) entity).setVariant(MushroomCow.MushroomType.BROWN));
        brown.walk(scene, new Vec3(0, 0, -2.8), 40);
        scene.overlay()
                .showControls(util.vector()
                        .of(2.5, 2.4, 2.5), Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .withItem(new ItemStack(Items.OXEYE_DAISY));
        scene.idle(CLICK_TICKS + FADE_TICKS);
        scene.overlay()
                .showText(190)
                .attachKeyFrame()
                .text("A brown mooshroom that has eaten a flower gives one serving of that flower's suspicious stew, then mushroom stew again")
                .pointAt(grate)
                .placeNearTarget();
        store.fill(scene, 130, suspicious);
        store.fill(scene, 60 + FADE_TICKS, stew);
        scene.overlay()
                .showText(160)
                .text("Animals from other mods work too, giving whatever fluid they would give to a bowl or a bucket")
                .pointAt(grate)
                .placeNearTarget();
        store.fill(scene, 160 + FADE_TICKS, stew);

        BlockPos above = FLOOR_DRAIN.above();
        scene.world()
                .setBlock(above, Blocks.WHITE_CARPET.defaultBlockState(), false);
        scene.world()
                .modifyEntity(brown.link(), entity -> entity.setPos(entity.position()
                        .add(0, 1 / 16d, 0)));
        scene.world()
                .showSection(util.select()
                        .position(above), Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(190)
                .attachKeyFrame()
                .text("Animals anywhere in the block above a drain can be processed, this means carpet or slabs can be placed above the drain")
                .pointAt(util.vector()
                        .topOf(above)
                        .add(0, -15 / 16d, 0))
                .placeNearTarget();
        store.fill(scene, 190 + FADE_TICKS, stew);
    }

    public static void storing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("livestock_drain_storing", "Storing and Moving Livestock Fluids");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(1.2f);
        scene.showBasePlate();
        scene.idle(10);

        FluidStack milk = new FluidStack(NeoForgeMod.MILK.get(), 1);
        Selection drain = util.select()
                .position(DRAIN);
        Store one = new Store(List.of(DRAIN));
        one.add(milk.copyWithAmount(900));
        one.write(scene);
        scene.world()
                .showSection(drain, Direction.DOWN);
        scene.idle(15);
        scene.overlay()
                .showText(120)
                .text("A Livestock Drain holds 1500mb of fluid")
                .pointAt(util.vector()
                        .blockSurface(DRAIN, Direction.WEST))
                .placeNearTarget();
        scene.idle(120 + FADE_TICKS);

        scene.world()
                .setBlock(WEST_DRAIN, alone().setValue(LivestockDrainBlock.EAST, true), false);
        scene.world()
                .setBlock(EAST_DRAIN, alone().setValue(LivestockDrainBlock.WEST, true), false);
        Store three = new Store(List.of(WEST_DRAIN, DRAIN, EAST_DRAIN));
        three.add(milk.copyWithAmount(900));
        three.write(scene);
        scene.world()
                .showSection(util.select()
                        .position(WEST_DRAIN)
                        .add(util.select()
                                .position(EAST_DRAIN)), Direction.DOWN);
        scene.world()
                .modifyBlock(DRAIN, state -> state.setValue(LivestockDrainBlock.EAST, true)
                        .setValue(LivestockDrainBlock.WEST, true), false);
        scene.idle(15);
        scene.overlay()
                .showText(170)
                .attachKeyFrame()
                .text("Drains placed side by side at the same height connect; they combine their contents and their capacity")
                .pointAt(util.vector()
                        .topOf(EAST_DRAIN))
                .placeNearTarget();
        scene.idle(170 + FADE_TICKS);

        Selection pumping = util.select()
                .position(PUMP)
                .add(util.select()
                        .position(TANK))
                .add(util.select()
                        .position(COG));
        scene.world()
                .showSection(pumping, Direction.DOWN);
        scene.idle(15);
        scene.world()
                .setKineticSpeed(util.select()
                        .position(PUMP)
                        .add(util.select()
                                .position(COG)), 32);
        scene.overlay()
                .showText(160)
                .attachKeyFrame()
                .text("Pumps and pipes on its sides or underneath can take fluid out of all of the connected drains")
                .pointAt(util.vector()
                        .blockSurface(PUMP, Direction.WEST))
                .placeNearTarget();
        for (int t = 0; t < 160; t += STEP_TICKS) {
            three.take(NeoForgeMod.MILK.get(), 40);
            three.write(scene);
            FluidStack inTank = milk.copyWithAmount(40);
            scene.world()
                    .modifyBlockEntity(TANK, FluidTankBlockEntity.class, tank -> tank.getTankInventory()
                            .fill(inTank, IFluidHandler.FluidAction.EXECUTE));
            scene.idle(STEP_TICKS);
        }
        scene.idle(FADE_TICKS);
        scene.overlay()
                .showText(120)
                .text("They can pump fluid in too")
                .pointAt(util.vector()
                        .blockSurface(PUMP, Direction.WEST))
                .placeNearTarget();
        scene.idle(120 + FADE_TICKS);
        scene.world()
                .setKineticSpeed(util.select()
                        .position(PUMP)
                        .add(util.select()
                                .position(COG)), 0);

        Vec3 top = util.vector()
                .topOf(WEST_DRAIN);
        scene.overlay()
                .showControls(top, Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .withItem(new ItemStack(Items.WATER_BUCKET));
        scene.idle(7);
        three.add(new FluidStack(Fluids.WATER, 1000));
        three.write(scene);
        scene.idle(CLICK_TICKS - 7 + FADE_TICKS);
        scene.overlay()
                .showText(150)
                .attachKeyFrame()
                .text("Players can also use buckets to manually fill or empty it")
                .pointAt(top)
                .placeNearTarget();
        scene.idle(150 + FADE_TICKS);
        scene.overlay()
                .showControls(top, Pointing.DOWN, CLICK_TICKS)
                .rightClick()
                .withItem(new ItemStack(Items.BUCKET));
        scene.idle(7);
        three.take(Fluids.WATER, 1000);
        three.write(scene);
        scene.idle(CLICK_TICKS - 7 + FADE_TICKS);
    }

    private static BlockState alone() {
        return BlockRegistry.LIVESTOCK_DRAIN.getDefaultState();
    }

    // What the drains in a scene hold
    private static class Store {

        private final List<BlockPos> drains;
        private final List<FluidStack> totals = new ArrayList<>();

        Store(List<BlockPos> drains) {
            this.drains = drains;
        }

        private int capacity() {
            return LivestockDrainGroup.CAPACITY * drains.size();
        }

        private int total() {
            int total = 0;
            for (FluidStack stack : totals)
                total += stack.getAmount();
            return total;
        }

        void add(FluidStack stack) {
            int amount = Math.min(stack.getAmount(), capacity() - total());
            if (amount <= 0)
                return;
            for (FluidStack total : totals)
                if (FluidStack.isSameFluidSameComponents(total, stack)) {
                    total.grow(amount);
                    return;
                }
            totals.add(stack.copyWithAmount(amount));
        }

        void take(Fluid fluid, int amount) {
            for (FluidStack total : totals)
                if (total.is(fluid)) {
                    total.shrink(Math.min(amount, total.getAmount()));
                    break;
                }
            totals.removeIf(FluidStack::isEmpty);
        }

        void write(SceneBuilder scene) {
            int count = drains.size();
            for (int i = 0; i < count; i++) {
                List<FluidStack> share = new ArrayList<>();
                for (FluidStack total : totals) {
                    int amount = total.getAmount() / count + (i < total.getAmount() % count ? 1 : 0);
                    if (amount > 0)
                        share.add(total.copyWithAmount(amount));
                }
                scene.world()
                        .modifyBlockEntity(drains.get(i), LivestockDrainBlockEntity.class, drain -> drain
                                .setShareForPonder(share));
            }
        }

        // Fills the drains over time
        void fill(SceneBuilder scene, int ticks, FluidStack... perTick) {
            for (int t = 0; t < ticks; t += STEP_TICKS) {
                int step = Math.min(STEP_TICKS, ticks - t);
                for (FluidStack stack : perTick)
                    add(stack.copyWithAmount(stack.getAmount() * step));
                write(scene);
                scene.idle(step);
            }
        }
    }
}
