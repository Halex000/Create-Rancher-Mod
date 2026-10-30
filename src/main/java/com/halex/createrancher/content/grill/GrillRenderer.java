package com.halex.createrancher.content.grill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// Draws the items on a grill
public class GrillRenderer extends SmartBlockEntityRenderer<GrillBlockEntity> {

    private static final float ITEM_SCALE = .3f;
    private static final float BLOCK_SCALE = .45f;

    private final ItemRenderer itemRenderer;

    public GrillRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
    }

    @Override
    protected void renderSafe(GrillBlockEntity grill, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(grill, partialTicks, ms, buffer, light, overlay);
        if (!(grill.getBlockState()
                .getBlock() instanceof GrillBlock block))
            return;
        Direction facing = block.getFacing(grill.getBlockState());
        BlockPos pos = grill.getBlockPos();
        NonNullList<ItemStack> items = grill.getItems();
        int seed = (int) pos.asLong();

        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty())
                continue;
            Vec3 at = grill.getRenderPosition(slot, partialTicks)
                    .subtract(Vec3.atLowerCornerOf(pos));
            BakedModel model = itemRenderer.getModel(stack, grill.getLevel(), null, seed + slot);
            ms.pushPose();
            ms.translate(at.x, at.y + 1 / 64f, at.z);
            ms.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            float scale = ITEM_SCALE;
            if (model.isGui3d()) {
                scale = BLOCK_SCALE;
                ms.translate(0, scale / 4, 0);
            } else
                ms.mulPose(Axis.XP.rotationDegrees(90));
            ms.scale(scale, scale, scale);
            itemRenderer.render(stack, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, model);
            ms.popPose();
        }
    }
}
