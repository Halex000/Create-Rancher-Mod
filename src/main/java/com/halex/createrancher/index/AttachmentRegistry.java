package com.halex.createrancher.index;

import com.halex.createrancher.CreateRancher;
import com.mojang.serialization.Codec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Data attached to mobs
public class AttachmentRegistry {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CreateRancher.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> DIGESTION = ATTACHMENTS
            .register("digestion", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .build());

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
