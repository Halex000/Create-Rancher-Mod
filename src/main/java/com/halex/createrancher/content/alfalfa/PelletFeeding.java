package com.halex.createrancher.content.alfalfa;

import com.halex.createrancher.CommonConfig;
import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.index.AttachmentRegistry;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// Animals that eat Alfalfa Pellets drop Dung a while later
public class PelletFeeding {

    public static final TagKey<EntityType<?>> EATS_ALFALFA_PELLETS =
            TagKey.create(Registries.ENTITY_TYPE, CreateRancher.asResource("eats_alfalfa_pellets"));

    public static boolean eatsPellets(Entity entity) {
        return entity.getType()
                .is(EATS_ALFALFA_PELLETS);
    }

    public static boolean isDigesting(Entity entity) {
        return entity.hasData(AttachmentRegistry.DIGESTION);
    }

    public static boolean canFeed(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && eatsPellets(entity)
                && !isDigesting(entity);
    }

    // Feeds the animal one pellet. Check canFeed first.
    public static void feed(LivingEntity animal) {
        animal.setData(AttachmentRegistry.DIGESTION, CommonConfig.ALFALFA_DIGESTION_TICKS.get());
        animal.playSound(SoundEvents.GENERIC_EAT, .8f, .8f + animal.getRandom()
                .nextFloat() * .4f);
        if (animal.level() instanceof ServerLevel level)
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemRegistry.ALFALFA_PELLETS.asStack()),
                    animal.getX(), animal.getEyeY(), animal.getZ(), 8, .2, .1, .2, .05);
    }

    // Drops Dung once the animal is done digesting
    public static void tick(Entity entity) {
        if (entity.level().isClientSide || !isDigesting(entity))
            return;
        int left = entity.getData(AttachmentRegistry.DIGESTION) - 1;
        if (left > 0) {
            entity.setData(AttachmentRegistry.DIGESTION, left);
            return;
        }
        entity.removeData(AttachmentRegistry.DIGESTION);
        if (!entity.isAlive())
            return;
        entity.spawnAtLocation(new ItemStack(BlockRegistry.DUNG.get()));
        entity.playSound(SoundEvents.CHICKEN_EGG, 1, .6f);
    }
}
