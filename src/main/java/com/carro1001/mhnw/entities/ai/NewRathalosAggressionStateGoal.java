package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.NewRathalosEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class NewRathalosAggressionStateGoal extends Goal {

    private final NewRathalosEntity rathalosEntity;

    int roarTime = -1;

    public NewRathalosAggressionStateGoal(NewRathalosEntity rathalosEntity) {
        this.rathalosEntity = rathalosEntity;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.rathalosEntity.getTarget() != null) {
            NewRathalosEntity.AggressionState aggressionState = this.rathalosEntity.getAggressionState();
            if (aggressionState == NewRathalosEntity.AggressionState.PASSIVE) {
                this.rathalosEntity.setAggressionState(NewRathalosEntity.AggressionState.ROAR);
                this.roarTime = 60;
            } else if (aggressionState == NewRathalosEntity.AggressionState.ROAR) {
                this.rathalosEntity.getLookControl().setLookAt(this.rathalosEntity.getTarget());
                if (this.roarTime < 0) {
                    this.rathalosEntity.setAggressionState(NewRathalosEntity.AggressionState.AGGRESSIVE); // We're done roaring
                } else {
                    this.roarTime--;
                }
            }
        } else {
            this.rathalosEntity.setAggressionState(NewRathalosEntity.AggressionState.PASSIVE);
        }

        super.tick();
    }
}
