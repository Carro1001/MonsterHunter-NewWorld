package com.carro1001.mhnw.entities.ai;

import com.carro1001.mhnw.entities.DragonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import java.util.EnumSet;

public class DragonTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal {
    private final DragonEntity dragonEntity;

    public DragonTargetGoal(DragonEntity pMob, Class<T> pTargetType, boolean pMustSee, boolean pMustReach) {
        super(pMob,pTargetType,pMustSee,pMustReach);
        this.dragonEntity = pMob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void start() {
        super.start();
        dragonEntity.prevAnimation = dragonEntity.getStateDir();
        dragonEntity.setStateDir(3);
        this.dragonEntity.setTarget(this.target);
        System.out.println("target aquired, going from: " + dragonEntity.prevAnimation);

    }

    @Override
    public void stop() {
        super.stop();
        dragonEntity.setStateDir(dragonEntity.prevAnimation);
        System.out.println("Stopped target, going to: " + dragonEntity.prevAnimation);
    }

}
