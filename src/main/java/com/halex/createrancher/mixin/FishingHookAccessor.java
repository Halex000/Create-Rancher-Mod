package com.halex.createrancher.mixin;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;

// Gets at private parts of the fishing hook
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(FishingHook.class)
public interface FishingHookAccessor {

    // Ticks left to reel in a catch
    @Accessor("nibble")
    int createrancher$getNibble();

    @Accessor("nibble")
    void createrancher$setNibble(int nibble);

    // Whether a fish is biting
    @Accessor("biting")
    boolean createrancher$isBiting();

    @Accessor("luck")
    int createrancher$getLuck();

    @Mutable
    @Accessor("luck")
    void createrancher$setLuck(int luck);

    @Mutable
    @Accessor("lureSpeed")
    void createrancher$setLureSpeed(int lureSpeed);

    @Accessor("openWater")
    void createrancher$setOpenWater(boolean openWater);

    @Invoker("calculateOpenWater")
    boolean createrancher$calculateOpenWater(BlockPos pos);
}
