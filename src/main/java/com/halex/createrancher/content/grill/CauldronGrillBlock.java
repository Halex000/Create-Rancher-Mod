package com.halex.createrancher.content.grill;

import javax.annotation.Nullable;

import com.halex.createrancher.content.grate.GrateSounds;
import com.halex.createrancher.index.BlockEntityRegistry;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// A cauldron with a grate on it that blasts items over lava or washes them over water
public class CauldronGrillBlock extends Block implements EntityBlock, IWrenchable, GrillBlock {

    public enum Contents implements StringRepresentable {
        EMPTY, WATER, LAVA;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final EnumProperty<Contents> CONTENTS = EnumProperty.create("contents", Contents.class);
    public static final IntegerProperty LEVEL = LayeredCauldronBlock.LEVEL;

    public static final float GRATE_TOP = 1;
    private static final VoxelShape GRATE = Block.box(0, 15, 0, 16, 16, 16);
    private static final VoxelShape SHAPE = Shapes.or(Shapes.join(Shapes.block(),
            Shapes.or(box(0, 0, 4, 16, 3, 12), box(4, 0, 0, 12, 3, 16), box(2, 0, 2, 14, 3, 14),
                    box(2, 4, 2, 14, 16, 14)), BooleanOp.ONLY_FIRST), GRATE);

    public CauldronGrillBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CONTENTS, Contents.EMPTY)
                .setValue(LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(CONTENTS, LEVEL));
    }

    // How full the cauldron is
    public static double getContentHeight(BlockState state) {
        return switch (state.getValue(CONTENTS)) {
            case WATER -> (6 + state.getValue(LEVEL) * 3) / 16d;
            case LAVA -> 15 / 16d;
            case EMPTY -> 4 / 16d;
        };
    }

    @Nullable
    @Override
    public GrillProcess getProcess(BlockState state) {
        return switch (state.getValue(CONTENTS)) {
            case WATER -> GrillProcess.WASHING;
            case LAVA -> GrillProcess.BLASTING;
            case EMPTY -> null;
        };
    }

    // Only works when full
    @Override
    public boolean isProcessing(Level level, BlockPos pos, BlockState state) {
        return switch (state.getValue(CONTENTS)) {
            case LAVA -> true;
            case WATER -> state.getValue(LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL && isHeated(level, pos);
            case EMPTY -> false;
        };
    }

    // Whether there's heat under the cauldron
    public static boolean isHeated(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        return BoilerHeater.findHeat(level, below, level.getBlockState(below)) >= BoilerHeater.PASSIVE_HEAT;
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
        return Direction.NORTH;
    }

    // Sounds like the grate when walked on
    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return GrateSounds.withGrateSteps(super.getSoundType(state, level, pos, entity));
    }

    public static boolean canBecomeGrill(BlockState state) {
        return state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON) || state.is(Blocks.LAVA_CAULDRON);
    }

    // Puts a grate on a cauldron
    public static void convertCauldron(Level level, BlockPos pos, BlockState cauldron) {
        BlockState grill = BlockRegistry.CAULDRON_GRILL.getDefaultState();
        if (cauldron.is(Blocks.WATER_CAULDRON))
            grill = grill.setValue(CONTENTS, Contents.WATER)
                    .setValue(LEVEL, cauldron.getValue(LEVEL));
        else if (cauldron.is(Blocks.LAVA_CAULDRON))
            grill = grill.setValue(CONTENTS, Contents.LAVA);
        level.setBlockAndUpdate(pos, grill);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1, .9f);
    }

    private static BlockState asCauldron(BlockState state) {
        return switch (state.getValue(CONTENTS)) {
            case WATER -> Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(LEVEL, state.getValue(LEVEL));
            case LAVA -> Blocks.LAVA_CAULDRON.defaultBlockState();
            case EMPTY -> Blocks.CAULDRON.defaultBlockState();
        };
    }

    // Sneak-wrenching takes the grate off
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        level.setBlockAndUpdate(pos, asCauldron(state));
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

    // Buckets, bottles and putting items on by hand
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        Contents contents = state.getValue(CONTENTS);
        boolean full = contents == Contents.LAVA || contents == Contents.WATER
                && state.getValue(LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL;
        if (stack.is(Items.WATER_BUCKET) && contents != Contents.LAVA && !full)
            return swapBucket(stack, state.setValue(CONTENTS, Contents.WATER)
                    .setValue(LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), new ItemStack(Items.BUCKET),
                    SoundEvents.BUCKET_EMPTY, level, pos, player, hand);
        if (stack.is(Items.LAVA_BUCKET) && contents == Contents.EMPTY)
            return swapBucket(stack, state.setValue(CONTENTS, Contents.LAVA), new ItemStack(Items.BUCKET),
                    SoundEvents.BUCKET_EMPTY_LAVA, level, pos, player, hand);
        if (stack.is(Items.BUCKET) && full)
            return swapBucket(stack, state.setValue(CONTENTS, Contents.EMPTY),
                    new ItemStack(contents == Contents.LAVA ? Items.LAVA_BUCKET : Items.WATER_BUCKET),
                    contents == Contents.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, level, pos,
                    player, hand);
        if (stack.is(Items.GLASS_BOTTLE) && contents == Contents.WATER) {
            int left = state.getValue(LEVEL) - 1;
            return swapBucket(stack, left == 0 ? state.setValue(CONTENTS, Contents.EMPTY)
                            .setValue(LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL) : state.setValue(LEVEL, left),
                    PotionContents.createItemStack(Items.POTION, Potions.WATER), SoundEvents.BOTTLE_FILL, level, pos,
                    player, hand);
        }
        if (isWaterBottle(stack) && (contents == Contents.EMPTY || contents == Contents.WATER && !full))
            return swapBucket(stack, state.setValue(CONTENTS, Contents.WATER)
                            .setValue(LEVEL, contents == Contents.EMPTY ? 1 : state.getValue(LEVEL) + 1),
                    new ItemStack(Items.GLASS_BOTTLE), SoundEvents.BOTTLE_EMPTY, level, pos, player, hand);
        return GrillBlock.placeByHand(stack, level, pos, player, () -> {
        });
    }

    private static boolean isWaterBottle(ItemStack stack) {
        return stack.is(Items.POTION) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .is(Potions.WATER);
    }

    private static ItemInteractionResult swapBucket(ItemStack held, BlockState filled, ItemStack result,
                                                    SoundEvent sound, Level level, BlockPos pos, Player player,
                                                    InteractionHand hand) {
        if (!level.isClientSide) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
            level.setBlockAndUpdate(pos, filled);
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1, 1);
            level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
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
        return type == BlockEntityRegistry.GRILL.get()
                ? (l, p, s, grill) -> ((GrillBlockEntity) grill).tick() : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }
}
