package com.halex.createrancher.content.fisher;

import javax.annotation.Nullable;

import com.halex.createrancher.content.net.FishingNetItem;
import com.halex.createrancher.index.BlockEntityRegistry;
import com.halex.createrancher.index.DataComponentRegistry;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Mechanical Fisher block
public class MechanicalFisherBlock extends HorizontalKineticBlock implements IBE<MechanicalFisherBlockEntity>,
        ICogWheel {

    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Block.box(2, 14, 2, 14, 16, 14),
            BooleanOp.ONLY_FIRST);

    public MechanicalFisherBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos spot = context.getItemInHand()
                .get(DataComponentRegistry.FISHING_SPOT);
        BlockPos pos = context.getClickedPos();
        Direction facing;
        if (spot != null && (spot.getX() != pos.getX() || spot.getZ() != pos.getZ()))
            facing = Direction.getNearest(spot.getX() - pos.getX(), 0, spot.getZ() - pos.getZ());
        else
            facing = context.getHorizontalDirection();
        return defaultBlockState().setValue(HORIZONTAL_FACING, facing);
    }

    // Its cog lies flat whichever way it faces
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    // A cog with no shaft through it
    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof FishingNetItem))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return onBlockEntityUseItemOn(level, pos, fisher -> {
            if (fisher.hasNet())
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide && fisher.insertNet(stack)) {
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1, .8f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        });
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return onBlockEntityUse(level, pos, fisher -> {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) {
                ItemStack net = fisher.takeNet();
                if (net.isEmpty())
                    return InteractionResult.PASS;
                player.getInventory()
                        .placeItemBackInInventory(net);
                return InteractionResult.SUCCESS;
            }
            if (!fisher.giveCatches(player))
                return InteractionResult.PASS;
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .3f, 1);
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide)
            return;
        BlockPos spot = stack.get(DataComponentRegistry.FISHING_SPOT);
        if (spot == null)
            return;
        withBlockEntityDo(level, pos, fisher -> {
            MechanicalFisherBlockEntity.LinkStatus status = fisher.setSpot(spot);
            if (placer instanceof Player player)
                player.displayClientMessage(Component.translatable(status.getMessageKey(),
                        MechanicalFisherBlockEntity.RANGE), true);
        });
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        withBlockEntityDo(level, pos, MechanicalFisherBlockEntity::redstoneUpdate);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                   boolean isMoving) {
        withBlockEntityDo(level, pos, MechanicalFisherBlockEntity::redstoneUpdate);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public Class<MechanicalFisherBlockEntity> getBlockEntityClass() {
        return MechanicalFisherBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MechanicalFisherBlockEntity> getBlockEntityType() {
        return BlockEntityRegistry.MECHANICAL_FISHER.get();
    }
}
