package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class DragonMeleeAttackGoal extends MeleeAttackGoal {


    private final DragonEntity dragonEntity;

    public DragonMeleeAttackGoal(DragonEntity dragonEntity, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen) {
        super(dragonEntity, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
        this.dragonEntity = dragonEntity;
    }

    @Override
    public boolean canUse() {
        return this.dragonEntity.getAggressionState() == DragonEntity.AggressionState.AGGRESSIVE && this.dragonEntity.getFireballChargeState() != DragonEntity.FireballState.CHARGING &&  super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.dragonEntity.getAggressionState() == DragonEntity.AggressionState.AGGRESSIVE && this.dragonEntity.getFireballChargeState() != DragonEntity.FireballState.CHARGING && super.canContinueToUse();
    }
}