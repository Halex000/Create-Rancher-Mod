package com.halex.createrancher.content.wood;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

// A log an axe can strip
public class StrippableLogBlock extends RotatedPillarBlock {

    private final Supplier<? extends Block> stripped;

    public StrippableLogBlock(Supplier<? extends Block> stripped, Properties properties) {
        super(properties);
        this.stripped = stripped;
    }

    @Nullable
    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility,
                                           boolean simulate) {
        if (itemAbility == ItemAbilities.AXE_STRIP)
            return stripped.get()
                    .defaultBlockState()
                    .setValue(AXIS, state.getValue(AXIS));
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }
}
