package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.LargeMonster;
import net.minecraft.world.entity.ai.goal.Goal;

public class SleepGoal extends Goal {

    LargeMonster largeMonster;
    //3 sec anim
    int animTicks = 0;
    int maxTicks = 20*10;

    public SleepGoal (LargeMonster largeMonster){
        this.largeMonster = largeMonster;
    }

    @Override
    public boolean canUse() {
        return largeMonster.isLimpining() && !largeMonster.isAttacking() && largeMonster.getRallyState() == LargeMonster.RallyState.COOL_DOWN;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        MHNW.debugLog("SleepGoal: start");
        super.start();
        largeMonster.setSleeping(true);
        largeMonster.triggerAnim("main_controller","sleep");

    }

    @Override
    public void stop() {
        MHNW.debugLog("SleepGoal: stop");
        super.stop();
        largeMonster.setSleeping(false);
    }

    @Override
    public boolean canContinueToUse() {
        return animTicks < maxTicks || largeMonster.isLimpining();
    }

    @Override
    public void tick() {
        super.tick();
        if(!largeMonster.level().isClientSide) {
            animTicks++;
            if (animTicks % 2 == 0) {
                largeMonster.heal(0.25f);
            }
        }
    }
}
