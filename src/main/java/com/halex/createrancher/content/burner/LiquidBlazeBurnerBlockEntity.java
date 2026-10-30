package com.halex.createrancher.content.burner;

import java.util.List;

import javax.annotation.Nullable;

import com.halex.createrancher.compat.sable.SubLevels;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

// Burns fuel from the burner's tank to heat the blaze
public class LiquidBlazeBurnerBlockEntity extends BlazeBurnerBlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 2000;
    private static final int TOP_UP = 400;
    private static final int QUIET_TICKS = 10;

    private final FluidTank tank = new FluidTank(CAPACITY, stack -> LiquidBlazeBurnerFuels.get(stack.getFluid()) != null) {
        @Override
        protected void onContentsChanged() {
            fuelChanged = true;
            setChanged();
        }
    };
    private final IFluidHandler fuelHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            return tank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int filled = tank.fill(resource, action);
            if (filled > 0 && action.execute())
                fuelled(filled);
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };
    private boolean fuelChanged;
    private long lastFuelled = Long.MIN_VALUE / 2;

    public LiquidBlazeBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IFluidHandler getFuelHandler() {
        return fuelHandler;
    }

    // Sets the fuel for Ponder scenes
    public void setFuel(FluidStack fuel) {
        tank.setFluid(fuel);
        fuelChanged = true;
    }

    public FluidStack getFuel() {
        return tank.getFluid();
    }

    // Which way the blaze is facing
    public float getHeadAngle(float partialTicks) {
        return headAngle.getValue(partialTicks);
    }

    @Override
    public void tick() {
        if (level != null && !level.isClientSide && !isCreative)
            burnFuel();
        super.tick();
        if (level == null)
            return;
        if (level.isClientSide && VisualizationManager.supportsVisualization(level))
            tickHead();
        if (!level.isClientSide && fuelChanged && level.getGameTime() % 5 == 0) {
            fuelChanged = false;
            sendData();
        }
    }

    private void fuelled(int amount) {
        if (level == null || level.isClientSide)
            return;
        long now = level.getGameTime();
        if (amount >= 1000 || now - lastFuelled > QUIET_TICKS)
            playSound();
        lastFuelled = now;
    }

    // Burns fuel from the tank
    private void burnFuel() {
        FluidStack inTank = tank.getFluid();
        LiquidBlazeBurnerFuels.Fuel fuel = inTank.isEmpty() ? null : LiquidBlazeBurnerFuels.get(inTank.getFluid());
        boolean changed = false;
        boolean drew = false;
        if (fuel != null) {
            FuelType type = fuel.superheated() ? FuelType.SPECIAL : FuelType.NORMAL;
            while (remainingBurnTime < TOP_UP && !tank.isEmpty()) {
                int wanted = Math.max(1, Mth.ceil(TOP_UP * 1000f / fuel.burnTime()));
                FluidStack burnt = tank.drain(wanted, IFluidHandler.FluidAction.EXECUTE);
                if (burnt.isEmpty())
                    break;
                remainingBurnTime += (int) ((long) fuel.burnTime() * burnt.getAmount() / 1000);
                drew = true;
                if (activeFuel != type) {
                    activeFuel = type;
                    changed = true;
                }
            }
        }
        if (activeFuel == FuelType.SPECIAL && remainingBurnTime <= 1) {
            activeFuel = FuelType.NORMAL;
            changed = true;
        }
        if (!changed)
            return;
        HeatLevel before = getHeatLevelFromBlock();
        updateBlockState();
        if (drew && before != getHeatLevelFromBlock())
            level.playSound(null, worldPosition, SoundEvents.BLAZE_AMBIENT, SoundSource.BLOCKS,
                    .125f + level.random.nextFloat() * .125f, 1.15f - level.random.nextFloat() * .25f);
    }

    // Solid fuel can't go in
    @Override
    protected boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, boolean simulate) {
        return false;
    }

    // The blaze head animation
    @OnlyIn(Dist.CLIENT)
    private void tickHead() {
        boolean active = getHeatLevelFromBlock().isAtLeast(HeatLevel.FADING) && isValidBlockAbove();
        if (!active) {
            float target = 0;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && !player.isInvisible()) {
                Vec3 seen = isVirtual() ? new Vec3(-4, 0, -10) : SubLevels.toLocal(level, getBlockPos(),
                        player.getEyePosition());
                double dx = seen.x - (getBlockPos().getX() + 0.5);
                double dz = seen.z - (getBlockPos().getZ() + 0.5);
                target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
            }
            target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
            headAngle.chase(target, .25f, Chaser.exp(5));
            headAngle.tickChaser();
        } else {
            headAngle.chase((AngleHelper.horizontalAngle(getBlockState().getOptionalValue(BlazeBurnerBlock.FACING)
                    .orElse(Direction.SOUTH)) + 180) % 360, .125f, Chaser.EXP);
            headAngle.tickChaser();
        }
        headAnimation.chase(active ? 1 : 0, .25f, Chaser.exp(.25f));
        headAnimation.tickChaser();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return containedFluidTooltip(tooltip, isPlayerSneaking, fuelHandler);
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.put("Fuel", tank.writeToNBT(registries, new CompoundTag()));
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tank.readFromNBT(registries, tag.getCompound("Fuel"));
        super.read(tag, registries, clientPacket);
    }

    @Nullable
    public LiquidBlazeBurnerFuels.Fuel getBurningFuel() {
        return tank.isEmpty() ? null : LiquidBlazeBurnerFuels.get(tank.getFluid()
                .getFluid());
    }
}
