package com.halex.createrancher.content.joist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.halex.createrancher.CreateRancher;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

// Finds which woods get a joist and registers them
public final class JoistWoods {

    public static final ResourceLocation PLANKS = ResourceLocation.withDefaultNamespace("planks");
    public static final ResourceLocation LOGS = ResourceLocation.withDefaultNamespace("logs");
    public static final ResourceLocation BAMBOO_BLOCKS = ResourceLocation.withDefaultNamespace("bamboo_blocks");
    public static final ResourceLocation STRIPPED_LOGS = ResourceLocation.fromNamespaceAndPath("c", "stripped_logs");
    private static final String[] STRIPPED_KINDS = {"_log", "_stem", "_block"};

    private static final List<JoistWood> WOODS = new ArrayList<>();

    private JoistWoods() {
    }

    public static List<JoistWood> all() {
        return Collections.unmodifiableList(WOODS);
    }

    // A wood found for a joist
    public record Found(ResourceLocation joist, ResourceLocation planks, ResourceLocation strippedLog, String name) {
    }

    // Pairs planks with stripped logs
    public static List<Found> find(Function<ResourceLocation, Set<ResourceLocation>> tags,
                                   Predicate<ResourceLocation> exists) {
        Set<ResourceLocation> stripped = new LinkedHashSet<>(tags.apply(STRIPPED_LOGS));
        for (ResourceLocation tag : List.of(LOGS, BAMBOO_BLOCKS))
            for (ResourceLocation block : tags.apply(tag))
                if (block.getPath()
                        .startsWith("stripped_"))
                    stripped.add(block);
        List<Found> found = new ArrayList<>();
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (ResourceLocation planks : tags.apply(PLANKS)) {
            String path = planks.getPath();
            if (!path.endsWith("_planks") || !exists.test(planks))
                continue;
            String wood = path.substring(0, path.length() - "_planks".length());
            for (String kind : STRIPPED_KINDS) {
                ResourceLocation log = ResourceLocation.fromNamespaceAndPath(planks.getNamespace(), "stripped_" + wood
                        + kind);
                if (!stripped.contains(log) || !exists.test(log))
                    continue;
                ResourceLocation joist = joistId(planks.getNamespace(), wood);
                if (ids.add(joist))
                    found.add(new Found(joist, planks, log, englishName(wood)));
                break;
            }
        }
        found.sort(Comparator.comparingInt(f -> order(f.planks()
                .getNamespace())));
        return found;
    }

    private static int order(String namespace) {
        return namespace.equals("minecraft") ? 0 : namespace.equals(CreateRancher.MOD_ID) ? 1 : 2;
    }

    // The joist's id. Other mods' joists get the mod's name in it.
    static ResourceLocation joistId(String namespace, String wood) {
        boolean ours = namespace.equals("minecraft") || namespace.equals(CreateRancher.MOD_ID);
        return CreateRancher.asResource((ours ? "" : namespace + "_") + wood + "_joist");
    }

    // "dark_oak" -> "Dark Oak"
    static String englishName(String wood) {
        StringBuilder out = new StringBuilder();
        for (String word : wood.split("_")) {
            if (word.isEmpty())
                continue;
            if (!out.isEmpty())
                out.append(' ');
            out.append(word.substring(0, 1)
                            .toUpperCase(Locale.ROOT))
                    .append(word.substring(1));
        }
        return out.toString();
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EventPriority.LOWEST, JoistWoods::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.BLOCK) {
            for (Found found : find(ModResources::blockTag, BuiltInRegistries.BLOCK::containsKey)) {
                Block planks = BuiltInRegistries.BLOCK.get(found.planks());
                JoistBlock block = new JoistBlock(BlockBehaviour.Properties.ofLegacyCopy(planks), planks);
                WOODS.add(new JoistWood(found, block));
                event.register(Registries.BLOCK, found.joist(), () -> block);
            }
            CreateRancher.LOGGER.info("Made {} joists: {}", WOODS.size(), WOODS.stream()
                    .map(wood -> wood.id()
                            .getPath())
                    .toList());
        }
        if (event.getRegistryKey() == Registries.ITEM)
            for (JoistWood wood : WOODS) {
                BlockItem item = new JoistItem(wood.block(), new Item.Properties());
                wood.setItem(item);
                event.register(Registries.ITEM, wood.id(), () -> item);
            }
    }
}
