package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.DragonEntity;
import com.carro1001.mhnw.entities.Monster;
import net.minecraft.world.entity.ai.goal.Goal;

public class MonsterAggressionStateGoal extends Goal {

    private final Monster dragonEntity;

    int roarTime = -1;

    public MonsterAggressionStateGoal(Monster dragonEntity) {
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
            Monster.AggressionState aggressionState = this.dragonEntity.getAggressionState();
            if (aggressionState == Monster.AggressionState.PASSIVE) {
                this.dragonEntity.setAggressionState(Monster.AggressionState.ROAR);
                this.roarTime = 60;
            } else if (aggressionState == Monster.AggressionState.ROAR) {
                this.dragonEntity.getLookControl().setLookAt(this.dragonEntity.getTarget());
                if (this.roarTime < 0) {
                    this.dragonEntity.setAggressionState(Monster.AggressionState.AGGRESSIVE); // We're done roaring
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
