package com.halex.createrancher.content.drain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.compat.VersionCompat;
import com.halex.createrancher.index.FluidRegistry;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.fluids.FluidStack;

// Works out what an animal gives a livestock drain
public final class LivestockMilking {

    public static final int AMOUNT = 2;
    private static final int CHECK_TICKS = 20;
    private static final GameProfile MILKER = new GameProfile(UUID.fromString("5d3b8a3e-6f1c-4c2e-9a57-2f0f8b1d9c41"),
            "[Livestock Drain]");

    private static final Map<Mob, Milking> MILKINGS = new WeakHashMap<>();
    private static final Map<Filled, FluidStack> FLUIDS = new HashMap<>();
    private static RecipeManager fluidsFrom;
    private static boolean asking;

    private LivestockMilking() {
    }

    // What an animal is giving and how much has gone in
    private static class Milking {
        private FluidStack fluid = FluidStack.EMPTY;
        private ItemStack container = ItemStack.EMPTY;
        private int serving;
        private int given;
        private long checkedAt;
        private boolean stale = true;
    }

    // A filled container and the empty one it came from
    private record Filled(ItemStack filled, Item container) {

        @Override
        public boolean equals(Object other) {
            return other instanceof Filled that && container == that.container && ItemStack.isSameItemSameComponents(
                    filled, that.filled);
        }

        @Override
        public int hashCode() {
            return 31 * ItemStack.hashItemAndComponents(filled) + container.hashCode();
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PlayLevelSoundEvent.AtEntity.class,
                LivestockMilking::hush);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PlayLevelSoundEvent.AtPosition.class,
                LivestockMilking::hush);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, VanillaGameEvent.class, LivestockMilking::hush);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, EntityJoinLevelEvent.class, LivestockMilking::hush);
    }

    private static void hush(ICancellableEvent event) {
        if (asking)
            event.setCanceled(true);
    }

    // What this animal gives this tick
    public static FluidStack milk(Mob animal) {
        Milking milking = MILKINGS.computeIfAbsent(animal, key -> new Milking());
        long now = animal.level()
                .getGameTime();
        if (milking.stale || now - milking.checkedAt >= CHECK_TICKS || now < milking.checkedAt)
            check(animal, milking, now);
        if (milking.fluid.isEmpty())
            return FluidStack.EMPTY;
        return milking.fluid.copyWithAmount(Math.min(AMOUNT, milking.serving - milking.given));
    }

    // Milks the animal for real once a serving has gone in
    public static void given(Mob animal, int amount) {
        Milking milking = MILKINGS.get(animal);
        if (milking == null || amount <= 0 || milking.fluid.isEmpty())
            return;
        milking.given += amount;
        if (milking.given < milking.serving)
            return;
        milking.given -= milking.serving;
        if (animal.level() instanceof ServerLevel level)
            offer(level, animal, milking.container);
        milking.stale = true;
    }

    private static void check(Mob animal, Milking milking, long now) {
        FluidStack before = milking.fluid;
        milking.checkedAt = now;
        milking.stale = false;
        milking.fluid = FluidStack.EMPTY;
        milking.container = ItemStack.EMPTY;
        if (animal.level() instanceof ServerLevel level) {
            Entity copy = null;
            for (ItemStack container : List.of(new ItemStack(Items.BOWL), new ItemStack(Items.BUCKET))) {
                if (copy == null) {
                    copy = animal.getType()
                            .create(level);
                    if (copy == null)
                        return;
                    copy.load(animal.saveWithoutId(new CompoundTag()));
                }
                Offered offered = offer(level, copy, container);
                if (copy.isRemoved()) {
                    copy = null;
                    continue;
                }
                FluidStack fluid = fluidFor(level, offered.result(), container);
                if (!fluid.isEmpty()) {
                    milking.fluid = fluid;
                    milking.container = container;
                    milking.serving = fluid.getAmount();
                    break;
                }
                if (!offered.passed() || !offered.result()
                        .isEmpty() && !offered.result()
                        .is(container.getItem()))
                    copy = null;
            }
        }
        if (!FluidStack.isSameFluidSameComponents(before, milking.fluid))
            milking.given = 0;
    }

    // What came back from holding out a container, and whether the animal ignored it
    private record Offered(ItemStack result, boolean passed) {
    }

    // Holds out a bowl or bucket and returns what comes back
    private static Offered offer(ServerLevel level, Entity animal, ItemStack container) {
        FakePlayer player = FakePlayerFactory.get(level, MILKER);
        player.getInventory()
                .clearContent();
        player.setPos(animal.getX(), animal.getY(), animal.getZ());
        player.setItemInHand(InteractionHand.MAIN_HAND, container.copy());
        ItemStack result = ItemStack.EMPTY;
        boolean passed = false;
        asking = true;
        try {
            passed = player.interactOn(animal, InteractionHand.MAIN_HAND) == InteractionResult.PASS;
            result = player.getMainHandItem();
            if (result.isEmpty() || result.is(container.getItem()))
                for (ItemStack stack : player.getInventory().items)
                    if (!stack.isEmpty() && !stack.is(container.getItem())) {
                        result = stack;
                        break;
                    }
            result = result.copy();
        } catch (RuntimeException e) {
            CreateRancher.LOGGER.debug("Couldn't milk {} on a livestock drain", animal, e);
        } finally {
            asking = false;
            player.getInventory()
                    .clearContent();
        }
        return new Offered(result, passed);
    }

    // The fluid in a filled bowl or bucket
    public static FluidStack fluidFor(Level level, ItemStack filled, ItemStack container) {
        if (filled.isEmpty() || filled.is(container.getItem()))
            return FluidStack.EMPTY;
        if (filled.is(Items.SUSPICIOUS_STEW)) {
            FluidStack stew = new FluidStack(FluidRegistry.SUSPICIOUS_STEW.get().getSource(), 250);
            SuspiciousStewEffects effects = filled.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
            if (effects != null && !effects.effects()
                    .isEmpty())
                stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
            return stew;
        }
        RecipeManager recipes = level.getRecipeManager();
        if (recipes != fluidsFrom) {
            FLUIDS.clear();
            fluidsFrom = recipes;
        }
        return FLUIDS.computeIfAbsent(new Filled(filled.copyWithCount(1), container.getItem()), key -> lookUpFluid(
                        level, key.filled(), container))
                .copy();
    }

    private static FluidStack lookUpFluid(Level level, ItemStack filled, ItemStack container) {
        FluidStack own = spoutFluid(level, filled, container, true);
        if (!own.isEmpty())
            return own;
        FluidStack emptied = GenericItemEmptying.emptyItem(level, filled.copy(), true)
                .getFirst();
        if (!emptied.isEmpty())
            return emptied;
        return spoutFluid(level, filled, container, false);
    }

    // The fluid a spout fills the container with to make the item
    private static FluidStack spoutFluid(Level level, ItemStack filled, ItemStack container, boolean ownOnly) {
        for (RecipeHolder<?> holder : level.getRecipeManager()
                .getAllRecipesFor(AllRecipeTypes.FILLING.getType())) {
            if (ownOnly && !holder.id()
                    .getNamespace()
                    .equals(CreateRancher.MOD_ID))
                continue;
            if (!(holder.value() instanceof FillingRecipe recipe) || recipe.getIngredients()
                    .isEmpty())
                continue;
            if (!recipe.getIngredients()
                    .get(0)
                    .test(container))
                continue;
            if (!ItemStack.isSameItemSameComponents(recipe.getResultItem(level.registryAccess()), filled))
                continue;
            List<FluidStack> fluids = VersionCompat.requiredFluids(recipe);
            if (!fluids.isEmpty() && !fluids.get(0)
                    .isEmpty())
                return fluids.get(0)
                        .copy();
        }
        return FluidStack.EMPTY;
    }
}
