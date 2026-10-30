package com.halex.createrancher.content.burner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.halex.createrancher.CreateRancher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;

// Loads Liquid Blaze Burner fuels from data packs
public class LiquidBlazeBurnerFuels extends SimpleJsonResourceReloadListener {

    public static final String FOLDER = "liquid_blaze_burner_fuels";
    private static final Gson GSON = new GsonBuilder().create();

    // A fuel, either a fluid or a fluid tag
    public record Fuel(Optional<ResourceLocation> fluid, Optional<TagKey<Fluid>> tag, int burnTime,
                       boolean superheated) {
        public static final Codec<Fuel> CODEC = RecordCodecBuilder.<Fuel>create(instance -> instance.group(
                        ResourceLocation.CODEC.optionalFieldOf("fluid")
                                .forGetter(Fuel::fluid),
                        TagKey.codec(Registries.FLUID)
                                .optionalFieldOf("tag")
                                .forGetter(Fuel::tag),
                        Codec.intRange(1, Integer.MAX_VALUE)
                                .fieldOf("burn_time")
                                .forGetter(Fuel::burnTime),
                        Codec.BOOL.optionalFieldOf("superheated", false)
                                .forGetter(Fuel::superheated))
                .apply(instance, Fuel::new))
                .validate(fuel -> fuel.fluid()
                        .isPresent() != fuel.tag()
                        .isPresent() ? DataResult.success(fuel) : DataResult.error(() -> "A fuel needs a fluid or a "
                        + "tag, not both"));

        public static Fuel ofFluid(ResourceLocation fluid, int burnTime, boolean superheated) {
            return new Fuel(Optional.of(fluid), Optional.empty(), burnTime, superheated);
        }

        public static Fuel ofTag(TagKey<Fluid> tag, int burnTime, boolean superheated) {
            return new Fuel(Optional.empty(), Optional.of(tag), burnTime, superheated);
        }

        // Whether this fluid is this fuel
        public boolean matches(Fluid fluid) {
            if (tag.isPresent())
                return fluid.is(tag.get());
            return fluid.isSame(BuiltInRegistries.FLUID.get(this.fluid.get()));
        }

        // Every fluid that counts as this fuel
        public List<Fluid> fluids() {
            if (tag.isPresent())
                return BuiltInRegistries.FLUID.getTag(tag.get())
                        .map(set -> set.stream()
                                .map(Holder::value)
                                .filter(fluid -> fluid.defaultFluidState()
                                        .isSource())
                                .toList())
                        .orElse(List.of());
            return BuiltInRegistries.FLUID.getOptional(this.fluid.get())
                    .map(List::of)
                    .orElse(List.of());
        }

        public boolean available() {
            return !fluids().isEmpty();
        }
    }

    private static volatile List<Fuel> fuels = List.of();

    public LiquidBlazeBurnerFuels() {
        super(GSON, FOLDER);
    }

    // The fuel for a fluid, or null
    @Nullable
    public static Fuel get(Fluid fluid) {
        for (Fuel fuel : fuels)
            if (fuel.fluid()
                    .isPresent() && fuel.matches(fluid))
                return fuel;
        for (Fuel fuel : fuels)
            if (fuel.tag()
                    .isPresent() && fuel.matches(fluid))
                return fuel;
        return null;
    }

    // Every fuel from the data packs
    public static List<Fuel> all() {
        return fuels;
    }

    // The fuels JEI should show
    public static List<Fuel> available() {
        return fuels.stream()
                .filter(Fuel::available)
                .toList();
    }

    // Stores the fuels the server sent
    public static void receive(List<Fuel> fromServer) {
        fuels = List.copyOf(fromServer);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        List<Fuel> loaded = new ArrayList<>();
        files.forEach((id, json) -> Fuel.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> CreateRancher.LOGGER.error("Couldn't read liquid blaze burner fuel {}: {}", id,
                        error))
                .ifPresent(loaded::add));
        fuels = List.copyOf(loaded);
        CreateRancher.LOGGER.info("Loaded {} liquid blaze burner fuels", loaded.size());
    }
}
