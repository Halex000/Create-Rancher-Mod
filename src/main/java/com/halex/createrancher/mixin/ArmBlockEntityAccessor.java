package com.halex.createrancher.mixin;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;

import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

// Gets at parts of Create's mechanical arm
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(ArmBlockEntity.class)
public interface ArmBlockEntityAccessor {

    // Where the arm takes items from
    @Accessor(value = "inputs", remap = false)
    List<ArmInteractionPoint> createrancher$getInputs();

    // Where the arm puts them
    @Accessor(value = "outputs", remap = false)
    List<ArmInteractionPoint> createrancher$getOutputs();

    // A redstone signal is stopping the arm
    @Accessor(value = "redstoneLocked", remap = false)
    boolean createrancher$isRedstoneLocked();

    // Whether the arm still needs to work out its targets
    @Accessor(value = "updateInteractionPoints", remap = false)
    boolean createrancher$isUpdatingInteractionPoints();

    // Replaces the arm's saved targets
    @Accessor(value = "interactionPointTag", remap = false)
    void createrancher$setInteractionPointTag(net.minecraft.nbt.ListTag points);

    // Makes the arm reload its targets
    @Accessor(value = "updateInteractionPoints", remap = false)
    void createrancher$setUpdatingInteractionPoints(boolean update);

    // Whether the arm's hand has goggles
    @Accessor(value = "goggles", remap = false)
    boolean createrancher$hasGoggles();

    // What the arm is doing
    @Accessor(value = "phase", remap = false)
    ArmBlockEntity.Phase createrancher$getPhase();

    @Accessor(value = "phase", remap = false)
    void createrancher$setPhase(ArmBlockEntity.Phase phase);

    @Accessor(value = "chasedPointProgress", remap = false)
    float createrancher$getChasedPointProgress();

    @Accessor(value = "chasedPointProgress", remap = false)
    void createrancher$setChasedPointProgress(float progress);

    @Accessor(value = "chasedPointIndex", remap = false)
    void createrancher$setChasedPointIndex(int index);

    @Accessor(value = "baseAngle", remap = false)
    LerpedFloat createrancher$getBaseAngle();

    @Accessor(value = "lowerArmAngle", remap = false)
    LerpedFloat createrancher$getLowerArmAngle();

    @Accessor(value = "upperArmAngle", remap = false)
    LerpedFloat createrancher$getUpperArmAngle();

    @Accessor(value = "headAngle", remap = false)
    LerpedFloat createrancher$getHeadAngle();

    // Replaces the item in the arm's hand
    @Accessor(value = "heldItem", remap = false)
    void createrancher$setHeldItem(ItemStack stack);
}
