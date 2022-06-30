package com.carro1001.mhnw.entities.dragon;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public abstract class DragonEntity extends PathfinderMob implements IAnimatable, IAnimationTickable {
    private AnimationFactory factory = new AnimationFactory(this);

    public boolean roaring = false;

    public int counter = 0;
    public int StateCounter = 0;

    public String name;

    public DragonPart headPart;
    public DragonPart neckPart;
    public DragonPart rightWingUpperPart;
    public DragonPart leftWingUpperPart;
    public DragonPart tail1Part;
    public DragonPart tail2Part;
    public DragonPart tail3Part;
    public DragonPart body;
    public DragonPart[] dragonParts;
    public int posPointer = -1;
    public final double[][] positions = new double[64][3];
    public DragonEntity(EntityType<? extends PathfinderMob > p_27557_, Level p_27558_, String name) {
        super(p_27557_, p_27558_);
        this.name = name;
        this.noCulling = true;
        resetParts();
        this.setId(ENTITY_COUNTER.getAndAdd(this.dragonParts.length + 1) + 1);

    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    public void resetParts() {
        this.headPart = new DragonPart(this, 2.1F, 2.0F);
        this.neckPart = new DragonPart(this, 2.1F, 2.1F);
        this.body = new DragonPart(this, 4.0F, 2.5F);
        this.rightWingUpperPart = new DragonPart(this, 5.3F, 1.3F);
        this.leftWingUpperPart = new DragonPart(this, 5.3F, 0.7F);
        this.tail1Part = new DragonPart(this, 2.2F, 2.2F);
        this.tail2Part = new DragonPart(this, 2.2F, 2.2F);
        this.tail3Part = new DragonPart(this, 2.2F, 2.2F);

        headPart.copyPosition(this);
        neckPart.copyPosition(this);
        rightWingUpperPart.copyPosition(this);
        leftWingUpperPart.copyPosition(this);
        tail1Part.copyPosition(this);
        tail2Part.copyPosition(this);
        tail3Part.copyPosition(this);
         body.copyPosition(this);
        dragonParts = new DragonPart[]{headPart,neckPart,body,rightWingUpperPart,leftWingUpperPart,tail1Part,tail2Part};
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    public void setId(int pId) {
        super.setId(pId);
        for (int i = 0; i < this.dragonParts.length; i++) // Forge: Fix MC-158205: Set part ids to successors of parent mob id
            this.dragonParts[i].setId(pId + i + 1);
    }
    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        roaring = true;
        counter = 0;
        StateCounter = 0;
        return super.mobInteract(pPlayer, pHand);
    }

    public double[] getLatencyPos(int pBufferIndexOffset, float pPartialTicks) {
        if (this.isDeadOrDying()) {
            pPartialTicks = 0.0F;
        }

        pPartialTicks = 1.0F - pPartialTicks;
        int i = this.posPointer - pBufferIndexOffset & 63;
        int j = this.posPointer - pBufferIndexOffset - 1 & 63;
        double[] adouble = new double[3];
        double d0 = this.positions[i][0];
        double d1 = Mth.wrapDegrees(this.positions[j][0] - d0);
        adouble[0] = d0 + d1 * (double)pPartialTicks;
        d0 = this.positions[i][1];
        d1 = this.positions[j][1] - d0;
        adouble[1] = d0 + d1 * (double)pPartialTicks;
        adouble[2] = Mth.lerp((double)pPartialTicks, this.positions[i][2], this.positions[j][2]);
        return adouble;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.isNoAi()) {

            if (this.posPointer < 0) {
                for(int i = 0; i < this.positions.length; ++i) {
                    this.positions[i][0] = (double)this.getYRot();
                    this.positions[i][1] = this.getY();
                }
            }

            if (++this.posPointer == this.positions.length) {
                this.posPointer = 0;
            }

            this.positions[this.posPointer][0] = (double)this.getYRot();
            this.positions[this.posPointer][1] = this.getY();
            if (this.level.isClientSide) {
                if (this.lerpSteps > 0) {
                    double d6 = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
                    double d0 = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
                    double d1 = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
                    double d2 = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
                    this.setYRot(this.getYRot() + (float)d2 / (float)this.lerpSteps);
                    this.setXRot(this.getXRot() + (float)(this.lerpXRot - (double)this.getXRot()) / (float)this.lerpSteps);
                    --this.lerpSteps;
                    this.setPos(d6, d0, d1);
                    this.setRot(this.getYRot(), this.getXRot());
                }
            }
            this.yBodyRot = this.getYRot();
            Vec3[] avector3d = new Vec3[this.dragonParts.length];

            for (int j = 0; j < this.dragonParts.length; ++j) {
                this.dragonParts[j].collideWithNearbyEntities();
                avector3d[j] = new Vec3(this.dragonParts[j].getX(), this.dragonParts[j].getY(), this.dragonParts[j].getZ());
            }

            float f12 = (float)(this.getLatencyPos(5, 1.0F)[1] - this.getLatencyPos(10, 1.0F)[1]) * 10.0F * ((float)Math.PI / 180F);
            float f13 = Mth.cos(f12);
            float f1 = Mth.sin(f12);
            float f14 = this.getYRot() * ((float)Math.PI / 180F);
            float f2 = Mth.sin(f14);
            float f15 = Mth.cos(f14);
            this.setPartPosition(this.body, (double)(f2 * 0.5F), 0.0D, (double)(-f15 * 0.5F));
            this.setPartPosition(this.leftWingUpperPart, (double)(f15 * 4.5F), 2.0D, (double)(f2 * 4.5F));
            this.setPartPosition(this.rightWingUpperPart, (double)(f15 * -4.5F), 2.0D, (double)(f2 * -4.5F));

            float f3 = Mth.sin(this.getYRot() * ((float)Math.PI / 180F) - 0.01F);
            float f16 = Mth.cos(this.getYRot() * ((float)Math.PI / 180F) - 0.01F);
            double[] adouble = this.getLatencyPos(5, 1.0F);
            double[] adouble1 = this.getLatencyPos(0, 1.0F);
            float f4 = (float)(adouble[1] - adouble1[1]);
            this.setPartPosition(this.headPart, (double)(f3 * 6.5F * f13), (double)(f4 + f1 * 6.5F), (double)(-f16 * 6.5F * f13));
            this.setPartPosition(this.neckPart, (double)(f3 * 5.5F * f13), (double)(f4 + f1 * 5.5F), (double)(-f16 * 5.5F * f13));
            adouble = this.getLatencyPos(5, 1.0F);
            for(int k = 0; k < 3; ++k) {
                DragonPart enderdragonpart = null;
                if (k == 0) {
                    enderdragonpart = this.tail1Part;
                }

                if (k == 1) {
                    enderdragonpart = this.tail2Part;
                }

                if (k == 2) {
                    enderdragonpart = this.tail3Part;
                }

                adouble1 = this.getLatencyPos(12 + k * 2, 1.0F);
                float f17 = this.getYRot() * ((float)Math.PI / 180F) + this.rotWrap(adouble1[0] - adouble[0]) * ((float)Math.PI / 180F);
                float f18 = Mth.sin(f17);
                float f20 = Mth.cos(f17);
                float f21 = 1.5F;
                float f22 = (float)(k + 1) * 2.0F;
                this.tickPart(enderdragonpart, (double)(-(f2 * 1.5F + f18 * f22) * f13), adouble1[1] - adouble[1] - (double)((f22 + 1.5F) * f1) + 1.5D, (double)((f15 * 1.5F + f20 * f22) * f13));
            }
            for (int l = 0; l < this.dragonParts.length; ++l) {
                this.dragonParts[l].xo = avector3d[l].x;
                this.dragonParts[l].yo = avector3d[l].y;
                this.dragonParts[l].zo = avector3d[l].z;
                this.dragonParts[l].xOld = avector3d[l].x;
                this.dragonParts[l].yOld = avector3d[l].y;
                this.dragonParts[l].zOld = avector3d[l].z;
            }
        }
    }
    private float rotWrap(double pAngle) {
        return (float)Mth.wrapDegrees(pAngle);
    }
    private void tickPart(DragonPart pPart, double pOffsetX, double pOffsetY, double pOffsetZ) {
        pPart.setPos(this.getX() + pOffsetX, this.getY() + pOffsetY, this.getZ() + pOffsetZ);
    }
    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.minecraftforge.entity.PartEntity<?>[] getParts() {
        return this.dragonParts;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource instanceof EntityDamageSource && ((EntityDamageSource)pSource).isThorns() && !this.level.isClientSide) {
            this.hurt(this.body, pSource, pAmount);
        }

        return false;
    }
    public boolean hurt(DragonPart pPart, DamageSource pSource, float pDamage) {

        if (pPart != this.headPart) {
            pDamage = pDamage / 2.0F;
        }

        if (pDamage < 0.01F) {
            return false;
        } else {
            if (pSource.getEntity() instanceof Player || pSource.isExplosion()) {
                float f = this.getHealth();
                this.reallyHurt(pSource, pDamage);
            }
            return true;
        }

    }
    protected boolean reallyHurt(DamageSource pDamageSource, float pAmount) {
        return super.hurt(pDamageSource, pAmount);
    }
    public boolean attackEntityPartFrom(DragonPart entityPart, DamageSource source, float amount) {
        return this.hurt(entityPart, source, amount);
    }
    private void setPartPosition(DragonPart part, double offsetX, double offsetY, double offsetZ) {
        part.setPos(this.getX() + offsetX * part.scale, this.getY() + offsetY * part.scale, this.getZ() + offsetZ * part.scale);
    }
    @Override
    public int tickTimer() {
        return tickCount;
    }

}
