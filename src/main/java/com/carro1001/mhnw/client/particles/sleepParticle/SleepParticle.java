package com.carro1001.mhnw.client.particles.sleepParticle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class SleepParticle extends TextureSheetParticle {
    public SleepParticle(ClientLevel worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double speedIn, int color) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, speedIn);

        if (color == -1){
            this.setColor(new Random().nextFloat(), new Random().nextFloat(), new Random().nextFloat());
        }else{
            var r = (color >> 16 & 255) / 255F * (1F - this.random.nextFloat() * 0.25F);
            var g = (color >> 8 & 255) / 255F * (1F - this.random.nextFloat() * 0.25F);
            var b = (color & 255) / 255F * (1F - this.random.nextFloat() * 0.25F);
            this.setColor(r, g, b);
        }
        this.setSize(0.1F, 0.1F);
        this.quadSize *= this.random.nextFloat() * 0.8F + 0.5F;
        this.xd *= (double) 0.02F;
        this.yd *= (double) 0.02F;
        this.zd *= (double) 0.02F;
        this.lifetime = (int)(30.0D / (Math.random() * 0.8D + 0.2D));
        this.alpha = 1f;
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public void move(double x, double y, double z) {
        this.setBoundingBox(this.getBoundingBox().move(x, y, z));
        this.setLocationFromBoundingbox();
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime-- <= 0) {
            this.remove();
        } else {
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.8D;
            this.yd *= 1.05D;
            this.zd *= 0.8D;
        }
    }

    @Override
    protected int getLightColor(float p_107249_) {
        return 15 << 20 | 15 << 4;
    }

}
