package com.halex.createrancher.content.net.client;

import com.halex.createrancher.content.net.FishingNetEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

// Draws a thrown fishing net
public class FishingNetRenderer extends EntityRenderer<FishingNetEntity> {

    private static final float OPEN_TICKS = 15;

    public FishingNetRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FishingNetEntity net, float entityYaw, float partialTick, PoseStack ms,
                       MultiBufferSource buffer, int light) {
        Player player = net.getPlayerOwner();
        if (player == null)
            return;
        float time = net.tickCount + partialTick;
        float open = Mth.clamp(time / OPEN_TICKS, 0, 1);
        float spread = Mth.lerp(open, NetRendering.GATHERED_SPREAD, 1);
        float floatSize = Mth.lerp(open, NetRendering.GATHERED_FLOAT_SIZE, NetRendering.FLOAT_SIZE);
        NetRendering.renderNet(ms, buffer, light, time, net.isBiting(), spread, net.getId(), floatSize,
                net.isOnLand());

        Vec3 center = net.getPosition(partialTick);
        Vec3 hand = handPosition(player, partialTick);
        Vec3 tie = NetRendering.edgeTowards(center, hand, spread);
        NetRendering.renderRope(ms, buffer, tie.subtract(center)
                .add(0, .04, 0), hand.subtract(center));
        super.render(net, entityYaw, partialTick, ms, buffer, light);
    }

    // Where the rope coil is held
    private Vec3 handPosition(Player player, float partialTick) {
        float swing = Mth.sin(Mth.sqrt(player.getAttackAnim(partialTick)) * Mth.PI);
        int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        ItemStack main = player.getMainHandItem();
        if (!main.canPerformAction(ItemAbilities.FISHING_ROD_CAST))
            side = -side;
        if (entityRenderDispatcher.options.getCameraType()
                .isFirstPerson() && player == Minecraft.getInstance().player) {
            double fovScale = 960.0 / entityRenderDispatcher.options.fov()
                    .get();
            Vec3 offset = entityRenderDispatcher.camera.getNearPlane()
                    .getPointOnPlane(side * .46f, -.2f)
                    .scale(fovScale)
                    .yRot(swing * .5f)
                    .xRot(-swing * .7f);
            return player.getEyePosition(partialTick)
                    .add(offset);
        }
        float bodyYaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(bodyYaw);
        double cos = Mth.cos(bodyYaw);
        float scale = player.getScale();
        double sideways = side * .35 * scale;
        double forward = .8 * scale;
        float crouch = player.isCrouching() ? -.1875f : 0;
        return player.getEyePosition(partialTick)
                .add(-cos * sideways - sin * forward, crouch - .45 * scale, -sin * sideways + cos * forward);
    }

    @Override
    public ResourceLocation getTextureLocation(FishingNetEntity net) {
        return NetRendering.NET_TEXTURE;
    }
}
