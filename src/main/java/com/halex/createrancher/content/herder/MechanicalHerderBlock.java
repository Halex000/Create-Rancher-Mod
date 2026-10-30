package com.halex.createrancher.content.herder;

import javax.annotation.Nullable;

import com.halex.createrancher.index.BlockEntityRegistry;
import com.halex.createrancher.index.DataComponentRegistry;
import com.halex.createrancher.content.herder.MechanicalHerderBlockEntity.DropOffStatus;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MechanicalHerderBlock extends HorizontalKineticBlock implements IBE<MechanicalHerderBlockEntity>, ICogWheel {

    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 16, 16), Block.box(3, 16, 3, 13, 20, 13),
            Block.box(5, 20, 5, 11, 23, 11));
    private static final VoxelShape COLLISION_SHAPE = Block.box(0, 0, 0, 16, 24, 16);
    private static final VoxelShape OCCLUSION_SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 6, 16),
            Block.box(0, 10, 0, 16, 16, 16));

    public MechanicalHerderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return OCCLUSION_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return COLLISION_SHAPE;
    }

    // Lets fences connect to it
    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    // Mobs treat it like a fence
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public @Nullable PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos,
                                               @Nullable Mob mob) {
        return PathType.FENCE;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide)
            return;
        BlockPos dropOff = stack.get(DataComponentRegistry.DROP_OFF);
        if (dropOff == null)
            return;
        withBlockEntityDo(level, pos, be -> {
            DropOffStatus status = be.setDropOff(dropOff);
            if (placer instanceof Player player)
                player.displayClientMessage(Component.translatable(status.getMessageKey(),
                        MechanicalHerderBlockEntity.RANGE), true);
        });
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        withBlockEntityDo(level, pos, MechanicalHerderBlockEntity::redstoneUpdate);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                   boolean isMoving) {
        withBlockEntityDo(level, pos, MechanicalHerderBlockEntity::redstoneUpdate);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.isEmpty())
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (level.isClientSide)
            withBlockEntityDo(level, pos, be -> CatnipServices.PLATFORM.executeOnClientOnly(
                    () -> () -> MechanicalHerderClient.openScreen(be)));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Class<MechanicalHerderBlockEntity> getBlockEntityClass() {
        return MechanicalHerderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MechanicalHerderBlockEntity> getBlockEntityType() {
        return BlockEntityRegistry.MECHANICAL_HERDER.get();
    }
}
