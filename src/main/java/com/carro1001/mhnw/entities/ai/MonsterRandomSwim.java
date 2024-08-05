package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.LargeMonster;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class MonsterRandomSwim extends RandomStrollGoal {

    LargeMonster largeMonster;
    Vec3 wantedPos;
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
         wantedPos = new Vec3(this.wantedX, this.wantedY, this.wantedZ);
        return super.canContinueToUse() && largeMonster.isInWater() && !(this.wantedPos.distanceTo(this.largeMonster.position()) <= this.largeMonster.getBbWidth() * 16 * 3);
    }

    public void tick() {
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
        return BehaviorUtils.getRandomSwimmablePos(this.mob, 52, 32);
    }
    //previously 32 and 12
}