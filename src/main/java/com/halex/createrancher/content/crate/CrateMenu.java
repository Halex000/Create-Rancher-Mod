package com.halex.createrancher.content.crate;

import com.halex.createrancher.index.MenuRegistry;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// The crate's menu
public class CrateMenu extends AbstractContainerMenu {

    public static final int CRATE_X = 44;
    public static final int CRATE_Y = 17;
    private static final int INVENTORY_Y = 84;

    private final Container crate;

    // Client side menu
    public CrateMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(CrateBlockEntity.SLOTS));
    }

    public CrateMenu(int containerId, Inventory inventory, Container crate) {
        super(MenuRegistry.CRATE.get(), containerId);
        checkContainerSize(crate, CrateBlockEntity.SLOTS);
        this.crate = crate;
        crate.startOpen(inventory.player);

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 5; col++)
                addSlot(new Slot(crate, col + row * 5, CRATE_X + col * 18, CRATE_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return CrateBlockEntity.fits(stack);
                    }
                });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < CrateBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, CrateBlockEntity.SLOTS, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!CrateBlockEntity.fits(stack) || !moveItemStackTo(stack, 0, CrateBlockEntity.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return crate.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        crate.stopOpen(player);
    }
}
