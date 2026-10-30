package com.halex.createrancher.content.satchel;

import com.halex.createrancher.index.MenuRegistry;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

// The satchel's menu
public class SatchelMenu extends AbstractContainerMenu {

    public static final int SLOTS = 9;

    private final ItemStack satchel;
    private final SimpleContainer contents;
    private final int satchelSlot;

    public SatchelMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(MenuRegistry.SATCHEL.get(), containerId);
        Player player = inventory.player;
        satchel = player.getItemInHand(hand);
        satchelSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : -1;

        contents = new SimpleContainer(SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                save();
            }
        };
        NonNullList<ItemStack> saved = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        satchel.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(saved);
        for (int i = 0; i < SLOTS; i++)
            contents.setItem(i, saved.get(i));

        for (int col = 0; col < SLOTS; col++)
            addSlot(new Slot(contents, col, 8 + col * 18, 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return SatchelItem.fits(stack);
                }
            });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 49 + row * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return getContainerSlot() != satchelSlot;
                    }
                });
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, 107) {
                @Override
                public boolean mayPickup(Player player) {
                    return getContainerSlot() != satchelSlot;
                }
            });
    }

    private void save() {
        satchel.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents.getItems()));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.SWAP && (button == satchelSlot || satchelSlot == -1
                && button == Inventory.SLOT_OFFHAND))
            return;
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId)
                .getItem() == satchel)
            return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || slot.getItem() == satchel)
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!SatchelItem.fits(stack) || !moveItemStackTo(stack, 0, SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return original;
    }

    // Closes if the satchel moves
    @Override
    public boolean stillValid(Player player) {
        ItemStack held = satchelSlot == -1 ? player.getOffhandItem() : player.getInventory()
                .getItem(satchelSlot);
        return held == satchel && !satchel.isEmpty();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        save();
    }
}
