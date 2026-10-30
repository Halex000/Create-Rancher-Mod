package com.halex.createrancher.content.grill;

import javax.annotation.Nullable;

import com.halex.createrancher.content.grate.GrateSounds;
import com.halex.createrancher.index.BlockEntityRegistry;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// A campfire with a grate on it that cooks items
public class CampfireGrillBlock extends CampfireBlock implements IWrenchable, GrillBlock {

    public static final float GRATE_TOP = 13 / 16f;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 13, 16);

    private final boolean soul;

    public CampfireGrillBlock(boolean soul, Properties properties) {
        super(!soul, soul ? 2 : 1, properties);
        this.soul = soul;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<CampfireBlock> codec() {
        return (MapCodec<CampfireBlock>) (MapCodec<?>) simpleCodec(properties -> new CampfireGrillBlock(soul,
                properties));
    }

    public boolean isSoul() {
        return soul;
    }

    @Override
    public GrillProcess getProcess(BlockState state) {
        return soul ? GrillProcess.HAUNTING : GrillProcess.SMOKING;
    }

    @Override
    public boolean isProcessing(Level level, BlockPos pos, BlockState state) {
        return state.getValue(LIT);
    }

    @Override
    public float getGrateTop() {
        return GRATE_TOP;
    }

    @Override
    public float getSlotSpacing() {
        return 5 / 16f;
    }

    @Override
    public Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }

    // Sounds like the grate when walked on
    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return GrateSounds.withGrateSteps(super.getSoundType(state, level, pos, entity));
    }

    public static boolean canBecomeGrill(BlockState state) {
        return state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
    }

    // Puts a grate on a campfire
    public static void convertCampfire(Level level, BlockPos pos, BlockState campfire) {
        if (level.getBlockEntity(pos) instanceof CampfireBlockEntity cooking)
            Containers.dropContents(level, pos.above(), cooking.getItems());
        Block grill = campfire.is(Blocks.SOUL_CAMPFIRE) ? BlockRegistry.SOUL_CAMPFIRE_GRILL.get()
                : BlockRegistry.CAMPFIRE_GRILL.get();
        level.setBlockAndUpdate(pos, withCampfireState(grill.defaultBlockState(), campfire));
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1, .9f);
    }

    private static BlockState withCampfireState(BlockState target, BlockState from) {
        return target.setValue(LIT, from.getValue(LIT))
                .setValue(SIGNAL_FIRE, from.getValue(SIGNAL_FIRE))
                .setValue(WATERLOGGED, from.getValue(WATERLOGGED))
                .setValue(FACING, from.getValue(FACING));
    }

    // Sneak-wrenching takes the grate off
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        Block campfire = soul ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE;
        level.setBlockAndUpdate(pos, withCampfireState(campfire.defaultBlockState(), state));
        Player player = context.getPlayer();
        if (player == null || !player.isCreative()) {
            ItemStack grate = ItemRegistry.ITEM_GRATE.asStack();
            if (player == null || !player.getInventory()
                    .add(grate))
                Block.popResource(level, pos.above(), grate);
        }
        level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1, .9f);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return GrillBlock.placeByHand(stack, level, pos, player,
                () -> player.awardStat(Stats.INTERACT_WITH_CAMPFIRE));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()))
            GrillBlock.dropContents(level, pos);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // Picks up items dropped on it
    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        super.updateEntityAfterFallOn(level, entity);
        GrillBlock.onLanded(entity);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        GrillBlock.onNeighbourChanged(level, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BlockEntityRegistry.GRILL.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return createTickerHelper(type, BlockEntityRegistry.GRILL.get(), (l, p, s, grill) -> grill.tick());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(soul ? Items.SOUL_CAMPFIRE : Items.CAMPFIRE);
    }
}
