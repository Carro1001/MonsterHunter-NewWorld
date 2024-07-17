package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;

public class DragonShootFireballGoal extends Goal {

    private final DragonEntity dragonEntity;

    float fireballChargeTime = 0;

    public DragonShootFireballGoal(DragonEntity dragonEntity) {
        this.dragonEntity = dragonEntity;
    }

    @Override
    public boolean canUse() {
        return true;
    }


    @Override
    public boolean canContinueToUse() {
        return true;
    }

    private boolean check() {
        return this.dragonEntity.getTarget() != null && this.dragonEntity.getAggressionState() == DragonEntity.AggressionState.AGGRESSIVE && this.dragonEntity.getFireballChargeState() != DragonEntity.FireballState.COOL_DOWN && this.dragonEntity.distanceToSqr(this.dragonEntity.getTarget()) < this.dragonEntity.getAttributeValue(Attributes.FOLLOW_RANGE) * this.dragonEntity.getAttributeValue(Attributes.FOLLOW_RANGE) && this.dragonEntity.distanceToSqr(this.dragonEntity.getTarget()) > 15 * 15;
    }


    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        DragonEntity.FireballState fireballChargeState = this.dragonEntity.getFireballChargeState();

        if (this.dragonEntity.getTarget() != null && this.dragonEntity.getAggressionState() == DragonEntity.AggressionState.AGGRESSIVE) {
            if (this.dragonEntity.getFireballCooldownTime() > 0) {
                this.dragonEntity.setFireBallChargeState(DragonEntity.FireballState.COOL_DOWN);
                return;
            }

            if (!this.dragonEntity.hasLineOfSight(this.dragonEntity.getTarget()) && fireballChargeState == DragonEntity.FireballState.CHARGING) {
                this.fireballChargeTime = 0;
                this.dragonEntity.setFireBallChargeState(DragonEntity.FireballState.READY);
                return;
            }

            if (fireballChargeState == DragonEntity.FireballState.READY) {
                this.dragonEntity.setFireBallChargeState(DragonEntity.FireballState.CHARGING);
                this.fireballChargeTime = 50;
            } else if (fireballChargeState == DragonEntity.FireballState.CHARGING) {
                if (this.fireballChargeTime < 0) {
                    this.dragonEntity.shootFireball();
                } else {
                    this.dragonEntity.getLookControl().setLookAt(this.dragonEntity.getTarget());
                    this.fireballChargeTime--;
                }
            }
        } else {
            if (fireballChargeState == DragonEntity.FireballState.CHARGING) {
                this.dragonEntity.setFireBallChargeState(DragonEntity.FireballState.READY);
            }
        }
    }
}
