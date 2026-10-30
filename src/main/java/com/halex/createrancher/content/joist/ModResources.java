package com.halex.createrancher.content.joist;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.halex.createrancher.CreateRancher;

import net.minecraft.DetectedVersion;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

// Reads tags, blockstates and models from mod jars at startup
public final class ModResources {

    private ModResources() {
    }

    // Every copy of a file in the game and mods
    public static List<JsonElement> readAll(String path) {
        List<JsonElement> found = new ArrayList<>();
        try (InputStream in = DetectedVersion.class.getResourceAsStream("/" + path)) {
            if (in != null)
                parse(in).ifPresent(found::add);
        } catch (IOException ignored) {
        }
        for (IModFileInfo info : ModList.get()
                .getModFiles()) {
            Path file;
            try {
                file = info.getFile()
                        .findResource(path.split("/"));
            } catch (RuntimeException e) {
                continue;
            }
            if (file == null || !Files.isRegularFile(file))
                continue;
            try (InputStream in = Files.newInputStream(file)) {
                parse(in).ifPresent(found::add);
            } catch (IOException | RuntimeException e) {
                CreateRancher.LOGGER.debug("Couldn't read {} from {}", path, info.getFile()
                        .getFileName(), e);
            }
        }
        return found;
    }

    // The first copy of a file
    public static Optional<JsonObject> readFirst(String path) {
        for (JsonElement element : readAll(path))
            if (element.isJsonObject())
                return Optional.of(element.getAsJsonObject());
        return Optional.empty();
    }

    private static Optional<JsonElement> parse(InputStream in) {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return Optional.of(JsonParser.parseReader(reader));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    // The blocks in a block tag
    public static Set<ResourceLocation> blockTag(ResourceLocation tag) {
        Set<ResourceLocation> blocks = new LinkedHashSet<>();
        collect(tag, blocks, new LinkedHashSet<>());
        return blocks;
    }

    private static void collect(ResourceLocation tag, Set<ResourceLocation> blocks, Set<ResourceLocation> seen) {
        if (!seen.add(tag))
            return;
        for (JsonElement file : readAll("data/" + tag.getNamespace() + "/tags/block/" + tag.getPath() + ".json")) {
            if (!file.isJsonObject() || !file.getAsJsonObject()
                    .has("values"))
                continue;
            for (JsonElement value : file.getAsJsonObject()
                    .getAsJsonArray("values")) {
                String id = value.isJsonObject() ? value.getAsJsonObject()
                        .get("id")
                        .getAsString() : value.getAsString();
                if (id.startsWith("#")) {
                    ResourceLocation inner = ResourceLocation.tryParse(id.substring(1));
                    if (inner != null)
                        collect(inner, blocks, seen);
                } else {
                    ResourceLocation block = ResourceLocation.tryParse(id);
                    if (block != null)
                        blocks.add(block);
                }
            }
        }
    }
}
