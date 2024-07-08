package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.NewRathalosEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class NewRathalosFlyingWaterAvoidingStrollGoal extends WaterAvoidingRandomFlyingGoal {
    private final NewRathalosEntity rathalosEntity;

    public NewRathalosFlyingWaterAvoidingStrollGoal(NewRathalosEntity rathalosEntity, double speedModifier) {
        super(rathalosEntity, speedModifier);
        this.rathalosEntity = rathalosEntity;
        //this.probability = 0.05F;
    }

    @Override
    public boolean canUse() {
        return this.rathalosEntity.getState() == NewRathalosEntity.State.FLYING && this.rathalosEntity.getAggressionState() == NewRathalosEntity.AggressionState.PASSIVE && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.rathalosEntity.getState() == NewRathalosEntity.State.FLYING && this.rathalosEntity.getAggressionState() == NewRathalosEntity.AggressionState.PASSIVE && super.canContinueToUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() { // TODO: Something smarter
        int range = 32;
        return this.rathalosEntity.position().add(Mth.randomBetween(this.rathalosEntity.getRandom(), -range, range), Mth.randomBetween(this.rathalosEntity.getRandom(), -range, range), Mth.randomBetween(this.rathalosEntity.getRandom(), -range, range));
    }
}
