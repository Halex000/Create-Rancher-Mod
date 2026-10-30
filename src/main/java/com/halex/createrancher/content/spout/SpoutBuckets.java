package com.halex.createrancher.content.spout;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

// Lets buckets fill and empty spouts by hand
public class SpoutBuckets {

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SpoutBuckets::onRightClickBlock);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (player.isSecondaryUseActive() || !AllBlocks.SPOUT.has(level.getBlockState(pos))
                || !(level.getBlockEntity(pos) instanceof SpoutBlockEntity spout) || !isBucket(level, held))
            return;
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (tank == null)
            return;
        FluidStack before = tank.getFluidInTank(0)
                .copy();
        if (FluidHelper.tryEmptyItemIntoBE(level, player, event.getHand(), held, spout)) {
            if (!level.isClientSide)
                level.playSound(null, pos, FluidHelper.getEmptySound(tank.getFluidInTank(0)), SoundSource.BLOCKS, .5f,
                        1);
        } else if (FluidHelper.tryFillItemFromBE(level, player, event.getHand(), held, spout)) {
            if (!level.isClientSide)
                level.playSound(null, pos, FluidHelper.getFillSound(before), SoundSource.BLOCKS, .5f, 1);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
    }

    // Whether the item is a bucket
    private static boolean isBucket(Level level, ItemStack stack) {
        if (stack.is(Items.BUCKET))
            return true;
        return GenericItemEmptying.canItemBeEmptied(level, stack) && GenericItemEmptying.emptyItem(level, stack.copy(),
                        false)
                .getSecond()
                .is(Items.BUCKET);
    }
}
