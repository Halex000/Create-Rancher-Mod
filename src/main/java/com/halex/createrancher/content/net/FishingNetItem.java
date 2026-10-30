package com.halex.createrancher.content.net;

import com.halex.createrancher.CreateRancher;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.EventHooks;

// The fishing net. Throw it and haul it in like a fishing rod.
public class FishingNetItem extends Item {

    public static final int DURABILITY = 43;
    public static final ResourceKey<Enchantment> TREASURE_HUNTER = ResourceKey.create(Registries.ENCHANTMENT,
            CreateRancher.asResource("treasure_hunter"));

    public FishingNetItem(Properties properties) {
        super(properties);
    }

    // Casts or hauls in the net. Does nothing in a deployer.
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof DeployerFakePlayer)
            return InteractionResultHolder.pass(stack);
        if (player.fishing == null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW,
                    SoundSource.NEUTRAL, .7f, .3f / (level.getRandom()
                            .nextFloat() * .4f + .8f));
            if (level instanceof ServerLevel serverLevel)
                level.addFreshEntity(FishingNetEntity.thrown(player, serverLevel, stack));
            player.awardStat(Stats.ITEM_USED.get(this));
            player.gameEvent(GameEvent.ITEM_INTERACT_START);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (!level.isClientSide) {
            int damage = player.fishing.retrieve(stack);
            ItemStack original = stack.copy();
            stack.hurtAndBreak(damage, player, LivingEntity.getSlotForHand(hand));
            if (stack.isEmpty())
                EventHooks.onPlayerDestroyItem(player, original, hand);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.NEUTRAL, 1, .4f / (level.getRandom()
                        .nextFloat() * .4f + .8f));
        player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // Counts as a fishing rod
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_FISHING_ROD_ACTIONS.contains(itemAbility);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 1;
    }

    // Whether the net has Treasure Hunter
    public static boolean hasTreasureHunter(ItemStack net, Level level) {
        return level.registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(TREASURE_HUNTER))
                .map(enchantment -> net.getEnchantmentLevel(enchantment) > 0)
                .orElse(false);
    }
}
