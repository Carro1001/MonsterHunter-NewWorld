package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.DragonEntity;
import com.carro1001.mhnw.entities.LargeMonster;
import net.minecraft.world.entity.ai.goal.Goal;

public class MonsterAggressionStateGoal extends Goal {

    private final LargeMonster dragonEntity;

    int roarTime = -1;

    public MonsterAggressionStateGoal(LargeMonster dragonEntity) {
        this.dragonEntity = dragonEntity;
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        MHNW.debugLog("MonsterAggressionStateGoal: do the roar");
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.dragonEntity.getTarget() != null) {
            LargeMonster.AggressionState aggressionState = this.dragonEntity.getAggressionState();
            if (aggressionState == LargeMonster.AggressionState.PASSIVE) {
                this.dragonEntity.triggerAnim("main_controller","roar");
                this.dragonEntity.setAggressionState(LargeMonster.AggressionState.ROAR);
                this.roarTime = 60;
            } else if (aggressionState == LargeMonster.AggressionState.ROAR) {
                this.dragonEntity.getLookControl().setLookAt(this.dragonEntity.getTarget());
                if (this.roarTime < 0) {
                    this.dragonEntity.setAggressionState(LargeMonster.AggressionState.AGGRESSIVE); // We're done roaring
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
