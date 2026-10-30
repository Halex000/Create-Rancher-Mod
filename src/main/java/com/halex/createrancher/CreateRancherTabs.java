package com.halex.createrancher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.halex.createrancher.content.stew.SuspiciousStews;
import com.halex.createrancher.content.joist.JoistItem;
import com.halex.createrancher.content.joist.JoistWood;
import com.halex.createrancher.content.joist.JoistWoods;
import com.halex.createrancher.index.BlockRegistry;
import com.halex.createrancher.index.ItemRegistry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CreateRancherTabs {

    private static final DeferredRegister<CreativeModeTab> TAB_REGISTER = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, CreateRancher.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE = TAB_REGISTER.register("base", CreateRancherTabs::base);

    public static void register(IEventBus modBus) {
        TAB_REGISTER.register(modBus);
        modBus.addListener(EventPriority.LOWEST, CreateRancherTabs::suspiciousStews);
        modBus.addListener(EventPriority.LOWEST, CreateRancherTabs::joists);
        modBus.addListener(EventPriority.LOWEST, CreateRancherTabs::arrange);
    }

    private static final List<String> ORDER = List.of(
            "mechanical_herder", "mechanical_feeder", "mechanical_shearer", "mechanical_fisher", "livestock_drain",
            "auger", "smart_depot", "liquid_blaze_burner", "item_grate", "crate", "tannic_powder",
            "stained_log", "stained_wood", "stripped_stained_log", "stripped_stained_wood", "stained_planks",
            "stained_stairs", "stained_slab", "stained_fence", "stained_fence_gate", "stained_door",
            "stained_trapdoor", "stained_pressure_plate", "stained_button", "stained_sign", "stained_hanging_sign",
            "manure_block", "manure_farmland", "dung", "manure", "wild_alfalfa", "alfalfa_bale", "alfalfa_seeds",
            "alfalfa", "alfalfa_powder", "alfalfa_pellets",
            "satchel", "fishing_net", "beef_stew", "chicken_stew", "pork_stew", "mutton_stew", "*joists",
            "empty_can", "can_of_milk", "can_of_chocolate_milk", "can_of_mushroom_stew", "can_of_rabbit_stew",
            "can_of_beetroot_soup", "can_of_beef_stew", "can_of_chicken_stew", "can_of_pork_stew",
            "can_of_mutton_stew", "can_of_suspicious_stew", "chocolate_milk_bucket", "tannin_bucket",
            "biofuel_bucket", "mushroom_stew_bucket", "rabbit_stew_bucket", "beetroot_soup_bucket",
            "beef_stew_bucket", "chicken_stew_bucket", "pork_stew_bucket", "mutton_stew_bucket");

    private static void arrange(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != BASE.getKey())
            return;
        CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        List<ItemStack> entries = new ArrayList<>(event.getParentEntries());
        List<Integer> places = new ArrayList<>();
        int last = -1;
        for (ItemStack stack : entries) {
            int place = place(stack);
            last = place < 0 ? last : place;
            places.add(last);
        }
        List<Integer> sorted = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++)
            sorted.add(i);
        sorted.sort(Comparator.comparingInt(places::get));
        for (ItemStack stack : entries)
            event.remove(stack, visibility);
        for (int i : sorted)
            event.accept(entries.get(i), visibility);
    }

    private static int place(ItemStack stack) {
        if (stack.getItem() instanceof JoistItem)
            return ORDER.indexOf("*joists");
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace()
                .equals(CreateRancher.MOD_ID) ? ORDER.indexOf(id.getPath()) : -1;
    }

    // Adds the joists to the creative tab
    private static void joists(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != BASE.getKey())
            return;
        CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        ItemStack after = ItemRegistry.STAINED_HANGING_SIGN.asStack();
        boolean anchored = event.getParentEntries()
                .contains(after);
        for (JoistWood wood : JoistWoods.all()) {
            ItemStack joist = new ItemStack(wood.item());
            if (anchored)
                event.insertAfter(after, joist, visibility);
            else
                event.accept(joist, visibility);
            after = joist;
        }
    }

    private static CreativeModeTab base(ResourceLocation id) {
        return CreativeModeTab.builder()
                .title(Component.translatable("item_group.createrancher.base"))
                .icon(BlockRegistry.MECHANICAL_HERDER::asStack)
                .build();
    }

    // Adds a can of suspicious stew for each effect
    private static void suspiciousStews(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != BASE.getKey())
            return;
        List<SuspiciousStewEffects> effects = SuspiciousStews.allEffects();
        replace(event, ItemRegistry.CAN_OF_SUSPICIOUS_STEW.get(), ItemRegistry.CAN_OF_MUTTON_STEW.asStack(), effects);
    }

    private static void replace(BuildCreativeModeTabContentsEvent event, Item item, ItemStack after,
                                List<SuspiciousStewEffects> effects) {
        CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        ItemStack plain = new ItemStack(item);
        if (event.getParentEntries()
                .contains(plain))
            event.remove(plain, visibility);
        boolean anchored = event.getParentEntries()
                .contains(after);
        for (SuspiciousStewEffects effect : effects) {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effect);
            if (anchored)
                event.insertAfter(after, stack, visibility);
            else
                event.accept(stack, visibility);
            after = stack;
            anchored = true;
        }
    }
}
