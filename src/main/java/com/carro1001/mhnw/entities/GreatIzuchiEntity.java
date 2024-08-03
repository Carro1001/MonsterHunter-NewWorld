package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.RallyGoal;
import com.carro1001.mhnw.entities.ai.SleepGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import static com.carro1001.mhnw.utils.MHNWReferences.GREAT;
import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

public class GreatIzuchiEntity extends LargeMonster {


    public GreatIzuchiEntity(EntityType<? extends LargeMonster> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.name = GREAT + "_" + IZUCHI;
    }

    protected PlayState poseBody(AnimationState<LargeMonster> animationState) {
        if(getDeathState() >= 1){
            return super.poseBody(animationState);
        }
        if (this.Attacking() && GetAttackingID() == ATTACK_ID.TAIL_SWIPE.ordinal()) {
            return animationState.setAndContinue(RawAnimation.begin().thenPlay("animation.great_izuchi.tailswipe"));
        }
        if (this.Attacking() && GetAttackingID() == ATTACK_ID.TAIL_SLAM.ordinal()) {
            return animationState.setAndContinue(RawAnimation.begin().thenPlay("animation.great_izuchi.tailslam"));
        }
        return super.poseBody(animationState);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(5, new TailAttackGoal(this));
        this.goalSelector.addGoal(8, new RallyGoal(this));
        this.goalSelector.addGoal(10, new SleepGoal(this));
    }

    public enum ATTACK_ID{
        NONE,
        TAIL_SWIPE,
        TAIL_SLAM
    }

    public class TailAttackGoal extends MeleeAttackGoal {

        GreatIzuchiEntity monster;
        //3 sec anim
        int animTicks = 0;
        int maxTicks = 20*3;
        public TailAttackGoal (GreatIzuchiEntity monster){
            super(monster, 1.2f,true);
            this.monster = monster;
        }

        @Override
        public boolean canUse() {
            return !monster.IsLimpining() && monster.getTarget() != null && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            int state = monster.random.nextIntBetweenInclusive(1,2);
            maxTicks = state == 1? 60 : 100;
            monster.setAttackingID(state);
            monster.setAttacking(true);
        }

        @Override
        public void stop() {
            super.stop();
            monster.setAttackingID(ATTACK_ID.NONE.ordinal());
            monster.setAttacking(false);
        }

        @Override
        public boolean canContinueToUse() {
            return animTicks < maxTicks && super.canContinueToUse();
        }

        @Override
        public void tick() {
            super.tick();
            if(!monster.level().isClientSide){
                animTicks++;
            }
        }
    }

}
