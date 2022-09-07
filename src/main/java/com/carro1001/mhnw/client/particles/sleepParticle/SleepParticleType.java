package com.carro1001.mhnw.client.particles.sleepParticle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class SleepParticleType extends SimpleParticleType {
    public SleepParticleType() {
        super(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static class SleepParticleFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public SleepParticleFactory(SpriteSet p_i50522_1_) {
            this.spriteSet = p_i50522_1_;
        }

        public Particle createParticle(@NotNull SimpleParticleType typeIn,@NotNull ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            SleepParticle particle = new SleepParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}