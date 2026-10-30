package com.halex.createrancher.content.grill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

import javax.annotation.Nullable;

import com.halex.createrancher.index.ParticleRegistry;
import com.halex.createrancher.mixin.ArmBlockEntityAccessor;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

// Holds and processes the items on a grill
public class GrillBlockEntity extends SmartBlockEntity implements Clearable {

    public static final int SLOTS = 9;
    private static final int PULL_MEMORY = 40;
    private static final int POP_DELAY = 10;
    private static final int GATHER_TIME = 10;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final int[] progress = new int[SLOTS];
    private final int[] cookTime = new int[SLOTS];
    private final boolean[] done = new boolean[SLOTS];
    private final long[] doneAt = new long[SLOTS];
    private final Direction[] exits = new Direction[SLOTS];
    private final GrillProcess[] placedFor = new GrillProcess[SLOTS];
    private final Direction[] slideEdge = new Direction[SLOTS];
    private final boolean[] slidingOff = new boolean[SLOTS];
    private final float[] slide = new float[SLOTS];
    private final float[] prevSlide = new float[SLOTS];
    private final int[] slideDelay = new int[SLOTS];
    private final List<List<ItemStack>> queued = new ArrayList<>();
    private long lastPulled = Long.MIN_VALUE / 2;
    private long clock;
    private boolean presentHeld;
    private boolean processingNow;
    private boolean pulledFromBeside;
    private final Set<BlockPos> pullingArms = new HashSet<>();
    private final IItemHandler itemHandler = new GrillItemHandler(true, null);
    private final IItemHandler armItemHandler = new GrillItemHandler(false, null);

    public GrillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int slot = 0; slot < SLOTS; slot++)
            queued.add(new ArrayList<>());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new DirectBeltInputBehaviour(this).setInsertionHandler(this::insertFromBelt));
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    // The inventory as something further away sees it
    public IItemHandler getArmItemHandler() {
        return armItemHandler;
    }

    // The inventory as a mechanical arm sees it
    public IItemHandler getArmItemHandler(BlockPos arm) {
        return new GrillItemHandler(false, arm.immutable());
    }

    // Empties it without dropping anything
    @Override
    public void clearContent() {
        Collections.fill(items, ItemStack.EMPTY);
    }

    // Everything the grill drops when broken
    public List<ItemStack> getContents() {
        List<ItemStack> contents = new ArrayList<>(items);
        for (List<ItemStack> waiting : queued)
            contents.addAll(waiting);
        return contents;
    }

    // A neighbour changed
    public void onNeighbourChanged() {
        if (!pulledFromBeside)
            return;
        pulledFromBeside = false;
        setChanged();
    }

    private boolean isSomethingPulling(long now) {
        return pulledFromBeside || now - lastPulled <= PULL_MEMORY || isArmPulling();
    }

    // Whether an arm is still taking items from the grill
    private boolean isArmPulling() {
        if (pullingArms.isEmpty() || level == null)
            return false;
        boolean pulling = false;
        for (Iterator<BlockPos> arms = pullingArms.iterator(); arms.hasNext(); ) {
            BlockPos pos = arms.next();
            if (!level.isLoaded(pos)) {
                pulling = true;
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof ArmBlockEntity arm)) {
                arms.remove();
                setChanged();
                continue;
            }
            ArmBlockEntityAccessor access = (ArmBlockEntityAccessor) arm;
            if (access.createrancher$isUpdatingInteractionPoints()) {
                pulling = true;
                continue;
            }
            if (access.createrancher$getInputs()
                    .stream()
                    .noneMatch(point -> point.getPos()
                            .equals(worldPosition))) {
                arms.remove();
                setChanged();
                continue;
            }
            pulling |= arm.getSpeed() != 0 && !access.createrancher$isRedstoneLocked()
                    && access.createrancher$getOutputs()
                    .stream()
                    .anyMatch(ArmInteractionPoint::isValid);
        }
        return pulling;
    }

    @Nullable
    private GrillBlock getGrill() {
        return getBlockState().getBlock() instanceof GrillBlock grill ? grill : null;
    }

    @Nullable
    private GrillProcess getProcess() {
        GrillBlock grill = getGrill();
        return grill == null ? null : grill.getProcess(getBlockState());
    }

    public boolean canCook(ItemStack stack) {
        GrillProcess process = getProcess();
        return level != null && !stack.isEmpty() && process != null && process.canProcess(stack, level);
    }

    public boolean hasFreeSlot() {
        return freeSlot() != -1;
    }

    private int freeSlots() {
        int free = 0;
        for (int slot = 0; slot < SLOTS; slot++)
            if (items.get(slot)
                    .isEmpty())
                free++;
        return free;
    }

    private int freeSlot() {
        for (int slot = 0; slot < SLOTS; slot++)
            if (items.get(slot)
                    .isEmpty())
                return slot;
        return -1;
    }

    // Puts one item on the grill if it fits. Doesn't change the stack.
    public boolean placeFood(ItemStack stack) {
        int slot = freeSlot();
        if (slot == -1 || !canCook(stack))
            return false;
        placeAt(slot, stack);
        return true;
    }

    private void placeAt(int slot, ItemStack stack) {
        placeAt(slot, stack, null, 0);
    }

    private void placeAt(int slot, ItemStack stack, @Nullable Direction exit, int slideDelay) {
        GrillProcess process = getProcess();
        items.set(slot, stack.copyWithCount(1));
        progress[slot] = 0;
        cookTime[slot] = process == null || level == null ? 100 : process.getTime(stack, level);
        done[slot] = false;
        exits[slot] = exit;
        placedFor[slot] = process;
        if (exit != null)
            startSlide(slot, exit.getOpposite(), false, slideDelay);
        sync();
    }

    // Picks up a dropped stack
    public void insertDropped(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (!entity.isAlive() || !canCook(stack))
            return;
        boolean placed = false;
        for (int slot = freeSlot(); slot != -1 && !stack.isEmpty(); slot = freeSlot()) {
            placeAt(slot, stack);
            stack = stack.copyWithCount(stack.getCount() - 1);
            placed = true;
        }
        if (!placed)
            return;
        if (stack.isEmpty())
            entity.discard();
        else
            entity.setItem(stack);
    }

    // Takes an item from a belt or passes it across
    private ItemStack insertFromBelt(TransportedItemStack transported, Direction side, boolean simulate) {
        ItemStack stack = transported.stack;
        Direction exit = side.getAxis()
                .isHorizontal() ? side : null;
        int slot = freeSlot();
        if (slot == -1)
            return stack;
        if (canCook(stack)) {
            int taking = exit != null && isGrill(exit.getOpposite()) ? Math.min(stack.getCount(), freeSlots()) : 1;
            if (!simulate)
                for (int i = 0; i < taking; i++)
                    placeAt(freeSlot(), stack, exit, i);
            return stack.copyWithCount(stack.getCount() - taking);
        }
        DirectBeltInputBehaviour next = exit == null ? null : getOutput(exit);
        if (next == null || next.handleInsertion(stack.copy(), exit, true)
                .getCount() == stack.getCount())
            return stack;
        if (!simulate) {
            items.set(slot, stack.copy());
            progress[slot] = 0;
            cookTime[slot] = 0;
            done[slot] = true;
            doneAt[slot] = clock;
            exits[slot] = exit;
            placedFor[slot] = null;
            startSlide(slot, exit.getOpposite(), false, 0);
            sync();
        }
        return ItemStack.EMPTY;
    }

    // Whether the block on that side is a grill
    private boolean isGrill(Direction side) {
        return level != null && level.getBlockEntity(worldPosition.relative(side)) instanceof GrillBlockEntity;
    }

    private void startSlide(int slot, Direction edge, boolean off, int delay) {
        slideEdge[slot] = edge;
        slidingOff[slot] = off;
        slide[slot] = prevSlide[slot] = 0;
        slideDelay[slot] = delay;
    }

    private void stopSlide(int slot) {
        slideEdge[slot] = null;
        slidingOff[slot] = false;
        slide[slot] = prevSlide[slot] = 0;
        slideDelay[slot] = 0;
    }

    private boolean isSlidingOffTowards(Direction edge) {
        for (int slot = 0; slot < SLOTS; slot++)
            if (slidingOff[slot] && slideEdge[slot] == edge)
                return true;
        return false;
    }

    private boolean isSlidingOn(int slot) {
        return slideEdge[slot] != null && !slidingOff[slot];
    }

    // How far an item has slid
    private void tickSlides() {
        for (int slot = 0; slot < SLOTS; slot++) {
            prevSlide[slot] = slide[slot];
            if (slideEdge[slot] == null || slide[slot] >= 1)
                continue;
            if (slideDelay[slot] > 0) {
                slideDelay[slot]--;
                continue;
            }
            slide[slot] += (1 - slide[slot]) / 4;
            if (1 - slide[slot] < 1 / 16f)
                slide[slot] = 1;
        }
    }

    // Where to draw an item
    public Vec3 getRenderPosition(int slot, float partialTicks) {
        Vec3 onGrate = getSlotPosition(slot);
        Direction edge = slideEdge[slot];
        if (edge == null)
            return onGrate;
        Vec3 atEdge = getCentre().add(Vec3.atLowerCornerOf(edge.getNormal())
                .scale(.5));
        float along = Math.min(1, prevSlide[slot] + (slide[slot] - prevSlide[slot]) * partialTicks);
        return slidingOff[slot] ? onGrate.lerp(atEdge, along) : atEdge.lerp(onGrate, along);
    }

    // What takes items from belts on that side
    @Nullable
    private DirectBeltInputBehaviour getOutput(Direction side) {
        DirectBeltInputBehaviour next = BlockEntityBehaviour.get(level, worldPosition.relative(side),
                DirectBeltInputBehaviour.TYPE);
        return next != null && next.canInsertFromSide(side) ? next : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null)
            return;
        tickSlides();
        if (!level.isClientSide || isVirtual())
            serverTick();
        if (level.isClientSide)
            clientTick();
    }

    // Holds off finishing in Ponder scenes
    public void presentHold() {
        presentHeld = true;
    }

    // Speeds up processing in Ponder scenes
    public void presentTimeLeft(int ticks) {
        presentTimeLeft(ticks, 0);
    }

    // Speeds up processing in Ponder scenes with items finishing one after another
    public void presentTimeLeft(int ticks, int apart) {
        presentHeld = false;
        int left = ticks;
        for (int slot = 0; slot < SLOTS; slot++)
            if (!items.get(slot)
                    .isEmpty() && !done[slot]) {
                progress[slot] = Math.max(progress[slot], cookTime[slot] - ticks) - (left - ticks);
                left += apart;
            }
    }

    private void serverTick() {
        GrillBlock grill = getGrill();
        Level level = this.level;
        if (grill == null || level == null)
            return;
        BlockState state = getBlockState();
        GrillProcess process = grill.getProcess(state);
        boolean processing = process != null && !presentHeld && grill.isProcessing(level, worldPosition, state);
        processingNow = processing;
        long now = ++clock;
        boolean pulling = isSomethingPulling(now);
        boolean changed = false;
        boolean popped = false;

        for (int slot = 0; slot < SLOTS; slot++)
            if (isSlidingOn(slot) && slide[slot] >= 1) {
                stopSlide(slot);
                changed = true;
            }
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty())
                continue;
            if (!done[slot]) {
                if (process != placedFor[slot]) {
                    if (process == null || !process.canProcess(stack, level)) {
                        done[slot] = true;
                        doneAt[slot] = now;
                        changed = true;
                        continue;
                    }
                    placedFor[slot] = process;
                    cookTime[slot] = process.getTime(stack, level);
                }
                if (processing && ++progress[slot] >= cookTime[slot]) {
                    finish(slot, process, now);
                    changed = true;
                }
                continue;
            }
            DirectBeltInputBehaviour next = exits[slot] == null ? null : getOutput(exits[slot]);
            if (next != null) {
                if (isSlidingOn(slot))
                    continue;
                if (!slidingOff[slot]) {
                    if (!isSlidingOffTowards(exits[slot]) && !isGathering(slot, now) && startSlidingOff(slot, next,
                            now))
                        changed = true;
                    continue;
                }
                if (!hasArrived(exits[slot]))
                    continue;
                handOff(exits[slot], next, now);
                changed = true;
                continue;
            }
            if (slidingOff[slot]) {
                stopSlide(slot);
                changed = true;
            }
            if (!pulling && !popped && now - doneAt[slot] >= POP_DELAY) {
                popOff(stack, getSlotPosition(slot));
                nextProduct(slot, now);
                popped = true;
                changed = true;
            }
        }
        if (changed)
            sync();
    }

    private void finish(int slot, GrillProcess process, long now) {
        Level level = this.level;
        if (level == null)
            return;
        ItemStack input = items.get(slot);
        List<ItemStack> results = process.canProcess(input, level) ? process.process(input, level) : null;
        if (results == null) {
            done[slot] = true;
            doneAt[slot] = now;
            return;
        }
        placeResults(slot, results, now);
    }

    // Swaps a finished item for what it made
    private void placeResults(int slot, List<ItemStack> products, long now) {
        List<ItemStack> results = products.stream()
                .filter(result -> !result.isEmpty())
                .toList();
        if (results.isEmpty()) {
            clearSlot(slot);
            return;
        }
        items.set(slot, results.get(0)
                .copy());
        List<ItemStack> waiting = queued.get(slot);
        waiting.clear();
        for (int i = 1; i < results.size(); i++)
            waiting.add(results.get(i)
                    .copy());
        done[slot] = true;
        doneAt[slot] = now;
    }

    // Shows the next product on a spot
    private void nextProduct(int slot, long now) {
        List<ItemStack> waiting = queued.get(slot);
        if (waiting.isEmpty()) {
            clearSlot(slot);
            return;
        }
        items.set(slot, waiting.remove(0));
        doneAt[slot] = now;
        stopSlide(slot);
    }

    // Whether a finished item is waiting for others to finish
    private boolean isGathering(int slot, long now) {
        if (!done[slot] || placedFor[slot] == null || !processingNow)
            return false;
        long waitUntil = doneAt[slot] + GATHER_TIME;
        if (now >= waitUntil)
            return false;
        for (int other = 0; other < SLOTS; other++)
            if (!items.get(other)
                    .isEmpty() && !done[other] && now + cookTime[other] - progress[other] <= waitUntil)
                return true;
        return false;
    }

    // Finished spots that can come off with this one
    private List<Integer> stackingWith(int slot, long now, IntPredicate also) {
        List<Integer> together = new ArrayList<>();
        together.add(slot);
        ItemStack first = items.get(slot);
        int count = first.getCount();
        for (int other = 0; other < SLOTS; other++) {
            ItemStack stack = items.get(other);
            if (other == slot || stack.isEmpty() || !done[other] || slideEdge[other] != null || isGathering(other, now)
                    || !ItemStack.isSameItemSameComponents(first, stack) || count + stack.getCount() > first
                    .getMaxStackSize() || !also.test(other))
                continue;
            together.add(other);
            count += stack.getCount();
        }
        return together;
    }

    // The items on these spots as one stack
    private ItemStack combine(List<Integer> slots) {
        ItemStack together = items.get(slots.get(0))
                .copy();
        for (int i = 1; i < slots.size(); i++)
            together.grow(items.get(slots.get(i))
                    .getCount());
        return together;
    }

    // Takes items off these spots
    private void takeFrom(List<Integer> slots, int count, long now) {
        int left = count;
        for (int slot : slots) {
            ItemStack stack = items.get(slot);
            int taking = Math.min(left, stack.getCount());
            left -= taking;
            if (taking == stack.getCount())
                nextProduct(slot, now);
            else {
                items.set(slot, stack.copyWithCount(stack.getCount() - taking));
                stopSlide(slot);
            }
        }
    }

    // Starts finished items sliding onto the next belt. Returns whether any did.
    private boolean startSlidingOff(int slot, DirectBeltInputBehaviour next, long now) {
        Direction exit = exits[slot];
        List<Integer> together = stackingWith(slot, now, other -> exits[other] == exit);
        ItemStack stack = combine(together);
        int fits = stack.getCount() - next.handleInsertion(stack.copy(), exit, true)
                .getCount();
        if (fits <= 0)
            return false;
        int sliding = 0;
        int delay = 0;
        for (int other : together) {
            int count = items.get(other)
                    .getCount();
            if (sliding > 0 && sliding + count > fits)
                break;
            startSlide(other, exit, true, delay++);
            sliding += count;
        }
        return true;
    }

    // Whether everything sliding off that side has arrived
    private boolean hasArrived(Direction exit) {
        for (int slot = 0; slot < SLOTS; slot++)
            if (slidingOff[slot] && slideEdge[slot] == exit && slide[slot] < 1)
                return false;
        return true;
    }

    // Puts the items that slid off onto the belt
    private void handOff(Direction exit, DirectBeltInputBehaviour next, long now) {
        List<Integer> arrived = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++)
            if (slidingOff[slot] && slideEdge[slot] == exit && slide[slot] >= 1)
                arrived.add(slot);
        ItemStack stack = combine(arrived);
        ItemStack left = next.handleInsertion(stack.copy(), exit, false);
        takeFrom(arrived, stack.getCount() - left.getCount(), now);
    }

    private Vec3 getCentre() {
        GrillBlock grill = getGrill();
        return Vec3.atBottomCenterOf(worldPosition)
                .add(0, grill == null ? 1 : grill.getGrateTop(), 0);
    }

    // Where an item sits on the grill
    public Vec3 getSlotPosition(int slot) {
        GrillBlock grill = getGrill();
        if (grill == null)
            return getCentre();
        float spacing = grill.getSlotSpacing();
        float right = (slot % 3 - 1) * spacing;
        float forward = (slot / 3 - 1) * spacing;
        Direction facing = grill.getFacing(getBlockState());
        Direction side = facing.getClockWise();
        return getCentre().add(side.getStepX() * right + facing.getStepX() * forward, 0,
                side.getStepZ() * right + facing.getStepZ() * forward);
    }

    // Pops a finished item off the grill
    private void popOff(ItemStack stack, Vec3 from) {
        if (level == null || stack.isEmpty())
            return;
        RandomSource random = level.random;
        double spread = .11485000171139836;
        ItemEntity item = new ItemEntity(level, from.x, from.y + 1 / 16f, from.z, stack.copy(),
                random.triangle(0, spread), random.triangle(.2, spread), random.triangle(0, spread));
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    private void clearSlot(int slot) {
        items.set(slot, ItemStack.EMPTY);
        queued.get(slot)
                .clear();
        progress[slot] = 0;
        cookTime[slot] = 0;
        done[slot] = false;
        exits[slot] = null;
        placedFor[slot] = null;
        stopSlide(slot);
    }

    private void clientTick() {
        GrillBlock grill = getGrill();
        BlockState state = getBlockState();
        GrillProcess process = grill == null ? null : grill.getProcess(state);
        Level level = this.level;
        if (process == null || level == null || !grill.isProcessing(level, worldPosition, state))
            return;
        RandomSource random = level.random;

        for (int slot = 0; slot < SLOTS; slot++) {
            if (items.get(slot)
                    .isEmpty() || done[slot])
                continue;
            Vec3 at = getSlotPosition(slot);
            if (process == GrillProcess.WASHING) {
                if (random.nextInt(8) == 0)
                    level.addParticle(ParticleRegistry.GRILL_SPIT.get(), at.x + (random.nextFloat() - .5f) * .15f,
                            at.y + .02, at.z + (random.nextFloat() - .5f) * .15f, 0, .06, 0);
                continue;
            }
            if (random.nextFloat() >= .2f)
                continue;
            for (int i = 0; i < 3; i++)
                level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0, 5.0E-4, 0);
            if (process == GrillProcess.HAUNTING && random.nextInt(3) == 0)
                level.addParticle(ParticleTypes.SOUL, at.x, at.y, at.z, 0, .02, 0);
        }

        if (process == GrillProcess.WASHING) {
            double surface = worldPosition.getY() + CauldronGrillBlock.getContentHeight(state);
            if (random.nextFloat() < .5f)
                level.addParticle(ParticleTypes.BUBBLE_POP, inside(worldPosition.getX(), random), surface,
                        inside(worldPosition.getZ(), random), 0, .01, 0);
            if (random.nextFloat() < .25f) {
                double top = worldPosition.getY() + grill.getGrateTop() + .02;
                for (int i = 0; i < 2; i++)
                    level.addParticle(ParticleTypes.SPLASH, inside(worldPosition.getX(), random), top,
                            inside(worldPosition.getZ(), random), (random.nextFloat() - .5f) * .1f, 0,
                            (random.nextFloat() - .5f) * .1f);
            }
        }
        if (process == GrillProcess.BLASTING && random.nextInt(25) == 0)
            level.addParticle(ParticleRegistry.GRILL_LAVA.get(), inside(worldPosition.getX(), random),
                    worldPosition.getY() + grill.getGrateTop() + .02, inside(worldPosition.getZ(), random), 0, 0, 0);
    }

    // A random spot over the grate
    private static double inside(int corner, RandomSource random) {
        return corner + 3 / 16f + random.nextFloat() * 10 / 16f;
    }

    private void sync() {
        setChanged();
        sendData();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ContainerHelper.saveAllItems(tag, items, true, registries);
        int[] doneFlags = new int[SLOTS];
        for (int slot = 0; slot < SLOTS; slot++)
            doneFlags[slot] = done[slot] ? 1 : 0;
        tag.putIntArray("Done", doneFlags);
        if (clientPacket) {
            tag.putIntArray("SlideEdges", exitIds(Arrays.asList(slideEdge)));
            int[] off = new int[SLOTS];
            for (int slot = 0; slot < SLOTS; slot++)
                off[slot] = slidingOff[slot] ? 1 : 0;
            tag.putIntArray("SlidingOff", off);
            ListTag slides = new ListTag();
            for (float along : slide)
                slides.add(FloatTag.valueOf(along));
            tag.put("Slides", slides);
            tag.putIntArray("SlideDelays", slideDelay);
            return;
        }
        tag.putIntArray("Progress", progress);
        tag.putIntArray("CookTime", cookTime);
        tag.putIntArray("Exits", exitIds(Arrays.asList(exits)));
        int[] processes = new int[SLOTS];
        for (int slot = 0; slot < SLOTS; slot++)
            processes[slot] = placedFor[slot] == null ? -1 : placedFor[slot].ordinal();
        tag.putIntArray("PlacedFor", processes);
        ListTag queuedTags = new ListTag();
        for (List<ItemStack> waiting : queued) {
            ListTag slotTags = new ListTag();
            for (ItemStack stack : waiting)
                slotTags.add(stack.save(registries));
            queuedTags.add(slotTags);
        }
        tag.put("Queued", queuedTags);
        tag.putBoolean("PulledFromBeside", pulledFromBeside);
        ListTag arms = new ListTag();
        for (BlockPos arm : pullingArms)
            arms.add(NbtUtils.writeBlockPos(arm));
        tag.put("PullingArms", arms);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        int[] doneFlags = tag.getIntArray("Done");
        for (int slot = 0; slot < SLOTS; slot++)
            done[slot] = slot < doneFlags.length && doneFlags[slot] != 0;
        if (clientPacket) {
            int[] edges = tag.getIntArray("SlideEdges");
            int[] off = tag.getIntArray("SlidingOff");
            ListTag slides = tag.getList("Slides", Tag.TAG_FLOAT);
            int[] delays = tag.getIntArray("SlideDelays");
            for (int slot = 0; slot < SLOTS; slot++) {
                Direction edge = exit(edges, slot);
                boolean wasOff = slidingOff[slot];
                Direction wasEdge = slideEdge[slot];
                slideEdge[slot] = edge;
                slidingOff[slot] = slot < off.length && off[slot] != 0;
                float along = slot < slides.size() ? slides.getFloat(slot) : 0;
                if (edge != wasEdge || slidingOff[slot] != wasOff || along < slide[slot]) {
                    slide[slot] = prevSlide[slot] = along;
                    slideDelay[slot] = slot < delays.length ? delays[slot] : 0;
                }
            }
            return;
        }
        copyInto(tag.getIntArray("Progress"), progress);
        copyInto(tag.getIntArray("CookTime"), cookTime);
        int[] exitIds = tag.getIntArray("Exits");
        int[] processes = tag.getIntArray("PlacedFor");
        GrillProcess current = getProcess();
        for (int slot = 0; slot < SLOTS; slot++) {
            exits[slot] = exit(exitIds, slot);
            placedFor[slot] = slot < processes.length ? processes[slot] >= 0 ? GrillProcess.values()[processes[slot]]
                    : null : items.get(slot)
                    .isEmpty() ? null : current;
        }
        ListTag queuedTags = tag.getList("Queued", Tag.TAG_LIST);
        for (int slot = 0; slot < SLOTS; slot++) {
            List<ItemStack> waiting = queued.get(slot);
            waiting.clear();
            if (slot < queuedTags.size())
                for (Tag stackTag : queuedTags.getList(slot))
                    ItemStack.parse(registries, stackTag)
                            .ifPresent(waiting::add);
        }
        for (Tag stackTag : tag.getList("Extras", Tag.TAG_COMPOUND))
            ItemStack.parse(registries, stackTag)
                    .ifPresent(this::queueLoose);
        pulledFromBeside = tag.getBoolean("PulledFromBeside");
        pullingArms.clear();
        for (Tag arm : tag.getList("PullingArms", Tag.TAG_INT_ARRAY)) {
            int[] xyz = ((IntArrayTag) arm).getAsIntArray();
            if (xyz.length == 3)
                pullingArms.add(new BlockPos(xyz[0], xyz[1], xyz[2]));
        }
    }

    // Loads products from older saves
    private void queueLoose(ItemStack stack) {
        for (int slot = 0; slot < SLOTS; slot++)
            if (done[slot] && !items.get(slot)
                    .isEmpty()) {
                queued.get(slot)
                        .add(stack);
                return;
            }
        int slot = freeSlot();
        if (slot == -1) {
            queued.get(0)
                    .add(stack);
            return;
        }
        items.set(slot, stack);
        done[slot] = true;
    }

    private static int[] exitIds(List<Direction> exits) {
        return exits.stream()
                .mapToInt(exit -> exit == null ? -1 : exit.get3DDataValue())
                .toArray();
    }

    @Nullable
    private static Direction exit(int[] ids, int index) {
        return index < ids.length && ids[index] >= 0 ? Direction.from3DDataValue(ids[index]) : null;
    }

    private static void copyInto(int[] from, int[] to) {
        for (int i = 0; i < to.length; i++)
            to[i] = i < from.length ? from[i] : 0;
    }

    // The grill's inventory for automation
    private class GrillItemHandler implements IItemHandler {

        private final boolean beside;
        @Nullable
        private final BlockPos arm;

        private GrillItemHandler(boolean beside, @Nullable BlockPos arm) {
            this.beside = beside;
            this.arm = arm;
        }

        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !items.get(slot)
                    .isEmpty() || !canCook(stack))
                return stack;
            if (!simulate)
                placeAt(slot, stack);
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (level != null && arm == null)
                lastPulled = clock;
            if (beside && !pulledFromBeside) {
                pulledFromBeside = true;
                setChanged();
            }
            if (arm != null && pullingArms.add(arm))
                setChanged();
            ItemStack stack = getStackInSlot(slot);
            if (amount <= 0 || stack.isEmpty() || !done[slot] || isGathering(slot, clock))
                return ItemStack.EMPTY;
            List<Integer> taking = arm == null ? List.of(slot) : stackingWith(slot, clock, other -> true);
            int count = Math.min(amount, combine(taking).getCount());
            if (!simulate) {
                takeFrom(taking, count, clock);
                sync();
            }
            return stack.copyWithCount(count);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return canCook(stack);
        }
    }
}
