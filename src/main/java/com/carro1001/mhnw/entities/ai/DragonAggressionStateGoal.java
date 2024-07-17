package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class DragonAggressionStateGoal extends Goal {

    private final DragonEntity dragonEntity;

    int roarTime = -1;

    public DragonAggressionStateGoal(DragonEntity dragonEntity) {
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

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.dragonEntity.getTarget() != null) {
            DragonEntity.AggressionState aggressionState = this.dragonEntity.getAggressionState();
            if (aggressionState == DragonEntity.AggressionState.PASSIVE) {
                this.dragonEntity.setAggressionState(DragonEntity.AggressionState.ROAR);
                this.roarTime = 60;
            } else if (aggressionState == DragonEntity.AggressionState.ROAR) {
                this.dragonEntity.getLookControl().setLookAt(this.dragonEntity.getTarget());
                if (this.roarTime < 0) {
                    this.dragonEntity.setAggressionState(DragonEntity.AggressionState.AGGRESSIVE); // We're done roaring
                } else {
                    this.roarTime--;
                }
            }
        } else {
            this.dragonEntity.setAggressionState(DragonEntity.AggressionState.PASSIVE);
        }

        super.tick();
    }
}
