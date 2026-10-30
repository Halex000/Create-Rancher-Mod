package com.halex.createrancher.mixin;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.halex.createrancher.index.FluidRegistry;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity.FuelType;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// Lets biofuel buckets superheat Create's blaze burner
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class BlazeBurnerBlockEntityMixin {

    @Unique
    private static final String createrancher$BIOFUEL_KEY = "CreateRancherBiofuel";

    @Shadow
    protected FuelType activeFuel;
    @Shadow
    protected int remainingBurnTime;
    @Shadow
    public boolean isCreative;

    @Unique
    private boolean createrancher$biofuel;

    @Shadow
    protected abstract void playSound();

    @Inject(method = "tryUpdateFuel", at = @At("HEAD"), cancellable = true)
    private void createrancher$takeBiofuel(ItemStack stack, boolean forceOverflow, boolean simulate,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (!stack.is(FluidRegistry.BIOFUEL.get()
                .getBucket()))
            return;
        BlazeBurnerBlockEntity self = (BlazeBurnerBlockEntity) (Object) this;
        if (isCreative || activeFuel == FuelType.SPECIAL && remainingBurnTime > BlazeBurnerBlockEntity.INSERTION_THRESHOLD) {
            cir.setReturnValue(false);
            return;
        }
        int burnTime = stack.getBurnTime(null) + (activeFuel == FuelType.SPECIAL ? remainingBurnTime : 0);
        cir.setReturnValue(true);
        if (simulate)
            return;
        activeFuel = FuelType.SPECIAL;
        remainingBurnTime = burnTime;
        createrancher$biofuel = true;
        Level level = self.getLevel();
        if (level == null)
            return;
        if (level.isClientSide) {
            self.spawnParticleBurst(true);
            return;
        }
        HeatLevel before = self.getHeatLevelFromBlock();
        playSound();
        self.updateBlockState();
        if (before != self.getHeatLevelFromBlock())
            level.playSound(null, self.getBlockPos(), SoundEvents.BLAZE_AMBIENT, SoundSource.BLOCKS,
                    .125f + level.random.nextFloat() * .125f, 1.15f - level.random.nextFloat() * .25f);
    }

    // Other fuels burn like normal
    @Inject(method = "tryUpdateFuel", at = @At("RETURN"))
    private void createrancher$otherFuel(ItemStack stack, boolean forceOverflow, boolean simulate,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!simulate && cir.getReturnValue() && !stack.is(FluidRegistry.BIOFUEL.get()
                .getBucket()))
            createrancher$biofuel = false;
    }

    // Puts biofuel out when it's used up
    @Inject(method = "tick", at = @At("HEAD"))
    private void createrancher$goOut(CallbackInfo ci) {
        BlazeBurnerBlockEntity self = (BlazeBurnerBlockEntity) (Object) this;
        if (!createrancher$biofuel || self.getLevel() == null || self.getLevel().isClientSide)
            return;
        if (isCreative || activeFuel != FuelType.SPECIAL) {
            createrancher$biofuel = false;
            return;
        }
        if (remainingBurnTime <= 1) {
            activeFuel = FuelType.NONE;
            createrancher$biofuel = false;
        }
    }

    @Inject(method = "write", at = @At("HEAD"))
    private void createrancher$write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket,
                                     CallbackInfo ci) {
        if (createrancher$biofuel)
            tag.putBoolean(createrancher$BIOFUEL_KEY, true);
    }

    @Inject(method = "read", at = @At("HEAD"))
    private void createrancher$read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket,
                                    CallbackInfo ci) {
        createrancher$biofuel = tag.getBoolean(createrancher$BIOFUEL_KEY);
    }
}
