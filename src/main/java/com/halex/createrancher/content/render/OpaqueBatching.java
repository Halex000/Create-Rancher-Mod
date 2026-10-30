package com.halex.createrancher.content.render;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.halex.createrancher.CreateRancher;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

// Draws the opaque layers of block entities once after they're all done
@EventBusSubscriber(modid = CreateRancher.MOD_ID, value = Dist.CLIENT)
public final class OpaqueBatching {

    private static final int START_SIZE = 1 << 16;
    private static final Map<RenderType, ByteBufferBuilder> MEMORY = new LinkedHashMap<>();
    private static final Map<RenderType, BufferBuilder> STARTED = new LinkedHashMap<>();
    private static Set<RenderType> layers;
    private static boolean open;
    private static boolean broken;

    private OpaqueBatching() {
    }

    // Buffers that hold back opaque layers while the world's block entities draw
    public static MultiBufferSource of(MultiBufferSource buffer) {
        if (!open || broken || buffer != Minecraft.getInstance()
                .renderBuffers()
                .bufferSource())
            return buffer;
        return type -> held(type) ? start(type) : buffer.getBuffer(type);
    }

    // Lets another opaque layer be held back
    public static RenderType hold(RenderType type) {
        layers();
        layers.add(type);
        return type;
    }

    private static boolean held(RenderType type) {
        return layers().contains(type);
    }

    private static Set<RenderType> layers() {
        if (layers == null)
            layers = new HashSet<>(Set.of(RenderType.solid(), RenderType.cutout(), RenderType.cutoutMipped(),
                    RenderType.lines()));
        return layers;
    }

    private static VertexConsumer start(RenderType type) {
        BufferBuilder builder = STARTED.get(type);
        if (builder == null) {
            builder = new BufferBuilder(MEMORY.computeIfAbsent(type, key -> new ByteBufferBuilder(START_SIZE)),
                    type.mode(), type.format());
            STARTED.put(type, builder);
        }
        return builder;
    }

    // Draws what's held back before the world's buffers draw something see-through over it
    public static void beforeSeeThrough(MultiBufferSource buffer) {
        if (open && buffer == Minecraft.getInstance()
                .renderBuffers()
                .bufferSource())
            draw();
    }

    // Draws what's held back once the world's block entities are done
    public static void endOfBlockEntities() {
        if (!open)
            return;
        open = false;
        draw();
    }

    private static void draw() {
        for (Map.Entry<RenderType, BufferBuilder> entry : STARTED.entrySet()) {
            MeshData mesh = entry.getValue()
                    .build();
            if (mesh != null)
                entry.getKey()
                        .draw(mesh);
        }
        STARTED.clear();
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            open = true;
        else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)
            endOfBlockEntities();
    }

    // Turns batching off for good if something skipped the stage that draws it
    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        open = false;
        if (STARTED.isEmpty())
            return;
        broken = true;
        for (BufferBuilder builder : STARTED.values()) {
            MeshData mesh = builder.build();
            if (mesh != null)
                mesh.close();
        }
        STARTED.clear();
        CreateRancher.LOGGER.warn("Block entity layers weren't drawn, so batching is off");
    }
}
