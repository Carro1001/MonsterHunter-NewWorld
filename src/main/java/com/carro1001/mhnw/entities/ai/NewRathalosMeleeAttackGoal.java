package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.NewRathalosEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class NewRathalosMeleeAttackGoal extends MeleeAttackGoal {


    private final NewRathalosEntity rathalosEntity;

    public NewRathalosMeleeAttackGoal(NewRathalosEntity rathalosEntity, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen) {
        super(rathalosEntity, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
        this.rathalosEntity = rathalosEntity;
    }

    @Override
    public boolean canUse() {
        return this.rathalosEntity.getAggressionState() == NewRathalosEntity.AggressionState.AGGRESSIVE && this.rathalosEntity.getFireballChargeState() != NewRathalosEntity.FireballState.CHARGING &&  super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.rathalosEntity.getAggressionState() == NewRathalosEntity.AggressionState.AGGRESSIVE && this.rathalosEntity.getFireballChargeState() != NewRathalosEntity.FireballState.CHARGING && super.canContinueToUse();
    }
}
