package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.LargeMonster;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class MonsterRandomSwim extends RandomStrollGoal {

    LargeMonster largeMonster;
     public MonsterRandomSwim(LargeMonster largeMonster, double p_25754_, int p_25755_) {
        super(largeMonster, p_25754_, p_25755_);
        this.largeMonster = largeMonster;
    }

    @Override
    public boolean canUse() {
        boolean canUse =super.canUse() && largeMonster.isInWater();
        MHNW.debugLog("MonsterRandomSwim: canContinueToUse" + canUse);
        return canUse;
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && largeMonster.isInWater();
    }

    @Override
    public void start() {
        super.start();
        largeMonster.setWalking(true);
        largeMonster.setSwimming(true);
    }

    @Override
    public void stop() {
        largeMonster.setWalking(false);
        largeMonster.setSwimming(false);
        super.stop();
    }

    @Nullable
    protected Vec3 getPosition() {
        return BehaviorUtils.getRandomSwimmablePos(this.mob, 32, 12);
    }
}