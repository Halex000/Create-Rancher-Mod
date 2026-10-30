package com.halex.createrancher.content.depot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.depot.DepotRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

// Draws the filter slots and the items on top
public class SmartDepotRenderer extends SmartBlockEntityRenderer<SmartDepotBlockEntity> {

    public SmartDepotRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SmartDepotBlockEntity depot, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(depot, partialTicks, ms, buffer, light, overlay);
        DepotRenderer.renderItemsOf(depot, partialTicks, ms, buffer, light, overlay, depot.getDepot());
    }
}
