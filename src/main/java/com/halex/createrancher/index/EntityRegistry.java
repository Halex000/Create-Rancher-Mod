package com.halex.createrancher.index;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.net.FishingNetEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityRegistry {

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE,
            CreateRancher.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<FishingNetEntity>> FISHING_NET = ENTITIES.register(
            "fishing_net", () -> EntityType.Builder.<FishingNetEntity>of(FishingNetEntity::new, MobCategory.MISC)
                    .noSave()
                    .noSummon()
                    .sized(.25f, .25f)
                    .clientTrackingRange(4)
                    .updateInterval(5)
                    .build("fishing_net"));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
