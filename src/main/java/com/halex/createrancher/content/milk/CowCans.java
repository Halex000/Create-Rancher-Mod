package com.halex.createrancher.content.milk;

import com.halex.createrancher.index.ItemRegistry;
import com.halex.createrancher.mixin.MushroomCowAccessor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// Fills empty cans from cows, goats and mooshrooms
public class CowCans {

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CowCans::onInteract);
    }

    private static void onInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack held = event.getItemStack();
        if (!held.is(ItemRegistry.EMPTY_CAN.get()) || !(event.getTarget() instanceof Animal animal) || animal.isBaby()
                || !(animal instanceof Cow || animal instanceof Goat))
            return;
        Player player = event.getEntity();
        Level level = event.getLevel();
        if (!level.isClientSide) {
            ItemStack filled;
            SoundEvent sound;
            if (animal instanceof MushroomCow mooshroom) {
                MushroomCowAccessor stew = (MushroomCowAccessor) mooshroom;
                SuspiciousStewEffects effects = stew.createrancher$getStewEffects();
                if (effects != null) {
                    filled = ItemRegistry.CAN_OF_SUSPICIOUS_STEW.asStack();
                    filled.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
                    stew.createrancher$setStewEffects(null);
                    sound = SoundEvents.MOOSHROOM_MILK_SUSPICIOUSLY;
                } else {
                    filled = ItemRegistry.CAN_OF_MUSHROOM_STEW.asStack();
                    sound = SoundEvents.MOOSHROOM_MILK;
                }
            } else {
                filled = ItemRegistry.CAN_OF_MILK.asStack();
                sound = animal instanceof Goat goat ? goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_MILK
                        : SoundEvents.GOAT_MILK : SoundEvents.COW_MILK;
            }
            player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(held, player, filled, false));
            level.playSound(null, animal.getX(), animal.getY(), animal.getZ(), sound, animal.getSoundSource(), 1, 1);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
    }
}
