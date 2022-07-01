package com.carro1001.mhnw.client.particles.poisonParticle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class PoisonParticleType extends SimpleParticleType {
    public PoisonParticleType() {
        super(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static class PoisonParticleFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public PoisonParticleFactory(SpriteSet p_i50522_1_) {
            this.spriteSet = p_i50522_1_;
        }


        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            PoisonParticle particle = new PoisonParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}