package com.halex.createrancher.content.herder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.halex.createrancher.CreateRancher;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

// Keeps track of which mobs herders have claimed
@EventBusSubscriber(modid = CreateRancher.MOD_ID)
public class HerderTracker {

    private static final Map<UUID, BlockPos> CLAIMS = new HashMap<>();

    public static boolean claim(UUID mob, BlockPos herder) {
        BlockPos owner = CLAIMS.putIfAbsent(mob, herder);
        return owner == null || owner.equals(herder);
    }

    public static boolean isClaimed(UUID mob) {
        return CLAIMS.containsKey(mob);
    }

    public static void release(UUID mob, BlockPos herder) {
        CLAIMS.remove(mob, herder);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!CLAIMS.containsKey(event.getEntity().getUUID()))
            return;
        if (event.getSource().is(DamageTypes.IN_WALL) || event.getSource().is(DamageTypeTags.IS_FALL))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CLAIMS.clear();
    }
}
