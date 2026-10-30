package com.halex.createrancher.index;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.crate.CrateMenu;
import com.halex.createrancher.content.satchel.SatchelMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MenuRegistry {

    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU,
            CreateRancher.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CrateMenu>> CRATE = MENUS.register("crate",
            () -> new MenuType<>(CrateMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<SatchelMenu>> SATCHEL = MENUS.register("satchel",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new SatchelMenu(id, inventory,
                    buf.readEnum(InteractionHand.class))));

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
