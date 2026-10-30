package com.halex.createrancher.content.auger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import com.halex.createrancher.compat.sable.Moves;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.halex.createrancher.content.auger.AugerBlock.Slope;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

// Holds and moves the items an auger is carrying
public class AugerBlockEntity extends KineticBlockEntity implements TransformableBlockEntity, Clearable {

    public static final float SPACING = .4f;
    public static final int MAX_LOADS = 3;
    public static final float REST = .45f;
    public static final float END_FLOOR = 2 / 16f;
    public static final float CENTRE = 4.5f / 16;
    public static final float HAND_OVER = 1 - CENTRE;
    private static final float RESYNC = 1;

    // A stack sitting on the auger's blades
    public static class Load {
        public ItemStack stack;
        public float height;
        public float prevHeight;
        final long id;
        long movedIn = Long.MIN_VALUE;

        Load(ItemStack stack, float height, long id) {
            this.stack = stack;
            this.height = prevHeight = height;
            this.id = id;
        }
    }

    private final List<Load> loads = new ArrayList<>();
    private final Map<Long, Load> departed = new HashMap<>();
    private final Map<Long, Integer> departedAt = new HashMap<>();
    private int clientTicks;
    private boolean moved;
    @Nullable
    private AugerBlock.Part lastPart;
    private int lastDirection = 1;
    private long neighbourTick = Long.MIN_VALUE;
    private BlockState neighbourState;
    private final AugerBlockEntity[] neighbours = new AugerBlockEntity[2];
    private final BlockState[] neighbourStates = new BlockState[2];
    private final boolean[] neighbourKnown = new boolean[2];
    private static final int DEPARTED_TICKS = 3;
    static long clientTick;
    private final AugerItemHandler itemHandler = new AugerItemHandler();
    @SuppressWarnings("unchecked")
    private final BlockCapabilityCache<IItemHandler, Direction>[] endInventories = new BlockCapabilityCache[2];

    public AugerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public List<Load> getLoads() {
        return loads;
    }

    // Speed in blocks per tick
    public float getClimbSpeed() {
        return Math.abs(getSpeed()) / 1200f;
    }

    // 1 when carrying up, -1 when carrying down
    public int getDirection() {
        float speed = getSpeed();
        return speed > 0 ? 1 : speed < 0 ? -1 : lastDirection;
    }

    public float getFloor() {
        return part().isBottom() ? END_FLOOR : 0;
    }

    // Its place in the column
    private AugerBlock.Part part() {
        BlockState state = getBlockState();
        if (level == null || AugerBlock.slopeOf(state).kind != Slope.Kind.DIAGONAL)
            return state.getValue(AugerBlock.PART);
        return AugerBlock.Part.of(AugerBlock.linked(level, worldPosition, state, false), AugerBlock.linked(level,
                worldPosition, state, true));
    }

    // Where a load goes in
    private float entry(int direction) {
        if (direction > 0)
            return getFloor();
        return part().isTop() ? REST : HAND_OVER - .001f;
    }

    // The next or previous auger in the column
    @Nullable
    private AugerBlockEntity getNeighbour(boolean above) {
        if (level == null)
            return null;
        long now = currentTick();
        BlockState state = getBlockState();
        if (now != neighbourTick || state != neighbourState) {
            neighbourTick = now;
            neighbourState = state;
            neighbourKnown[0] = false;
            neighbourKnown[1] = false;
        }
        int side = above ? 1 : 0;
        AugerBlockEntity cached = neighbours[side];
        if (neighbourKnown[side] && (cached == null || !cached.isRemoved() && cached.getBlockState() == neighbourStates[side]))
            return cached;
        AugerBlockEntity found = findNeighbour(above, state);
        neighbours[side] = found;
        neighbourStates[side] = found == null ? null : found.getBlockState();
        neighbourKnown[side] = true;
        return found;
    }

    // The next or previous auger in the column
    @Nullable
    private AugerBlockEntity findNeighbour(boolean above, BlockState state) {
        BlockPos pos = above ? AugerBlock.nextPos(state, worldPosition) : AugerBlock.previousPos(state, worldPosition);
        if (!level.isLoaded(pos) || !AugerBlock.linked(level, worldPosition, state, above))
            return null;
        return level.getBlockEntity(pos) instanceof AugerBlockEntity auger ? auger : null;
    }

    // How long this auger's stretch is compared to an upright one
    public float stretchAt(float height) {
        Slope slope = AugerBlock.slopeOf(getBlockState());
        if (slope.kind == null)
            return 1;
        return switch (slope.kind) {
            case DIAGONAL -> AugerBlock.DIAGONAL_LENGTH;
            case START -> height < .5f ? 1 : AugerBlock.DIAGONAL_LENGTH;
            case END -> height < .5f ? AugerBlock.DIAGONAL_LENGTH : 1;
        };
    }

    @Nullable
    private Load lowest() {
        return loads.isEmpty() ? null : loads.get(0);
    }

    @Nullable
    private Load highest() {
        return loads.isEmpty() ? null : loads.get(loads.size() - 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (moved && level != null && !level.isClientSide) {
            moved = false;
            AugerBlock.recheck(level, worldPosition);
            AugerBlock.recheckAround(level, worldPosition, false);
        }
        if (level != null && level.isClientSide && !departed.isEmpty()) {
            clientTicks++;
            departedAt.entrySet()
                    .removeIf(e -> {
                        boolean old = clientTicks - e.getValue() > DEPARTED_TICKS;
                        if (old)
                            departed.remove(e.getKey());
                        return old;
                    });
        }
        AugerBlock.Part part = part();
        if (part != lastPart) {
            boolean placed = lastPart == null;
            lastPart = part;
            boolean moved = settle(part);
            if (level != null && !level.isClientSide && (moved || !placed))
                changed();
        }
        long now = currentTick();
        for (Load load : loads)
            if (load.movedIn != now)
                load.prevHeight = load.height;
        float climb = getClimbSpeed();
        if (climb == 0)
            return;
        int direction = getDirection();
        lastDirection = direction;
        if (!loads.isEmpty()) {
            if (direction > 0)
                carryUp(climb, now);
            else
                carryDown(climb, now);
        }
        if (level != null && (!level.isClientSide || isVirtual()))
            workEndInventories(direction);
    }

    // Pulls items in and pushes them out at the ends of the column
    private void workEndInventories(int direction) {
        if (getNeighbour(direction < 0) == null) {
            IItemHandler from = endInventory(direction < 0);
            if (from != null && roomToGoIn())
                for (int slot = 0; slot < from.getSlots(); slot++) {
                    ItemStack stack = from.extractItem(slot, 64, true);
                    if (stack.isEmpty())
                        continue;
                    stack = from.extractItem(slot, stack.getCount(), false);
                    if (stack.isEmpty())
                        continue;
                    addLoad(stack, direction);
                    break;
                }
        }
        if (getNeighbour(direction > 0) == null) {
            Load arrived = direction > 0 ? highest() : lowest();
            float end = direction > 0 ? REST : END_FLOOR;
            if (arrived == null || Math.abs(arrived.height - end) > 1e-4)
                return;
            IItemHandler into = endInventory(direction > 0);
            if (into == null)
                return;
            ItemStack left = ItemHandlerHelper.insertItemStacked(into, arrived.stack.copy(), false);
            if (left.getCount() == arrived.stack.getCount())
                return;
            if (left.isEmpty())
                loads.remove(arrived);
            else
                arrived.stack = left;
            changed();
        }
    }

    // The inventory past one end of this auger
    @Nullable
    private IItemHandler endInventory(boolean positiveEnd) {
        if (level == null)
            return null;
        BlockState state = getBlockState();
        BlockPos at = positiveEnd ? AugerBlock.nextPos(state, worldPosition) : AugerBlock.previousPos(state,
                worldPosition);
        BlockPos step = worldPosition.subtract(at);
        Direction side = Direction.fromDelta(step.getX(), step.getY(), step.getZ());
        if (side == null)
            return null;
        if (!(level instanceof ServerLevel server))
            return level.getCapability(Capabilities.ItemHandler.BLOCK, at, side);
        int end = positiveEnd ? 1 : 0;
        BlockCapabilityCache<IItemHandler, Direction> cache = endInventories[end];
        if (cache == null || !cache.pos()
                .equals(at) || cache.context() != side)
            endInventories[end] = cache = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, server, at,
                    side, () -> !isRemoved(), () -> {
                    });
        return cache.getCapability();
    }

    // Adds a new stack
    private void addLoad(ItemStack stack, int direction) {
        Load load = new Load(stack, entry(direction), newId());
        if (direction > 0)
            loads.add(0, load);
        else
            loads.add(load);
        changed();
    }

    private void carryUp(float climb, long now) {
        AugerBlockEntity above = getNeighbour(true);
        float limit = REST;
        if (above != null) {
            Load next = above.lowest();
            limit = next == null ? Float.MAX_VALUE : next.height + 1 - SPACING;
        }
        for (int i = loads.size() - 1; i >= 0; i--) {
            Load load = loads.get(i);
            if (load.movedIn != now) {
                load.height = Math.max(load.height, Math.min(load.height + climb / stretchAt(load.height), limit));
                load.movedIn = now;
            }
            limit = load.height - SPACING;
        }
        Load top = highest();
        if (above != null && top != null && top.height >= HAND_OVER && above.loads.size() < MAX_LOADS) {
            loads.remove(top);
            top.height -= 1;
            top.prevHeight -= 1;
            above.loads.add(0, top);
            if (level != null && !level.isClientSide) {
                changed();
                above.changed();
            }
        }
    }

    // Like carryUp but down the column
    private void carryDown(float climb, long now) {
        AugerBlockEntity below = getNeighbour(false);
        float limit = END_FLOOR;
        if (below != null) {
            Load next = below.highest();
            limit = next == null ? -Float.MAX_VALUE : next.height - 1 + SPACING;
        }
        for (Load load : loads) {
            if (load.movedIn != now) {
                load.height = Math.min(load.height, Math.max(load.height - climb / stretchAt(load.height), limit));
                load.movedIn = now;
            }
            limit = load.height + SPACING;
        }
        Load bottom = lowest();
        if (below != null && bottom != null && bottom.height < -CENTRE && below.loads.size() < MAX_LOADS) {
            loads.remove(bottom);
            bottom.height += 1;
            bottom.prevHeight += 1;
            below.loads.add(bottom);
            if (level != null && !level.isClientSide) {
                changed();
                below.changed();
            }
        }
    }

    // Moves loads that don't fit where they are. Returns whether anything moved.
    private boolean settle(AugerBlock.Part part) {
        int n = loads.size();
        if (n == 0)
            return false;
        float top = part.isTop() ? REST : HAND_OVER - .001f;
        float bottom = part.isBottom() ? END_FLOOR : -CENTRE;
        float gap = n > 1 ? Math.min(SPACING, (top - bottom) / (n - 1)) : SPACING;
        boolean moved = false;
        float limit = top;
        for (int i = n - 1; i >= 0; i--) {
            Load load = loads.get(i);
            if (load.height > limit) {
                load.height = limit;
                moved = true;
            }
            limit = load.height - gap;
        }
        float floor = bottom;
        for (Load load : loads) {
            if (load.height < floor) {
                load.height = floor;
                moved = true;
            }
            floor = load.height + gap;
        }
        return moved;
    }

    private long currentTick() {
        if (level == null)
            return 0;
        if (isVirtual())
            return AugerPonderTicks.now(level);
        return level.isClientSide ? clientTick : level.getGameTime();
    }

    @Nullable
    private Load find(long id) {
        for (Load load : loads)
            if (load.id == id)
                return load;
        return null;
    }

    // Syncs the client's loads with the server's
    private void receive(List<Load> incoming) {
        int direction = getDirection();
        AugerBlockEntity ahead = getNeighbour(direction > 0);
        AugerBlockEntity behind = getNeighbour(direction < 0);
        List<Load> kept = new ArrayList<>();
        for (Load in : incoming) {
            Load mine = find(in.id);
            if (mine == null && ahead != null && ahead.find(in.id) != null)
                continue;
            if (mine == null && behind != null) {
                mine = behind.find(in.id);
                if (mine != null)
                    behind.loads.remove(mine);
                else
                    mine = behind.takeDeparted(in.id);
                if (mine != null) {
                    mine.height -= direction;
                    mine.prevHeight -= direction;
                }
            }
            if (mine == null) {
                float climb = getClimbSpeed() / stretchAt(in.height);
                if (Math.abs(in.height - entry(direction)) <= climb)
                    in.prevHeight = in.height - direction * climb;
                kept.add(in);
                continue;
            }
            mine.stack = in.stack;
            if (Math.abs(mine.height - in.height) > RESYNC)
                mine.height = mine.prevHeight = in.height;
            kept.add(mine);
        }
        kept.sort((a, b) -> Float.compare(a.height, b.height));
        for (Load load : loads)
            if (!kept.contains(load)) {
                departed.put(load.id, load);
                departedAt.put(load.id, clientTicks);
            }
        loads.clear();
        loads.addAll(kept);
    }

    @Nullable
    private Load takeDeparted(long id) {
        departedAt.remove(id);
        return departed.remove(id);
    }

    private void changed() {
        setChanged();
        sendData();
    }

    // The load that was just put in
    @Nullable
    private Load inlet() {
        int direction = getDirection();
        Load first = direction > 0 ? lowest() : highest();
        if (first == null)
            return null;
        return Math.abs(first.height - entry(direction)) < SPACING ? first : null;
    }

    // Whether there's room for a new load
    private boolean roomToGoIn() {
        if (loads.size() >= MAX_LOADS || inlet() != null)
            return false;
        int direction = getDirection();
        AugerBlockEntity behind = getNeighbour(direction < 0);
        if (behind == null)
            return true;
        float entry = entry(direction);
        if (direction > 0) {
            Load coming = behind.highest();
            return coming == null || coming.height - 1 + SPACING <= entry;
        }
        Load coming = behind.lowest();
        return coming == null || coming.height + 1 - SPACING >= entry;
    }

    // Slot 0 is the input and the rest are the loads
    @Nullable
    private Load inSlot(int slot) {
        Load inlet = inlet();
        if (slot == 0)
            return inlet;
        List<Load> others = new ArrayList<>(loads);
        others.remove(inlet);
        if (getDirection() > 0)
            java.util.Collections.reverse(others);
        int index = slot - 1;
        return index < others.size() ? others.get(index) : null;
    }

    // Passes rotation along to diagonal augers
    @Override
    public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        Slope slope = AugerBlock.slopeOf(state);
        if (slope.toDiagonal())
            neighbours.add(AugerBlock.nextPos(state, worldPosition));
        if (slope.fromDiagonal())
            neighbours.add(AugerBlock.previousPos(state, worldPosition));
        return super.addPropagationLocations(block, state, neighbours);
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff,
                                     boolean connectedViaAxes, boolean connectedViaCogs) {
        Slope slope = AugerBlock.slopeOf(stateFrom);
        if (target instanceof AugerBlockEntity && level != null && slope != Slope.NONE) {
            BlockPos at = target.getBlockPos();
            if (slope.toDiagonal() && at.equals(AugerBlock.nextPos(stateFrom, worldPosition)) && AugerBlock.linked(level,
                    worldPosition, stateFrom, true))
                return 1;
            if (slope.fromDiagonal() && at.equals(AugerBlock.previousPos(stateFrom, worldPosition))
                    && AugerBlock.linked(level, worldPosition, stateFrom, false))
                return 1;
        }
        return super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
    }

    // Lets diagonals render outside their block
    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1);
    }

    // Keeps the load where it is when a contraption flips the auger
    @Override
    public void transform(BlockEntity blockEntity, StructureTransform transform) {
        Direction.Axis now = AugerBlock.axisOf(getBlockState());
        for (Direction.Axis was : Direction.Axis.values()) {
            Direction to = AugerBlock.positiveEndAfter(was, transform);
            if (to.getAxis() != now)
                continue;
            if (to.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                for (Load load : loads)
                    load.height = load.prevHeight = 1 - 2 * CENTRE - load.height;
                loads.sort((a, b) -> Float.compare(a.height, b.height));
                lastPart = null;
                notifyUpdate();
            }
            return;
        }
    }

    // Empties it without dropping anything
    @Override
    public void clearContent() {
        loads.clear();
        departed.clear();
        departedAt.clear();
    }

    @Override
    public void destroy() {
        super.destroy();
        departed.clear();
        departedAt.clear();
        if (level == null || level.isClientSide) {
            loads.clear();
            return;
        }
        for (Load load : loads)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    load.stack);
        loads.clear();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ListTag list = new ListTag();
        for (Load load : loads) {
            CompoundTag loadTag = new CompoundTag();
            loadTag.put("Item", load.stack.save(registries));
            loadTag.putFloat("Height", load.height);
            loadTag.putLong("Id", load.id);
            list.add(loadTag);
        }
        tag.put("Loads", list);
        tag.putInt("Direction", lastDirection);
        if (!clientPacket)
            Moves.saveOrigin(tag, worldPosition);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (!clientPacket)
            moved = Moves.moved(tag, worldPosition);
        if (tag.contains("Direction"))
            lastDirection = tag.getInt("Direction") < 0 ? -1 : 1;
        List<Load> read = new ArrayList<>();
        for (Tag entry : tag.getList("Loads", Tag.TAG_COMPOUND)) {
            CompoundTag loadTag = (CompoundTag) entry;
            long id = loadTag.contains("Id") ? loadTag.getLong("Id") : newId();
            ItemStack.parse(registries, loadTag.getCompound("Item"))
                    .ifPresent(stack -> read.add(new Load(stack, loadTag.getFloat("Height"), id)));
        }
        if (clientPacket && level != null && level.isClientSide) {
            receive(read);
            return;
        }
        loads.clear();
        loads.addAll(read);
    }

    private long newId() {
        return level != null ? level.getRandom()
                .nextLong() : ThreadLocalRandom.current()
                .nextLong();
    }

    private class AugerItemHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return MAX_LOADS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            Load load = inSlot(slot);
            return load == null ? ItemStack.EMPTY : load.stack;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty())
                return stack;
            Load inlet = inlet();
            int room;
            if (inlet != null) {
                if (!ItemStack.isSameItemSameComponents(inlet.stack, stack))
                    return stack;
                room = inlet.stack.getMaxStackSize() - inlet.stack.getCount();
            } else {
                if (!roomToGoIn())
                    return stack;
                room = stack.getMaxStackSize();
            }
            int moved = Math.min(room, stack.getCount());
            if (moved <= 0)
                return stack;
            if (!simulate) {
                if (inlet != null) {
                    inlet.stack.grow(moved);
                    changed();
                } else
                    addLoad(stack.copyWithCount(moved), getDirection());
            }
            return stack.copyWithCount(stack.getCount() - moved);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            Load load = inSlot(slot);
            if (load == null || amount <= 0)
                return ItemStack.EMPTY;
            ItemStack taken = load.stack.copyWithCount(Math.min(amount, load.stack.getCount()));
            if (!simulate) {
                load.stack.shrink(taken.getCount());
                if (load.stack.isEmpty())
                    loads.remove(load);
                changed();
            }
            return taken;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0;
        }
    }
}
