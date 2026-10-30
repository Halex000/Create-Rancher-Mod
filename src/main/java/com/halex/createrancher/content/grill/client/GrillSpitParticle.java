package com.halex.createrancher.content.grill.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpitParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

// A small splash over items being washed
public class GrillSpitParticle extends SpitParticle {

    protected GrillSpitParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed,
                                double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        quadSize *= .35f;
        xd *= .3;
        zd *= .3;
        lifetime = Math.max(4, lifetime / 2);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new GrillSpitParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
