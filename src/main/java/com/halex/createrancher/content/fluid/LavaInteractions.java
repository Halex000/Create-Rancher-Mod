package com.halex.createrancher.content.fluid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.halex.createrancher.index.FluidRegistry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;

// What the mod's fluids turn into when they meet lava
public final class LavaInteractions {

    // The blocks a fluid makes with lava
    public record Interaction(Supplier<? extends FlowingFluid> fluid, Block withSource, Block withFlowing,
                              Block fromAbove) {
    }

    private static final Map<Supplier<? extends FlowingFluid>, Interaction> ALL = new LinkedHashMap<>();

    static {
        add(FluidRegistry.TANNIN, Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE);
        add(FluidRegistry.BIOFUEL, Blocks.BLACKSTONE, Blocks.BLACKSTONE);
        for (Supplier<? extends FlowingFluid> stew : List.of(FluidRegistry.MUSHROOM_STEW, FluidRegistry.RABBIT_STEW,
                FluidRegistry.BEETROOT_SOUP, FluidRegistry.BEEF_STEW, FluidRegistry.CHICKEN_STEW,
                FluidRegistry.PORK_STEW, FluidRegistry.MUTTON_STEW))
            add(stew, Blocks.DRIPSTONE_BLOCK, Blocks.DRIPSTONE_BLOCK);
    }

    private LavaInteractions() {
    }

    // A lava source always turns to obsidian
    private static void add(Supplier<? extends FlowingFluid> fluid, Block cobble, Block stone) {
        ALL.put(fluid, new Interaction(fluid, Blocks.OBSIDIAN, cobble, stone));
    }

    // What lava and this fluid make, or null
    @Nullable
    public static Interaction of(Fluid fluid) {
        for (Interaction interaction : ALL.values())
            if (interaction.fluid()
                    .get()
                    .isSame(fluid))
                return interaction;
        return null;
    }

    // Registers the lava interactions
    public static void register() {
        for (Interaction interaction : ALL.values())
            FluidInteractionRegistry.addInteraction(NeoForgeMod.LAVA_TYPE.value(),
                    new FluidInteractionRegistry.InteractionInformation(interaction.fluid()
                            .get()
                            .getFluidType(), lava -> (lava.isSource() ? interaction.withSource()
                            : interaction.withFlowing()).defaultBlockState()));
    }
}
