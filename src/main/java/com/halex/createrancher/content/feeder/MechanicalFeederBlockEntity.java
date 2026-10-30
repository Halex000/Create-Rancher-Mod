package com.halex.createrancher.content.feeder;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.halex.createrancher.compat.sable.Moves;
import com.halex.createrancher.compat.sable.SubLevels;
import com.halex.createrancher.content.alfalfa.PelletFeeding;
import com.halex.createrancher.index.ArmPointTypeRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.halex.createrancher.index.LangRegistry;
import com.halex.createrancher.mixin.ArmBlockEntityAccessor;
import com.halex.createrancher.mixin.MushroomCowAccessor;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

// A mechanical arm that feeds nearby animals
public class MechanicalFeederBlockEntity extends ArmBlockEntity implements Clearable {

    private static final GameProfile FEEDER = new GameProfile(UUID.fromString("0f9d2b7c-5a41-4e5e-8f3c-6b2d1e7a9c13"),
            "[Mechanical Feeder]");

    public static final int AREA_RADIUS = 3;
    public static final int AREA_HALF_HEIGHT = 1;
    private static final double AIM_TOLERANCE = 1;

    public enum LinkStatus {
        LINKED, TOO_FAR, NOT_A_PICKUP, INVALID_TARGET;

        public String getMessageKey() {
            return "createrancher.message.feeder." + name().toLowerCase();
        }
    }

    @Nullable
    private CompoundTag pickupPoint;
    private boolean moved;

    @Nullable
    private UUID feedTarget;
    private int feedTargetId = -1;
    @Nullable
    private Vec3 lastFeedTargetTop;
    @Nullable
    private Entity presentedTarget;

    public MechanicalFeederBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
        FeedingPoint.whileFeederTicks(super::tick);
    }

    // Empties it without dropping anything
    @Override
    public void clearContent() {
        ((ArmBlockEntityAccessor) this).createrancher$setHeldItem(ItemStack.EMPTY);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!level.isClientSide)
            checkPickupStillThere();
    }

    // Drops the pickup point if it's gone or on another sub-level
    private void checkPickupStillThere() {
        ArmBlockEntityAccessor arm = (ArmBlockEntityAccessor) this;
        if (pickupPoint == null || arm.createrancher$isUpdatingInteractionPoints())
            return;
        List<ArmInteractionPoint> inputs = arm.createrancher$getInputs();
        if (!inputs.isEmpty()) {
            ArmInteractionPoint pickup = inputs.getFirst();
            boolean onSubLevel = SubLevels.isOnSubLevel(level, worldPosition);
            boolean loaded = level.isLoaded(pickup.getPos());
            if (!loaded && !onSubLevel)
                return;
            boolean gone = onSubLevel && (!loaded || !pickup.isValid());
            if (!gone && SubLevels.sameSubLevel(level, worldPosition, pickup.getPos()))
                return;
        }
        setPickupPoint(null);
        sendData();
        setChanged();
    }

    // Sets the arm's pickup point
    private void setPickupPoint(@Nullable CompoundTag pickup) {
        pickupPoint = pickup;
        ListTag points = new ListTag();
        if (pickup != null)
            points.add(pickup.copy());
        points.add(feedingPointTag());
        ArmBlockEntityAccessor arm = (ArmBlockEntityAccessor) this;
        arm.createrancher$setInteractionPointTag(points);
        arm.createrancher$setUpdatingInteractionPoints(true);
    }

    public AABB getFeedingArea() {
        return new AABB(worldPosition).inflate(AREA_RADIUS, AREA_HALF_HEIGHT, AREA_RADIUS);
    }

    // Whether a block can be the pickup point
    public static boolean isPickupCandidate(Level level, BlockPos pos) {
        ArmInteractionPoint point = ArmInteractionPoint.create(level, pos, level.getBlockState(pos));
        return point != null && !(point instanceof DepositOnlyArmInteractionPoint);
    }

    // Sets the pickup point
    public LinkStatus linkPickup(BlockPos pickup) {
        if (level == null || !isPickupCandidate(level, pickup))
            return LinkStatus.NOT_A_PICKUP;
        if (!SubLevels.sameSubLevel(level, worldPosition, pickup))
            return LinkStatus.INVALID_TARGET;
        if (!pickup.closerThan(worldPosition, getRange()))
            return LinkStatus.TOO_FAR;

        ArmInteractionPoint point = ArmInteractionPoint.create(level, pickup, level.getBlockState(pickup));
        point.cycleMode();

        setPickupPoint(point.serialize(worldPosition));
        sendData();
        setChanged();
        return LinkStatus.LINKED;
    }

    public boolean hasPickupPoint() {
        return pickupPoint != null;
    }

    private static CompoundTag feedingPointTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ArmPointTypeRegistry.ANIMAL_FEEDING.getId()
                .toString());
        tag.put("Pos", NbtUtils.writeBlockPos(BlockPos.ZERO));
        NBTHelper.writeEnum(tag, "Mode", ArmInteractionPoint.Mode.DEPOSIT);
        return tag;
    }

    // Feeds the animal the arm is aiming at
    public ItemStack feed(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || stack.getItem() instanceof BucketItem)
            return stack;

        Animal aimedAt = getAimedAnimal();
        boolean aimStillGood = aimedAt != null && wouldEat(aimedAt, stack) && getFeedingArea().inflate(AIM_TOLERANCE)
                .contains(SubLevels.toLocal(level, worldPosition, aimedAt.position()));

        if (!simulate) {
            if (!aimStillGood) {
                setFeedTarget(chooseAnimal(stack));
                return stack;
            }
            if (isPellets(stack)) {
                PelletFeeding.feed(aimedAt);
            } else if (wantsFlower(aimedAt, stack)) {
                feedFlower((MushroomCow) aimedAt, stack);
            } else {
                aimedAt.setInLove(null);
                level.playSound(null, aimedAt.blockPosition(), aimedAt.getEatingSound(stack), SoundSource.NEUTRAL,
                        1, 1);
            }
            return withOneLess(stack);
        }

        Animal target = aimStillGood ? aimedAt : chooseAnimal(stack);
        setFeedTarget(target);
        return target == null ? stack : withOneLess(stack);
    }

    private static ItemStack withOneLess(ItemStack stack) {
        ItemStack remainder = stack.copy();
        remainder.shrink(1);
        return remainder;
    }

    @Nullable
    private Animal getAimedAnimal() {
        if (feedTarget == null || !(level instanceof ServerLevel serverLevel))
            return null;
        return serverLevel.getEntity(feedTarget) instanceof Animal animal ? animal : null;
    }

    @Nullable
    public UUID getFeedTarget() {
        return feedTarget;
    }

    private static boolean isPellets(ItemStack stack) {
        return stack.is(ItemRegistry.ALFALFA_PELLETS.get());
    }

    // Whether the animal would take this food right now
    private boolean wouldEat(Animal animal, ItemStack stack) {
        if (isPellets(stack))
            return PelletFeeding.canFeed(animal);
        if (wantsFlower(animal, stack))
            return true;
        return animal.isFood(stack) && canBeFed(animal) && hasPartner(animal, animalsEating(stack));
    }

    private List<Animal> animalsEating(ItemStack stack) {
        return level.getEntitiesOfClass(Animal.class, getFeedingArea(),
                animal -> animal.isAlive() && animal.isFood(stack));
    }

    // Whether there's a partner nearby
    private static boolean hasPartner(Animal animal, List<Animal> animals) {
        for (Animal other : animals)
            if (other != animal && other.getType() == animal.getType() && isBreedingAdult(other)
                    && (other.isInLove() || canBeFed(other)))
                return true;
        return false;
    }

    // Picks which animal to feed
    @Nullable
    private Animal chooseAnimal(ItemStack stack) {
        Vec3 center = SubLevels.toWorld(level, worldPosition, VecHelper.getCenterOf(worldPosition));
        if (isStewFlower(stack)) {
            Animal mooshroom = level.getEntitiesOfClass(MushroomCow.class, getFeedingArea(),
                            cow -> wantsFlower(cow, stack))
                    .stream()
                    .min((a, b) -> Double.compare(a.distanceToSqr(center), b.distanceToSqr(center)))
                    .orElse(null);
            if (mooshroom != null)
                return mooshroom;
        }
        if (isPellets(stack))
            return level.getEntitiesOfClass(Animal.class, getFeedingArea(), PelletFeeding::canFeed)
                    .stream()
                    .min((a, b) -> Double.compare(a.distanceToSqr(center), b.distanceToSqr(center)))
                    .orElse(null);

        List<Animal> animals = animalsEating(stack);

        Animal best = null;
        double bestScore = Double.MAX_VALUE;
        for (Animal animal : animals) {
            if (!canBeFed(animal) || !hasPartner(animal, animals))
                continue;
            boolean partnerWaiting = animals.stream()
                    .anyMatch(other -> other != animal && other.getType() == animal.getType() && other.isInLove()
                            && isBreedingAdult(other));
            double score = animal.distanceToSqr(center) - (partnerWaiting ? 10000 : 0);
            if (score < bestScore) {
                bestScore = score;
                best = animal;
            }
        }
        return best;
    }

    // Whether the item is a suspicious stew flower
    private static boolean isStewFlower(ItemStack stack) {
        return stack.is(ItemTags.SMALL_FLOWERS) && SuspiciousEffectHolder.tryGet(stack.getItem()) != null;
    }

    // Whether a brown mooshroom will take a flower
    private static boolean wantsFlower(Animal animal, ItemStack stack) {
        return animal instanceof MushroomCow mooshroom && mooshroom.isAlive()
                && mooshroom.getVariant() == MushroomCow.MushroomType.BROWN && isStewFlower(stack)
                && ((MushroomCowAccessor) mooshroom).createrancher$getStewEffects() == null;
    }

    // Feeds the animal like a player would
    private void feedFlower(MushroomCow mooshroom, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        FakePlayer player = FakePlayerFactory.get(serverLevel, FEEDER);
        player.setPos(mooshroom.getX(), mooshroom.getY(), mooshroom.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack.copyWithCount(1));
        mooshroom.interact(player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        serverLevel.sendParticles(ParticleTypes.EFFECT, mooshroom.getX(), mooshroom.getY(0.5), mooshroom.getZ(), 4,
                .25, .1, .25, 0);
    }

    // Whether the animal can breed right now
    private static boolean isBreedingAdult(Animal animal) {
        if (!animal.isAlive() || animal.getAge() != 0)
            return false;
        if (animal instanceof TamableAnimal tamable && !tamable.isTame())
            return false;
        return !(animal instanceof AbstractHorse horse) || horse.isTamed();
    }

    private static boolean canBeFed(Animal animal) {
        return isBreedingAdult(animal) && animal.canFallInLove();
    }

    private void setFeedTarget(@Nullable Animal animal) {
        UUID id = animal == null ? null : animal.getUUID();
        if (id == null ? feedTarget == null : id.equals(feedTarget))
            return;
        feedTarget = id;
        sendData();
    }

    // Where the arm aims
    public Vec3 getFeedTargetTop() {
        Entity target = presentedTarget;
        if (target == null && level instanceof ServerLevel serverLevel && feedTarget != null)
            target = serverLevel.getEntity(feedTarget);
        else if (target == null && level.isClientSide && feedTargetId != -1)
            target = level.getEntity(feedTargetId);
        if (target != null)
            lastFeedTargetTop = SubLevels.toLocal(level, worldPosition, target.position()
                    .add(0, target.getBbHeight(), 0));
        return lastFeedTargetTop != null ? lastFeedTargetTop : VecHelper.getCenterOf(worldPosition)
                .add(0, 1, 0);
    }

    // Sets the animal to aim at in Ponder scenes
    public void presentTarget(@Nullable Entity animal) {
        presentedTarget = animal;
    }

    // Starts the arm's next move in Ponder scenes
    public void presentPhase(Phase phase, ItemStack held, int targetIndex) {
        CompoundTag tag = new CompoundTag();
        write(tag, level.registryAccess(), true);
        NBTHelper.writeEnum(tag, "Phase", phase);
        tag.put("HeldItem", held.saveOptional(level.registryAccess()));
        tag.putInt("TargetPointIndex", targetIndex);
        tag.putFloat("MovementProgress", 0);
        read(tag, level.registryAccess(), true);
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (pickupPoint != null || isPlayerSneaking)
            return super.addToTooltip(tooltip, isPlayerSneaking);
        LangRegistry.builder()
                .translate("hint.feeder.no_pickup.title")
                .style(ChatFormatting.GOLD)
                .forGoggles(tooltip);
        for (Component line : FontHelper.cutTextComponent(
                Component.translatable("createrancher.hint.feeder.no_pickup", getRange()),
                FontHelper.Palette.GRAY_AND_WHITE))
            LangRegistry.builder()
                    .add(line)
                    .forGoggles(tooltip);
        return true;
    }

    // Turns the pickup point with the feeder after it was moved and turned
    @Override
    protected void initInteractionPoints() {
        if (moved && pickupPoint != null) {
            moved = false;
            Rotation turn = Moves.armTurn(level, worldPosition, pickupPoint);
            if (turn != Rotation.NONE) {
                Moves.turnPoint(pickupPoint, turn);
                ListTag points = new ListTag();
                points.add(pickupPoint.copy());
                points.add(feedingPointTag());
                ((ArmBlockEntityAccessor) this).createrancher$setInteractionPointTag(points);
            }
        }
        super.initInteractionPoints();
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (pickupPoint != null)
            tag.put("PickupPoint", pickupPoint);
        if (feedTarget != null)
            tag.putUUID("FeedTarget", feedTarget);
        if (!clientPacket)
            Moves.saveOrigin(tag, worldPosition);
        if (clientPacket) {
            Entity target = level instanceof ServerLevel serverLevel && feedTarget != null
                    ? serverLevel.getEntity(feedTarget) : null;
            tag.putInt("FeedTargetId", target == null ? -1 : target.getId());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        pickupPoint = tag.contains("PickupPoint") ? tag.getCompound("PickupPoint") : null;
        if (pickupPoint != null) {
            ListTag points = new ListTag();
            points.add(pickupPoint.copy());
            points.add(feedingPointTag());
            tag.put("InteractionPoints", points);
        }
        super.read(tag, registries, clientPacket);
        if (!clientPacket)
            moved = Moves.moved(tag, worldPosition);
        feedTarget = tag.hasUUID("FeedTarget") ? tag.getUUID("FeedTarget") : null;
        if (clientPacket)
            feedTargetId = tag.getInt("FeedTargetId");
    }
}
