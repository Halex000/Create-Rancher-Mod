package com.halex.createrancher.content.net.client;

import com.halex.createrancher.CreateRancher;
import com.halex.createrancher.content.net.FishingNetEntity;
import com.halex.createrancher.content.render.OpaqueBatching;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// Draws a fishing net spread out on water or the ground
public class NetRendering {

    public static final ResourceLocation NET_TEXTURE = CreateRancher.asResource("textures/entity/fishing_net/net.png");
    public static final ResourceLocation FLOAT_TEXTURE =
            CreateRancher.asResource("textures/entity/fishing_net/float.png");

    private static final int CELLS = 6;
    private static final float TEXTURE_SPAN = FishingNetEntity.HALF_WIDTH * 2 + 1 / 16f;
    public static final float FLOAT_SIZE = .2f;
    public static final float GATHERED_SPREAD = .1f;
    public static final float GATHERED_FLOAT_SIZE = 2 / 16f;
    private static final float BITE_DIP = .22f;
    private static final float GROUND_LIFT = 1 / 64f;
    public static final int ROPE_COLOUR = 0xFF6B5035;

    // Draws the net at the water's surface
    public static void renderNet(PoseStack ms, MultiBufferSource buffer, int light, float time, boolean biting,
                                 float spread, long seed, float floatSize) {
        renderNet(ms, buffer, light, time, biting, spread, seed, floatSize, false);
    }

    // Draws the net lying still on the ground
    public static void renderNet(PoseStack ms, MultiBufferSource buffer, int light, float time, boolean biting,
                                 float spread, long seed, float floatSize, boolean onGround) {
        float half = FishingNetEntity.HALF_WIDTH * spread;
        float[] heights = new float[4];
        for (int corner = 0; corner < 4; corner++) {
            float phase = (float) ((seed * 31 + corner * 7919) % 628) / 100f;
            float speed = .11f + ((seed + corner * 13) & 7) * .006f;
            heights[corner] = onGround ? GROUND_LIFT
                    : .05f * Mth.sin(time * speed + phase) + .02f * Mth.sin(time * speed * 2.7f + phase * 3)
                    - (biting ? BITE_DIP : 0);
        }
        float sag = onGround ? 0 : spread;

        PoseStack.Pose pose = ms.last();
        VertexConsumer mesh = buffer.getBuffer(OpaqueBatching.hold(RenderType.entityCutoutNoCull(NET_TEXTURE)));
        for (int i = 0; i < CELLS; i++) {
            for (int j = 0; j < CELLS; j++) {
                float u0 = i / (float) CELLS;
                float u1 = (i + 1) / (float) CELLS;
                float v0 = j / (float) CELLS;
                float v1 = (j + 1) / (float) CELLS;
                meshVertex(mesh, pose, half, heights, sag, u0, v0, light);
                meshVertex(mesh, pose, half, heights, sag, u0, v1, light);
                meshVertex(mesh, pose, half, heights, sag, u1, v1, light);
                meshVertex(mesh, pose, half, heights, sag, u1, v0, light);
            }
        }

        VertexConsumer floats = buffer.getBuffer(OpaqueBatching.hold(RenderType.entityCutout(FLOAT_TEXTURE)));
        for (int corner = 0; corner < 4; corner++) {
            float x = corner == 1 || corner == 2 ? half : -half;
            float z = corner >= 2 ? half : -half;
            ms.pushPose();
            ms.translate(x, onGround ? floatSize / 2 : heights[corner] + .02f, z);
            renderFloat(floats, ms.last(), light, floatSize);
            ms.popPose();
        }
    }

    // How high the net is at a point
    private static float heightAt(float[] heights, float spread, float u, float v) {
        float edge = heights[0] * (1 - u) * (1 - v) + heights[1] * u * (1 - v) + heights[2] * u * v
                + heights[3] * (1 - u) * v;
        float sag = (1 - (2 * u - 1) * (2 * u - 1)) * (1 - (2 * v - 1) * (2 * v - 1));
        return edge - sag * .14f * spread;
    }

    private static void meshVertex(VertexConsumer consumer, PoseStack.Pose pose, float half, float[] heights,
                                   float spread, float u, float v, int light) {
        consumer.addVertex(pose, -half + u * 2 * half, heightAt(heights, spread, u, v), -half + v * 2 * half)
                .setColor(-1)
                .setUv(u * TEXTURE_SPAN, v * TEXTURE_SPAN)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }

    // A float
    private static void renderFloat(VertexConsumer consumer, PoseStack.Pose pose, int light, float size) {
        float s = size / 2;
        float[][] faces = {
                {-s, s, -s, -s, s, s, s, s, s, s, s, -s, 0, 1, 0},
                {-s, -s, s, -s, -s, -s, s, -s, -s, s, -s, s, 0, -1, 0},
                {-s, -s, -s, -s, s, -s, s, s, -s, s, -s, -s, 0, 0, -1},
                {s, -s, s, s, s, s, -s, s, s, -s, -s, s, 0, 0, 1},
                {-s, -s, s, -s, s, s, -s, s, -s, -s, -s, -s, -1, 0, 0},
                {s, -s, -s, s, s, -s, s, s, s, s, -s, s, 1, 0, 0}};
        float[][] uvs = {{0, 0}, {0, .5f}, {1, .5f}, {1, 0}};
        for (int f = 0; f < faces.length; f++) {
            float[] face = faces[f];
            boolean top = f == 0;
            boolean bottom = f == 1;
            for (int corner = 0; corner < 4; corner++) {
                float u = uvs[corner][0];
                float v = uvs[corner][1];
                if (top)
                    v *= .5f;
                else if (bottom)
                    v = .5f + v * .5f;
                else
                    v = face[corner * 3 + 1] > 0 ? .25f : .75f;
                consumer.addVertex(pose, face[corner * 3], face[corner * 3 + 1], face[corner * 3 + 2])
                        .setColor(-1)
                        .setUv(u, v)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(pose, face[12], face[13], face[14]);
            }
        }
    }

    // Where the rope ties onto the net
    public static Vec3 edgeTowards(Vec3 center, Vec3 toward, float spread) {
        double dx = toward.x - center.x;
        double dz = toward.z - center.z;
        double reach = Math.max(Math.abs(dx), Math.abs(dz));
        if (reach < 1e-3)
            return center;
        double scale = Math.min(1, FishingNetEntity.HALF_WIDTH * spread / reach);
        return center.add(dx * scale, 0, dz * scale);
    }

    // Draws a sagging rope between two points
    public static void renderRope(PoseStack ms, MultiBufferSource buffer, Vec3 from, Vec3 to) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        PoseStack.Pose pose = ms.last();
        float dx = (float) (to.x - from.x);
        float dy = (float) (to.y - from.y);
        float dz = (float) (to.z - from.z);
        int segments = 16;
        float x = (float) from.x;
        float y = (float) from.y;
        float z = (float) from.z;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            float nextX = (float) from.x + dx * t;
            float nextY = (float) from.y + dy * (t * t + t) * .5f;
            float nextZ = (float) from.z + dz * t;
            float nx = nextX - x;
            float ny = nextY - y;
            float nz = nextZ - z;
            float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (length < 1e-6f)
                length = 1;
            nx /= length;
            ny /= length;
            nz /= length;
            consumer.addVertex(pose, x, y, z)
                    .setColor(ROPE_COLOUR)
                    .setNormal(pose, nx, ny, nz);
            consumer.addVertex(pose, nextX, nextY, nextZ)
                    .setColor(ROPE_COLOUR)
                    .setNormal(pose, nx, ny, nz);
            x = nextX;
            y = nextY;
            z = nextZ;
        }
    }
}
