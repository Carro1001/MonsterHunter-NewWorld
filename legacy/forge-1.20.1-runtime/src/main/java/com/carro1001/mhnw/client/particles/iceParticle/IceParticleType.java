package com.carro1001.mhnw.client.particles.iceParticle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class IceParticleType extends SimpleParticleType {
    public IceParticleType() {
        super(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static class IceParticleFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public IceParticleFactory(SpriteSet p_i50522_1_) {
            this.spriteSet = p_i50522_1_;
        }


        public Particle createParticle(@NotNull SimpleParticleType typeIn,@NotNull ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            IceParticle particle = new IceParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}