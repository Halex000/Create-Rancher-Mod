package com.halex.createrancher.content.shearer;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.halex.createrancher.compat.sable.SubLevels;
import com.halex.createrancher.index.ArmPointTypeRegistry;
import com.halex.createrancher.mixin.ArmBlockEntityAccessor;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

// A mechanical arm that shears nearby animals
public class MechanicalShearerBlockEntity extends ArmBlockEntity implements Clearable {

    public static final int AREA_RADIUS = 3;
    public static final int AREA_HALF_HEIGHT = 1;
    private static final double AIM_TOLERANCE = 1;
    private static final int PULL_MEMORY = 40;
    private static final int UNTAKEN_TICKS = 40;
    private static final int LOOK_BREAK = 200;
    private static final float SNIP_TICKS = 3;

    @Nullable
    private UUID shearTarget;
    private int shearTargetId = -1;
    @Nullable
    private Vec3 lastTargetTop;
    @Nullable
    private Entity presentedTarget;
    private boolean pointsGiven;

    private final ItemStackHandler waiting = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler fromBeside = new Output(true);
    private final IItemHandler fromAfar = new Output(false);
    private boolean pulledFromBeside;
    private long lastPulled = Long.MIN_VALUE / 2;
    private long lastLooked = Long.MIN_VALUE / 2;
    private long looksIgnoredUntil = Long.MIN_VALUE / 2;
    private long heldForLookerSince = -1;

    private int snips;
    private int seenSnips = -1;
    private float sinceSnip = Float.MAX_VALUE;

    public MechanicalShearerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.removeIf(behaviour -> behaviour instanceof ScrollOptionBehaviour<?>
                || behaviour instanceof AdvancementBehaviour);
    }

    @Override
    public void tick() {
        if (!pointsGiven && level != null && !level.isClientSide) {
            pointsGiven = true;
            loadWithComponents(saveWithoutMetadata(level.registryAccess()), level.registryAccess());
            sendData();
        }
        ShearingPoint.whileShearerTicks(super::tick);
        if (level != null && level.isClientSide)
            sinceSnip++;
        else if (level != null && heldForLookerSince >= 0 && level.getGameTime() - heldForLookerSince > UNTAKEN_TICKS)
            dropUntaken();
    }

    // Drops sheared items that sat in the shearer untaken
    private void dropUntaken() {
        heldForLookerSince = -1;
        looksIgnoredUntil = level.getGameTime() + LOOK_BREAK;
        Vec3 at = SubLevels.toWorld(level, worldPosition, VecHelper.getCenterOf(worldPosition)
                .add(0, .75, 0));
        for (int i = 0; i < waiting.getSlots(); i++) {
            Containers.dropItemStack(level, at.x, at.y, at.z, waiting.getStackInSlot(i));
            waiting.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    private ArmBlockEntityAccessor arm() {
        return (ArmBlockEntityAccessor) this;
    }

    public AABB getShearingArea() {
        return new AABB(worldPosition).inflate(AREA_RADIUS, AREA_HALF_HEIGHT, AREA_RADIUS);
    }

    // Whether there's an animal to shear
    @Override
    protected int getDistributableAmount(ArmInteractionPoint point, int slot) {
        if (!(point instanceof ShearingPoint))
            return super.getDistributableAmount(point, slot);
        Mob aimed = getAimedAnimal();
        Mob target = isStillGood(aimed) ? aimed : chooseAnimal();
        setShearTarget(target);
        return target == null ? 0 : 1;
    }

    // Shears the animal the arm reached
    @Override
    protected void collectItem() {
        Mob aimed = getAimedAnimal();
        if (isStillGood(aimed))
            snip(aimed);
        arm().createrancher$setPhase(Phase.SEARCH_INPUTS);
        arm().createrancher$setChasedPointProgress(0);
        arm().createrancher$setChasedPointIndex(-1);
        sendData();
        setChanged();
    }

    private boolean isStillGood(@Nullable Mob animal) {
        return animal != null && Shearing.canShear(animal) && getShearingArea().inflate(AIM_TOLERANCE)
                .contains(SubLevels.toLocal(level, worldPosition, animal.position()));
    }

    @Nullable
    private Mob chooseAnimal() {
        Vec3 center = SubLevels.toWorld(level, worldPosition, VecHelper.getCenterOf(worldPosition));
        return Shearing.shearableIn(level, getShearingArea())
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(center), b.distanceToSqr(center)))
                .orElse(null);
    }

    private void snip(Mob animal) {
        List<ItemEntity> dropped = Shearing.shear(animal);
        snips++;
        boolean pulled = isSomethingPulling();
        boolean pulling = pulled || isSomethingLooking();
        for (ItemEntity item : dropped) {
            ItemStack left = pulling ? ItemHandlerHelper.insertItemStacked(waiting, item.getItem(), false)
                    : item.getItem();
            if (left.getCount() < item.getItem()
                    .getCount() && !pulled && heldForLookerSince < 0)
                heldForLookerSince = level.getGameTime();
            if (left.isEmpty())
                continue;
            item.setItem(left);
            level.addFreshEntity(item);
        }
    }

    @Nullable
    private Mob getAimedAnimal() {
        if (shearTarget == null || !(level instanceof ServerLevel serverLevel))
            return null;
        return serverLevel.getEntity(shearTarget) instanceof Mob mob ? mob : null;
    }

    private void setShearTarget(@Nullable Mob animal) {
        UUID id = animal == null ? null : animal.getUUID();
        if (id == null ? shearTarget == null : id.equals(shearTarget))
            return;
        shearTarget = id;
        sendData();
    }

    // Where the arm aims
    public Vec3 getShearTargetTop() {
        Entity target = presentedTarget;
        if (target == null && level instanceof ServerLevel serverLevel && shearTarget != null)
            target = serverLevel.getEntity(shearTarget);
        else if (target == null && level != null && level.isClientSide && shearTargetId != -1)
            target = level.getEntity(shearTargetId);
        if (target != null)
            lastTargetTop = SubLevels.toLocal(level, worldPosition, target.position()
                    .add(0, target.getBbHeight(), 0));
        return lastTargetTop != null ? lastTargetTop : VecHelper.getCenterOf(worldPosition)
                .add(0, 1, 0);
    }

    // How far open the shears are, from 0 to 1
    public float getShearsOpen(float partialTicks) {
        float since = sinceSnip + partialTicks;
        if (since < SNIP_TICKS)
            return 1 - since / SNIP_TICKS;
        if (arm().createrancher$getPhase() != Phase.MOVE_TO_INPUT)
            return 0;
        float progress = arm().createrancher$getChasedPointProgress();
        return Math.max(0, Math.min(1, (progress - .5f) * 2));
    }

    // Sets the animal to aim at in Ponder scenes
    public void presentTarget(@Nullable Entity animal) {
        presentedTarget = animal;
    }

    // Starts the arm's next move in Ponder scenes
    public void presentPhase(Phase phase, int targetIndex) {
        CompoundTag tag = new CompoundTag();
        write(tag, level.registryAccess(), true);
        NBTHelper.writeEnum(tag, "Phase", phase);
        tag.putInt("TargetPointIndex", targetIndex);
        tag.putFloat("MovementProgress", 0);
        read(tag, level.registryAccess(), true);
    }

    // Snaps the shears shut in Ponder scenes
    public void presentSnip() {
        snips++;
        seenSnips = snips;
        sinceSnip = 0;
    }

    public IItemHandler getItemHandler(boolean beside) {
        return beside ? fromBeside : fromAfar;
    }

    public void onNeighbourChanged() {
        if (!pulledFromBeside)
            return;
        pulledFromBeside = false;
        setChanged();
    }

    private boolean isSomethingPulling() {
        return pulledFromBeside || level.getGameTime() - lastPulled <= PULL_MEMORY;
    }

    // Whether a neighbouring block has recently checked the shearer's inventory
    private boolean isSomethingLooking() {
        long now = level.getGameTime();
        return now - lastLooked <= PULL_MEMORY && now >= looksIgnoredUntil;
    }

    // Lets hoppers and funnels take the sheared items
    private class Output implements IItemHandler {

        private final boolean beside;

        Output(boolean beside) {
            this.beside = beside;
        }

        @Override
        public int getSlots() {
            return waiting.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (beside && level != null && !level.isClientSide)
                lastLooked = level.getGameTime();
            return waiting.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (level != null && !level.isClientSide) {
                lastPulled = level.getGameTime();
                if (beside && !pulledFromBeside) {
                    pulledFromBeside = true;
                    setChanged();
                }
            }
            ItemStack taken = waiting.extractItem(slot, amount, simulate);
            if (!simulate && !taken.isEmpty())
                heldForLookerSince = -1;
            return taken;
        }

        @Override
        public int getSlotLimit(int slot) {
            return waiting.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }

    // Empties it without dropping anything
    @Override
    public void clearContent() {
        for (int i = 0; i < waiting.getSlots(); i++)
            waiting.setStackInSlot(i, ItemStack.EMPTY);
        ((ArmBlockEntityAccessor) this).createrancher$setHeldItem(ItemStack.EMPTY);
    }

    // Drops what's inside when broken
    @Override
    public void destroy() {
        super.destroy();
        for (int i = 0; i < waiting.getSlots(); i++)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    waiting.getStackInSlot(i));
    }

    private static CompoundTag shearingPointTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ArmPointTypeRegistry.SHEARING.getId()
                .toString());
        tag.put("Pos", NbtUtils.writeBlockPos(BlockPos.ZERO));
        NBTHelper.writeEnum(tag, "Mode", ArmInteractionPoint.Mode.TAKE);
        return tag;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (shearTarget != null)
            tag.putUUID("ShearTarget", shearTarget);
        tag.putInt("Snips", snips);
        if (clientPacket) {
            Entity target = level instanceof ServerLevel serverLevel && shearTarget != null
                    ? serverLevel.getEntity(shearTarget) : null;
            tag.putInt("ShearTargetId", target == null ? -1 : target.getId());
            return;
        }
        tag.put("Waiting", waiting.serializeNBT(registries));
        tag.putBoolean("PulledFromBeside", pulledFromBeside);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        ListTag points = new ListTag();
        points.add(shearingPointTag());
        tag.put("InteractionPoints", points);
        super.read(tag, registries, clientPacket);
        shearTarget = tag.hasUUID("ShearTarget") ? tag.getUUID("ShearTarget") : null;
        snips = tag.getInt("Snips");
        if (clientPacket) {
            shearTargetId = tag.getInt("ShearTargetId");
            if (seenSnips != -1 && snips != seenSnips)
                sinceSnip = 0;
            seenSnips = snips;
            return;
        }
        if (tag.contains("Waiting"))
            waiting.deserializeNBT(registries, tag.getCompound("Waiting"));
        pulledFromBeside = tag.getBoolean("PulledFromBeside");
    }
}
