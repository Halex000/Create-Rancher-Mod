package com.halex.createrancher.content.crate;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;

// A crate with its items in it
public class CrateItem extends BlockItem {

    public CrateItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    // Spills the crate's items if the item gets destroyed
    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        ItemContainerContents contents = itemEntity.getItem()
                .set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (contents != null)
            ItemUtils.onContainerDestroyed(itemEntity, contents.nonEmptyItemsCopy());
    }
}
