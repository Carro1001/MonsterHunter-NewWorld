package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.DragonEntity;
import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class MonsterAggressionStateGoal extends Goal {

    private final NewWorldMonsterEntity dragonEntity;

    int roarTime = -1;

    public MonsterAggressionStateGoal(NewWorldMonsterEntity dragonEntity) {
        this.dragonEntity = dragonEntity;
    }

    @Override
    public boolean canUse() {
        return dragonEntity.getAggressionState() == NewWorldMonsterEntity.AggressionState.PASSIVE;
    }

    @Override
    public boolean canContinueToUse() {
        MHNW.debugLog("MonsterAggressionStateGoal: do the roar");
        return dragonEntity.getAggressionState() == NewWorldMonsterEntity.AggressionState.PASSIVE || dragonEntity.getAggressionState() == NewWorldMonsterEntity.AggressionState.ROAR;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.dragonEntity.getTarget() != null) {
            NewWorldMonsterEntity.AggressionState aggressionState = this.dragonEntity.getAggressionState();
            if (aggressionState == NewWorldMonsterEntity.AggressionState.PASSIVE) {
                this.dragonEntity.triggerAnim("main_controller","roar");
                this.dragonEntity.setAggressionState(NewWorldMonsterEntity.AggressionState.ROAR);
                this.roarTime = 60;
            } else if (aggressionState == NewWorldMonsterEntity.AggressionState.ROAR) {
                this.dragonEntity.getLookControl().setLookAt(this.dragonEntity.getTarget());
                if (this.roarTime < 0) {
                    this.dragonEntity.setAggressionState(NewWorldMonsterEntity.AggressionState.AGGRESSIVE); // We're done roaring
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
