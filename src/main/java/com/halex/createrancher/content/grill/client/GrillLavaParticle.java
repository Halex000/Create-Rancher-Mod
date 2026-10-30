package com.halex.createrancher.content.grill.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.LavaParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

// A small lava pop that hops on the grill
public class GrillLavaParticle extends LavaParticle {

    protected GrillLavaParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        xd *= .2;
        zd *= .2;
        yd = random.nextFloat() * .07 + .05;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            GrillLavaParticle particle = new GrillLavaParticle(level, x, y, z);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
