package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.AptonothEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class AptonothPanicOrAttack extends MeleeAttackGoal {

    public static final int WATER_CHECK_DISTANCE_VERTICAL = 1;
    protected final AptonothEntity aptonoth;
    protected final double speedModifier;
    protected double posX;
    protected double posY;
    protected double posZ;
    protected boolean isRunning;
    protected boolean isAttacking = false;
    private int attackTicks;

    public AptonothPanicOrAttack(AptonothEntity pMob, double pSpeedModifier) {
        super(pMob, pSpeedModifier, true);
        this.aptonoth = pMob;
        this.speedModifier = pSpeedModifier;
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
     * method as well.
     */
    public boolean canUse() {
        if (!this.shouldPanic()) {
            return false;
        } else {
            if (this.mob.isOnFire()) {
                BlockPos blockpos = this.lookForWater(this.mob.level(), this.mob, 5);
                if (blockpos != null) {
                    this.posX = (double)blockpos.getX();
                    this.posY = (double)blockpos.getY();
                    this.posZ = (double)blockpos.getZ();
                    return true;
                }
            }

            return this.findRandomPosition();
        }
    }

    protected boolean shouldPanic() {
        return this.mob.getLastHurtByMob() != null || this.mob.isFreezing() || this.mob.isOnFire();
    }

    protected boolean findRandomPosition() {
        Vec3 vec3 = DefaultRandomPos.getPos(this.mob, 5, 4);
        isAttacking = mob.getRandom().nextFloat() > 0.75f && !aptonoth.isBaby();
        if(isAttacking){
            return true;
        }
        if (vec3 == null) {
            return false;
        } else {
            this.posX = vec3.x;
            this.posY = vec3.y;
            this.posZ = vec3.z;
            return true;
        }
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start() {
        aptonoth.setWalking(2);
        aptonoth.setAttacking(false);
        if (!isAttacking) {
            this.mob.getNavigation().moveTo(this.posX, this.posY, this.posZ, this.speedModifier);
            this.isRunning = true;
        }else{
            super.start();
        }
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void stop() {
        aptonoth.setWalking(0);
        if (isAttacking) {
            aptonoth.setAttacking(false);
            super.stop();
            this.aptonoth.setAggressive(false);

        }else{
            this.isRunning = false;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean canContinueToUse() {
        return isAttacking ? super.canContinueToUse(): !this.mob.getNavigation().isDone() && aptonoth.isPanic();
    }

    public void tick() {
        if(isAttacking){
            ++this.attackTicks;
            if((this.attackTicks >= 5 && this.getTicksUntilNextAttack() < this.getAttackInterval() / 2)){
                this.aptonoth.setAggressive(true);
                aptonoth.setAttacking(true);
            }else{
                this.aptonoth.setAggressive(false);
                aptonoth.setAttacking(false);
            }
            super.tick();

        }
    }

    @Nullable
    protected BlockPos lookForWater(BlockGetter pLevel, Entity pEntity, int pRange) {
        BlockPos blockpos = pEntity.blockPosition();
        return !pLevel.getBlockState(blockpos).getCollisionShape(pLevel, blockpos).isEmpty() ? null : BlockPos.findClosestMatch(pEntity.blockPosition(), pRange, 1, (p_196649_) -> {
            return pLevel.getFluidState(p_196649_).is(FluidTags.WATER);
        }).orElse((BlockPos)null);
    }
}