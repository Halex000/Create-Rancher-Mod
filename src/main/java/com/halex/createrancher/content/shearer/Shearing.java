package com.halex.createrancher.content.shearer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.halex.createrancher.CreateRancher;
import com.mojang.authlib.GameProfile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

// Shears an animal like a player would
public final class Shearing {

    private static final GameProfile SHEARER = new GameProfile(UUID.fromString("2c6b1a4e-91d7-4f0e-b3a8-7d5e0c9f6a21"),
            "[Mechanical Shearer]");
    private static final int CHECK_TICKS = 20;

    private static final Map<Mob, Check> CHECKS = new WeakHashMap<>();
    private static boolean asking;
    @Nullable
    private static List<ItemEntity> catching;

    private Shearing() {
    }

    private record Check(long at, boolean shearable) {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, EntityJoinLevelEvent.class, event -> {
            if (catching != null && event.getEntity() instanceof ItemEntity item) {
                catching.add(item);
                event.setCanceled(true);
            } else if (asking)
                event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PlayLevelSoundEvent.AtEntity.class, Shearing::hush);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PlayLevelSoundEvent.AtPosition.class, Shearing::hush);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VanillaGameEvent.class, Shearing::hush);
    }

    private static void hush(ICancellableEvent event) {
        if (asking)
            event.setCanceled(true);
    }

    // Whether shears would get anything from this animal
    public static boolean canShear(Mob animal) {
        if (!animal.isAlive() || !(animal.level() instanceof ServerLevel level))
            return false;
        if (animal instanceof IShearable shearable)
            return shearable.isShearable(player(level, animal), new ItemStack(Items.SHEARS), level,
                    animal.blockPosition());
        long now = level.getGameTime();
        Check check = CHECKS.get(animal);
        if (check == null || now - check.at() >= CHECK_TICKS || now < check.at()) {
            check = new Check(now, tryOnCopy(level, animal));
            CHECKS.put(animal, check);
        }
        return check.shearable();
    }

    // Every animal in the area that shears would get something from
    public static List<Mob> shearableIn(Level level, AABB area) {
        if (!(level instanceof ServerLevel serverLevel))
            return List.of();
        FakePlayer[] ready = new FakePlayer[1];
        return level.getEntitiesOfClass(Mob.class, area, animal -> {
            if (!(animal instanceof IShearable shearable) || !animal.isAlive() || animal.level() != serverLevel) {
                ready[0] = null;
                return canShear(animal);
            }
            if (ready[0] == null)
                ready[0] = player(serverLevel, animal);
            else
                ready[0].setPos(animal.getX(), animal.getY(), animal.getZ());
            return shearable.isShearable(ready[0], new ItemStack(Items.SHEARS), serverLevel, animal.blockPosition());
        });
    }

    private static boolean tryOnCopy(ServerLevel level, Mob animal) {
        Entity copy = animal.getType()
                .create(level);
        if (copy == null)
            return false;
        copy.load(animal.saveWithoutId(new CompoundTag()));
        FakePlayer player = player(level, copy);
        asking = true;
        try {
            return player.interactOn(copy, InteractionHand.MAIN_HAND)
                    .consumesAction() && !copy.isRemoved();
        } catch (RuntimeException e) {
            CreateRancher.LOGGER.debug("Couldn't try shearing {}", animal, e);
            return false;
        } finally {
            asking = false;
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    // Shears the animal and returns what came off
    public static List<ItemEntity> shear(Mob animal) {
        List<ItemEntity> dropped = new ArrayList<>();
        if (!(animal.level() instanceof ServerLevel level))
            return dropped;
        FakePlayer player = player(level, animal);
        catching = dropped;
        try {
            player.interactOn(animal, InteractionHand.MAIN_HAND);
        } catch (RuntimeException e) {
            CreateRancher.LOGGER.debug("Couldn't shear {}", animal, e);
        } finally {
            catching = null;
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        CHECKS.remove(animal);
        return dropped;
    }

    // A fake player holding shears
    private static FakePlayer player(ServerLevel level, Entity animal) {
        FakePlayer player = FakePlayerFactory.get(level, SHEARER);
        player.getInventory()
                .clearContent();
        player.setPos(animal.getX(), animal.getY(), animal.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        return player;
    }
}
