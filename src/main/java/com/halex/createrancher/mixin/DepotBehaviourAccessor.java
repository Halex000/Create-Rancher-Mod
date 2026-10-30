package com.halex.createrancher.mixin;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.items.ItemStackHandler;

// Gets at what a depot holds
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(DepotBehaviour.class)
public interface DepotBehaviourAccessor {

    // The item on top
    @Nullable
    @Accessor(value = "heldItem", remap = false)
    TransportedItemStack createrancher$getHeldItem();

    // Replaces the item on top
    @Accessor(value = "heldItem", remap = false)
    void createrancher$setHeldItem(@Nullable TransportedItemStack stack);

    // Items on their way onto the depot
    @Accessor(value = "incoming", remap = false)
    List<TransportedItemStack> createrancher$getIncoming();

    // What a machine made that waits to go back on top
    @Accessor(value = "processingOutputBuffer", remap = false)
    ItemStackHandler createrancher$getOutputBuffer();
}
