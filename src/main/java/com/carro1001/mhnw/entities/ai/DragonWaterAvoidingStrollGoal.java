package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class DragonWaterAvoidingStrollGoal extends WaterAvoidingRandomStrollGoal {


    private final DragonEntity dragonEntity;

    public DragonWaterAvoidingStrollGoal(DragonEntity dragonEntity, double pSpeedModifier) {
        super(dragonEntity, pSpeedModifier);
        this.dragonEntity = dragonEntity;
    }

    public DragonWaterAvoidingStrollGoal(DragonEntity dragonEntity, double pSpeedModifier, float pProbability) {
        super(dragonEntity, pSpeedModifier, pProbability);
        this.dragonEntity = dragonEntity;

    }

    @Override
    public boolean canUse() {
        return this.dragonEntity.getState() == DragonEntity.State.WALKING && this.dragonEntity.getAggressionState() == DragonEntity.AggressionState.PASSIVE && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.dragonEntity.getState() == DragonEntity.State.WALKING && this.dragonEntity.getAggressionState() == DragonEntity.AggressionState.PASSIVE && super.canContinueToUse();
    }
}
