package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.DragonEntity;
import com.carro1001.mhnw.entities.SpitFireball;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DragonFireball extends Goal{
    private final DragonEntity dragonEntity;
    public int chargeTime;

    public DragonFireball(DragonEntity dragonEntity) {
        this.dragonEntity = dragonEntity;
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
     * method as well.
     */
    public boolean canUse() {
        return this.dragonEntity.getTarget() != null;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start() {
        this.chargeTime = 0;
        dragonEntity.setStateDir(1);
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void stop() {
        dragonEntity.setStateDir(0);
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

            double d0 = 64.0D;
            if (livingentity.distanceToSqr(this.dragonEntity) < 4096.0D && this.dragonEntity.hasLineOfSight(livingentity)) {
                Level level = this.dragonEntity.level;
                ++this.chargeTime;
                if (this.chargeTime == 10 && !this.dragonEntity.isSilent()) {
                    level.levelEvent((Player)null, 1015, this.dragonEntity.blockPosition(), 0);
                }

                if (this.chargeTime == 20) {
                    double d1 = 4.0D;
                    Vec3 vec3 = this.dragonEntity.getViewVector(1.0F);
                    double d2 = livingentity.getX() - (this.dragonEntity.getX() + vec3.x * 12.0D);
                    double d3 = livingentity.getY(0.5D) - (0.5D + this.dragonEntity.getY(0.5D));
                    double d4 = livingentity.getZ() - (this.dragonEntity.getZ() + vec3.z * 12.0D);
                    if (!this.dragonEntity.isSilent()) {
                        level.levelEvent((Player)null, 1016, this.dragonEntity.blockPosition(), 0);
                    }

                    SpitFireball largefireball = new SpitFireball(level, this.dragonEntity, d2, d3, d4);
                    largefireball.setPos(this.dragonEntity.getX() + vec3.x * 8.0D, this.dragonEntity.getY(0.5D) + 0.5D, largefireball.getZ() + vec3.z * 8.0D);
                    level.addFreshEntity(largefireball);
                    this.chargeTime = -40;
                }
            } else if (this.chargeTime > 0) {
                --this.chargeTime;
            }

            this.dragonEntity.setCharging(this.chargeTime > 10);
        }
    }

}
