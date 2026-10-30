package com.halex.createrancher.content.herder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.halex.createrancher.CreateRancher;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;

// Which mobs the herder can pick up
public class HerderCreatures {

    private static final Map<EntityType<?>, Boolean> IS_MOB = new ConcurrentHashMap<>();

    public static boolean isExcluded(EntityType<?> type) {
        return type.is(Tags.EntityTypes.BOSSES) || type.is(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED)
                || type.is(Tags.EntityTypes.TELEPORTING_NOT_SUPPORTED);
    }

    public static boolean isCreature(EntityType<?> type, Level level) {
        if (!type.canSummon() || !type.isEnabled(level.enabledFeatures()) || isExcluded(type))
            return false;
        return IS_MOB.computeIfAbsent(type, t -> createsMob(t, level));
    }

    private static boolean createsMob(EntityType<?> type, Level level) {
        try {
            Entity entity = type.create(level);
            if (entity == null)
                return false;
            boolean mob = entity instanceof Mob;
            entity.discard();
            return mob;
        } catch (Throwable t) {
            CreateRancher.LOGGER.debug("Could not create {} to check whether it is a creature",
                    EntityType.getKey(type), t);
            return false;
        }
    }

    // All creature types sorted by name
    public static List<EntityType<?>> all(Level level) {
        return BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> isCreature(type, level))
                .sorted(Comparator.comparing(type -> type.getDescription()
                        .getString()))
                .toList();
    }
}
