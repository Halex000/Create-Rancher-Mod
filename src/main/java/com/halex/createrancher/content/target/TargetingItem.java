package com.halex.createrancher.content.target;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

// A machine item that picks a target block before it's placed
public interface TargetingItem {

    // The component older copies of this item saved their target in
    DataComponentType<BlockPos> savedTarget();

    // The message shown when the target is dropped
    Component targetDroppedMessage();

    // The target meant by looking at a block, or null
    @Nullable
    default BlockPos targetAt(Level level, BlockPos looked) {
        return looked;
    }
}
