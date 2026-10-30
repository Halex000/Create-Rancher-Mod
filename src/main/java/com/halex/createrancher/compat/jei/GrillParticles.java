package com.halex.createrancher.compat.jei;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.halex.createrancher.content.grill.CauldronGrillBlock;
import com.halex.createrancher.content.grill.GrillProcess;
import com.mojang.blaze3d.systems.RenderSystem;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

// Fake particles for cauldron grills drawn in JEI
public class GrillParticles {

    private static final ResourceLocation[] BUBBLE_POP = sprites("bubble_pop_", 5);
    private static final ResourceLocation[] SPLASH = sprites("splash_", 4);
    private static final ResourceLocation LAVA = ResourceLocation.withDefaultNamespace("lava");

    private final GrillProcess process;
    private final double surface;
    private final double grateTop;
    private final List<Particle> particles = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private int lastTick = -1;

    public GrillParticles(GrillProcess process, BlockState grill) {
        this.process = process;
        surface = CauldronGrillBlock.getContentHeight(grill);
        grateTop = ((CauldronGrillBlock) grill.getBlock()).getGrateTop();
    }

    private static ResourceLocation[] sprites(String prefix, int count) {
        ResourceLocation[] names = new ResourceLocation[count];
        for (int i = 0; i < count; i++)
            names[i] = ResourceLocation.withDefaultNamespace(prefix + i);
        return names;
    }

    // A random spot over the grate
    private double inside() {
        return 3 / 16f + random.nextFloat() * 10 / 16f;
    }

    private void tick() {
        int now = AnimationTickHolder.getTicks(true);
        if (lastTick < 0 || now < lastTick)
            lastTick = now;
        int steps = Math.min(now - lastTick, 40);
        lastTick = now;
        for (int i = 0; i < steps; i++) {
            spawn();
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext())
                if (!it.next()
                        .tick())
                    it.remove();
        }
    }

    private void spawn() {
        if (process == GrillProcess.WASHING) {
            if (random.nextFloat() < .5f)
                particles.add(new BubblePop(inside(), surface, inside()));
            if (random.nextFloat() < .25f)
                for (int i = 0; i < 2; i++)
                    particles.add(new Splash(inside(), grateTop + .02, inside(), (random.nextFloat() - .5f) * .1f,
                            (random.nextFloat() - .5f) * .1f));
        }
        if (process == GrillProcess.BLASTING && random.nextInt(25) == 0)
            particles.add(new LavaPop(inside(), grateTop + .02, inside()));
    }

    // Draws the particles
    public void render(GuiGraphics graphics, Matrix4f toScreen, float scale) {
        tick();
        if (particles.isEmpty())
            return;
        float partial = AnimationTickHolder.getPartialTicksUI();
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_PARTICLES);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        graphics.pose()
                .pushPose();
        graphics.pose()
                .last()
                .pose()
                .identity();
        for (Particle p : particles) {
            Vector4f at = toScreen.transform(new Vector4f((float) Mth.lerp(partial, p.xo, p.x),
                    (float) Mth.lerp(partial, p.yo, p.y), (float) Mth.lerp(partial, p.zo, p.z), 1));
            int size = Math.max(1, Math.round(p.size(partial) * 2 * scale));
            TextureAtlasSprite sprite = atlas.getSprite(p.sprite());
            graphics.pose()
                    .pushPose();
            graphics.pose()
                    .translate(0, 0, at.z + 1);
            graphics.blit(Math.round(at.x - size / 2f), Math.round(at.y - size / 2f), 0, size, size, sprite);
            graphics.pose()
                    .popPose();
        }
        graphics.pose()
                .popPose();
    }

    // A basic particle
    private abstract class Particle {
        double x, y, z, xo, yo, zo, xd, yd, zd;
        int age, lifetime;
        float quadSize = .1f * (random.nextFloat() * .5f + .5f) * 2;

        Particle(double x, double y, double z) {
            this.x = xo = x;
            this.y = yo = y;
            this.z = zo = z;
        }

        // Gives a particle a random start
        void randomStart() {
            xd = (random.nextFloat() * 2 - 1) * .4;
            yd = (random.nextFloat() * 2 - 1) * .4;
            zd = (random.nextFloat() * 2 - 1) * .4;
            double speed = (random.nextFloat() + random.nextFloat() + 1) * .15;
            double length = Math.sqrt(xd * xd + yd * yd + zd * zd);
            xd = xd / length * speed * .4;
            yd = yd / length * speed * .4 + .1;
            zd = zd / length * speed * .4;
        }

        void move() {
            xo = x;
            yo = y;
            zo = z;
            x += xd;
            y += yd;
            z += zd;
        }

        // Returns false once the particle is gone
        abstract boolean tick();

        abstract ResourceLocation sprite();

        float size(float partial) {
            return quadSize;
        }
    }

    // A bubble pop
    private class BubblePop extends Particle {
        BubblePop(double x, double y, double z) {
            super(x, y, z);
            lifetime = 4;
            yd = .01;
        }

        @Override
        boolean tick() {
            yd -= .008;
            move();
            return age++ < lifetime;
        }

        @Override
        ResourceLocation sprite() {
            return BUBBLE_POP[Math.min(BUBBLE_POP.length - 1, age * BUBBLE_POP.length / (lifetime + 1))];
        }
    }

    // A water splash that hops up and lands back on the grate
    private class Splash extends Particle {
        final ResourceLocation sprite = SPLASH[random.nextInt(SPLASH.length)];

        Splash(double x, double y, double z, double xSpeed, double zSpeed) {
            super(x, y, z);
            lifetime = (int) (8 / (random.nextFloat() * .8 + .2));
            xd = xSpeed;
            yd = .1;
            zd = zSpeed;
        }

        @Override
        boolean tick() {
            if (lifetime-- <= 0)
                return false;
            yd -= .04;
            move();
            xd *= .98;
            yd *= .98;
            zd *= .98;
            return y > grateTop || yd > 0;
        }

        @Override
        ResourceLocation sprite() {
            return sprite;
        }
    }

    // A lava pop that hops on the grate
    private class LavaPop extends Particle {
        LavaPop(double x, double y, double z) {
            super(x, y, z);
            randomStart();
            xd *= .8 * .2;
            zd *= .8 * .2;
            yd = random.nextFloat() * .07 + .05;
            quadSize *= random.nextFloat() * 2 + .2f;
            lifetime = (int) (16 / (random.nextFloat() * .8 + .2));
        }

        @Override
        boolean tick() {
            if (age++ >= lifetime)
                return false;
            yd -= .04 * .75;
            move();
            if (y <= grateTop) {
                y = grateTop;
                yd = 0;
                xd *= .7;
                zd *= .7;
            }
            xd *= .999;
            yd *= .999;
            zd *= .999;
            return true;
        }

        @Override
        ResourceLocation sprite() {
            return LAVA;
        }

        @Override
        float size(float partial) {
            float done = (age + partial) / lifetime;
            return quadSize * (1 - done * done);
        }
    }
}
