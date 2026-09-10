package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class MonsterRandomSwim extends RandomStrollGoal {

    NewWorldMonsterEntity newWorldMonsterEntity;
    Vec3 wantedPos;
     public MonsterRandomSwim(NewWorldMonsterEntity newWorldMonsterEntity, double p_25754_, int p_25755_) {
        super(newWorldMonsterEntity, p_25754_, p_25755_);
        this.newWorldMonsterEntity = newWorldMonsterEntity;
    }

    @Override
    public boolean canUse() {
        boolean canUse =super.canUse() && newWorldMonsterEntity.isInWater();
        MHNW.debugLog("MonsterRandomSwim: canContinueToUse" + canUse);
        return canUse;
    }

    @Override
    public boolean canContinueToUse() {
         wantedPos = new Vec3(this.wantedX, this.wantedY, this.wantedZ);
        return super.canContinueToUse() && newWorldMonsterEntity.isInWater() && !(this.wantedPos.distanceTo(this.newWorldMonsterEntity.position()) <= this.newWorldMonsterEntity.getBbWidth() * 16 * 3);
    }

    public void tick() {
    }

    @Override
    public void start() {
        super.start();
        newWorldMonsterEntity.setSwimming(true);
    }

    @Override
    public void stop() {
        newWorldMonsterEntity.setSwimming(false);
        super.stop();
    }

    @Nullable
    protected Vec3 getPosition() {
        return BehaviorUtils.getRandomSwimmablePos(this.mob, 52, 32);
    }
    //previously 32 and 12
}