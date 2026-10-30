package com.halex.createrancher.mixin;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import com.halex.createrancher.content.grate.BeltGrates;
import com.halex.createrancher.content.grate.GrateMetal;
import com.halex.createrancher.content.grate.GratedBelt;
import com.halex.createrancher.content.grate.client.BeltGrateClient;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltModel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;

// Stores grates on belt segments
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(BeltBlockEntity.class)
public abstract class BeltBlockEntityMixin implements GratedBelt {

    @Unique
    private static final String createrancher$GRATE_KEY = "CreateRancherGrate";

    @Unique
    private static final String createrancher$METAL_KEY = "CreateRancherGrateMetal";

    @Unique
    private boolean createrancher$grated;

    @Unique
    private GrateMetal createrancher$metal = GrateMetal.PLAIN;

    @Unique
    private CasingType createrancher$lastCasing = CasingType.NONE;

    @Override
    public boolean createrancher$isGrated() {
        return createrancher$grated;
    }

    @Override
    public void createrancher$setGrated(boolean grated) {
        createrancher$grated = grated;
    }

    @Override
    public GrateMetal createrancher$getGrateMetal() {
        return createrancher$metal;
    }

    @Override
    public void createrancher$setGrateMetal(GrateMetal metal) {
        createrancher$metal = metal;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void createrancher$writeGrate(CompoundTag compound, HolderLookup.Provider registries,
                                          boolean clientPacket, CallbackInfo ci) {
        if (createrancher$grated)
            compound.putBoolean(createrancher$GRATE_KEY, true);
        if (createrancher$grated && createrancher$metal != GrateMetal.PLAIN)
            compound.putString(createrancher$METAL_KEY, createrancher$metal.getName());
    }

    // Loads the grate
    @Inject(method = "read", at = @At("RETURN"))
    private void createrancher$readGrate(CompoundTag compound, HolderLookup.Provider registries,
                                         boolean clientPacket, CallbackInfo ci) {
        boolean wasGrated = createrancher$grated;
        GrateMetal wasMetal = createrancher$metal;
        createrancher$grated = compound.getBoolean(createrancher$GRATE_KEY);
        createrancher$metal = GrateMetal.byName(compound.getString(createrancher$METAL_KEY));
        BeltBlockEntity self = (BeltBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (!clientPacket || level == null || !level.isClientSide)
            return;
        if (wasGrated != createrancher$grated || wasMetal != createrancher$metal)
            BeltGrateClient.onGrateChanged(self);
        if (createrancher$lastCasing != self.casing)
            BeltGrateClient.onCasingChanged(self);
        createrancher$lastCasing = self.casing;
    }

    // Tells the belt model about the grate
    @ModifyReturnValue(method = "getModelData", at = @At("RETURN"))
    private ModelData createrancher$addGrate(ModelData data) {
        BeltBlockEntity self = (BeltBlockEntity) (Object) this;
        if (!createrancher$grated || !BeltGrates.canHoldGrate(self.getBlockState()))
            return data;
        return data.derive()
                .with(BeltGrates.GRATED_CASING_PROPERTY, BeltGrates.casingSlopes(self))
                .with(BeltModel.COVER_PROPERTY, false)
                .build();
    }

    // Drops the grate if the segment can't hold one any more
    @Inject(method = "tick", at = @At("HEAD"))
    private void createrancher$checkGrate(CallbackInfo ci) {
        BeltBlockEntity self = (BeltBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (createrancher$grated && level != null && !level.isClientSide
                && !BeltGrates.canHoldGrate(self.getBlockState()))
            BeltGrates.dropGrate(level, self.getBlockPos());
    }
}
