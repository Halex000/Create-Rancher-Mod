package com.halex.createrancher.content.joist;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.resources.IoSupplier;

// A resource pack made in code for the joists
public class GeneratedPack implements PackResources {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .create();

    private final PackLocationInfo location;
    private final PackType type;
    private final Supplier<Map<String, JsonElement>> contents;
    private Map<String, byte[]> files;

    private GeneratedPack(PackLocationInfo location, PackType type, Supplier<Map<String, JsonElement>> contents) {
        this.location = location;
        this.type = type;
        this.contents = contents;
    }

    // The pack
    @Nullable
    public static Pack create(String id, String title, PackType type, Supplier<Map<String, JsonElement>> contents) {
        PackLocationInfo location = new PackLocationInfo(id, Component.literal(title), PackSource.BUILT_IN,
                Optional.empty());
        Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo info) {
                return new GeneratedPack(info, type, contents);
            }

            @Override
            public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return new GeneratedPack(info, type, contents);
            }
        };
        return Pack.readMetaAndCreate(location, resources, type, new PackSelectionConfig(true, Pack.Position.BOTTOM,
                false));
    }

    private Map<String, byte[]> files() {
        if (files == null) {
            files = new TreeMap<>();
            contents.get()
                    .forEach((path, json) -> files.put(path, GSON.toJson(json)
                            .getBytes(StandardCharsets.UTF_8)));
        }
        return files;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        if (packType != type)
            return null;
        byte[] file = files().get(packType.getDirectory() + "/" + location.getNamespace() + "/" + location.getPath());
        return file == null ? null : () -> new ByteArrayInputStream(file);
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput output) {
        if (packType != type)
            return;
        String root = packType.getDirectory() + "/" + namespace + "/";
        String prefix = root + path + "/";
        files().forEach((file, bytes) -> {
            if (file.startsWith(prefix))
                output.accept(ResourceLocation.fromNamespaceAndPath(namespace, file.substring(root.length())),
                        () -> new ByteArrayInputStream(bytes));
        });
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        Set<String> namespaces = new TreeSet<>();
        if (packType != type)
            return namespaces;
        String root = packType.getDirectory() + "/";
        for (String file : files().keySet())
            if (file.startsWith(root))
                namespaces.add(file.substring(root.length(), file.indexOf('/', root.length())));
        return namespaces;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        if (deserializer != PackMetadataSection.TYPE)
            return null;
        return (T) new PackMetadataSection(location.title(), SharedConstants.getCurrentVersion()
                .getPackVersion(type));
    }

    @Override
    public PackLocationInfo location() {
        return location;
    }

    @Override
    public void close() {
    }
}
