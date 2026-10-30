package com.halex.createrancher.mixin;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.item.component.SuspiciousStewEffects;

// Gets at a brown mooshroom's suspicious stew effect
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(MushroomCow.class)
public interface MushroomCowAccessor {

    @Nullable
    @Accessor("stewEffects")
    SuspiciousStewEffects createrancher$getStewEffects();

    @Accessor("stewEffects")
    void createrancher$setStewEffects(@Nullable SuspiciousStewEffects effects);
}
