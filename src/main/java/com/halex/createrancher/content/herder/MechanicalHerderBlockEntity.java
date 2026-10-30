package com.halex.createrancher.content.herder;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.halex.createrancher.compat.sable.Moves;
import com.halex.createrancher.compat.sable.SubLevels;
import com.halex.createrancher.index.LangRegistry;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MechanicalHerderBlockEntity extends KineticBlockEntity {

    public static final int RANGE = 7;
    public static final int[] AREA_SIZES = {3, 5, 7};
    public static final int DEFAULT_AREA_SIZE = 5;
    public static final int AREA_HEIGHT = 3;
    public static final double CARRY_LIFT = 4;

    private static final int REACQUIRE_TIMEOUT = 60;
    private static final int DROP_OFF_WAIT_TIMEOUT = 100;
    private static final double GRAB_DISTANCE = .05;
    private static final double CHASE_PROGRESS = .05;
    private static final int CHASE_TIMEOUT = 100;
    private static final int SYNC_INTERVAL = 10;
    private static final int MAX_CATCH_UP = 10;
    private static final double CLIENT_CORRECTION_RATE = .5;
    private static final double CLIENT_SNAP_DISTANCE = 3;
    private static final int MAX_LANDING_TICKS = 5;
    private static final int RELEASE_SETTLE_TICKS = 6;

    public enum Phase {
        IDLE, MOVE_TO_MOB, MOVE_TO_DROP_OFF,
        RETURN_TO_PICKUP;

        public boolean isCarrying() {
            return this == MOVE_TO_DROP_OFF || this == RETURN_TO_PICKUP;
        }
    }

    public enum AgeFilter {
        ANY, BABIES, ADULTS;

        public String getTranslationKey() {
            return "createrancher.gui.mechanical_herder.age." + Lang.asId(name());
        }

        public boolean test(LivingEntity entity) {
            return switch (this) {
                case ANY -> true;
                case BABIES -> entity.isBaby();
                case ADULTS -> !entity.isBaby();
            };
        }
    }

    public enum DropOffStatus {
        NONE, VALID, TOO_FAR, IN_AREA, INVALID_TARGET;

        // Tooltip explaining the problem
        public String getHintKey() {
            return "createrancher.hint.drop_off." + Lang.asId(name());
        }

        // Short message for the action bar
        public String getMessageKey() {
            return "createrancher.message.drop_off." + Lang.asId(name());
        }
    }

    @Nullable
    private ResourceLocation mobFilter;
    private AgeFilter ageFilter;
    private int areaSize;
    @Nullable
    private BlockPos dropOff;

    private Phase phase;
    @Nullable
    private Vec3 plunger;
    @Nullable
    private Vec3 destination;
    private double minTravelHeight;
    private double routeHeight = Double.NEGATIVE_INFINITY;
    private double riseAfterDropOff = Double.NEGATIVE_INFINITY;
    @Nullable
    private UUID targetMob;
    @Nullable
    private Vec3 pickupPoint;
    private float carriedYawOffset;
    private boolean redstoneLocked;

    private int reacquireTicks;
    private int dropOffWaitTicks;
    private double bestChaseDistance;
    private int ticksWithoutChaseProgress;
    private int syncTimer;

    private int targetEntityId;
    private int releasedEntityId = -1;
    @Nullable
    private Vec3 releasedAt;
    private int releaseSettleTicks;
    @Nullable
    private Vec3 serverShadow;
    @Nullable
    private Vec3 shadowOffset;
    private int landingTicks;
    @Nullable
    private Entity presentedMob;

    @Nullable
    private HerderArmPose currentPose;
    final LerpedFloat yaw;
    final LerpedFloat shoulderAngle;
    final LerpedFloat bendAngle;

    public MechanicalHerderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        ageFilter = AgeFilter.ANY;
        areaSize = DEFAULT_AREA_SIZE;
        phase = Phase.IDLE;
        minTravelHeight = Double.NEGATIVE_INFINITY;
        targetEntityId = -1;
        yaw = LerpedFloat.angular();
        shoulderAngle = LerpedFloat.linear();
        bendAngle = LerpedFloat.linear();
        setLazyTickRate(10);
    }

    @Override
    public void tick() {
        super.tick();
        if (phase == Phase.MOVE_TO_MOB)
            followTarget();
        tickArm();

        if (level.isClientSide) {
            tickCarriedClient();
            return;
        }

        if (plunger != null && plunger.y >= riseAfterDropOff - 1e-4)
            riseAfterDropOff = Double.NEGATIVE_INFINITY;

        switch (phase) {
            case MOVE_TO_MOB -> tickMoveToMob();
            case MOVE_TO_DROP_OFF, RETURN_TO_PICKUP -> tickCarrying();
            default -> {
            }
        }

        if (phase != Phase.IDLE || !hasArrived()) {
            if (++syncTimer >= SYNC_INTERVAL) {
                syncTimer = 0;
                sendData();
            }
        } else {
            syncTimer = 0;
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!level.isClientSide)
            checkDropOffStillThere();
        if (level.isClientSide || phase != Phase.IDLE)
            return;
        if (hasArrived() && destination != null && destination.distanceToSqr(getRestPoint()) > 1e-4)
            returnToRest();
        searchForMob();
    }

    // Drops the drop-off if it's gone or on another sub-level
    private void checkDropOffStillThere() {
        if (dropOff == null)
            return;
        boolean onSubLevel = SubLevels.isOnSubLevel(level, worldPosition);
        boolean loaded = level.isLoaded(dropOff);
        if (!loaded && !onSubLevel)
            return;
        boolean gone = onSubLevel && (!loaded || level.getBlockState(dropOff)
                .isAir());
        if (!gone && SubLevels.sameSubLevel(level, worldPosition, dropOff))
            return;
        dropOff = null;
        sendData();
        setChanged();
    }

    private void tickArm() {
        if (plunger == null)
            plunger = getRestPoint();
        if (destination == null)
            destination = plunger;

        if (serverShadow != null)
            tickPickupOnClient();
        else
            stepPlunger();

        float fallbackYaw = currentPose == null ? facingYaw() : currentPose.yaw;
        currentPose = HerderArmPose.reaching(worldPosition, plunger, fallbackYaw);
        yaw.setValue(currentPose.yaw);
        shoulderAngle.setValue(currentPose.shoulderAngle);
        bendAngle.setValue(currentPose.bendAngle);
    }

    private void catchUp(int ticks) {
        for (int i = 0; i < ticks; i++) {
            if (phase == Phase.MOVE_TO_MOB)
                followTarget();
            stepPlunger();
        }
    }

    private void stepPlunger() {
        float speed = getArmSpeed();
        if (speed > 0 && !hasArrived() && plunger != null) {
            plunger = HerderSteering.step(plunger, destination, minTravelHeight, axisX(), axisZ(), speed);
            climbed(plunger);
        }
    }

    // Marks the climb as done once the plunger reaches travel height
    private void climbed(Vec3 position) {
        if (position.y >= minTravelHeight - 1e-4)
            minTravelHeight = Double.NEGATIVE_INFINITY;
    }

    // Pickup on the client
    private void tickPickupOnClient() {
        float speed = getArmSpeed();
        if (speed > 0) {
            serverShadow = HerderSteering.step(serverShadow, destination, minTravelHeight, axisX(), axisZ(), speed);
            climbed(serverShadow);
        }

        if (landingTicks > 0) {
            Entity mob = level.getEntity(targetEntityId);
            Vec3 mobTop = mob == null ? null : local(mob.position()
                    .add(0, mob.getBbHeight(), 0));
            if (mobTop != null && speed > 0 && plunger != null)
                plunger = HerderSteering.step(plunger, mobTop, Double.NEGATIVE_INFINITY, axisX(), axisZ(), speed);
            if (mobTop == null || plunger.distanceToSqr(mobTop) < 1e-8 || --landingTicks <= 0)
                landingTicks = 0;
            return;
        }

        if (shadowOffset == null)
            shadowOffset = plunger.subtract(serverShadow);
        shadowOffset = shadowOffset.scale(1 - CLIENT_CORRECTION_RATE);
        plunger = serverShadow.add(shadowOffset);
        if (shadowOffset.lengthSqr() < 1e-6) {
            plunger = serverShadow;
            serverShadow = null;
            shadowOffset = null;
        }
    }

    // Plunger speed in blocks per tick
    private float getArmSpeed() {
        return Math.min(256, Math.abs(getSpeed())) / 160f;
    }

    private boolean hasArrived() {
        return plunger == null || destination == null || plunger.distanceToSqr(destination) < 1e-8;
    }

    // Follows the mob being chased
    private void followTarget() {
        Entity target = level.isClientSide ? getClientTarget() : findTargetMob();
        if (target != null)
            destination = local(target.position()
                    .add(0, target.getBbHeight(), 0));
    }

    // A world position in the herder's frame
    private Vec3 local(Vec3 world) {
        return SubLevels.toLocal(level, worldPosition, world);
    }

    // Turns a position in the herder's frame into a world position
    private Vec3 world(Vec3 local) {
        return SubLevels.toWorld(level, worldPosition, local);
    }

    // The resting position
    private Vec3 getRestPoint() {
        return VecHelper.getCenterOf(worldPosition)
                .add(Vec3.atLowerCornerOf(getFacing().getNormal())
                        .scale(1 + areaSize / 2))
                .add(0, 3.5, 0);
    }

    // Heads for a destination
    private void startMovement(Phase phase, Vec3 destination, double minTravelHeight) {
        this.phase = phase;
        this.destination = destination;
        this.minTravelHeight = minTravelHeight;
        routeHeight = minTravelHeight;
        bestChaseDistance = Double.MAX_VALUE;
        ticksWithoutChaseProgress = 0;
        syncTimer = 0;
        sendData();
        setChanged();
    }

    private void returnToRest() {
        releaseClaim();
        targetMob = null;
        startMovement(Phase.IDLE, getRestPoint(), riseAfterDropOff);
    }

    private float facingYaw() {
        Direction facing = getFacing();
        return AngleHelper.deg(Mth.atan2(-facing.getStepX(), -facing.getStepZ()));
    }

    private double axisX() {
        return worldPosition.getX() + .5;
    }

    private double axisZ() {
        return worldPosition.getZ() + .5;
    }

    private void searchForMob() {
        if (redstoneLocked || getSpeed() == 0 || getDropOffStatus() != DropOffStatus.VALID)
            return;
        Vec3 dropPoint = getDropPoint();
        if (dropPoint == null)
            return;

        AABB area = getSearchArea();
        List<Mob> candidates = level.getEntitiesOfClass(Mob.class, area, mob -> canGrab(mob)
                && area.contains(local(mob.position()))
                && fitsAt(mob, dropPoint)
                && !HerderTracker.isClaimed(mob.getUUID()));

        Vec3 center = VecHelper.getCenterOf(worldPosition);
        candidates.stream()
                .min(Comparator.comparingDouble(mob -> local(mob.position()).distanceToSqr(center)))
                .ifPresent(mob -> {
                    if (!HerderTracker.claim(mob.getUUID(), worldPosition))
                        return;
                    targetMob = mob.getUUID();
                    startMovement(Phase.MOVE_TO_MOB, local(mob.position()
                            .add(0, mob.getBbHeight(), 0)), riseAfterDropOff);
                });
    }

    public AABB getSearchArea() {
        return getSearchArea(worldPosition, getFacing(), areaSize);
    }

    // The herder's search area
    public static AABB getSearchArea(BlockPos herder, Direction facing, int size) {
        Direction side = facing.getClockWise();
        int halfWidth = size / 2;
        int halfHeight = AREA_HEIGHT / 2;
        BlockPos near = herder.relative(facing)
                .relative(side, -halfWidth)
                .below(halfHeight);
        BlockPos far = herder.relative(facing, size)
                .relative(side, halfWidth)
                .above(halfHeight);
        return AABB.encapsulatingFullBlocks(near, far);
    }

    public boolean canGrab(Mob mob) {
        if (!mob.isAlive() || mob.isPassenger() || mob.isVehicle())
            return false;
        if (HerderCreatures.isExcluded(mob.getType()))
            return false;
        if (mobFilter != null && !mobFilter.equals(EntityType.getKey(mob.getType())))
            return false;
        return ageFilter.test(mob);
    }

    // Whether the mob has room to stand there
    private boolean fitsAt(Mob mob, Vec3 feet) {
        return level.noCollision(mob, mob.getDimensions(mob.getPose())
                .makeBoundingBox(feet));
    }

    private void tickMoveToMob() {
        Mob mob = findTargetMob();
        if (mob == null || !canGrab(mob) || redstoneLocked) {
            returnToRest();
            return;
        }

        Vec3 dropPoint = getDropPoint();
        if (dropPoint == null || getDropOffStatus() != DropOffStatus.VALID || !getSearchArea().inflate(1)
                .contains(local(mob.position()))) {
            returnToRest();
            return;
        }

        Vec3 mobTop = local(mob.position()
                .add(0, mob.getBbHeight(), 0));
        double distance = mobTop.distanceTo(currentPose.plungerTip(worldPosition));
        if (distance > GRAB_DISTANCE) {
            if (distance < bestChaseDistance - CHASE_PROGRESS) {
                bestChaseDistance = distance;
                ticksWithoutChaseProgress = 0;
            } else if (++ticksWithoutChaseProgress > CHASE_TIMEOUT) {
                returnToRest();
            }
            return;
        }

        carriedYawOffset = mob.getYRot() - currentPose.yaw;
        pickupPoint = local(mob.position());
        dropOffWaitTicks = 0;
        double height = mob.getBbHeight();
        startMovement(Phase.MOVE_TO_DROP_OFF, dropPoint.add(0, height, 0),
                Math.max(pickupPoint.y, dropPoint.y) + CARRY_LIFT + height);
        holdEntity(mob);
        level.playSound(null, worldPosition, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, .5f, 1.2f);
    }

    private void tickCarrying() {
        Mob mob = findTargetMob();
        if (mob == null) {
            if (isCarriedMobGone() || ++reacquireTicks > REACQUIRE_TIMEOUT)
                returnToRest();
            return;
        }
        reacquireTicks = 0;
        if (!mob.isAlive()) {
            returnToRest();
            return;
        }

        holdEntity(mob);
        mob.getNavigation()
                .stop();

        if (!hasArrived())
            return;

        if (phase == Phase.RETURN_TO_PICKUP) {
            letGo(mob, pickupPoint == null ? local(mob.position()) : pickupPoint);
            return;
        }

        Vec3 dropPoint = getDropPoint();
        if (dropPoint == null) {
            letGo(mob, local(mob.position()));
            return;
        }

        if (!fitsAt(mob, dropPoint)) {
            if (++dropOffWaitTicks > DROP_OFF_WAIT_TIMEOUT && pickupPoint != null) {
                double height = mob.getBbHeight();
                startMovement(Phase.RETURN_TO_PICKUP, pickupPoint.add(0, height, 0),
                        Math.max(pickupPoint.y, dropPoint.y) + CARRY_LIFT + height);
            }
            return;
        }

        letGo(mob, dropPoint);
        level.playSound(null, BlockPos.containing(dropPoint), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.BLOCKS, .5f,
                1.4f);
    }

    // Whether the carried mob is gone
    private boolean isCarriedMobGone() {
        return level instanceof ServerLevel serverLevel && plunger != null
                && serverLevel.areEntitiesLoaded(ChunkPos.asLong(BlockPos.containing(world(plunger))));
    }

    private void letGo(Mob mob, Vec3 position) {
        Vec3 at = world(position);
        mob.moveTo(at.x, at.y, at.z, mob.getYRot(), mob.getXRot());
        mob.setDeltaMovement(Vec3.ZERO);
        mob.resetFallDistance();
        riseAfterDropOff = routeHeight;
        returnToRest();
    }

    // Holds the mob under the plunger
    private void holdEntity(Entity entity) {
        Vec3 tip = world(currentPose.plungerTip(worldPosition));
        entity.setPos(tip.x, tip.y - entity.getBbHeight(), tip.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.resetFallDistance();

        float entityYaw = currentPose.yaw + carriedYawOffset;
        entity.setYRot(entityYaw);
        entity.setYHeadRot(entityYaw);
        if (entity instanceof LivingEntity living)
            living.yBodyRot = entityYaw;
    }

    private void tickCarriedClient() {
        if (landingTicks > 0) {
            Entity mob = level.getEntity(targetEntityId);
            if (mob != null)
                pinClientEntity(mob, mob.position());
            return;
        }

        if (releaseSettleTicks > 0) {
            releaseSettleTicks--;
            Entity released = level.getEntity(releasedEntityId);
            if (released != null && releasedAt != null)
                pinClientEntity(released, releasedAt);
        }

        if (!phase.isCarrying())
            return;
        Entity entity = getClientTarget();
        if (entity == null)
            return;
        if (presentedMob != null)
            entity.setOldPosAndRot();
        holdEntity(entity);
    }

    @Nullable
    private Entity getClientTarget() {
        if (presentedMob != null)
            return presentedMob;
        return targetEntityId == -1 ? null : level.getEntity(targetEntityId);
    }

    // Moves the arm in Ponder scenes
    public void present(Phase phase, Vec3 destination, double minTravelHeight, @Nullable Entity mob) {
        this.phase = phase;
        this.destination = destination;
        this.minTravelHeight = minTravelHeight;
        presentedMob = mob;
        serverShadow = null;
        shadowOffset = null;
        landingTicks = 0;
    }

    // Sends the arm back to rest in Ponder scenes
    public void presentRest() {
        present(Phase.IDLE, getRestPoint(), Double.NEGATIVE_INFINITY, null);
    }

    // Holds a client side entity in place
    private static void pinClientEntity(Entity entity, Vec3 position) {
        entity.setPos(position);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.lerpTo(position.x, position.y, position.z, entity.getYRot(), entity.getXRot(), 0);
    }

    @Nullable
    private Mob findTargetMob() {
        if (targetMob == null || !(level instanceof ServerLevel serverLevel))
            return null;
        if (!(serverLevel.getEntity(targetMob) instanceof Mob mob))
            return null;
        HerderTracker.claim(targetMob, worldPosition);
        return mob;
    }

    private void releaseClaim() {
        if (targetMob != null && !level.isClientSide)
            HerderTracker.release(targetMob, worldPosition);
    }

    @Nullable
    public BlockPos getDropOff() {
        return dropOff;
    }

    public DropOffStatus getDropOffStatus() {
        return checkDropOff(level, worldPosition, getFacing(), areaSize, dropOff);
    }

    // Whether the drop-off can be used
    public static DropOffStatus checkDropOff(@Nullable Level level, BlockPos herder, Direction facing, int areaSize,
                                             @Nullable BlockPos dropOff) {
        if (dropOff == null)
            return DropOffStatus.NONE;
        if (!SubLevels.sameSubLevel(level, herder, dropOff))
            return DropOffStatus.INVALID_TARGET;
        if (herder.distSqr(dropOff) > (RANGE + .5) * (RANGE + .5))
            return DropOffStatus.TOO_FAR;
        AABB area = getSearchArea(herder, facing, areaSize);
        if (area.contains(Vec3.atCenterOf(dropOff)) || area.contains(Vec3.atCenterOf(dropOff.above())))
            return DropOffStatus.IN_AREA;
        return DropOffStatus.VALID;
    }

    public DropOffStatus setDropOff(BlockPos dropOff) {
        DropOffStatus status = checkDropOff(level, worldPosition, getFacing(), areaSize, dropOff);
        if (status != DropOffStatus.VALID)
            return status;
        this.dropOff = dropOff;
        sendData();
        setChanged();
        return status;
    }

    // Where a mob gets set down
    @Nullable
    public Vec3 getDropPoint() {
        if (dropOff == null || !level.isLoaded(dropOff))
            return null;
        VoxelShape shape = level.getBlockState(dropOff)
                .getCollisionShape(level, dropOff);
        double top = shape.isEmpty() ? 0 : shape.max(Direction.Axis.Y);
        return Vec3.atBottomCenterOf(dropOff)
                .add(0, top, 0);
    }

    @Nullable
    public ResourceLocation getMobFilter() {
        return mobFilter;
    }

    public AgeFilter getAgeFilter() {
        return ageFilter;
    }

    public int getAreaSize() {
        return areaSize;
    }

    public static boolean isValidAreaSize(int size) {
        for (int valid : AREA_SIZES)
            if (valid == size)
                return true;
        return false;
    }

    public void applySettings(@Nullable ResourceLocation mobFilter, AgeFilter ageFilter, int areaSize) {
        if (mobFilter != null && !BuiltInRegistries.ENTITY_TYPE.containsKey(mobFilter))
            mobFilter = null;
        this.mobFilter = mobFilter;
        this.ageFilter = ageFilter;
        if (isValidAreaSize(areaSize)
                && checkDropOff(level, worldPosition, getFacing(), areaSize, dropOff) != DropOffStatus.IN_AREA)
            this.areaSize = areaSize;
        sendData();
        setChanged();
    }

    public Direction getFacing() {
        return getBlockState().getValue(MechanicalHerderBlock.HORIZONTAL_FACING);
    }

    public boolean isCarrying() {
        return phase.isCarrying();
    }

    // Whether it's reaching for a mob to pick up
    public boolean isChasing() {
        return phase == Phase.MOVE_TO_MOB;
    }

    @Nullable
    public Vec3 getPlungerTip() {
        return currentPose == null ? null : currentPose.plungerTip(worldPosition);
    }

    public void redstoneUpdate() {
        if (level.isClientSide)
            return;
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (powered == redstoneLocked)
            return;
        redstoneLocked = powered;
        sendData();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (!phase.isCarrying())
            return;
        Mob mob = findTargetMob();
        if (mob != null)
            mob.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        releaseClaim();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(worldPosition).inflate(HerderArmPose.SEGMENTS * HerderArmPose.SEGMENT_LENGTH + 2);
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (super.addToTooltip(tooltip, isPlayerSneaking))
            return true;
        DropOffStatus status = getDropOffStatus();
        if (isPlayerSneaking || status == DropOffStatus.VALID)
            return false;
        LangRegistry.builder()
                .translate("hint.drop_off.title")
                .style(ChatFormatting.GOLD)
                .forGoggles(tooltip);
        Component hint = Component.translatable(status.getHintKey(), RANGE);
        for (Component line : FontHelper.cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE))
            LangRegistry.builder()
                    .add(line)
                    .forGoggles(tooltip);
        return true;
    }

    // Puts the arm back at rest after the herder was moved
    private void restAfterMove() {
        phase = Phase.IDLE;
        plunger = null;
        destination = null;
        pickupPoint = null;
        targetMob = null;
        minTravelHeight = Double.NEGATIVE_INFINITY;
        routeHeight = Double.NEGATIVE_INFINITY;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (mobFilter != null)
            tag.putString("MobFilter", mobFilter.toString());
        NBTHelper.writeEnum(tag, "AgeFilter", ageFilter);
        tag.putInt("AreaSize", areaSize);
        if (dropOff != null)
            tag.putLong("DropOffOffset", dropOff.subtract(worldPosition)
                    .asLong());
        if (!clientPacket) {
            Moves.saveOrigin(tag, worldPosition);
            Moves.saveFacing(tag, getFacing());
        }

        NBTHelper.writeEnum(tag, "Phase", phase);
        if (plunger != null)
            writeVec(tag, "Plunger", plunger);
        if (destination != null)
            writeVec(tag, "Destination", destination);
        if (minTravelHeight != Double.NEGATIVE_INFINITY)
            tag.putDouble("MinTravelHeight", minTravelHeight);
        if (!clientPacket && routeHeight != Double.NEGATIVE_INFINITY)
            tag.putDouble("RouteHeight", routeHeight);
        if (targetMob != null)
            tag.putUUID("TargetMob", targetMob);
        if (pickupPoint != null)
            writeVec(tag, "PickupPoint", pickupPoint);
        tag.putFloat("CarriedYaw", carriedYawOffset);
        tag.putBoolean("Powered", redstoneLocked);

        if (clientPacket) {
            Mob target = phase == Phase.IDLE ? null : findTargetMob();
            tag.putInt("TargetId", target == null ? -1 : target.getId());
            tag.putLong("SyncTime", level.getGameTime());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        mobFilter = tag.contains("MobFilter") ? ResourceLocation.tryParse(tag.getString("MobFilter")) : null;
        ageFilter = NBTHelper.readEnum(tag, "AgeFilter", AgeFilter.class);
        int savedSize = tag.getInt("AreaSize");
        areaSize = isValidAreaSize(savedSize) ? savedSize : DEFAULT_AREA_SIZE;
        if (tag.contains("DropOffOffset"))
            dropOff = worldPosition.offset(BlockPos.of(tag.getLong("DropOffOffset"))
                    .rotate(clientPacket ? Rotation.NONE : Moves.turn(tag, getFacing())));
        else
            dropOff = tag.contains("DropOff") ? BlockPos.of(tag.getLong("DropOff")) : null;

        Phase previousPhase = phase;
        int previousTargetId = targetEntityId;
        phase = NBTHelper.readEnum(tag, "Phase", Phase.class);
        destination = tag.contains("DestinationX") ? readVec(tag, "Destination") : null;
        minTravelHeight = tag.contains("MinTravelHeight") ? tag.getDouble("MinTravelHeight")
                : Double.NEGATIVE_INFINITY;
        if (!clientPacket)
            routeHeight = tag.contains("RouteHeight") ? tag.getDouble("RouteHeight") : minTravelHeight;
        targetMob = tag.hasUUID("TargetMob") ? tag.getUUID("TargetMob") : null;
        pickupPoint = tag.contains("PickupPointX") ? readVec(tag, "PickupPoint") : null;
        carriedYawOffset = tag.getFloat("CarriedYaw");
        redstoneLocked = tag.getBoolean("Powered");

        Vec3 clientPlunger = plunger;
        Vec3 serverPlunger = tag.contains("PlungerX") ? readVec(tag, "Plunger") : null;
        plunger = serverPlunger;
        if (!clientPacket) {
            if (Moves.moved(tag, worldPosition))
                restAfterMove();
            return;
        }

        targetEntityId = tag.getInt("TargetId");
        if (!hasLevel() || serverPlunger == null || clientPlunger == null) {
            serverShadow = null;
            shadowOffset = null;
            landingTicks = 0;
            return;
        }

        int behind = (int) Mth.clamp(level.getGameTime() - tag.getLong("SyncTime"), 0, MAX_CATCH_UP);
        boolean closeToServer = clientPlunger.distanceTo(serverPlunger) < CLIENT_SNAP_DISTANCE;

        if (phase == Phase.MOVE_TO_MOB && previousPhase == Phase.MOVE_TO_MOB && closeToServer) {
            plunger = clientPlunger;
        } else {
            catchUp(behind);
            boolean pickedUp = previousPhase == Phase.MOVE_TO_MOB && phase == Phase.MOVE_TO_DROP_OFF;
            boolean stillEasingIn = serverShadow != null && phase.isCarrying();
            if ((pickedUp || stillEasingIn) && closeToServer) {
                serverShadow = plunger;
                shadowOffset = null;
                plunger = clientPlunger;
                if (pickedUp)
                    landingTicks = MAX_LANDING_TICKS;
            } else {
                serverShadow = null;
                shadowOffset = null;
                landingTicks = 0;
            }
        }

        if (previousPhase.isCarrying() && !phase.isCarrying() && previousTargetId != -1) {
            Entity released = level.getEntity(previousTargetId);
            if (released != null) {
                releasedEntityId = previousTargetId;
                releasedAt = released.position();
                releaseSettleTicks = RELEASE_SETTLE_TICKS;
            }
        }
    }

    private static void writeVec(CompoundTag tag, String key, Vec3 vec) {
        tag.putDouble(key + "X", vec.x);
        tag.putDouble(key + "Y", vec.y);
        tag.putDouble(key + "Z", vec.z);
    }

    private static Vec3 readVec(CompoundTag tag, String key) {
        return new Vec3(tag.getDouble(key + "X"), tag.getDouble(key + "Y"), tag.getDouble(key + "Z"));
    }
}
