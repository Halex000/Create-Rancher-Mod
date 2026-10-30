package com.halex.createrancher.index;

import static com.halex.createrancher.CreateRancher.REGISTRATE;


import com.halex.createrancher.content.grate.ItemGrateItem;
import com.halex.createrancher.content.manure.ManureItem;
import com.halex.createrancher.content.milk.CanOfMilkItem;
import com.halex.createrancher.content.milk.ChocolateMilkBucketItem;
import com.halex.createrancher.content.milk.MilkDrink;
import com.halex.createrancher.content.net.FishingNetItem;
import com.halex.createrancher.content.satchel.SatchelItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.SignItem;

public class ItemRegistry {

    public static final ItemEntry<Item> ALFALFA = REGISTRATE
            .item("alfalfa", Item::new)
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<ItemNameBlockItem> ALFALFA_SEEDS = REGISTRATE
            .item("alfalfa_seeds", p -> new ItemNameBlockItem(BlockRegistry.ALFALFA_CROP.get(), p))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<Item> ALFALFA_POWDER = REGISTRATE
            .item("alfalfa_powder", Item::new)
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<Item> ALFALFA_PELLETS = REGISTRATE
            .item("alfalfa_pellets", Item::new)
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<ManureItem> MANURE = REGISTRATE
            .item("manure", ManureItem::new)
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<ItemGrateItem> ITEM_GRATE = REGISTRATE
            .item("item_grate", ItemGrateItem::new)
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<Item> TANNIC_POWDER = REGISTRATE
            .item("tannic_powder", Item::new)
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<SignItem> STAINED_SIGN = REGISTRATE
            .item("stained_sign", p -> new SignItem(p.stacksTo(16), BlockRegistry.STAINED_SIGN.get(),
                    BlockRegistry.STAINED_WALL_SIGN.get()))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<HangingSignItem> STAINED_HANGING_SIGN = REGISTRATE
            .item("stained_hanging_sign", p -> new HangingSignItem(BlockRegistry.STAINED_HANGING_SIGN.get(),
                    BlockRegistry.STAINED_WALL_HANGING_SIGN.get(), p.stacksTo(16)))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<SatchelItem> SATCHEL = REGISTRATE
            .item("satchel", SatchelItem::new)
            .properties(p -> p.stacksTo(1))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<FishingNetItem> FISHING_NET = REGISTRATE
            .item("fishing_net", FishingNetItem::new)
            .properties(p -> p.durability(FishingNetItem.DURABILITY))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<Item> BEEF_STEW = bowlOfStew("beef_stew");
    public static final ItemEntry<Item> CHICKEN_STEW = bowlOfStew("chicken_stew");
    public static final ItemEntry<Item> PORK_STEW = bowlOfStew("pork_stew");
    public static final ItemEntry<Item> MUTTON_STEW = bowlOfStew("mutton_stew");

    public static final ItemEntry<Item> EMPTY_CAN = REGISTRATE
            .item("empty_can", Item::new)
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<Item> CAN_OF_MUSHROOM_STEW = REGISTRATE
            .item("can_of_mushroom_stew", Item::new)
            .properties(p -> p.food(cannedStew(6).build()))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<Item> CAN_OF_RABBIT_STEW = REGISTRATE
            .item("can_of_rabbit_stew", Item::new)
            .properties(p -> p.food(heartyStew().build()))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<Item> CAN_OF_BEETROOT_SOUP = REGISTRATE
            .item("can_of_beetroot_soup", Item::new)
            .properties(p -> p.food(cannedStew(6).build()))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<Item> CAN_OF_BEEF_STEW = cannedHeartyStew("can_of_beef_stew");
    public static final ItemEntry<Item> CAN_OF_CHICKEN_STEW = cannedHeartyStew("can_of_chicken_stew");
    public static final ItemEntry<Item> CAN_OF_PORK_STEW = cannedHeartyStew("can_of_pork_stew");
    public static final ItemEntry<Item> CAN_OF_MUTTON_STEW = cannedHeartyStew("can_of_mutton_stew");

    public static final ItemEntry<SuspiciousStewItem> CAN_OF_SUSPICIOUS_STEW = REGISTRATE
            .item("can_of_suspicious_stew", SuspiciousStewItem::new)
            .properties(p -> p.food(cannedStew(6).alwaysEdible()
                            .build())
                    .component(DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.EMPTY))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<CanOfMilkItem> CAN_OF_MILK = REGISTRATE
            .item("can_of_milk", p -> new CanOfMilkItem(MilkDrink.MILK, p))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<CanOfMilkItem> CAN_OF_CHOCOLATE_MILK = REGISTRATE
            .item("can_of_chocolate_milk", p -> new CanOfMilkItem(MilkDrink.CHOCOLATE_MILK, p))
            .transform(ItemRegistry::handmade)
            .register();

    public static final ItemEntry<ChocolateMilkBucketItem> CHOCOLATE_MILK_BUCKET = REGISTRATE
            .item("chocolate_milk_bucket", p -> new ChocolateMilkBucketItem(FluidRegistry.CHOCOLATE_MILK.get()
                    .getSource(), p))
            .properties(p -> p.stacksTo(1)
                    .craftRemainder(Items.BUCKET))
            .transform(ItemRegistry::handmade)
            .register();

    // Food for a can of stew
    private static FoodProperties.Builder cannedStew(int nutrition) {
        return new FoodProperties.Builder().nutrition(nutrition)
                .saturationModifier(.6f);
    }

    // Food for the hearty stews
    public static FoodProperties.Builder heartyStew() {
        return new FoodProperties.Builder().nutrition(HEARTY_HUNGER)
                .saturationModifier(HEARTY_SATURATION / (HEARTY_HUNGER * 2f));
    }

    private static final int HEARTY_HUNGER = 14;
    private static final float HEARTY_SATURATION = 18;

    private static ItemEntry<Item> bowlOfStew(String name) {
        return REGISTRATE.item(name, Item::new)
                .properties(p -> p.stacksTo(1)
                        .food(heartyStew().usingConvertsTo(Items.BOWL)
                                .build()))
                .transform(ItemRegistry::handmade)
                .register();
    }

    private static ItemEntry<Item> cannedHeartyStew(String name) {
        return REGISTRATE.item(name, Item::new)
                .properties(p -> p.food(heartyStew().build()))
                .transform(ItemRegistry::handmade)
                .register();
    }

    // For items with handmade assets in src/main/resources
    private static <T extends Item> ItemBuilder<T, CreateRegistrate> handmade(ItemBuilder<T, CreateRegistrate> builder) {
        return builder.model(NonNullBiConsumer.noop())
                .setData(ProviderType.LANG, NonNullBiConsumer.noop());
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ItemRegistry::heartierRabbitStew);
    }

    // Makes vanilla's rabbit stew more filling
    private static void heartierRabbitStew(ModifyDefaultComponentsEvent event) {
        event.modify(Items.RABBIT_STEW, builder -> builder.set(DataComponents.FOOD, heartyStew()
                .usingConvertsTo(Items.BOWL)
                .build()));
    }
}
