package com.halex.createrancher.content.drain;

import java.util.ArrayList;
import java.util.List;

import com.halex.createrancher.compat.sable.Moves;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

// One livestock drain
public class LivestockDrainBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    private List<FluidStack> share = new ArrayList<>();
    LivestockDrainGroup group;
    boolean shareChanged;
    private final LerpedFloat shownLevel = LerpedFloat.linear()
            .startWithValue(0);
    private float shownTarget;
    private float shownSpeed;
    private boolean shownYet;
    private boolean moved;

    public LivestockDrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public LivestockDrainGroup getGroup() {
        if (group == null || !group.isValid())
            LivestockDrainGroup.form(this);
        return group;
    }

    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return getGroup().getFluidHandler()
                    .getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return getGroup().getFluidHandler()
                    .getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return getGroup().getFluidHandler()
                    .getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return getGroup().getFluidHandler()
                    .fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return getGroup().getFluidHandler()
                    .drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return getGroup().getFluidHandler()
                    .drain(maxDrain, action);
        }
    };

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    public List<FluidStack> getShare() {
        return share;
    }

    public int getShareAmount() {
        int total = 0;
        for (FluidStack stack : share)
            total += stack.getAmount();
        return total;
    }

    // Sets this drain's share. Returns whether a fluid was added or removed.
    boolean setShare(List<FluidStack> newShare) {
        boolean same = newShare.size() == share.size();
        boolean sameKinds = same;
        for (int i = 0; same && i < share.size(); i++) {
            sameKinds = FluidStack.isSameFluidSameComponents(share.get(i), newShare.get(i));
            same = sameKinds && share.get(i)
                    .getAmount() == newShare.get(i)
                    .getAmount();
        }
        if (same)
            return false;
        share = newShare;
        shareChanged = true;
        setChanged();
        return !sameKinds;
    }

    // Sets what the drain holds in Ponder scenes
    public void setShareForPonder(List<FluidStack> newShare) {
        List<FluidStack> copy = new ArrayList<>();
        for (FluidStack stack : newShare)
            copy.add(stack.copy());
        setShare(copy);
    }

    public float getShownLevel(float partialTicks) {
        return shownLevel.getValue(partialTicks);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null)
            return;
        if (moved && !level.isClientSide) {
            moved = false;
            LivestockDrainBlock.joinAround(level, worldPosition);
        }
        LivestockDrainGroup group = getGroup();
        if (!level.isClientSide) {
            if (group.isLeader(this))
                group.tick();
            return;
        }
        float full = Math.min(1, getShareAmount() / (float) LivestockDrainGroup.CAPACITY);
        if (!shownYet) {
            shownYet = true;
            shownLevel.startWithValue(full);
        }
        if (full != shownTarget) {
            shownTarget = full;
            shownSpeed = Math.max(Math.abs(full - shownLevel.getValue()) / LivestockDrainGroup.SYNC_TICKS, 1e-4f);
        }
        shownLevel.chase(full, shownSpeed, Chaser.LINEAR);
        shownLevel.tickChaser();
    }

    // Joins its group up again when the drains it's joined to change
    @SuppressWarnings("deprecation")
    @Override
    public void setBlockState(BlockState state) {
        BlockState old = getBlockState();
        super.setBlockState(state);
        if (group != null && !sameJoins(old, state))
            group.invalidate();
    }

    private static boolean sameJoins(BlockState a, BlockState b) {
        for (Direction direction : Direction.Plane.HORIZONTAL)
            if (LivestockDrainBlock.joins(a, direction) != LivestockDrainBlock.joins(b, direction))
                return false;
        return true;
    }

    // Splits the group when this drain goes
    @Override
    public void invalidate() {
        super.invalidate();
        if (group != null)
            group.invalidate();
    }

    // Goggle tooltip
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        LivestockDrainGroup group = getGroup();
        List<FluidStack> contents = group.getContents();
        if (contents.size() < 2)
            return containedFluidTooltip(tooltip, isPlayerSneaking, getFluidHandler());
        LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");
        CreateLang.translate("gui.goggles.fluid_container")
                .forGoggles(tooltip);
        CreateLang.builder()
                .add(Component.translatable("createrancher.gui.goggles.livestock_drain.capacity"))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);
        CreateLang.builder()
                .add(CreateLang.number(group.getTotalAmount())
                        .add(mb)
                        .style(ChatFormatting.GOLD))
                .text(ChatFormatting.GRAY, " / ")
                .add(CreateLang.number(group.getCapacity())
                        .add(mb)
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);
        for (FluidStack fluid : contents) {
            CreateLang.fluidName(fluid)
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
            CreateLang.number(fluid.getAmount())
                    .add(mb)
                    .style(ChatFormatting.GOLD)
                    .forGoggles(tooltip, 1);
        }
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ListTag fluids = new ListTag();
        for (FluidStack stack : share)
            fluids.add(stack.save(registries));
        tag.put("Fluids", fluids);
        if (!clientPacket)
            Moves.saveOrigin(tag, worldPosition);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        List<FluidStack> read = new ArrayList<>();
        for (Tag fluid : tag.getList("Fluids", Tag.TAG_COMPOUND)) {
            FluidStack stack = FluidStack.parseOptional(registries, (CompoundTag) fluid);
            if (!stack.isEmpty())
                read.add(stack);
        }
        share = read;
        if (!clientPacket)
            moved = Moves.moved(tag, worldPosition);
    }
}
