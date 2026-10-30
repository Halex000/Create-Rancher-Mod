package com.halex.createrancher.content.net;

import java.util.ArrayList;
import java.util.List;

import com.halex.createrancher.index.EntityRegistry;
import com.halex.createrancher.mixin.FishingHookAccessor;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

// A thrown fishing net
public class FishingNetEntity extends FishingHook {

    public static final int MIN_CATCHES = 1;
    public static final int MAX_CATCHES = 4;
    public static final float HALF_WIDTH = 1.25f;

    private boolean landed;

    public FishingNetEntity(EntityType<? extends FishingNetEntity> type, Level level) {
        super(type, level);
    }

    // Throws a net from the player's hand
    public static FishingNetEntity thrown(Player player, ServerLevel level, ItemStack net) {
        FishingNetEntity entity = new FishingNetEntity(EntityRegistry.FISHING_NET.get(), level);
        entity.setOwner(player);
        FishingHookAccessor accessor = (FishingHookAccessor) entity;
        accessor.createrancher$setLuck(EnchantmentHelper.getFishingLuckBonus(level, net, player));
        accessor.createrancher$setLureSpeed((int) (EnchantmentHelper.getFishingTimeReduction(level, net, player)
                * 20));

        float pitch = player.getXRot();
        float yaw = player.getYRot();
        float cosYaw = Mth.cos(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float sinYaw = Mth.sin(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float cosPitch = -Mth.cos(-pitch * Mth.DEG_TO_RAD);
        float sinPitch = Mth.sin(-pitch * Mth.DEG_TO_RAD);
        entity.moveTo(player.getX() - sinYaw * .3, player.getEyeY(), player.getZ() - cosYaw * .3, yaw, pitch);
        Vec3 direction = new Vec3(-sinYaw, Mth.clamp(-(sinPitch / cosPitch), -5, 5), -cosYaw);
        double speed = (.6 / direction.length()) * 1.5;
        direction = direction.multiply(speed + entity.random.triangle(.5, .0103365) * .1,
                speed + entity.random.triangle(.5, .0103365) * .1, speed + entity.random.triangle(.5, .0103365) * .1);
        entity.setDeltaMovement(direction);
        entity.setYRot((float) (Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG));
        entity.setXRot((float) (Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG));
        entity.yRotO = entity.getYRot();
        entity.xRotO = entity.getXRot();
        return entity;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !landed && isOnWater()) {
            landed = true;
            playSound(SoundEvents.PLAYER_SPLASH, .5f, 1.2f);
        }
    }

    public boolean isOnWater() {
        return level().getFluidState(blockPosition())
                .is(FluidTags.WATER);
    }

    // Whether it's lying on solid ground instead of floating
    public boolean isOnLand() {
        return onGround() && !isOnWater();
    }

    public boolean isBiting() {
        return ((FishingHookAccessor) this).createrancher$isBiting();
    }

    // Doesn't hook mobs or items
    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    public int retrieve(ItemStack stack) {
        Player player = getPlayerOwner();
        if (level().isClientSide || player == null || !isHeldBy(player))
            return 0;

        int damage = 0;
        if (((FishingHookAccessor) this).createrancher$getNibble() > 0) {
            List<ItemStack> caught = catchFish(stack, player);
            ItemFishedEvent event = new ItemFishedEvent(caught, onGround() ? 2 : 1, this);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                discard();
                return event.getRodDamage();
            }
            CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer) player, stack, this, caught);
            for (ItemStack item : caught) {
                ItemEntity entity = new ItemEntity(level(), getX(), getY(), getZ(), item);
                double dx = player.getX() - getX();
                double dy = player.getY() - getY();
                double dz = player.getZ() - getZ();
                entity.setDeltaMovement(dx * .1, dy * .1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * .08,
                        dz * .1);
                level().addFreshEntity(entity);
                player.level()
                        .addFreshEntity(new ExperienceOrb(player.level(), player.getX(), player.getY() + .5,
                                player.getZ() + .5, random.nextInt(6) + 1));
                if (item.is(ItemTags.FISHES))
                    player.awardStat(Stats.FISH_CAUGHT, 1);
            }
            damage = event.getRodDamage();
        }
        if (onGround())
            damage = 2;
        discard();
        return damage;
    }

    // Rolls 1 to 4 catches
    public List<ItemStack> catchFish(ItemStack net, Entity fisher) {
        ServerLevel level = (ServerLevel) level();
        if (!FishingNetItem.hasTreasureHunter(net, level))
            ((FishingHookAccessor) this).createrancher$setOpenWater(false);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, position())
                .withParameter(LootContextParams.TOOL, net)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ATTACKING_ENTITY, fisher)
                .withLuck(((FishingHookAccessor) this).createrancher$getLuck() + (fisher instanceof Player player
                        ? player.getLuck() : 0))
                .create(LootContextParamSets.FISHING);
        LootTable table = level.getServer()
                .reloadableRegistries()
                .getLootTable(BuiltInLootTables.FISHING);
        List<ItemStack> caught = new ArrayList<>();
        int rolls = MIN_CATCHES + random.nextInt(MAX_CATCHES - MIN_CATCHES + 1);
        for (int i = 0; i < rolls; i++)
            caught.addAll(table.getRandomItems(params));
        return caught;
    }

    // Whether the net should stay out
    private boolean isHeldBy(Player player) {
        return !player.isRemoved() && player.isAlive() && (player.getMainHandItem()
                .canPerformAction(ItemAbilities.FISHING_ROD_CAST) || player.getOffhandItem()
                .canPerformAction(ItemAbilities.FISHING_ROD_CAST)) && distanceToSqr(player) <= 1024;
    }
}
