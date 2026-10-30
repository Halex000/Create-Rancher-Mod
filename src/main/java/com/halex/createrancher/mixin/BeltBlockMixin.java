package com.halex.createrancher.mixin;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import com.halex.createrancher.content.grate.BeltGrates;
import com.halex.createrancher.content.grate.GrateSounds;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltShapes;
import com.simibubi.create.foundation.item.ItemHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// Belt changes for grates
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(BeltBlock.class)
public abstract class BeltBlockMixin {

    // Hitbox for grated segments
    @ModifyReturnValue(method = "getShape", at = @At("RETURN"))
    private VoxelShape createrancher$grateShape(VoxelShape shape, BlockState state, BlockGetter level, BlockPos pos,
                                                CollisionContext context) {
        VoxelShape grate = BeltGrates.grateShape(level, pos, state, true);
        return grate.isEmpty() ? shape : BeltGrates.withGrate(shape, grate);
    }

    // Collision for grated segments
    @ModifyReturnValue(method = "getCollisionShape", at = @At("RETURN"))
    private VoxelShape createrancher$grateCollision(VoxelShape shape, BlockState state, BlockGetter level,
                                                    BlockPos pos, CollisionContext context) {
        VoxelShape grate = BeltGrates.grateShape(level, pos, state, false);
        return grate.isEmpty() ? shape : BeltGrates.withGrate(BeltShapes.getCollisionShape(state), grate);
    }

    // Grated belts sound like item drains when walked on
    @SuppressWarnings("deprecation")
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        SoundType belt = state.getSoundType();
        return BeltGrates.isGrated(level, pos) ? GrateSounds.withGrateSteps(belt) : belt;
    }

    // Only items go through the grate
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void createrancher$onlyItemsThroughGrate(BlockState state, Level level, BlockPos pos, Entity entity,
                                                     CallbackInfo ci) {
        if (BeltGrates.isGrated(level, pos) && ItemHelper.fromItemEntity(entity)
                .isEmpty())
            ci.cancel();
    }

    // No shafts through grated segments
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void createrancher$noShaftUnderGrate(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                 Player player, InteractionHand hand, BlockHitResult hitResult,
                                                 CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (player.isShiftKeyDown() || !AllBlocks.SHAFT.isIn(stack) || !BeltGrates.isGrated(level, pos))
            return;
        if (!level.isClientSide)
            player.displayClientMessage(Component.translatable("createrancher.message.grate.remove_first"), true);
        cir.setReturnValue(ItemInteractionResult.FAIL);
    }

    // Drops grates when a belt is broken
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void createrancher$dropGrates(BlockState state, Level level, BlockPos pos, BlockState newState,
                                          boolean isMoving, CallbackInfo ci) {
        if (level.isClientSide || isMoving || state.getBlock() == newState.getBlock())
            return;
        BeltGrates.dropChainGrates(level, pos, state);
    }

    // Mobs can walk over grates
    @Inject(method = "getBlockPathType", at = @At("HEAD"), cancellable = true)
    private void createrancher$walkableGrate(BlockState state, BlockGetter level, BlockPos pos, Mob mob,
                                             CallbackInfoReturnable<PathType> cir) {
        if (BeltGrates.isGrated(level, pos))
            cir.setReturnValue(PathType.WALKABLE);
    }
}
