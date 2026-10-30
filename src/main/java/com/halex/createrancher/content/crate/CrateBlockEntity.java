package com.halex.createrancher.content.crate;

import com.halex.createrancher.compat.VersionCompat;
import com.halex.createrancher.index.BlockEntityRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// A crate's inventory. It keeps its items when broken.
public class CrateBlockEntity extends RandomizableContainerBlockEntity {

    public static final int SLOTS = 15;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int openCount;

    public CrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        this(BlockEntityRegistry.CRATE.get(), pos, state);
    }

    // Whether an item can go in a crate
    public static boolean fits(ItemStack stack) {
        return VersionCompat.fitsInContainerItems(stack);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    // Stops automation putting containers in
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return fits(stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.createrancher.crate");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CrateMenu(containerId, inventory, this);
    }

    @Override
    public void startOpen(Player player) {
        if (remove || player.isSpectator())
            return;
        if (openCount++ == 0)
            level.playSound(null, worldPosition, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, .5f,
                    level.random.nextFloat() * .1f + .9f);
    }

    @Override
    public void stopOpen(Player player) {
        if (remove || player.isSpectator())
            return;
        if (--openCount <= 0) {
            openCount = 0;
            level.playSound(null, worldPosition, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, .5f,
                    level.random.nextFloat() * .1f + .9f);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag))
            ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag))
            ContainerHelper.saveAllItems(tag, items, false, registries);
    }
}
