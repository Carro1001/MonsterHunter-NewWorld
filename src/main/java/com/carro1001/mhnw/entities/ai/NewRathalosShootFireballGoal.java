package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.NewRathalosEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;

public class NewRathalosShootFireballGoal extends Goal {

    private final NewRathalosEntity rathalosEntity;

    float fireballChargeTime = 0;

    public NewRathalosShootFireballGoal(NewRathalosEntity rathalosEntity) {
        this.rathalosEntity = rathalosEntity;
    }

    @Override
    public boolean canUse() {
        return check();
    }


    @Override
    public boolean canContinueToUse() {
        return check();
    }

    private boolean check() {
        return this.rathalosEntity.getTarget() != null && this.rathalosEntity.getAggressionState() == NewRathalosEntity.AggressionState.AGGRESSIVE && this.rathalosEntity.getFireballChargeState() != NewRathalosEntity.FireballState.COOL_DOWN && this.rathalosEntity.distanceToSqr(this.rathalosEntity.getTarget()) < this.rathalosEntity.getAttributeValue(Attributes.FOLLOW_RANGE) * this.rathalosEntity.getAttributeValue(Attributes.FOLLOW_RANGE) && this.rathalosEntity.distanceToSqr(this.rathalosEntity.getTarget()) > 15 * 15;
    }


    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        NewRathalosEntity.FireballState fireballChargeState = this.rathalosEntity.getFireballChargeState();

        if (this.rathalosEntity.getTarget() != null) {
            if (!this.rathalosEntity.hasLineOfSight(this.rathalosEntity.getTarget()) && fireballChargeState == NewRathalosEntity.FireballState.CHARGING) {
                this.fireballChargeTime = 0;
                this.rathalosEntity.setFireBallChargeState(NewRathalosEntity.FireballState.READY);
                return;
            }

            if (fireballChargeState == NewRathalosEntity.FireballState.READY) {
                this.rathalosEntity.setFireBallChargeState(NewRathalosEntity.FireballState.CHARGING);
                this.fireballChargeTime = 50;
            } else if (fireballChargeState == NewRathalosEntity.FireballState.CHARGING) {
                if (this.fireballChargeTime < 0) {
                    this.rathalosEntity.shootFireball();
                } else {
                    this.rathalosEntity.getLookControl().setLookAt(this.rathalosEntity.getTarget());
                    this.fireballChargeTime--;
                }
            }
        } else {
            this.rathalosEntity.setAggressionState(NewRathalosEntity.AggressionState.PASSIVE);
        }
    }
}
