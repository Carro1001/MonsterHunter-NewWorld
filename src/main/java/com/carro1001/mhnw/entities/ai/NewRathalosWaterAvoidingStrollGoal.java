package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.NewRathalosEntity;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class NewRathalosWaterAvoidingStrollGoal extends WaterAvoidingRandomStrollGoal {


    private final NewRathalosEntity rathalosEntity;

    public NewRathalosWaterAvoidingStrollGoal(NewRathalosEntity rathalosEntity, double pSpeedModifier) {
        super(rathalosEntity, pSpeedModifier);
        this.rathalosEntity = rathalosEntity;
    }

    public NewRathalosWaterAvoidingStrollGoal(NewRathalosEntity rathalosEntity, double pSpeedModifier, float pProbability) {
        super(rathalosEntity, pSpeedModifier, pProbability);
        this.rathalosEntity = rathalosEntity;

    }

    @Override
    public boolean canUse() {
        return this.rathalosEntity.getState() == NewRathalosEntity.State.WALKING && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.rathalosEntity.getState() == NewRathalosEntity.State.WALKING && super.canContinueToUse();
    }
}
