package com.carro1001.mhnw.entities.rathian;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.Random;

public class
RathianEntity extends PathfinderMob implements IAnimatable, IAnimationTickable {
    private AnimationFactory factory = new AnimationFactory(this);
    public boolean hover = false;
    public boolean roaring = false;
    public boolean agro = false;
    public boolean backflip = false;
    public boolean tailswipe = false;

    public final RathianPart neck1;
    public final RathianPart chest;
    public final RathianPart tail1;
    public final RathianPart rightArm1;
    public final RathianPart leftArm1;
    public final RathianPart hips;
    public final RathianPart[] rathianParts;
    public int counter = 0;
    public int posPointer = -1;
    public final double[][] positions = new double[64][3];

    public RathianEntity(EntityType<? extends PathfinderMob > p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.noCulling = true;
        this.neck1 = new RathianPart(this, 3F, 2.1F);
        this.chest = new RathianPart(this, 5.0F, 2.5F);
        this.tail1 = new RathianPart(this, 5.1F, 2.2F);
        this.rightArm1 = new RathianPart(this, 11.3F, 1.3F);
        this.leftArm1 = new RathianPart(this, 11.3F, 1.3F);
        this.hips = new RathianPart(this, 2.5F, 2.5F);
        this.rathianParts = new RathianPart[]{this.neck1, this.chest, this.tail1, this.rightArm1, this.leftArm1, this.hips};
        this.setId(ENTITY_COUNTER.getAndAdd(this.rathianParts.length + 1) + 1);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }


    @Override
    protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        return super.mobInteract(pPlayer, pHand);
    }

    @Override
    public void turn(double pYRot, double pXRot) {
        super.turn(pYRot, pXRot);
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {

        if (!event.isMoving() && this.getSpeed() <= 0) {
            if (hover) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathian.hover", true));
                return PlayState.CONTINUE;
            }
            if (roaring) {
                roaring = false;
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathian.roar_normal", false));
                return PlayState.CONTINUE;
            }
            if (agro) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathian.idle_aggro", true));
                return PlayState.CONTINUE;
            }
            if (backflip) {
                backflip = false;
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathian.backflip", false));
                return PlayState.CONTINUE;
            }
            if (tailswipe) {
                tailswipe = false;
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathian.tailswipe", false));
                return PlayState.CONTINUE;
            }
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathian.idle_normal", true));
            return PlayState.CONTINUE;
        }
        if (event.isMoving()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathian.walk", true));
            return PlayState.CONTINUE;
        }

        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.rathian.idle_normal", true));
        return PlayState.CONTINUE;
    }
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 6, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
    }
    @Override
    public void setId(int pId) {
        super.setId(pId);
        for (int i = 0; i < this.rathianParts.length; i++) // Forge: Fix MC-158205: Set part ids to successors of parent mob id
            this.rathianParts[i].setId(pId + i + 1);
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
    private void setPartPosition(RathianPart part, double offsetX, double offsetY, double offsetZ) {
        part.setPos(this.getX() + offsetX * part.scale, this.getY() + offsetY * part.scale, this.getZ() + offsetZ * part.scale);
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
            Vec3[] avector3d = new Vec3[this.rathianParts.length];

            for (int j = 0; j < this.rathianParts.length; ++j) {
                this.rathianParts[j].collideWithNearbyEntities();
                avector3d[j] = new Vec3(this.rathianParts[j].getX(), this.rathianParts[j].getY(), this.rathianParts[j].getZ());
            }

            float f12 = (float)(this.getLatencyPos(5, 1.0F)[1] - this.getLatencyPos(10, 1.0F)[1]) * 10.0F * ((float)Math.PI / 180F);
            float f13 = Mth.cos(f12);
            float f1 = Mth.sin(f12);
            float f14 = this.getYRot() * ((float)Math.PI / 180F);
            float pitch = this.getXRot() * ((float) Math.PI / 180F);
            float f2 = Mth.sin(f14);
            float f15 = Mth.cos(f14);
            float f3 = Mth.sin(f14) * (1 - Math.abs(this.getXRot() / 90F));
            float f18 = Mth.cos(f14) * (1 - Math.abs(this.getXRot() / 90F));
            this.setPartPosition(this.chest, (double)(f2 * 0.5F), 0.0D, (double)(-f15 * 0.5F));
            this.setPartPosition(this.leftArm1, (double)(f15 * 4.5F), 2.0D, (double)(f2 * 4.5F));
            this.setPartPosition(this.rightArm1, (double)(f15 * -4.5F), 2.0D, (double)(f2 * -4.5F));
            float f16 = Mth.cos(f12);
            this.setPartPosition(this.neck1, f3 * -7F, -pitch * 5F, -f18 * -7F);
            double[] adouble = this.getLatencyPos(5, 1.0F);



            double[] adouble1 = this.getLatencyPos(15 + 3 * 5, 1.0F);
            float f7 = this.getYRot() * ((float) Math.PI / 180F) + (float) Mth.wrapDegrees(adouble1[0] - adouble[0]) * ((float) Math.PI / 180F);
            float f20 = Mth.sin(f7) * (1 - Math.abs(this.getXRot() / 90F));
            float f21 = Mth.cos(f7) * (1 - Math.abs(this.getXRot() / 90F));
            float f22 = -3.6F;
            float f23 = (float) (3) * f22 - 2F;
            this.setPartPosition(tail1, -(f3 * 0.5F + f20 * f23) * f16, pitch * 1.5F * (4), (f18 * 0.5F + f21 * f23) * f16);

            for (int l = 0; l < this.rathianParts.length; ++l) {
                this.rathianParts[l].xo = avector3d[l].x;
                this.rathianParts[l].yo = avector3d[l].y;
                this.rathianParts[l].zo = avector3d[l].z;
                this.rathianParts[l].xOld = avector3d[l].x;
                this.rathianParts[l].yOld = avector3d[l].y;
                this.rathianParts[l].zOld = avector3d[l].z;
            }
        }
    }

    @Override
    public void tick() {
        counter++;
        if(counter >= 20*20){
            counter = 0;
            hover = false;
            roaring = false;
            agro = false;
            backflip = false;
            tailswipe = false;
            switch(new Random().nextInt(8)){
                case 0:
                    hover = true;
                    break;
                case 1:
                    roaring = true;
                    break;
                case 2:
                    agro = true;
                    break;
                case 3:
                    backflip = true;
                    break;
                case 4:
                    tailswipe = true;
                    break;
            }
        }
        super.tick();
    }

    @Override
    public int tickTimer() {
        return tickCount;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.minecraftforge.entity.PartEntity<?>[] getParts() {
        return this.rathianParts;
    }
    public boolean attackEntityPartFrom(RathianPart entityCachalotPart, DamageSource source, float amount) {
        return this.hurt(source, amount);
    }
}
