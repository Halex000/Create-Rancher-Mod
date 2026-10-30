package com.halex.createrancher.content.villager;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.halex.createrancher.index.ItemRegistry;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

// Adds alfalfa and fishing nets to villager trades
public class VillagerTradeChanges {

    private static final int ALFALFA_WANTED = 20;
    private static final int ALFALFA_STOCK = 16;
    private static final int ALFALFA_EXPERIENCE = 2;

    private static final int NET_BASE_EMERALDS = 3;
    private static final int NET_STOCK = 3;
    private static final int NET_EXPERIENCE = 10;
    private static final float NET_PRICE_MULTIPLIER = 0.2f;
    private static final String VANILLA_ENCHANTED_ITEM = "net.minecraft.world.entity.npc.VillagerTrades$EnchantedItemForEmeralds";

    // Pays an emerald for items
    private record EmeraldForItems(ItemCost wanted, int maxUses, int villagerXp) implements ItemListing {

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return new MerchantOffer(wanted, new ItemStack(Items.EMERALD), maxUses, villagerXp, 0.05f);
        }
    }

    // Sells an enchanted item
    private record EnchantedItemForEmeralds(Item item, int baseEmeraldCost, int maxUses, int villagerXp,
                                            float priceMultiplier) implements ItemListing {

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            int level = 5 + random.nextInt(15);
            RegistryAccess registries = trader.level()
                    .registryAccess();
            Optional<HolderSet.Named<Enchantment>> enchantments = registries.registryOrThrow(Registries.ENCHANTMENT)
                    .getTag(EnchantmentTags.ON_TRADED_EQUIPMENT);
            ItemStack enchanted = EnchantmentHelper.enchantItem(random, new ItemStack(item), level, registries,
                    enchantments);
            ItemCost price = new ItemCost(Items.EMERALD, Math.min(baseEmeraldCost + level, 64));
            return new MerchantOffer(price, enchanted, maxUses, villagerXp, priceMultiplier);
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VillagerTradeChanges::onTrades);
    }

    private static void onTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.FARMER)
            event.getTrades()
                    .get(1)
                    .add(new EmeraldForItems(new ItemCost(ItemRegistry.ALFALFA.get(), ALFALFA_WANTED), ALFALFA_STOCK,
                            ALFALFA_EXPERIENCE));
        if (event.getType() == VillagerProfession.FISHERMAN)
            rodOrNet(event.getTrades()
                    .get(3));
    }

    // Makes the fisherman's enchanted rod sometimes a net
    private static void rodOrNet(@Nullable List<ItemListing> journeyman) {
        if (journeyman == null)
            return;
        ItemListing net = new EnchantedItemForEmeralds(ItemRegistry.FISHING_NET.get(), NET_BASE_EMERALDS, NET_STOCK,
                NET_EXPERIENCE, NET_PRICE_MULTIPLIER);
        for (int i = 0; i < journeyman.size(); i++) {
            ItemListing rod = journeyman.get(i);
            if (rod.getClass()
                    .getName()
                    .equals(VANILLA_ENCHANTED_ITEM))
                journeyman.set(i, (trader, random) -> (random.nextBoolean() ? rod : net).getOffer(trader, random));
        }
    }
}
