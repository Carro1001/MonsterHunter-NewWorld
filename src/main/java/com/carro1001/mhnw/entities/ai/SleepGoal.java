package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.MHNW;
import com.carro1001.mhnw.entities.NewWorldMonsterEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class SleepGoal extends Goal {

    NewWorldMonsterEntity newWorldMonsterEntity;
    //3 sec anim
    int animTicks = 0;
    int maxTicks = 20*10;

    public SleepGoal (NewWorldMonsterEntity newWorldMonsterEntity){
        this.newWorldMonsterEntity = newWorldMonsterEntity;
    }

    @Override
    public boolean canUse() {
        return newWorldMonsterEntity.isLimpining() && !newWorldMonsterEntity.isAttacking() && newWorldMonsterEntity.getRallyState() == NewWorldMonsterEntity.RallyState.COOL_DOWN;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        MHNW.debugLog("SleepGoal: start");
        super.start();
        newWorldMonsterEntity.getMoveControl().setWantedPosition(newWorldMonsterEntity.getX(), newWorldMonsterEntity.getY(), newWorldMonsterEntity.getZ(), 0);
        newWorldMonsterEntity.setDeltaMovement(Vec3.ZERO);
        newWorldMonsterEntity.setSleeping(true);
        newWorldMonsterEntity.triggerAnim("main_controller","sleep");

    }

    @Override
    public void stop() {
        MHNW.debugLog("SleepGoal: stop");
        super.stop();
        newWorldMonsterEntity.setSleeping(false);
    }

    @Override
    public boolean canContinueToUse() {
        return animTicks < maxTicks || newWorldMonsterEntity.isLimpining();
    }
    @Override
    public void tick() {
        super.tick();
        newWorldMonsterEntity.setDeltaMovement(Vec3.ZERO);
        if(!newWorldMonsterEntity.level().isClientSide) {
            animTicks++;
            if (animTicks % 2 == 0) {
                newWorldMonsterEntity.heal(0.25f);
            }
        }
    }
}
