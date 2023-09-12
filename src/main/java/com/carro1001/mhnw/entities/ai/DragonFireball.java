package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.DragonEntity;
import com.carro1001.mhnw.entities.SpitFireball;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class DragonFireball extends Goal{
    private final DragonEntity dragonEntity;
    public int chargeTime;

    public DragonFireball(DragonEntity dragonEntity) {
        this.dragonEntity = dragonEntity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
     * method as well.
     */
    public boolean canUse() {
        return this.dragonEntity.getTarget() != null && dragonEntity.hasRoared() ;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start() {
        super.start();
        this.chargeTime = 0;
        System.out.println("fireball start");

        dragonEntity.setAggressive(true);
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void stop() {
        this.chargeTime = -1;

        dragonEntity.setAggressive(false);
        dragonEntity.setStateDir(0);
        System.out.println("fireball done");

    }


    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void tick() {
        LivingEntity livingentity = this.dragonEntity.getTarget();
        if (livingentity != null) {
            if(this.dragonEntity.hasLineOfSight(livingentity)){
                Level level = this.dragonEntity.level();
                ++this.chargeTime;
                this.dragonEntity.lookAt(livingentity, 30.0F, 30.0F);
                if (this.chargeTime == 10) {
                    dragonEntity.setStateDir(1);
                    double x = this.dragonEntity.position().x + this.dragonEntity.getRandom().nextGaussian() * 5.0;
                    double y = this.dragonEntity.position().y + this.dragonEntity.getRandom().nextGaussian() * 5.0;
                    double z = this.dragonEntity.position().z + this.dragonEntity.getRandom().nextGaussian() * 5.0;
                    if (!this.dragonEntity.isSilent()) {
                        level.levelEvent(null, 1016, this.dragonEntity.blockPosition(), 0);
                    }

                    SpitFireball largefireball = new SpitFireball(level, this.dragonEntity, x, y, z);
                    largefireball.setPos(this.dragonEntity.getX(), this.dragonEntity.getY() + this.dragonEntity.getEyeHeight(), largefireball.getZ());
                    largefireball.setDeltaMovement(this.dragonEntity.getLookAngle().x * 2.0, this.dragonEntity.getLookAngle().y * 2.0, this.dragonEntity.getLookAngle().z * 2.0);
                    level.addFreshEntity(largefireball);
                    this.chargeTime = -40;
                    this.stop();
                }
            }

        }
    }

}
