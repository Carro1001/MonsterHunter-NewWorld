package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.Monster;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class MonsterRandomSwim extends RandomStrollGoal {

    Monster monster;
     public MonsterRandomSwim(Monster monster, double p_25754_, int p_25755_) {
        super(monster, p_25754_, p_25755_);
        this.monster = monster;
    }

    @Override
    public boolean canUse() {
        return super.canUse() && monster.isInWater();
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && monster.isInWater();
    }

    @Override
    public void start() {
        super.start();
        monster.setWalking(true);
        monster.setSwimming(true);
    }

    @Override
    public void stop() {
        monster.setWalking(false);
        monster.setSwimming(false);
        super.stop();
    }

    @Nullable
    protected Vec3 getPosition() {
        return BehaviorUtils.getRandomSwimmablePos(this.mob, 32, 12);
    }
}