package com.carro1001.mhnw.entities;

import com.carro1001.mhnw.entities.ai.RallyGoal;
import com.carro1001.mhnw.entities.ai.SleepGoal;
import com.carro1001.mhnw.registration.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import static com.carro1001.mhnw.utils.MHNWReferences.GREAT;
import static com.carro1001.mhnw.utils.MHNWReferences.IZUCHI;

public class GreatIzuchiEntity extends LargeMonster {


    public GreatIzuchiEntity(EntityType<? extends LargeMonster> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
        this.name = GREAT + "_" + IZUCHI;
        shouldRage = true;
    }

    protected PlayState poseBody(AnimationState<LargeMonster> animationState) {
        if(getDeathState() >= 1){
            return super.poseBody(animationState);
        }
        if (this.isAttacking() && getAttackingID() == ATTACK_ID.TAIL_SWIPE.ordinal()) {
            return animationState.setAndContinue(RawAnimation.begin().thenPlay("animation.great_izuchi.tailswipe"));
        }
        if (this.isAttacking() && getAttackingID() == ATTACK_ID.TAIL_SLAM.ordinal()) {
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

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        for (int i = 0; i < 2; i++) {
            BlockPos pos = i == 0 ? getOnPos().east(4):getOnPos().west(4);

            IzuchiEntity izuchi = ModEntities.IZUCHI.get().create(pLevel.getLevel());
            izuchi.setPos(pos.getX(),pos.getY()+1,pos.getZ());
            pLevel.getLevel().addFreshEntity(izuchi);
        }
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return LargeMonster.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 350)
                .add(Attributes.FOLLOW_RANGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.ARMOR, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, (double)0.6F)
                .add(Attributes.ATTACK_KNOCKBACK, (double)1F)
                .add(Attributes.ARMOR_TOUGHNESS,1.0D);
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
            return !monster.isLimpining() && monster.getTarget() != null && super.canUse();
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
