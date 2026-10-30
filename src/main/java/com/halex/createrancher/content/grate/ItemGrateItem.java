package com.halex.createrancher.content.grate;

import com.halex.createrancher.content.grill.CampfireGrillBlock;
import com.halex.createrancher.content.grill.CauldronGrillBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// The Item Grate. Goes on belts, campfires and cauldrons.
public class ItemGrateItem extends Item {

    public ItemGrateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (AllBlocks.BELT.has(state)) {
            if (!BeltGrates.canHoldGrate(state)) {
                if (player != null && !level.isClientSide)
                    player.displayClientMessage(Component.translatable(state.getValue(BeltBlock.SLOPE)
                            != BeltSlope.HORIZONTAL ? "createrancher.message.grate.needs_flat_belt"
                            : "createrancher.message.grate.no_shaft"), true);
                return InteractionResult.FAIL;
            }
            if (BeltGrates.isGrated(level, pos))
                return InteractionResult.PASS;
            if (!level.isClientSide && BeltGrates.placeGrate(level, pos))
                context.getItemInHand()
                        .consume(1, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (CampfireGrillBlock.canBecomeGrill(state)) {
            if (!level.isClientSide) {
                CampfireGrillBlock.convertCampfire(level, pos, state);
                context.getItemInHand()
                        .consume(1, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (CauldronGrillBlock.canBecomeGrill(state)) {
            if (!level.isClientSide) {
                CauldronGrillBlock.convertCauldron(level, pos, state);
                context.getItemInHand()
                        .consume(1, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }
}
