package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.Monster;
import net.minecraft.world.entity.ai.goal.Goal;

public class SleepGoal extends Goal {

    Monster monster;
    //3 sec anim
    int animTicks = 0;
    int maxTicks = 20*10;

    public SleepGoal (Monster monster){
        this.monster = monster;
    }

    @Override
    public boolean canUse() {
        return monster.IsLimpining() && monster.getRallyState() == Monster.RallyState.COOL_DOWN;
    }

    @Override
    public void start() {
        super.start();
        monster.setSleeping(true);
    }

    @Override
    public void stop() {
        super.stop();
        monster.setSleeping(false);
    }

    @Override
    public boolean canContinueToUse() {
        return animTicks < maxTicks || monster.IsLimpining();
    }

    @Override
    public void tick() {
        super.tick();
        animTicks++;
        if(animTicks%2 == 0){
            monster.heal(0.25f);
        }
    }
}
