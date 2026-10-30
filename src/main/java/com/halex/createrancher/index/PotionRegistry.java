package com.halex.createrancher.index;

import com.halex.createrancher.CreateRancher;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// Potions brewed from ranch leftovers
public class PotionRegistry {

    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION,
            CreateRancher.MOD_ID);

    public static final DeferredHolder<Potion, Potion> HASTE = potion("haste", MobEffects.DIG_SPEED, 3600, 0);
    public static final DeferredHolder<Potion, Potion> LONG_HASTE = potion("long_haste", "haste",
            MobEffects.DIG_SPEED, 9600, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_HASTE = potion("strong_haste", "haste",
            MobEffects.DIG_SPEED, 1800, 1);

    public static final DeferredHolder<Potion, Potion> MINING_FATIGUE = potion("mining_fatigue",
            MobEffects.DIG_SLOWDOWN, 1800, 0);
    public static final DeferredHolder<Potion, Potion> LONG_MINING_FATIGUE = potion("long_mining_fatigue",
            "mining_fatigue", MobEffects.DIG_SLOWDOWN, 4800, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_MINING_FATIGUE = potion("strong_mining_fatigue",
            "mining_fatigue", MobEffects.DIG_SLOWDOWN, 900, 1);

    public static final DeferredHolder<Potion, Potion> NAUSEA = potion("nausea", MobEffects.CONFUSION, 1800, 0);
    public static final DeferredHolder<Potion, Potion> LONG_NAUSEA = potion("long_nausea", "nausea",
            MobEffects.CONFUSION, 4800, 0);

    public static final DeferredHolder<Potion, Potion> BLINDNESS = potion("blindness", MobEffects.BLINDNESS, 1800,
            0);
    public static final DeferredHolder<Potion, Potion> LONG_BLINDNESS = potion("long_blindness", "blindness",
            MobEffects.BLINDNESS, 4800, 0);

    public static final DeferredHolder<Potion, Potion> GLOWING = potion("glowing", MobEffects.GLOWING, 3600, 0);
    public static final DeferredHolder<Potion, Potion> LONG_GLOWING = potion("long_glowing", "glowing",
            MobEffects.GLOWING, 9600, 0);

    public static final DeferredHolder<Potion, Potion> DARKNESS = potion("darkness", MobEffects.DARKNESS, 1800, 0);
    public static final DeferredHolder<Potion, Potion> LONG_DARKNESS = potion("long_darkness", "darkness",
            MobEffects.DARKNESS, 4800, 0);

    public static final DeferredHolder<Potion, Potion> HUNGER = potion("hunger", MobEffects.HUNGER, 1800, 0);
    public static final DeferredHolder<Potion, Potion> LONG_HUNGER = potion("long_hunger", "hunger", MobEffects.HUNGER,
            4800, 0);
    public static final DeferredHolder<Potion, Potion> STRONG_HUNGER = potion("strong_hunger", "hunger",
            MobEffects.HUNGER, 900, 1);

    private static DeferredHolder<Potion, Potion> potion(String id, Holder<MobEffect> effect, int duration,
                                                         int amplifier) {
        return potion(id, id, effect, duration, amplifier);
    }

    // Registers a potion
    private static DeferredHolder<Potion, Potion> potion(String id, String name, Holder<MobEffect> effect,
                                                         int duration, int amplifier) {
        return POTIONS.register(id, () -> new Potion(name, new MobEffectInstance(effect, duration, amplifier)));
    }

    public static void register(IEventBus modBus) {
        POTIONS.register(modBus);
        NeoForge.EVENT_BUS.addListener(PotionRegistry::registerBrewing);
    }

    private static void registerBrewing(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder brewing = event.getBuilder();
        brewing.addMix(Potions.AWKWARD, ItemRegistry.TANNIC_POWDER.get(), HASTE);
        brewing.addMix(HASTE, Items.REDSTONE, LONG_HASTE);
        brewing.addMix(HASTE, Items.GLOWSTONE_DUST, STRONG_HASTE);
        brewing.addMix(HASTE, Items.FERMENTED_SPIDER_EYE, MINING_FATIGUE);
        brewing.addMix(LONG_HASTE, Items.FERMENTED_SPIDER_EYE, LONG_MINING_FATIGUE);
        brewing.addMix(STRONG_HASTE, Items.FERMENTED_SPIDER_EYE, STRONG_MINING_FATIGUE);
        brewing.addMix(MINING_FATIGUE, Items.REDSTONE, LONG_MINING_FATIGUE);
        brewing.addMix(MINING_FATIGUE, Items.GLOWSTONE_DUST, STRONG_MINING_FATIGUE);

        brewing.addMix(Potions.AWKWARD, BlockRegistry.DUNG.asItem(), NAUSEA);
        brewing.addMix(NAUSEA, Items.REDSTONE, LONG_NAUSEA);
        brewing.addMix(NAUSEA, Items.FERMENTED_SPIDER_EYE, BLINDNESS);
        brewing.addMix(LONG_NAUSEA, Items.FERMENTED_SPIDER_EYE, LONG_BLINDNESS);
        brewing.addMix(BLINDNESS, Items.REDSTONE, LONG_BLINDNESS);

        brewing.addMix(Potions.AWKWARD, Items.GLOW_BERRIES, GLOWING);
        brewing.addMix(GLOWING, Items.REDSTONE, LONG_GLOWING);
        brewing.addMix(GLOWING, Items.FERMENTED_SPIDER_EYE, DARKNESS);
        brewing.addMix(LONG_GLOWING, Items.FERMENTED_SPIDER_EYE, LONG_DARKNESS);
        brewing.addMix(DARKNESS, Items.REDSTONE, LONG_DARKNESS);

        brewing.addMix(Potions.AWKWARD, Items.ROTTEN_FLESH, HUNGER);
        brewing.addMix(HUNGER, Items.REDSTONE, LONG_HUNGER);
        brewing.addMix(HUNGER, Items.GLOWSTONE_DUST, STRONG_HUNGER);
    }
}
