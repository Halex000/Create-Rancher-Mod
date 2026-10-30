package com.halex.createrancher.content.depot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.halex.createrancher.mixin.DepotBehaviourAccessor;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

// Moves items between the inventory below and the depot top
public class SmartDepotBlockEntity extends SmartBlockEntity implements Clearable {

    private DepotBehaviour depot;
    private FilteringBehaviour filtering;
    private InvManipulationBehaviour below;
    private VersionedInventoryTrackerBehaviour belowVersion;
    private int extractionCooldown;

    public static final int SETTLE_TICKS = 5;
    static final int FAN_STALL_TICKS = 10;
    private TransportedItemStack watched;
    private int settledTicks;
    private int lastFanTime;
    private int sinceFanMoved;
    private boolean watchedWorkedOn;
    private boolean freshOutput;
    private boolean watchedLocked;
    private ItemStack watchedStack = ItemStack.EMPTY;
    private Boolean workedOnAgain;

    public SmartDepotBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(depot = new DepotBehaviour(this));
        depot.addSubBehaviours(behaviours);

        behaviours.add(below = new InvManipulationBehaviour(this, (level, pos, state) -> new BlockFace(pos,
                Direction.DOWN)));
        behaviours.add(belowVersion = new VersionedInventoryTrackerBehaviour(this));

        filtering = new FilteringBehaviour(this, new SmartDepotFilterSlot()).showCountWhen(this::isExtracting);
        filtering.withCallback($ -> belowVersion.reset());
        behaviours.add(filtering);
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide)
            return;

        if (getBlockState().getValue(SmartDepotBlock.POWERED)) {
            extractionCooldown = 0;
            return;
        }
        if (!isExtracting()) {
            putInBelow();
            return;
        }
        if (extractionCooldown > 0) {
            extractionCooldown--;
            return;
        }
        pullFromBelow();
    }

    // Empties it without dropping anything
    @Override
    public void clearContent() {
        DepotBehaviourAccessor contents = (DepotBehaviourAccessor) depot;
        contents.createrancher$setHeldItem(null);
        contents.createrancher$getIncoming()
                .clear();
        ItemStackHandler buffer = contents.createrancher$getOutputBuffer();
        for (int slot = 0; slot < buffer.getSlots(); slot++)
            buffer.setStackInSlot(slot, ItemStack.EMPTY);
    }

    private boolean isExtracting() {
        return getBlockState().getValue(SmartDepotBlock.EXTRACTING);
    }

    // Puts what's on top into the inventory below
    private void putInBelow() {
        if (depot.isEmpty() || !below.hasInventory())
            return;
        boolean changed = false;
        TransportedItemStack held = ((DepotBehaviourAccessor) depot).createrancher$getHeldItem();
        if (held != null && !beingProcessed(held) && filtering.test(held.stack)) {
            ItemStack left = below.insert(held.stack.copy());
            if (left.getCount() != held.stack.getCount()) {
                onPutIn(held.stack.copyWithCount(held.stack.getCount() - left.getCount()));
                if (left.isEmpty())
                    depot.removeHeldItem();
                else
                    held.stack = left;
                changed = true;
            }
        }
        IItemHandler top = depot.itemHandler;
        for (int slot = 1; slot < top.getSlots(); slot++) {
            ItemStack stack = top.getStackInSlot(slot);
            if (stack.isEmpty() || !filtering.test(stack))
                continue;
            ItemStack left = below.simulate()
                    .insert(stack.copy());
            int fits = stack.getCount() - left.getCount();
            if (fits <= 0)
                continue;
            ItemStack moved = top.extractItem(slot, fits, false);
            below.insert(moved);
            onPutIn(moved);
            changed = true;
        }
        if (changed)
            notifyUpdate();
    }

    // Whether the item on top is still being worked on
    private boolean beingProcessed(TransportedItemStack held) {
        if (held != watched) {
            freshOutput = watched != null && watchedWorkedOn;
            workedOnAgain = freshOutput ? willWorkOnAgain(watchedStack, held) : null;
            watched = held;
            watchedLocked = held.locked;
            settledTicks = 0;
            lastFanTime = held.processingTime;
            sinceFanMoved = FAN_STALL_TICKS;
        }
        watchedStack = held.stack.copy();
        boolean holdMeansWork = !(freshOutput && Boolean.FALSE.equals(workedOnAgain));
        if (watchedLocked && !held.locked && holdMeansWork)
            settledTicks = 0;
        watchedLocked = held.locked;
        watchedWorkedOn = held.locked || held.processedBy != null && held.processingTime > 0;
        if (held.processingTime != lastFanTime) {
            lastFanTime = held.processingTime;
            sinceFanMoved = 0;
        } else if (sinceFanMoved < FAN_STALL_TICKS)
            sinceFanMoved++;
        if (.5f - held.beltPosition >= 1 / 16f)
            return true;
        if (held.locked && holdMeansWork)
            return true;
        if (held.processedBy != null && held.processingTime > 0 && sinceFanMoved < FAN_STALL_TICKS)
            return true;
        return ++settledTicks <= SETTLE_TICKS;
    }

    // Whether a machine will work on its output again, or null if it can't tell
    @Nullable
    private Boolean willWorkOnAgain(ItemStack input, TransportedItemStack output) {
        BlockPos machinePos = worldPosition.above(2);
        if (BlockEntityBehaviour.get(level, machinePos, BeltProcessingBehaviour.TYPE) instanceof PressingBehaviour press)
            return press.specifics.tryProcessOnBelt(output, null, true);
        BlockEntity machine = level.getBlockEntity(machinePos);
        if (machine instanceof DeployerBlockEntity deployer)
            return deployer.getRecipe(output.stack) != null;
        if (machine instanceof SpoutBlockEntity) {
            IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, machinePos, null);
            FluidStack fluid = tank == null || tank.getTanks() == 0 ? FluidStack.EMPTY : tank.getFluidInTank(0);
            return FillingBySpout.canItemBeFilled(level, output.stack) && !fluid.isEmpty()
                    && FillingBySpout.getRequiredAmountForItem(level, output.stack, fluid.copy()) != -1;
        }
        return recipesWorkOnAgain(input, output.stack);
    }

    // Whether a recipe would work on the output again
    @Nullable
    private Boolean recipesWorkOnAgain(ItemStack input, ItemStack output) {
        if (input.isEmpty())
            return null;
        Set<RecipeType<?>> kinds = new HashSet<>();
        for (RecipeHolder<?> holder : level.getRecipeManager()
                .getRecipes()) {
            Recipe<?> recipe = holder.value();
            List<Ingredient> ingredients = recipe.getIngredients();
            if (!ingredients.isEmpty() && ingredients.get(0)
                    .test(input) && makes(recipe, output))
                kinds.add(recipe.getType());
        }
        if (kinds.isEmpty())
            return null;
        for (RecipeHolder<?> holder : level.getRecipeManager()
                .getRecipes()) {
            Recipe<?> recipe = holder.value();
            List<Ingredient> ingredients = recipe.getIngredients();
            if (kinds.contains(recipe.getType()) && !ingredients.isEmpty() && ingredients.get(0)
                    .test(output))
                return true;
        }
        return false;
    }

    // Whether the recipe can make the item
    private boolean makes(Recipe<?> recipe, ItemStack item) {
        if (recipe instanceof ProcessingRecipe<?, ?> processing)
            return processing.getRollableResults()
                    .stream()
                    .anyMatch(result -> result.getStack()
                            .is(item.getItem()));
        return recipe.getResultItem(level.registryAccess())
                .is(item.getItem());
    }

    // Lets smart observers see what went through
    private void onPutIn(ItemStack stack) {
        AllBlocks.SMART_OBSERVER.get()
                .onFunnelTransfer(level, worldPosition, stack);
    }

    private void pullFromBelow() {
        if (!depot.isEmpty())
            return;
        if (belowVersion.stillWaiting(below))
            return;

        int amount = below.getAmountFromFilter();
        ExtractionCountMode mode = below.getModeFromFilter();
        ItemStack stack = below.extract(mode, amount, depot::isItemValid);
        if (stack.isEmpty()) {
            belowVersion.awaitNewVersion(below);
            return;
        }

        TransportedItemStack transported = new TransportedItemStack(stack);
        transported.insertedFrom = Direction.DOWN;
        transported.beltPosition = .5f;
        transported.prevBeltPosition = .5f;
        ItemStack left = depot.insert(transported, false);
        if (!left.isEmpty()) {
            left = below.insert(left);
            if (!left.isEmpty())
                Block.popResource(level, worldPosition.above(), left);
        }
        notifyUpdate();

        AllBlocks.SMART_OBSERVER.get()
                .onFunnelTransfer(level, worldPosition, stack);
        extractionCooldown = AllConfigs.server().logistics.defaultExtractionTimer.get();
    }

    public DepotBehaviour getDepot() {
        return depot;
    }

    public FilteringBehaviour getFiltering() {
        return filtering;
    }

    public IItemHandler getItemHandler() {
        return depot.itemHandler;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("TransferCooldown", extractionCooldown);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        extractionCooldown = tag.getInt("TransferCooldown");
    }
}
