package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.DragonEntity;
import com.carro1001.mhnw.entities.SpitFireball;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
        dragonEntity.setAggressive(true);
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void stop() {
        this.chargeTime = -1;
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
            if(this.dragonEntity.hasLineOfSight(livingentity) || this.chargeTime > 0){
                Level level = this.dragonEntity.level;
                ++this.chargeTime;
                this.dragonEntity.lookAt(livingentity, 30.0F, 30.0F);
                if (this.chargeTime == 5 && !this.dragonEntity.isSilent()) {
                    level.levelEvent((Player)null, 1015, this.dragonEntity.blockPosition(), 0);
                }
                if (this.chargeTime == 10) {
                    dragonEntity.setStateDir(1);
                    Vec3 vec3 = this.dragonEntity.getViewVector(1.0F);
                    double d2 = livingentity.getX() - (this.dragonEntity.getX() + vec3.x * 12.0D);
                    double d3 = livingentity.getY(0.5D) - (0.5D + this.dragonEntity.getY(0.5D));
                    double d4 = livingentity.getZ() - (this.dragonEntity.getZ() + vec3.z * 12.0D);
                    if (!this.dragonEntity.isSilent()) {
                        level.levelEvent((Player)null, 1016, this.dragonEntity.blockPosition(), 0);
                    }

                    SpitFireball largefireball = new SpitFireball(level, this.dragonEntity, d2, d3, d4);
                    largefireball.setPos(this.dragonEntity.getX() + vec3.x * 4.0D, this.dragonEntity.getY(0.5D) + 0.5D, largefireball.getZ() + vec3.z * 4.0D);
                    level.addFreshEntity(largefireball);
                    this.chargeTime = -40;
                }
                this.dragonEntity.setCharging(this.chargeTime > 10);
            }

        }
    }

}
