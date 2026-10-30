package com.halex.createrancher.content.fisher;

import java.util.List;

import javax.annotation.Nullable;

import com.halex.createrancher.compat.sable.SubLevels;
import com.halex.createrancher.CommonConfig;
import com.halex.createrancher.compat.sable.Moves;
import com.halex.createrancher.content.net.FishingNetEntity;
import com.halex.createrancher.content.net.FishingNetItem;
import com.halex.createrancher.index.EntityRegistry;
import com.halex.createrancher.index.LangRegistry;
import com.halex.createrancher.mixin.FishingHookAccessor;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

// Fishes with a Fishing Net
public class MechanicalFisherBlockEntity extends KineticBlockEntity implements Clearable {

    public static final int RANGE = 16;
    public static final int OUTPUT_SLOTS = 9;
    private static final int COOLDOWN = 10;
    public static final int FLIGHT_TICKS = 32;
    public static final int REEL_TICKS = 24;
    public static final int REACTION_TICKS = 6;

    public enum Phase {
        IDLE, LAUNCHING, FISHING, REELING
    }

    public enum LinkStatus {
        LINKED, TOO_FAR, NOT_FISHABLE, INVALID_TARGET;

        public String getMessageKey() {
            return "createrancher.message.fisher." + name().toLowerCase();
        }
    }

    @Nullable
    private BlockPos spot;
    private ItemStack net = ItemStack.EMPTY;
    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler itemHandler = new FisherItemHandler();

    private Phase phase = Phase.IDLE;
    private int phaseTicks;
    private int waitTicks;
    private int approachTicks;
    private float fishAngle;
    private int reactionTicks;
    private boolean biting;
    private boolean netWornOut;
    private int cooldown;
    private boolean redstoneLocked;
    private int animationTicks;

    public MechanicalFisherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public LinkStatus setSpot(BlockPos surface) {
        if (!FishingSpots.isFishable(level, surface))
            return LinkStatus.NOT_FISHABLE;
        if (!SubLevels.sameSubLevel(level, worldPosition, surface))
            return LinkStatus.INVALID_TARGET;
        if (!isInRange(surface))
            return LinkStatus.TOO_FAR;
        spot = surface.immutable();
        invalidateRenderBoundingBox();
        setChanged();
        sendData();
        return LinkStatus.LINKED;
    }

    private boolean isInRange(BlockPos surface) {
        return Vec3.atCenterOf(worldPosition)
                .closerThan(Vec3.atCenterOf(surface), RANGE + .5);
    }

    @Nullable
    public BlockPos getSpot() {
        return spot;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!level.isClientSide)
            checkSpotStillThere();
    }

    // Drops the fishing spot if it's gone or on another sub-level
    private void checkSpotStillThere() {
        if (spot == null)
            return;
        boolean onSubLevel = SubLevels.isOnSubLevel(level, worldPosition);
        boolean loaded = level.isLoaded(spot);
        if (!loaded && !onSubLevel)
            return;
        boolean gone = onSubLevel && (!loaded || !FishingSpots.isFishable(level, spot));
        if (!gone && SubLevels.sameSubLevel(level, worldPosition, spot))
            return;
        spot = null;
        invalidateRenderBoundingBox();
        setChanged();
        sendData();
    }

    private boolean canFishAtSpot() {
        return spot != null && isInRange(spot) && FishingSpots.isFishable(level, spot);
    }

    // Where the net floats
    public Vec3 getNetPosition() {
        return spot == null ? Vec3.atCenterOf(worldPosition) : Vec3.atBottomCenterOf(spot)
                .add(0, level == null ? .9 : Math.max(.1, level.getFluidState(spot)
                        .getHeight(level, spot)), 0);
    }

    public boolean hasNet() {
        return !net.isEmpty();
    }

    public ItemStack getNet() {
        return net;
    }

    // Loads a net
    public boolean insertNet(ItemStack stack) {
        if (!(stack.getItem() instanceof FishingNetItem) || hasNet() || phase != Phase.IDLE)
            return false;
        net = stack.copyWithCount(1);
        netChanged();
        return true;
    }

    // Takes the net off
    public ItemStack takeNet() {
        if (net.isEmpty())
            return ItemStack.EMPTY;
        if (phase != Phase.IDLE) {
            biting = false;
            cooldown = COOLDOWN;
            setPhase(Phase.IDLE);
            level.playSound(null, worldPosition, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.BLOCKS, .8f, .8f);
        }
        ItemStack taken = net;
        net = ItemStack.EMPTY;
        netWornOut = false;
        netChanged();
        return taken;
    }

    private void netChanged() {
        setChanged();
        sendData();
    }

    public ItemStackHandler getOutput() {
        return output;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    // Gives the player the catch
    public boolean giveCatches(Player player) {
        boolean gave = false;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack stack = output.extractItem(slot, 64, false);
            if (stack.isEmpty())
                continue;
            player.getInventory()
                    .placeItemBackInInventory(stack);
            gave = true;
        }
        return gave;
    }

    private int freeSlots() {
        int free = 0;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++)
            if (output.getStackInSlot(slot)
                    .isEmpty())
                free++;
        return free;
    }

    public Phase getPhase() {
        return phase;
    }

    public int getPhaseTicks() {
        return phaseTicks;
    }

    // Same stress at any speed, even when stopped
    @Override
    public float calculateStressApplied() {
        float speed = Math.max(1, Math.abs(getTheoreticalSpeed()));
        float stress = CommonConfig.MECHANICAL_FISHER_STRESS.get()
                .floatValue() / speed;
        lastStressApplied = stress;
        return stress;
    }

    // Shows how to set a fishing spot when it has none
    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (spot != null || isPlayerSneaking)
            return super.addToTooltip(tooltip, isPlayerSneaking);
        LangRegistry.builder()
                .translate("hint.fisher.no_spot.title")
                .style(ChatFormatting.GOLD)
                .forGoggles(tooltip);
        for (Component line : FontHelper.cutTextComponent(Component.translatable("createrancher.hint.fisher.no_spot",
                RANGE), FontHelper.Palette.GRAY_AND_WHITE))
            LangRegistry.builder()
                    .add(line)
                    .forGoggles(tooltip);
        return true;
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide || !hasNetwork())
            return;
        getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        getOrCreateNetwork().updateStress();
    }

    // How long the net takes to fly out
    public int getFlightTicks() {
        return FLIGHT_TICKS;
    }

    // How long it takes to haul the net back in
    public int getReelTicks() {
        return REEL_TICKS;
    }

    public boolean isBiting() {
        return biting;
    }

    public int getAnimationTicks() {
        return animationTicks;
    }

    // Sets what it's doing in Ponder scenes
    public void presentPhase(Phase phase, boolean biting) {
        this.phase = phase;
        this.biting = biting;
        phaseTicks = 0;
    }

    // Loads a net in Ponder scenes
    public void presentNet(ItemStack net) {
        this.net = net.copyWithCount(Math.min(1, net.getCount()));
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
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            animationTicks++;
            if (getSpeed() != 0 && phase != Phase.IDLE)
                phaseTicks++;
            return;
        }

        float speed = Math.abs(getSpeed());
        if (phase == Phase.IDLE) {
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (speed == 0 || redstoneLocked || !hasNet() || freeSlots() < FishingNetEntity.MAX_CATCHES
                    || !canFishAtSpot())
                return;
            setPhase(Phase.LAUNCHING);
            level.playSound(null, worldPosition, SoundEvents.FISHING_BOBBER_THROW, SoundSource.BLOCKS, .8f, .6f);
            return;
        }

        if (speed == 0 || !canFishAtSpot()) {
            recall();
            return;
        }
        phaseTicks++;
        switch (phase) {
            case LAUNCHING -> {
                if (phaseTicks >= FLIGHT_TICKS)
                    startFishing();
            }
            case FISHING -> tickFishing();
            case REELING -> {
                if (phaseTicks >= getReelTicks())
                    finishReeling();
            }
            default -> {
            }
        }
    }

    private void setPhase(Phase phase) {
        this.phase = phase;
        phaseTicks = 0;
        setChanged();
        sendData();
    }

    private void startFishing() {
        waitTicks = Math.max(1, Mth.nextInt(level.random, 100, 600) - lureTicks());
        approachTicks = 0;
        biting = false;
        setPhase(Phase.FISHING);
        Vec3 at = getNetPosition();
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_SPLASH, SoundSource.BLOCKS, .5f, 1.2f);
    }

    private void tickFishing() {
        if (biting) {
            if (--reactionTicks <= 0) {
                haulIn();
                reelIn();
            }
            return;
        }

        int countdown = 1;
        if (level.random.nextFloat() < .25f && level.isRainingAt(spot.above()))
            countdown++;
        if (level.random.nextFloat() < .5f && !level.canSeeSky(spot.above()))
            countdown--;
        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 at = getNetPosition();
        if (waitTicks > 0) {
            waitTicks -= countdown;
            splashNearby(serverLevel, at);
            if (waitTicks <= 0) {
                fishAngle = Mth.nextFloat(level.random, 0, 360);
                approachTicks = Mth.nextInt(level.random, 20, 80);
            }
            return;
        }
        approachTicks -= countdown;
        if (approachTicks > 0) {
            swimTowards(serverLevel, at);
            return;
        }

        biting = true;
        reactionTicks = REACTION_TICKS;
        sendData();
        serverLevel.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 20, 1, 0, 1, .2);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.BLOCKS, .5f, 1.1f);
    }

    // Splashes around the net while it waits for a bite
    private void splashNearby(ServerLevel level, Vec3 at) {
        float chance = .15f;
        if (waitTicks < 20)
            chance += (20 - waitTicks) * .05f;
        else if (waitTicks < 40)
            chance += (40 - waitTicks) * .02f;
        else if (waitTicks < 60)
            chance += (60 - waitTicks) * .01f;
        if (level.random.nextFloat() >= chance)
            return;
        float angle = Mth.nextFloat(level.random, 0, 360) * Mth.DEG_TO_RAD;
        float distance = Mth.nextFloat(level.random, 25, 60) * .1f;
        double x = at.x + Mth.sin(angle) * distance;
        double y = Mth.floor(at.y) + 1;
        double z = at.z + Mth.cos(angle) * distance;
        if (level.getBlockState(BlockPos.containing(x, y - 1, z))
                .is(Blocks.WATER))
            level.sendParticles(ParticleTypes.SPLASH, x, y, z, 2 + level.random.nextInt(2), .1, 0, .1, 0);
    }

    // A fish's wake heading for the net
    private void swimTowards(ServerLevel level, Vec3 at) {
        fishAngle += (float) level.random.triangle(0, 9.188);
        float angle = fishAngle * Mth.DEG_TO_RAD;
        float sin = Mth.sin(angle);
        float cos = Mth.cos(angle);
        double x = at.x + sin * approachTicks * .1f;
        double y = Mth.floor(at.y) + 1;
        double z = at.z + cos * approachTicks * .1f;
        if (!level.getBlockState(BlockPos.containing(x, y - 1, z))
                .is(Blocks.WATER))
            return;
        if (level.random.nextFloat() < .15f)
            level.sendParticles(ParticleTypes.BUBBLE, x, y - .1, z, 1, sin, .1, cos, 0);
        float sideways = sin * .04f;
        float along = cos * .04f;
        level.sendParticles(ParticleTypes.FISHING, x, y, z, 0, along, .01, -sideways, 1);
        level.sendParticles(ParticleTypes.FISHING, x, y, z, 0, -along, .01, sideways, 1);
    }

    // Rolls the catch
    private void haulIn() {
        FishingNetEntity hook = hookAtSpot();
        List<ItemStack> caught = hook.catchFish(net, hook);
        for (ItemStack stack : caught) {
            ItemStack left = ItemHandlerHelper.insertItem(output, stack, false);
            if (!left.isEmpty())
                Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + 1,
                        worldPosition.getZ() + .5, left);
        }
        net.hurtAndBreak(1, (ServerLevel) level, null, item -> netWornOut = true);
        setChanged();
    }

    private FishingNetEntity hookAtSpot() {
        FishingNetEntity hook = new FishingNetEntity(EntityRegistry.FISHING_NET.get(), level);
        Vec3 at = getNetPosition();
        hook.setPos(at.x, at.y, at.z);
        FishingHookAccessor accessor = (FishingHookAccessor) hook;
        accessor.createrancher$setLuck(EnchantmentHelper.getFishingLuckBonus((ServerLevel) level, net, hook));
        if (spot != null)
            accessor.createrancher$setOpenWater(accessor.createrancher$calculateOpenWater(spot));
        return hook;
    }

    private int lureTicks() {
        if (net.isEmpty())
            return 0;
        return (int) (EnchantmentHelper.getFishingTimeReduction((ServerLevel) level, net, hookAtSpot()) * 20);
    }

    private void reelIn() {
        biting = false;
        setPhase(Phase.REELING);
        level.playSound(null, worldPosition, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.BLOCKS, .8f, .8f);
    }

    // Brings the net straight back with no catch
    private void recall() {
        biting = false;
        level.playSound(null, worldPosition, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.BLOCKS, .8f, .8f);
        finishReeling();
    }

    private void finishReeling() {
        cooldown = COOLDOWN;
        if (netWornOut) {
            netWornOut = false;
            net = ItemStack.EMPTY;
            level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, .8f, 1);
        }
        setPhase(Phase.IDLE);
    }

    // Drops the net and catch when broken
    @Override
    public void destroy() {
        super.destroy();
        Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5,
                worldPosition.getZ() + .5, net);
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++)
            Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5,
                    worldPosition.getZ() + .5, output.getStackInSlot(slot));
    }

    // Empties it without dropping anything
    @Override
    public void clearContent() {
        net = ItemStack.EMPTY;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++)
            output.setStackInSlot(slot, ItemStack.EMPTY);
    }

    // Lets the net render far from the block
    @Override
    protected AABB createRenderBoundingBox() {
        AABB box = new AABB(worldPosition);
        if (spot != null)
            box = box.minmax(new AABB(spot));
        return box.inflate(3);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (spot != null)
            tag.put("SpotOffset", NbtUtils.writeBlockPos(spot.subtract(worldPosition)));
        if (!clientPacket)
            Moves.saveFacing(tag, getBlockState().getValue(MechanicalFisherBlock.HORIZONTAL_FACING));
        if (!net.isEmpty())
            tag.put("Net", net.save(registries));
        tag.putString("Phase", phase.name());
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putBoolean("Biting", biting);
        tag.putBoolean("Locked", redstoneLocked);
        if (clientPacket)
            return;
        tag.put("Output", output.serializeNBT(registries));
        tag.putInt("Wait", waitTicks);
        tag.putInt("Approach", approachTicks);
        tag.putFloat("FishAngle", fishAngle);
        tag.putInt("Reaction", reactionTicks);
        tag.putBoolean("WornOut", netWornOut);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        BlockPos oldSpot = spot;
        if (tag.contains("SpotOffset"))
            spot = NbtUtils.readBlockPos(tag, "SpotOffset")
                    .map(offset -> worldPosition.offset(offset.rotate(clientPacket ? Rotation.NONE
                            : Moves.turn(tag, getBlockState().getValue(MechanicalFisherBlock.HORIZONTAL_FACING)))))
                    .orElse(null);
        else
            spot = NbtUtils.readBlockPos(tag, "Spot")
                    .orElse(null);
        if (hasLevel() && (spot == null ? oldSpot != null : !spot.equals(oldSpot)))
            invalidateRenderBoundingBox();
        net = tag.contains("Net") ? ItemStack.parseOptional(registries, tag.getCompound("Net")) : ItemStack.EMPTY;
        try {
            phase = Phase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException e) {
            phase = Phase.IDLE;
        }
        phaseTicks = tag.getInt("PhaseTicks");
        biting = tag.getBoolean("Biting");
        redstoneLocked = tag.getBoolean("Locked");
        if (clientPacket)
            return;
        output.deserializeNBT(registries, tag.getCompound("Output"));
        waitTicks = tag.getInt("Wait");
        approachTicks = tag.getInt("Approach");
        fishAngle = tag.getFloat("FishAngle");
        reactionTicks = tag.getInt("Reaction");
        netWornOut = tag.getBoolean("WornOut");
        cooldown = tag.getInt("Cooldown");
    }

    // The fisher's inventory for hoppers and funnels
    private class FisherItemHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return OUTPUT_SLOTS + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? net : output.getStackInSlot(slot - 1);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !(stack.getItem() instanceof FishingNetItem) || hasNet()
                    || phase != Phase.IDLE)
                return stack;
            if (!simulate)
                insertNet(stack);
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0)
                return ItemStack.EMPTY;
            return output.extractItem(slot - 1, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.getItem() instanceof FishingNetItem;
        }
    }
}
