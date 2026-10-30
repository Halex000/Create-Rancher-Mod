package com.halex.createrancher.mixin;

import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.halex.createrancher.content.fluid.LavaInteractions;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.neoforged.neoforge.event.EventHooks;

// Lava flowing down onto the mod's fluids makes stone
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void createrancher$intoTheModsFluids(LevelAccessor level, BlockPos pos, BlockState state,
                                                 Direction direction, FluidState spreading, CallbackInfo ci) {
        if (direction != Direction.DOWN || !spreading.is(FluidTags.LAVA))
            return;
        LavaInteractions.Interaction interaction = LavaInteractions.of(level.getFluidState(pos)
                .getType());
        if (interaction == null)
            return;
        if (state.getBlock() instanceof LiquidBlock)
            level.setBlock(pos, EventHooks.fireFluidPlaceBlockEvent(level, pos, pos, interaction.fromAbove()
                    .defaultBlockState()), 3);
        level.levelEvent(1501, pos, 0);
        ci.cancel();
    }
}
